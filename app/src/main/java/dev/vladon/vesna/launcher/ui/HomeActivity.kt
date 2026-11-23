package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.VesnaApplication
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.settings.AppearancePreferences
import dev.vladon.vesna.launcher.settings.DrawerPreferences
import dev.vladon.vesna.launcher.settings.WorkspacePreferences
import dev.vladon.vesna.launcher.settings.ThemeMode
import dev.vladon.vesna.launcher.settings.VesnaTheme
import dev.vladon.vesna.launcher.workspace.CellContent
import dev.vladon.vesna.launcher.workspace.Container
import dev.vladon.vesna.launcher.workspace.Folder
import dev.vladon.vesna.launcher.workspace.FolderRemovalResult
import dev.vladon.vesna.launcher.workspace.LauncherMessage
import dev.vladon.vesna.launcher.workspace.LauncherUiState
import dev.vladon.vesna.launcher.workspace.MAX_WORKSPACE_PAGES
import dev.vladon.vesna.launcher.workspace.PageMoveOptions
import dev.vladon.vesna.launcher.workspace.WorkspaceRepository.FolderDropSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val DRAWER_OPEN_STATE = "drawer_open"

/**
 * The HOME activity: hosts workspace, dock, and drawer, and opens the launcher
 * and app-item context menus.
 */
class HomeActivity : ComponentActivity() {

    private lateinit var workspace: WorkspaceView
    private lateinit var dock: DockView
    private lateinit var drawer: DrawerView
    private lateinit var homeContent: FrameLayout
    private lateinit var homeControls: View
    private lateinit var homeOptionsButton: View
    private lateinit var openDrawerButton: View
    private lateinit var emptyHint: TextView
    private lateinit var pageControls: View
    private lateinit var previousPage: View
    private lateinit var nextPage: View
    private lateinit var pageIndicator: WorkspacePageIndicator

    /** The icon currently being dragged across workspace and dock. */
    private data class DragSource(
        val app: LauncherApp,
        val container: Container,
        val fromCell: Int,
        val pageId: Long,
        val fromDrawer: Boolean,
        val dockPinId: Long = 0,
    )

    private var dragSource: DragSource? = null
    private var rootDragMoved = false
    private var dockBottomInset = 0
    private var drawerReturnFocus: View? = null
    private var activePopup: VesnaPopupMenu? = null
    private var pendingPageMenu: Job? = null
    private var folderDialog: FolderDialog? = null
    private data class FolderMemberDrag(val folderId: Long, val pinId: Long, val pageId: Long)

    private var folderMemberDrag: FolderMemberDrag? = null
    private val workspaceScreenLocation = IntArray(2)

    /** The open rename editor: a folder title, or a per-pin label override on Home. */
    private sealed interface HomeRename {
        data class Folder(val folderId: Long, val editor: RenameEditorState) : HomeRename

        data class Pin(
            val pinId: Long,
            val cell: Int,
            val pageId: Long,
            val key: AppKey,
            val savedLabel: String,
            val editor: RenameEditorState,
        ) : HomeRename
    }

    private sealed interface ShortcutFocus {
        data class WorkspaceApp(val pageId: Long, val cell: Int, val key: AppKey) : ShortcutFocus
        data class WorkspaceFolder(val pageId: Long, val cell: Int, val id: Long) : ShortcutFocus
        data class Dock(val id: Long) : ShortcutFocus
    }

    private class HomeUiState : ViewModel() {
        var shortcutFocus: ShortcutFocus? = null
        var drawerAppFocus: AppKey? = null
        var openFolderId: Long? = null
        var focusedMemberId: Long? = null
        var rename: HomeRename? = null
    }

