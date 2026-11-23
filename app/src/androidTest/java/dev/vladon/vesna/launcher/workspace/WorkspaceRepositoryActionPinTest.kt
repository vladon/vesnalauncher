package dev.vladon.vesna.launcher.workspace

import android.content.Context
import androidx.core.content.edit
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.data.WorkspaceItemEntity
import dev.vladon.vesna.launcher.settings.WorkspacePreferences
import dev.vladon.vesna.launcher.workspace.WorkspaceRepository.FolderDropSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Data-layer contract for app-published shortcut (action) pins: first-free-cell
 * placement on the selected page under the mutation mutex and workspace lock,
 * dedup by publisher package + profile + shortcut ID across every container,
 * platform pin-set validation before the Room write (with rollback of the
 * previous platform set if the write fails), real `shortcutIds`
 * snapshots, stable per-pin renames, copy-label preservation, and action-aware
 * pruning/auto-pin.
 */
@RunWith(AndroidJUnit4::class)
class WorkspaceRepositoryActionPinTest {

    private val publisherPackage = "dev.vladon.qa.publisher"

    private val shareKey = AppKey(publisherPackage, "$publisherPackage.MainActivity", shortcutId = "share")
    private val share = LauncherApp(shareKey, label = "Share")

    private val openKey = AppKey(publisherPackage, "$publisherPackage.MainActivity", shortcutId = "open")
    private val open = LauncherApp(openKey, label = "Open")

    private lateinit var db: LauncherDatabase
    private lateinit var repository: WorkspaceRepository
    private lateinit var prefsName: String

