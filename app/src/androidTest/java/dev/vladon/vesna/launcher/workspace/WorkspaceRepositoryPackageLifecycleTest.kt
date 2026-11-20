package dev.vladon.vesna.launcher.workspace

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceRepositoryPackageLifecycleTest {
    @Test
    fun disabledPackageKeepsAllItsPinsAndOnlyUninstallDissolvesItsFolder() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK)
            .build()
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
                pin.copy(container = Container.DOCK.name, cellIndex = 2, pageId = null),
                pin.copy(folderId = folderId, cellIndex = 0, pageId = null),
                pin.copy(userSerial = 10, cellIndex = 5),
                pin.copy(packageName = "dev.vladon.qa.other", className = "Other", folderId = folderId, cellIndex = 1, pageId = null),
            ).forEach { dao.insert(it) }
            val before = dao.getAll()
            val liveKeys = setOf(AppKey("dev.vladon.qa.other", "Other"))

            repository.pruneUninstalled(liveKeys) { name, serial -> name == packageName && serial == 0L }

            assertEquals(before.filter { it.userSerial == 0L }, dao.getAll())
            assertEquals(folder.copy(id = folderId), folders.getById(folderId))

            repository.pruneUninstalled(liveKeys) { _, _ -> false }

            val sibling = before.single { it.packageName != packageName }
            assertEquals(
                listOf(sibling.copy(folderId = null, cellIndex = folder.cellIndex, pageId = DEFAULT_WORKSPACE_PAGE_ID)),
                dao.getAll(),
            )
            assertNull(folders.getById(folderId))
        } finally {
            db.close()
        }
    }

    @Test
    fun liveUninstallRollsBackPackagePinsWhenFolderDissolutionFails() = runBlocking {
        verifyCleanupRollback { repository, packageName, _ ->
            repository.removeAllForPackage(packageName, userSerial = 10)
        }
    }

    @Test
    fun startupPruneRollsBackPackagePinsWhenFolderDissolutionFails() = runBlocking {
        verifyCleanupRollback { repository, packageName, survivor ->
            repository.pruneUninstalled(setOf(survivor)) { name, serial ->
                name == packageName && serial == 0L
            }
        }
    }

    private suspend fun verifyCleanupRollback(
        cleanup: suspend (WorkspaceRepository, String, AppKey) -> Unit,
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK)
            .build()
        try {
            val dao = db.workspaceItemDao()
            val folders = db.folderDao()
            val repository = WorkspaceRepository(
                dao, db, folders, context.getSharedPreferences("package_lifecycle_test", Context.MODE_PRIVATE),
            )
            val pageId = requireNotNull(repository.createPage())
            val folder = FolderEntity(title = "Work tools", cellIndex = 14, pageId = pageId)
            val folderId = folders.insert(folder)
            val packageName = "dev.vladon.qa.bothprofiles"
            val pin = WorkspaceItemEntity(
                packageName = packageName, className = "Main", container = Container.WORKSPACE.name, cellIndex = 6,
            )
            val survivor = AppKey("dev.vladon.qa.survivor", "Main")
            listOf(
                pin,
                pin.copy(userSerial = 10, pageId = pageId),
                pin.copy(userSerial = 10, container = Container.DOCK.name, cellIndex = 2, pageId = null),
                pin.copy(userSerial = 10, folderId = folderId, cellIndex = 0, pageId = null),
                pin.copy(packageName = survivor.packageName, folderId = folderId, cellIndex = 1, pageId = null),
            ).forEach { dao.insert(it) }
            val before = dao.getAll().sortedBy { it.id }
            val sqlite = db.openHelper.writableDatabase
            val failpoint = "package cleanup folder delete interrupted"
            sqlite.execSQL(
                "CREATE TRIGGER fail_package_cleanup BEFORE DELETE ON folders WHEN OLD.id = $folderId " +
                    "BEGIN SELECT RAISE(ABORT, '$failpoint'); END",
            )

            val failure = runCatching { cleanup(repository, packageName, survivor) }.exceptionOrNull()
            assertEquals("Failed cleanup must leave all original pins and folder membership", before, dao.getAll().sortedBy { it.id })
            assertEquals(folder.copy(id = folderId), folders.getById(folderId))
            assertTrue("Expected the installed SQLite failpoint, got $failure", failure is SQLiteConstraintException &&
                failure.message.orEmpty().contains(failpoint))

            sqlite.execSQL("DROP TRIGGER fail_package_cleanup")
            cleanup(repository, packageName, survivor)
            val personalPin = before.single { it.packageName == packageName && it.userSerial == 0L }
            val remainingMember = before.single { it.packageName == survivor.packageName }
            assertEquals(
                listOf(personalPin, remainingMember.copy(folderId = null, cellIndex = folder.cellIndex, pageId = pageId)),
                dao.getAll().sortedBy { it.id },
            )
            assertNull(folders.getById(folderId))
        } finally {
            db.close()
        }
    }
}
