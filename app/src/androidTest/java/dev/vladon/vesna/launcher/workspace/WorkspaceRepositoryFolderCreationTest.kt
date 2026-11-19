package dev.vladon.vesna.launcher.workspace

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.data.WorkspaceItemDao
import dev.vladon.vesna.launcher.data.WorkspaceItemEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Regression for the drawer self-drop: dragging a drawer item onto the workspace
 * cell that holds the same app's only top-level pin routes through
 * [WorkspaceRepository.createFolderFromDrop], where the dragged-row lookup (app
 * key, any container) and the target-row lookup (workspace row at the target
 * cell) resolve to the SAME row. The legacy code then put that lone pin into a
 * freshly inserted folder as its single member, turning the pin into a
 * one-member folder. The repository must refuse before any mutation when the
 * resolved source and target row ids are identical, while a drop of one
 * distinct pin onto another still creates a two-member folder. Runs against an
 * isolated in-memory Room database; the production database is never touched.
 */
@RunWith(AndroidJUnit4::class)
class WorkspaceRepositoryFolderCreationTest {

    private lateinit var db: LauncherDatabase
    private lateinit var dao: WorkspaceItemDao
    private lateinit var repo: WorkspaceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .build()
        val prefs = context.getSharedPreferences(
            "folder-creation-qa-${UUID.randomUUID()}",
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

    /** Seeds a top-level WORKSPACE pin; [cellIndex] is its grid cell. */
    private suspend fun seedPin(pkg: String, cellIndex: Int) {
        dao.insert(
            WorkspaceItemEntity(
                packageName = pkg,
                className = "$pkg.Activity",
                container = "WORKSPACE",
                cellIndex = cellIndex,
            ),
        )
    }

    /**
     * The drawer item's only top-level row IS the drop target: the drop must
     * return false and leave the row and the folder table unchanged. With the
     * legacy lookups the same row is claimed by both the dragged and the target
     * arm, so a one-member folder appeared at the pin's cell.
     */
    @Test
    fun selfDropOntoSolePinReturnsFalseAndChangesNothing() = runBlocking {
        seedPin("com.example.contacts", cellIndex = 5)
        val rowsBefore = dao.getAll()
        val foldersBefore = db.folderDao().getAll()

        assertFalse(
            "VESNA-BUG-008: a drawer self-drop must be refused without mutating",
            repo.createFolderFromDrop(app("com.example.contacts"), targetCell = 5, defaultTitle = "Folder"),
        )

        assertEquals("VESNA-BUG-008: the sole pin must stay untouched", rowsBefore, dao.getAll())
        assertEquals("no folder may be created for a self-drop", foldersBefore, db.folderDao().getAll())
    }

    /** Positive control: a distinct pin dropped onto another still creates a two-member folder. */
    @Test
    fun dropOfDistinctRowOntoOccupiedCellStillCreatesTwoMemberFolder() = runBlocking {
        seedPin("com.example.contacts", cellIndex = 5)
        seedPin("com.example.calendar", cellIndex = 6)

        assertTrue(repo.createFolderFromDrop(app("com.example.calendar"), targetCell = 5, defaultTitle = "Folder"))

        val folder = db.folderDao().getAll().single { it.cellIndex == 5 }
        assertEquals(
            "both distinct pins become members of the one new folder",
            setOf("com.example.contacts", "com.example.calendar"),
            dao.getAll().mapNotNull { if (it.folderId == folder.id) it.packageName else null }.toSet(),
        )
    }

    /** VESNA-BUG-009: abort the second membership write and check rollback. */
    @Test
    fun failedMembershipUpdateLeavesNoCommittedFolder() = runBlocking {
        seedPin("com.example.contacts", cellIndex = 5)
        seedPin("com.example.browser", cellIndex = 9)
        val rowsBefore = dao.getAll()
        val foldersBefore = db.folderDao().getAll()

        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER vesna_bug009_failpoint " +
                "BEFORE UPDATE OF folderId ON workspace_items " +
                "WHEN NEW.packageName = 'com.example.browser' " +
                "BEGIN SELECT RAISE(ABORT, 'VESNA-BUG-009 failpoint: dragged membership update'); END",
        )

        val thrown: SQLiteConstraintException = try {
            repo.createFolderFromDrop(
                app("com.example.browser"),
                targetCell = 5,
                defaultTitle = "Folder",
            )
            error("VESNA-BUG-009: the aborted membership update must surface to the caller")
        } catch (expected: SQLiteConstraintException) {
            expected
        }
        assertTrue(
            "VESNA-BUG-009: the failure must come from the installed failpoint trigger, got: $thrown",
            thrown.message.orEmpty().contains("VESNA-BUG-009 failpoint"),
        )

        assertEquals(
            "VESNA-BUG-009: the aborted transaction must leave both placements untouched",
            rowsBefore,
            dao.getAll(),
        )
        assertEquals(
            "VESNA-BUG-009: no empty folder may be left behind by the failed drop",
            foldersBefore,
            db.folderDao().getAll(),
        )
    }
}
