package dev.vladon.vesna.launcher.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.FolderEntity
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.data.toDomain
import dev.vladon.vesna.launcher.settings.WorkspacePreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewAppPlacementTest {
    @Test
    fun optInUsesFirstFreeCellOnSelectedPageAndRespectsLock() = withLayout { db, repo, prefs ->
        val page = requireNotNull(repo.createPage())
        assertTrue(repo.selectPage(page))
        db.folderDao().insert(FolderEntity(title = "Saved folder", cellIndex = 0, pageId = page))
        assertTrue(repo.insertWorkspaceAt(app("keep"), cell = 1, pageId = page))
        val before = db.workspaceItemDao().getAll()
        val newApp = app("new")

        repo.pinNewlyInstalledApp(newApp)
        assertEquals(before, db.workspaceItemDao().getAll())
        prefs.pinNewApps = true
        prefs.layoutLocked = true
        repo.pinNewlyInstalledApp(newApp)
        assertEquals(before, db.workspaceItemDao().getAll())

        prefs.layoutLocked = false
        repo.pinNewlyInstalledApp(newApp)
        val after = db.workspaceItemDao().getAll()
        val pin = after.single { it.packageName == newApp.key.packageName }.toDomain()
        assertEquals(WorkspaceItem(pin.id, newApp.key, Container.WORKSPACE, 2, pageId = page), pin)
        assertEquals(before, after.filterNot { it.id == pin.id })
        assertEquals("Saved folder", db.folderDao().getAll().single().title)
    }

    @Test
    fun fullSelectedPageDoesNotSpillToAnotherPageOrAlterLayout() = withLayout { db, repo, prefs ->
        val page = requireNotNull(repo.createPage())
        assertTrue(repo.selectPage(page))
        for (cell in 0 until GridCanvas.DEFAULT.capacity) {
            assertTrue(repo.insertWorkspaceAt(app("filled$cell"), cell = cell, pageId = page))
        }
        val before = db.workspaceItemDao().getAll().sortedBy { it.id }
        val pages = db.workspacePageDao().getAll()
        prefs.pinNewApps = true

        repo.pinNewlyInstalledApp(app("new"))

        assertEquals(before, db.workspaceItemDao().getAll().sortedBy { it.id })
        assertEquals(pages, db.workspacePageDao().getAll())
        assertEquals(page, repo.savedActivePageId())
    }

    @Test
    fun existingFolderOrDockPinSuppressesSameProfileButNotAnotherProfile() = withLayout { db, repo, prefs ->
        val dock = app("dock")
        val folder = app("folder")
        assertTrue(repo.insertIntoDock(dock, slot = 0))
        assertTrue(repo.insertWorkspaceAt(app("anchor"), cell = 0))
        assertTrue(repo.createFolderFromDrop(folder, 0, "Saved folder", WorkspaceRepository.FolderDropSource.Drawer))
        val page = requireNotNull(repo.createPage())
        assertTrue(repo.selectPage(page))
        val before = db.workspaceItemDao().getAll().sortedBy { it.id }
        prefs.pinNewApps = true

        repo.pinNewlyInstalledApp(dock.copy(key = dock.key.copy(className = "Secondary")))
        repo.pinNewlyInstalledApp(folder)
        val work = folder.copy(key = folder.key.copy(userSerial = 42))
        repo.pinNewlyInstalledApp(work)
        repo.pinNewlyInstalledApp(work)

        val after = db.workspaceItemDao().getAll().sortedBy { it.id }
        val pin = after.single { it.userSerial == 42L }.toDomain()
        assertEquals(WorkspaceItem(pin.id, work.key, Container.WORKSPACE, 0, userSerial = 42, pageId = page), pin)
        assertEquals(before, after.filterNot { it.id == pin.id })
    }

    private fun app(name: String) = LauncherApp(AppKey("qa.newplacement.$name", "Main"), name)

    private fun withLayout(
        block: suspend (LauncherDatabase, WorkspaceRepository, WorkspacePreferences) -> Unit,
    ) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsName = "qa_new_app_placement"
        context.deleteSharedPreferences(prefsName)
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK).build()
        try {
            block(db, WorkspaceRepository(db.workspaceItemDao(), db, db.folderDao(), prefs), WorkspacePreferences(prefs))
        } finally {
            db.close()
            context.deleteSharedPreferences(prefsName)
        }
    }
}
