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
    fun reorderingShiftsPagesAndKeepsSelectionAndSavedContentsOnTheirOriginalIds() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        val prefsName = "qa_workspace_page_reorder"
        context.deleteSharedPreferences(prefsName)
        try {
            val pages = db.workspacePageDao()
            val first = pages.getAll().single().id
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val repository = WorkspaceRepository(db.workspaceItemDao(), db, db.folderDao(), prefs)
            val second = requireNotNull(repository.createPage())
            val removed = requireNotNull(repository.createPage())
            val third = requireNotNull(repository.createPage())
            val fourth = requireNotNull(repository.createPage())
            assertTrue(repository.removeEmptyPage(removed))
            assertTrue(repository.selectPage(second))
            val app = LauncherApp(AppKey("qa.pin", "Main"), "Pin", null)
            val other = LauncherApp(AppKey("qa.other", "Main"), "Other", null)
            assertTrue(repository.insertWorkspaceAt(app, 0, first))
            assertTrue(repository.insertWorkspaceAt(app, 0, second))
            assertFalse(repository.insertWorkspaceAt(other, 0, second))
            assertFalse(repository.insertWorkspaceAt(other, 1, Long.MAX_VALUE))
            assertTrue(repository.insertWorkspaceAt(other, 1, first))
            assertTrue(repository.moveWithinWorkspace(app, 0, 1, second))
            assertTrue(repository.insertIntoDock(other, 0))
            db.folderDao().insert(FolderEntity(title = "Saved folder", cellIndex = 5, pageId = second))
            val before = repository.items.first().sortedBy { it.id }

            val savedFolders = db.folderDao().getAll()
            val initial = listOf(first, second, third, fourth)
            val reordered = listOf(first, third, fourth, second)
            assertTrue(repository.reorderPage(second, destinationIndex = 3, displayedOrder = initial))
            val recreated = WorkspaceRepository(db.workspaceItemDao(), db, db.folderDao(), prefs)
            assertEquals(reordered, recreated.pages.first().map { it.id })
            assertEquals(second, recreated.savedActivePageId())
            assertEquals(savedFolders, db.folderDao().getAll())
            assertEquals(before, repository.items.first().sortedBy { it.id })
            val apps = listOf(app, other).associateBy { it.key }
            assertEquals(
                mapOf(0 to CellContent.App(app), 1 to CellContent.App(other)),
                WorkspaceResolver.workspaceCells(before, apps, emptyList(), first),
            )
            assertEquals(mapOf(1 to CellContent.App(app)), WorkspaceResolver.workspaceCells(before, apps, emptyList(), second))
            repository.removeFromWorkspace(app, cell = 1, pageId = second)
            assertEquals(before.filterNot { it.pageId == second }, repository.items.first().sortedBy { it.id })
            assertTrue(repository.reorderPage(second, destinationIndex = 1, displayedOrder = reordered))
            assertEquals(initial, recreated.pages.first().map { it.id })
        } finally {
            db.close()
            context.deleteSharedPreferences(prefsName)
        }
    }

    @Test
    fun reorderingRejectsTheFixedFirstPageAndOutdatedPositions() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            val repository = WorkspaceRepository(
                db.workspaceItemDao(), db, db.folderDao(),
                context.getSharedPreferences("qa_workspace_page_reorder", Context.MODE_PRIVATE),
            )
            val first = DEFAULT_WORKSPACE_PAGE_ID
            val second = requireNotNull(repository.createPage())
            val third = requireNotNull(repository.createPage())
            val initial = listOf(first, second, third)
            assertFalse(repository.reorderPage(first, destinationIndex = 1, displayedOrder = initial))
            assertFalse(repository.reorderPage(second, destinationIndex = 0, displayedOrder = initial))
            assertEquals(initial, repository.pages.first().map { it.id })

            assertTrue(repository.reorderPage(third, destinationIndex = 1, displayedOrder = initial))
            val reordered = listOf(first, third, second)
            assertFalse(repository.reorderPage(second, destinationIndex = 2, displayedOrder = initial))
            assertEquals(reordered, repository.pages.first().map { it.id })

            assertTrue(repository.removeEmptyPage(second))
            assertFalse(repository.reorderPage(third, destinationIndex = 2, displayedOrder = reordered))
            assertEquals(listOf(first, third), repository.pages.first().map { it.id })
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
            val apps = listOf(target, dock, other).associateBy { it.key }
            assertEquals(
                mapOf(0 to CellContent.App(dock), 3 to CellContent.App(target)),
                WorkspaceResolver.workspaceCells(items, apps, emptyList(), second),
            )
            assertEquals(DockMoveResult.MOVED, repository.moveWorkspaceToDock(dock, fromCell = 0, slot = 0, pageId = second))
            assertNull(dao.getAll().single { it.packageName == dock.packageName }.pageId)
            assertEquals(listOf(DockPin(dockPinId, dock)), WorkspaceResolver.dockLayout(repository.items.first(), apps).pins)
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
            assertEquals(backOnDefault.dock, afterRemoval.dock)
            assertEquals(1, discovery.calls)
        } finally {
            scope.cancel()
            db.close()
        }
    }

    @Test(timeout = 15_000)
    fun catalogueRefreshKeepsPersonalAndWorkPinsDistinctAcrossSurfaces() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        val prefsName = "qa_page_catalogue_refresh"
        context.deleteSharedPreferences(prefsName)
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        try {
            val repository = WorkspaceRepository(
                db.workspaceItemDao(), db, db.folderDao(), context.getSharedPreferences(prefsName, Context.MODE_PRIVATE),
            )
            val personal = LauncherApp(AppKey("qa.dual", "Main"), "Personal")
            val work = LauncherApp(personal.key.copy(userSerial = 10), "Work")
            val second = requireNotNull(repository.createPage())
            assertTrue(repository.insertWorkspaceAt(personal, cell = 0, pageId = DEFAULT_WORKSPACE_PAGE_ID))
            assertTrue(repository.insertWorkspaceAt(work, cell = 0, pageId = second))
            assertTrue(repository.insertWorkspaceAt(personal, cell = 2, pageId = second))
            assertTrue(repository.insertIntoDock(work, slot = 0))
            assertTrue(repository.createFolderFromDrop(work, 2, "Both", WorkspaceRepository.FolderDropSource.Drawer, second))
            val folder = db.folderDao().getAll().single().id
            val savedItems = db.workspaceItemDao().getAll().sortedBy { it.id }

            val discovery = CountingDiscovery(listOf(work, personal))
            val events = MutableSharedFlow<PackageEvent>()
            val controller = LauncherController(scope, discovery, AppLauncher(context), repository, events)
            events.subscriptionCount.first { it > 0 }
            controller.selectWorkspacePage(second)
            val both = controller.state.first { it.activePageId == second && it.folderContents[folder]?.size == 2 }
            assertEquals(listOf(work, personal), both.apps)
            assertEquals(CellContent.App(work), both.workspaceCells[0])
            assertEquals(work, both.dock.pins.single().app)
            assertEquals(listOf(personal, work), both.folderContents[folder])

            discovery.apps = listOf(personal)
            events.emit(PackageEvent.CatalogueChanged)
            val personalOnly = controller.state.first { it.apps == listOf(personal) }
            assertFalse(personalOnly.workspaceCells.containsKey(0))
            assertTrue(personalOnly.dock.pins.isEmpty())
            assertEquals(listOf(personal), personalOnly.folderContents[folder])
            assertEquals(savedItems, db.workspaceItemDao().getAll().sortedBy { it.id })

            val updatedWork = work.copy(label = "Updated Work")
            discovery.apps = listOf(personal, updatedWork)
            events.emit(PackageEvent.CatalogueChanged)
            val restored = controller.state.first { it.apps == discovery.apps }
            assertEquals(CellContent.App(updatedWork), restored.workspaceCells[0])
            assertEquals(updatedWork, restored.dock.pins.single().app)
            assertEquals(listOf(personal, updatedWork), restored.folderContents[folder])
            controller.selectWorkspacePage(DEFAULT_WORKSPACE_PAGE_ID)
            val first = controller.state.first { it.activePageId == DEFAULT_WORKSPACE_PAGE_ID }
            assertEquals(CellContent.App(personal), first.workspaceCells[0])
            assertEquals(savedItems, db.workspaceItemDao().getAll().sortedBy { it.id })
        } finally {
            scope.cancel()
            db.close()
            context.deleteSharedPreferences(prefsName)
        }
    }

    @Test
    fun pageMoveUsesTheExactPinAndTheFirstUnoccupiedCell() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            val dao = db.workspaceItemDao()
            val folders = db.folderDao()
            val repository = WorkspaceRepository(
                dao, db, folders, context.getSharedPreferences("qa_workspace_page_moves", Context.MODE_PRIVATE),
            )
            val source = DEFAULT_WORKSPACE_PAGE_ID
            val full = db.workspacePageDao().insert(WorkspacePageEntity(rank = 1))
            val destination = db.workspacePageDao().insert(WorkspacePageEntity(rank = 9))
            val app = LauncherApp(AppKey("qa.duplicate", "Main"), "Duplicate", null)
            assertTrue(repository.insertWorkspaceAt(app, 0, source))
            assertTrue(repository.insertWorkspaceAt(app, 3, source))
            assertTrue(repository.insertWorkspaceAt(app, 0, destination))
            assertTrue(repository.addToDock(app))
            folders.insert(FolderEntity(title = "Reserved", pageId = destination, cellIndex = 1))
            for (cell in 0 until GridCanvas.CAPACITY) {
                assertTrue(repository.insertWorkspaceAt(app, cell, full))
            }
            val before = dao.getAll().sortedBy { it.id }
            val savedFolders = folders.getAll()
            val selectedId = before.single { it.pageId == source && it.cellIndex == 3 }.id

            val choices = requireNotNull(repository.pageMoveOptions(app.key, cell = 3, pageId = source))
            assertEquals(selectedId, choices.sourceId)
            assertEquals(listOf(PageMoveTarget(destination, number = 3)), choices.pages)
            assertEquals(PageMoveResult.MOVED, repository.movePinToPage(selectedId, 3, source, destination))
            assertEquals(
                before.map { if (it.id == selectedId) it.copy(pageId = destination, cellIndex = 2) else it },
                dao.getAll().sortedBy { it.id },
            )
            assertEquals(savedFolders, folders.getAll())
        } finally {
            db.close()
        }
    }

    @Test
    fun pageMoveRevalidatesCapacityAndDoesNotMoveAReplacementPin() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            val dao = db.workspaceItemDao()
            val repository = WorkspaceRepository(
                dao, db, db.folderDao(), context.getSharedPreferences("qa_workspace_page_moves", Context.MODE_PRIVATE),
            )
            val origin = DEFAULT_WORKSPACE_PAGE_ID
            val target = requireNotNull(repository.createPage())
            val retiredTarget = requireNotNull(repository.createPage())
            val app = LauncherApp(AppKey("qa.move", "Main"), "Move", null)
            assertTrue(repository.insertWorkspaceAt(app, 0, origin))
            for (cell in 0 until GridCanvas.CAPACITY - 1) {
                assertTrue(repository.insertWorkspaceAt(app, cell, target))
            }
            val choice = requireNotNull(repository.pageMoveOptions(app.key, 0, origin))
            assertTrue(choice.pages.any { it.id == target })
            assertTrue(repository.insertWorkspaceAt(app, GridCanvas.CAPACITY - 1, target))
            assertTrue(repository.removeEmptyPage(retiredTarget))
            val fullLayout = dao.getAll().sortedBy { it.id }

            assertEquals(PageMoveResult.FULL, repository.movePinToPage(choice.sourceId, 0, origin, target))
            assertEquals(PageMoveResult.UNAVAILABLE, repository.movePinToPage(choice.sourceId, 0, origin, retiredTarget))
            assertEquals(fullLayout, dao.getAll().sortedBy { it.id })

            repository.removeFromWorkspace(app, cell = 0, pageId = target)
            repository.removeFromWorkspace(app, cell = 0, pageId = origin)
            assertTrue(repository.insertWorkspaceAt(app, 0, origin))
            val replacementLayout = dao.getAll().sortedBy { it.id }
            assertEquals(PageMoveResult.UNAVAILABLE, repository.movePinToPage(choice.sourceId, 0, origin, target))
            assertEquals(replacementLayout, dao.getAll().sortedBy { it.id })
        } finally {
            db.close()
        }
    }

    @Test
    fun folderMoveSkipsFullPagesAndOnlyUpdatesTheSelectedFoldersPlacement() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            val dao = db.workspaceItemDao()
            val folders = db.folderDao()
            val repository = WorkspaceRepository(
                dao, db, folders, context.getSharedPreferences("qa_workspace_page_moves", Context.MODE_PRIVATE),
            )
            val source = DEFAULT_WORKSPACE_PAGE_ID
            val full = requireNotNull(repository.createPage())
            val destination = requireNotNull(repository.createPage())
            val one = LauncherApp(AppKey("qa.folder.one", "Main"), "One", null)
            val two = LauncherApp(AppKey("qa.folder.two", "Main"), "Two", null)
            assertTrue(repository.insertWorkspaceAt(one, 7, source))
            assertTrue(repository.insertWorkspaceAt(two, 8, source))
            assertTrue(repository.createFolderFromDrop(
                two, 7, "Tools", WorkspaceRepository.FolderDropSource.Workspace(8), source,
            ))
            val selected = folders.getAll().single()
            val folder = Folder(selected.id, selected.title, selected.cellIndex, source)
            for (cell in 0 until GridCanvas.CAPACITY) {
                assertTrue(repository.insertWorkspaceAt(one, cell, full))
            }
            assertTrue(repository.insertWorkspaceAt(two, 0, destination))
            folders.insert(FolderEntity(title = "Tools", cellIndex = 1, pageId = destination))
            val savedItems = dao.getAll().sortedBy { it.id }
            val savedFolders = folders.getAll().sortedBy { it.id }

            assertEquals(listOf(PageMoveTarget(destination, number = 3)), repository.folderPageMoveOptions(folder)?.pages)
            assertEquals(PageMoveResult.MOVED, repository.moveFolderToPage(folder, destination))
            assertEquals(
                savedFolders.map { if (it.id == selected.id) it.copy(pageId = destination, cellIndex = 2) else it },
                folders.getAll().sortedBy { it.id },
            )
            assertEquals(savedItems, dao.getAll().sortedBy { it.id })
        } finally {
            db.close()
        }
    }

    @Test
    fun folderMoveRejectsStalePlacementsAndDestinations() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            val folders = db.folderDao()
            val repository = WorkspaceRepository(
                db.workspaceItemDao(), db, folders, context.getSharedPreferences("qa_workspace_page_moves", Context.MODE_PRIVATE),
            )
            val source = DEFAULT_WORKSPACE_PAGE_ID
            val target = requireNotNull(repository.createPage())
            val retired = requireNotNull(repository.createPage())
            val id = folders.insert(FolderEntity(title = "Tools", cellIndex = 0, pageId = source))
            val folder = Folder(id, "Tools", 0, source)
            val pin = LauncherApp(AppKey("qa.folder.pin", "Main"), "Pin", null)
            val choices = requireNotNull(repository.folderPageMoveOptions(folder))
            assertEquals(listOf(target, retired), choices.pages.map { it.id })
            assertTrue(repository.removeEmptyPage(retired))
            for (cell in 0 until GridCanvas.CAPACITY) {
                assertTrue(repository.insertWorkspaceAt(pin, cell, target))
            }
            val snapshot = folders.getAll()
            assertEquals(PageMoveResult.FULL, repository.moveFolderToPage(folder, target))
            assertEquals(PageMoveResult.UNAVAILABLE, repository.moveFolderToPage(folder, retired))
            assertEquals(snapshot, folders.getAll())

            repository.removeFromWorkspace(pin, cell = 0, pageId = target)
            assertEquals(PageMoveResult.MOVED, repository.moveFolderToPage(folder, target))
            val moved = folders.getAll()
            val anotherPage = requireNotNull(repository.createPage())
            assertNull(repository.folderPageMoveOptions(folder))
            assertEquals(PageMoveResult.UNAVAILABLE, repository.moveFolderToPage(folder, anotherPage))
            assertEquals(moved, folders.getAll())
        } finally {
            db.close()
        }
    }

    @Test
    fun folderDragChangesOnlyTheFolderCellAndRejectsOccupiedOrStaleDrops() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            val items = db.workspaceItemDao()
            val folders = db.folderDao()
            val repository = WorkspaceRepository(
                items, db, folders, context.getSharedPreferences("qa_workspace_page_moves", Context.MODE_PRIVATE),
            )
            val page = requireNotNull(repository.createPage())
            val one = LauncherApp(AppKey("qa.folder.one", "Main"), "One", null)
            val two = LauncherApp(AppKey("qa.folder.two", "Main"), "Two", null)
            assertTrue(repository.insertWorkspaceAt(one, 0, page))
            assertTrue(repository.insertWorkspaceAt(two, 1, page))
            assertTrue(repository.createFolderFromDrop(
                two, 0, "Tools", WorkspaceRepository.FolderDropSource.Workspace(1), page,
            ))
            val source = folders.getAll().single()
            val folder = Folder(source.id, source.title, source.cellIndex, page)
            assertTrue(repository.insertWorkspaceAt(one, 4, page))
            folders.insert(FolderEntity(title = "Tools", cellIndex = 5, pageId = page))
            assertTrue(repository.insertWorkspaceAt(two, 7, DEFAULT_WORKSPACE_PAGE_ID))
            val beforeItems = items.getAll().sortedBy { it.id }
            val beforeFolders = folders.getAll().sortedBy { it.id }

            for (invalid in listOf(-1, 0, 4, 5, GridCanvas.CAPACITY)) {
                assertFalse(repository.moveFolderWithinPage(folder, invalid))
            }
            assertEquals(beforeFolders, folders.getAll().sortedBy { it.id })
            assertTrue(repository.moveFolderWithinPage(folder, 7))
            assertFalse(repository.moveFolderWithinPage(folder, 6))
            assertEquals(
                beforeFolders.map { if (it.id == folder.id) it.copy(cellIndex = 7) else it },
                folders.getAll().sortedBy { it.id },
            )
            assertEquals(beforeItems, items.getAll().sortedBy { it.id })
        } finally {
            db.close()
        }
    }

    private class CountingDiscovery(@Volatile var apps: List<LauncherApp>) : AppDiscovery {
        var calls = 0
            private set

        override fun discover(): List<LauncherApp> {
            calls++
            return apps
        }

        override fun isPackageInstalled(packageName: String, userSerial: Long) = true
    }
}
