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
enum class LauncherMessage { WORKSPACE_FULL, DOCK_FULL }

/** Everything the launcher UI renders. */
data class LauncherUiState(
    val apps: List<LauncherApp> = emptyList(),
    val workspaceCells: Map<Int, CellContent> = emptyMap(),
    val dockApps: List<LauncherApp> = emptyList(),
    val folders: Map<Long, Folder> = emptyMap(),
    val folderContents: Map<Long, List<LauncherApp>> = emptyMap(),
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
    private val _apps = MutableStateFlow<List<LauncherApp>>(emptyList())

    private val _messages = MutableSharedFlow<LauncherMessage>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages: SharedFlow<LauncherMessage> = _messages.asSharedFlow()

    val state: StateFlow<LauncherUiState> =
        combine<List<LauncherApp>, List<WorkspaceItem>, List<Folder>, LauncherUiState>(
            _apps,
            workspaceRepository.items,
            workspaceRepository.folders,
        ) { apps, items, folders ->
            LauncherUiState(
                apps = apps,
                workspaceCells = WorkspaceResolver.workspaceCells(items, apps, folders),
                dockApps = WorkspaceResolver.dockApps(items, apps),
                folders = folders.associateBy { it.id },
                folderContents = WorkspaceResolver.folderContents(items, apps),
            )
        }.stateIn(scope, SharingStarted.Eagerly, LauncherUiState())

    init {
        scope.launch { refresh() }
        scope.launch {
            packageEvents.collect { event ->
                if (event is PackageEvent.Removed) {
                    workspaceRepository.removeAllForPackage(event.packageName, event.userSerial)
                }
                refresh()
            }
        }
    }

    fun launch(app: LauncherApp) {
        if (!appLauncher.launch(app)) {
            // The icon was stale (app disabled/removed since the last discovery);
            // recover the list instead of leaving a dead icon around.
            scope.launch { refresh() }
        }
    }

    /** Drawer/app-source insert into an empty workspace [cell]. */
    fun insertWorkspaceAt(app: LauncherApp, cell: Int) {
        scope.launch { workspaceRepository.insertWorkspaceAt(app, cell) }
    }

    /** Drawer-source insert into the dock at [slot]; emits DOCK_FULL when full. */
    fun insertIntoDock(app: LauncherApp, slot: Int) {
        scope.launch {
            if (!workspaceRepository.insertIntoDock(app, slot)) {
                _messages.tryEmit(LauncherMessage.DOCK_FULL)
            }
        }
    }

    /** Creates a folder at [targetCell] from a drop of [dragged] onto its occupant. */
    fun createFolderFromDrop(dragged: LauncherApp, targetCell: Int, defaultTitle: String, fromWorkspaceCell: Int?) {
        scope.launch {
            workspaceRepository.createFolderFromDrop(dragged, targetCell, defaultTitle, fromWorkspaceCell)
        }
    }

    /** Adds a dragged app into an existing folder. */
    fun addToFolder(app: LauncherApp, folderId: Long, fromWorkspaceCell: Int?) {
        scope.launch { workspaceRepository.addToFolder(app, folderId, fromWorkspaceCell) }
    }

    /** Returns a member of [folderId] to the first free grid cell (dissolves sparse folders). */
    fun removeFromFolder(app: LauncherApp, folderId: Long) {
        scope.launch { workspaceRepository.removeFromFolder(app, folderId) }
    }

    /** Renames a folder. */
    fun renameFolder(folderId: Long, title: String) {
        scope.launch { workspaceRepository.renameFolder(folderId, title) }
    }

    /** Removes a folder, returning members to free grid cells. */
    fun removeFolder(folderId: Long) {
        scope.launch { workspaceRepository.removeFolder(folderId) }
    }

    /** Workspace → dock drag at [slot]; emits [LauncherMessage.DOCK_FULL] when full. */
    fun moveWorkspaceToDock(app: LauncherApp, fromCell: Int, slot: Int) {
        scope.launch {
            if (workspaceRepository.moveWorkspaceToDock(app, fromCell, slot) == DockMoveResult.FULL) {
                _messages.tryEmit(LauncherMessage.DOCK_FULL)
            }
        }
    }

    /** Dock → workspace drag onto an empty [cell]; no-op when the cell is occupied. */
    fun moveDockToWorkspace(app: LauncherApp, cell: Int) {
        scope.launch { workspaceRepository.moveDockToWorkspace(app, cell) }
    }

    /** Moves the selected workspace pin from [fromCell] to [toCell]. */
    fun moveWithinWorkspace(app: LauncherApp, fromCell: Int, toCell: Int) {
        scope.launch { workspaceRepository.moveWithinWorkspace(app, fromCell, toCell) }
    }

    /** Dock-internal reorder to [slot]. */
    fun reorderDock(app: LauncherApp, slot: Int) {
        scope.launch { workspaceRepository.reorderDock(app, slot) }
    }

    fun addToWorkspace(app: LauncherApp) {
        scope.launch {
            if (!workspaceRepository.addToWorkspace(app)) _messages.tryEmit(LauncherMessage.WORKSPACE_FULL)
        }
    }

    fun removeFromWorkspace(app: LauncherApp) {
        scope.launch { workspaceRepository.removeFromWorkspace(app) }
    }

    fun addToDock(app: LauncherApp) {
        scope.launch {
            if (!workspaceRepository.addToDock(app)) _messages.tryEmit(LauncherMessage.DOCK_FULL)
        }
    }

    fun removeFromDock(app: LauncherApp) {
        scope.launch { workspaceRepository.removeFromDock(app) }
    }

    /**
     * Refreshes launchable apps, seeds the layout on first run, and removes pins
     * for packages uninstalled while the launcher was not running.
     * Serialized with a mutex so overlapping package events cannot double-seed.
     */
    private suspend fun refresh() = refreshMutex.withLock {
        val apps = withContext(Dispatchers.IO) { discovery.discover() }
        _apps.value = apps
        if (!workspaceRepository.isSeeded() && apps.isNotEmpty()) {
            if (workspaceRepository.isEmpty()) {
                workspaceRepository.seed(SeedPlanner.plan(apps))
            } else {
                // A pre-existing layout (upgrading install): keep it, just adopt it.
                workspaceRepository.markSeeded()
            }
        }
        withContext(Dispatchers.IO) {
            workspaceRepository.pruneUninstalled(apps.mapTo(HashSet()) { it.key }, discovery::isPackageInstalled)
        }
    }
}
