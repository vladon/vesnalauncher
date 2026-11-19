package dev.vladon.vesna.launcher.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.data.FolderEntity
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.data.WorkspaceItemEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceRepositoryPackageLifecycleTest {
    @Test
    fun disabledPackageKeepsAllItsPinsAndOnlyUninstallDissolvesItsFolder() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java).build()
        try {
            val dao = db.workspaceItemDao()
            val folders = db.folderDao()
            val repository = WorkspaceRepository(
                dao, db, folders, context.getSharedPreferences("package_lifecycle_test", Context.MODE_PRIVATE),
            )
            val packageName = "dev.vladon.qa.disabled"
            val pin = WorkspaceItemEntity(
                packageName = packageName,
                className = "$packageName.MainActivity",
                container = Container.WORKSPACE.name,
                cellIndex = 6,
            )
            val folder = FolderEntity(title = "Pinned apps", cellIndex = 14)
            val folderId = folders.insert(folder)
            listOf(
                pin,
                pin.copy(container = Container.DOCK.name, cellIndex = 2),
                pin.copy(folderId = folderId, cellIndex = 0),
                pin.copy(userSerial = 10, cellIndex = 5),
                pin.copy(packageName = "dev.vladon.qa.other", className = "Other", folderId = folderId, cellIndex = 1),
            ).forEach { dao.insert(it) }
            val before = dao.getAll()
            val liveKeys = setOf(AppKey("dev.vladon.qa.other", "Other"))

            repository.pruneUninstalled(liveKeys) { name, serial -> name == packageName && serial == 0L }

            assertEquals(before.filter { it.userSerial == 0L }, dao.getAll())
            assertEquals(folder.copy(id = folderId), folders.getById(folderId))

            repository.pruneUninstalled(liveKeys) { _, _ -> false }

            val sibling = before.single { it.packageName != packageName }
            assertEquals(listOf(sibling.copy(folderId = null, cellIndex = folder.cellIndex)), dao.getAll())
            assertNull(folders.getById(folderId))
        } finally {
            db.close()
        }
    }
}
