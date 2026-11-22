package dev.vladon.vesna.launcher.workspace

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.platform.AppDiscovery
import dev.vladon.vesna.launcher.platform.AppLauncher
import dev.vladon.vesna.launcher.platform.PackageEvent

/** Non-fatal feedback from launcher actions, mapped to user-visible strings by the UI. */
enum class LauncherMessage {
    WORKSPACE_FULL, DOCK_FULL, WORKSPACE_PAGE_NOT_EMPTY,
    SHORTCUT_MOVED, FOLDER_MOVED, NO_MOVE_DESTINATION, MOVE_DESTINATION_FULL, MOVE_UNAVAILABLE,
    WORKSPACE_PAGE_REORDERED, WORKSPACE_PAGE_ORDER_CHANGED,
    APP_INFO_UNAVAILABLE,
}

/** Everything the launcher UI renders. */
data class LauncherUiState(
    val apps: List<LauncherApp> = emptyList(),
    val workspaceCells: Map<Int, CellContent> = emptyMap(),
    val reservedWorkspaceCellsMask: Int = 0,
    val dock: DockLayout = DockLayout(emptyList(), emptyList()),
    val folders: Map<Long, Folder> = emptyMap(),
    val folderContents: Map<Long, List<FolderPin>> = emptyMap(),
    /** Workspace page IDs in display order; [activePageId] names the rendered page. */
    val pageIds: List<Long> = emptyList(),
    val activePageId: Long = DEFAULT_WORKSPACE_PAGE_ID,
    val canRemoveActivePage: Boolean = false,
) {
    val isWorkspaceEmpty: Boolean get() = workspaceCells.isEmpty()
}

/**
 * Application-scoped state holder of the launcher. A launcher process is long-lived
 * and its layout must survive activity recreation, so state lives here instead of
 * per-activity ViewModels.
 *
 * Combines live app discovery with the persisted layout and exposes one
 * [StateFlow] for the UI plus action methods that mutate persistence.
 */
