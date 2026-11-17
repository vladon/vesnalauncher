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
    val workspaceApps: Map<Int, LauncherApp> = emptyMap(),
    val dockApps: List<LauncherApp> = emptyList(),
) {
    val isWorkspaceEmpty: Boolean get() = workspaceApps.isEmpty()
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
        combine(_apps, workspaceRepository.items) { apps, items ->
            LauncherUiState(
                apps = apps,
                workspaceApps = WorkspaceResolver.workspaceApps(items, apps),
                dockApps = WorkspaceResolver.dockApps(items, apps),
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

    /** Persists a drag-and-drop result: move to an empty cell or swap with its occupant. */
    fun moveWithinWorkspace(app: LauncherApp, toCell: Int) {
        scope.launch { workspaceRepository.moveWithinWorkspace(app, toCell) }
    }

    /** Workspace → dock drag at [slot]; emits [LauncherMessage.DOCK_FULL] when full. */
    fun moveWorkspaceToDock(app: LauncherApp, slot: Int) {
        scope.launch {
            if (!workspaceRepository.moveWorkspaceToDock(app, slot)) {
                _messages.tryEmit(LauncherMessage.DOCK_FULL)
            }
        }
    }

    /** Dock → workspace drag onto an empty [cell]; no-op when the cell is occupied. */
    fun moveDockToWorkspace(app: LauncherApp, cell: Int) {
        scope.launch { workspaceRepository.moveDockToWorkspace(app, cell) }
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
     * Re-reads the installed app list, seeds the layout on first run, and prunes
     * entries of apps that disappeared while the launcher was not running.
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
        workspaceRepository.pruneStale(apps.map { it.key }.toSet())
    }
}
