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
import dev.vladon.vesna.launcher.settings.DockPreferences
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
 * Dock capacity (#115 stage 2): [DockPreferences.capacity] slots per page across
 * [DockPages.MAX_PAGES] pages bound every dock write, so a full two-page dock
 * refuses more pins while overflow flows onto the second page in order. Runs
 * against an isolated in-memory Room database and its own preference file.
 */
@RunWith(AndroidJUnit4::class)
class WorkspaceRepositoryDockCapacityTest {

    private lateinit var db: LauncherDatabase
    private lateinit var dao: WorkspaceItemDao
    private lateinit var repo: WorkspaceRepository
    private lateinit var dockPreferences: DockPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
            .addCallback(LauncherDatabase.DEFAULT_PAGE_SEED_CALLBACK)
            .allowMainThreadQueries()
            .build()
        val prefs = context.getSharedPreferences(
            "dock-capacity-qa-${UUID.randomUUID()}",
            Context.MODE_PRIVATE,
        )
        dao = db.workspaceItemDao()
        repo = WorkspaceRepository(dao, db, db.folderDao(), prefs)
        dockPreferences = DockPreferences(prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun app(pkg: String) = LauncherApp(AppKey(pkg, "$pkg.Activity"), pkg)

    private suspend fun dockSlots(): List<Int> =
        dao.getAll().filter { it.container == "DOCK" && it.folderId == null }.sortedBy { it.cellIndex }
            .map { it.cellIndex }

    @Test
    fun twoPagesBoundEveryDockInsert() = runBlocking {
        dockPreferences.capacity = 2
        repeat(DockPages.totalCapacity(2)) { i -> assertTrue(repo.addToDock(app("qa.cap$i"))) }
        assertFalse("A full second page refuses the next pin", repo.addToDock(app("qa.overflow")))
        assertEquals(DockPages.totalCapacity(2), dockSlots().size)
    }

    @Test
    fun defaultCapacityStillHoldsEightPins() = runBlocking {
        assertEquals(DockPages.DEFAULT_CAPACITY, dockPreferences.capacity)
        repeat(DockPages.totalCapacity(DockPages.DEFAULT_CAPACITY)) { i ->
            assertTrue(repo.addToDock(app("qa.default$i")))
        }
        assertFalse(repo.addToDock(app("qa.default-overflow")))
    }

    @Test
    fun insertIntoDockShiftsOverflowOntoTheSecondPage() = runBlocking {
        dockPreferences.capacity = 3
        repeat(4) { i -> assertTrue(repo.addToDock(app("qa.shift$i"))) }
        // The fourth pin waits on the second page; appending fills its free slot.
        assertTrue(repo.insertIntoDock(app("qa.appended"), 4))
        assertEquals(listOf(0, 1, 2, 3, 4), dockSlots())
        // Inserting at the front pushes every pin right across the page boundary.
        assertTrue(repo.insertIntoDock(app("qa.front"), 0))
        assertEquals(listOf(0, 1, 2, 3, 4, 5), dockSlots())
        assertFalse("Six pins fill two pages of three", repo.insertIntoDock(app("qa.full"), 0))
    }

    @Test
    fun workspaceToDockRefusesAFullDock() = runBlocking {
        dockPreferences.capacity = 2
        repeat(4) { i -> assertTrue(repo.addToDock(app("qa.wtd$i"))) }
        dao.insert(
            WorkspaceItemEntity(
                packageName = "qa.movable",
                className = "qa.movable.Activity",
                container = "WORKSPACE",
                cellIndex = 0,
            ),
        )
        assertEquals(
            DockMoveResult.FULL,
            repo.moveWorkspaceToDock(app("qa.movable"), fromCell = 0, slot = 1),
        )
        // With room on the second page the same drag moves instead of refusing.
        val lastPin = dao.getAll().last { it.container == "DOCK" && it.folderId == null }
        repo.removeFromDock(lastPin.id)
        assertEquals(
            DockMoveResult.MOVED,
            repo.moveWorkspaceToDock(app("qa.movable"), fromCell = 0, slot = 3),
        )
    }
}