class LauncherController(
    private val scope: CoroutineScope,
    private val discovery: AppDiscovery,
    private val appLauncher: AppLauncher,
    private val workspaceRepository: WorkspaceRepository,
    packageEvents: SharedFlow<PackageEvent>,
) {

    private val refreshMutex = Mutex()
    private val pageActionMutex = Mutex()
    // The ordered drawer list and the placement index come from the same discovery.
    private data class AppCatalogue(val apps: List<LauncherApp>) {
        val byKey = apps.associateBy { it.key }
    }

    private class LayoutProjection(
        val catalogue: AppCatalogue,
        val items: List<WorkspaceItem>,
        val folders: List<Folder>,
    ) {
        val dock = WorkspaceResolver.dockLayout(items, catalogue.byKey)
        val foldersById = folders.associateBy { it.id }
        val folderContents = WorkspaceResolver.folderContents(items, catalogue.byKey)
    }

    private val appCatalogue = MutableStateFlow(AppCatalogue(emptyList()))

    /** The selected page ID; stale IDs on startup resolve to the primary page. */
    private val _activePageId = MutableStateFlow(workspaceRepository.savedActivePageId())

    /** Shared page order so [state] and [createWorkspacePage] observe the same IDs. */
    private val pageIds = workspaceRepository.pages
        .map { pages -> pages.map { it.id } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _messages = MutableSharedFlow<LauncherMessage>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages: SharedFlow<LauncherMessage> = _messages.asSharedFlow()

    // Page navigation changes the grid, not the global dock or saved folder contents.
    private val layout = combine(appCatalogue, workspaceRepository.savedLayout) { catalogue, saved ->
        LayoutProjection(catalogue, saved.items, saved.folders)
    }

    val state: StateFlow<LauncherUiState> =
        combine(layout, pageIds, _activePageId) { layout, orderedIds, activePageId ->
            // The stored ID can outlive a deleted page if the process stopped
            // between the Room write and the preference update.
            val selection = if (activePageId in orderedIds) {
                activePageId
            } else {
                orderedIds.firstOrNull() ?: DEFAULT_WORKSPACE_PAGE_ID
            }
            val workspace = WorkspaceResolver.workspaceLayout(
                layout.items, layout.catalogue.byKey, layout.folders, selection,
            )
            LauncherUiState(
                apps = layout.catalogue.apps,
                workspaceCells = workspace.cells,
                reservedWorkspaceCellsMask = workspace.reservedCellsMask,
                dock = layout.dock,
                folders = layout.foldersById,
                folderContents = layout.folderContents,
                pageIds = orderedIds,
                activePageId = selection,
                canRemoveActivePage = selection != DEFAULT_WORKSPACE_PAGE_ID &&
                    layout.items.none { it.pageId == selection } && layout.folders.none { it.pageId == selection },
            )
        }.stateIn(scope, SharingStarted.Eagerly, LauncherUiState())

    init {
        scope.launch { refresh() }
        scope.launch {
            packageEvents.collect { event ->
                if (event is PackageEvent.Removed) {
                    workspaceRepository.removeAllForPackage(event.packageName, event.userSerial)
                }
                refresh(event as? PackageEvent.Added)
            }
        }
    }

    internal fun launch(app: LauncherApp): Boolean {
        if (appLauncher.launch(app)) return true
        // A package change may have made the tile stale. Refresh the catalogue.
        scope.launch { refresh() }
        return false
    }

    internal fun openAppInfo(app: LauncherApp) {
        if (!appLauncher.openAppInfo(app)) _messages.tryEmit(LauncherMessage.APP_INFO_UNAVAILABLE)
    }

    /** Appends a new page (capped at [MAX_WORKSPACE_PAGES]) and selects it; a no-op at the cap. */
    fun createWorkspacePage() {
        scope.launch {
            pageActionMutex.withLock {
                val pageId = workspaceRepository.createPage() ?: return@withLock
                pageIds.first { pageId in it }
                if (workspaceRepository.selectPage(pageId)) _activePageId.value = pageId
            }
        }
    }

    /** Selects an existing page and persists the choice for the next process; a no-op for unknown IDs. */
    fun selectWorkspacePage(pageId: Long) {
        scope.launch {
            pageActionMutex.withLock {
                if (workspaceRepository.selectPage(pageId)) _activePageId.value = pageId
            }
        }
    }

    /** Removes the captured page only if it is still active and has no saved content. */
    fun removeEmptyWorkspacePage(pageId: Long) {
        scope.launch {
            pageActionMutex.withLock {
                if (pageId == DEFAULT_WORKSPACE_PAGE_ID || _activePageId.value != pageId) return@withLock
                if (workspaceRepository.removeEmptyPage(pageId)) {
                    _activePageId.value = DEFAULT_WORKSPACE_PAGE_ID
                } else {
                    _messages.tryEmit(LauncherMessage.WORKSPACE_PAGE_NOT_EMPTY)
                }
            }
        }
    }

    /** Reorders the active page without changing the selected ID or its contents. */
    internal fun reorderWorkspacePage(pageId: Long, destinationIndex: Int, displayedOrder: List<Long>) {
        scope.launch {
            pageActionMutex.withLock {
                val moved = _activePageId.value == pageId &&
                    workspaceRepository.reorderPage(pageId, destinationIndex, displayedOrder)
                _messages.tryEmit(
                    if (moved) LauncherMessage.WORKSPACE_PAGE_REORDERED else LauncherMessage.WORKSPACE_PAGE_ORDER_CHANGED,
                )
            }
        }
    }

    /** Drawer/app-source insert into an empty workspace [cell] on [pageId]. */
    fun insertWorkspaceAt(app: LauncherApp, cell: Int, pageId: Long) {
        scope.launch { workspaceRepository.insertWorkspaceAt(app, cell, pageId) }
    }

    /** Drawer-source insert into the dock at [slot]; emits DOCK_FULL when full. */
    fun insertIntoDock(app: LauncherApp, slot: Int) {
        scope.launch {
            if (!workspaceRepository.insertIntoDock(app, slot)) {
                _messages.tryEmit(LauncherMessage.DOCK_FULL)
            }
        }
    }

    /** Creates a folder at [targetCell] on [pageId] from a drop of [dragged] onto its occupant. */
    internal fun createFolderFromDrop(dragged: LauncherApp, targetCell: Int, defaultTitle: String, source: WorkspaceRepository.FolderDropSource, pageId: Long) {
        scope.launch {
            workspaceRepository.createFolderFromDrop(dragged, targetCell, defaultTitle, source, pageId)
        }
    }

    /** Adds a dragged app into an existing folder. */
    internal fun addToFolder(app: LauncherApp, folderId: Long, source: WorkspaceRepository.FolderDropSource) {
        scope.launch { workspaceRepository.addToFolder(app, folderId, source) }
    }

    /** Returns a member of [folderId] to the first free grid cell (dissolves sparse folders). */
    internal suspend fun removeFromFolder(pinId: Long, folderId: Long): FolderRemovalResult =
        workspaceRepository.removeFromFolder(pinId, folderId)

    /** Renames a folder. */
    fun renameFolder(folderId: Long, title: String) {
        scope.launch { workspaceRepository.renameFolder(folderId, title) }
    }

    /** Removes a folder, returning members to free grid cells. */
    internal suspend fun removeFolder(folderId: Long): FolderRemovalResult =
        workspaceRepository.removeFolder(folderId)

    /** Workspace → dock drag at [slot] from [pageId]; emits [LauncherMessage.DOCK_FULL] when full. */
    fun moveWorkspaceToDock(app: LauncherApp, fromCell: Int, slot: Int, pageId: Long) {
        scope.launch {
            if (workspaceRepository.moveWorkspaceToDock(app, fromCell, slot, pageId) == DockMoveResult.FULL) {
                _messages.tryEmit(LauncherMessage.DOCK_FULL)
            }
        }
    }

    /** Dock → workspace drag onto an empty [cell] of [pageId]; no-op when the cell is occupied. */
    fun moveDockToWorkspace(pinId: Long, cell: Int, pageId: Long) {
        scope.launch { workspaceRepository.moveDockToWorkspace(pinId, cell, pageId) }
    }

    /** Moves the workspace pin from [fromCell] to [toCell] on [pageId]. */
    fun moveWithinWorkspace(app: LauncherApp, fromCell: Int, toCell: Int, pageId: Long) {
        scope.launch { workspaceRepository.moveWithinWorkspace(app, fromCell, toCell, pageId) }
    }

    internal suspend fun pageMoveOptions(app: LauncherApp, cell: Int, pageId: Long): PageMoveOptions? =
        workspaceRepository.pageMoveOptions(app.key, cell, pageId)

    internal fun movePinToPage(pinId: Long, cell: Int, pageId: Long, destinationId: Long) {
        scope.launch {
            reportPageMove(workspaceRepository.movePinToPage(pinId, cell, pageId, destinationId), LauncherMessage.SHORTCUT_MOVED)
        }
    }

    internal suspend fun folderPageMoveOptions(folder: Folder): PageMoveOptions? =
        workspaceRepository.folderPageMoveOptions(folder)

    internal fun moveFolderToPage(folder: Folder, destinationId: Long) {
        scope.launch {
            reportPageMove(workspaceRepository.moveFolderToPage(folder, destinationId), LauncherMessage.FOLDER_MOVED)
        }
    }

    internal fun moveFolderWithinPage(folder: Folder, toCell: Int) {
        scope.launch { workspaceRepository.moveFolderWithinPage(folder, toCell) }
    }

    private fun reportPageMove(result: PageMoveResult, success: LauncherMessage) {
        _messages.tryEmit(when (result) {
            PageMoveResult.MOVED -> success
            PageMoveResult.FULL -> LauncherMessage.MOVE_DESTINATION_FULL
            PageMoveResult.UNAVAILABLE -> LauncherMessage.MOVE_UNAVAILABLE
        })
    }

    /** Dock-internal reorder to [slot]. */
    fun reorderDock(pinId: Long, slot: Int) {
        scope.launch { workspaceRepository.reorderDock(pinId, slot) }
    }

    /** Places [app] into the first free cell of [pageId]; emits WORKSPACE_FULL when that page is full. */
    fun addToWorkspace(app: LauncherApp, pageId: Long) {
        scope.launch {
            if (!workspaceRepository.addToWorkspace(app, pageId)) _messages.tryEmit(LauncherMessage.WORKSPACE_FULL)
        }
    }

    fun removeFromWorkspace(app: LauncherApp, cell: Int, pageId: Long) {
        scope.launch { workspaceRepository.removeFromWorkspace(app, cell, pageId) }
    }

    fun addToDock(app: LauncherApp) {
        scope.launch {
            if (!workspaceRepository.addToDock(app)) _messages.tryEmit(LauncherMessage.DOCK_FULL)
        }
    }

    fun removeFromDock(pinId: Long) {
        scope.launch { workspaceRepository.removeFromDock(pinId) }
    }

    /** A locale change updates app metadata, not the saved layout. */
    internal fun refreshLocalizedApps() {
        scope.launch {
            refreshMutex.withLock { appCatalogue.value = discoverApps() }
        }
    }

    private suspend fun discoverApps(): AppCatalogue =
        withContext(Dispatchers.IO) { AppCatalogue(discovery.discover()) }

    /**
     * Refreshes launchable apps, seeds the layout on first run, and removes pins
     * for packages uninstalled while the launcher was not running.
     * Serialized with a mutex so overlapping package events cannot double-seed;
     * [WorkspaceRepository.seedOrAdopt] additionally keeps the adopt-or-seed
     * decision atomic against pins placed before the first seed completes.
     */
    private suspend fun refresh(added: PackageEvent.Added? = null): Unit = refreshMutex.withLock {
        val catalogue = discoverApps()
        appCatalogue.value = catalogue
        if (!workspaceRepository.isSeeded() && catalogue.apps.isNotEmpty()) {
            workspaceRepository.seedOrAdopt(SeedPlanner.plan(catalogue.apps))
        }
        withContext(Dispatchers.IO) {
            workspaceRepository.pruneUninstalled(catalogue.byKey.keys, discovery::isPackageInstalled)
        }
        if (added != null) {
            val app = catalogue.apps.firstOrNull {
                it.key.packageName == added.packageName && it.key.userSerial == added.userSerial
            }
            if (app != null) workspaceRepository.pinNewlyInstalledApp(app)
        }
    }
}
