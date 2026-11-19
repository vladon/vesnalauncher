package dev.vladon.vesna.launcher.workspace

import android.content.Context
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

    /** A dock with four top-level entries is genuinely full and rejects appends. */
    @Test
    fun fullDockStillRejectsAddToDock() = runBlocking {
        val folderId = db.folderDao().insert(FolderEntity(title = "Folder", cellIndex = 14))
        seedDockRow("com.google.android.calendar", 0)
        seedDockRow("com.android.camera2", 1)
        seedDockRow("com.android.chrome", 2)
        seedDockRow("dev.vladon.qa.lifecyclefixture", 3)
        seedDockRow("com.google.android.deskclock", 3, folderId)

        assertFalse(repo.addToDock(app("com.other.app")))
        assertEquals(5, dao.getAll().count { it.container == "DOCK" })
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

        assertTrue(repo.moveDockToWorkspace(app("com.google.android.deskclock"), 15))

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
}
