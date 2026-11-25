package dev.vladon.vesna.launcher.workspace

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.withTransaction
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.DockPinSnapshot
import dev.vladon.vesna.launcher.data.FolderDao
import dev.vladon.vesna.launcher.data.FolderEntity
import dev.vladon.vesna.launcher.data.FolderSnapshot
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.data.LayoutSnapshotException
import dev.vladon.vesna.launcher.data.LayoutSnapshotV1
import dev.vladon.vesna.launcher.data.PinSnapshot
import dev.vladon.vesna.launcher.data.WorkspaceGridEntity
import dev.vladon.vesna.launcher.data.WorkspaceItemDao
import dev.vladon.vesna.launcher.data.WorkspaceItemEntity
import dev.vladon.vesna.launcher.data.WorkspacePageEntity
import dev.vladon.vesna.launcher.data.toDomain
import dev.vladon.vesna.launcher.data.toEntity
import dev.vladon.vesna.launcher.settings.DockPreferences
import dev.vladon.vesna.launcher.settings.WorkspacePreferences
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal enum class DockMoveResult { MOVED, SOURCE_MISSING, FULL }
internal enum class PageMoveResult { MOVED, FULL, UNAVAILABLE }
internal enum class FolderRemovalResult { REMOVED, NO_ROOM, UNAVAILABLE }
internal enum class FolderRestoreResult { RESTORED, NO_ROOM, UNAVAILABLE, LOCKED }
internal enum class GridResizeResult { APPLIED, TOO_MANY_ITEMS, LOCKED }

/** Outcome of pinning an app-published action to a workspace page. */
enum class AppActionPinResult {
    /** The action was pinned to the platform and recorded in the layout. */
    PINNED,

    /** The same publisher action is already pinned in any container. */
    ALREADY_PINNED,

    /** The selected page has no free cell. */
    FULL,

    /** Locked layout, missing page, not an action, or the platform pin failed. */
    UNAVAILABLE,
}

internal data class PageMoveTarget(val id: Long, val number: Int)
internal data class PageMoveOptions(val sourceId: Long, val pages: List<PageMoveTarget>)

internal class RemovedFolder(val folder: FolderEntity, val members: List<WorkspaceItemEntity>)

internal class SavedLayout(
    val items: List<WorkspaceItem>,
    val folders: List<Folder>,
    val pageIds: List<Long>,
    val grid: GridCanvas,
)

/**
 * What a #116 layout restore would apply, shown for the explicit
 * confirmation before anything changes (ADR-BACKUP-1 part 1).
 */
internal data class LayoutRestorePreview(
    val snapshot: LayoutSnapshotV1,
    val pageCount: Int,
    val workspacePinCount: Int,
    val dockPinCount: Int,
    val folderCount: Int,
    val folderTitles: List<String>,
    val unavailablePackages: List<String>,
    val replacedPinCount: Int,
)

/** Outcome of opening a snapshot file for a #116 restore. */
internal sealed interface LayoutRestoreAttempt {
    /** Validated preview awaiting the explicit confirmation. */
    data class Ready(val preview: LayoutRestorePreview) : LayoutRestoreAttempt

