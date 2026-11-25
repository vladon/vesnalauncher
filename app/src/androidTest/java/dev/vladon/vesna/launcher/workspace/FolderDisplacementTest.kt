package dev.vladon.vesna.launcher.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.data.FolderEntity
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.data.WorkspaceItemEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * #169: a closed folder dropped onto an occupied cell displaces the single
 * top-level pin to the nearest free cell (the folder's vacated source counts
 * as free); with no free cell on the destination page the drop is rejected
 * whole. Runs against an isolated in-memory Room database.
 */
@RunWith(AndroidJUnit4::class)
class FolderDisplacementTest {

    private lateinit var db: LauncherDatabase
    private lateinit var repo: WorkspaceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK)
            .allowMainThreadQueries()
            .build()
        val prefs = context.getSharedPreferences(
            "folder-displace-qa-${java.util.UUID.randomUUID()}",
            Context.MODE_PRIVATE,
        )
        repo = WorkspaceRepository(db.workspaceItemDao(), db, db.folderDao(), prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun pin(pkg: String, cell: Int, pageId: Long) {
        db.workspaceItemDao().insert(
            WorkspaceItemEntity(
                packageName = pkg, className = "$pkg.Main",
                container = "WORKSPACE", cellIndex = cell, userSerial = 0L, pageId = pageId,
            ),
        )
    }

    private suspend fun folderAt(cell: Int, pageId: Long): Long =
        db.folderDao().insert(FolderEntity(title = "qa.folder", cellIndex = cell, pageId = pageId))

    private suspend fun pinsOnPage(pageId: Long): Map<Int, String> =
        db.workspaceItemDao().getAll()
            .filter { it.container == "WORKSPACE" && it.folderId == null && it.pageId == pageId }
            .associate { it.cellIndex to it.packageName }

    private suspend fun folderCell(id: Long): Pair<Long, Int> =
        db.folderDao().getById(id)!!.let { it.pageId!! to it.cellIndex }

    private suspend fun folderDomain(id: Long): Folder {
        val row = db.folderDao().getById(id)!!
        return Folder(row.id, row.title, row.cellIndex, row.pageId!!)
    }

    @Test
    fun folderDropDisplacesAppToNearestForwardCell() = runBlocking {
        val page = db.workspacePageDao().getAll().first().id
        val folderId = folderAt(0, page)
        pin("qa.app", 1, page)
        val grid = GridCanvas(4, 5)
        assertTrue(repo.moveFolderToCell(folderDomain(folderId), page, 1, grid))
        assertEquals("qa.app", pinsOnPage(page)[2])
        assertEquals(page to 1, folderCell(folderId))
    }

    @Test
    fun folderDropUsesVacatedSourceCellWhenItIsNearest() = runBlocking {
        val page = db.workspacePageDao().getAll().first().id
        val folderId = folderAt(0, page)
        // Everything forward of the target is occupied; the folder's own
        // source cell (0) is the only free slot for the displaced app.
        for (cell in 1..23) {
            if (cell != 3) pin("qa.filler$cell", cell, page)
        }
        pin("qa.displaced", 3, page)
        val grid = GridCanvas(4, 5)
        assertTrue(repo.moveFolderToCell(folderDomain(folderId), page, 3, grid))
        assertEquals("qa.displaced", pinsOnPage(page)[0])
        assertEquals(page to 3, folderCell(folderId))
    }

    @Test
    fun folderDropRejectedWhenDestinationPageHasNoFreeCell() = runBlocking {
        val pages = db.workspacePageDao().getAll()
        val pageA = pages[0].id
        val pageB = repo.createPage()!!
        // Page A: every cell occupied except the target (3); the folder sits on page B.
        for (cell in 0..19) {
            if (cell != 3) pin("qa.fill$cell", cell, pageA)
        }
        pin("qa.displaced", 3, pageA)
        val folderId = folderAt(0, pageB)
        val grid = GridCanvas(4, 5)
        assertFalse(repo.moveFolderToCell(folderDomain(folderId), pageA, 3, grid))
        // Nothing moved: the displaced pin is still on its cell, the folder on page B.
        assertEquals("qa.displaced", pinsOnPage(pageA)[3])
        assertEquals(pageB to 0, folderCell(folderId))
    }
}
