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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A menu-driven unpin removes only the selected placement. Other copies of
 * the app, including same-key folder memberships, are preserved.
 */
@RunWith(AndroidJUnit4::class)
class WorkspaceRepositoryTopLevelRemovalTest {

    private lateinit var db: LauncherDatabase
    private lateinit var repository: WorkspaceRepository

    private val clock = app("com.google.android.deskclock", "com.android.deskclock.DeskClock")
    private val calendar = app("com.google.android.calendar", "com.android.calendar.AllInOneActivity")
    private val fixture = app("dev.vladon.qa.lifecyclefixture", "dev.vladon.qa.lifecyclefixture.MainActivity")
    private val contacts = app("com.google.android.contacts", "com.android.contacts.activities.PeopleActivity")

    private fun app(pkg: String, cls: String) =
        LauncherApp(key = AppKey(pkg, cls), label = "label", icon = null)

    private fun item(
        key: AppKey,
        container: Container,
        cellIndex: Int,
        userSerial: Long = 0,
        folderId: Long? = null,
    ) = WorkspaceItemEntity(
        packageName = key.packageName,
        className = key.className,
        container = container.name,
        cellIndex = cellIndex,
        userSerial = userSerial,
        folderId = folderId,
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK)
            .build()
        repository = WorkspaceRepository(
            dao = db.workspaceItemDao(),
            db = db,
            folderDao = db.folderDao(),
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun removeFromDockKeepsFolderMemberWithSameKeyAndContainer() = runBlocking {
        val dao = db.workspaceItemDao()
        val folderId = db.folderDao().insert(FolderEntity(title = "Folder", cellIndex = 14))
        dao.insert(item(calendar.key, Container.DOCK, cellIndex = 0)) // unrelated dock entry
        dao.insert(item(contacts.key, Container.WORKSPACE, cellIndex = 0)) // unrelated grid entry
        dao.insert(item(fixture.key, Container.WORKSPACE, cellIndex = 14, folderId = folderId))
        val hiddenPin = dao.insert(item(clock.key, Container.DOCK, cellIndex = 3, folderId = folderId))
        dao.insert(item(clock.key, Container.DOCK, cellIndex = 2))
        val topLevelPin = dao.insert(item(clock.key, Container.DOCK, cellIndex = 3))
        dao.insert(item(clock.key, Container.DOCK, cellIndex = 1, userSerial = 1)) // other profile

        val before = dao.getAll()
        repository.removeFromDock(hiddenPin)
        repository.removeFromDock(topLevelPin)
        assertEquals(before.filterNot { it.id == topLevelPin }, dao.getAll())
        assertNotNull(db.folderDao().getById(folderId))
    }

    @Test
    fun removeFromWorkspaceKeepsFolderMemberWithSameKeyAndContainer() = runBlocking {
        val dao = db.workspaceItemDao()
        val folderId = db.folderDao().insert(FolderEntity(title = "Folder", cellIndex = 9))
        dao.insert(item(clock.key, Container.DOCK, cellIndex = 2)) // unrelated dock entry
        dao.insert(item(contacts.key, Container.WORKSPACE, cellIndex = 7, folderId = folderId))
        dao.insert(item(contacts.key, Container.WORKSPACE, cellIndex = 5))
        val topLevel = dao.insert(item(contacts.key, Container.WORKSPACE, cellIndex = 0, userSerial = 0))

        val before = dao.getAll()
        repository.removeFromWorkspace(contacts, cell = 7)
        repository.removeFromWorkspace(contacts, cell = 0)
        assertEquals(before.filterNot { it.id == topLevel }, dao.getAll())
        assertNotNull(db.folderDao().getById(folderId))
    }

    private companion object {
        const val PREFS_NAME = "workspace_repository_top_level_removal_test"
    }
}
