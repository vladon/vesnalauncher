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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Regression for VESNA-BUG-007: the same app key may be a member of several
 * folders (a state the UI can produce by re-pinning an app and filing it again),
 * so "remove from folder" must target the membership of the folder the user
 * tapped, not the first matching row across all folders. Removing the tapped
 * member from the selected folder must keep every other folder's placements
 * exactly equal, return the tapped member to the first free grid cell, and
 * dissolve only the selected folder, returning its remaining member to the
 * folder's cell. Runs against an isolated in-memory Room database; the
 * production database is never touched.
 */
@RunWith(AndroidJUnit4::class)
class WorkspaceRepositoryFolderRemovalTest {

    private lateinit var db: LauncherDatabase
    private lateinit var dao: WorkspaceItemDao
    private lateinit var repo: WorkspaceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK)
            .build()
        val prefs = context.getSharedPreferences(
            "folder-removal-qa-${UUID.randomUUID()}",
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

    /** Seeds a WORKSPACE folder member; [cellIndex] is its stored placement. */
    private suspend fun seedMember(pkg: String, folderId: Long, cellIndex: Int) {
        dao.insert(
            WorkspaceItemEntity(
                packageName = pkg,
                className = "$pkg.Activity",
                container = "WORKSPACE",
                cellIndex = cellIndex,
                folderId = folderId,
            ),
        )
    }

    /**
     * Both folders hold two members and contain the same app: folder A has the
     * duplicate plus calendar, folder B the duplicate plus camera. Removing the
     * duplicate from the selected folder B must free only B's duplicate row, send
     * B's other member back to B's cell when B dissolves, and leave folder A with
     * both of its placements untouched. With the legacy lookup that ignores the
     * folder scope, the first matching membership wins — A's duplicate row is
     * seeded first, so the pre-fix behavior moved A's row, sent A's other member
     * home and dissolved folder A instead of B.
     */
    @Test
    fun removalFromSelectedFolderMovesOnlyItsMemberAndDissolvesOnlyIt() = runBlocking {
        val folderA = db.folderDao().insert(FolderEntity(title = "Alpha", cellIndex = 10))
        val folderB = db.folderDao().insert(FolderEntity(title = "Beta", cellIndex = 11))
        // Folder A's duplicate is seeded FIRST so the legacy cross-folder
        // first-match lookup picks it instead of the tapped folder B's row.
        seedMember("com.google.android.deskclock", folderA, cellIndex = 1)
        seedMember("com.google.android.calendar", folderA, cellIndex = 2)
        seedMember("com.google.android.deskclock", folderB, cellIndex = 3)
        seedMember("com.android.camera2", folderB, cellIndex = 4)
        val rowsBefore = dao.getAll()
        val rowADup = rowsBefore.single { it.folderId == folderA && it.packageName == "com.google.android.deskclock" }
        val rowACal = rowsBefore.single { it.folderId == folderA && it.packageName == "com.google.android.calendar" }
        val rowBDup = rowsBefore.single { it.folderId == folderB && it.packageName == "com.google.android.deskclock" }
        val rowBCam = rowsBefore.single { it.folderId == folderB && it.packageName == "com.android.camera2" }

        assertTrue(repo.removeFromFolder(app("com.google.android.deskclock"), folderB))

        val rowsAfter = dao.getAll()
        assertEquals(
            "VESNA-BUG-007: removing B's member must keep A's duplicate membership pinned",
            folderA,
            rowsAfter.single { it.id == rowADup.id }.folderId,
        )
        assertEquals("folder A keeps its first placement", rowADup, rowsAfter.single { it.id == rowADup.id })
        assertEquals("folder A keeps its second placement", rowACal, rowsAfter.single { it.id == rowACal.id })
        assertNotNull("folder A survives removing folder B's member", db.folderDao().getById(folderA))

        val freed = rowsAfter.single { it.id == rowBDup.id }
        assertEquals("the tapped member returns to the workspace", "WORKSPACE", freed.container)
        assertNull("the tapped member leaves folder B", freed.folderId)
        assertEquals("the tapped member lands in the first free grid cell", 0, freed.cellIndex)

        assertNull("folder B dissolves when left with one member", db.folderDao().getById(folderB))
        val returned = rowsAfter.single { it.id == rowBCam.id }
        assertEquals("B's remaining member returns to the workspace", "WORKSPACE", returned.container)
        assertNull(returned.folderId)
        assertEquals("B's remaining member returns to B's cell", 11, returned.cellIndex)
    }

    /**
     * Removing via a folder id that does not exist returns false and leaves every
     * row and folder untouched.
     */
    @Test
    fun removalFromMissingFolderReturnsFalseWithoutChanges() = runBlocking {
        val folderA = db.folderDao().insert(FolderEntity(title = "Alpha", cellIndex = 10))
        seedMember("com.google.android.deskclock", folderA, cellIndex = 1)
        seedMember("com.google.android.calendar", folderA, cellIndex = 2)
        val rowsBefore = dao.getAll()

        assertFalse(repo.removeFromFolder(app("com.google.android.deskclock"), folderId = 9999L))

        assertEquals("no row changed", rowsBefore, dao.getAll())
        assertNotNull("folder A survives", db.folderDao().getById(folderA))
    }

    /** VESNA-BUG-011: abort folder deletion and verify both memberships roll back. */
    @Test
    fun failedFolderDissolveRollsBackTheWholeRemovalAtomically() = runBlocking {
        val folderB = db.folderDao().insert(FolderEntity(title = "Beta", cellIndex = 10))
        seedMember("com.google.android.deskclock", folderB, cellIndex = 1)
        seedMember("com.google.android.calendar", folderB, cellIndex = 2)
        val rowsBefore = dao.getAll()
        val foldersBefore = db.folderDao().getAll()

        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER vesna_bug011_failpoint " +
                "BEFORE DELETE ON folders WHEN OLD.id = $folderB " +
                "BEGIN SELECT RAISE(ABORT, 'VESNA-BUG-011 failpoint: sparse folder deletion'); END",
        )

        val thrown: SQLiteConstraintException = try {
            repo.removeFromFolder(app("com.google.android.deskclock"), folderB)
            error("VESNA-BUG-011: the aborted folder deletion must surface to the caller")
        } catch (expected: SQLiteConstraintException) {
            expected
        }
        assertTrue(
            "VESNA-BUG-011: the failure must come from the installed failpoint trigger, got: $thrown",
            thrown.message.orEmpty().contains("VESNA-BUG-011 failpoint"),
        )

        assertEquals(
            "VESNA-BUG-011: the rolled-back removal must leave every placement untouched",
            rowsBefore,
            dao.getAll(),
        )
        assertEquals(
            "VESNA-BUG-011: the fixture folder must survive the failed dissolution",
            foldersBefore,
            db.folderDao().getAll(),
        )
    }
}
