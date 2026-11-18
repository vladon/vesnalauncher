package dev.vladon.vesna.launcher.workspace

import android.content.SharedPreferences
import androidx.room.withTransaction
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.FolderDao
import dev.vladon.vesna.launcher.data.FolderEntity
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
    private val folderDao: FolderDao,
    private val prefs: SharedPreferences,
) {

    // Serializes read-modify-write operations so two quick actions (for example two
    // "add to dock" taps) cannot both compute placements from the same snapshot.
    private val mutationMutex = Mutex()

    val items: Flow<List<WorkspaceItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    val folders: Flow<List<Folder>> =
        folderDao.observeAll().map { list -> list.map { Folder(it.id, it.title, it.cellIndex) } }

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

    /** Places the app into the first free grid cell (folder cells are occupied). */
    suspend fun addToWorkspace(app: LauncherApp): Boolean = mutationMutex.withLock {
        val occupied = dao.getAll()
            .filter { it.container == Container.WORKSPACE.name && it.folderId == null }
            .mapTo(mutableSetOf()) { it.cellIndex }
        folderDao.getAll().mapTo(occupied) { it.cellIndex }
        val cell = GridCanvas.firstFreeCell(occupied) ?: return@withLock false
        dao.insert(WorkspaceItem(id = 0, key = app.key, container = Container.WORKSPACE, cellIndex = cell).toEntity())
        true
    }

    /**
     * Appends the app to the dock; false when the dock is full. Only top-level
     * dock entries count toward capacity and append order: rows that are folder
     * members (folderId set, container kept DOCK after a folder drag) are
     * invisible in the dock, so they must not consume slots — matching
     * [insertIntoDock], [moveWorkspaceToDock] and dock resolution.
     */
    suspend fun addToDock(app: LauncherApp): Boolean = mutationMutex.withLock {
        val dock = dao.getAll().filter { it.container == Container.DOCK.name && it.folderId == null }
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

    /** Drops persisted items that reference apps no longer installed, then dissolves sparse folders. */
    suspend fun pruneStale(liveKeys: Set<AppKey>) = mutationMutex.withLock {
        val stale = dao.getAll().filter { AppKey(it.packageName, it.className, it.userSerial) !in liveKeys }
        for (item in stale) dao.deleteById(item.id)
        stale.mapNotNull { it.folderId }.toSet().forEach { dissolveIfSparse(it) }
        normalizeFolders()
    }

    /** Dissolves folders that hold fewer than two members. */
    private suspend fun normalizeFolders() {
        for (folder in folderDao.getAll()) dissolveIfSparse(folder.id)
    }

    /**
     * Moves a workspace item to an EMPTY [toCell]. In 0.3.x this swapped with the
     * occupant; since 0.4.0 drops onto occupied cells create folders instead, so
     * this op only relocates. Returns false when the cell is occupied.
     */
    suspend fun moveWithinWorkspace(app: LauncherApp, toCell: Int): Boolean = mutationMutex.withLock {
        if (!GridCanvas.isValidCell(toCell)) return@withLock false
        val items = dao.getAll()
        val source = items.firstOrNull {
            it.container == Container.WORKSPACE.name &&
                it.folderId == null &&
                it.packageName == app.key.packageName &&
                it.className == app.key.className &&
                it.userSerial == app.key.userSerial
        } ?: return@withLock false
        if (source.cellIndex == toCell) return@withLock true
        if (items.any {
                it.container == Container.WORKSPACE.name && it.folderId == null && it.cellIndex == toCell
            }
        ) {
            return@withLock false
        }
        if (folderAtCell(toCell) != null) return@withLock false
        db.withTransaction {
            dao.updateCellIndex(source.id, toCell)
        }
        true
    }

    private suspend fun folderAtCell(cellIndex: Int): FolderEntity? =
        folderDao.getAll().firstOrNull { it.cellIndex == cellIndex }

    /**
     * Inserts a top-level app (for example dragged from the drawer) into an empty
     * workspace [cell]. Returns false when the cell is occupied by an app or folder.
     */
    suspend fun insertWorkspaceAt(app: LauncherApp, cell: Int): Boolean = mutationMutex.withLock {
        if (!GridCanvas.isValidCell(cell)) return@withLock false
        val items = dao.getAll()
        if (items.any {
                it.container == Container.WORKSPACE.name &&
                    it.folderId == null && it.cellIndex == cell
            }
        ) {
            return@withLock false
        }
        if (folderAtCell(cell) != null) return@withLock false
        dao.insert(
            WorkspaceItem(
                id = 0,
                key = app.key,
                container = Container.WORKSPACE,
                cellIndex = cell,
                userSerial = app.key.userSerial,
            ).toEntity(),
        )
        true
    }

    /**
     * Inserts a top-level app (for example dragged from the drawer) into the dock
     * at [slot]; dock entries at or after the slot shift right. Returns false when
     * the dock is full.
     */
    suspend fun insertIntoDock(app: LauncherApp, slot: Int): Boolean = mutationMutex.withLock {
        val dockIds = dao.getAll().filter { it.container == Container.DOCK.name && it.folderId == null }
            .sortedBy { it.cellIndex }
            .map { it.id }
        if (dockIds.size >= GridCanvas.DOCK_CAPACITY) return@withLock false
        val order = DockOrdering.insertAt(dockIds, 0L, slot)
        val newId = dao.insert(
            WorkspaceItem(
                id = 0,
                key = app.key,
                container = Container.DOCK,
                cellIndex = 0,
                userSerial = app.key.userSerial,
            ).toEntity(),
        )
        // Replace the placeholder id with the generated one and normalize slots.
        val finalOrder = order.map { if (it == 0L) newId else it }
        db.withTransaction {
            finalOrder.forEachIndexed { index, id -> dao.updatePlacement(id, Container.DOCK.name, index) }
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
                it.folderId == null &&
                it.packageName == app.key.packageName &&
                it.className == app.key.className &&
                it.userSerial == app.key.userSerial
        } ?: return@withLock false
        val dockIds = items.filter { it.container == Container.DOCK.name && it.folderId == null }
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
     * when the cell is occupied (the icon returns home). Only the top-level entry
     * moves: a dock row that is a hidden folder member never matches the source.
     */
    suspend fun moveDockToWorkspace(app: LauncherApp, cell: Int): Boolean = mutationMutex.withLock {
        if (!GridCanvas.isValidCell(cell)) return@withLock false
        val items = dao.getAll()
        val source = items.firstOrNull {
            it.container == Container.DOCK.name &&
                it.folderId == null &&
                it.packageName == app.key.packageName &&
                it.className == app.key.className &&
                it.userSerial == app.key.userSerial
        } ?: return@withLock false
        if (items.any {
                it.container == Container.WORKSPACE.name &&
                    it.folderId == null && it.cellIndex == cell
            }
        ) {
            return@withLock false
        }
        if (folderAtCell(cell) != null) return@withLock false
        db.withTransaction {
            dao.updatePlacement(source.id, Container.WORKSPACE.name, cell)
        }
        true
    }

    /** Reorders a dock entry to [slot] (dragging sideways inside the dock). */
    suspend fun reorderDock(app: LauncherApp, slot: Int): Boolean = mutationMutex.withLock {
        val items = dao.getAll()
        val dock = items.filter { it.container == Container.DOCK.name && it.folderId == null }
            .sortedBy { it.cellIndex }
        val from = dock.indexOfFirst {
            it.folderId == null &&
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

    // Folders -------------------------------------------------------------------

    /**
     * Creates a folder at [targetCell] from a drop of [dragged] onto the app that
     * currently occupies the cell. Both entries become folder members. Returns
     * false when either entry is missing or a folder already occupies the cell
     * (the caller should use [addToFolder] instead).
     */
    suspend fun createFolderFromDrop(
        dragged: LauncherApp,
        targetCell: Int,
        defaultTitle: String,
    ): Boolean = mutationMutex.withLock {
        val items = dao.getAll()
        val draggedItem = items.firstOrNull {
            it.folderId == null && it.packageName == dragged.key.packageName &&
                it.className == dragged.key.className && it.userSerial == dragged.key.userSerial
        } ?: return@withLock false
        val target = items.firstOrNull {
            it.container == Container.WORKSPACE.name && it.folderId == null && it.cellIndex == targetCell
        } ?: return@withLock false
        val foldersHere = folderDao.getAll().any { it.cellIndex == targetCell }
        if (foldersHere) return@withLock false
        val folderId = folderDao.insert(FolderEntity(title = defaultTitle, cellIndex = targetCell))
        db.withTransaction {
            dao.updateFolder(target.id, folderId)
            dao.updateFolder(draggedItem.id, folderId)
        }
        true
    }

    /** Adds a top-level app (workspace or dock) into an existing folder. */
    suspend fun addToFolder(app: LauncherApp, folderId: Long): Boolean = mutationMutex.withLock {
        if (folderDao.getById(folderId) == null) return@withLock false
        val items = dao.getAll()
        val entry = items.firstOrNull {
            it.folderId == null && it.packageName == app.key.packageName &&
                it.className == app.key.className && it.userSerial == app.key.userSerial
        } ?: return@withLock false
        dao.updateFolder(entry.id, folderId)
        true
    }

    /**
     * Returns a folder member to the first free grid cell and dissolves the folder
     * when fewer than two members remain. The folder's own cell counts as occupied
     * while it persists; returns false (member stays in the folder) when no cell
     * is free.
     */
    suspend fun removeFromFolder(app: LauncherApp): Boolean = mutationMutex.withLock {
        val items = dao.getAll()
        val entry = items.firstOrNull {
            it.folderId != null && it.packageName == app.key.packageName &&
                it.className == app.key.className && it.userSerial == app.key.userSerial
        } ?: return@withLock false
        val folder = folderDao.getById(entry.folderId!!) ?: return@withLock false
        val occupied = items.filter { it.folderId == null && it.container == Container.WORKSPACE.name }
            .mapTo(mutableSetOf()) { it.cellIndex }
        folderDao.getAll().mapTo(occupied) { it.cellIndex }
        val cell = GridCanvas.firstFreeCell(occupied) ?: return@withLock false
        db.withTransaction {
            dao.updatePlacement(entry.id, Container.WORKSPACE.name, cell)
            dao.updateFolder(entry.id, null)
        }
        dissolveIfSparse(folder.id)
        true
    }

    /** Renames a folder. Returns false when the folder does not exist. */
    suspend fun renameFolder(folderId: Long, title: String): Boolean = mutationMutex.withLock {
        val folder = folderDao.getById(folderId) ?: return@withLock false
        folderDao.update(folder.copy(title = title))
        true
    }

    /**
     * Removes a folder, returning every member to distinct free grid cells in
     * order. Returns false without mutating when the folder does not exist or the
     * grid cannot fit all members.
     */
    suspend fun removeFolder(folderId: Long): Boolean = mutationMutex.withLock {
        val folder = folderDao.getById(folderId) ?: return@withLock false
        val members = dao.getAll().filter { it.folderId == folderId }.sortedBy { it.cellIndex }
        val items = dao.getAll()
        val occupied = items.filter { it.folderId == null && it.container == Container.WORKSPACE.name }
            .mapTo(mutableSetOf()) { it.cellIndex }
        occupied += folderDao.getAll().filter { it.id != folder.id }.map { it.cellIndex }
        val freeCells = (0 until GridCanvas.CAPACITY).filter { it !in occupied }
        if (members.size > freeCells.size) return@withLock false
        db.withTransaction {
            folderDao.deleteById(folderId)
            for ((index, member) in members.withIndex()) {
                val cell = freeCells[index]
                dao.updatePlacement(member.id, Container.WORKSPACE.name, cell)
                dao.updateFolder(member.id, null)
            }
        }
        true
    }

    /**
     * Dissolves a folder that holds fewer than two members: the sole member returns
     * to the folder's grid cell, an empty folder is deleted.
     */
    private suspend fun dissolveIfSparse(folderId: Long) {
        val members = dao.getAll().filter { it.folderId == folderId }
        when (members.size) {
            0 -> folderDao.deleteById(folderId)
            1 -> {
                val only = members.first()
                val folder = folderDao.getById(folderId) ?: return
                dao.updatePlacement(only.id, Container.WORKSPACE.name, folder.cellIndex)
                dao.updateFolder(only.id, null)
                folderDao.deleteById(folderId)
            }
        }
    }

    private suspend fun removeFromContainer(key: AppKey, container: Container) = mutationMutex.withLock {
        dao.deleteTopLevelByKey(key.packageName, key.className, container.name, key.userSerial)
    }

    private companion object {
        const val KEY_SEEDED = "workspace_seeded"
    }
}
