package dev.vladon.vesna.launcher.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.FolderEntity
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.data.WorkspacePageEntity
import dev.vladon.vesna.launcher.data.toDomain
import dev.vladon.vesna.launcher.platform.AppDiscovery
import dev.vladon.vesna.launcher.platform.AppLauncher
import dev.vladon.vesna.launcher.platform.PackageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceRepositoryPagesTest {
    @Test
    fun pageOrderDoesNotChangePinIdentityOrCellOccupancy() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            val pages = db.workspacePageDao()
            val first = pages.getAll().single().id
            val second = pages.insert(WorkspacePageEntity(rank = 1))
            val repository = WorkspaceRepository(
                db.workspaceItemDao(), db, db.folderDao(),
                context.getSharedPreferences("qa_workspace_pages", Context.MODE_PRIVATE),
            )
            val app = LauncherApp(AppKey("qa.pin", "Main"), "Pin", null)
            val other = LauncherApp(AppKey("qa.other", "Main"), "Other", null)
            assertTrue(repository.insertWorkspaceAt(app, 0, first))
            assertTrue(repository.insertWorkspaceAt(app, 0, second))
            assertFalse(repository.insertWorkspaceAt(other, 0, second))
            assertFalse(repository.insertWorkspaceAt(other, 1, Long.MAX_VALUE))
            assertTrue(repository.insertWorkspaceAt(other, 1, first))
            assertTrue(repository.moveWithinWorkspace(app, 0, 1, second))
            val before = repository.items.first().sortedBy { it.id }

            pages.updateRank(second, -1)

            assertEquals(listOf(second, first), pages.getAll().map { it.id })
            assertEquals(before, repository.items.first().sortedBy { it.id })
            assertEquals(mapOf(0 to app, 1 to other), WorkspaceResolver.workspaceApps(before, listOf(app, other), first))
            assertEquals(mapOf(1 to app), WorkspaceResolver.workspaceApps(before, listOf(app, other), second))
            repository.removeFromWorkspace(app, cell = 1, pageId = second)
            assertEquals(before.filter { it.pageId == first }, repository.items.first().sortedBy { it.id })
        } finally {
            db.close()
        }
    }

    @Test
    fun folderDissolutionReturnsMembersToItsOwnPageAndDockStaysGlobal() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            val pages = db.workspacePageDao()
            val first = pages.getAll().single().id
            val second = pages.insert(WorkspacePageEntity(rank = 1))
            val dao = db.workspaceItemDao()
            val repository = WorkspaceRepository(
                dao, db, db.folderDao(), context.getSharedPreferences("qa_workspace_pages", Context.MODE_PRIVATE),
            )
            val target = LauncherApp(AppKey("qa.target", "Main"), "Target", null)
            val dock = LauncherApp(AppKey("qa.dock", "Main"), "Dock", null)
            val other = LauncherApp(AppKey("qa.other", "Main"), "Other", null)
            assertTrue(repository.insertWorkspaceAt(other, 0, first))
            assertTrue(repository.insertWorkspaceAt(target, 3, second))
            assertTrue(repository.insertIntoDock(dock, 0))
            val dockPinId = dao.getAll().single { it.container == Container.DOCK.name }.id
            assertTrue(repository.createFolderFromDrop(dock, 3, "Folder", WorkspaceRepository.FolderDropSource.Dock(dockPinId), pageId = second))
            val folder = db.folderDao().getAll().single()
            assertEquals(second, folder.pageId)
            assertFalse(repository.insertWorkspaceAt(other, 3, second))
            assertTrue(repository.insertWorkspaceAt(other, 3, first))
            val members = dao.getAll().filter { it.folderId == folder.id }
            assertEquals(setOf(target.packageName, dock.packageName), members.map { it.packageName }.toSet())
            assertTrue(members.all { it.pageId == null })

            assertTrue(repository.removeFromFolder(dock, folder.id))

            assertNull(db.folderDao().getById(folder.id))
            val items = dao.getAll().map { it.toDomain() }
            assertEquals(mapOf(0 to dock, 3 to target), WorkspaceResolver.workspaceApps(items, listOf(target, dock, other), second))
            assertEquals(DockMoveResult.MOVED, repository.moveWorkspaceToDock(dock, fromCell = 0, slot = 0, pageId = second))
            assertNull(dao.getAll().single { it.packageName == dock.packageName }.pageId)
            assertEquals(listOf(DockPin(dockPinId, dock)), WorkspaceResolver.dockPins(repository.items.first(), listOf(dock)))
        } finally {
            db.close()
        }
    }

    @Test
    fun pageCreationIsCappedAndRanksContinueFromTheExistingMaximum() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            val repository = WorkspaceRepository(
                db.workspaceItemDao(), db, db.folderDao(),
                context.getSharedPreferences("qa_page_controls_create", Context.MODE_PRIVATE),
            )
            val defaultId = db.workspacePageDao().getAll().single().id
            val created = List(MAX_WORKSPACE_PAGES - 1) { checkNotNull(repository.createPage()) }

            val pages = repository.pages.first()
            // Display order: the default page (rank 0) first, then creations in order.
            assertEquals(listOf(defaultId) + created, pages.map { it.id })
            assertEquals(List(MAX_WORKSPACE_PAGES) { it }, pages.map { it.rank })

            // The cap refuses without side effects: count and ranks stay untouched.
            assertNull(repository.createPage())
            assertEquals(MAX_WORKSPACE_PAGES, repository.pages.first().size)
            assertEquals(List(MAX_WORKSPACE_PAGES) { it }, repository.pages.first().map { it.rank })
        } finally {
            db.close()
        }
    }

    @Test
    fun onlyAnEmptyExtraPageCanBeRemoved() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        val prefsName = "qa_remove_empty_page"
        context.deleteSharedPreferences(prefsName)
        try {
            val dao = db.workspaceItemDao()
            val folders = db.folderDao()
            val repository = WorkspaceRepository(dao, db, folders, context.getSharedPreferences(prefsName, Context.MODE_PRIVATE))
            val occupied = checkNotNull(repository.createPage())
            val empty = checkNotNull(repository.createPage())
            val folderPage = checkNotNull(repository.createPage())
            val app = LauncherApp(AppKey("qa.saved", "Main"), "Saved", null)
            assertTrue(repository.insertWorkspaceAt(app, 0, occupied))
            assertTrue(repository.insertIntoDock(app, 1))
            folders.insert(FolderEntity(title = "Saved folder", cellIndex = 0, pageId = folderPage))
            val savedItems = dao.getAll()
            val savedFolders = folders.getAll()
            val initialPages = repository.pages.first()

            assertFalse(repository.removeEmptyPage(DEFAULT_WORKSPACE_PAGE_ID))
            assertFalse(repository.removeEmptyPage(occupied))
            assertFalse(repository.removeEmptyPage(folderPage))
            assertEquals(initialPages, repository.pages.first())

            assertTrue(repository.selectPage(empty))
            assertTrue(repository.removeEmptyPage(empty))
            assertEquals(DEFAULT_WORKSPACE_PAGE_ID, repository.savedActivePageId())
            // Removing a middle page must not reassign any ID, rank, or saved content.
            assertEquals(initialPages.filterNot { it.id == empty }, repository.pages.first())
            assertEquals(savedItems, dao.getAll())
            assertEquals(savedFolders, folders.getAll())
        } finally {
            db.close()
            context.deleteSharedPreferences(prefsName)
        }
    }

    @Test
    fun selectionUsesItsOwnPreferenceKeyAndSurvivesRepositoryRecreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            context.deleteSharedPreferences("qa_page_controls_select")
            val prefs = context.getSharedPreferences("qa_page_controls_select", Context.MODE_PRIVATE)
            val repository = WorkspaceRepository(db.workspaceItemDao(), db, db.folderDao(), prefs)
            assertEquals(DEFAULT_WORKSPACE_PAGE_ID, repository.savedActivePageId())
            val second = repository.createPage()!!

            assertTrue(repository.selectPage(second))
            assertEquals(second, repository.savedActivePageId())
            // Unknown pages are refused and leave the saved selection untouched.
            assertFalse(repository.selectPage(99L))
            assertEquals(second, repository.savedActivePageId())
            // A fresh repository (next process) reads the same stable ID.
            val recreated = WorkspaceRepository(db.workspaceItemDao(), db, db.folderDao(), prefs)
            assertEquals(second, recreated.savedActivePageId())

            // Seeding and the seeded flag must not touch the selection key.
            val apps = List(6) { n -> LauncherApp(AppKey("qa.sel$n", "Main"), "Sel $n", null) }
            assertTrue(repository.seedOrAdopt(SeedPlanner.plan(apps)))
            assertTrue(repository.isSeeded())
            assertEquals(second, repository.savedActivePageId())
        } finally {
            db.close()
        }
    }

    @Test
    fun firstRunSeedTargetsTheDefaultPageEvenWhenAPageWasCreatedFirst() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            context.deleteSharedPreferences("qa_page_controls_seed")
            val repository = WorkspaceRepository(
                db.workspaceItemDao(), db, db.folderDao(),
                context.getSharedPreferences("qa_page_controls_seed", Context.MODE_PRIVATE),
            )
            val earlyPage = repository.createPage()!!
            val apps = List(6) { n -> LauncherApp(AppKey("qa.seed$n", "Main"), "Seed $n", null) }

            assertTrue(repository.seedOrAdopt(SeedPlanner.plan(apps)))
            assertTrue(repository.isSeeded())
            // The seed targeted the default page; the early page survived untouched.
            assertEquals(listOf(DEFAULT_WORKSPACE_PAGE_ID, earlyPage), db.workspacePageDao().getAll().map { it.id })
            assertEquals(listOf(0, 1), db.workspacePageDao().getAll().map { it.rank })
            val workspaceItems = db.workspaceItemDao().getAll().map { it.toDomain() }
                .filter { it.container == Container.WORKSPACE }
            assertEquals(listOf(DEFAULT_WORKSPACE_PAGE_ID, DEFAULT_WORKSPACE_PAGE_ID), workspaceItems.map { it.pageId })
            assertEquals(listOf(0, 1), workspaceItems.map { it.cellIndex }.sorted())
            assertEquals(setOf(apps[4].key, apps[5].key), workspaceItems.map { it.key }.toSet())
            assertTrue(db.workspaceItemDao().getAll().map { it.toDomain() }
                .filter { it.container == Container.DOCK }.all { it.pageId == null })
            // Creating a page before seeding did not touch the selection preference.
            assertEquals(DEFAULT_WORKSPACE_PAGE_ID, repository.savedActivePageId())
        } finally {
            db.close()
        }
    }

    @Test
    fun seedOrAdoptAdoptsAnEarlyPinInsteadOfWipingIt() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            context.deleteSharedPreferences("qa_page_controls_adopt")
            val repository = WorkspaceRepository(
                db.workspaceItemDao(), db, db.folderDao(),
                context.getSharedPreferences("qa_page_controls_adopt", Context.MODE_PRIVATE),
            )
            val pageId = repository.createPage()!!
            val app = LauncherApp(AppKey("qa.early", "Main"), "Early", null)
            assertTrue(repository.insertWorkspaceAt(app, cell = 2, pageId = pageId))
            val plan = SeedPlanner.plan(List(6) { n -> LauncherApp(AppKey("qa.n$n", "Main"), "N$n", null) })

            // A pin landed before the first seed finished: adopt it, never wipe it.
            assertFalse(repository.seedOrAdopt(plan))
            assertTrue(repository.isSeeded())
            val items = db.workspaceItemDao().getAll().map { it.toDomain() }
            assertEquals(listOf(app.key), items.map { it.key })
            assertEquals(pageId, items.single().pageId)
            assertEquals(2, items.single().cellIndex)
            // No seed items were inserted; the default page stayed empty.
            assertEquals(listOf(DEFAULT_WORKSPACE_PAGE_ID, pageId), db.workspacePageDao().getAll().map { it.id })
        } finally {
            db.close()
        }
    }

    @Test(timeout = 15_000)
    fun controllerProjectsCellsAndScopesActionsToTheSuppliedPage() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        context.deleteSharedPreferences("qa_page_controls_controller")
        val prefs = context.getSharedPreferences("qa_page_controls_controller", Context.MODE_PRIVATE)
        val apps = List(6) { n -> LauncherApp(AppKey("qa.app$n", "Main"), "App $n", null) }
        val discovery = CountingDiscovery(apps)
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        try {
            val repository = WorkspaceRepository(db.workspaceItemDao(), db, db.folderDao(), prefs)
            val controller = LauncherController(
                scope,
                discovery,
                AppLauncher(context),
                repository,
                MutableSharedFlow<PackageEvent>(),
            )
            // First run: the seed lands on the default page and page metadata emits.
            val seeded = controller.state.first { it.pageIds.isNotEmpty() && it.workspaceCells.isNotEmpty() }
            assertEquals(listOf(DEFAULT_WORKSPACE_PAGE_ID), seeded.pageIds)
            assertEquals(DEFAULT_WORKSPACE_PAGE_ID, seeded.activePageId)
            assertEquals(CellContent.App(apps[4]), seeded.workspaceCells.getValue(0))
            assertEquals(CellContent.App(apps[5]), seeded.workspaceCells.getValue(1))

            // Create appends after the default page and selects it in one snapshot.
            controller.createWorkspacePage()
            val second = controller.state.first { it.pageIds.size == 2 && it.activePageId == it.pageIds.last() }
            val secondPageId = second.pageIds.last()
            assertTrue(second.workspaceCells.isEmpty())
            assertTrue(second.canRemoveActivePage)

            // A drawer insert lands on the rendered page only.
            controller.insertWorkspaceAt(apps[5], cell = 0, pageId = secondPageId)
            val placed = controller.state.first { it.workspaceCells.isNotEmpty() }
            assertEquals(secondPageId, placed.activePageId)
            assertEquals(mapOf(0 to CellContent.App(apps[5])), placed.workspaceCells)
            assertFalse(placed.canRemoveActivePage)

            // Selecting another page re-projects that page's own cells.
            controller.selectWorkspacePage(DEFAULT_WORKSPACE_PAGE_ID)
            val backOnDefault = controller.state.first {
                it.activePageId == DEFAULT_WORKSPACE_PAGE_ID &&
                    it.workspaceCells.getValue(1) == CellContent.App(apps[5])
            }
            assertEquals(CellContent.App(apps[4]), backOnDefault.workspaceCells.getValue(0))
            assertEquals(1, discovery.calls)

            // Removal is scoped to the page passed to the controller.
            controller.removeFromWorkspace(apps[5], cell = 0, pageId = secondPageId)
            repository.items.first { items -> items.none { it.pageId == secondPageId } }
            assertEquals(CellContent.App(apps[5]), controller.state.value.workspaceCells.getValue(1))
            assertEquals(CellContent.App(apps[4]), controller.state.value.workspaceCells.getValue(0))

            // A disabled or undiscovered app still has a saved pin on its page.
            val unavailable = LauncherApp(AppKey("qa.unavailable", "Main"), "Unavailable", null)
            assertTrue(repository.insertWorkspaceAt(unavailable, cell = 0, pageId = secondPageId))
            controller.selectWorkspacePage(secondPageId)
            controller.state.first { it.activePageId == secondPageId && it.isWorkspaceEmpty && !it.canRemoveActivePage }

            repository.removeFromWorkspace(unavailable, cell = 0, pageId = secondPageId)
            controller.state.first { it.activePageId == secondPageId && it.canRemoveActivePage }
            controller.removeEmptyWorkspacePage(secondPageId)
            val afterRemoval = controller.state.first { it.pageIds == listOf(DEFAULT_WORKSPACE_PAGE_ID) }
            assertEquals(DEFAULT_WORKSPACE_PAGE_ID, afterRemoval.activePageId)
            assertEquals(backOnDefault.workspaceCells, afterRemoval.workspaceCells)
            assertEquals(backOnDefault.dockPins, afterRemoval.dockPins)
            assertEquals(1, discovery.calls)
        } finally {
            scope.cancel()
            db.close()
        }
    }

    private class CountingDiscovery(private val apps: List<LauncherApp>) : AppDiscovery {
        var calls = 0
            private set

        override fun discover(): List<LauncherApp> {
            calls++
            return apps
        }

        override fun isPackageInstalled(packageName: String, userSerial: Long) = true
    }
}
