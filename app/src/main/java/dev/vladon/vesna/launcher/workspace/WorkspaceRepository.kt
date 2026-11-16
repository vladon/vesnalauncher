package dev.vladon.vesna.launcher.workspace

import androidx.room.withTransaction
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.data.WorkspaceItemDao
import dev.vladon.vesna.launcher.data.toDomain
import dev.vladon.vesna.launcher.data.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed store of the launcher layout: which apps sit in which workspace
 * cells and dock slots.
 */
class WorkspaceRepository(
    private val dao: WorkspaceItemDao,
    private val db: LauncherDatabase,
) {

    val items: Flow<List<WorkspaceItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun isEmpty(): Boolean = dao.getAll().isEmpty()

    /** Replaces the whole layout; used once for the initial seeding of a fresh install. */
    suspend fun seed(items: List<WorkspaceItem>) {
        db.withTransaction {
            dao.deleteAll()
            dao.insertAll(items.map { it.toEntity() })
        }
    }

    /** Places the app into the first free workspace cell; false when the grid is full. */
    suspend fun addToWorkspace(app: LauncherApp): Boolean {
        val occupied = dao.getAll()
            .filter { it.container == Container.WORKSPACE.name }
            .mapTo(mutableSetOf()) { it.cellIndex }
        val cell = GridCanvas.firstFreeCell(occupied) ?: return false
        dao.insert(WorkspaceItem(id = 0, key = app.key, container = Container.WORKSPACE, cellIndex = cell).toEntity())
        return true
    }

    /** Appends the app to the dock; false when the dock is full. */
    suspend fun addToDock(app: LauncherApp): Boolean {
        val dock = dao.getAll().filter { it.container == Container.DOCK.name }
        if (dock.size >= GridCanvas.DOCK_CAPACITY) return false
        val order = (dock.maxOfOrNull { it.cellIndex } ?: -1) + 1
        dao.insert(WorkspaceItem(id = 0, key = app.key, container = Container.DOCK, cellIndex = order).toEntity())
        return true
    }

    suspend fun removeFromWorkspace(app: LauncherApp) = removeFromContainer(app.key, Container.WORKSPACE)

    suspend fun removeFromDock(app: LauncherApp) = removeFromContainer(app.key, Container.DOCK)

    /** Removes every layout entry of an uninstalled package. */
    suspend fun removeAllForPackage(packageName: String) {
        dao.deleteByPackage(packageName)
    }

    /** Drops persisted items that reference apps no longer installed. */
    suspend fun pruneStale(liveKeys: Set<AppKey>) {
        val stale = dao.getAll().filter { AppKey(it.packageName, it.className) !in liveKeys }
        for (item in stale) dao.deleteById(item.id)
    }

    private suspend fun removeFromContainer(key: AppKey, container: Container) {
        dao.deleteByKey(key.packageName, key.className, container.name)
    }
}