    private val platformCalls = mutableListOf<Set<String>>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefsName = "action_pin_test_${System.nanoTime()}"
        db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK)
            .build()
        repository = WorkspaceRepository(
            db.workspaceItemDao(), db, db.folderDao(), context.getSharedPreferences(prefsName, Context.MODE_PRIVATE),
        )
    }

    @After
    fun tearDown() {
        db.close()
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit { clear() }
    }

    /** Records the desired platform set and answers [accept]. */
    private fun platform(accept: Boolean = true): suspend (Set<String>) -> Boolean = { desired ->
        platformCalls.add(desired)
        accept
    }

    private fun lockLayout() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit { putBoolean(WorkspacePreferences.LAYOUT_LOCKED, true) }
    }

    private suspend fun pinIds() = db.workspaceItemDao().getAll().map { it.shortcutId }

    // ------------------------------------------------------------------ pinning

    @Test
    fun pinActionWritesRowOnSelectedPageAndCachesPublisherLabel() = runBlocking {
        assertEquals(AppActionPinResult.PINNED, repository.pinAppAction(share, DEFAULT_WORKSPACE_PAGE_ID, platform()))

        val row = db.workspaceItemDao().getAll().single()
        assertEquals(shareKey.packageName, row.packageName)
        assertEquals(shareKey.shortcutId, row.shortcutId)
        assertEquals("The publisher label is cached for role-loss rendering", "Share", row.shortcutLabel)
        assertNull(row.customLabel)
        assertEquals("The first free cell of the selected page", 0, row.cellIndex)
        assertEquals(DEFAULT_WORKSPACE_PAGE_ID, row.pageId)
        assertEquals(Container.WORKSPACE.name, row.container)
        assertEquals(listOf(setOf("share")), platformCalls)
        assertEquals(setOf("share"), repository.shortcutIds(publisherPackage, userSerial = 0))
    }

    @Test
    fun pinActionDesiredSetSpansContainersAndProfilesAreSeparate() = runBlocking {
        val dao = db.workspaceItemDao()
        dao.insert(
            WorkspaceItemEntity(
                packageName = publisherPackage, className = shareKey.className,
                container = Container.DOCK.name, cellIndex = 0, shortcutId = "existing",
            ),
        )
        dao.insert(
            WorkspaceItemEntity(
                packageName = publisherPackage, className = shareKey.className, userSerial = 11,
                container = Container.WORKSPACE.name, cellIndex = 3, shortcutId = "other-profile",
            ),
        )

        assertEquals(AppActionPinResult.PINNED, repository.pinAppAction(share, DEFAULT_WORKSPACE_PAGE_ID, platform()))

        // Desired IDs include all same package/profile action IDs across
        // containers, but never another profile's.
        assertEquals(listOf(setOf("existing", "share")), platformCalls)
        assertEquals(setOf("existing", "share"), repository.shortcutIds(publisherPackage, userSerial = 0))
        assertEquals(setOf("other-profile"), repository.shortcutIds(publisherPackage, userSerial = 11))
    }

    @Test
    fun pinActionIsDedupedAcrossContainersWithoutTouchingPlatform() = runBlocking {
        val dao = db.workspaceItemDao()
        dao.insert(
            WorkspaceItemEntity(
                packageName = publisherPackage, className = shareKey.className,
                container = Container.DOCK.name, cellIndex = 1, shortcutId = "share", shortcutLabel = "Share",
            ),
        )
        dao.insert(
            WorkspaceItemEntity(
                packageName = publisherPackage, className = shareKey.className,
                container = Container.WORKSPACE.name, cellIndex = 4, folderId = 1, pageId = null,
                shortcutId = "open", shortcutLabel = "Open",
            ),
        )

        assertEquals(AppActionPinResult.ALREADY_PINNED, repository.pinAppAction(share, DEFAULT_WORKSPACE_PAGE_ID, platform()))
        assertEquals(AppActionPinResult.ALREADY_PINNED, repository.pinAppAction(open, DEFAULT_WORKSPACE_PAGE_ID, platform()))

        assertTrue("No platform contact and no Room row for a duplicate", platformCalls.isEmpty())
        assertEquals(listOf("share", "open"), pinIds())
    }

    @Test
    fun pinActionReportsFullPageWithoutTouchingPlatform() = runBlocking {
        val dao = db.workspaceItemDao()
        val capacity = GridCanvas.DEFAULT.capacity
        for (cell in 0 until capacity) {
            dao.insert(
                WorkspaceItemEntity(
                    packageName = "dev.vladon.qa.filler", className = "Filler$cell",
                    container = Container.WORKSPACE.name, cellIndex = cell, pageId = DEFAULT_WORKSPACE_PAGE_ID,
                ),
            )
        }

        assertEquals(AppActionPinResult.FULL, repository.pinAppAction(share, DEFAULT_WORKSPACE_PAGE_ID, platform()))
        assertTrue(platformCalls.isEmpty())
        assertEquals(capacity, dao.getAll().size)
    }

    @Test
    fun pinActionWritesNoRowWhenPlatformRejects() = runBlocking {
        assertEquals(
            AppActionPinResult.UNAVAILABLE,
            repository.pinAppAction(share, DEFAULT_WORKSPACE_PAGE_ID, platform(accept = false)),
        )
        assertEquals(listOf(setOf("share")), platformCalls)
        assertTrue(db.workspaceItemDao().getAll().isEmpty())
    }

    @Test
    fun pinActionRollsBackPlatformSetWhenRowWriteFails() = runBlocking {
        val dao = db.workspaceItemDao()
        dao.insert(
            WorkspaceItemEntity(
                packageName = publisherPackage, className = shareKey.className,
                container = Container.WORKSPACE.name, cellIndex = 2, shortcutId = "existing",
            ),
        )
        val sqlite = db.openHelper.writableDatabase
        sqlite.execSQL(
            "CREATE TRIGGER fail_action_insert BEFORE INSERT ON workspace_items " +
                "BEGIN SELECT RAISE(ABORT, 'action row write interrupted'); END",
        )
        try {
            assertEquals(
                AppActionPinResult.UNAVAILABLE,
                repository.pinAppAction(share, DEFAULT_WORKSPACE_PAGE_ID, platform()),
            )
        } finally {
            sqlite.execSQL("DROP TRIGGER fail_action_insert")
        }

        assertEquals(
            listOf(setOf("existing", "share"), setOf("existing")),
            platformCalls,
        )
        assertEquals(listOf("existing"), pinIds())
    }

    @Test
    fun pinActionUnavailableWhenLockedMissingPageOrNotAnAction() = runBlocking {
        lockLayout()
        assertEquals(AppActionPinResult.UNAVAILABLE, repository.pinAppAction(share, DEFAULT_WORKSPACE_PAGE_ID, platform()))

        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit { putBoolean(WorkspacePreferences.LAYOUT_LOCKED, false) }
        assertEquals(
            AppActionPinResult.UNAVAILABLE,
            repository.pinAppAction(share, pageId = 999, desiredIds = platform()),
        )

        val plainApp = LauncherApp(AppKey(publisherPackage, "$publisherPackage.MainActivity"), label = "Publisher")
        assertEquals(AppActionPinResult.UNAVAILABLE, repository.pinAppAction(plainApp, DEFAULT_WORKSPACE_PAGE_ID, platform()))

        assertTrue(platformCalls.isEmpty())
        assertTrue(db.workspaceItemDao().getAll().isEmpty())
    }

    // ------------------------------------------------------------------ renames

    @Test
    fun renameOverridesSinglePinOnlyAndClears() = runBlocking {
        val dao = db.workspaceItemDao()
        val appRow = WorkspaceItemEntity(
            packageName = publisherPackage, className = shareKey.className,
            container = Container.WORKSPACE.name, cellIndex = 0,
        )
        dao.insert(appRow)
        dao.insert(
            WorkspaceItemEntity(
                packageName = publisherPackage, className = shareKey.className,
                container = Container.WORKSPACE.name, cellIndex = 1,
                shortcutId = "share", shortcutLabel = "Share",
            ),
        )

        val actionPinId = repository.workspacePinId(shareKey, cell = 1, pageId = DEFAULT_WORKSPACE_PAGE_ID)!!
        val appPinId = repository.workspacePinId(shareKey.copy(shortcutId = null), cell = 0, pageId = DEFAULT_WORKSPACE_PAGE_ID)!!

        assertTrue(repository.renameWorkspacePin(actionPinId, cell = 1, pageId = DEFAULT_WORKSPACE_PAGE_ID, label = "Send it"))
        val renamed = dao.getAll().single { it.id == actionPinId }
        assertEquals("Send it", renamed.customLabel)
        assertEquals("Cached publisher label survives the override write", "Share", renamed.shortcutLabel)
        assertNull("The sibling app pin keeps its own (null) label", dao.getAll().single { it.id == appPinId }.customLabel)

        assertTrue(repository.renameWorkspacePin(actionPinId, cell = 1, pageId = DEFAULT_WORKSPACE_PAGE_ID, label = null))
        assertNull(dao.getAll().single { it.id == actionPinId }.customLabel)
    }

    @Test
    fun renameFailsAfterPinMovesOrWhenLocked() = runBlocking {
        repository.pinAppAction(share, DEFAULT_WORKSPACE_PAGE_ID, platform())
        val pinId = repository.workspacePinId(shareKey, cell = 0, pageId = DEFAULT_WORKSPACE_PAGE_ID)!!

        lockLayout()
        assertFalse(
            "A submitted or stale editor must not save after the lock",
            repository.renameWorkspacePin(pinId, cell = 0, pageId = DEFAULT_WORKSPACE_PAGE_ID, label = "Late"),
        )
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit { putBoolean(WorkspacePreferences.LAYOUT_LOCKED, false) }

        assertTrue(repository.moveWithinWorkspace(share, fromCell = 0, toCell = 5))
        assertFalse(repository.renameWorkspacePin(pinId, cell = 0, pageId = DEFAULT_WORKSPACE_PAGE_ID, label = "Stale"))
        assertTrue(
            repository.renameWorkspacePin(pinId, cell = 5, pageId = DEFAULT_WORKSPACE_PAGE_ID, label = "Fresh"),
        )
        val moved = db.workspaceItemDao().getAll().single()
        assertEquals("The override follows the pin through moves", "Fresh", moved.customLabel)
        assertEquals(5, moved.cellIndex)
        assertEquals(
            "The old cell no longer resolves the pin",
            null,
            repository.workspacePinId(shareKey, cell = 0, pageId = DEFAULT_WORKSPACE_PAGE_ID),
        )
        assertEquals(repository.workspacePinId(shareKey, cell = 5, pageId = DEFAULT_WORKSPACE_PAGE_ID), pinId)
    }

    // ------------------------------------------------------- lifecycle policies

    @Test
    fun autoPinDedupsOnlyOrdinaryAppRows() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(WorkspacePreferences.LAYOUT_LOCKED, false) }
        WorkspacePreferences(prefs).pinNewApps = true

        val dao = db.workspaceItemDao()
        dao.insert(
            WorkspaceItemEntity(
                packageName = publisherPackage, className = shareKey.className,
                container = Container.WORKSPACE.name, cellIndex = 3,
                shortcutId = "share", shortcutLabel = "Share",
            ),
        )

        repository.pinNewlyInstalledApp(share.copy(key = shareKey.copy(shortcutId = null), label = "Publisher"))

        val rows = dao.getAll()
        assertEquals("The saved action must not block the fresh app's auto-pin", 2, rows.size)
        assertEquals("The app takes the first free cell, not the action's cell", 0, rows.single { it.shortcutId == null }.cellIndex)

        dao.deleteByPackage(publisherPackage, userSerial = 0)
        dao.insert(
            WorkspaceItemEntity(
                packageName = publisherPackage, className = shareKey.className,
                container = Container.WORKSPACE.name, cellIndex = 4,
            ),
        )
        repository.pinNewlyInstalledApp(share.copy(key = shareKey.copy(shortcutId = null), label = "Publisher"))
        assertEquals("An existing ordinary app row still blocks the auto-pin", 1, dao.getAll().size)
    }

    @Test
    fun copiedActionPinsCacheTheirLabelWhileAppPinsStayBare() = runBlocking {
        val plainApp = LauncherApp(AppKey(publisherPackage, "$publisherPackage.MainActivity"), label = "Publisher")

        assertTrue(repository.addToWorkspace(share))
        assertTrue(repository.addToDock(share))
        assertTrue(repository.insertWorkspaceAt(open, cell = 7))
        assertTrue(repository.insertIntoDock(open, slot = 0))
        assertTrue(
            repository.createFolderFromDrop(
                dragged = share,
                targetCell = 7,
                defaultTitle = "Work",
                source = FolderDropSource.Drawer,
            ),
        )
        val folderId = db.folderDao().getAll().single().id
        assertTrue(repository.addToFolder(plainApp, folderId, FolderDropSource.Drawer))

        val rows = db.workspaceItemDao().getAll()
        val actionRows = rows.filter { it.shortcutId != null }
        val appRows = rows.filter { it.shortcutId == null }
        assertTrue(actionRows.isNotEmpty() && appRows.isNotEmpty())
        assertEquals("Every copied action row caches its publisher label", actionRows.size, actionRows.count { it.shortcutLabel == "Share" || it.shortcutLabel == "Open" })
        assertEquals("Ordinary app pins keep no cached label", 0, appRows.count { it.shortcutLabel != null })
    }

    @Test
    fun renamedActionCopyCachesCanonicalPublisherLabel() = runBlocking {
        // A rendered pin whose rename was already projected onto the app object.
        val renamedShare = share.withVisibleLabel("Send it")
        assertEquals("Send it", renamedShare.label)

        assertTrue(repository.addToDock(renamedShare))

        val row = db.workspaceItemDao().getAll().single()
        assertEquals("The canonical publisher name is cached, never the override", "Share", row.shortcutLabel)
    }

    @Test
    fun pruneKeepsActionPinsWhilePublisherIsInstalled() = runBlocking {
        val dao = db.workspaceItemDao()
        dao.insert(
            WorkspaceItemEntity(
                packageName = publisherPackage, className = shareKey.className,
                container = Container.WORKSPACE.name, cellIndex = 0,
                shortcutId = "share", shortcutLabel = "Share",
            ),
        )
        dao.insert(
            WorkspaceItemEntity(
                packageName = "dev.vladon.qa.gone", className = "Gone",
                container = Container.WORKSPACE.name, cellIndex = 1,
            ),
        )

        repository.pruneUninstalled(launchableKeys = emptySet()) { name, _ -> name == publisherPackage }

        val rows = dao.getAll()
        assertEquals("Temporarily missing action pins survive the prune", listOf("share"), rows.map { it.shortcutId })

        repository.pruneUninstalled(launchableKeys = emptySet()) { _, _ -> false }
        assertTrue("Uninstalling the publisher removes its action pins", dao.getAll().isEmpty())
        assertTrue(repository.shortcutIds(publisherPackage, userSerial = 0).isEmpty())
    }
}