    private val homeUiState: HomeUiState by viewModels {
        viewModelFactory { initializer { HomeUiState() } }
    }
    private var pendingShortcutFocus: ShortcutFocus? = null
    private var renameHost: ComposeView? = null
    private var snackbarJob: Job? = null
    private val homeSnackbar by lazy { createHomeSnackbar() }
    private var renderedState = LauncherUiState()
    private val workspacePreferences by lazy { WorkspacePreferences(this) }
    private val drawerPreferences by lazy { DrawerPreferences(this) }
    private var hiddenDrawerApps = emptySet<AppKey>()
    private var layoutLocked = false
    private val workspacePreferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == WorkspacePreferences.LAYOUT_LOCKED) applyWorkspaceLock()
    }
    private lateinit var appearance: AppearancePreferences
    private lateinit var themeMode: ThemeMode
    private var showAppNames = true

    private val controller
        get() = VesnaApplication.graph(this).controller

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            cancelDrawerGesture()
            drawer.close()
        }
    }

    private val defaultHomeRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun attachBaseContext(newBase: Context) {
        appearance = AppearancePreferences(newBase)
        themeMode = appearance.themeMode
        val night = themeMode.nightOverride
        // Override only the night bits; locale, density and other changes still come from Android.
        val context = if (night == null) newBase else newBase.createConfigurationContext(
            Configuration().apply { uiMode = night },
        )
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingShortcutFocus = homeUiState.shortcutFocus
        homeUiState.shortcutFocus = null
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applySystemBarAppearance(drawerOpened = false)
        setContentView(R.layout.activity_home)

        workspace = findViewById(R.id.workspace)
        dock = findViewById(R.id.dock)
        drawer = findViewById(R.id.drawer)
        applyAppNameVisibility(appearance.showAppNames)
        homeContent = findViewById(R.id.home_content)
        homeControls = findViewById(R.id.home_controls)
        homeOptionsButton = findViewById(R.id.home_options)
        homeOptionsButton.setOnClickListener { showLauncherMenu() }
        openDrawerButton = findViewById(R.id.home_open_drawer)
        openDrawerButton.setOnClickListener { openDrawer() }
        emptyHint = findViewById(R.id.workspace_empty_hint)
        pageControls = findViewById(R.id.workspace_page_controls)
        previousPage = findViewById(R.id.workspace_previous_page)
        nextPage = findViewById(R.id.workspace_next_page)
        pageIndicator = findViewById(R.id.workspace_page_indicator)
        previousPage.setOnClickListener { stepPage(forward = false) }
        nextPage.setOnClickListener { stepPage(forward = true) }
        pageIndicator.setOnClickListener { showPagePicker(pageIndicator) }
        ViewCompat.replaceAccessibilityAction(pageIndicator, ACTION_CLICK, getString(R.string.workspace_choose_page)) {
            view, _ -> view.performClick()
        }

        val graph = VesnaApplication.graph(this)
        val controller = graph.controller

        workspace.isLayoutLocked = { layoutLocked }
        dock.isLayoutLocked = { layoutLocked }
        drawer.isLayoutLocked = { layoutLocked }

        workspace.onAppClick = { launchApp(it) }
        workspace.onAppOptions = { cell, app, anchor ->
            showWorkspaceItemMenu(anchor, app, cell, renderedState.activePageId)
        }
        workspace.onCellClick = { content ->
            if (content is CellContent.FolderCell) showFolderDialog(content.folder)
        }
        workspace.onFolderLongClick = { folder, anchor -> showFolderMenu(folder, anchor) }
        workspace.onFolderDrop = controller::moveFolderWithinPage
        workspace.onEmptyAreaLongClick = {
            if (layoutLocked) showSnackbar(R.string.workspace_layout_locked, null)
            else showLauncherMenu()
        }
        workspace.onSwipeUp = ::openDrawer
        workspace.onSwipePage = ::stepPage
        workspace.onDragStarted = { cell, app, _ ->
            dragSource = DragSource(app, Container.WORKSPACE, cell, renderedState.activePageId, fromDrawer = false)
        }
        workspace.onDragProgress = { x, y -> updateDragTarget(x + workspace.left, y + workspace.top) }
        workspace.onDragReleased = { x, y, moved, cancelled, app, anchor ->
            onDragReleased(Container.WORKSPACE, x + workspace.left, y + workspace.top, moved, cancelled, app, anchor)
        }

        dock.onAppClick = { launchApp(it) }
        dock.onAppOptions = { pin, anchor -> showDockItemMenu(anchor, pin.app, pin.id) }
        dock.onDragStarted = { pin, _ ->
            dragSource = DragSource(
                pin.app, Container.DOCK, -1, renderedState.activePageId,
                fromDrawer = false, dockPinId = pin.id,
            )
        }
        dock.onDragProgress = { x, y -> updateDragTarget(x + dock.left, y + dock.top) }
        dock.onDragReleased = { x, y, moved, cancelled, app, anchor ->
            onDragReleased(Container.DOCK, x + dock.left, y + dock.top, moved, cancelled, app, anchor)
        }

        drawer.onAppClick = ::launchApp
        drawer.closeOnLaunch = { drawerPreferences.closeOnLaunch }
        drawer.onItemOptions = { app, anchor -> showDrawerItemMenu(anchor, app, renderedState.activePageId) }
        drawer.onItemDragStarted = { app, _ ->
            dragSource = DragSource(app, Container.WORKSPACE, -1, renderedState.activePageId, fromDrawer = true)
        }
        drawer.onItemDragReleased = { app, anchor ->
            onDragReleased(Container.WORKSPACE, anchor.left.toFloat(), anchor.top.toFloat(), moved = false, cancelled = false, app, anchor)
        }
        drawer.onOpenedChanged = ::onDrawerOpenedChanged

        val dragLayer = findViewById<LauncherDragLayer>(R.id.launcher_root)
        dragLayer.isDragActive = { dragSource != null || workspace.isDragging }
        dragLayer.isDrawerHoldArmed = { dragSource?.fromDrawer == true }
        dragLayer.dragTouchHandler = drawerDrag@{ ev ->
            if (dragSource?.fromDrawer != true) return@drawerDrag
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    if (!rootDragMoved) {
                        rootDragMoved = true
                        drawer.close()
                    }
                    updateDragTarget(ev.x, ev.y)
                }
                MotionEvent.ACTION_UP -> onRootDragReleased(ev.x, ev.y)
                MotionEvent.ACTION_CANCEL -> onRootDragCancelled()
            }
        }

        onBackPressedDispatcher.addCallback(this, backCallback)

        applySystemBarInsets()

        workspacePreferences.registerListener(workspacePreferenceListener)
        applyWorkspaceLock()
        hiddenDrawerApps = drawerPreferences.hiddenApps
        drawer.layoutMode = drawerPreferences.layoutMode

        // Open before hierarchy restoration so Android can restore the search text and focus.
        if (savedInstanceState?.getBoolean(DRAWER_OPEN_STATE) == true) drawer.open()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    controller.state.collect { render(it) }
                }
                launch {
                    controller.messages.collect { message -> showMessage(message) }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(DRAWER_OPEN_STATE, drawer.isOpen)
        drawer.saveResultViewport(outState)
        homeUiState.drawerAppFocus = drawer.focusedAppKey()
        homeUiState.shortcutFocus = focusedHomeShortcut()
        folderDialog?.let { homeUiState.focusedMemberId = it.focusedMemberId() }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Resolve the viewport and focused result after Android restores the search field.
        val focus = homeUiState.drawerAppFocus
        homeUiState.drawerAppFocus = null
        drawer.restoreResults(savedInstanceState, focus)
    }

    override fun onResume() {
        super.onResume()
        // Settings can change the preference while Home is on the back stack.
        if (appearance.themeMode != themeMode) {
            recreate()
            return
        }
        applyAppNameVisibility(appearance.showAppNames)
        drawer.layoutMode = drawerPreferences.layoutMode
        val hiddenApps = drawerPreferences.hiddenApps
        if (hiddenApps != hiddenDrawerApps) {
            hiddenDrawerApps = hiddenApps
            drawer.submitApps(renderedState.apps, hiddenDrawerApps)
        }
        // The Home role can change while Home is on the back stack; reconcile
        // the saved action pins with what Android still reports.
        controller.recheckAppActions()
    }

    override fun onDestroy() {
        workspacePreferences.unregisterListener(workspacePreferenceListener)
        super.onDestroy()
    }

    private fun applyWorkspaceLock() {
        val locked = workspacePreferences.layoutLocked
        if (layoutLocked == locked) return
        layoutLocked = locked
        activePopup?.dismiss()
        pendingPageMenu?.cancel()
        if (locked) {
            workspace.cancelDrag()
            dock.cancelDrag()
            cancelDrawerGesture()
            dismissHomeRename()
        }
        folderDialog?.layoutLocked = locked
        emptyHint.setText(if (locked) R.string.workspace_layout_locked else R.string.workspace_empty_hint)
    }

    override fun onStop() {
        folderDialog?.cancelMemberDrag()
        snackbarJob?.cancel()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        cancelDrawerGesture()
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME) || intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            snackbarJob?.cancel()
            pendingPageMenu?.cancel()
            pendingPageMenu = null
            activePopup?.dismiss()
            dismissHomeRename()
            dismissFolderDialog()
        }
        drawer.close()
    }

    private fun showPopup(popup: VesnaPopupMenu) {
        activePopup?.dismiss()
        activePopup = popup
        popup.show { if (activePopup === popup) activePopup = null }
    }

    private fun schedulePageMenu(show: suspend () -> Unit) {
        pendingPageMenu?.cancel()
        // Let the invoking popup finish handling its click before opening the next one.
        pendingPageMenu = lifecycleScope.launch(Dispatchers.Main) { show() }
    }

    private fun applyAppNameVisibility(visible: Boolean) {
        if (showAppNames == visible) return
        showAppNames = visible
        workspace.showAppNames = visible
        dock.showAppNames = visible
        drawer.showAppNames = visible
        folderDialog?.showAppNames = visible
    }

    private fun render(state: LauncherUiState) {
        val previous = renderedState
        val gridChanged = state.grid != previous.grid
        if (state.activePageId != previous.activePageId) {
            workspace.cancelDrag()
            onRootDragCancelled()
            if (renameHost != null) dismissHomeRename()
            folderDialog?.dismiss()
        }
        if (gridChanged) {
            // Resizing invalidates every coordinate a live drag aims at, and
            // moves anything a popup or draft is anchored to.
            workspace.cancelDrag()
            onRootDragCancelled()
            activePopup?.dismiss()
            pendingPageMenu?.cancel()
            dismissHomeRename()
            folderDialog?.dismiss()
        }
        renderedState = state
        workspace.reservedCellsMask = state.reservedWorkspaceCellsMask
        // A geometry-only change rebinds the same cells so the grid still resizes.
        if (gridChanged || state.workspaceCells != previous.workspaceCells) {
            workspace.bind(state.grid, state.workspaceCells)
        }
        if (state.dock.pins != previous.dock.pins) dock.bind(state.dock.pins)
        if (state.apps != previous.apps) updateAppCatalogue(state.apps)
        emptyHint.isVisible = state.isWorkspaceEmpty
        if (state.pageIds != previous.pageIds || state.activePageId != previous.activePageId) {
            val position = state.pageIds.indexOf(state.activePageId)
            val hadPreviousFocus = previousPage.hasFocus()
            val hadNextFocus = nextPage.hasFocus()
            val hadIndicatorFocus = pageIndicator.hasFocus()
            pageControls.isVisible = state.pageIds.size > 1
            previousPage.isEnabled = position > 0
            nextPage.isEnabled = position in 0 until state.pageIds.lastIndex
            previousPage.alpha = if (previousPage.isEnabled) 1f else 0.38f
            nextPage.alpha = if (nextPage.isEnabled) 1f else 0.38f
            if (pageControls.isVisible) {
                if (hadPreviousFocus && !previousPage.isEnabled) nextPage.requestFocus()
                if (hadNextFocus && !nextPage.isEnabled) previousPage.requestFocus()
            } else if (hadPreviousFocus || hadNextFocus || hadIndicatorFocus) {
                openDrawerButton.requestFocus()
            }
            pageIndicator.bind(position, state.pageIds.size)
        }
        renderFolderUi(state)
        restoreHomeShortcut(state)
    }

    internal fun updateAppCatalogue(apps: List<LauncherApp>) {
        val source = dragSource
        if (source?.fromDrawer == true && apps.none { it.key == source.app.key }) {
            // The root may already own the touch stream; losing a drawer tile
            // cannot cancel that stream on its own.
            onRootDragCancelled()
        }
        drawer.submitApps(apps, hiddenDrawerApps)
    }

    private fun focusedHomeShortcut(): ShortcutFocus? {
        if (drawer.isOpen || folderDialog != null || renameHost != null) return null
        val state = renderedState
        val cell = workspace.focusedChild?.tag as? Int
        if (cell != null) {
            return when (val content = state.workspaceCells[cell]) {
                is CellContent.App -> ShortcutFocus.WorkspaceApp(state.activePageId, cell, content.app.key)
                is CellContent.FolderCell -> ShortcutFocus.WorkspaceFolder(state.activePageId, cell, content.folder.id)
                null -> null
            }
        }
        val tile = dock.focusedChild ?: return null
        return state.dock.pins.getOrNull(dock.indexOfChild(tile))?.let { ShortcutFocus.Dock(it.id) }
    }

    private fun restoreHomeShortcut(state: LauncherUiState) {
        val focus = pendingShortcutFocus ?: return
        if (state.pageIds.isEmpty()) return
        pendingShortcutFocus = null
        requestHomeShortcutFocus(focus)
    }

    private fun requestHomeShortcutFocus(focus: ShortcutFocus) {
        // On recreation or dialog dismissal, wait for the underlying Home layout.
        homeContent.doOnPreDraw {
            if (drawer.isOpen || folderDialog != null || renameHost != null) return@doOnPreDraw
            val current = renderedState
            val tile = when (focus) {
                is ShortcutFocus.WorkspaceApp -> workspace.findChildForCell(focus.cell).takeIf {
                    current.activePageId == focus.pageId &&
                        (current.workspaceCells[focus.cell] as? CellContent.App)?.app?.key == focus.key
                }
                is ShortcutFocus.WorkspaceFolder -> workspace.findChildForCell(focus.cell).takeIf {
                    current.activePageId == focus.pageId &&
                        (current.workspaceCells[focus.cell] as? CellContent.FolderCell)?.folder?.id == focus.id
                }
                is ShortcutFocus.Dock -> dock.getChildAt(current.dock.pins.indexOfFirst { it.id == focus.id })
            }
            tile?.requestFocus()
        }
    }

    private fun openDrawer() {
        if (dragSource == null && !workspace.isDragging) drawer.open()
    }

    private fun onDrawerOpenedChanged(opened: Boolean) {
        // A drag hides the drawer itself, but Back should still cancel it.
        if (!opened && !rootDragMoved) cancelDrawerGesture()
        backCallback.isEnabled = opened || rootDragMoved
        applySystemBarAppearance(drawerOpened = opened)
        if (opened) drawerReturnFocus = homeContent.findFocus()
        // The drawer animates over Home, but only the active surface handles
        // keyboard traversal and accessibility navigation.
        homeContent.descendantFocusability =
            if (opened) FrameLayout.FOCUS_BLOCK_DESCENDANTS else FrameLayout.FOCUS_BEFORE_DESCENDANTS
        homeContent.importantForAccessibility =
            if (opened) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_NO
        homeControls.visibility = if (opened) View.INVISIBLE else View.VISIBLE
        if (!opened) {
            val target = drawerReturnFocus
            drawerReturnFocus = null
            if (target?.isShown != true || !target.requestFocus()) openDrawerButton.requestFocus()
        }
    }

    private fun applySystemBarAppearance(drawerOpened: Boolean) {
        val lightDrawer = drawerOpened &&
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightDrawer
            isAppearanceLightNavigationBars = lightDrawer
        }
    }

    private fun stepPage(forward: Boolean) {
        if (dragSource != null || workspace.isDragging || backCallback.isEnabled) return
        val state = renderedState
        val current = state.pageIds.indexOf(state.activePageId)
        if (current < 0) return
        val step = if (forward) 1 else -1
        state.pageIds.getOrNull(current + step)?.let(controller::selectWorkspacePage)
    }

    private fun showPagePicker(anchor: View) {
        if (dragSource != null || workspace.isDragging || backCallback.isEnabled || !anchor.isShown) return
        val state = renderedState
        val activeIndex = state.pageIds.indexOf(state.activePageId)
        if (state.pageIds.size < 2 || activeIndex < 0) return

        val popup = VesnaPopupMenu(anchor)
        state.pageIds.forEachIndexed { index, pageId ->
            val number = index + 1
            popup.add(
                label = getString(R.string.workspace_page_picker_label, number),
                description = getString(R.string.workspace_page_number, number),
                checked = index == activeIndex,
            ) {
                if (pageId != renderedState.activePageId) controller.selectWorkspacePage(pageId)
            }
        }
        showPopup(popup)
    }

    /**
     * Computes and previews the drop target in launcher-root coordinates. The
     * dock zone wins when the finger is over it; otherwise the workspace
     * converts the point into its local cell coordinates.
     */
    private fun updateDragTarget(x: Float, y: Float) {
        val source = dragSource ?: return
        if (y >= dock.top) {
            workspace.showDropHighlight(null)
            dock.showSlotHighlight(if (withinDockDropBounds(x, y)) dockDropColumn(source, x) else null)
            return
        }
        dock.showSlotHighlight(null)
        workspace.showDropHighlight(workspaceDropCell(source, x, y))
    }

    /** Commits the drag result, or falls back to the context menu without movement. */
    private fun onDragReleased(
        container: Container,
        x: Float,
        y: Float,
        moved: Boolean,
        cancelled: Boolean,
        app: LauncherApp,
        anchor: View,
    ) {
        val source = dragSource
        workspace.showDropHighlight(null)
        dock.showSlotHighlight(null)
        dragSource = null
        if (source == null || cancelled) return

        if (!moved) {
            when {
                source.fromDrawer -> showDrawerItemMenu(anchor, app, source.pageId)
                container == Container.WORKSPACE -> showWorkspaceItemMenu(anchor, app, source.fromCell, source.pageId)
                else -> showDockItemMenu(anchor, app, source.dockPinId)
            }
            return
        }

        commitDrop(source, x, y)
    }

    /** Root-driven drag release (drawer sources; the drawer closes mid-gesture). */
    private fun onRootDragReleased(x: Float, y: Float) {
        val source = dragSource
        val moved = rootDragMoved
        onRootDragCancelled()
        if (source != null && moved) commitDrop(source, x, y)
    }

    private fun cancelDrawerGesture() {
        if (dragSource?.fromDrawer == true) onRootDragCancelled()
    }

    private fun onRootDragCancelled() {
        dragSource = null
        workspace.showDropHighlight(null)
        dock.showSlotHighlight(null)
        rootDragMoved = false
        backCallback.isEnabled = drawer.isOpen
    }

    // The dock background extends behind navigation, but the drop area does not.
    private fun withinDockDropBounds(x: Float, y: Float): Boolean =
        x >= dock.left && x < dock.right && y < dock.bottom - dockBottomInset

    private fun dockDropColumn(source: DragSource, x: Float): Int? =
        renderedState.dock.dropColumn(dock.slotAtLocalX(x - dock.left), reordering = source.container == Container.DOCK)

    private fun workspaceDropCell(source: DragSource, x: Float, y: Float): Int? {
        val cell = workspace.cellAtLocal(x - workspace.left, y - workspace.top) ?: return null
        if (cell == source.fromCell || workspace.isCellReserved(cell)) return null
        if (!source.fromDrawer) return cell

        // A drawer drag copies an app; moving an existing pin is a separate operation.
        val key = source.app.key
        val duplicate = when (val content = workspace.contentAt(cell)) {
            null -> false
            is CellContent.App -> content.app.key == key
            is CellContent.FolderCell -> renderedState.folderContents[content.folder.id].orEmpty().any { it.app.key == key }
        }
        return if (duplicate) null else cell
    }

    private fun commitDrop(source: DragSource, x: Float, y: Float) {
        if (layoutLocked) return
        if (y >= dock.top) {
            if (!withinDockDropBounds(x, y)) return
            val column = dockDropColumn(source, x)
            if (column == null) {
                if (source.container != Container.DOCK) showMessage(LauncherMessage.DOCK_FULL)
                return
            }
            val slot = renderedState.dock.savedSlot(column)
            when {
                source.fromDrawer -> controller.insertIntoDock(source.app, slot)
                source.container == Container.DOCK -> controller.reorderDock(source.dockPinId, slot)
                else -> controller.moveWorkspaceToDock(source.app, source.fromCell, slot, source.pageId)
            }
            return
        }
        val cell = workspaceDropCell(source, x, y) ?: return
        commitOnCell(source, source.app, cell)
    }

    private fun startFolderMemberDrag(folder: Folder, pinId: Long): Boolean {
        val state = renderedState
        if (layoutLocked || dragSource != null || folderMemberDrag != null ||
            state.activePageId != folder.pageId ||
            state.folderContents[folder.id].orEmpty().none { it.id == pinId }
        ) return false
        folderMemberDrag = FolderMemberDrag(folder.id, pinId, folder.pageId)
        return true
    }

    private fun folderMemberDropCell(source: FolderMemberDrag, rawX: Float, rawY: Float): Int? {
        if (layoutLocked || renderedState.activePageId != source.pageId) return null
        workspace.getLocationOnScreen(workspaceScreenLocation)
        return workspace.folderDropTarget(rawX - workspaceScreenLocation[0], rawY - workspaceScreenLocation[1])
    }

    private fun updateFolderMemberDrag(rawX: Float, rawY: Float) {
        val source = folderMemberDrag ?: return
        workspace.showDropHighlight(folderMemberDropCell(source, rawX, rawY))
    }

    private fun clearFolderMemberDrag() {
        if (folderMemberDrag == null) return
        folderMemberDrag = null
        workspace.showDropHighlight(null)
    }

    private fun finishFolderMemberDrag(rawX: Float, rawY: Float, cancelled: Boolean): Boolean {
        val source = folderMemberDrag ?: return false
        clearFolderMemberDrag()
        val dialog = folderDialog ?: return false
        val cell = if (cancelled) null else folderMemberDropCell(source, rawX, rawY)
        if (cell == null) return false
        lifecycleScope.launch {
            val placed = controller.moveFolderMemberToCell(source.pinId, source.folderId, source.pageId, cell)
            if (dialog === folderDialog) dialog.finishMemberDrop(placed)
        }
        return true
    }

    private fun reorderFolderMember(targetId: Long): Boolean {
        val source = folderMemberDrag ?: return false
        clearFolderMemberDrag()
        val dialog = folderDialog ?: return false
        if (layoutLocked || source.pinId == targetId || renderedState.activePageId != source.pageId) return false
        lifecycleScope.launch {
            controller.reorderFolderMember(source.folderId, source.pageId, source.pinId, targetId)
            if (dialog === folderDialog) dialog.finishMemberReorder()
        }
        return true
    }

    private fun showFolderDialog(folder: Folder, focusedMemberId: Long? = null) {
        folderDialog?.dismiss()
        val dialog = FolderDialog(
            context = this,
            folder = folder,
            pendingMemberFocus = focusedMemberId,
            onLaunch = { launchApp(it) },
            onMemberDragStarted = { startFolderMemberDrag(folder, it.id) },
            onMemberDragMoved = ::updateFolderMemberDrag,
            onMemberDragFinished = ::finishFolderMemberDrag,
            onMemberReordered = ::reorderFolderMember,
            onAppInfo = { controller.openAppInfo(it) },
            onRemoveFromFolder = { pinId ->
                removeFolderItems(folder.pageId) { controller.removeFromFolder(pinId, folder.id) }
            },
            onRename = { promptFolderRename(folder.id) },
            onRemoveFolder = {
                removeFolderItems(folder.pageId) { controller.removeFolder(folder.id) }
            },
            onShowPopup = ::showPopup,
            onDismissed = {
                clearFolderMemberDrag()
                snackbarJob?.cancel()
                folderDialog = null
                homeUiState.openFolderId = null
                homeUiState.focusedMemberId = null
                renderedState.folders[folder.id]?.let { anchor ->
                    requestHomeShortcutFocus(ShortcutFocus.WorkspaceFolder(anchor.pageId, anchor.cellIndex, anchor.id))
                }
            },
        )
        dialog.showAppNames = showAppNames
        dialog.layoutLocked = layoutLocked
        folderDialog = dialog
        homeUiState.openFolderId = folder.id
        homeUiState.focusedMemberId = null
        dialog.show(homeContent, themeMode)
        dialog.update(renderedState)
    }

    private fun dismissFolderDialog() {
        homeUiState.openFolderId = null
        homeUiState.focusedMemberId = null
        folderDialog?.dismiss()
    }

    private fun showFolderMenu(folder: Folder, anchor: View) {
        if (layoutLocked) {
            showSnackbar(R.string.workspace_layout_locked, null)
            return
        }
        val popup = VesnaPopupMenu(anchor)
        popup.add(R.string.folder_rename) { promptFolderRename(folder.id) }
        if (renderedState.folderContents[folder.id].orEmpty().size >= 2) {
            popup.add(R.string.folder_sort_alphabetically) {
                if (!layoutLocked && renderedState.activePageId == folder.pageId) {
                    controller.sortFolderMembers(folder, resources.configuration.locales[0])
                }
            }
        }
        if (renderedState.pageIds.size > 1) {
            popup.add(R.string.menu_move_to_page) {
                showMovePageMenu(anchor, folder.pageId, { controller.folderPageMoveOptions(folder) }) {
                    _, destinationId -> controller.moveFolderToPage(folder, destinationId)
                }
            }
        }
        popup.add(R.string.folder_remove) {
            removeFolderItems(folder.pageId) { controller.removeFolder(folder.id) }
        }
        showPopup(popup)
    }

    private fun promptFolderRename(folderId: Long) {
        if (layoutLocked) return
        val folder = renderedState.folders[folderId] ?: return
        dismissHomeRename()
        val rename = HomeRename.Folder(folderId, RenameEditorState(folder.title))
        homeUiState.rename = rename
        showHomeRename(rename)
    }

    private fun renderFolderUi(state: LauncherUiState) {
        if (state.pageIds.isEmpty()) return
        // Rebuild the folder before restoring the rename editor above it.
        homeUiState.openFolderId?.let { id ->
            val folder = state.folders[id]
            when {
                folder == null || folder.pageId != state.activePageId -> dismissFolderDialog()
                folderDialog == null -> showFolderDialog(folder, homeUiState.focusedMemberId)
                else -> folderDialog?.update(state)
            }
        }
        when (val rename = homeUiState.rename) {
            null -> Unit
            is HomeRename.Folder -> {
                val folder = state.folders[rename.folderId]
                if (layoutLocked || folder == null || folder.pageId != state.activePageId) {
                    dismissHomeRename()
                } else if (renameHost == null) {
                    showHomeRename(rename)
                }
            }
            is HomeRename.Pin -> {
                // The editor only stays while the captured cell still holds the pin.
                val present = !layoutLocked && state.activePageId == rename.pageId &&
                    (state.workspaceCells[rename.cell] as? CellContent.App)?.app?.key == rename.key
                if (!present) dismissHomeRename() else if (renameHost == null) showHomeRename(rename)
            }
        }
    }

    private fun showHomeRename(rename: HomeRename) {
        val host = ComposeView(this)
        renameHost = host
        host.setContent {
            VesnaTheme(dark = themeMode.isDark(isSystemInDarkTheme())) {
                when (rename) {
                    is HomeRename.Folder -> VesnaRenameDialog(
                        editor = rename.editor,
                        title = stringResource(R.string.folder_rename),
                        fieldLabel = stringResource(R.string.folder_name),
                        errorMessage = stringResource(R.string.name_required),
                        onDismiss = ::dismissHomeRename,
                        onSave = { title ->
                            val folder = renderedState.folders[rename.folderId]
                            if (folder != null && title != folder.title) {
                                controller.renameFolder(folder.id, title)
                            }
                            dismissHomeRename()
                        },
                    )
                    is HomeRename.Pin -> VesnaRenameDialog(
                        editor = rename.editor,
                        title = stringResource(R.string.menu_edit_name),
                        fieldLabel = stringResource(R.string.pin_name),
                        errorMessage = stringResource(R.string.name_required),
                        onDismiss = {
                            dismissHomeRename()
                            focusRenamedPin(rename)
                        },
                        onSave = { title -> commitPinRename(rename, title) },
                        onReset = { commitPinRename(rename, null) },
                    )
                }
            }
        }
        // Compose supplies a separate dialog window; Home needs only a composition host.
        homeContent.addView(host, 0, 0)
    }

    private fun dismissHomeRename() {
        homeUiState.rename = null
        val host = renameHost ?: return
        renameHost = null
        homeContent.removeView(host)
    }

    /**
     * Opens the label editor of a Home pin (an ordinary app pin or a saved
     * action pin). The stable pin ID is resolved first; the captured page and
     * cell are re-checked by [LauncherController.renameWorkspacePin] at save
     * time, so a pin that moved or vanished cannot be renamed wrongly.
     */
    private fun promptPinRename(app: LauncherApp, cell: Int, pageId: Long) {
        if (layoutLocked) return
        schedulePageMenu {
            if (layoutLocked || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
                renderedState.activePageId != pageId
            ) {
                return@schedulePageMenu
            }
            val pinId = controller.workspacePinId(app, cell, pageId) ?: return@schedulePageMenu
            if (layoutLocked || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
                renderedState.activePageId != pageId ||
                (renderedState.workspaceCells[cell] as? CellContent.App)?.app?.key != app.key
            ) return@schedulePageMenu
            dismissHomeRename()
            val rename = HomeRename.Pin(pinId, cell, pageId, app.key, app.label, RenameEditorState(app.label))
            homeUiState.rename = rename
            showHomeRename(rename)
        }
    }

    /** Saves a pin label; [label] null restores the default name (Reset). */
    private fun commitPinRename(rename: HomeRename.Pin, label: String?) {
        dismissHomeRename()
        if (label == null || label != rename.savedLabel) {
            lifecycleScope.launch {
                val renamed = controller.renameWorkspacePin(rename.pinId, rename.cell, rename.pageId, label)
                if (!renamed && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    announce(R.string.toast_shortcut_unavailable)
                }
            }
        }
        focusRenamedPin(rename)
    }

    private fun focusRenamedPin(rename: HomeRename.Pin) {
        requestHomeShortcutFocus(ShortcutFocus.WorkspaceApp(rename.pageId, rename.cell, rename.key))
    }

    /**
     * Commits a drop on a grid cell for every route: a drawer app is inserted
     * (move ops require an existing row and silently no-op for unpinned apps),
     * workspace sources relocate within the grid, dock sources return to the
     * grid, and drops onto occupants create or extend folders.
     */
    private fun commitOnCell(source: DragSource, app: LauncherApp, cell: Int) {
        when (val content = workspace.contentAt(cell)) {
            null -> when {
                source.fromDrawer -> controller.insertWorkspaceAt(app, cell, source.pageId)
                source.container == Container.WORKSPACE ->
                    controller.moveWithinWorkspace(app, source.fromCell, cell, source.pageId)
                else -> controller.moveDockToWorkspace(source.dockPinId, cell, source.pageId)
            }
            is CellContent.App -> controller.createFolderFromDrop(
                app,
                cell,
                getString(R.string.folder_default_title),
                source.folderDropSource(),
                source.pageId,
            )
            is CellContent.FolderCell -> controller.addToFolder(app, content.folder.id, source.folderDropSource())
        }
    }

    private fun DragSource.folderDropSource(): FolderDropSource = when {
        fromDrawer -> FolderDropSource.Drawer
        container == Container.WORKSPACE -> FolderDropSource.Workspace(fromCell)
        else -> FolderDropSource.Dock(dockPinId)
    }

    private fun launchApp(app: LauncherApp): Boolean {
        if (controller.launch(app)) return true
        showSnackbar(R.string.app_launch_failed, folderDialog)
        return false
    }

    private fun removeFolderItems(pageId: Long, remove: suspend () -> FolderRemovalResult) {
        if (layoutLocked) return
        val dialog = folderDialog
        lifecycleScope.launch {
            if (remove() == FolderRemovalResult.NO_ROOM &&
                dialog === folderDialog && renderedState.activePageId == pageId &&
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            ) {
                showSnackbar(R.string.folder_removal_no_room, dialog)
            }
        }
    }

    private fun showSnackbar(messageId: Int, dialog: FolderDialog?) {
        snackbarJob?.cancel()
        val message = getString(messageId)
        snackbarJob = lifecycleScope.launch {
            if (dialog != null) dialog.showSnackbar(message)
            else homeSnackbar.showSnackbar(message)
        }
    }

    private fun createHomeSnackbar(): SnackbarHostState {
        val state = SnackbarHostState()
        val host = ComposeView(this)
        // Draw after the elevated drawer without adding a shadow to the host.
        host.translationZ = drawer.elevation
        host.setContent {
            VesnaTheme(dark = themeMode.isDark(isSystemInDarkTheme())) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    SnackbarHost(state)
                }
            }
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM,
        )
        findViewById<LauncherDragLayer>(R.id.launcher_root).addView(host, params)
        ViewCompat.requestApplyInsets(host)
        return state
    }

    private fun showMessage(message: LauncherMessage) {
        val text = when (message) {
            LauncherMessage.WORKSPACE_FULL -> getString(R.string.toast_workspace_full)
            LauncherMessage.DOCK_FULL -> getString(R.string.toast_dock_full)
            LauncherMessage.WORKSPACE_PAGE_NOT_EMPTY -> getString(R.string.toast_workspace_page_not_empty)
            LauncherMessage.SHORTCUT_MOVED -> getString(R.string.toast_shortcut_moved)
            LauncherMessage.FOLDER_MOVED -> getString(R.string.toast_folder_moved)
            LauncherMessage.NO_MOVE_DESTINATION -> getString(R.string.toast_no_move_destination)
            LauncherMessage.MOVE_DESTINATION_FULL -> getString(R.string.toast_move_destination_full)
            LauncherMessage.MOVE_UNAVAILABLE -> getString(R.string.toast_move_unavailable)
            LauncherMessage.WORKSPACE_PAGE_REORDERED -> getString(R.string.toast_workspace_page_reordered)
            LauncherMessage.WORKSPACE_PAGE_ORDER_CHANGED -> getString(R.string.toast_workspace_page_order_changed)
            LauncherMessage.APP_INFO_UNAVAILABLE -> getString(R.string.toast_app_info_unavailable)
            LauncherMessage.SHORTCUT_UNAVAILABLE -> getString(R.string.toast_shortcut_unavailable)
            LauncherMessage.SHORTCUT_ALREADY_PINNED -> getString(R.string.toast_shortcut_already_pinned)
        }
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    /** Feedback for a UI-initiated action that carries no [LauncherMessage]. */
    private fun announce(messageId: Int) {
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show()
    }

    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.launcher_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            updateSystemBarInsets(insets)
            insets
        }
        // The dock overlays the workspace and includes the navigation inset.
        dock.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) updateWorkspaceBottom()
        }
    }

    internal fun updateSystemBarInsets(insets: WindowInsetsCompat) {
        val homeInsets = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        val safe = insets.getInsets(homeInsets)
        val dockBottomPadding = dock.paddingBottom - dockBottomInset
        dockBottomInset = safe.bottom
        // Keep the wallpaper edge to edge and center Home content between side insets.
        homeContent.setPadding(safe.left, 0, safe.right, 0)
        workspace.setPadding(0, safe.top, 0, workspace.paddingBottom)
        dock.setPadding(dock.paddingLeft, dock.paddingTop, dock.paddingRight, dockBottomInset + dockBottomPadding)
        drawer.applyContentInsets(insets.getInsets(homeInsets or WindowInsetsCompat.Type.ime()))
    }

    private fun updateWorkspaceBottom() {
        val params = homeControls.layoutParams as FrameLayout.LayoutParams
        if (params.bottomMargin != dock.height) {
            params.bottomMargin = dock.height
            homeControls.layoutParams = params
        }
        val bottom = dock.height + params.height
        if (workspace.paddingBottom != bottom) {
            workspace.setPadding(0, workspace.paddingTop, 0, bottom)
        }
    }

    /**
     * The Home context menu of a pinned app: the usual options plus the
     * publisher's enabled actions, queried asynchronously. The query result can
     * arrive after the anchor went stale, so every guard is re-checked before
     * the menu opens.
     */
    private fun showWorkspaceItemMenu(anchor: View, app: LauncherApp, cell: Int, pageId: Long) {
        schedulePageMenu {
            val actions = controller.appActions(app)
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
                dragSource != null || workspace.isDragging ||
                renderedState.activePageId != pageId ||
                !anchor.isAttachedToWindow || !anchor.isShown
            ) {
                return@schedulePageMenu
            }
            val popup = VesnaPopupMenu(anchor)
            if (!layoutLocked) {
                popup.add(R.string.menu_add_to_dock) { controller.addToDock(app) }
                if (renderedState.pageIds.size > 1) {
                    popup.add(R.string.menu_move_to_page) {
                        showMovePageMenu(anchor, pageId, { controller.pageMoveOptions(app, cell, pageId) }) {
                            pinId, destinationId -> controller.movePinToPage(pinId, cell, pageId, destinationId)
                        }
                    }
                }
                popup.add(R.string.menu_remove_from_workspace) { controller.removeFromWorkspace(app, cell, pageId) }
                popup.add(R.string.menu_edit_name) { promptPinRename(app, cell, pageId) }
            }
            for (action in actions) {
                popup.add(action.label, description = action.accessibilityLabel.takeUnless { it == action.label }) {
                    showActionMenu(anchor, action, pageId)
                }
            }
            popup.add(R.string.menu_app_info) { controller.openAppInfo(app) }
            showPopup(popup)
        }
    }

    /**
     * The follow-up anchored menu of one published action: launch it now, or
     * pin it to the captured page. Pinning is a layout mutation, so it is
     * offered only while the layout is unlocked.
     */
    private fun showActionMenu(anchor: View, action: LauncherApp, pageId: Long) {
        schedulePageMenu {
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
                dragSource != null || workspace.isDragging || renderedState.activePageId != pageId ||
                !anchor.isAttachedToWindow || !anchor.isShown
            ) {
                return@schedulePageMenu
            }
            val popup = VesnaPopupMenu(anchor)
            popup.add(R.string.menu_action_open) { launchApp(action) }
            if (!layoutLocked) popup.add(R.string.menu_add_to_home) { controller.pinAppAction(action, pageId) }
            showPopup(popup)
        }
    }

    private fun showMovePageMenu(
        anchor: View,
        pageId: Long,
        loadOptions: suspend () -> PageMoveOptions?,
        move: (sourceId: Long, destinationId: Long) -> Unit,
    ) = schedulePageMenu {
        val options = loadOptions()
        if (layoutLocked || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
            renderedState.activePageId != pageId || !anchor.isAttachedToWindow || !anchor.isShown
        ) return@schedulePageMenu
        if (options == null) {
            showMessage(LauncherMessage.MOVE_UNAVAILABLE)
            return@schedulePageMenu
        }
        if (options.pages.isEmpty()) {
            showMessage(LauncherMessage.NO_MOVE_DESTINATION)
            return@schedulePageMenu
        }
        val popup = VesnaPopupMenu(anchor)
        for (target in options.pages) {
            popup.add(getString(R.string.workspace_page_number, target.number)) {
                move(options.sourceId, target.id)
            }
        }
        showPopup(popup)
    }

    private fun showDockItemMenu(anchor: View, app: LauncherApp, pinId: Long) {
        val popup = VesnaPopupMenu(anchor)
        if (!layoutLocked) popup.add(R.string.menu_remove_from_dock) { controller.removeFromDock(pinId) }
        popup.add(R.string.menu_app_info) { controller.openAppInfo(app) }
        showPopup(popup)
    }

    private fun showDrawerItemMenu(anchor: View, app: LauncherApp, pageId: Long) {
        val popup = VesnaPopupMenu(anchor)
        if (!layoutLocked) {
            popup.add(R.string.menu_add_to_workspace) { controller.addToWorkspace(app, pageId) }
            popup.add(R.string.menu_add_to_dock) { controller.addToDock(app) }
        }
        popup.add(R.string.menu_app_info) { controller.openAppInfo(app) }
        showPopup(popup)
    }

    private fun showLauncherMenu() {
        val popup = VesnaPopupMenu(homeOptionsButton)
        popup.add(R.string.menu_launcher_settings) {
            startActivity(Intent(this, dev.vladon.vesna.launcher.settings.SettingsActivity::class.java))
        }
        if (!layoutLocked) popup.add(R.string.menu_wallpaper, ::startWallpaperPicker)
        val graph = VesnaApplication.graph(this)
        if (!graph.homeRoleChecker.isDefaultHome() &&
            graph.homeRoleChecker.defaultHomeRequestIntent() != null
        ) {
            popup.add(R.string.menu_default_launcher) {
                graph.homeRoleChecker.defaultHomeRequestIntent()?.let { defaultHomeRequest.launch(it) }
            }
        }
        val page = renderedState
        if (page.pageIds.size > 1) {
            popup.add(R.string.workspace_choose_page) { schedulePageMenu { showPagePicker(homeOptionsButton) } }
        }
        if (!layoutLocked) {
            if (page.pageIds.size in 1 until MAX_WORKSPACE_PAGES) {
                popup.add(R.string.menu_add_workspace_page) { controller.createWorkspacePage() }
            }
            if (page.canRemoveActivePage) {
                popup.add(R.string.menu_remove_empty_workspace_page) { controller.removeEmptyWorkspacePage(page.activePageId) }
            }
            val position = page.pageIds.indexOf(page.activePageId)
            if (page.pageIds.size > 2 && position > 0) {
                popup.add(R.string.menu_reorder_workspace_page) {
                    schedulePageMenu { showReorderPageMenu(page, position) }
                }
            }
        }
        showPopup(popup)
    }

    private fun showReorderPageMenu(page: LauncherUiState, position: Int) {
        if (layoutLocked || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || !homeOptionsButton.isShown) return

        val popup = VesnaPopupMenu(homeOptionsButton)
        for (index in 1..page.pageIds.lastIndex) {
            if (index == position) continue
            val number = index + 1
            popup.add(getString(R.string.reorder_page_position, number)) {
                controller.reorderWorkspacePage(page.activePageId, index, page.pageIds)
            }
        }
        showPopup(popup)
    }

    private fun startWallpaperPicker() {
        val intent = Intent.createChooser(
            Intent(Intent.ACTION_SET_WALLPAPER),
            getString(R.string.menu_wallpaper),
        )
        startActivity(intent)
    }
}
