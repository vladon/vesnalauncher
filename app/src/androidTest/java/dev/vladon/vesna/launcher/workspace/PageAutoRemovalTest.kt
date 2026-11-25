package dev.vladon.vesna.launcher.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
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
 * #176: after an item moves away from a page, the page drops when the move
 * emptied it. The default page and non-empty pages are kept, including the
 * only remaining page. Runs against an isolated in-memory Room database.
 */
@RunWith(AndroidJUnit4::class)
class PageAutoRemovalTest {

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
            "page-auto-removal-qa-${java.util.UUID.randomUUID()}",
            Context.MODE_PRIVATE,
        )
        repo = WorkspaceRepository(db.workspaceItemDao(), db, db.folderDao(), prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun pageIds(): List<Long> = db.workspacePageDao().getAll().map { it.id }

    private suspend fun insertPinAt(pkg: String, cell: Int, pageId: Long) {
        db.workspaceItemDao().insert(
            WorkspaceItemEntity(
                packageName = pkg, className = "$pkg.Main",
                container = "WORKSPACE", cellIndex = cell, userSerial = 0L, pageId = pageId,
            ),
        )
    }

    private suspend fun insertFolderAt(cell: Int, pageId: Long): Long =
        db.folderDao().insert(FolderEntity(title = "qa.folder", cellIndex = cell, pageId = pageId))

    @Test
    fun emptyingSourcePageByPageMoveDropsIt() = runBlocking {
        val source = repo.createPage()!!
        val destination = repo.createPage()!!
        insertPinAt("qa.solo", 2, source)
        val pinId = db.workspaceItemDao().getAll().single { it.packageName == "qa.solo" }.id
        assertEquals(PageMoveResult.MOVED, repo.movePinToPage(pinId, 2, source, destination))
        repo.autoRemoveIfEmptied(source)
        assertFalse("The emptied source page is dropped", pageIds().contains(source))
        assertTrue("The destination keeps the pin", pageIds().contains(destination))
    }

    @Test
    fun nonEmptyPageIsKept() = runBlocking {
        val source = repo.createPage()!!
        val destination = repo.createPage()!!
        insertPinAt("qa.keep1", 2, source)
        insertPinAt("qa.keep2", 5, source)
        val movedRow = db.workspaceItemDao().getAll().first { it.packageName == "qa.keep1" }
        assertEquals(PageMoveResult.MOVED, repo.movePinToPage(movedRow.id, 2, source, destination))
        repo.autoRemoveIfEmptied(source)
        assertTrue("The non-empty page survives", pageIds().contains(source))
    }

    @Test
    fun defaultPageIsNeverDropped() = runBlocking {
        val source = pageIds().first()
        insertPinAt("qa.solo", 2, source)
        val pinId = db.workspaceItemDao().getAll().single { it.packageName == "qa.solo" }.id
        db.workspaceItemDao().deleteById(pinId)
        repo.autoRemoveIfEmptied(source)
        assertTrue("The default page survives an emptying", pageIds().contains(source))
    }

    @Test
    fun onlyRemainingPageIsNeverDropped() = runBlocking {
        val source = pageIds().first()
        insertPinAt("qa.only", 6, source)
        val pinId = db.workspaceItemDao().getAll().single { it.packageName == "qa.only" }.id
        db.workspaceItemDao().deleteById(pinId)
        // The single remaining page survives even though it emptied.
        assertEquals(listOf(source), pageIds())
    }

    @Test
    fun crossPageFolderMoveDropsEmptiedSource() = runBlocking {
        val source = repo.createPage()!!
        val destination = repo.createPage()!!
        val folderId = insertFolderAt(1, source)
        insertPinAt("qa.member", 0, folderId)
        val folderRow = db.folderDao().getById(folderId)!!
        val folder = Folder(folderRow.id, folderRow.title, folderRow.cellIndex, folderRow.pageId!!)
        assertTrue(repo.moveFolderToCell(folder, destination, 12, GridCanvas(4, 5)))
        repo.autoRemoveIfEmptied(source)
        assertFalse("The emptied source page is dropped", pageIds().contains(source))
        val movedFolder = db.folderDao().getById(folderId)!!
        assertEquals(destination, movedFolder.pageId)
        assertEquals("qa.folder", movedFolder.title)
    }
}
