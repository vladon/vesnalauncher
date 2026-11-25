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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.platform.AppDiscovery
import dev.vladon.vesna.launcher.platform.AppLauncher
import dev.vladon.vesna.launcher.platform.PackageEvent
import dev.vladon.vesna.launcher.platform.ShortcutPublisher
import java.util.Locale

/** Non-fatal feedback from launcher actions, mapped to user-visible strings by the UI. */
enum class LauncherMessage {
    WORKSPACE_FULL, DOCK_FULL, WORKSPACE_PAGE_NOT_EMPTY,
    SHORTCUT_MOVED, FOLDER_MOVED, NO_MOVE_DESTINATION, MOVE_DESTINATION_FULL, MOVE_UNAVAILABLE,
    WORKSPACE_PAGE_REORDERED, WORKSPACE_PAGE_ORDER_CHANGED,
    APP_INFO_UNAVAILABLE, SHORTCUT_UNAVAILABLE, SHORTCUT_ALREADY_PINNED,
    FOLDER_APPS_UNAVAILABLE,
}

/** Render-only page identity; never included in [LauncherUiState.pageIds] or persisted. */
internal const val NEW_WORKSPACE_PAGE_PREVIEW_ID = Long.MIN_VALUE

/** Everything the launcher UI renders. */
data class LauncherUiState(
    val apps: List<LauncherApp> = emptyList(),
    val workspaceCells: Map<Int, CellContent> = emptyMap(),
    val reservedWorkspaceCellsMask: Int = 0,
    val grid: GridCanvas = GridCanvas.DEFAULT,
    val dock: DockLayout = DockLayout(emptyList(), emptyList()),
    val folders: Map<Long, Folder> = emptyMap(),
    val folderContents: Map<Long, List<FolderPin>> = emptyMap(),
    /** Persisted workspace page IDs in display order. */
    val pageIds: List<Long> = emptyList(),
    val activePageId: Long = DEFAULT_WORKSPACE_PAGE_ID,
    val canRemoveActivePage: Boolean = false,
    internal val isDragPagePreview: Boolean = false,
    internal val newPagePreviewAfter: Long? = null,
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
    private val shortcutMutationMutex = Mutex()
    // Keep Android action pins alive across the Undo window even while their Room rows are absent.
    // A genuine package removal invalidates its snapshots, but completion still releases their pins.
    private val pendingFolderUnpins = mutableMapOf<RemovedFolder, Boolean>()
    private val shortcutRevision = MutableStateFlow(0)
    private var hadShortcutHostPermission = false
    // The ordered drawer list and the placement index come from the same discovery.
    private data class AppCatalogue(val apps: List<LauncherApp>) {
        val byKey = apps.associateBy { it.key }
    }

    private class LayoutProjection(
        val catalogue: AppCatalogue,
        val saved: SavedLayout,
        val renderedApps: Map<AppKey, LauncherApp>,
    ) {
        val dock = WorkspaceResolver.dockLayout(saved.items, renderedApps)
        val foldersById = saved.folders.associateBy { it.id }
        val folderContents = WorkspaceResolver.folderContents(saved.items, renderedApps)
    }

    private val appCatalogue = MutableStateFlow(AppCatalogue(emptyList()))

    /**
     * The catalogue the drawer shows. It starts on the fast phase-1 list (labels
     * with pending icons) and is replaced by the fully decoded one after the
     * first frame — the workspace never rebinds for it.
     */
    private val drawerCatalogue = MutableStateFlow<List<LauncherApp>>(emptyList())
    val drawerApps: StateFlow<List<LauncherApp>> = drawerCatalogue.asStateFlow()

    private sealed interface DragPagePreview {
        data class Existing(val pageId: Long) : DragPagePreview
        data class Append(val afterPageId: Long) : DragPagePreview
    }

    private data class PageSelection(val saved: Long, val dragPreview: DragPagePreview? = null)

    private val pageSelection = MutableStateFlow(PageSelection(workspaceRepository.savedActivePageId()))

    private val _messages = MutableSharedFlow<LauncherMessage>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages: SharedFlow<LauncherMessage> = _messages.asSharedFlow()

    // Page navigation changes the grid, not the global dock or saved folder contents.
    private val layout = combine(appCatalogue, workspaceRepository.savedLayout, shortcutRevision) { catalogue, saved, revision ->
        LayoutProjection(catalogue, saved, placementApps(catalogue, saved, revision))
    }

    private data class PinnedAppProjection(
        val catalogue: AppCatalogue,
        val savedLabels: Map<AppKey, String>,
        val revision: Int,
        val apps: Map<AppKey, LauncherApp>,
    )

    // Confined to the layout collector. Geometry and label edits should not reload publisher icons.
    private var pinnedAppProjection: PinnedAppProjection? = null

    private suspend fun placementApps(
        catalogue: AppCatalogue,
        saved: SavedLayout,
        revision: Int,
    ): Map<AppKey, LauncherApp> {
        val cached = pinnedAppProjection?.takeIf { it.catalogue === catalogue && it.revision == revision }
        var actionCount = 0
        var changed = false
        for (item in saved.items) {
            if (item.key.shortcutId == null || item.shortcutLabel == null) continue
            actionCount++
            if (cached?.savedLabels?.get(item.key) != item.shortcutLabel) changed = true
        }
        if (actionCount == 0) {
            pinnedAppProjection = null
            return catalogue.byKey
        }
        if (cached != null && !changed && cached.savedLabels.size == actionCount) return cached.apps
        val labels = buildMap {
            for (item in saved.items) {
                val label = item.shortcutLabel ?: continue
                if (item.key.shortcutId != null) putIfAbsent(item.key, label)
            }
        }
        val pinned = withContext(Dispatchers.IO) { appLauncher.shortcuts.resolvePinned(labels, catalogue.byKey) }
        val apps = if (pinned.isEmpty()) catalogue.byKey else catalogue.byKey + pinned
        pinnedAppProjection = PinnedAppProjection(catalogue, labels, revision, apps)
        return apps
    }

    val state: StateFlow<LauncherUiState> =
        combine(layout, pageSelection) { layout, pages ->
            val saved = layout.saved
            val orderedIds = saved.pageIds
            // The stored ID can outlive a deleted page if the process stopped
            // between the Room write and the preference update.
            val drag = pages.dragPreview
            val appendAfter = (drag as? DragPagePreview.Append)?.afterPageId?.takeIf { it in orderedIds }
            val preview = if (appendAfter != null) NEW_WORKSPACE_PAGE_PREVIEW_ID
            else (drag as? DragPagePreview.Existing)?.pageId?.takeIf { it in orderedIds }
            val selection = preview ?: pages.saved.takeIf { it in orderedIds }
                ?: orderedIds.firstOrNull() ?: DEFAULT_WORKSPACE_PAGE_ID
            val workspace = if (appendAfter != null) null else WorkspaceResolver.workspaceLayout(
                saved.items, layout.renderedApps, saved.folders, selection, saved.grid,
            )
            LauncherUiState(
                apps = layout.catalogue.apps,
                workspaceCells = workspace?.cells.orEmpty(),
                reservedWorkspaceCellsMask = workspace?.reservedCellsMask ?: 0,
                grid = saved.grid,
                dock = layout.dock,
                folders = layout.foldersById,
                folderContents = layout.folderContents,
                pageIds = orderedIds,
                activePageId = selection,
                canRemoveActivePage = appendAfter == null && selection != DEFAULT_WORKSPACE_PAGE_ID &&
                    saved.items.none { it.pageId == selection } && saved.folders.none { it.pageId == selection },
                isDragPagePreview = preview != null,
                newPagePreviewAfter = appendAfter,
            )
        }.stateIn(scope, SharingStarted.Eagerly, LauncherUiState())

    init {
        scope.launch { refresh() }
        recheckAppActions()
        scope.launch {
            packageEvents.collect { event ->
                if (event == PackageEvent.ShortcutsChanged) {
                    shortcutRevision.update { it + 1 }
                    return@collect
                }
                if (event is PackageEvent.Removed) {
                    shortcutMutationMutex.withLock {
                        for (entry in pendingFolderUnpins.entries) {
                            if (entry.key.members.any {
                                    it.packageName == event.packageName && it.userSerial == event.userSerial
                                }
                            ) entry.setValue(false)
                        }
                        workspaceRepository.removeAllForPackage(event.packageName, event.userSerial)
                    }
                }
                refresh(event as? PackageEvent.Added)
            }
        }
    }

    internal fun launch(app: LauncherApp): Boolean {
        if (app.key.shortcutId != null) {
            val launched = appLauncher.launch(app)
            if (!launched) shortcutRevision.update { it + 1 }
            return launched
        }
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
                state.first { pageId in it.pageIds }
                if (workspaceRepository.selectPage(pageId)) pageSelection.value = PageSelection(pageId)
            }
        }
    }

    /** Selects an existing page and persists the choice for the next process; a no-op for unknown IDs. */
    fun selectWorkspacePage(pageId: Long) {
        scope.launch {
            pageActionMutex.withLock {
                if (workspaceRepository.selectPage(pageId)) pageSelection.value = PageSelection(pageId)
            }
        }
    }

    /** Drag hover changes what Home renders, not the page restored on a cold start. */
    internal fun previewWorkspacePageForDrag(pageId: Long) {
        pageSelection.update { it.copy(dragPreview = DragPagePreview.Existing(pageId)) }
    }

    internal fun previewNewWorkspacePageForDrag(afterPageId: Long) {
        pageSelection.update { it.copy(dragPreview = DragPagePreview.Append(afterPageId)) }
    }

    internal fun cancelWorkspacePagePreview() {
        pageSelection.update { if (it.dragPreview == null) it else it.copy(dragPreview = null) }
    }

    /** Commit the preview's page only after the exact drop has succeeded. */
    internal suspend fun finishWorkspacePagePreview(pageId: Long) = pageActionMutex.withLock {
        if ((pageSelection.value.dragPreview as? DragPagePreview.Existing)?.pageId == pageId &&
            workspaceRepository.selectPage(pageId)
        ) {
            pageSelection.value = PageSelection(pageId)
        }
    }

    internal suspend fun finishNewWorkspacePagePreview(
        pinId: Long,
        sourcePageId: Long,
        sourceCell: Int,
        cell: Int,
        grid: GridCanvas,
    ): Boolean = commitNewWorkspacePagePreview { afterPageId ->
        workspaceRepository.appendPageWithWorkspacePin(pinId, sourcePageId, sourceCell, afterPageId, cell, grid)
    }

    internal suspend fun finishNewWorkspacePagePreviewFromFolder(
        pinId: Long,
        folderId: Long,
        sourcePageId: Long,
        cell: Int,
        grid: GridCanvas,
    ): Boolean = commitNewWorkspacePagePreview { afterPageId ->
        workspaceRepository.appendPageWithFolderMember(pinId, folderId, sourcePageId, afterPageId, cell, grid)
    }

    /** Materializes the trailing preview and moves the exact closed folder in one commit. */
    internal suspend fun finishNewWorkspacePagePreviewWithFolder(
        folderId: Long,
        sourcePageId: Long,
        sourceCell: Int,
        cell: Int,
        grid: GridCanvas,
    ): Boolean = commitNewWorkspacePagePreview { afterPageId ->
        workspaceRepository.appendPageWithFolder(folderId, sourcePageId, sourceCell, afterPageId, cell, grid)
    }

    private suspend inline fun commitNewWorkspacePagePreview(append: (Long) -> Long?): Boolean =
        pageActionMutex.withLock {
            val preview = pageSelection.value.dragPreview as? DragPagePreview.Append ?: return@withLock false
            val pageId = append(preview.afterPageId) ?: return@withLock false
            if (workspaceRepository.selectPage(pageId)) pageSelection.value = PageSelection(pageId)
            true
        }

    /** Removes the captured page only if it is still active and has no saved content. */
    fun removeEmptyWorkspacePage(pageId: Long) {
        scope.launch {
            pageActionMutex.withLock {
                if (pageId == DEFAULT_WORKSPACE_PAGE_ID || pageSelection.value.saved != pageId) return@withLock
                if (workspaceRepository.removeEmptyPage(pageId)) {
                    pageSelection.value = PageSelection(DEFAULT_WORKSPACE_PAGE_ID)
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
                val moved = pageSelection.value.saved == pageId &&
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

    /** Awaits an edge-preview drop so Home keeps the preview until the folder change is saved. */
    internal suspend fun groupWithCell(
        dragged: LauncherApp,
        targetCell: Int,
        defaultTitle: String,
        source: WorkspaceRepository.FolderDropSource,
        pageId: Long,
    ): Boolean = workspaceRepository.createFolderFromDrop(dragged, targetCell, defaultTitle, source, pageId)

    internal suspend fun joinFolder(
        app: LauncherApp,
        folderId: Long,
        source: WorkspaceRepository.FolderDropSource,
    ): Boolean = workspaceRepository.addToFolder(app, folderId, source)

    internal fun addAppsToFolder(folder: Folder, keys: List<AppKey>) {
        scope.launch {
            val catalogue = appCatalogue.value.byKey
            val apps = keys.mapNotNull(catalogue::get)
            if (apps.size != keys.size ||
                !workspaceRepository.addAppsToFolder(folder.id, folder.pageId, folder.cellIndex, apps)
            ) {
                _messages.tryEmit(LauncherMessage.FOLDER_APPS_UNAVAILABLE)
            }
        }
    }

    internal suspend fun reorderFolderMember(folderId: Long, pageId: Long, pinId: Long, targetId: Long): Boolean =
        workspaceRepository.reorderFolderMember(folderId, pageId, pinId, targetId)

    internal fun sortFolderMembers(folder: Folder, locale: Locale) {
        val members = state.value.folderContents[folder.id] ?: return
        if (members.size < 2) return
        val labels = buildMap(members.size) {
            for (member in members) put(member.id, member.app.label)
        }
        scope.launch { workspaceRepository.sortFolderMembers(folder.id, folder.pageId, labels, locale) }
    }

    internal suspend fun moveFolderMemberToCell(
        pinId: Long,
        folderId: Long,
        pageId: Long,
        cell: Int,
        destinationPageId: Long = pageId,
    ): Boolean = workspaceRepository.moveFolderMemberToCell(pinId, folderId, pageId, cell, destinationPageId)

    internal suspend fun moveWorkspacePinToCell(
        pinId: Long,
        sourcePageId: Long,
        sourceCell: Int,
        destinationPageId: Long,
        destinationCell: Int,
    ): Boolean =
        workspaceRepository.moveWorkspacePinToCell(pinId, sourcePageId, sourceCell, destinationPageId, destinationCell)

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

    internal suspend fun unpinFolderWithUndo(folder: Folder): RemovedFolder? = withContext(Dispatchers.IO) {
        shortcutMutationMutex.withLock {
            val removed = workspaceRepository.unpinFolder(folder) ?: return@withLock null
            pendingFolderUnpins[removed] = true
            removed
        }
    }

    internal suspend fun finishFolderUnpin(removed: RemovedFolder, undo: Boolean): FolderRestoreResult? =
        withContext(Dispatchers.IO) {
            shortcutMutationMutex.withLock {
                val restorable = pendingFolderUnpins.remove(removed) ?: return@withLock null
                try {
                    when {
                        !undo -> null
                        restorable -> workspaceRepository.restoreFolder(removed)
                        else -> FolderRestoreResult.UNAVAILABLE
                    }
                } finally {
                    val publishers = buildSet {
                        for (member in removed.members) {
                            if (member.shortcutId != null) add(ShortcutPublisher(member.packageName, member.userSerial))
                        }
                    }
                    for (publisher in publishers) syncAndroidPins(publisher)
                }
            }
        }

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

    internal suspend fun moveFolderToCell(folder: Folder, pageId: Long, cell: Int, grid: GridCanvas): Boolean =
        workspaceRepository.moveFolderToCell(folder, pageId, cell, grid)

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

    internal suspend fun appActions(app: LauncherApp): List<LauncherApp> =
        withContext(Dispatchers.IO) { appLauncher.shortcuts.published(app) }

    internal fun pinAppAction(action: LauncherApp, pageId: Long) {
        val shortcutId = action.key.shortcutId ?: return
        val publisher = ShortcutPublisher(action.key)
        scope.launch(Dispatchers.IO) {
            val result = shortcutMutationMutex.withLock {
                workspaceRepository.pinAppAction(action, pageId) { ids ->
                    appLauncher.shortcuts.replacePins(publisher, retainUndoPins(publisher, ids), shortcutId.takeIf { it in ids })
                }
            }
            when (result) {
                AppActionPinResult.PINNED -> shortcutRevision.update { it + 1 }
                AppActionPinResult.FULL -> _messages.tryEmit(LauncherMessage.WORKSPACE_FULL)
                AppActionPinResult.ALREADY_PINNED -> _messages.tryEmit(LauncherMessage.SHORTCUT_ALREADY_PINNED)
                AppActionPinResult.UNAVAILABLE -> _messages.tryEmit(LauncherMessage.SHORTCUT_UNAVAILABLE)
            }
        }
    }

    internal suspend fun workspacePinId(app: LauncherApp, cell: Int, pageId: Long): Long? =
        workspaceRepository.workspacePinId(app.key, cell, pageId)

    internal suspend fun renameWorkspacePin(pinId: Long, cell: Int, pageId: Long, label: String?): Boolean =
        workspaceRepository.renameWorkspacePin(pinId, cell, pageId, label)

    internal fun recheckAppActions() {
        scope.launch(Dispatchers.IO) {
            val changed = shortcutMutationMutex.withLock {
                val allowed = appLauncher.shortcuts.canQuery()
                if (hadShortcutHostPermission == allowed) return@withLock false
                hadShortcutHostPermission = allowed
                if (allowed) {
                    // Reconcile a prior interruption between Android and Room, or a removal while
                    // another launcher held the Home role. Only our own Android pins are touched.
                    for (publisher in appLauncher.shortcuts.pinnedPublishers()) syncAndroidPins(publisher)
                }
                true
            }
            if (changed) shortcutRevision.update { it + 1 }
        }
    }

    private suspend fun syncAndroidPins(publisher: ShortcutPublisher) {
        val ids = workspaceRepository.shortcutIds(publisher.packageName, publisher.userSerial)
        appLauncher.shortcuts.replacePins(publisher, retainUndoPins(publisher, ids))
    }

    /** Called under [shortcutMutationMutex] for every Android publisher replacement. */
    private fun retainUndoPins(publisher: ShortcutPublisher, ids: Set<String>): Set<String> {
        var retained: MutableSet<String>? = null
        for (snapshot in pendingFolderUnpins.keys) {
            for (member in snapshot.members) {
                val id = member.shortcutId ?: continue
                if (member.packageName == publisher.packageName && member.userSerial == publisher.userSerial && id !in ids) {
                    (retained ?: ids.toMutableSet().also { retained = it }).add(id)
                }
            }
        }
        return retained ?: ids
    }

    fun removeFromWorkspace(app: LauncherApp, cell: Int, pageId: Long) {
        scope.launch(Dispatchers.IO) {
            if (app.key.shortcutId == null) {
                pageActionMutex.withLock {
                    if (workspaceRepository.removeFromWorkspace(app, cell, pageId) && pageSelection.value.saved == pageId) {
                        pageSelection.value = PageSelection(DEFAULT_WORKSPACE_PAGE_ID)
                    }
                }
            } else {
                shortcutMutationMutex.withLock {
                    workspaceRepository.removeFromWorkspace(app, cell, pageId)
                    syncAndroidPins(ShortcutPublisher(app.key))
                }
            }
        }
    }

    fun addToDock(app: LauncherApp) {
        scope.launch {
            if (!workspaceRepository.addToDock(app)) _messages.tryEmit(LauncherMessage.DOCK_FULL)
        }
    }

    fun removeFromDock(pinId: Long) {
        val key = state.value.dock.pins.firstOrNull { it.id == pinId }?.app?.key
        scope.launch(Dispatchers.IO) {
            if (key?.shortcutId == null) {
                workspaceRepository.removeFromDock(pinId)
            } else {
                shortcutMutationMutex.withLock {
                    workspaceRepository.removeFromDock(pinId)
                    syncAndroidPins(ShortcutPublisher(key))
                }
            }
        }
    }

    /** A locale change updates app metadata, not the saved layout. */
    internal fun refreshLocalizedApps() {
        scope.launch {
            refreshMutex.withLock {
                val catalogue = discoverApps()
                appCatalogue.value = catalogue
                drawerCatalogue.value = catalogue.apps
            }
        }
    }

    /** An icon-pack change updates app artwork, not the saved layout. */
    internal fun refreshAppArtwork() {
        scope.launch {
            refreshMutex.withLock {
                val catalogue = discoverApps()
                appCatalogue.value = catalogue
                drawerCatalogue.value = catalogue.apps
            }
        }
    }

    private suspend fun discoverApps(decodeIconsFor: Set<AppKey>? = null): AppCatalogue =
        withContext(Dispatchers.IO) { AppCatalogue(discovery.discover(decodeIconsFor)) }

    /**
     * Refreshes launchable apps, seeds the layout on first run, and removes pins
     * for packages uninstalled while the launcher was not running.
     * Serialized with a mutex so overlapping package events cannot double-seed;
     * [WorkspaceRepository.seedOrAdopt] additionally keeps the adopt-or-seed
     * decision atomic against pins placed before the first seed completes.
     *
     * Two phases: the first catalogue decodes icons only for the saved layout
     * (the tiles the first frame shows) so Home renders without waiting for one
     * launcher IPC per app; the drawer's icons then arrive in a second pass.
     */
    private suspend fun refresh(added: PackageEvent.Added? = null): Unit = refreshMutex.withLock {
        // The saved-layout flow is collected eagerly from process start, so its first
        // value is usually ready already; the fast catalogue decodes icons only for
        // the tiles it shows and the drawer's icons arrive in the second pass.
        val saved = workspaceRepository.savedLayout.first()
        val savedKeys = saved.items.mapTo(HashSet()) { it.key }
        val catalogue = discoverApps(savedKeys)
        appCatalogue.value = catalogue
        drawerCatalogue.value = catalogue.apps
        if (!workspaceRepository.isSeeded() && catalogue.apps.isNotEmpty()) {
            workspaceRepository.seedOrAdopt(catalogue.apps)
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
        val deferred = catalogue.apps.count { it.icon == null }
        if (deferred > 0) {
            // Let the fast catalogue propagate to Home before the slower full decode.
            // Only the drawer takes the fully decoded list — the workspace keeps the
            // fast one, so its tiles never rebind for artwork alone.
            yield()
            drawerCatalogue.value = discoverApps(decodeIconsFor = null).apps
        }
    }
}
