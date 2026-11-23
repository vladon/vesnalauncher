package dev.vladon.vesna.launcher.workspace

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.settings.WorkspacePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class WorkspaceGridResizeTest {
    @Test
    fun shrinkKeepsTheSamePinsAndFolderAndReusesAnExistingPageBeforeAppending() = withLayout { db, repo, prefs ->
        val second = checkNotNull(repo.createPage())
        assertTrue(repo.selectPage(second))
        for (cell in listOf(0, 1, 4, 5, 15, 17)) {
            assertTrue(repo.insertWorkspaceAt(app(cell), cell))
        }
        assertTrue(repo.createFolderFromDrop(app(99), 17, "Saved folder", WorkspaceRepository.FolderDropSource.Drawer))
        for (cell in listOf(0, 1, 4)) assertTrue(repo.insertWorkspaceAt(app(20 + cell), cell, second))
        assertTrue(repo.insertIntoDock(app(70), 0))
        val itemsBefore = db.workspaceItemDao().getAll().associateBy { it.id }
        val folderBefore = db.folderDao().getAll().single()

        assertEquals(GridResizeResult.APPLIED, repo.resizeGrid(GridCanvas(2, 2)))

        val saved = WorkspaceRepository(db.workspaceItemDao(), db, db.folderDao(), prefs).savedLayout.first()
        assertEquals(GridCanvas(2, 2), saved.grid)
        val first = DEFAULT_WORKSPACE_PAGE_ID
        val third = saved.pageIds.last()
        assertEquals(3, saved.pageIds.distinct().size)
        assertEquals(listOf(first, second), saved.pageIds.take(2))
        val itemsAfter = db.workspaceItemDao().getAll().associateBy { it.id }
        assertEquals(itemsBefore.keys, itemsAfter.keys)
        val expected = mapOf(0 to (first to 0), 1 to (first to 1), 4 to (first to 2), 5 to (first to 3),
            15 to (second to 3), 20 to (second to 0), 21 to (second to 1), 24 to (second to 2))
        for ((id, old) in itemsBefore) {
            val destination = expected[old.packageName.substringAfterLast('.').toInt()]
            val wanted = if (destination == null) old else old.copy(pageId = destination.first, cellIndex = destination.second)
            assertEquals(wanted, itemsAfter[id])
        }
        assertEquals(folderBefore.copy(pageId = third, cellIndex = 0), db.folderDao().getAll().single())
        assertEquals(second, repo.savedActivePageId())

        // The selected page is full and an index only valid in the old grid must not be accepted.
        WorkspacePreferences(prefs).pinNewApps = true
        repo.pinNewlyInstalledApp(app(98))
        assertFalse(repo.insertWorkspaceAt(app(98), 4, third))
        assertEquals(itemsAfter, db.workspaceItemDao().getAll().associateBy { it.id })
        assertTrue(repo.insertWorkspaceAt(app(98), 3, third))
    }

    @Test
    fun overCapacityAndLockedResizesLeaveEveryRowUntouched() = withLayout { db, repo, prefs ->
        val second = checkNotNull(repo.createPage())
        for (cell in 0 until 20) assertTrue(repo.insertWorkspaceAt(app(cell), cell))
        for (cell in 0 until 13) assertTrue(repo.insertWorkspaceAt(app(20 + cell), cell, second))
        val before = db.workspaceItemDao().getAll().associateBy { it.id }
        val pages = db.workspacePageDao().getAll()

        assertEquals(GridResizeResult.TOO_MANY_ITEMS, repo.resizeGrid(GridCanvas(2, 2)))
        WorkspacePreferences(prefs).layoutLocked = true
        assertEquals(GridResizeResult.LOCKED, repo.resizeGrid(GridCanvas(5, 6)))

        assertEquals(GridCanvas.DEFAULT, repo.grid.first())
        assertEquals(pages, db.workspacePageDao().getAll())
        assertEquals(before, db.workspaceItemDao().getAll().associateBy { it.id })
    }

    @Test
    fun aFailedFinalWriteRollsBackNewPagesAndRelocations() = withLayout { db, repo, _ ->
        for (cell in listOf(0, 1, 4, 5, 19)) assertTrue(repo.insertWorkspaceAt(app(cell), cell))
        val before = db.workspaceItemDao().getAll().associateBy { it.id }
        val pages = db.workspacePageDao().getAll()
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_grid_change BEFORE INSERT ON workspace_grid " +
                "BEGIN SELECT RAISE(ABORT, 'resize test failure'); END",
        )

        val failure = try {
            repo.resizeGrid(GridCanvas(2, 2))
            error("Expected the final grid write to fail")
        } catch (expected: SQLiteConstraintException) {
            expected
        }

        assertTrue(failure.message.orEmpty().contains("resize test failure"))
        assertEquals(GridCanvas.DEFAULT, repo.grid.first())
        assertEquals(pages, db.workspacePageDao().getAll())
        assertEquals(before, db.workspaceItemDao().getAll().associateBy { it.id })
    }

    private fun app(id: Int) = LauncherApp(AppKey("qa.resize.$id", "Main"), "App $id")

    private fun withLayout(
        block: suspend (LauncherDatabase, WorkspaceRepository, android.content.SharedPreferences) -> Unit,
    ) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsName = "qa_workspace_resize_${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            block(db, WorkspaceRepository(db.workspaceItemDao(), db, db.folderDao(), prefs), prefs)
        } finally {
            db.close()
            context.deleteSharedPreferences(prefsName)
        }
    }
}
