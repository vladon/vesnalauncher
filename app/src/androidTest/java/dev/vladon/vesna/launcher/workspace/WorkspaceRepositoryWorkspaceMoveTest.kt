package dev.vladon.vesna.launcher.workspace

import android.content.Context
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
 * Regression for the duplicate-key workspace move: the drawer "Add to workspace"
 * action permits two top-level pins of the same app, and dragging the SECOND pin
 * to an empty cell must relocate only the dragged row. The legacy
 * [WorkspaceRepository.moveWithinWorkspace] resolved its source by app key alone
 * and always picked the FIRST matching row, so the earlier pin jumped to the
 * drop cell while the dragged pin stayed at its starting cell (VESNA-BUG-010,
 * reproduced live on the QA AVD). The source is now matched by container,
 * top-level membership, exact app key AND the drag's source cell; a stale or
 * missing source cell must refuse the move without falling back to another
 * same-key row. Runs against an isolated in-memory Room database; the
 * production database is never touched.
 */
@RunWith(AndroidJUnit4::class)
class WorkspaceRepositoryWorkspaceMoveTest {

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
            "workspace-move-qa-${UUID.randomUUID()}",
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
     * Two identical-key top-level pins at cells 0 (earlier row) and 5 (dragged
     * row): moving from the dragged row's cell must relocate exactly that row to
     * the drop cell and leave the earlier same-key pin at cell 0.
     */
    @Test
    fun duplicateKeyMoveRelocatesDraggedRowOnly() = runBlocking {
        seedPin("com.example.contacts", cellIndex = 0)
        seedPin("com.example.contacts", cellIndex = 5)
        val before = dao.getAll()
        assertEquals("setup: two top-level rows of the same app key", 2, before.size)
        val earlierId = before.first { it.cellIndex == 0 }.id
        val draggedId = before.first { it.cellIndex == 5 }.id

        assertTrue(
            "VESNA-BUG-010: the drag of the pin at cell 5 must move",
            repo.moveWithinWorkspace(app("com.example.contacts"), fromCell = 5, toCell = 9),
        )

        val after = dao.getAll().map { it.id to it.cellIndex }
        assertEquals(
            "VESNA-BUG-010: exactly the dragged row (cell 5) must sit at the drop cell 9; " +
                "the earlier same-key pin must stay at cell 0",
            listOf(earlierId to 0, draggedId to 9),
            after,
        )
    }

    /**
     * A source cell that holds no top-level row of the app (stale drag state)
     * must return false and change nothing: no fallback to another same-key row.
     */
    @Test
    fun staleSourceCellRefusesWithoutFallback() = runBlocking {
        seedPin("com.example.contacts", cellIndex = 0)
        seedPin("com.example.contacts", cellIndex = 5)
        val before = dao.getAll()

        assertFalse(
            "VESNA-BUG-010: a stale source cell must refuse the move",
            repo.moveWithinWorkspace(app("com.example.contacts"), fromCell = 7, toCell = 9),
        )

        assertEquals("VESNA-BUG-010: no row may move without a matched source cell", before, dao.getAll())
    }

    @Test
    fun workspaceToDockMovesOnlyTheSelectedDuplicate() = runBlocking {
        val contacts = app("com.example.contacts")
        seedPin(contacts.packageName, cellIndex = 0)
        seedPin(contacts.packageName, cellIndex = 5)
        val before = dao.getAll().associateBy { it.cellIndex }

        assertEquals(DockMoveResult.MOVED, repo.moveWorkspaceToDock(contacts, fromCell = 5, slot = 0))

        val after = dao.getAll().associateBy { it.id }
        val first = before.getValue(0)
        val dragged = before.getValue(5)
        assertEquals(first, after.getValue(first.id))
        assertEquals(dragged.copy(container = Container.DOCK.name, cellIndex = 0, pageId = null), after.getValue(dragged.id))
    }

    @Test
    fun workspaceToDockRejectsStaleSourceAndFullDockWithoutChangingLayout() = runBlocking {
        val contacts = app("com.example.contacts")
        seedPin(contacts.packageName, cellIndex = 0)
        repeat(GridCanvas.DOCK_CAPACITY) { assertTrue(repo.addToDock(app("com.example.dock$it"))) }
        val before = dao.getAll()

        assertEquals(DockMoveResult.SOURCE_MISSING, repo.moveWorkspaceToDock(contacts, fromCell = 5, slot = 0))
        assertEquals(DockMoveResult.FULL, repo.moveWorkspaceToDock(contacts, fromCell = 0, slot = 0))

        assertEquals(before, dao.getAll())
    }
}
