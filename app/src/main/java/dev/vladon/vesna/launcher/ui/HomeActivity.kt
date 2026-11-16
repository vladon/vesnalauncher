package dev.vladon.vesna.launcher.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
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
import dev.vladon.vesna.launcher.workspace.LauncherMessage
import dev.vladon.vesna.launcher.workspace.LauncherUiState
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
        workspace.onAppLongClick = { anchor, app ->
            showWorkspaceItemMenu(anchor, app)
            true
        }
        workspace.onEmptyAreaLongClick = { showLauncherMenu() }

        dock.onAppClick = { controller.launch(it) }
        dock.onAppLongClick = { anchor, app ->
            showDockItemMenu(anchor, app)
            true
        }

        drawer.onAppClick = { controller.launch(it) }
        drawer.onAppLongClick = { app ->
            showDrawerItemMenu(app)
            true
        }
        drawer.onOpenedChanged = { backCallback.isEnabled = it }

        onBackPressedDispatcher.addCallback(this, backCallback)
        installSwipeUpToOpenDrawer()

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
        workspace.bind(state.workspaceApps)
        dock.bind(state.dockApps)
        drawer.submitApps(state.apps)
        emptyHint.isVisible = state.isWorkspaceEmpty
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

    /** Swipe up anywhere on the workspace opens the drawer. */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun installSwipeUpToOpenDrawer() {
        val density = resources.displayMetrics.density
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val dx = e2.x - (e1?.x ?: e2.x)
                if (velocityY < -SWIPE_VELOCITY && kotlin.math.abs(dx) < 100 * density) {
                    drawer.open()
                    return true
                }
                return false
            }
        })
        workspace.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
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
        val popup = PopupMenu(this, workspace)
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
        const val SWIPE_VELOCITY = 1200f
        const val MENU_ADD_TO_WORKSPACE = 1
        const val MENU_REMOVE = 2
        const val MENU_ADD_TO_DOCK = 3
        const val MENU_APP_INFO = 4
        const val MENU_SETTINGS = 5
        const val MENU_WALLPAPER = 6
        const val MENU_SET_DEFAULT = 7
    }
}