    /** Rejected file; [reason] carries the validation failure for the dialog. */
    data class Rejected(val reason: String? = null) : LayoutRestoreAttempt
}

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
    private val workspacePreferences = WorkspacePreferences(prefs)
    private val gridDao = db.workspaceGridDao()

    /** Favorite slots per dock page; shared with Settings and Home through [DockPreferences]. */
    private val dockCapacityPerPage: Int
        get() = DockPreferences(prefs).capacity

    // Serializes read-modify-write operations so two quick actions (for example two
    // "add to dock" taps) cannot both compute placements from the same snapshot.
    private val mutationMutex = Mutex()

    /** Geometry, ordered page IDs and content describe the same transaction. */
    internal val savedLayout: Flow<SavedLayout> = db.invalidationTracker
        .createFlow("workspace_items", "folders", "workspace_pages", "workspace_grid")
        .map {
            db.withTransaction {
                SavedLayout(
                    items = dao.getAll().map { it.toDomain() },
                    folders = folderDao.getAll().mapNotNull { row ->
                        row.pageId?.let { Folder(row.id, row.title, row.cellIndex, it) }
                    },
                    pageIds = db.workspacePageDao().getAll().map { it.id },
                    grid = readGrid(),
                )
            }
        }

    internal val grid: Flow<GridCanvas> = gridDao.observe().map { it.toGrid() }.distinctUntilChanged()

    private suspend fun readGrid(): GridCanvas = gridDao.get().toGrid()

    private fun WorkspaceGridEntity?.toGrid(): GridCanvas =
        if (this == null) GridCanvas.DEFAULT else GridCanvas(columnCount, rowCount)

    /** Reflows top-level workspace cells only. Geometry and all writes commit together. */
    internal suspend fun resizeGrid(target: GridCanvas): GridResizeResult = mutationMutex.withLock {
        db.withTransaction {
            if (workspacePreferences.layoutLocked) return@withTransaction GridResizeResult.LOCKED
            val previous = readGrid()
            if (previous == target) return@withTransaction GridResizeResult.APPLIED
            val pageDao = db.workspacePageDao()
            val pages = db.workspacePageDao().getAll()
            val pageIndices = pages.withIndex().associate { (index, page) -> page.id to index }
            fun pageIndex(id: Long?): Int = if (id == null) -1 else pageIndices[id] ?: -1
            val placements = mutableListOf<GridPlacement>()
            for (item in dao.getAll()) {
                if (item.folderId == null && item.container != Container.DOCK.name) {
                    placements.add(GridPlacement(item.id, false, pageIndex(item.pageId), item.cellIndex))
                }
            }
            for (folder in folderDao.getAll()) {
                placements.add(GridPlacement(folder.id, true, pageIndex(folder.pageId), folder.cellIndex))
            }
            val neededPages = reflowWorkspaceGrid(previous, target, pages.size, placements)
                ?: return@withTransaction GridResizeResult.TOO_MANY_ITEMS
            if (workspacePreferences.layoutLocked) return@withTransaction GridResizeResult.LOCKED
            val pageIds = LongArray(neededPages)
            val lastRank = pages.lastOrNull()?.rank ?: -1
            for (index in pageIds.indices) {
                pageIds[index] = if (index < pages.size) pages[index].id else {
                    db.workspacePageDao().insert(WorkspacePageEntity(rank = lastRank + index - pages.lastIndex))
                }
            }
            for (placement in placements) {
                if (placement.page == placement.oldPage && placement.cell == placement.oldCell) continue
                val pageId = pageIds[placement.page]
                if (placement.folder) folderDao.updateWorkspacePosition(placement.id, placement.cell, pageId)
                else dao.updateWorkspacePosition(placement.id, placement.cell, pageId)
            }
            gridDao.set(WorkspaceGridEntity(columnCount = target.columns, rowCount = target.rows))
            GridResizeResult.APPLIED
        }
    }

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
    suspend fun seedOrAdopt(apps: List<LauncherApp>): Boolean = mutationMutex.withLock {
        if (dao.getAll().isNotEmpty() || folderDao.getAll().isNotEmpty()) {
            prefs.edit { putBoolean(KEY_SEEDED, true) }
            return@withLock false
        }
        db.withTransaction {
            dao.insertAll(SeedPlanner.plan(apps, readGrid()).map { it.toEntity() })
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
        insertPageAfter(existing.lastOrNull())
    }

    private suspend fun insertPageAfter(last: WorkspacePageEntity?): Long =
        db.workspacePageDao().insert(WorkspacePageEntity(rank = (last?.rank ?: -1) + 1))

    /** Removes an empty extra page without changing any other page or placement. */
    internal suspend fun removeEmptyPage(pageId: Long): Boolean = mutationMutex.withLock {
        if (!deleteEmptyExtraPage(pageId)) return@withLock false
        clearRemovedPageSelection(pageId)
        true
    }

    private suspend fun deleteEmptyExtraPage(pageId: Long): Boolean =
        pageId != DEFAULT_WORKSPACE_PAGE_ID && db.workspacePageDao().deleteIfEmpty(pageId) != 0

    private fun clearRemovedPageSelection(pageId: Long) {
        if (savedActivePageId() == pageId) {
            prefs.edit { remove(KEY_ACTIVE_PAGE) }
        }
    }

    /** Moves a page to its new display index, shifting the pages in between. */
    internal suspend fun reorderPage(pageId: Long, destinationIndex: Int, displayedOrder: List<Long>): Boolean =
        mutationMutex.withLock {
            val sourceIndex = displayedOrder.indexOf(pageId)
            if (sourceIndex <= 0 || pageId == DEFAULT_WORKSPACE_PAGE_ID ||
                destinationIndex !in 1..displayedOrder.lastIndex || destinationIndex == sourceIndex
            ) return@withLock false

            val pageDao = db.workspacePageDao()
            val pages = db.workspacePageDao().getAll()
            // The chooser names positions, so reject a choice from an older order.
            if (pages.size != displayedOrder.size || pages.indices.any { pages[it].id != displayedOrder[it] }) {
                return@withLock false
            }

            db.withTransaction {
                val step = if (destinationIndex > sourceIndex) 1 else -1
                var index = sourceIndex
                while (index != destinationIndex) {
                    val next = index + step
                    pageDao.updateRank(pages[next].id, pages[index].rank)
                    index = next
                }
                pageDao.updateRank(pageId, pages[destinationIndex].rank)
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
        val cell = readGrid().firstFreeCell(occupied) ?: return@withLock false
        insertWorkspaceApp(app, cell, pageId)
        true
    }

    /** Auto-pins only to the selected page, without moving or duplicating existing items. */
    internal suspend fun pinNewlyInstalledApp(app: LauncherApp): Unit = mutationMutex.withLock {
        if (!allowsAutomaticPlacement()) return@withLock
        val pageId = savedActivePageId()
        if (db.workspacePageDao().getById(pageId) == null) return@withLock
        val items = dao.getAll()
        // An ordinary app row blocks the auto-pin; the package's saved action
        // pins do not — the fresh app itself must still be placed.
        if (items.any {
                it.shortcutId == null && it.packageName == app.key.packageName && it.userSerial == app.key.userSerial
            }
        ) {
            return@withLock
        }
        val cell = readGrid().firstFreeCell(occupiedCells(items, folderDao.getAll(), pageId)) ?: return@withLock
        // Settings can change while Room reads are suspended.
        if (allowsAutomaticPlacement()) insertWorkspaceApp(app, cell, pageId)
    }

    private fun allowsAutomaticPlacement(): Boolean =
        workspacePreferences.pinNewApps && !workspacePreferences.layoutLocked

    private suspend fun insertWorkspaceApp(app: LauncherApp, cell: Int, pageId: Long) {
        dao.insert(pinEntity(app.key, Container.WORKSPACE, cell, pageId = pageId, publisherLabel = actionPinLabel(app)))
    }

    /**
     * The publisher label an action pin caches at creation time; ordinary app
     * pins keep no cached label. Reads the canonical [LauncherApp.publisherLabel]
     * so copying a renamed (already projected) pin persists the publisher's
     * name, never the user's visible override.
     */
    private fun actionPinLabel(app: LauncherApp): String? = if (app.key.shortcutId != null) app.publisherLabel else null

    private fun pinEntity(
        key: AppKey,
        container: Container,
        cellIndex: Int,
        pageId: Long? = null,
        folderId: Long? = null,
        publisherLabel: String? = null,
    ): WorkspaceItemEntity =
        WorkspaceItem(
            id = 0,
            key = key,
            container = container,
            cellIndex = cellIndex,
            folderId = folderId,
            pageId = pageId,
            shortcutLabel = publisherLabel,
        ).toEntity()

    /** Reads the selected pin and destinations from the same layout snapshot. */
    internal suspend fun pageMoveOptions(key: AppKey, cell: Int, pageId: Long): PageMoveOptions? =
        mutationMutex.withLock {
            val items = dao.getAll()
            val source = workspacePinAt(items, key, pageId, cell) ?: return@withLock null
            PageMoveOptions(source.id, pageMoveTargets(pageId, items, folderDao.getAll()))
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
            val destinationCell = readGrid().firstFreeCell(occupiedCells(items, folderDao.getAll(), destinationId))
                ?: return@withLock PageMoveResult.FULL
            dao.updatePlacement(source.id, Container.WORKSPACE.name, destinationCell, destinationId)
            PageMoveResult.MOVED
        }

    /**
     * Move the same pin from its exact source placement to an empty cell on an
     * existing page after an edge preview. Never substitute another cell or
     * another copy of the pin. This also accepts a preview back on the origin.
     */
    internal suspend fun moveWorkspacePinToCell(
        pinId: Long,
        sourcePageId: Long,
        sourceCell: Int,
        destinationPageId: Long,
        destinationCell: Int,
    ): Boolean = mutationMutex.withLock {
        if (workspacePreferences.layoutLocked) return@withLock false
        val items = dao.getAll()
        val source = items.firstOrNull { it.id == pinId && it.isOnPage(sourcePageId) && it.cellIndex == sourceCell }
            ?: return@withLock false
        if (destinationPageId != sourcePageId && db.workspacePageDao().getById(destinationPageId) == null) {
            return@withLock false
        }
        if (!readGrid().isValidCell(destinationCell) ||
            cellIsOccupied(items, destinationPageId, destinationCell)
        ) {
            return@withLock false
        }
        // Settings can change while the reads above are suspended.
        if (workspacePreferences.layoutLocked) return@withLock false
        dao.updatePlacement(source.id, Container.WORKSPACE.name, destinationCell, destinationPageId)
        true
    }

    /** Materializes a trailing drag preview and moves the exact regular pin in one commit. */
    internal suspend fun appendPageWithWorkspacePin(
        pinId: Long,
        sourcePageId: Long,
        sourceCell: Int,
        afterPageId: Long,
        destinationCell: Int,
        grid: GridCanvas,
    ): Long? = mutationMutex.withLock {
        db.withTransaction {
            if (workspacePreferences.layoutLocked || !grid.isValidCell(destinationCell) ||
                !dao.hasRegularWorkspacePin(pinId, sourcePageId, sourceCell)
            ) return@withTransaction null
            val destinationPageId = insertPageForPreview(afterPageId, grid) ?: return@withTransaction null
            dao.updateWorkspacePosition(pinId, destinationCell, destinationPageId)
            destinationPageId
        }
    }

    /** Appends and promotes the exact member, resolving its source folder in the same commit. */
    internal suspend fun appendPageWithFolderMember(
        pinId: Long,
        folderId: Long,
        sourcePageId: Long,
        afterPageId: Long,
        destinationCell: Int,
        grid: GridCanvas,
    ): Long? = mutationMutex.withLock {
        db.withTransaction {
            if (workspacePreferences.layoutLocked || !grid.isValidCell(destinationCell) ||
                !dao.hasFolderMember(pinId, folderId) || folderDao.getById(folderId)?.pageId != sourcePageId
            ) return@withTransaction null
            val destinationPageId = insertPageForPreview(afterPageId, grid) ?: return@withTransaction null
            promoteMemberToWorkspace(pinId, destinationCell, destinationPageId, folderId)
            destinationPageId
        }
    }

    /** Appends a page and moves the exact closed folder in one commit; its member rows stay as they are. */
    internal suspend fun appendPageWithFolder(
        folderId: Long,
        sourcePageId: Long,
        sourceCell: Int,
        afterPageId: Long,
        destinationCell: Int,
        grid: GridCanvas,
    ): Long? = mutationMutex.withLock {
        db.withTransaction {
            val source = folderDao.getById(folderId)
            if (workspacePreferences.layoutLocked || !grid.isValidCell(destinationCell) || source == null ||
                source.pageId != sourcePageId || source.cellIndex != sourceCell
            ) return@withTransaction null
            val destinationPageId = insertPageForPreview(afterPageId, grid) ?: return@withTransaction null
            folderDao.updateWorkspacePosition(folderId, destinationCell, destinationPageId)
            destinationPageId
        }
    }

    /** Under the mutation lock and transaction, materialize only the still-current trailing preview. */
    private suspend fun insertPageForPreview(afterPageId: Long, grid: GridCanvas): Long? {
        val pages = db.workspacePageDao().getAll()
        val last = pages.lastOrNull() ?: return null
        if (pages.size >= MAX_WORKSPACE_PAGES || last.id != afterPageId || readGrid() != grid ||
            workspacePreferences.layoutLocked
        ) return null
        return insertPageAfter(last)
    }

    /** Reads eligible destinations only if the selected folder is still in its source cell. */
    internal suspend fun folderPageMoveOptions(folder: Folder): PageMoveOptions? = mutationMutex.withLock {
        val folders = folderDao.getAll()
        val source = folders.atPlacement(folder) ?: return@withLock null
        PageMoveOptions(source.id, pageMoveTargets(folder.pageId, dao.getAll(), folders))
    }

    /** Changes only the folder's placement; its members still belong to the same folder ID. */
    internal suspend fun moveFolderToPage(folder: Folder, destinationId: Long): PageMoveResult =
        mutationMutex.withLock {
            if (folder.pageId == destinationId || db.workspacePageDao().getById(destinationId) == null) {
                return@withLock PageMoveResult.UNAVAILABLE
            }
            val folders = folderDao.getAll()
            val source = folders.atPlacement(folder) ?: return@withLock PageMoveResult.UNAVAILABLE
            val destinationCell = readGrid().firstFreeCell(occupiedCells(dao.getAll(), folders, destinationId))
                ?: return@withLock PageMoveResult.FULL
            folderDao.update(source.copy(pageId = destinationId, cellIndex = destinationCell))
            PageMoveResult.MOVED
        }

    /** Changes only the dragged folder's placement on an existing page. */
    internal suspend fun moveFolderToCell(
        folder: Folder,
        pageId: Long,
        cell: Int,
        grid: GridCanvas,
    ): Boolean = mutationMutex.withLock {
        // The caller's placement is matched literally: a stale drag source
        // (the UI always refreshes between drags) must not teleport the
        // folder — the move rejects instead (the stale-drop safety the
        // PagesTest suite asserts).
        if (workspacePreferences.layoutLocked || !grid.isValidCell(cell) ||
            (pageId == folder.pageId && cell == folder.cellIndex) || readGrid() != grid ||
            workspacePreferences.layoutLocked
        ) return@withLock false
        val items = dao.getAll()
        android.util.Log.d("FolderMove169", "in: caller=${folder.pageId}/${folder.cellIndex} -> $pageId/$cell grid=$grid items=${items.size}")
        // #169: a closed folder dropped onto an occupied cell displaces the
        // single top-level pin sitting there to the nearest free cell (the
        // folder's own source cell counts as free — it vacates). Without a
        // free cell the drop is rejected whole.
        val occupant = items.firstOrNull {
            it.container == Container.WORKSPACE.name && it.folderId == null &&
                it.pageId == pageId && it.cellIndex == cell
        } ?: return@withLock folderDao.moveToEmptyCell(folder.id, folder.pageId, folder.cellIndex, pageId, cell).also { android.util.Log.d("FolderMove169", "empty-move=$it") } == 1
        val occupied = buildSet {
            items.forEach { item ->
                if (item.id != occupant.id && item.container == Container.WORKSPACE.name && item.folderId == null) {
                    item.pageId?.let { add(it to item.cellIndex) }
                }
            }
            folderDao.getAll().forEach { placedFolder ->
                if (placedFolder.id != folder.id) placedFolder.pageId?.let { add(it to placedFolder.cellIndex) }
            }
        }
        val pageCellCount = grid.rows * grid.columns
        var freeCell = -1
        for (step in 1 until pageCellCount) {
            val candidate = (cell + step) % pageCellCount
            if ((pageId to candidate) !in occupied) {
                freeCell = candidate
                break
            }
        }
        if (freeCell < 0) return@withLock false
        db.withTransaction {
            dao.updateCellIndex(occupant.id, freeCell)
            if (folderDao.moveToEmptyCell(folder.id, folder.pageId, folder.cellIndex, pageId, cell) != 1) {
                // Rolled back with the transaction; the drop is rejected.
                throw IllegalStateException("Folder placement lost its target cell")
            }
        }
        true
    }

    private fun List<FolderEntity>.atPlacement(folder: Folder): FolderEntity? =
        firstOrNull { it.id == folder.id && it.pageId == folder.pageId && it.cellIndex == folder.cellIndex }

    private suspend fun pageMoveTargets(
        pageId: Long,
        items: List<WorkspaceItemEntity>,
        folders: List<FolderEntity>,
    ): List<PageMoveTarget> = buildList {
        val grid = readGrid()
        db.workspacePageDao().getAll().forEachIndexed { index, page ->
            if (page.id != pageId && grid.firstFreeCell(occupiedCells(items, folders, page.id)) != null) {
                add(PageMoveTarget(page.id, index + 1))
            }
        }
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
        if (dock.size >= DockPages.totalCapacity(dockCapacityPerPage)) return@withLock false
        val order = (dock.maxOfOrNull { it.cellIndex } ?: -1) + 1
        dao.insert(pinEntity(app.key, Container.DOCK, order, publisherLabel = actionPinLabel(app)))
        true
    }

    /** Unpins the exact shortcut; returns true only if this also removed its page. */
    internal suspend fun removeFromWorkspace(app: LauncherApp, cell: Int, pageId: Long = DEFAULT_WORKSPACE_PAGE_ID): Boolean =
        mutationMutex.withLock {
            val key = app.key
            val pageRemoved = db.withTransaction {
                if (workspacePreferences.layoutLocked) return@withTransaction false
                val deleted = dao.deleteWorkspacePin(key.packageName, key.className, key.userSerial, key.shortcutId, pageId, cell)
                deleted != 0 && key.shortcutId == null && deleteEmptyExtraPage(pageId)
            }
            if (pageRemoved) clearRemovedPageSelection(pageId)
            pageRemoved
        }

    suspend fun removeFromDock(pinId: Long) = mutationMutex.withLock { dao.deleteDockPin(pinId) }

    // App actions -----------------------------------------------------------------

    /**
     * Pins the app-published action [action] into the first free cell of
     * [pageId]. The whole sequence runs under [mutationMutex]:
     *
     * 1. A locked layout, a missing page or a non-action key reports
     *    [AppActionPinResult.UNAVAILABLE] — the platform is never contacted.
     * 2. Dedup matches the publisher package + profile + shortcut ID across
     *    every pinned placement (workspace, dock, folders) and reports
     *    [AppActionPinResult.ALREADY_PINNED].
     * 3. A full page reports [AppActionPinResult.FULL].
     * 4. Only then [desiredIds] validates the desired platform pin set — every
     *    currently pinned action ID of the same package and profile across all
     *    containers, plus this action. A `false` return (platform pin failed)
     *    reports [AppActionPinResult.UNAVAILABLE] without any Room row.
     * 5. Once platform pinning starts, finish the Room write or restore the
     *    previous platform set (best effort) before allowing cancellation.
     *    A failed database write reports [AppActionPinResult.UNAVAILABLE].
     *
     * The inserted row caches the action's publisher label as `shortcutLabel`.
     */
    suspend fun pinAppAction(
        action: LauncherApp,
        pageId: Long,
        desiredIds: suspend (Set<String>) -> Boolean,
    ): AppActionPinResult = mutationMutex.withLock {
        val key = action.key
        val shortcutId = key.shortcutId ?: return@withLock AppActionPinResult.UNAVAILABLE
        if (workspacePreferences.layoutLocked) return@withLock AppActionPinResult.UNAVAILABLE
        if (db.workspacePageDao().getById(pageId) == null) return@withLock AppActionPinResult.UNAVAILABLE

        val items = dao.getAll()
        // Dedup by publisher package + profile + shortcut ID across every
        // pinned placement (className is only metadata and may drift).
        if (items.any { it.packageName == key.packageName && it.userSerial == key.userSerial && it.shortcutId == shortcutId }) {
            return@withLock AppActionPinResult.ALREADY_PINNED
        }
        val cell = readGrid().firstFreeCell(occupiedCells(items, folderDao.getAll(), pageId))
            ?: return@withLock AppActionPinResult.FULL

        val previousIds = dao.shortcutIds(key.packageName, key.userSerial).toSet()
        withContext(NonCancellable) {
            if (!desiredIds(previousIds + shortcutId)) return@withContext AppActionPinResult.UNAVAILABLE
            try {
                dao.insert(
                    pinEntity(key, Container.WORKSPACE, cell, pageId = pageId, publisherLabel = action.publisherLabel),
                )
            } catch (_: Exception) {
                // The layout never recorded the pin, so the platform must not keep it.
                runCatching { desiredIds(previousIds) }
                return@withContext AppActionPinResult.UNAVAILABLE
            }
            AppActionPinResult.PINNED
        }
    }

    /**
     * Real snapshot of the pinned action IDs of one publisher package in one
     * profile, for syncing the platform pin set when saved pins are removed.
     */
    suspend fun shortcutIds(packageName: String, userSerial: Long): Set<String> = mutationMutex.withLock {
        dao.shortcutIds(packageName, userSerial).toSet()
    }

    /**
     * The stable row ID of the top-level pin for [key] still sitting at
     * ([pageId], [cell]), or null when the pin has moved or is gone.
     */
    internal suspend fun workspacePinId(key: AppKey, cell: Int, pageId: Long): Long? = mutationMutex.withLock {
        workspacePinAt(dao.getAll(), key, pageId, cell)?.id
    }

    /**
     * Renames the single pin identified by stable [pinId], verified against the
     * page and cell the caller is looking at. The override applies to this pin
     * only — the app's catalogue entry and every position stay untouched — and
     * a null [label] clears it. Returns false when the layout is locked (a
     * submitted or stale editor must not save after the lock) or the pin has
     * moved or is gone.
     */
    internal suspend fun renameWorkspacePin(pinId: Long, cell: Int, pageId: Long, label: String?): Boolean =
        mutationMutex.withLock {
            if (workspacePreferences.layoutLocked) return@withLock false
            val pin = dao.getAll().firstOrNull { it.id == pinId && it.isOnPage(pageId) && it.cellIndex == cell }
                ?: return@withLock false
            dao.updateCustomLabel(pin.id, label) > 0
        }

    /** Removes an uninstalled package's pins and resolves affected folders in the same transaction. */
    suspend fun removeAllForPackage(packageName: String, userSerial: Long) = mutationMutex.withLock {
        db.withTransaction {
            val affectedFolders = dao.folderIdsForPackage(packageName, userSerial)
            dao.deleteByPackage(packageName, userSerial)
            for (folderId in affectedFolders) dissolveIfSparse(folderId)
        }
        // An explicit uninstall also ends any restored-pin protection.
        val pendingKey = "$packageName:$userSerial"
        val pending = prefs.getStringSet(KEY_RESTORED_PENDING, null).orEmpty()
        if (pendingKey in pending) {
            prefs.edit { putStringSet(KEY_RESTORED_PENDING, pending - pendingKey) }
        }
    }

    /**
     * Removes pins only for uninstalled packages; unavailable pins keep their
     * positions. Pins recorded in [KEY_RESTORED_PENDING] survive this cleanup
     * while their package is still absent (the #116 restore may land before
     * setup finishes installing the apps); the protection ends once the
     * package turns up installed.
     */
    suspend fun pruneUninstalled(
        launchableKeys: Set<AppKey>,
        isPackageInstalled: (String, Long) -> Boolean,
    ) = mutationMutex.withLock {
        // Check package availability before opening the database write transaction.
        val restoredPending = prefs.getStringSet(KEY_RESTORED_PENDING, null).orEmpty()
        var pending = restoredPending
        val uninstalledPins = ArrayList<Long>()
        val packageInstalled = HashMap<Pair<String, Long>, Boolean>()
        for (item in dao.getAll()) {
            if (AppKey(item.packageName, item.className, item.userSerial, item.shortcutId) in launchableKeys) continue
            val installed = packageInstalled.getOrPut(item.packageName to item.userSerial) {
                isPackageInstalled(item.packageName, item.userSerial)
            }
            val pendingKey = "${item.packageName}:${item.userSerial}"
            if (pendingKey in pending) {
                if (installed) {
                    // The restored package arrived: end its protection so later
                    // prunes treat the pin like any other.
                    pending = pending - pendingKey
                }
                continue
            }
            if (!installed) uninstalledPins.add(item.id)
        }
        db.withTransaction {
            for (id in uninstalledPins) dao.deleteById(id)
            normalizeFolders()
        }
        if (pending !== restoredPending) {
            prefs.edit { putStringSet(KEY_RESTORED_PENDING, pending) }
        }
    }

    /** Dissolves folders that hold fewer than two members. */
    private suspend fun normalizeFolders() {
        for (folder in folderDao.getAll()) dissolveIfSparse(folder.id)
    }

    /**
     * Reads the personal layout as a versioned export snapshot (ADR-BACKUP-1
     * part 1, #116). One consistent read under the mutation mutex: pages in
     * display order, personal-profile pins only, dock in order, folders with
     * their titles, and the seeding flag. Work-profile pins never cross the
     * file.
     */
    internal suspend fun exportLayout(): LayoutSnapshotV1 = mutationMutex.withLock {
        val grid = readGrid()
        val pages = db.workspacePageDao().getAll()
        val pagePositions = pages.withIndex().associate { (index, page) -> page.id to index }
        val allItems = dao.getAll().filter { it.userSerial == 0L }
        val folders = folderDao.getAll()
        val exportedMembers = allItems.filter { it.folderId != null }
        // Keep folders that a personal pin references or that hold no member at
        // all (an empty folder tile is layout structure); drop folders whose
        // members are exclusively work-profile rows the file never carries.
        val folderOrder = folders
            .filter { folder -> folder.id in exportedMembers.mapNotNull { it.folderId } || dao.firstTwoFolderMembers(folder.id).isEmpty() }
            .sortedWith(compareBy({ pagePositions[it.pageId] ?: Int.MAX_VALUE }, { it.cellIndex }))
        val folderPositions = folderOrder.withIndex().associate { (index, folder) -> folder.id to index }
        LayoutSnapshotV1(
            columns = grid.columns,
            rows = grid.rows,
            seeded = isSeeded(),
            pages = pages.map { it.rank },
            dock = allItems
                .filter { it.container == Container.DOCK.name }
                .sortedBy { it.cellIndex }
                .map {
                    DockPinSnapshot(it.packageName, it.className, it.shortcutId, it.customLabel, it.shortcutLabel)
                },
            folders = folderOrder.map { folder ->
                FolderSnapshot(
                    title = folder.title,
                    pagePosition = pagePositions[folder.pageId] ?: 0,
                    cell = folder.cellIndex,
                )
            },
            pins = allItems
                .filter { it.container != Container.DOCK.name }
                .mapNotNull { item ->
                    val pagePosition = pagePositions[item.pageId]
                    val folderIndex = item.folderId?.let { folderId -> folderPositions[folderId] }
                    if (item.folderId == null && pagePosition == null) {
                        null // Malformed row without a page or folder: never export it.
                    } else if (folderIndex == null && item.folderId != null) {
                        null // Member of a folder that holds only work rows.
                    } else {
                        PinSnapshot(
                            packageName = item.packageName,
                            className = item.className,
                            pagePosition = pagePosition ?: 0,
                            cell = item.cellIndex,
                            folderIndex = folderIndex,
                            shortcutId = item.shortcutId,
                            customLabel = item.customLabel,
                            shortcutLabel = item.shortcutLabel,
                        )
                    }
                },
        )
    }

    /**
     * Validates the snapshot and describes what a restore would apply: counts,
     * folder titles, the packages that are not installed here, and how many
     * current pins the restore replaces. Throws [LayoutSnapshotException] for
     * corrupt or unsupported input; the layout is untouched.
     */
    internal suspend fun restorePreview(
        snapshot: LayoutSnapshotV1,
        isPackageInstalled: (String) -> Boolean,
    ): LayoutRestorePreview = mutationMutex.withLock {
        requireDockFits(snapshot)
        val unavailable = snapshot.packages().filterNot(isPackageInstalled).sorted()
        LayoutRestorePreview(
            snapshot = snapshot,
            pageCount = snapshot.pages.size,
            workspacePinCount = snapshot.pins.size,
            dockPinCount = snapshot.dock.size,
            folderCount = snapshot.folders.size,
            folderTitles = snapshot.folders.map { it.title },
            unavailablePackages = unavailable,
            replacedPinCount = dao.getAll().size,
        )
    }

    /**
     * The dock capacity is a destination setting the snapshot never carries, so
     * a file from a larger dock is unsupported here and the whole restore is
     * rejected rather than stranding overflow rows beyond the last dock page.
     */
    private fun requireDockFits(snapshot: LayoutSnapshotV1) {
        val capacity = DockPages.totalCapacity(dockCapacityPerPage)
        if (snapshot.dock.size > capacity) {
            throw LayoutSnapshotException(
                "Dock holds ${snapshot.dock.size} pins, over this device's dock capacity $capacity",
            )
        }
    }

    /**
     * Replaces the whole layout with the snapshot in one transaction: pages in
     * display order, personal pins, dock, folders with membership, grid, and
     * the seeding flag. Absent packages land in [KEY_RESTORED_PENDING] so
     * [pruneUninstalled] keeps their pins until the packages arrive. Runs
     * after the previewed explicit confirmation; any failure rolls back to the
     * previous layout.
     */
    internal suspend fun applyRestore(
        snapshot: LayoutSnapshotV1,
        isPackageInstalled: (String) -> Boolean,
    ): Unit = mutationMutex.withLock {
        // The capacity may have moved since the preview; the apply is just as
        // all-or-nothing about it.
        requireDockFits(snapshot)
        val restoredPending = snapshot.packages()
            .filterNot(isPackageInstalled)
            .map { "$it:0" }
            .toSet()
        db.withTransaction {
            val pageDao = db.workspacePageDao()
            dao.deleteAll()
            folderDao.deleteAll()
            pageDao.deleteAll()
            val pageIds = snapshot.pages.map { rank -> db.workspacePageDao().insert(WorkspacePageEntity(rank = rank)) }
            val folderIds = snapshot.folders.map { folder ->
                folderDao.insert(
                    FolderEntity(title = folder.title, cellIndex = folder.cell, pageId = pageIds[folder.pagePosition])
                )
            }
            snapshot.dock.forEachIndexed { order, dockPin ->
                dao.insert(
                    WorkspaceItemEntity(
                        packageName = dockPin.packageName,
                        className = dockPin.className,
                        container = Container.DOCK.name,
                        cellIndex = order,
                        userSerial = 0L,
                        shortcutId = dockPin.shortcutId,
                        customLabel = dockPin.customLabel,
                        shortcutLabel = dockPin.shortcutLabel,
                    )
                )
            }
            snapshot.pins.forEach { pin ->
                val inFolder = pin.folderIndex != null
                dao.insert(
                    WorkspaceItemEntity(
                        packageName = pin.packageName,
                        className = pin.className,
                        container = Container.WORKSPACE.name,
                        cellIndex = pin.cell,
                        userSerial = 0L,
                        folderId = pin.folderIndex?.let { folderIds[it] },
                        pageId = if (inFolder) null else pageIds[pin.pagePosition],
                        shortcutId = pin.shortcutId,
                        customLabel = pin.customLabel,
                        shortcutLabel = pin.shortcutLabel,
                    )
                )
            }
            gridDao.set(WorkspaceGridEntity(columnCount = snapshot.columns, rowCount = snapshot.rows))
        }
        prefs.edit {
            putBoolean(KEY_SEEDED, snapshot.seeded)
            putStringSet(KEY_RESTORED_PENDING, restoredPending)
        }
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
        if (!readGrid().isValidCell(toCell)) return@withLock false
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
            it.isOnPage(pageId) && it.cellIndex == cell && it.hasKey(key)
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
        if (!readGrid().isValidCell(cell) || db.workspacePageDao().getById(pageId) == null) return@withLock false
        if (cellIsOccupied(dao.getAll(), pageId, cell)) return@withLock false
        insertWorkspaceApp(app, cell, pageId)
        true
    }

    /**
     * Inserts a top-level app at [slot] and shifts existing dock pins in a
     * single transaction. Returns false when the dock is full.
     */
    suspend fun insertIntoDock(app: LauncherApp, slot: Int): Boolean = mutationMutex.withLock {
        db.withTransaction {
            val dock = dao.getAll().filter { it.container == Container.DOCK.name && it.folderId == null }
                .sortedBy { it.cellIndex }
            if (dock.size >= DockPages.totalCapacity(dockCapacityPerPage)) return@withTransaction false

            val insertSlot = slot.coerceIn(0, dock.size)
            dao.insert(
                pinEntity(app.key, Container.DOCK, insertSlot, publisherLabel = actionPinLabel(app)),
            )
            dock.forEachIndexed { index, pin ->
                val newSlot = if (index < insertSlot) index else index + 1
                if (pin.cellIndex != newSlot) dao.updateCellIndex(pin.id, newSlot)
            }
            true
        }
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
        if (dockIds.size >= DockPages.totalCapacity(dockCapacityPerPage)) return@withLock DockMoveResult.FULL
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
        if (!readGrid().isValidCell(cell) || db.workspacePageDao().getById(pageId) == null) return@withLock false
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
        /** The moved pin's own placement, which may be on another page than the drop target. */
        data class Workspace(val cell: Int, val pageId: Long = DEFAULT_WORKSPACE_PAGE_ID) : FolderDropSource
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
        if (!readGrid().isValidCell(targetCell)) return@withLock false
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
            folderDropPin(items, key, source) ?: return@withLock false
        }
        if (draggedItem?.id == target.id || folderAtCell(pageId, targetCell) != null) return@withLock false
        // Roll back the folder row if either membership update fails.
        db.withTransaction {
            val folderId = folderDao.insert(FolderEntity(title = defaultTitle, cellIndex = targetCell, pageId = pageId))
            dao.updateFolder(target.id, folderId)
            if (draggedItem == null) insertFolderMember(key, folderId, targetCell + 1, actionPinLabel(dragged))
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
                insertFolderMember(key, folderId, lastPosition + 1, actionPinLabel(app))
            } else {
                val entry = folderDropPin(items, key, source) ?: return@withLock false
                dao.updateFolder(entry.id, folderId)
            }
            true
        }

    /**
     * Appends a resolved batch of ordinary apps as new copies at the end of
     * [folderId], in the provided order, without touching prior members or any
     * top-level duplicate pins. The caller resolves live installed targets, so
     * the repository only revalidates under [mutationMutex] in one transaction:
     * the layout must be unlocked, the folder must still sit at the exact
     * ([pageId], [cellIndex]) the caller looked at, and every key must be a
     * plain activity key, distinct within the batch and absent from the folder's
     * persisted members. Any stale target, duplicate or mixed batch is reported
     * as `false` before the first insert — members are never partially added.
     */
    internal suspend fun addAppsToFolder(
        folderId: Long,
        pageId: Long,
        cellIndex: Int,
        apps: List<LauncherApp>,
    ): Boolean = mutationMutex.withLock {
        db.withTransaction {
            if (workspacePreferences.layoutLocked) return@withTransaction false
            val folder = folderDao.getById(folderId)
            if (folder == null || folder.pageId != pageId || folder.cellIndex != cellIndex) {
                return@withTransaction false
            }
            if (apps.isEmpty()) return@withTransaction false
            val keys = HashSet<AppKey>(apps.size)
            for (app in apps) {
                if (app.key.shortcutId != null || !keys.add(app.key)) return@withTransaction false
            }
            val members = dao.folderMembersInOrder(folderId)
            if (members.any { AppKey(it.packageName, it.className, it.userSerial, it.shortcutId) in keys }) {
                return@withTransaction false
            }
            // Settings can change while the reads above are suspended.
            if (workspacePreferences.layoutLocked) return@withTransaction false
            // Same append convention as a single drawer drop: continue after the
            // sparse-or-dense maximum rank.
            var position = members.maxOfOrNull { it.cellIndex } ?: -1
            for (app in apps) {
                insertFolderMember(app.key, folderId, ++position, actionPinLabel(app))
            }
            true
        }
    }

    private suspend fun insertFolderMember(key: AppKey, folderId: Long, position: Int, publisherLabel: String?) {
        dao.insert(pinEntity(key, Container.WORKSPACE, position, folderId = folderId, publisherLabel = publisherLabel))
    }

    private fun folderDropPin(
        items: List<WorkspaceItemEntity>,
        key: AppKey,
        source: FolderDropSource,
    ): WorkspaceItemEntity? = when (source) {
        is FolderDropSource.Workspace -> workspacePinAt(items, key, source.pageId, source.cell)
        is FolderDropSource.Dock -> items.firstOrNull { it.isDockPin(source.pinId) && it.hasKey(key) }
        FolderDropSource.Drawer -> null
    }

    private fun WorkspaceItemEntity.isDockPin(pinId: Long): Boolean =
        id == pinId && container == Container.DOCK.name && folderId == null

    private fun WorkspaceItemEntity.hasKey(key: AppKey): Boolean =
        packageName == key.packageName && className == key.className && userSerial == key.userSerial &&
            shortcutId == key.shortcutId

    /**
     * Returns the selected pin of [folderId] to the first free grid cell. If the folder
     * dissolves, its last member returns to the folder cell in the same transaction.
     * Reports insufficient space separately from a missing folder or membership.
     */
    internal suspend fun removeFromFolder(pinId: Long, folderId: Long): FolderRemovalResult = mutationMutex.withLock {
        val items = dao.getAll()
        if (items.none { it.id == pinId && it.folderId == folderId }) return@withLock FolderRemovalResult.UNAVAILABLE
        val folder = folderDao.getById(folderId) ?: return@withLock FolderRemovalResult.UNAVAILABLE
        val pageId = folder.pageId ?: return@withLock FolderRemovalResult.UNAVAILABLE
        val occupied = occupiedCells(items, folderDao.getAll(), pageId)
        val cell = readGrid().firstFreeCell(occupied) ?: return@withLock FolderRemovalResult.NO_ROOM
        db.withTransaction { promoteMemberToWorkspace(pinId, cell, pageId, folderId) }
        FolderRemovalResult.REMOVED
    }

    /** Inserts the moving pin at the target pin's current rank, shifting intervening members. */
    internal suspend fun reorderFolderMember(
        folderId: Long,
        pageId: Long,
        pinId: Long,
        targetId: Long,
    ): Boolean = mutationMutex.withLock {
        if (workspacePreferences.layoutLocked || pinId == targetId) return@withLock false
        if (folderDao.getById(folderId)?.pageId != pageId) return@withLock false
        val members = dao.folderMembersInOrder(folderId)
        val from = members.indexOfFirst { it.id == pinId }
        val to = members.indexOfFirst { it.id == targetId }
        if (from < 0 || to < 0) return@withLock false
        writeFolderRanks(members.size) { index ->
            val previousIndex = when {
                index == to -> from
                from < to && index in from until to -> index + 1
                to < from && index > to && index <= from -> index - 1
                else -> index
            }
            members[previousIndex]
        }
        true
    }

    /** Sorts the currently visible labels; unavailable members keep their ordinal slots. */
    internal suspend fun sortFolderMembers(
        folderId: Long,
        pageId: Long,
        labels: Map<Long, String>,
        locale: Locale,
    ): Boolean = mutationMutex.withLock {
        if (workspacePreferences.layoutLocked || labels.size < 2) return@withLock false
        if (folderDao.getById(folderId)?.pageId != pageId) return@withLock false
        val members = dao.folderMembersInOrder(folderId)
        val visible = ArrayList<WorkspaceItemEntity>(labels.size)
        for (member in members) if (member.id in labels) visible.add(member)
        if (visible.size < 2) return@withLock false
        val collation = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
        visible.sortWith { left, right ->
            collation.compare(labels.getValue(left.id), labels.getValue(right.id))
        }
        var visibleIndex = 0
        writeFolderRanks(members.size) { index ->
            val member = members[index]
            if (member.id in labels) visible[visibleIndex++] else member
        }
        true
    }

    private suspend inline fun writeFolderRanks(
        size: Int,
        crossinline memberAt: (Int) -> WorkspaceItemEntity,
    ) {
        db.withTransaction {
            // Older membership moves retained the previous cell, so ranks may be sparse or equal.
            for (index in 0 until size) {
                val member = memberAt(index)
                if (member.cellIndex != index) dao.updateCellIndex(member.id, index)
            }
        }
    }

    /**
     * Promote the same folder member into the exact empty destination cell.
     * The destination defaults to the source page for ordinary same-page drops.
     * Sparse-folder dissolution still returns the other member to the source
     * folder's cell in the same transaction.
     */
    internal suspend fun moveFolderMemberToCell(
        pinId: Long,
        folderId: Long,
        pageId: Long,
        cell: Int,
        destinationPageId: Long = pageId,
    ): Boolean = mutationMutex.withLock {
        if (workspacePreferences.layoutLocked) return@withLock false
        val items = dao.getAll()
        if (items.none { it.id == pinId && it.folderId == folderId }) return@withLock false
        val folder = folderDao.getById(folderId) ?: return@withLock false
        if (folder.pageId != pageId) return@withLock false
        if (destinationPageId != pageId && db.workspacePageDao().getById(destinationPageId) == null) {
            return@withLock false
        }
        if (!readGrid().isValidCell(cell) || cellIsOccupied(items, destinationPageId, cell)) {
            return@withLock false
        }
        // Settings can change while the reads above are suspended.
        if (workspacePreferences.layoutLocked) return@withLock false
        db.withTransaction { promoteMemberToWorkspace(pinId, cell, destinationPageId, folderId) }
        true
    }

    /**
     * The shared write behind "remove from folder" and an exact member drop:
     * moves [pinId] to the top-level workspace [cell] on the placement page
     * [pageId] — the destination page for a cross-page drop — and resolves the
     * folder inside the caller's Room transaction: a folder left with fewer than
     * two members dissolves with its remaining member returning to the folder
     * cell on the folder's own page.
     */
    private suspend fun promoteMemberToWorkspace(pinId: Long, cell: Int, pageId: Long, folderId: Long) {
        dao.updatePlacement(pinId, Container.WORKSPACE.name, cell, pageId)
        dissolveIfSparse(folderId)
    }

    /** Renames a folder. Returns false when the folder does not exist. */
    suspend fun renameFolder(folderId: Long, title: String): Boolean = mutationMutex.withLock {
        val folder = folderDao.getById(folderId) ?: return@withLock false
        folderDao.update(folder.copy(title = title))
        true
    }

    /**
     * Removes a folder, returning every member to distinct free grid cells in order.
     * Reports insufficient space or a missing folder without changing any placement.
     */
    internal suspend fun removeFolder(folderId: Long): FolderRemovalResult = mutationMutex.withLock {
        val folder = folderDao.getById(folderId) ?: return@withLock FolderRemovalResult.UNAVAILABLE
        val pageId = folder.pageId ?: return@withLock FolderRemovalResult.UNAVAILABLE
        val items = dao.getAll()
        val members = items.filter { it.folderId == folderId }.sortedBy { it.cellIndex }
        val occupied = occupiedCells(items, folderDao.getAll(), pageId, excludedFolderId = folder.id)
        val freeCells = (0 until readGrid().capacity).filter { it !in occupied }
        if (members.size > freeCells.size) return@withLock FolderRemovalResult.NO_ROOM
        db.withTransaction {
            folderDao.deleteById(folderId)
            for ((index, member) in members.withIndex()) {
                val cell = freeCells[index]
                dao.updatePlacement(member.id, Container.WORKSPACE.name, cell, pageId)
            }
        }
        FolderRemovalResult.REMOVED
    }

    /** Unpins the captured folder and its members; keeps an exact snapshot for Undo. */
    internal suspend fun unpinFolder(folder: Folder): RemovedFolder? = mutationMutex.withLock {
        db.withTransaction {
            if (workspacePreferences.layoutLocked) return@withTransaction null
            val saved = folderDao.getById(folder.id)?.takeIf {
                it.pageId == folder.pageId && it.cellIndex == folder.cellIndex
            } ?: return@withTransaction null
            val members = dao.folderMembersInOrder(saved.id)
            if (workspacePreferences.layoutLocked) return@withTransaction null
            dao.deleteFolderMembers(saved.id)
            folderDao.deleteById(saved.id)
            RemovedFolder(saved, members)
        }
    }

    /** Restores only the original placement and rows; never displaces a new pin. */
    internal suspend fun restoreFolder(removed: RemovedFolder): FolderRestoreResult = mutationMutex.withLock {
        db.withTransaction {
            if (workspacePreferences.layoutLocked) return@withTransaction FolderRestoreResult.LOCKED
            val folder = removed.folder
            val pageId = folder.pageId ?: return@withTransaction FolderRestoreResult.UNAVAILABLE
            if (db.workspacePageDao().getById(pageId) == null) return@withTransaction FolderRestoreResult.UNAVAILABLE
            val items = dao.getAll()
            if (!readGrid().isValidCell(folder.cellIndex) || cellIsOccupied(items, pageId, folder.cellIndex)) {
                return@withTransaction FolderRestoreResult.NO_ROOM
            }
            val actionAlreadyPinned = removed.members.any { member ->
                member.shortcutId != null && items.any {
                    it.packageName == member.packageName && it.userSerial == member.userSerial &&
                        it.shortcutId == member.shortcutId
                }
            }
            if (actionAlreadyPinned) return@withTransaction FolderRestoreResult.UNAVAILABLE
            if (workspacePreferences.layoutLocked) return@withTransaction FolderRestoreResult.LOCKED
            folderDao.insert(folder)
            dao.insertAll(removed.members)
            FolderRestoreResult.RESTORED
        }
    }

    /**
     * Dissolves a folder that holds fewer than two members: the sole member returns
     * to the folder's grid cell, an empty folder is deleted.
     */
    private suspend fun dissolveIfSparse(folderId: Long) {
        val members = dao.firstTwoFolderMembers(folderId)
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

        /** Package:serial pairs whose restored pins survive [pruneUninstalled] until the package arrives. */
        const val KEY_RESTORED_PENDING = "restored_pending_packages"
    }
}
