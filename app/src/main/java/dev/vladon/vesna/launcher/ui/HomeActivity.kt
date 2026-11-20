package dev.vladon.vesna.launcher.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.VesnaApplication
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.settings.AppearancePreferences
import dev.vladon.vesna.launcher.settings.ThemeMode
import dev.vladon.vesna.launcher.settings.VesnaTheme
import dev.vladon.vesna.launcher.workspace.CellContent
import dev.vladon.vesna.launcher.workspace.Container
import dev.vladon.vesna.launcher.workspace.Folder
import dev.vladon.vesna.launcher.workspace.GridCanvas
import dev.vladon.vesna.launcher.workspace.LauncherMessage
import dev.vladon.vesna.launcher.workspace.LauncherUiState
import dev.vladon.vesna.launcher.workspace.MAX_WORKSPACE_PAGES
import dev.vladon.vesna.launcher.workspace.PageMoveOptions
import dev.vladon.vesna.launcher.workspace.WorkspaceRepository.FolderDropSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
    private var drawerClosedForDrag = false
    private var rootDragMoved = false
    private var drawerReturnFocus: View? = null
    private var activePopup: PopupMenu? = null
    private var pendingPageMenu: Job? = null
    private var folderDialog: FolderDialog? = null
    private data class FolderRename(val folderId: Long, val host: ComposeView)
    private var folderRename: FolderRename? = null
    private var launchFailure: Job? = null
    private val launchFailureSnackbar by lazy { createLaunchFailureSnackbar() }
    private var renderedState = LauncherUiState()
    private lateinit var appearance: AppearancePreferences
    private lateinit var themeMode: ThemeMode
    private var showAppNames = true

    private val controller
        get() = VesnaApplication.graph(this).controller

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
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

        workspace.onAppClick = { launchApp(it) }
        workspace.onAppOptions = { cell, app, anchor ->
            showWorkspaceItemMenu(anchor, app, cell, renderedState.activePageId)
        }
        workspace.onCellClick = { content ->
            if (content is CellContent.FolderCell) showFolderDialog(content.folder)
        }
        workspace.onFolderLongClick = { folder, anchor -> showFolderMenu(folder, anchor) }
        workspace.onFolderDrop = controller::moveFolderWithinPage
        workspace.onEmptyAreaLongClick = ::showLauncherMenu
        workspace.onSwipeUp = ::openDrawer
        workspace.onSwipePage = ::stepPage
        workspace.onDragStarted = { cell, app, _ ->
            dragSource = DragSource(app, Container.WORKSPACE, cell, renderedState.activePageId, fromDrawer = false)
        }
        workspace.onDragProgress = ::updateDragTarget
        workspace.onDragReleased = { x, y, moved, cancelled, app, anchor ->
            onDragReleased(Container.WORKSPACE, x, y, moved, cancelled, app, anchor)
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
        drawer.onItemOptions = { app, anchor -> showDrawerItemMenu(anchor, app, renderedState.activePageId) }
        drawer.onItemDragStarted = { app, _ ->
            dragSource = DragSource(app, Container.WORKSPACE, -1, renderedState.activePageId, fromDrawer = true)
        }
        drawer.onItemDragReleased = { app, anchor ->
            onDragReleased(Container.WORKSPACE, anchor.left.toFloat(), anchor.top.toFloat(), moved = false, cancelled = false, app, anchor)
        }
        drawer.onOpenedChanged = ::onDrawerOpenedChanged

        val dragLayer = findViewById<LauncherDragLayer>(R.id.launcher_root)
        dragLayer.dragInterceptor = { ev ->
            dragSource?.fromDrawer == true && ev.actionMasked == MotionEvent.ACTION_MOVE
        }
        dragLayer.dragTouchHandler = { ev ->
            val source = dragSource
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    rootDragMoved = true
                    if (source?.fromDrawer == true && !drawerClosedForDrag) {
                        drawerClosedForDrag = true
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

    override fun onResume() {
        super.onResume()
        // Settings can change the preference while Home is on the back stack.
        if (appearance.themeMode != themeMode) {
            recreate()
            return
        }
        applyAppNameVisibility(appearance.showAppNames)
    }

    override fun onStop() {
        launchFailure?.cancel()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            launchFailure?.cancel()
            pendingPageMenu?.cancel()
            pendingPageMenu = null
            activePopup?.dismiss()
            dismissFolderRename()
            folderDialog?.dismiss()
        }
        drawer.close()
    }

    private fun showPopup(popup: PopupMenu) {
        activePopup?.dismiss()
        activePopup = popup
        popup.setOnDismissListener { if (activePopup === it) activePopup = null }
        popup.show()
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
        if (state.activePageId != previous.activePageId) {
            workspace.cancelDrag()
            onRootDragCancelled()
            dismissFolderRename()
            folderDialog?.dismiss()
        }
        folderRename?.let { if (it.folderId !in state.folders) dismissFolderRename() }
        renderedState = state
        folderDialog?.update(state)
        if (state.workspaceCells != previous.workspaceCells) workspace.bind(state.workspaceCells)
        if (state.dock.pins != previous.dock.pins) dock.bind(state.dock.pins)
        if (state.apps != previous.apps) drawer.submitApps(state.apps)
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
    }

    private fun openDrawer() {
        if (dragSource == null && !workspace.isDragging) drawer.open()
    }

    private fun onDrawerOpenedChanged(opened: Boolean) {
        backCallback.isEnabled = opened
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

        val popup = vesnaPopupMenu(anchor)
        state.pageIds.forEachIndexed { index, pageId ->
            val number = index + 1
            popup.menu.add(0, number, number, getString(R.string.workspace_page_number, number))
                .setOnMenuItemClickListener {
                    if (pageId != renderedState.activePageId) controller.selectWorkspacePage(pageId)
                    true
                }
        }
        popup.menu.setGroupCheckable(0, true, true)
        popup.menu.findItem(activeIndex + 1).isChecked = true
        showPopup(popup)
    }

    /**
     * Computes and previews the drop target for the active drag. Coordinates are
     * workspace-local for both containers (dock coordinates are converted by the
     * caller). The dock zone wins when the finger is over it; elsewhere the grid
     * cell decides. Unacceptable targets show no highlight (drop returns home).
     */
    private fun updateDragTarget(wx: Float, wy: Float) {
        val source = dragSource ?: return
        if (wy >= dock.top) {
            workspace.showDropHighlight(null)
            dock.showSlotHighlight(dockDropColumn(source, wx))
            return
        }
        dock.showSlotHighlight(null)
        val cell = workspace.cellAtLocal(wx, wy)
        // Occupied cells accept the drop as folder creation / folder addition, so
        // any cell except the drag's own source cell is a valid target.
        val acceptable = cell != null && cell != source.fromCell
        workspace.showDropHighlight(if (acceptable) cell else null)
    }

    /** Commits the drag result, or falls back to the context menu without movement. */
    private fun onDragReleased(
        container: Container,
        wx: Float,
        wy: Float,
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

        commitDrop(source, wx, wy)
    }

    /** Root-driven drag release (drawer sources; the drawer closes mid-gesture). */
    private fun onRootDragReleased(x: Float, y: Float) {
        val source = dragSource
        workspace.showDropHighlight(null)
        dock.showSlotHighlight(null)
        dragSource = null
        drawerClosedForDrag = false
        if (source == null) return
        if (rootDragMoved) commitDrop(source, x, y)
        rootDragMoved = false
    }

    private fun onRootDragCancelled() {
        dragSource = null
        workspace.showDropHighlight(null)
        dock.showSlotHighlight(null)
        rootDragMoved = false
        drawerClosedForDrag = false
    }

    private fun dockDropColumn(source: DragSource, x: Float): Int? =
        renderedState.dock.dropColumn(dock.slotAtLocalX(x - dock.left), reordering = source.container == Container.DOCK)

    private fun commitDrop(source: DragSource, x: Float, y: Float) {
        if (y >= dock.top) {
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
        val cell = workspace.cellAtLocal(x, y) ?: return
        if (cell != source.fromCell) commitOnCell(source, source.app, cell)
    }

    private fun showFolderDialog(folder: Folder) {
        folderDialog?.dismiss()
        val dialog = FolderDialog(
            context = this,
            folder = folder,
            onLaunch = { launchApp(it) },
            onAppInfo = { controller.openAppInfo(it) },
            onRemoveFromFolder = { pinId -> controller.removeFromFolder(pinId, folder.id) },
            onRename = { promptFolderRename(folder.id) },
            onRemoveFolder = { controller.removeFolder(folder.id) },
            onShowPopup = ::showPopup,
            onDismissed = {
                launchFailure?.cancel()
                folderDialog = null
            },
        )
        dialog.showAppNames = showAppNames
        folderDialog = dialog
        dialog.show(homeContent, themeMode)
        dialog.update(renderedState)
    }

    private fun showFolderMenu(folder: Folder, anchor: View) {
        val popup = vesnaPopupMenu(anchor)
        popup.menu.add(0, MENU_FOLDER_RENAME, 0, R.string.folder_rename)
        if (renderedState.pageIds.size > 1) {
            popup.menu.add(0, MENU_MOVE_TO_PAGE, 1, R.string.menu_move_to_page)
        }
        popup.menu.add(0, MENU_FOLDER_REMOVE, 2, R.string.folder_remove)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_FOLDER_RENAME -> promptFolderRename(folder.id)
                MENU_MOVE_TO_PAGE -> showMovePageMenu(anchor, folder.pageId, { controller.folderPageMoveOptions(folder) }) {
                    _, destinationId -> controller.moveFolderToPage(folder, destinationId)
                }
                MENU_FOLDER_REMOVE -> controller.removeFolder(folder.id)
            }
            true
        }
        showPopup(popup)
    }

    private fun promptFolderRename(folderId: Long) {
        val folder = renderedState.folders[folderId] ?: return
        dismissFolderRename()
        val host = ComposeView(this)
        folderRename = FolderRename(folderId, host)
        host.setContent {
            VesnaTheme(dark = themeMode.isDark(isSystemInDarkTheme())) {
                FolderRenameDialog(
                    initialTitle = folder.title,
                    onDismiss = ::dismissFolderRename,
                    onSave = { title ->
                        if (title != folder.title) controller.renameFolder(folderId, title)
                        dismissFolderRename()
                    },
                )
            }
        }
        // Compose supplies a separate dialog window; Home needs only a composition host.
        homeContent.addView(host, 0, 0)
    }

    private fun dismissFolderRename() {
        val rename = folderRename ?: return
        folderRename = null
        homeContent.removeView(rename.host)
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
        launchFailure?.cancel()
        val folder = folderDialog
        val message = getString(R.string.app_launch_failed)
        launchFailure = lifecycleScope.launch {
            if (folder != null) folder.showLaunchFailure(message)
            else launchFailureSnackbar.showSnackbar(message)
        }
        return false
    }

    private fun createLaunchFailureSnackbar(): SnackbarHostState {
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
        }
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.launcher_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Preserve the space reserved for the dock and Home controls.
            workspace.setPadding(0, bars.top, 0, workspace.paddingBottom)
            dock.setPadding(dock.paddingLeft, dock.paddingTop, dock.paddingRight, bars.bottom + dock.paddingTop)
            drawer.applyContentInsets(insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime(),
            ))
            insets
        }
        // The dock overlays the workspace and includes the navigation inset.
        dock.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) updateWorkspaceBottom()
        }
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

    private fun showWorkspaceItemMenu(anchor: View, app: LauncherApp, cell: Int, pageId: Long) {
        val popup = vesnaPopupMenu(anchor)
        val controller = VesnaApplication.graph(this).controller
        popup.menu.add(0, MENU_ADD_TO_DOCK, 0, R.string.menu_add_to_dock)
        if (renderedState.pageIds.size > 1) {
            popup.menu.add(0, MENU_MOVE_TO_PAGE, 1, R.string.menu_move_to_page)
        }
        popup.menu.add(0, MENU_REMOVE, 2, R.string.menu_remove_from_workspace)
        popup.menu.add(0, MENU_APP_INFO, 3, R.string.menu_app_info)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_ADD_TO_DOCK -> controller.addToDock(app)
                MENU_MOVE_TO_PAGE -> showMovePageMenu(anchor, pageId, { controller.pageMoveOptions(app, cell, pageId) }) {
                    pinId, destinationId -> controller.movePinToPage(pinId, cell, pageId, destinationId)
                }
                MENU_REMOVE -> controller.removeFromWorkspace(app, cell, pageId)
                MENU_APP_INFO -> controller.openAppInfo(app)
            }
            true
        }
        showPopup(popup)
    }

    private fun showMovePageMenu(
        anchor: View,
        pageId: Long,
        loadOptions: suspend () -> PageMoveOptions?,
        move: (sourceId: Long, destinationId: Long) -> Unit,
    ) = schedulePageMenu {
        val options = loadOptions()
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
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
        val popup = vesnaPopupMenu(anchor)
        for (target in options.pages) {
            popup.menu.add(0, target.number, target.number, getString(R.string.workspace_page_number, target.number))
                .setOnMenuItemClickListener {
                    move(options.sourceId, target.id)
                    true
                }
        }
        showPopup(popup)
    }

    private fun showDockItemMenu(anchor: View, app: LauncherApp, pinId: Long) {
        val popup = vesnaPopupMenu(anchor)
        val controller = VesnaApplication.graph(this).controller
        popup.menu.add(0, MENU_REMOVE, 0, R.string.menu_remove_from_dock)
        popup.menu.add(0, MENU_APP_INFO, 1, R.string.menu_app_info)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_REMOVE -> controller.removeFromDock(pinId)
                MENU_APP_INFO -> controller.openAppInfo(app)
            }
            true
        }
        showPopup(popup)
    }

    private fun showDrawerItemMenu(anchor: View, app: LauncherApp, pageId: Long) {
        val popup = vesnaPopupMenu(anchor)
        val controller = VesnaApplication.graph(this).controller
        popup.menu.add(0, MENU_ADD_TO_WORKSPACE, 0, R.string.menu_add_to_workspace)
        popup.menu.add(0, MENU_ADD_TO_DOCK, 1, R.string.menu_add_to_dock)
        popup.menu.add(0, MENU_APP_INFO, 2, R.string.menu_app_info)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_ADD_TO_WORKSPACE -> controller.addToWorkspace(app, pageId)
                MENU_ADD_TO_DOCK -> controller.addToDock(app)
                MENU_APP_INFO -> controller.openAppInfo(app)
            }
            true
        }
        showPopup(popup)
    }

    private fun showLauncherMenu() {
        val popup = vesnaPopupMenu(homeOptionsButton)
        popup.menu.add(0, MENU_SETTINGS, 0, R.string.menu_launcher_settings)
        popup.menu.add(0, MENU_WALLPAPER, 1, R.string.menu_wallpaper)
        val graph = VesnaApplication.graph(this)
        if (!graph.homeRoleChecker.isDefaultHome() &&
            graph.homeRoleChecker.defaultHomeRequestIntent() != null
        ) {
            popup.menu.add(0, MENU_SET_DEFAULT, 2, R.string.menu_default_launcher)
        }
        val page = renderedState
        if (page.pageIds.size > 1) {
            popup.menu.add(0, MENU_CHOOSE_WORKSPACE_PAGE, 3, R.string.workspace_choose_page)
        }
        if (page.pageIds.size in 1 until MAX_WORKSPACE_PAGES) {
            popup.menu.add(0, MENU_ADD_WORKSPACE_PAGE, 4, R.string.menu_add_workspace_page)
        }
        if (page.canRemoveActivePage) {
            popup.menu.add(0, MENU_REMOVE_WORKSPACE_PAGE, 5, R.string.menu_remove_empty_workspace_page)
        }
        val position = page.pageIds.indexOf(page.activePageId)
        if (page.pageIds.size > 2 && position > 0) {
            popup.menu.add(0, MENU_REORDER_WORKSPACE_PAGE, 6, R.string.menu_reorder_workspace_page)
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_SETTINGS -> startActivity(Intent(this, dev.vladon.vesna.launcher.settings.SettingsActivity::class.java))
                MENU_WALLPAPER -> startWallpaperPicker()
                MENU_SET_DEFAULT -> graph.homeRoleChecker.defaultHomeRequestIntent()
                    ?.let { defaultHomeRequest.launch(it) }
                MENU_CHOOSE_WORKSPACE_PAGE -> schedulePageMenu { showPagePicker(homeOptionsButton) }
                MENU_ADD_WORKSPACE_PAGE -> controller.createWorkspacePage()
                MENU_REMOVE_WORKSPACE_PAGE -> controller.removeEmptyWorkspacePage(page.activePageId)
                MENU_REORDER_WORKSPACE_PAGE -> schedulePageMenu { showReorderPageMenu(page, position) }
            }
            true
        }
        showPopup(popup)
    }

    private fun showReorderPageMenu(page: LauncherUiState, position: Int) {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || !homeOptionsButton.isShown) return

        val popup = vesnaPopupMenu(homeOptionsButton)
        for (index in 1..page.pageIds.lastIndex) {
            if (index == position) continue
            val number = index + 1
            popup.menu.add(0, number, number, getString(R.string.reorder_page_position, number))
                .setOnMenuItemClickListener {
                    controller.reorderWorkspacePage(page.activePageId, index, page.pageIds)
                    true
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

    private companion object {
        const val MENU_ADD_TO_WORKSPACE = 1
        const val MENU_REMOVE = 2
        const val MENU_ADD_TO_DOCK = 3
        const val MENU_APP_INFO = 4
        const val MENU_SETTINGS = 5
        const val MENU_WALLPAPER = 6
        const val MENU_SET_DEFAULT = 7
        const val MENU_FOLDER_RENAME = 8
        const val MENU_FOLDER_REMOVE = 9
        const val MENU_ADD_WORKSPACE_PAGE = 10
        const val MENU_REMOVE_WORKSPACE_PAGE = 11
        const val MENU_MOVE_TO_PAGE = 12
        const val MENU_REORDER_WORKSPACE_PAGE = 13
        const val MENU_CHOOSE_WORKSPACE_PAGE = 14
    }
}
