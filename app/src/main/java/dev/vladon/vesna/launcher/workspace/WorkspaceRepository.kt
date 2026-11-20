package dev.vladon.vesna.launcher.workspace

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.withTransaction
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.FolderDao
import dev.vladon.vesna.launcher.data.FolderEntity
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.data.WorkspaceItemDao
import dev.vladon.vesna.launcher.data.WorkspaceItemEntity
import dev.vladon.vesna.launcher.data.WorkspacePageEntity
import dev.vladon.vesna.launcher.data.toDomain
import dev.vladon.vesna.launcher.data.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal enum class DockMoveResult { MOVED, SOURCE_MISSING, FULL }
internal enum class PageMoveResult { MOVED, FULL, UNAVAILABLE }

internal data class PageMoveTarget(val id: Long, val number: Int)
internal data class PageMoveOptions(val pinId: Long, val pages: List<PageMoveTarget>)

/** Hard cap on workspace pages; removing an empty extra page frees a slot. */
internal const val MAX_WORKSPACE_PAGES = 8

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
        folderDao.observeAll().map { list ->
            list.mapNotNull { row -> row.pageId?.let { Folder(row.id, row.title, row.cellIndex, it) } }
        }

    /** All pages in display order (rank, then id). */
    val pages: Flow<List<WorkspacePageEntity>> = db.workspacePageDao().observeAll()

    /**
     * Whether the initial seeding already happened. Deliberately not derived from
     * table emptiness: a user who removed every icon must not get the default
     * layout restored by the next package event, and an upgrading install with an
     * existing layout must keep it.
     */
    fun isSeeded(): Boolean = prefs.getBoolean(KEY_SEEDED, false)

    /**
     * Seeds a fresh install with [items], or adopts a pre-existing layout without
     * touching it. The emptiness check runs under [mutationMutex] together with
     * both outcomes, so a pin placed before the first seed completes can neither
     * be wiped by a racing seed nor adopted-as-is without the flag: the layout it
     * creates is kept and marked as seeded. Returns true when the layout was
     * seeded, false when an existing layout was adopted.
     */
    suspend fun seedOrAdopt(items: List<WorkspaceItem>): Boolean = mutationMutex.withLock {
        if (dao.getAll().isNotEmpty() || folderDao.getAll().isNotEmpty()) {
            prefs.edit { putBoolean(KEY_SEEDED, true) }
            return@withLock false
        }
        db.withTransaction {
            dao.insertAll(items.map { it.toEntity() })
        }
        prefs.edit { putBoolean(KEY_SEEDED, true) }
        true
    }

    // Pages ----------------------------------------------------------------------

    /**
     * Appends a new page after every existing one and returns its ID, or null when
     * [MAX_WORKSPACE_PAGES] pages already exist. The rank continues from the current
     * maximum, and the cap check runs before the insert under [mutationMutex], so
     * concurrent creations can neither exceed the cap nor produce equal ranks.
     */
    suspend fun createPage(): Long? = mutationMutex.withLock {
        val existing = db.workspacePageDao().getAll()
        if (existing.size >= MAX_WORKSPACE_PAGES) return@withLock null
        val nextRank = (existing.maxOfOrNull { it.rank } ?: -1) + 1
        db.workspacePageDao().insert(WorkspacePageEntity(rank = nextRank))
    }

    /** Removes an empty extra page without changing any other page or placement. */
    internal suspend fun removeEmptyPage(pageId: Long): Boolean = mutationMutex.withLock {
        if (pageId == DEFAULT_WORKSPACE_PAGE_ID || db.workspacePageDao().deleteIfEmpty(pageId) == 0) {
            return@withLock false
        }
        if (savedActivePageId() == pageId) {
            prefs.edit { remove(KEY_ACTIVE_PAGE) }
        }
        true
    }

    /** The page selected in the last session; the default page until one is stored. */
    fun savedActivePageId(): Long = prefs.getLong(KEY_ACTIVE_PAGE, DEFAULT_WORKSPACE_PAGE_ID)

    /**
     * Persists [pageId] as the selected page under its own preference key and
     * returns false when the page does not exist.
     */
    suspend fun selectPage(pageId: Long): Boolean = mutationMutex.withLock {
        if (db.workspacePageDao().getById(pageId) == null) return@withLock false
        prefs.edit { putLong(KEY_ACTIVE_PAGE, pageId) }
        true
    }

    /** Places the app into the first free grid cell (folder cells are occupied). */
    suspend fun addToWorkspace(
        app: LauncherApp,
        pageId: Long = DEFAULT_WORKSPACE_PAGE_ID,
    ): Boolean = mutationMutex.withLock {
        if (db.workspacePageDao().getById(pageId) == null) return@withLock false
        val occupied = occupiedCells(dao.getAll(), folderDao.getAll(), pageId)
        val cell = GridCanvas.firstFreeCell(occupied) ?: return@withLock false
        dao.insert(
            WorkspaceItem(id = 0, key = app.key, container = Container.WORKSPACE, cellIndex = cell, pageId = pageId)
                .toEntity(),
        )
        true
    }

    /** Reads the selected pin and destinations from the same layout snapshot. */
    internal suspend fun pageMoveOptions(key: AppKey, cell: Int, pageId: Long): PageMoveOptions? =
        mutationMutex.withLock {
            val items = dao.getAll()
            val source = workspacePinAt(items, key, pageId, cell) ?: return@withLock null
            val folders = folderDao.getAll()
            val pages = buildList {
                db.workspacePageDao().getAll().forEachIndexed { index, page ->
                    if (page.id != pageId &&
                        GridCanvas.firstFreeCell(occupiedCells(items, folders, page.id)) != null
                    ) add(PageMoveTarget(page.id, index + 1))
                }
            }
            PageMoveOptions(source.id, pages)
        }

    /** Moves the same pin into the first free cell, without changing the selected page. */
    internal suspend fun movePinToPage(pinId: Long, cell: Int, pageId: Long, destinationId: Long): PageMoveResult =
        mutationMutex.withLock {
            if (pageId == destinationId || db.workspacePageDao().getById(destinationId) == null) {
                return@withLock PageMoveResult.UNAVAILABLE
            }
            val items = dao.getAll()
            val source = items.firstOrNull { it.id == pinId && it.isOnPage(pageId) && it.cellIndex == cell }
                ?: return@withLock PageMoveResult.UNAVAILABLE
            val destinationCell = GridCanvas.firstFreeCell(occupiedCells(items, folderDao.getAll(), destinationId))
                ?: return@withLock PageMoveResult.FULL
            dao.updatePlacement(source.id, Container.WORKSPACE.name, destinationCell, destinationId)
            PageMoveResult.MOVED
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

    suspend fun removeFromWorkspace(app: LauncherApp, cell: Int, pageId: Long = DEFAULT_WORKSPACE_PAGE_ID) =
        mutationMutex.withLock {
            dao.deleteWorkspacePin(app.key.packageName, app.key.className, app.key.userSerial, pageId, cell)
        }

    suspend fun removeFromDock(pinId: Long) = mutationMutex.withLock { dao.deleteDockPin(pinId) }

    /** Removes every layout entry of an uninstalled package in one profile. */
    suspend fun removeAllForPackage(packageName: String, userSerial: Long) = mutationMutex.withLock {
        dao.deleteByPackage(packageName, userSerial)
    }

    /** Removes pins only for uninstalled packages; unavailable pins keep their positions. */
    suspend fun pruneUninstalled(
        launchableKeys: Set<AppKey>,
        isPackageInstalled: (String, Long) -> Boolean,
    ) = mutationMutex.withLock {
        val packageInstalled = HashMap<Pair<String, Long>, Boolean>()
        for (item in dao.getAll()) {
            if (AppKey(item.packageName, item.className, item.userSerial) in launchableKeys) continue
            val installed = packageInstalled.getOrPut(item.packageName to item.userSerial) {
                isPackageInstalled(item.packageName, item.userSerial)
            }
            if (!installed) dao.deleteById(item.id)
        }
        normalizeFolders()
    }

    /** Dissolves folders that hold fewer than two members. */
    private suspend fun normalizeFolders() {
        for (folder in folderDao.getAll()) dissolveIfSparse(folder.id)
    }

    /**
     * Moves the top-level pin for [app] from [fromCell] to an empty [toCell].
     * Returns false if the source is missing or the destination is invalid or
     * occupied. Dropping on the same cell succeeds without changing the pin.
     */
    suspend fun moveWithinWorkspace(
        app: LauncherApp,
        fromCell: Int,
        toCell: Int,
        pageId: Long = DEFAULT_WORKSPACE_PAGE_ID,
    ): Boolean = mutationMutex.withLock {
        if (!GridCanvas.isValidCell(toCell)) return@withLock false
        val items = dao.getAll()
        val source = workspacePinAt(items, app.key, pageId, fromCell) ?: return@withLock false
        if (source.cellIndex == toCell) return@withLock true
        if (cellIsOccupied(items, pageId, toCell)) return@withLock false
        db.withTransaction {
            dao.updateCellIndex(source.id, toCell)
        }
        true
    }

    private fun workspacePinAt(items: List<WorkspaceItemEntity>, key: AppKey, pageId: Long, cell: Int): WorkspaceItemEntity? =
        items.firstOrNull {
            it.isOnPage(pageId) && it.cellIndex == cell &&
                it.packageName == key.packageName && it.className == key.className && it.userSerial == key.userSerial
        }

    private fun WorkspaceItemEntity.isOnPage(pageId: Long): Boolean =
        this.pageId == pageId && folderId == null && container != Container.DOCK.name

    private suspend fun cellIsOccupied(items: List<WorkspaceItemEntity>, pageId: Long, cell: Int): Boolean =
        items.any { it.isOnPage(pageId) && it.cellIndex == cell } || folderAtCell(pageId, cell) != null

    private suspend fun folderAtCell(pageId: Long, cell: Int): FolderEntity? =
        folderDao.getAll().firstOrNull { it.pageId == pageId && it.cellIndex == cell }

    private fun occupiedCells(
        items: List<WorkspaceItemEntity>,
        folders: List<FolderEntity>,
        pageId: Long,
        excludedFolderId: Long? = null,
    ): Set<Int> = buildSet {
        for (item in items) if (item.isOnPage(pageId)) add(item.cellIndex)
        for (folder in folders) {
            if (folder.pageId == pageId && folder.id != excludedFolderId) add(folder.cellIndex)
        }
    }

    /**
     * Inserts a top-level app (for example dragged from the drawer) into an empty
     * workspace [cell]. Returns false when the cell is occupied by an app or folder.
     */
    suspend fun insertWorkspaceAt(
        app: LauncherApp,
        cell: Int,
        pageId: Long = DEFAULT_WORKSPACE_PAGE_ID,
    ): Boolean = mutationMutex.withLock {
        if (!GridCanvas.isValidCell(cell) || db.workspacePageDao().getById(pageId) == null) return@withLock false
        if (cellIsOccupied(dao.getAll(), pageId, cell)) return@withLock false
        dao.insert(
            WorkspaceItem(
                id = 0,
                key = app.key,
                container = Container.WORKSPACE,
                cellIndex = cell,
                userSerial = app.key.userSerial,
                pageId = pageId,
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
            finalOrder.forEachIndexed { index, id -> dao.updatePlacement(id, Container.DOCK.name, index, pageId = null) }
        }
        true
    }

    /**
     * Moves the pin at [fromCell] on [pageId] into dock [slot].
     * Existing dock entries at or after the slot shift right.
     */
    internal suspend fun moveWorkspaceToDock(
        app: LauncherApp,
        fromCell: Int,
        slot: Int,
        pageId: Long = DEFAULT_WORKSPACE_PAGE_ID,
    ): DockMoveResult = mutationMutex.withLock {
        val items = dao.getAll()
        val source = workspacePinAt(items, app.key, pageId, fromCell)
            ?: return@withLock DockMoveResult.SOURCE_MISSING
        val dockIds = items.filter { it.container == Container.DOCK.name && it.folderId == null }
            .sortedBy { it.cellIndex }
            .map { it.id }
        if (dockIds.size >= GridCanvas.DOCK_CAPACITY) return@withLock DockMoveResult.FULL
        val order = DockOrdering.insertAt(dockIds, source.id, slot)
        db.withTransaction {
            order.forEachIndexed { index, id -> dao.updatePlacement(id, Container.DOCK.name, index, pageId = null) }
        }
        DockMoveResult.MOVED
    }

    /**
     * Drag-and-drop from the dock into the workspace grid at [cell]. Returns false
     * when the cell is occupied (the icon returns home). Only the top-level entry
     * moves: a dock row that is a hidden folder member never matches the source.
     */
    suspend fun moveDockToWorkspace(
        pinId: Long,
        cell: Int,
        pageId: Long = DEFAULT_WORKSPACE_PAGE_ID,
    ): Boolean = mutationMutex.withLock {
        if (!GridCanvas.isValidCell(cell) || db.workspacePageDao().getById(pageId) == null) return@withLock false
        val items = dao.getAll()
        val source = items.firstOrNull { it.isDockPin(pinId) } ?: return@withLock false
        if (cellIsOccupied(items, pageId, cell)) return@withLock false
        db.withTransaction {
            dao.updatePlacement(source.id, Container.WORKSPACE.name, cell, pageId)
        }
        true
    }

    /** Reorders a dock entry to [slot] (dragging sideways inside the dock). */
    suspend fun reorderDock(pinId: Long, slot: Int): Boolean = mutationMutex.withLock {
        val items = dao.getAll()
        val dock = items.filter { it.container == Container.DOCK.name && it.folderId == null }
            .sortedBy { it.cellIndex }
        val from = dock.indexOfFirst { it.id == pinId }
        if (from < 0) return@withLock false
        val order = DockOrdering.moveTo(dock.map { it.id }, from, slot)
        db.withTransaction {
            order.forEachIndexed { index, id -> dao.updateCellIndex(id, index) }
        }
        true
    }

    // Folders -------------------------------------------------------------------

    internal sealed interface FolderDropSource {
        data class Workspace(val cell: Int) : FolderDropSource
        data class Dock(val pinId: Long) : FolderDropSource
        data object Drawer : FolderDropSource
    }

    /**
     * Creates a folder from an occupied cell and a dropped app. Drawer apps
     * add a new member; workspace and dock pins are moved, not copied.
     */
    internal suspend fun createFolderFromDrop(
        dragged: LauncherApp,
        targetCell: Int,
        defaultTitle: String,
        source: FolderDropSource,
        pageId: Long = DEFAULT_WORKSPACE_PAGE_ID,
    ): Boolean = mutationMutex.withLock {
        if (!GridCanvas.isValidCell(targetCell)) return@withLock false
        val items = dao.getAll()
        val target = items.firstOrNull {
            it.isOnPage(pageId) && it.cellIndex == targetCell
        } ?: return@withLock false
        val copying = source == FolderDropSource.Drawer
        val key = dragged.key
        val draggedItem = if (copying) {
            if (target.hasKey(key)) return@withLock false
            null
        } else {
            folderDropPin(items, key, pageId, source) ?: return@withLock false
        }
        if (draggedItem?.id == target.id || folderAtCell(pageId, targetCell) != null) return@withLock false
        // Roll back the folder row if either membership update fails.
        db.withTransaction {
            val folderId = folderDao.insert(FolderEntity(title = defaultTitle, cellIndex = targetCell, pageId = pageId))
            dao.updateFolder(target.id, folderId)
            if (draggedItem == null) insertFolderMember(key, folderId, targetCell + 1)
            else dao.updateFolder(draggedItem.id, folderId)
        }
        true
    }

    /** Adds a drawer copy or moves an existing top-level pin into a folder. */
    internal suspend fun addToFolder(app: LauncherApp, folderId: Long, source: FolderDropSource): Boolean =
        mutationMutex.withLock {
            val pageId = folderDao.getById(folderId)?.pageId ?: return@withLock false
            val items = dao.getAll()
            val key = app.key
            if (source == FolderDropSource.Drawer) {
                var lastPosition = -1
                for (item in items) {
                    if (item.folderId != folderId) continue
                    if (item.hasKey(key)) return@withLock false
                    lastPosition = maxOf(lastPosition, item.cellIndex)
                }
                insertFolderMember(key, folderId, lastPosition + 1)
            } else {
                val entry = folderDropPin(items, key, pageId, source) ?: return@withLock false
                dao.updateFolder(entry.id, folderId)
            }
            true
        }

    private suspend fun insertFolderMember(key: AppKey, folderId: Long, position: Int) {
        dao.insert(WorkspaceItem(0, key, Container.WORKSPACE, position, folderId = folderId).toEntity())
    }

    private fun folderDropPin(
        items: List<WorkspaceItemEntity>,
        key: AppKey,
        pageId: Long,
        source: FolderDropSource,
    ): WorkspaceItemEntity? = when (source) {
        is FolderDropSource.Workspace -> workspacePinAt(items, key, pageId, source.cell)
        is FolderDropSource.Dock -> items.firstOrNull { it.isDockPin(source.pinId) && it.hasKey(key) }
        FolderDropSource.Drawer -> null
    }

    private fun WorkspaceItemEntity.isDockPin(pinId: Long): Boolean =
        id == pinId && container == Container.DOCK.name && folderId == null

    private fun WorkspaceItemEntity.hasKey(key: AppKey): Boolean =
        packageName == key.packageName && className == key.className && userSerial == key.userSerial

    /**
     * Returns a member of [folderId] to the first free grid cell. If the folder
     * dissolves, its last member returns to the folder cell in the same transaction.
     * Returns false if the folder or membership is missing or the workspace is full.
     */
    suspend fun removeFromFolder(app: LauncherApp, folderId: Long): Boolean = mutationMutex.withLock {
        val items = dao.getAll()
        val entry = items.firstOrNull {
            it.folderId == folderId && it.packageName == app.key.packageName &&
                it.className == app.key.className && it.userSerial == app.key.userSerial
        } ?: return@withLock false
        val folder = folderDao.getById(folderId) ?: return@withLock false
        val pageId = folder.pageId ?: return@withLock false
        val occupied = occupiedCells(items, folderDao.getAll(), pageId)
        val cell = GridCanvas.firstFreeCell(occupied) ?: return@withLock false
        db.withTransaction {
            dao.updatePlacement(entry.id, Container.WORKSPACE.name, cell, pageId)
            dissolveIfSparse(folder.id)
        }
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
        val pageId = folder.pageId ?: return@withLock false
        val occupied = occupiedCells(dao.getAll(), folderDao.getAll(), pageId, excludedFolderId = folder.id)
        val freeCells = (0 until GridCanvas.CAPACITY).filter { it !in occupied }
        if (members.size > freeCells.size) return@withLock false
        db.withTransaction {
            folderDao.deleteById(folderId)
            for ((index, member) in members.withIndex()) {
                val cell = freeCells[index]
                dao.updatePlacement(member.id, Container.WORKSPACE.name, cell, pageId)
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
                val pageId = folder.pageId ?: return
                dao.updatePlacement(only.id, Container.WORKSPACE.name, folder.cellIndex, pageId)
                folderDao.deleteById(folderId)
            }
        }
    }

    private companion object {
        const val KEY_SEEDED = "workspace_seeded"
        const val KEY_ACTIVE_PAGE = "workspace_active_page"
    }
}
