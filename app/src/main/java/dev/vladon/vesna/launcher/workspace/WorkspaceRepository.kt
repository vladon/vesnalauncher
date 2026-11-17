package dev.vladon.vesna.launcher.workspace

import android.content.SharedPreferences
import androidx.room.withTransaction
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.data.WorkspaceItemDao
import dev.vladon.vesna.launcher.data.toDomain
import dev.vladon.vesna.launcher.data.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Room-backed store of the launcher layout: which apps sit in which workspace
 * cells and dock slots.
 */
class WorkspaceRepository(
    private val dao: WorkspaceItemDao,
    private val db: LauncherDatabase,
    private val prefs: SharedPreferences,
) {

    // Serializes read-modify-write operations so two quick actions (for example two
    // "add to dock" taps) cannot both compute placements from the same snapshot.
    private val mutationMutex = Mutex()

    val items: Flow<List<WorkspaceItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    /**
     * Whether the initial seeding already happened. Deliberately not derived from
     * table emptiness: a user who removed every icon must not get the default
     * layout restored by the next package event, and an upgrading install with an
     * existing layout must keep it.
     */
    fun isSeeded(): Boolean = prefs.getBoolean(KEY_SEEDED, false)

    /** Marks an existing (pre-flag) layout as adopted without touching it. */
    fun markSeeded() {
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    suspend fun isEmpty(): Boolean = dao.getAll().isEmpty()

    /** Replaces the whole layout; used once for the initial seeding of a fresh install. */
    suspend fun seed(items: List<WorkspaceItem>) = mutationMutex.withLock {
        db.withTransaction {
            dao.deleteAll()
            dao.insertAll(items.map { it.toEntity() })
        }
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    /** Places the app into the first free workspace cell; false when the grid is full. */
    suspend fun addToWorkspace(app: LauncherApp): Boolean = mutationMutex.withLock {
        val occupied = dao.getAll()
            .filter { it.container == Container.WORKSPACE.name }
            .mapTo(mutableSetOf()) { it.cellIndex }
        val cell = GridCanvas.firstFreeCell(occupied) ?: return@withLock false
        dao.insert(WorkspaceItem(id = 0, key = app.key, container = Container.WORKSPACE, cellIndex = cell).toEntity())
        true
    }

    /** Appends the app to the dock; false when the dock is full. */
    suspend fun addToDock(app: LauncherApp): Boolean = mutationMutex.withLock {
        val dock = dao.getAll().filter { it.container == Container.DOCK.name }
        if (dock.size >= GridCanvas.DOCK_CAPACITY) return@withLock false
        val order = (dock.maxOfOrNull { it.cellIndex } ?: -1) + 1
        dao.insert(WorkspaceItem(id = 0, key = app.key, container = Container.DOCK, cellIndex = order).toEntity())
        true
    }

    suspend fun removeFromWorkspace(app: LauncherApp) = removeFromContainer(app.key, Container.WORKSPACE)

    suspend fun removeFromDock(app: LauncherApp) = removeFromContainer(app.key, Container.DOCK)

    /** Removes every layout entry of an uninstalled package in one profile. */
    suspend fun removeAllForPackage(packageName: String, userSerial: Long) = mutationMutex.withLock {
        dao.deleteByPackage(packageName, userSerial)
    }

    /** Drops persisted items that reference apps no longer installed. */
    suspend fun pruneStale(liveKeys: Set<AppKey>) = mutationMutex.withLock {
        val stale = dao.getAll().filter { AppKey(it.packageName, it.className, it.userSerial) !in liveKeys }
        for (item in stale) dao.deleteById(item.id)
    }

    /**
     * Moves a workspace item to [toCell]; dropping it onto an occupied cell swaps
     * the two entries, so both icons stay on the grid.
     */
    suspend fun moveWithinWorkspace(app: LauncherApp, toCell: Int): Boolean = mutationMutex.withLock {
        if (!GridCanvas.isValidCell(toCell)) return@withLock false
        val items = dao.getAll()
        val source = items.firstOrNull {
            it.container == Container.WORKSPACE.name &&
                it.packageName == app.key.packageName &&
                it.className == app.key.className &&
                it.userSerial == app.key.userSerial
        } ?: return@withLock false
        if (source.cellIndex == toCell) return@withLock true
        val target = items.firstOrNull {
            it.container == Container.WORKSPACE.name && it.cellIndex == toCell
        }
        db.withTransaction {
            dao.updateCellIndex(source.id, toCell)
            target?.let { dao.updateCellIndex(it.id, source.cellIndex) }
        }
        true
    }

    /**
     * Drag-and-drop from the workspace into the dock at [slot]. Existing dock
     * entries at or after the slot shift right. Returns false when the dock is full.
     */
    suspend fun moveWorkspaceToDock(app: LauncherApp, slot: Int): Boolean = mutationMutex.withLock {
        val items = dao.getAll()
        val source = items.firstOrNull {
            it.container == Container.WORKSPACE.name &&
                it.packageName == app.key.packageName &&
                it.className == app.key.className &&
                it.userSerial == app.key.userSerial
        } ?: return@withLock false
        val dockIds = items.filter { it.container == Container.DOCK.name }
            .sortedBy { it.cellIndex }
            .map { it.id }
        if (dockIds.size >= GridCanvas.DOCK_CAPACITY) return@withLock false
        val order = DockOrdering.insertAt(dockIds, source.id, slot)
        db.withTransaction {
            order.forEachIndexed { index, id -> dao.updatePlacement(id, Container.DOCK.name, index) }
        }
        true
    }

    /**
     * Drag-and-drop from the dock into the workspace grid at [cell]. Returns false
     * when the cell is occupied (the icon returns home).
     */
    suspend fun moveDockToWorkspace(app: LauncherApp, cell: Int): Boolean = mutationMutex.withLock {
        if (!GridCanvas.isValidCell(cell)) return@withLock false
        val items = dao.getAll()
        val source = items.firstOrNull {
            it.container == Container.DOCK.name &&
                it.packageName == app.key.packageName &&
                it.className == app.key.className &&
                it.userSerial == app.key.userSerial
        } ?: return@withLock false
        if (items.any { it.container == Container.WORKSPACE.name && it.cellIndex == cell }) {
            return@withLock false
        }
        db.withTransaction {
            dao.updatePlacement(source.id, Container.WORKSPACE.name, cell)
        }
        true
    }

    /** Reorders a dock entry to [slot] (dragging sideways inside the dock). */
    suspend fun reorderDock(app: LauncherApp, slot: Int): Boolean = mutationMutex.withLock {
        val items = dao.getAll()
        val dock = items.filter { it.container == Container.DOCK.name }
            .sortedBy { it.cellIndex }
        val from = dock.indexOfFirst {
            it.packageName == app.key.packageName &&
                it.className == app.key.className &&
                it.userSerial == app.key.userSerial
        }
        if (from < 0) return@withLock false
        val order = DockOrdering.moveTo(dock.map { it.id }, from, slot)
        db.withTransaction {
            order.forEachIndexed { index, id -> dao.updateCellIndex(id, index) }
        }
        true
    }

    private suspend fun removeFromContainer(key: AppKey, container: Container) = mutationMutex.withLock {
        dao.deleteByKey(key.packageName, key.className, container.name, key.userSerial)
    }

    private companion object {
        const val KEY_SEEDED = "workspace_seeded"
    }
}
