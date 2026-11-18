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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * VESNA-BUG-005 regression: a menu-driven unpin ([WorkspaceRepository.removeFromDock] /
 * [WorkspaceRepository.removeFromWorkspace]) must delete ONLY the top-level
 * placement of the app. An app can additionally be a folder member carrying the
 * same key+container (a dock item dragged onto a workspace folder keeps its
 * DOCK container; see QA-003D), and that membership must survive the unpin.
 *
 * Both cases run against an in-memory Room database and an isolated
 * [WorkspaceRepository]; the production database is never touched. Unrelated
 * rows (other apps, other profiles, other containers, other folders) assert the
 * delete stays scoped to the key+container+profile of the top-level row.
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
        db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java).build()
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
        dao.insert(item(clock.key, Container.DOCK, cellIndex = 3, folderId = folderId))
        val topLevelPin = dao.insert(item(clock.key, Container.DOCK, cellIndex = 3))
        dao.insert(item(clock.key, Container.DOCK, cellIndex = 1, userSerial = 1)) // other profile

        repository.removeFromDock(clock)

        assertNull(dao.getAll().firstOrNull { it.id == topLevelPin })
        val member = dao.getAll().firstOrNull { it.packageName == clock.key.packageName && it.folderId != null }
        assertNotNull(member)
        assertEquals(Container.DOCK.name, member!!.container)
        assertEquals(3, member.cellIndex)
        assertEquals(folderId, member.folderId)
        assertEquals(1L, dao.getAll().firstOrNull { it.userSerial == 1L }!!.userSerial)
        assertTrue(dao.getAll().any { it.packageName == fixture.key.packageName && it.folderId == folderId })
        assertTrue(dao.getAll().any { it.container == Container.DOCK.name && it.folderId == null && it.cellIndex == 0 })
        assertTrue(dao.getAll().any { it.container == Container.WORKSPACE.name && it.folderId == null && it.cellIndex == 0 })
        // The folder itself is untouched: folder dissolution is not part of unpinning.
        assertNotNull(db.folderDao().getById(folderId))
    }

    @Test
    fun removeFromWorkspaceKeepsFolderMemberWithSameKeyAndContainer() = runBlocking {
        val dao = db.workspaceItemDao()
        val folderId = db.folderDao().insert(FolderEntity(title = "Folder", cellIndex = 9))
        dao.insert(item(clock.key, Container.DOCK, cellIndex = 2)) // unrelated dock entry
        dao.insert(item(contacts.key, Container.WORKSPACE, cellIndex = 7, folderId = folderId))
        val topLevel = dao.insert(item(contacts.key, Container.WORKSPACE, cellIndex = 0, userSerial = 0))

        repository.removeFromWorkspace(contacts)

        assertNull(dao.getAll().firstOrNull { it.id == topLevel })
        val member = dao.getAll().firstOrNull { it.packageName == contacts.key.packageName && it.folderId != null }
        assertNotNull(member)
        assertEquals(Container.WORKSPACE.name, member!!.container)
        assertEquals(7, member.cellIndex)
        assertEquals(folderId, member.folderId)
        assertTrue(dao.getAll().any { it.container == Container.DOCK.name && it.folderId == null && it.cellIndex == 2 })
        assertNotNull(db.folderDao().getById(folderId))
    }

    private companion object {
        const val PREFS_NAME = "workspace_repository_top_level_removal_test"
    }
}
