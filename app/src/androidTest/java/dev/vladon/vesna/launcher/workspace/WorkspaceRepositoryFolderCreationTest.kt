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
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK)
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
            repo.createFolderFromDrop(app("com.example.contacts"), 5, "Folder", WorkspaceRepository.FolderDropSource.Drawer),
        )

        assertEquals("VESNA-BUG-008: the sole pin must stay untouched", rowsBefore, dao.getAll())
        assertEquals("no folder may be created for a self-drop", foldersBefore, db.folderDao().getAll())
    }

    /** Positive control: a distinct pin dropped onto another still creates a two-member folder. */
    @Test
    fun dropOfDistinctRowOntoOccupiedCellStillCreatesTwoMemberFolder() = runBlocking {
        seedPin("com.example.contacts", cellIndex = 5)
        seedPin("com.example.calendar", cellIndex = 6)

        assertTrue(repo.createFolderFromDrop(app("com.example.calendar"), 5, "Folder", WorkspaceRepository.FolderDropSource.Workspace(6)))

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
                source = WorkspaceRepository.FolderDropSource.Workspace(9),
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

    @Test
    fun workspaceDropsUseTheSelectedDuplicateForNewAndExistingFolders() = runBlocking {
        val contacts = app("com.example.contacts")
        seedPin(contacts.packageName, cellIndex = 0)
        seedPin(contacts.packageName, cellIndex = 5)
        seedPin(contacts.packageName, cellIndex = 9)
        seedPin("com.example.calendar", cellIndex = 6)
        val before = dao.getAll().associateBy { it.cellIndex }

        assertFalse(repo.createFolderFromDrop(contacts, 6, "Folder", WorkspaceRepository.FolderDropSource.Workspace(7)))
        assertEquals(before, dao.getAll().associateBy { it.cellIndex })
        assertTrue(db.folderDao().getAll().isEmpty())
        assertTrue(repo.createFolderFromDrop(contacts, 6, "Folder", WorkspaceRepository.FolderDropSource.Workspace(5)))
        val folder = db.folderDao().getAll().single()
        val afterCreation = dao.getAll().associateBy { it.cellIndex }
        assertEquals(before.getValue(0), afterCreation.getValue(0))
        assertEquals(folder.id, afterCreation.getValue(5).folderId)
        assertEquals(folder.id, afterCreation.getValue(6).folderId)
        assertEquals(before.getValue(9), afterCreation.getValue(9))

        assertFalse(repo.addToFolder(contacts, folder.id, WorkspaceRepository.FolderDropSource.Workspace(5)))
        assertEquals(afterCreation, dao.getAll().associateBy { it.cellIndex })
        assertTrue(repo.addToFolder(contacts, folder.id, WorkspaceRepository.FolderDropSource.Workspace(9)))
        val afterExtension = dao.getAll().associateBy { it.cellIndex }
        assertEquals(before.getValue(0), afterExtension.getValue(0))
        assertEquals(before.getValue(9).copy(folderId = folder.id, pageId = null), afterExtension.getValue(9))
    }

    @Test
    fun drawerDropsCreateNewMembersWithoutRemovingPinsOrDuplicatingAFolderMember() = runBlocking {
        val drawer = WorkspaceRepository.FolderDropSource.Drawer
        val unpinned = app("com.example.unpinned")
        val pinned = app("com.example.pinned")
        seedPin("com.example.target", cellIndex = 6)
        seedPin(pinned.packageName, cellIndex = 11)
        assertTrue(repo.addToDock(pinned))
        val originalPins = dao.getAll().filter { it.packageName == pinned.packageName }

        assertTrue(repo.createFolderFromDrop(unpinned, 6, "Folder", drawer))
        val folder = db.folderDao().getAll().single()
        assertTrue(repo.addToFolder(pinned, folder.id, drawer))
        assertFalse(repo.addToFolder(pinned, folder.id, drawer))

        val all = dao.getAll()
        assertEquals(originalPins, all.filter { it.packageName == pinned.packageName && it.folderId == null })
        val members = all.filter { it.folderId == folder.id }.sortedBy { it.cellIndex }
        assertEquals(listOf("com.example.target", unpinned.packageName, pinned.packageName), members.map { it.packageName })
        assertTrue(members.all { it.pageId == null })
    }

    @Test
    fun dockDropDoesNotClaimAWorkspacePinOfTheSameApp() = runBlocking {
        val pinned = app("com.example.pinned")
        seedPin(pinned.packageName, cellIndex = 0)
        seedPin("com.example.target", cellIndex = 6)
        assertTrue(repo.addToDock(pinned))
        val workspacePin = dao.getAll().first { it.packageName == pinned.packageName && it.container == "WORKSPACE" }
        val originalDock = dao.getAll().single { it.container == Container.DOCK.name }
        assertTrue(repo.addToDock(pinned))
        val dockPin = dao.getAll().single { it.container == Container.DOCK.name && it.id != originalDock.id }

        assertTrue(repo.createFolderFromDrop(pinned, 6, "Folder", WorkspaceRepository.FolderDropSource.Dock(dockPin.id)))

        val folder = db.folderDao().getAll().single()
        val rows = dao.getAll().associateBy { it.id }
        assertEquals(workspacePin, rows.getValue(workspacePin.id))
        assertEquals(originalDock, rows.getValue(originalDock.id))
        assertEquals(folder.id, rows.getValue(dockPin.id).folderId)
    }
}
