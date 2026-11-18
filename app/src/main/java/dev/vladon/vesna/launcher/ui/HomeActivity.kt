package dev.vladon.vesna.launcher.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.VesnaApplication
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.CellContent
import dev.vladon.vesna.launcher.workspace.Container
import dev.vladon.vesna.launcher.workspace.Folder
import dev.vladon.vesna.launcher.workspace.GridCanvas
import dev.vladon.vesna.launcher.workspace.LauncherMessage
import dev.vladon.vesna.launcher.workspace.LauncherUiState
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
    private lateinit var emptyHint: TextView
    private lateinit var defaultHomeBanner: TextView

    /** The icon currently being dragged across workspace and dock. */
    private data class DragSource(
        val app: LauncherApp,
        val container: Container,
        val fromCell: Int,
        val anchor: View,
        val fromDrawer: Boolean,
    )

    private var dragSource: DragSource? = null
    private var drawerClosedForDrag = false
    private var rootDragMoved = false
    private var folderDialog: FolderDialog? = null
    private var folderDialogJob: Job? = null

    private val controller
        get() = VesnaApplication.graph(this).controller

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            drawer.close()
        }
    }

    private val defaultHomeRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // The role result arrives as a fresh HOME intent or an onStart re-check.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_home)

        workspace = findViewById(R.id.workspace)
        dock = findViewById(R.id.dock)
        drawer = findViewById(R.id.drawer)
        emptyHint = findViewById(R.id.workspace_empty_hint)
        defaultHomeBanner = findViewById(R.id.banner_default_home)

        val graph = VesnaApplication.graph(this)
        val controller = graph.controller

        workspace.onAppClick = { controller.launch(it) }
        workspace.onCellClick = { content ->
            if (content is CellContent.FolderCell) showFolderDialog(content.folder)
        }
        workspace.onFolderLongClick = { folder, anchor -> showFolderMenu(folder, anchor) }
        workspace.onEmptyAreaLongClick = { showLauncherMenu() }
        workspace.onSwipeUp = { drawer.open() }
        workspace.onDragStarted = { cell, app, anchor ->
            dragSource = DragSource(app, Container.WORKSPACE, cell, anchor, fromDrawer = false)
        }
        workspace.onDragProgress = { x, y -> updateDragTarget(Container.WORKSPACE, x, y) }
        workspace.onDragReleased = { x, y, moved, cancelled, app, anchor ->
            onDragReleased(Container.WORKSPACE, x, y, moved, cancelled, app, anchor)
        }

        dock.onAppClick = { controller.launch(it) }
        dock.onDragStarted = { slot, app, anchor ->
            dragSource = DragSource(app, Container.DOCK, slot, anchor, fromDrawer = false)
        }
        dock.onDragProgress = { x, y -> updateDragTarget(Container.DOCK, x + dock.left, y + dock.top) }
        dock.onDragReleased = { x, y, moved, cancelled, app, anchor ->
            onDragReleased(Container.DOCK, x + dock.left, y + dock.top, moved, cancelled, app, anchor)
        }

        drawer.onAppClick = { controller.launch(it) }
        drawer.onItemDragStarted = { app, anchor ->
            dragSource = DragSource(app, Container.WORKSPACE, fromCell = -1, anchor, fromDrawer = true)
        }
        drawer.onItemDragReleased = { app, anchor ->
            onDragReleased(Container.WORKSPACE, anchor.left.toFloat(), anchor.top.toFloat(), moved = false, cancelled = false, app, anchor)
        }
        drawer.onOpenedChanged = { backCallback.isEnabled = it }

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
                    updateDragTarget(Container.WORKSPACE, ev.x, ev.y)
                }
                MotionEvent.ACTION_UP -> onRootDragReleased(ev.x, ev.y)
                MotionEvent.ACTION_CANCEL -> onRootDragCancelled()
            }
        }

        onBackPressedDispatcher.addCallback(this, backCallback)

        defaultHomeBanner.setOnClickListener {
            graph.homeRoleChecker.defaultHomeRequestIntent()?.let { intent ->
                defaultHomeRequest.launch(intent)
            }
        }

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing HOME while the drawer is open closes it.
        drawer.close()
    }

    override fun onStart() {
        super.onStart()
        defaultHomeBanner.isVisible = !VesnaApplication.graph(this).homeRoleChecker.isDefaultHome()
    }

    private fun render(state: LauncherUiState) {
        workspace.bind(state.workspaceCells)
        dock.bind(state.dockApps)
        drawer.submitApps(state.apps)
        emptyHint.isVisible = state.isWorkspaceEmpty
    }

    /**
     * Computes and previews the drop target for the active drag. Coordinates are
     * workspace-local for both containers (dock coordinates are converted by the
     * caller). The dock zone wins when the finger is over it; elsewhere the grid
     * cell decides. Unacceptable targets show no highlight (drop returns home).
     */
    private fun updateDragTarget(container: Container, wx: Float, wy: Float) {
        val source = dragSource ?: return
        if (wy >= dock.top) {
            workspace.showDropHighlight(null)
            val canAccept = source.container == Container.DOCK ||
                dock.boundCount < GridCanvas.DOCK_CAPACITY
            dock.showSlotHighlight(if (canAccept) dock.slotAtLocalX(wx - dock.left) else null)
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
                source.fromDrawer -> showDrawerItemMenu(app)
                container == Container.WORKSPACE -> showWorkspaceItemMenu(anchor, app)
                else -> showDockItemMenu(anchor, app)
            }
            return
        }

        val overDock = wy >= dock.top
        when {
            overDock && source.fromDrawer ->
                controller.insertIntoDock(app, dock.slotAtLocalX(wx - dock.left))
            overDock && source.container == Container.WORKSPACE ->
                controller.moveWorkspaceToDock(app, dock.slotAtLocalX(wx - dock.left))
            overDock && source.container == Container.DOCK ->
                controller.reorderDock(app, dock.slotAtLocalX(wx - dock.left))
            !overDock && source.fromDrawer ->
                workspace.cellAtLocal(wx, wy)?.let { cell ->
                    commitOnCell(source, app, cell)
                }
            !overDock && source.container == Container.WORKSPACE ->
                workspace.cellAtLocal(wx, wy)?.let { cell ->
                    if (cell != source.fromCell) commitOnCell(source, app, cell)
                }
            !overDock && source.container == Container.DOCK ->
                workspace.cellAtLocal(wx, wy)?.let { cell ->
                    commitOnCell(source, app, cell)
                }
        }
    }

    /** Root-driven drag release (drawer sources; the drawer closes mid-gesture). */
    private fun onRootDragReleased(x: Float, y: Float) {
        val source = dragSource
        workspace.showDropHighlight(null)
        dock.showSlotHighlight(null)
        dragSource = null
        drawerClosedForDrag = false
        if (source == null) return
        if (rootDragMoved) commitGridDrop(source, x, y, overDock = y >= dock.top)
        rootDragMoved = false
    }

    private fun onRootDragCancelled() {
        rootDragMoved = false
        drawerClosedForDrag = false
    }

    /**
     * Commits a root-driven grid release: dock insert/move, or the shared cell
     * routing ([commitOnCell]). Drawer sources carry Container.WORKSPACE, so the
     * fromDrawer discriminator must be checked before the container arms.
     */
    private fun commitGridDrop(source: DragSource, x: Float, y: Float, overDock: Boolean) {
        val app = source.app
        if (overDock) {
            when {
                source.fromDrawer || source.container == Container.DOCK ->
                    controller.insertIntoDock(app, dock.slotAtLocalX(x))
                else -> controller.moveWorkspaceToDock(app, dock.slotAtLocalX(x))
            }
            return
        }
        val cell = workspace.cellAtLocal(x, y) ?: return
        commitOnCell(source, app, cell)
    }

    private fun showFolderDialog(folder: Folder) {
        folderDialog?.dismiss()
        val dialog = FolderDialog(
            context = this,
            folder = folder,
            onLaunch = { controller.launch(it) },
            onRemoveFromFolder = { controller.removeFromFolder(it) },
            onRename = { f, title -> controller.renameFolder(f.id, title) },
            onRemoveFolder = { controller.removeFolder(folder.id) },
            onDismissed = {
                folderDialogJob?.cancel()
                folderDialog = null
            },
        )
        folderDialog = dialog
        folderDialogJob = lifecycleScope.launch {
            controller.state.collect { state ->
                folderDialog?.update(state)
                if (state.folders[folder.id] == null) folderDialog?.dismiss()
            }
        }
        dialog.show()
    }

    private fun showFolderMenu(folder: Folder, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, MENU_FOLDER_RENAME, 0, R.string.folder_rename)
        popup.menu.add(0, MENU_FOLDER_REMOVE, 1, R.string.folder_remove)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_FOLDER_RENAME -> promptFolderRename(folder)
                MENU_FOLDER_REMOVE -> controller.removeFolder(folder.id)
            }
            true
        }
        popup.show()
    }

    private fun promptFolderRename(folder: Folder) {
        val input = EditText(this)
        input.setText(folder.title)
        input.setSelection(input.text.length)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.folder_rename))
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty()) controller.renameFolder(folder.id, newTitle)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
                source.fromDrawer -> controller.insertWorkspaceAt(app, cell)
                source.container == Container.WORKSPACE -> controller.moveWithinWorkspace(app, cell)
                else -> controller.moveDockToWorkspace(app, cell)
            }
            is CellContent.App -> controller.createFolderFromDrop(
                app,
                cell,
                getString(R.string.folder_default_title),
            )
            is CellContent.FolderCell -> controller.addToFolder(app, content.folder.id)
        }
    }

    private fun showMessage(message: LauncherMessage) {
        val text = when (message) {
            LauncherMessage.WORKSPACE_FULL -> getString(R.string.toast_workspace_full)
            LauncherMessage.DOCK_FULL -> getString(R.string.toast_dock_full)
        }
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.launcher_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            workspace.setPadding(0, bars.top, 0, 0)
            dock.setPadding(dock.paddingLeft, dock.paddingTop, dock.paddingRight, bars.bottom + dock.paddingTop)
            drawer.applySystemBarInsets(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    private fun showWorkspaceItemMenu(anchor: View, app: LauncherApp) {
        val popup = PopupMenu(this, anchor)
        val controller = VesnaApplication.graph(this).controller
        popup.menu.add(0, MENU_ADD_TO_DOCK, 0, R.string.menu_add_to_dock)
        popup.menu.add(0, MENU_REMOVE, 1, R.string.menu_remove_from_workspace)
        popup.menu.add(0, MENU_APP_INFO, 2, R.string.menu_app_info)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_ADD_TO_DOCK -> controller.addToDock(app)
                MENU_REMOVE -> controller.removeFromWorkspace(app)
                MENU_APP_INFO -> showAppInfo(app)
            }
            true
        }
        popup.show()
    }

    private fun showDockItemMenu(anchor: View, app: LauncherApp) {
        val popup = PopupMenu(this, anchor)
        val controller = VesnaApplication.graph(this).controller
        popup.menu.add(0, MENU_REMOVE, 0, R.string.menu_remove_from_dock)
        popup.menu.add(0, MENU_APP_INFO, 1, R.string.menu_app_info)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_REMOVE -> controller.removeFromDock(app)
                MENU_APP_INFO -> showAppInfo(app)
            }
            true
        }
        popup.show()
    }

    private fun showDrawerItemMenu(app: LauncherApp) {
        val popup = PopupMenu(this, drawer)
        val controller = VesnaApplication.graph(this).controller
        popup.menu.add(0, MENU_ADD_TO_WORKSPACE, 0, R.string.menu_add_to_workspace)
        popup.menu.add(0, MENU_ADD_TO_DOCK, 1, R.string.menu_add_to_dock)
        popup.menu.add(0, MENU_APP_INFO, 2, R.string.menu_app_info)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_ADD_TO_WORKSPACE -> controller.addToWorkspace(app)
                MENU_ADD_TO_DOCK -> controller.addToDock(app)
                MENU_APP_INFO -> showAppInfo(app)
            }
            true
        }
        popup.show()
    }

    private fun showLauncherMenu() {
        val popup = PopupMenu(this, dock)
        popup.menu.add(0, MENU_SETTINGS, 0, R.string.menu_launcher_settings)
        popup.menu.add(0, MENU_WALLPAPER, 1, R.string.menu_wallpaper)
        val graph = VesnaApplication.graph(this)
        if (!graph.homeRoleChecker.isDefaultHome() &&
            graph.homeRoleChecker.defaultHomeRequestIntent() != null
        ) {
            popup.menu.add(0, MENU_SET_DEFAULT, 2, R.string.menu_set_default_home)
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_SETTINGS -> startActivity(Intent(this, dev.vladon.vesna.launcher.settings.SettingsActivity::class.java))
                MENU_WALLPAPER -> startWallpaperPicker()
                MENU_SET_DEFAULT -> graph.homeRoleChecker.defaultHomeRequestIntent()
                    ?.let { defaultHomeRequest.launch(it) }
            }
            true
        }
        popup.show()
    }

    private fun startWallpaperPicker() {
        val intent = Intent.createChooser(
            Intent(Intent.ACTION_SET_WALLPAPER),
            getString(R.string.menu_wallpaper),
        )
        startActivity(intent)
    }

    private fun showAppInfo(app: LauncherApp) {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", app.packageName, null),
            ),
        )
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
    }
}
