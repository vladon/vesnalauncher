package dev.vladon.vesna.launcher.workspace

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.FolderEntity
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.data.WorkspaceItemDao
import dev.vladon.vesna.launcher.data.WorkspaceItemEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Regression for VESNA-BUG-003: dock rows that are folder members (folderId set,
 * container kept DOCK after a folder drag) must not count toward
 * [WorkspaceRepository.addToDock] capacity or append order — the dock renders and
 * inserts top-level entries only, so a hidden member must not consume a slot.
 * Runs against an isolated in-memory Room database; the production database is
 * never touched.
 */
@RunWith(AndroidJUnit4::class)
class WorkspaceRepositoryDockFolderTest {

    private lateinit var db: LauncherDatabase
    private lateinit var dao: WorkspaceItemDao
    private lateinit var repo: WorkspaceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK)
            .allowMainThreadQueries()
            .build()
        val prefs = context.getSharedPreferences(
            "dock-folder-qa-${UUID.randomUUID()}",
            Context.MODE_PRIVATE,
        )
        dao = db.workspaceItemDao()
        repo = WorkspaceRepository(dao, db, db.folderDao(), prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun app(pkg: String) = LauncherApp(AppKey(pkg, "$pkg.Activity"), pkg)

    private suspend fun seedDockRow(pkg: String, cellIndex: Int, folderId: Long? = null) {
        dao.insert(
            WorkspaceItemEntity(
                packageName = pkg,
                className = "$pkg.Activity",
                container = "DOCK",
                cellIndex = cellIndex,
                folderId = folderId,
            ),
        )
    }

    /** Three visible dock icons plus a folder member must leave room to append at slot 3. */
    @Test
    fun folderMemberDoesNotConsumeCapacityOrAppendOrder() = runBlocking {
        val folderId = db.folderDao().insert(FolderEntity(title = "Folder", cellIndex = 14))
        seedDockRow("com.google.android.calendar", 0)
        seedDockRow("com.android.camera2", 1)
        seedDockRow("com.android.chrome", 2)
        // DeskClock was dragged into a workspace folder: its row keeps container
        // DOCK and the original slot, but it is no longer visible in the dock.
        seedDockRow("com.google.android.deskclock", 3, folderId)

        assertTrue(repo.addToDock(app("dev.vladon.qa.lifecyclefixture")))

        val dock = dao.getAll().filter { it.container == "DOCK" }
        val topLevel = dock.filter { it.folderId == null }.sortedBy { it.cellIndex }
        assertEquals(listOf(0, 1, 2, 3), topLevel.map { it.cellIndex })
        assertEquals(
            listOf(
                "com.google.android.calendar",
                "com.android.camera2",
                "com.android.chrome",
                "dev.vladon.qa.lifecyclefixture",
            ),
            topLevel.map { it.packageName },
        )
        // The folder-member row is preserved untouched.
        val clock = dock.first { it.packageName == "com.google.android.deskclock" }
        assertEquals(folderId, clock.folderId)
        assertEquals(3, clock.cellIndex)
    }

    /** A dock with both pages' top-level entries is genuinely full and rejects appends. */
    @Test
    fun fullDockStillRejectsAddToDock() = runBlocking {
        val folderId = db.folderDao().insert(FolderEntity(title = "Folder", cellIndex = 14))
        repeat(DockPages.totalCapacity(DockPages.DEFAULT_CAPACITY)) { index ->
            seedDockRow("dev.vladon.qa.dock$index", index)
        }
        // A hidden folder member never counts toward the two-page capacity.
        seedDockRow("com.google.android.deskclock", 8, folderId)

        assertFalse(repo.addToDock(app("com.other.app")))
        assertEquals(DockPages.totalCapacity(DockPages.DEFAULT_CAPACITY) + 1, dao.getAll().count { it.container == "DOCK" })
    }

    @Test
    fun dockInsertRollsBackTheNewPinAndSlotShiftsTogether() = runBlocking {
        seedDockRow("com.example.first", 0)
        seedDockRow("com.example.middle", 1)
        seedDockRow("com.example.last", 2)
        val incoming = app("com.example.incoming")
        val before = dao.getAll().sortedBy { it.id }
        val failpoint = "dock slot shift interrupted"
        val sqlite = db.openHelper.writableDatabase
        sqlite.execSQL(
            "CREATE TRIGGER fail_dock_slot_shift BEFORE UPDATE OF cellIndex ON workspace_items " +
                "WHEN OLD.packageName = 'com.example.last' AND NEW.cellIndex = 3 " +
                "BEGIN SELECT RAISE(ABORT, '$failpoint'); END",
        )
        val failure = try {
            repo.insertIntoDock(incoming, slot = 1)
            error("Expected the dock slot shift to abort")
        } catch (expected: SQLiteConstraintException) {
            expected
        }
        assertTrue(failure.message.orEmpty().contains(failpoint))
        assertEquals("A rejected drop must not leave a saved pin or shift other pins", before, dao.getAll().sortedBy { it.id })

        sqlite.execSQL("DROP TRIGGER fail_dock_slot_shift")
        assertTrue(repo.insertIntoDock(incoming, slot = 1))
        val dock = dao.getAll().sortedBy { it.cellIndex }
        assertEquals(listOf(0, 1, 2, 3), dock.map { it.cellIndex })
        assertEquals(listOf("com.example.first", incoming.packageName, "com.example.middle", "com.example.last"), dock.map { it.packageName })
        assertEquals(before.map { it.id }, dock.filterNot { it.packageName == incoming.packageName }.map { it.id })
    }

    /**
     * Regression for VESNA-BUG-006: when the same app has a hidden dock row that is
     * a folder member (its dock icon was dragged into a folder) plus a visible
     * top-level dock row (re-pinned from the drawer), a dock-to-workspace drag must
     * move the visible row. The hidden member is seeded first so the pre-fix
     * first-match lookup without a folderId filter picked it.
     */
    @Test
    fun moveDockToWorkspaceMovesVisibleRowNotHiddenFolderMember() = runBlocking {
        val folderId = db.folderDao().insert(FolderEntity(title = "Folder", cellIndex = 14))
        seedDockRow("com.google.android.deskclock", 3, folderId)
        seedDockRow("com.google.android.deskclock", 3)

        val rows = dao.getAll()
        val hidden = rows.single { it.folderId == folderId }
        val visible = rows.single { it.packageName == "com.google.android.deskclock" && it.folderId == null }
        assertTrue("hidden row must precede the visible row", hidden.id < visible.id)

        assertFalse(repo.moveDockToWorkspace(hidden.id, 15))
        assertTrue(repo.moveDockToWorkspace(visible.id, 15))

        val after = dao.getAll()
        val moved = after.single { it.id == visible.id }
        assertEquals("WORKSPACE", moved.container)
        assertEquals(15, moved.cellIndex)
        assertNull(moved.folderId)
        val member = after.single { it.id == hidden.id }
        assertEquals("DOCK", member.container)
        assertEquals(3, member.cellIndex)
        assertEquals(folderId, member.folderId)
        assertEquals(14, db.folderDao().getById(folderId)?.cellIndex)
    }

    @Test
    fun duplicateDockPinsReorderAndMoveByTheirOwnIds() = runBlocking {
        seedDockRow("com.example.contacts", 0)
        seedDockRow("com.example.calendar", 1)
        seedDockRow("com.example.contacts", 2)
        val before = dao.getAll().sortedBy { it.cellIndex }
        val (first, middle, dragged) = before

        assertTrue(repo.reorderDock(dragged.id, 0))
        assertEquals(
            listOf(dragged.id, first.id, middle.id),
            dao.getAll().sortedBy { it.cellIndex }.map { it.id },
        )
        assertTrue(repo.moveDockToWorkspace(dragged.id, 7))
        val after = dao.getAll().associateBy { it.id }
        assertEquals(dragged.copy(container = "WORKSPACE", cellIndex = 7, pageId = DEFAULT_WORKSPACE_PAGE_ID), after.getValue(dragged.id))
        assertEquals(first.copy(cellIndex = 1), after.getValue(first.id))
        assertFalse(repo.reorderDock(dragged.id, 1))
        assertFalse(repo.moveDockToWorkspace(dragged.id, 8))
        assertEquals(after, dao.getAll().associateBy { it.id })
    }
}
