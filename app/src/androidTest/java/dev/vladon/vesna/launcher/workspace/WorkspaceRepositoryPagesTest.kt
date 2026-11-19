package dev.vladon.vesna.launcher.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.data.WorkspacePageEntity
import dev.vladon.vesna.launcher.data.toDomain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceRepositoryPagesTest {
    @Test
    fun pageOrderDoesNotChangePinIdentityOrCellOccupancy() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            val pages = db.workspacePageDao()
            val first = pages.getAll().single().id
            val second = pages.insert(WorkspacePageEntity(rank = 1))
            val repository = WorkspaceRepository(
                db.workspaceItemDao(), db, db.folderDao(),
                context.getSharedPreferences("qa_workspace_pages", Context.MODE_PRIVATE),
            )
            val app = LauncherApp(AppKey("qa.pin", "Main"), "Pin", null)
            val other = LauncherApp(AppKey("qa.other", "Main"), "Other", null)
            assertTrue(repository.insertWorkspaceAt(app, 0, first))
            assertTrue(repository.insertWorkspaceAt(app, 0, second))
            assertFalse(repository.insertWorkspaceAt(other, 0, second))
            assertFalse(repository.insertWorkspaceAt(other, 1, Long.MAX_VALUE))
            assertTrue(repository.insertWorkspaceAt(other, 1, first))
            assertTrue(repository.moveWithinWorkspace(app, 0, 1, second))
            val before = repository.items.first().sortedBy { it.id }

            pages.updateRank(second, -1)

            assertEquals(listOf(second, first), pages.getAll().map { it.id })
            assertEquals(before, repository.items.first().sortedBy { it.id })
            assertEquals(mapOf(0 to app, 1 to other), WorkspaceResolver.workspaceApps(before, listOf(app, other), first))
            assertEquals(mapOf(1 to app), WorkspaceResolver.workspaceApps(before, listOf(app, other), second))
            repository.removeFromWorkspace(app, second)
            assertEquals(before.filter { it.pageId == first }, repository.items.first().sortedBy { it.id })
        } finally {
            db.close()
        }
    }

    @Test
    fun folderDissolutionReturnsMembersToItsOwnPageAndDockStaysGlobal() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            val pages = db.workspacePageDao()
            val first = pages.getAll().single().id
            val second = pages.insert(WorkspacePageEntity(rank = 1))
            val dao = db.workspaceItemDao()
            val repository = WorkspaceRepository(
                dao, db, db.folderDao(), context.getSharedPreferences("qa_workspace_pages", Context.MODE_PRIVATE),
            )
            val target = LauncherApp(AppKey("qa.target", "Main"), "Target", null)
            val dock = LauncherApp(AppKey("qa.dock", "Main"), "Dock", null)
            val other = LauncherApp(AppKey("qa.other", "Main"), "Other", null)
            assertTrue(repository.insertWorkspaceAt(other, 0, first))
            assertTrue(repository.insertWorkspaceAt(target, 3, second))
            assertTrue(repository.insertIntoDock(dock, 0))
            assertTrue(repository.createFolderFromDrop(dock, 3, "Folder", second))
            val folder = db.folderDao().getAll().single()
            assertEquals(second, folder.pageId)
            assertFalse(repository.insertWorkspaceAt(other, 3, second))
            assertTrue(repository.insertWorkspaceAt(other, 3, first))
            val members = dao.getAll().filter { it.folderId == folder.id }
            assertEquals(setOf(target.packageName, dock.packageName), members.map { it.packageName }.toSet())
            assertTrue(members.all { it.pageId == null })

            assertTrue(repository.removeFromFolder(dock, folder.id))

            assertNull(db.folderDao().getById(folder.id))
            val items = dao.getAll().map { it.toDomain() }
            assertEquals(mapOf(0 to dock, 3 to target), WorkspaceResolver.workspaceApps(items, listOf(target, dock, other), second))
            assertEquals(DockMoveResult.MOVED, repository.moveWorkspaceToDock(dock, fromCell = 0, slot = 0, pageId = second))
            assertNull(dao.getAll().single { it.packageName == dock.packageName }.pageId)
            assertEquals(listOf(dock), WorkspaceResolver.dockApps(repository.items.first(), listOf(dock)))
        } finally {
            db.close()
        }
    }
}
