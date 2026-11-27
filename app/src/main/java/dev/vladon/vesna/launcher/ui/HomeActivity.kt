package dev.vladon.vesna.launcher.ui

import android.accessibilityservice.AccessibilityService
import android.app.WallpaperManager
import dev.vladon.vesna.launcher.accessibility.GestureAccessibilityService
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.provider.Settings
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.graphics.Bitmap
import android.widget.LinearLayout
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.VesnaApplication
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.settings.AppearancePreferences
import dev.vladon.vesna.launcher.settings.DockPreferences
import dev.vladon.vesna.launcher.settings.DrawerPreferences
import dev.vladon.vesna.launcher.settings.HomeOrientation
import dev.vladon.vesna.launcher.settings.GestureAction
import dev.vladon.vesna.launcher.settings.GestureId
import dev.vladon.vesna.launcher.settings.HomeSearchBarPlacement
import dev.vladon.vesna.launcher.settings.LabelFont
import dev.vladon.vesna.launcher.settings.LabelFontScale
import dev.vladon.vesna.launcher.settings.PageTransition
import dev.vladon.vesna.launcher.settings.IconShapeMode
import dev.vladon.vesna.launcher.settings.LauncherPalette
import dev.vladon.vesna.launcher.settings.WorkspacePreferences
import dev.vladon.vesna.launcher.settings.ThemeMode
import dev.vladon.vesna.launcher.settings.VesnaTheme
import dev.vladon.vesna.launcher.workspace.CellContent
import dev.vladon.vesna.launcher.workspace.Container
import dev.vladon.vesna.launcher.workspace.DockPages
import dev.vladon.vesna.launcher.workspace.Folder
import dev.vladon.vesna.launcher.workspace.FolderRemovalResult
import dev.vladon.vesna.launcher.workspace.FolderRestoreResult
import dev.vladon.vesna.launcher.workspace.LauncherMessage
import dev.vladon.vesna.launcher.workspace.LauncherUiState
import dev.vladon.vesna.launcher.workspace.MAX_WORKSPACE_PAGES
import dev.vladon.vesna.launcher.workspace.NEW_WORKSPACE_PAGE_PREVIEW_ID
import dev.vladon.vesna.launcher.workspace.PageMoveOptions
import dev.vladon.vesna.launcher.workspace.WorkspaceRepository.FolderDropSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import dev.vladon.vesna.launcher.notifications.NotificationBadges
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DRAWER_OPEN_STATE = "drawer_open"
private const val DRAG_PAGE_EDGE_DP = 32
private const val DRAG_PAGE_DWELL_MS = 650L

private class FolderUndoVisuals(
    override val message: String,
    override val actionLabel: String,
    val focusAction: Boolean,
) : SnackbarVisuals {
    override val duration = SnackbarDuration.Long
    override val withDismissAction = true
}

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
    private lateinit var menuAnchor: View
    private lateinit var emptyHint: TextView
    private lateinit var pageControls: View
    private lateinit var previousPage: View
    private lateinit var nextPage: View
    private lateinit var pageIndicator: WorkspacePageIndicator
    private val wallpaperManager by lazy { getSystemService(WallpaperManager::class.java) }
    private val widgetHostManager by lazy {
        dev.vladon.vesna.launcher.workspace.WidgetHostManager(this, VesnaApplication.graph(this).widgetDao)
    }

    /** The icon currently being dragged across workspace and dock. */
    private data class DragSource(
        val app: LauncherApp,
        val container: Container,
        val fromCell: Int,
        val pageId: Long,
        val fromDrawer: Boolean,
        val dockPinId: Long = 0,
    )

    /** [wholeFolder] marks a closed folder dragged as a unit, which may also append a page. */
    private class EdgePageDrag(val homePin: DragSource? = null, val wholeFolder: Boolean = false) {
        var direction = 0
        var dwell: Job? = null
        var requestedPage: Long? = null
        var stablePinId: Long? = null
        var submitting = false
    }

    private var edgePageDrag: EdgePageDrag? = null

    private var dragSource: DragSource? = null
    private var rootDragMoved = false
    private var dockBottomInset = 0

    /** The catalogue the drawer currently shows (fast at start, fully decoded right after). */
    private var drawerCatalogue: List<LauncherApp> = emptyList()

    /** The managed profile currently in quiet mode, if any (the paused banner's target). */
    private var pausedWorkProfile: UserHandle? = null
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

    private class FolderAppSelection(
        val target: ShortcutFocus.WorkspaceFolder,
        val apps: List<LauncherApp>,
        val restoreKeyboardFocus: Boolean,
    ) {
        val selected = mutableStateOf<Set<AppKey>>(emptySet())
    }

    private class HomeUiState : ViewModel() {
        var shortcutFocus: ShortcutFocus? = null
        var drawerAppFocus: AppKey? = null
        var openFolderId: Long? = null
        var focusedMemberId: Long? = null
        var rename: HomeRename? = null
        var folderAppSelection: FolderAppSelection? = null
    }

    private val homeUiState: HomeUiState by viewModels {
        viewModelFactory { initializer { HomeUiState() } }
    }
    private var pendingShortcutFocus: ShortcutFocus? = null
    private var renameHost: ComposeView? = null
    private var folderAppPickerHost: ComposeView? = null
    private var snackbarJob: Job? = null
    private val homeSnackbar by lazy { createHomeSnackbar() }
    private var renderedState = LauncherUiState()
    private val workspacePreferences by lazy { WorkspacePreferences(this) }
    private val dockPreferences by lazy { DockPreferences(this) }
    private val userManager by lazy { getSystemService(UserManager::class.java) }
    private val drawerPreferences by lazy { DrawerPreferences(this) }
    private var hiddenDrawerApps = emptySet<AppKey>()
    private var layoutLocked = false

    /** #177: the current session's unlock — in-memory only, never persisted. */
    private var sessionUnlocked by mutableStateOf(false)
    private var searchBar: LinearLayout? = null
    private var labelFontScalePercent = LabelFontScale.DEFAULT
    private var labelFont = LabelFont.INTER_MEDIUM
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) relockWorkspaceForSession()
        }
    }
    private val workspacePreferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == WorkspacePreferences.LAYOUT_LOCKED) applyWorkspaceLock()
        if (key == WorkspacePreferences.SEARCH_BAR_PLACEMENT) applySearchBarPlacement()
        if (key == WorkspacePreferences.PAGE_INDICATOR) applyPageIndicatorStyle()
        if (key == WorkspacePreferences.WALLPAPER_SCROLL) applyWallpaperOffset()
        if (key == WorkspacePreferences.LABEL_FONT_SCALE_PERCENT || key == WorkspacePreferences.LABEL_FONT) applyLabelTypography()
    }
    private lateinit var appearance: AppearancePreferences
    private lateinit var themeMode: ThemeMode
    private lateinit var iconShape: IconShapeMode
    private lateinit var palette: LauncherPalette
    private var showAppNames = true

    /** The icon pack the current catalogue artwork was built with. */
    private var appliedIconPack = AppearancePreferences.SYSTEM_ICON_PACK

    private val controller
        get() = VesnaApplication.graph(this).controller

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (workspace.isDragging) {
                workspace.cancelDrag()
                return
            }
            cancelDrawerGesture()
            drawer.close()
        }
    }

    private val defaultHomeRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    /** Holds the host ID while the system bind-grant dialog is open. */
    private var pendingWidgetBind: Int = 0

    private val widgetBindRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val id = pendingWidgetBind
            pendingWidgetBind = 0
            if (result.resultCode != RESULT_OK || id == 0) {
                if (id != 0) widgetHostManager.deleteId(id)
                return@registerForActivityResult
            }
            val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)
            if (info == null) {
                widgetHostManager.deleteId(id)
            } else {
                commitWidget(id, info)
            }
        }

    override fun attachBaseContext(newBase: Context) {
        appearance = AppearancePreferences(newBase)
        themeMode = appearance.themeMode
        iconShape = appearance.iconShape
        palette = appearance.palette
        appliedIconPack = appearance.iconPack
        // Tiles read the shape when they bind, so it is fixed before any view inflates.
        IconShape.roundedSquare = iconShape == IconShapeMode.ROUNDED_SQUARE
        val night = themeMode.nightOverride
        // Override only the night bits; locale, density and other changes still come from Android.
        val context = if (night == null) newBase else newBase.createConfigurationContext(
            Configuration().apply { uiMode = night },
        )
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // View-layer launcher controls take their palette roles from the activity theme.
        if (palette.overlayStyle != 0) theme.applyStyle(palette.overlayStyle, true)
        pendingShortcutFocus = homeUiState.shortcutFocus
        homeUiState.shortcutFocus = null
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applySystemBarAppearance(drawerOpened = false)
        setContentView(R.layout.activity_home)

        workspace = findViewById(R.id.workspace)
        dock = findViewById(R.id.dock)
        drawer = findViewById(R.id.drawer)
        setupSearchBar()
        // The saved label typography must apply at startup, not only when the
        // settings write it while Home is resumed (#231 restart case).
        applyLabelTypography()
        drawer.applyDrawerBackground(
            drawerPreferences.background,
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES,
        )
        applyAppNameVisibility(appearance.showAppNames)
        homeContent = findViewById(R.id.home_content)
        homeControls = findViewById(R.id.home_controls)
        menuAnchor = findViewById(R.id.menu_anchor)
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

        // #184: the Home press, a launcher start and a restart open the main page.
        lifecycleScope.launch {
            controller.selectMainPage()
        }

        workspace.labelFontScalePercent = labelFontScalePercent
        workspace.labelFont = labelFont
        dock.labelFontScalePercent = labelFontScalePercent
        dock.labelFont = labelFont
        workspace.isLayoutLocked = { effectiveLayoutLocked() }
        dock.isLayoutLocked = { effectiveLayoutLocked() }
        drawer.isLayoutLocked = { effectiveLayoutLocked() }

        workspace.onAppClick = { launchApp(it) }
        workspace.onAppOptions = { cell, app, anchor ->
            showWorkspaceItemMenu(anchor, app, cell, renderedState.activePageId)
        }
        workspace.onCellClick = { content ->
            if (content is CellContent.FolderCell) showFolderDialog(content.folder)
        }
        workspace.onFolderLongClick = { folder, anchor -> showFolderMenu(folder, anchor) }
        workspace.onFolderDragProgress = ::updateWholeFolderDrag
        workspace.onFolderDragReleased = ::finishWholeFolderDrag
        workspace.onEmptyAreaLongClick = {
            if (effectiveLayoutLocked()) showLockedWorkspaceSnackbar()
            else showLauncherMenu()
        }
        workspace.onSwipeUp = ::openDrawer
        workspace.onSwipePage = ::stepPage
        workspace.onGesture = ::handleGesture
        workspace.onGestureOpenDrawer = ::openDrawer
        workspace.onGestureHomeOptions = ::showLauncherMenu
        workspace.widgetViewProvider = { id -> widgetHostManager.createHostView(id) }
        workspace.onWidgetOptions = ::showWidgetOptions
        lifecycleScope.launch { widgetHostManager.reconcile() }
        workspace.onPageDrag = ::onPageDragProgress
        workspace.onPageDragEnd = ::onPageDragEnded
        workspace.onDragStarted = { cell, app, _ ->
            if (edgePageDrag == null && !renderedState.isDragPagePreview) {
                val source = DragSource(app, Container.WORKSPACE, cell, renderedState.activePageId, fromDrawer = false)
                dragSource = source
                if (renderedState.pageIds.size > 1 ||
                    (app.key.shortcutId == null && renderedState.pageIds.isNotEmpty())
                ) {
                    edgePageDrag = EdgePageDrag(homePin = source)
                    backCallback.isEnabled = true
                }
            }
        }
        workspace.onDragProgress = { x, y ->
            val edge = edgePageDrag
            if (edge?.homePin != null) updateEdgeDwell(edge, x, y)
            if (edge?.requestedPage == null) {
                updateDragTarget(x + workspace.left, y + workspace.top)
            } else {
                dock.showSlotHighlight(null)
                workspace.showDropHighlight(edge.homePin?.let { previewDropCell(it, edge, x, y) })
            }
        }
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
        drawer.onSearchAction = ::launchSearchAction
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
        drawer.keyboardOnOpen = drawerPreferences.keyboardOnOpen
        drawer.externalSearchActions = drawerPreferences.externalSearchActions
        applyDockVisibility()
        applyDockCapacity()
        applyHomeOrientation()
        applyIconScales()
        applyPageIndicatorStyle()

        // Open before hierarchy restoration so Android can restore the search text and focus.
        if (savedInstanceState?.getBoolean(DRAWER_OPEN_STATE) == true) drawer.open(showKeyboard = false)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    controller.state.collect { render(it) }
                }
                launch {
                    // The drawer shows the fully decoded catalogue; the workspace
                    // state keeps the fast one so its tiles never rebind for
                    // artwork alone.
                    controller.drawerApps.collect { apps ->
                        val source = dragSource
                        if (source?.fromDrawer == true && apps.none { it.key == source.app.key }) {
                            // The root may already own the touch stream; losing a drawer tile
                            // cannot cancel that stream on its own.
                            onRootDragCancelled()
                        }
                        drawerCatalogue = apps
                        drawer.submitApps(apps, hiddenDrawerApps)
                    }
                }
                launch {
                    controller.messages.collect { message -> showMessage(message) }
                }
                launch {
                    // Notification dots (#200): rebind the visible tiles on every
                    // badge-state change (a posted or removed notification, the
                    // toggle, or a grant change).
                    NotificationBadges.updates.collect {
                        drawer.refreshNotificationDots()
                        dock.refreshNotificationDots()
                        workspace.refreshNotificationDots()
                    }
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
        maybeShowGestureHint()
        // Android delivers a Home intent before resume. Returning via Back
        // does not confirm the viewport staged by an auto-closed launch.
        drawer.discardUnconfirmedAutoCloseViewport()
        // A pack picked in Settings only changes artwork; rebuild the catalogue in
        // place. Runs before the theme recreate below so a recreation, which
        // re-reads the preference into appliedIconPack, cannot swallow it.
        if (appearance.iconPack != appliedIconPack) {
            appliedIconPack = appearance.iconPack
            controller.refreshAppArtwork()
        }
        // Settings can change the preference while Home is on the back stack.
        if (appearance.themeMode != themeMode || appearance.iconShape != iconShape || appearance.palette != palette) {
            recreate()
            return
        }
        applyAppNameVisibility(appearance.showAppNames)
        drawer.layoutMode = drawerPreferences.layoutMode
        drawer.rememberPosition = drawerPreferences.rememberPosition
        drawer.keyboardOnOpen = drawerPreferences.keyboardOnOpen
        drawer.externalSearchActions = drawerPreferences.externalSearchActions
        drawer.applyDrawerBackground(
            drawerPreferences.background,
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES,
        )
        applyDockVisibility()
        applyDockCapacity()
        applyHomeOrientation()
        applyWorkPausedState()
        applyWorkTabState()
        applyIconScales()
        val hiddenApps = drawerPreferences.hiddenApps
        if (hiddenApps != hiddenDrawerApps) {
            hiddenDrawerApps = hiddenApps
            drawer.submitApps(drawerCatalogue, hiddenDrawerApps)
        }
        // The Home role can change while Home is on the back stack; reconcile
        // the saved action pins with what Android still reports.
        controller.recheckAppActions()
        // Re-enumerate the catalogue outside the package-event pipeline (#203):
        // a framework that loses package events (observed on a wedged AVD session)
        // self-heals here instead of needing a framework restart. Full pass: no
        // pending-icon flicker on the already-rendered drawer.
        controller.refreshCatalogue()
    }

    /** Shows or hides the dock strip; hidden docks keep their pins. */
    private fun applyDockVisibility() {
        dock.visibility = if (dockPreferences.visible) View.VISIBLE else View.GONE
        updateWorkspaceBottom()
    }

    /** Slots per dock page; the next render rebinds so overflow reflows onto pages. */
    private fun applyDockCapacity() {
        dock.capacity = dockPreferences.capacity
    }

    /**
     * The work-profile pause state for the drawer's paused banner (#198): any
     * managed profile in quiet mode shows the banner with a resume action; the
     * action requests quiet mode off, and the resulting suspend/unsuspend events
     * refresh the catalogue. Re-checked on resume so a pause made while Vesna
     * was backgrounded is picked up.
     */
    /** Wires the drawer's Work tab (#197): the managed profile's serial and pause toggle. */
    private fun applyWorkTabState() {
        val profile = userManager.userProfiles.firstOrNull { it != Process.myUserHandle() }
        drawer.workProfileUserSerial = profile?.let { userManager.getSerialNumberForUser(it) }
        // The quiet-mode APIs need API 28; on older devices the Work tab ships
        // without the Turn-off affordance (the row hides itself).
        drawer.onTurnOffWorkApps = profile
            ?.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.P }
            ?.let { handle ->
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        userManager.requestQuietModeEnabled(true, handle)
                        applyWorkPausedState()
                    } catch (e: SecurityException) {
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            }
        }
    }

    private fun applyWorkPausedState() {
        // The quiet-mode APIs require API 28; API 26/27 devices keep the plain
        // dimmed tiles (no banner, no resume affordance).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        pausedWorkProfile = userManager.userProfiles.firstOrNull { profile ->
            profile != Process.myUserHandle() && userManager.isQuietModeEnabled(profile)
        }
        drawer.workAppsPaused = pausedWorkProfile != null
        drawer.onResumeWorkApps = pausedWorkProfile?.let { profile ->
            {
                try {
                    // Quiet mode is toggleable only by the foreground default
                    // launcher (Vesna's normal role) or privileged callers.
                    userManager.requestQuietModeEnabled(false, profile)
                    applyWorkPausedState()
                } catch (e: SecurityException) {
                    // Not the default launcher here: route to the system control
                    // that resumes the profile (the acceptance's allowed route).
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
    }

    /**
     * Follow the device (the shipped default) or hold the launcher window portrait.
     * Home handles configuration changes in place (see the manifest), so switching
     * re-lays the workspace, dock, page controls and drawer out live, and a launch
     * from Home still gets the device orientation.
     */
    private fun applyHomeOrientation() {
        requestedOrientation = when (workspacePreferences.orientation) {
            HomeOrientation.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            HomeOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    /** Artwork only: the grid, dock, labels and saved placements are not resized. */
    private fun applyIconScales() {
        val home = workspacePreferences.iconScalePercent
        workspace.iconScale = home / 100f
        dock.iconScale = dockPreferences.iconScalePercent / 100f
        val drawerSize = if (drawerPreferences.followsHomeIconScale) home else drawerPreferences.iconScalePercent
        drawer.iconScale = drawerSize / 100f
    }

    override fun onDestroy() {
        clearEdgeDrag()
        unregisterScreenOffRelock()
        workspacePreferences.unregisterListener(workspacePreferenceListener)
        super.onDestroy()
    }

    /** #177: the persisted lock gated by the current session's unlock. */
    private fun effectiveLayoutLocked(): Boolean =
        workspacePreferences.layoutLocked && !sessionUnlocked

    /** #177: unlocks layout editing for the current session (never persisted). */
    private fun unlockWorkspaceForSession() {
        sessionUnlocked = true
        applyWorkspaceLock()
        registerScreenOffRelock()
        showSnackbar(R.string.workspace_session_unlocked, null)
    }

    /** #177: relocks after an app launch or the display turning off. */
    private fun relockWorkspaceForSession() {
        if (!sessionUnlocked) return
        sessionUnlocked = false
        applyWorkspaceLock()
        unregisterScreenOffRelock()
        showSnackbar(R.string.workspace_relocked, null)
    }

    private fun registerScreenOffRelock() {
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    private fun unregisterScreenOffRelock() {
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: IllegalArgumentException) {
            // Already unregistered.
        }
    }

    /** The locked-workspace notice with the deliberate Unlock action (#177). */
    private fun showLockedWorkspaceSnackbar() {
        snackbarJob?.cancel()
        snackbarJob = lifecycleScope.launch {
            val result = homeSnackbar.showSnackbar(
                getString(R.string.workspace_layout_locked),
                actionLabel = getString(R.string.workspace_unlock_action),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) unlockWorkspaceForSession()
        }
    }

    /** #186: builds the persistent search bar and places it per the setting. */
    private fun setupSearchBar() {
        val area = findViewById<LinearLayout>(R.id.home_dock_area)
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = getDrawable(R.drawable.home_search_bar)
            val horizontal = (16 * resources.displayMetrics.density).toInt()
            setPadding(horizontal, (10 * resources.displayMetrics.density).toInt(), horizontal, (10 * resources.displayMetrics.density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(horizontal, (6 * resources.displayMetrics.density).toInt(), horizontal, (6 * resources.displayMetrics.density).toInt())
            }
            addView(ImageView(this@HomeActivity).apply {
                setImageResource(R.drawable.ic_search)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            addView(TextView(this@HomeActivity).apply {
                text = getString(R.string.search_bar_hint)
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@HomeActivity, R.color.vesna_on_surface_variant))
            })
            contentDescription = getString(R.string.search_bar_hint)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            setOnClickListener { openDrawerWithSearch() }
        }
        searchBar = bar
        area.addView(bar)
        applySearchBarPlacement()
    }

    /** #186: applies the saved search bar placement (below/above the dock or off). */
    private fun applySearchBarPlacement() {
        val bar = searchBar ?: return
        val area = bar.parent as? LinearLayout ?: return
        when (workspacePreferences.searchBarPlacement) {
            HomeSearchBarPlacement.BELOW_DOCK -> {
                bar.visibility = View.VISIBLE
                if (area.indexOfChild(bar) < area.indexOfChild(dock)) {
                    area.removeView(bar)
                    area.addView(bar)
                }
            }
            HomeSearchBarPlacement.ABOVE_DOCK -> {
                bar.visibility = View.VISIBLE
                if (area.indexOfChild(bar) > area.indexOfChild(dock)) {
                    area.removeView(bar)
                    area.addView(bar, 0)
                }
            }
            HomeSearchBarPlacement.OFF -> bar.visibility = View.GONE
        }
    }

    /** #186: opens the drawer with the search field focused. */
    private fun openDrawerWithSearch() {
        if (dragSource == null && !workspace.isDragging && edgePageDrag == null && !renderedState.isDragPagePreview) {
            drawer.open(showKeyboard = true)
        }
    }

    /** #228: a one-time hint compensating the removed bottom pills. */
    private var gestureHintShown = false

    private fun maybeShowGestureHint() {
        if (gestureHintShown) return
        val prefs = getSharedPreferences("vesna_launcher", MODE_PRIVATE)
        if (prefs.getBoolean("gesture_hint_shown", false)) return
        gestureHintShown = true
        prefs.edit().putBoolean("gesture_hint_shown", true).apply()
        lifecycleScope.launch {
            homeSnackbar.showSnackbar(
                getString(R.string.gesture_hint_message),
                duration = SnackbarDuration.Long,
            )
        }
    }

    /** #183: runs the action bound to a recognised Home gesture. */
    private fun handleGesture(gesture: GestureId) {
        // The gestures never fire over an open dialog, popup, folder or drag session.
        if (dragSource != null || workspace.isDragging || edgePageDrag != null ||
            folderDialog != null || activePopup != null || backCallback.isEnabled
        ) return
        when (workspacePreferences.gestureBinding(gesture)) {
            GestureAction.OPEN_DRAWER -> drawer.open()
            GestureAction.OPEN_SEARCH -> openDrawerWithSearch()
            GestureAction.OPEN_SETTINGS -> startActivity(Intent(this, dev.vladon.vesna.launcher.settings.SettingsActivity::class.java))
            GestureAction.EXPAND_NOTIFICATIONS -> expandNotifications()
            GestureAction.NONE -> {}
        }
    }

    /**
     * #183: expands the notification shade from the swipe-down gesture via the
     * status bar service. The accessor is a greylist reflection — wrapped so a
     * future platform change degrades the gesture to a no-op instead of
     * crashing the launcher.
     */
    private fun expandNotifications() {
        try {
            val statusBar = getSystemService("statusbar") ?: return
            statusBar.javaClass.getMethod("expandNotificationsPanel").invoke(statusBar)
        } catch (e: Exception) {
            // The reflection failed (a tightened platform): fall back to the
            // gesture-only accessibility service if the user enabled it.
            GestureAccessibilityService.instance?.performGlobalAction(
                1, // AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS.
            )
        }
    }

    /** #174: applies the saved page indicator style to the workspace indicator. */
    private fun applyPageIndicatorStyle() {
        pageIndicator.mode = workspacePreferences.pageIndicator
    }

    /**
     * #181: scrolls the wallpaper with the shown page — the first page shows
     * the wallpaper's left edge, the last its right edge; with the option off
     * (or a single page) the wallpaper stays centered.
     */
    private fun applyWallpaperOffset() {
        val token = window?.decorView?.windowToken ?: return
        val state = renderedState
        val position = state.pageIds.indexOf(state.activePageId)
        val xOffset = if (workspacePreferences.wallpaperScroll && state.pageIds.size > 1 && position >= 0) {
            position.toFloat() / (state.pageIds.size - 1)
        } else {
            0.5f
        }
        wallpaperManager.setWallpaperOffsets(token, xOffset, 0.5f)
    }

    /** #173: applies the label typography to the workspace and dock tiles. */
    private fun applyLabelTypography() {
        labelFontScalePercent = workspacePreferences.labelFontScalePercent
        labelFont = workspacePreferences.labelFont
        workspace.labelFontScalePercent = labelFontScalePercent
        workspace.labelFont = labelFont
        dock.labelFontScalePercent = labelFontScalePercent
        dock.labelFont = labelFont
        workspace.refreshLabelTypography(labelFontScalePercent, labelFont)
        dock.refreshLabelTypography(labelFontScalePercent, labelFont)
    }

    private fun applyWorkspaceLock() {
        val locked = effectiveLayoutLocked()
        if (layoutLocked == locked) return
        layoutLocked = locked
        activePopup?.dismiss()
        pendingPageMenu?.cancel()
        if (locked) {
            workspace.cancelDrag()
            dock.cancelDrag()
            cancelDrawerGesture()
            dismissHomeRename()
            dismissFolderAppPicker()
        }
        folderDialog?.layoutLocked = locked
        emptyHint.setText(if (locked) R.string.workspace_layout_locked else R.string.workspace_empty_hint)
    }

    override fun onStart() {
        super.onStart()
        widgetHostManager.startListening()
    }

    override fun onStop() {
        widgetHostManager.stopListening()
        workspace.cancelDrag()
        folderDialog?.cancelMemberDrag()
        snackbarJob?.cancel()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        cancelDrawerGesture()
        super.onNewIntent(intent)
        val fromSystemHome = intent.hasCategory(Intent.CATEGORY_HOME)
        if (fromSystemHome || intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            workspace.cancelDrag()
            snackbarJob?.cancel()
            pendingPageMenu?.cancel()
            pendingPageMenu = null
            activePopup?.dismiss()
            dismissHomeRename()
            dismissFolderAppPicker()
            dismissFolderDialog()
        }
        if (fromSystemHome) drawer.closeForHome() else drawer.close()
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
        val homeVisible = visible && !workspacePreferences.hideHomeAppNames
        workspace.showAppNames = homeVisible
        dock.showAppNames = homeVisible
        if (showAppNames == visible) return
        showAppNames = visible
        drawer.showAppNames = visible
        folderDialog?.showAppNames = visible
    }

    private fun render(state: LauncherUiState) {
        val previous = renderedState
        val gridChanged = state.grid != previous.grid
        if (state.activePageId != previous.activePageId) {
            if (state.isDragPagePreview && state.activePageId == edgePageDrag?.requestedPage) {
                // Keep the stream but release the old tile's dim before its cell is rebound.
                workspace.clearDragAnchorHighlight()
                workspace.showDropHighlight(null)
            } else {
                workspace.cancelDrag()
                onRootDragCancelled()
                if (renameHost != null) dismissHomeRename()
                if (!state.isDragPagePreview && !previous.isDragPagePreview) folderDialog?.dismiss()
            }
        }
        if (gridChanged) {
            // Resizing invalidates every coordinate a live drag aims at, and
            // moves anything a popup or draft is anchored to.
            workspace.cancelDrag()
            folderDialog?.cancelMemberDrag()
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
            workspace.bind(state.grid, state.workspaceCells, state.isDragPagePreview)
        }
        if (state.dock.pins != previous.dock.pins) dock.bind(state.dock.pins)
        val appendAfter = state.newPagePreviewAfter
        emptyHint.isVisible = state.isWorkspaceEmpty && appendAfter == null
        if (state.pageIds != previous.pageIds || state.activePageId != previous.activePageId ||
            appendAfter != previous.newPagePreviewAfter
        ) {
            val position = if (appendAfter == null) state.pageIds.indexOf(state.activePageId)
            else state.pageIds.indexOf(appendAfter) + 1
            val count = if (appendAfter == null) state.pageIds.size else position + 1
            val hadPreviousFocus = previousPage.hasFocus()
            val hadNextFocus = nextPage.hasFocus()
            val hadIndicatorFocus = pageIndicator.hasFocus()
            pageControls.isVisible = count > 1
            previousPage.isEnabled = position > 0
            nextPage.isEnabled = position in 0 until count - 1
            previousPage.alpha = if (previousPage.isEnabled) 1f else 0.38f
            nextPage.alpha = if (nextPage.isEnabled) 1f else 0.38f
            if (pageControls.isVisible) {
                if (hadPreviousFocus && !previousPage.isEnabled) nextPage.requestFocus()
                if (hadNextFocus && !nextPage.isEnabled) previousPage.requestFocus()
            } else if (hadPreviousFocus || hadNextFocus || hadIndicatorFocus) {
                workspace.requestFocus()
            }
            pageIndicator.bind(position, count)
            applyWallpaperOffset()
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
        drawerCatalogue = apps
        drawer.submitApps(apps, hiddenDrawerApps)
    }

    private fun focusedHomeShortcut(): ShortcutFocus? {
        if (drawer.isOpen || folderDialog != null || renameHost != null || folderAppPickerHost != null) return null
        val state = renderedState
        val cell = workspace.focusedChild?.tag as? Int
        if (cell != null) {
            return when (val content = state.workspaceCells[cell]) {
                is CellContent.App -> ShortcutFocus.WorkspaceApp(state.activePageId, cell, content.app.key)
                is CellContent.FolderCell -> ShortcutFocus.WorkspaceFolder(state.activePageId, cell, content.folder.id)
                is CellContent.WidgetCell -> null
                null -> null
            }
        }
        val tile = dock.focusedChild ?: return null
        return state.dock.pins.getOrNull(dock.pinIndexAt(dock.indexOfChild(tile)))?.let { ShortcutFocus.Dock(it.id) }
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
            if (drawer.isOpen || folderDialog != null || renameHost != null || folderAppPickerHost != null) {
                return@doOnPreDraw
            }
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
                is ShortcutFocus.Dock -> dock.childAtPin(current.dock.pins.indexOfFirst { it.id == focus.id })
            }
            tile?.requestFocus()
        }
    }

    private fun openDrawer() {
        if (dragSource == null && !workspace.isDragging && edgePageDrag == null && !renderedState.isDragPagePreview) {
            drawer.open()
        }
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
            if (target?.isShown != true || !target.requestFocus()) workspace.requestFocus()
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
        if (dragSource != null || workspace.isDragging || backCallback.isEnabled || edgePageDrag != null) return
        // A finger-follow session owns the release; its settle path commits.
        if (pageDragSession != null) return
        val state = renderedState
        val current = state.pageIds.indexOf(state.activePageId)
        if (current < 0) return
        val step = if (forward) 1 else -1
        var target = state.pageIds.getOrNull(current + step)
        if (target == null && workspacePreferences.pageLoop && state.pageIds.size > 1) {
            // #174: looping wraps BOTH outward edges — the last page forward
            // onto the first, and (superseding the #179 drawer edge while the
            // loop is on) the first page backward onto the last.
            target = if (forward) state.pageIds.firstOrNull() else state.pageIds.lastOrNull()
        }
        if (target == null) {
            // #179: the outward swipe on the first page opens the drawer (the
            // mirrored direction is handled by the view's RTL-aware forward).
            if (!forward && current == 0) drawer.open()
            return
        }
        // #180: the Slide effect animates the outgoing page over the incoming
        // one; Instant switches as before. Both respect the animator scale
        // and the Remove animations accessibility setting.
        val scale = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) * if (Settings.Secure.getInt(contentResolver, "accessibility_remove_animations", 0) == 1) 0f else 1f
        if (workspacePreferences.pageTransition == PageTransition.SLIDE && scale > 0f) {
            animatePageSlide(forward, scale)
        }
        controller.selectWorkspacePage(target)
    }

    /** #180: slides a snapshot of the current page toward the swipe direction. */
    private fun animatePageSlide(forward: Boolean, scale: Float) {
        val snapshot = workspace.capturePageBitmap()
        val overlay = ImageView(this).apply {
            setImageBitmap(snapshot)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        (findViewById<View>(R.id.home_content) as ViewGroup).addView(overlay)
        overlay.post {
            overlay.animate()
                .translationX((if (forward) -1 else 1) * overlay.width.toFloat())
                .setDuration((300 * scale).toLong())
                .withEndAction { (overlay.parent as? ViewGroup)?.removeView(overlay) }
                .start()
        }
    }

    // ---- #180 phase 2: the pages follow the finger and settle on release. ----

    /** The live finger-follow session; null while no page swipe layers exist. */
    private var pageDragSession: PageDragSession? = null

    /** One finger-follow session: the outgoing and incoming page bitmaps plus overlays. */
    private inner class PageDragSession(
        val forward: Boolean,
        val targetPageId: Long,
        val scale: Float,
        val workspaceWidth: Int,
    ) {
        val content = findViewById<ViewGroup>(R.id.home_content)
        val incomingView = ImageView(this@HomeActivity).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val outgoingView = ImageView(this@HomeActivity).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        /** The drag delta the layers were prepared for; progress before readiness lands here. */
        var pendingDx: Float? = null
        var ready = false

        /** The swipe ended while the capture window was still running. */
        @Volatile var released = false

        /** The release committed to the target page: the capture must not flip back. */
        @Volatile var committed = false

        init {
            incomingView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            outgoingView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            content.addView(incomingView)
            content.addView(outgoingView)
        }

        /** Positions both layers for [dx]: the outgoing page follows the finger. */
        fun translate(dx: Float) {
            if (!ready) {
                pendingDx = dx
                return
            }
            outgoingView.translationX = dx
            incomingView.translationX = dx + (if (forward) workspaceWidth else -workspaceWidth)
        }

        fun removeFromParent() {
            (incomingView.parent as? ViewGroup)?.removeView(incomingView)
            (outgoingView.parent as? ViewGroup)?.removeView(outgoingView)
        }
    }

    /** First horizontal MOVE past the swipe slop: prepare the follow layers. */
    private fun onPageDragProgress(dx: Float) {
        val session = pageDragSession
        if (session != null) {
            session.translate(dx)
            return
        }
        startPageDragFollow(dx)
    }

    /**
     * Prepares the two-layer follow: the outgoing snapshot covers the workspace
     * while the live binding flips to the adjacent page for its bitmap and back.
     * Without an adjacent page (or with the Instant effect) nothing starts and
     * the release falls back to today's switch.
     */
    private fun startPageDragFollow(firstDx: Float) {
        val state = renderedState
        if (workspacePreferences.pageTransition != PageTransition.SLIDE) return
        if (state.isDragPagePreview || dragSource != null || workspace.isDragging) return
        val rtl = workspace.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val forward = (firstDx < 0f) != rtl
        val current = state.pageIds.indexOf(state.activePageId)
        val step = if (forward) 1 else -1
        var adjacent = state.pageIds.getOrNull(current + step)
        if (adjacent == null && workspacePreferences.pageLoop && state.pageIds.size > 1) {
            // #174: the same both-edges wrap as stepPage; a one-page home
            // never starts a same-page follow.
            adjacent = if (forward) state.pageIds.firstOrNull() else state.pageIds.lastOrNull()
        }
        val target = adjacent ?: return
        val scale = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) * if (Settings.Secure.getInt(contentResolver, "accessibility_remove_animations", 0) == 1) 0f else 1f
        val session = PageDragSession(forward, target, scale, workspace.width)
        pageDragSession = session
        val currentId = state.activePageId
        val width = workspace.width
        val indicator = findViewById<WorkspacePageIndicator>(R.id.workspace_page_indicator)
        lifecycleScope.launch {
            try {
                // The outgoing snapshot hides the capture rebinds from the user.
                session.outgoingView.setImageBitmap(workspace.capturePageBitmap())
                indicator.isVisible = false
                controller.selectWorkspacePage(target)
                var flipped = withTimeoutOrNull(600L) {
                    while (renderedState.activePageId != target) delay(8)
                    true
                } ?: false
                if (!flipped) error("the adjacent page never rendered")
                session.incomingView.setImageBitmap(workspace.capturePageBitmap())
                if (!session.committed) {
                    controller.selectWorkspacePage(currentId)
                    flipped = withTimeoutOrNull(600L) {
                        while (renderedState.activePageId != currentId) delay(8)
                        true
                    } ?: false
                    if (!flipped) error("the original page never rendered back")
                }
                indicator.isVisible = true
                session.incomingView.translationX = (if (session.forward) width else -width).toFloat()
                if (session.released) {
                    session.removeFromParent()
                    return@launch
                }
                session.ready = true
                session.pendingDx?.let { session.translate(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                pageDragSession = null
                session.removeFromParent()
                indicator.isVisible = true
                if (!session.committed) controller.selectWorkspacePage(currentId)
            }
        }
    }

    /** The swipe stream ended: commit and settle, or spring the layers back. */
    private fun onPageDragEnded(dx: Float) {
        val session = pageDragSession ?: return
        pageDragSession = null
        session.released = true
        val rtl = workspace.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val forward = (dx < 0f) != rtl
        val commits = abs(dx) >= workspace.pageSwipeThresholdPx
        val currentId = renderedState.activePageId
        if (!session.ready) {
            // The capture window lost the race with the release: switch with
            // today's plain visuals (or spring nothing back) and let the
            // capture job restore the selection unless the commit wins.
            session.committed = commits
            session.removeFromParent()
            findViewById<WorkspacePageIndicator>(R.id.workspace_page_indicator).isVisible = true
            if (commits) controller.selectWorkspacePage(session.targetPageId)
            return
        }
        session.committed = commits && session.targetPageId != currentId
        val settleTo = when {
            commits && forward -> -workspace.width.toFloat()
            commits -> workspace.width.toFloat()
            else -> 0f
        }
        val distance = abs(settleTo - session.outgoingView.translationX).coerceAtLeast(1f)
        val duration = (distance / workspace.width * 300 * session.scale).toLong().coerceAtMost(300L)
        session.outgoingView.animate().translationX(settleTo).setDuration(duration)
            .withEndAction { session.removeFromParent() }.start()
        session.incomingView.animate()
            .translationX(settleTo + (if (session.forward) workspace.width else -workspace.width))
            .setDuration(duration).start()
        if (session.committed) controller.selectWorkspacePage(session.targetPageId)
    }

    private fun showPagePicker(anchor: View) {
        if (dragSource != null || workspace.isDragging || backCallback.isEnabled || edgePageDrag != null ||
            !anchor.isShown
        ) return
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

    /** One adjacent turn per edge entry; moving into the interior rearms the dwell. */
    private fun updateEdgeDwell(drag: EdgePageDrag, x: Float, y: Float) {
        val width = workspace.width
        val band = (DRAG_PAGE_EDGE_DP * resources.displayMetrics.density).coerceAtMost(width / 4f)
        val direction = when {
            y < workspace.paddingTop || y >= workspace.height - workspace.paddingBottom -> 0
            x >= 0f && x < band -> -1
            x >= width - band && x < width -> 1
            else -> 0
        }
        if (drag.direction == direction) return
        drag.direction = direction
        drag.dwell?.cancel()
        if (direction == 0 || layoutLocked || drag.submitting) return
        val state = renderedState
        if (!isRenderedDragPage(drag)) return
        val step = if (workspace.layoutDirection == View.LAYOUT_DIRECTION_RTL) -direction else direction
        val appendAfter = state.newPagePreviewAfter
        val destination = if (appendAfter != null) {
            if (step < 0) appendAfter else return
        } else {
            val current = state.pageIds.indexOf(state.activePageId)
            if (current < 0) return
            state.pageIds.getOrNull(current + step) ?: if (
                step > 0 && current == state.pageIds.lastIndex && state.pageIds.size < MAX_WORKSPACE_PAGES &&
                (drag.wholeFolder || folderMemberDrag != null ||
                    drag.homePin?.app?.key?.let { it.shortcutId == null } == true)
            ) NEW_WORKSPACE_PAGE_PREVIEW_ID else return
        }
        drag.dwell = lifecycleScope.launch {
            delay(DRAG_PAGE_DWELL_MS)
            val pin = drag.homePin
            if (pin != null && drag.stablePinId == null) {
                drag.stablePinId = controller.workspacePinId(pin.app, pin.fromCell, pin.pageId) ?: return@launch
            }
            if (edgePageDrag !== drag || drag.direction != direction || layoutLocked ||
                renderedState.activePageId != state.activePageId || renderedState.pageIds != state.pageIds
            ) return@launch
            drag.requestedPage = destination
            workspace.showDropHighlight(null)
            if (destination == NEW_WORKSPACE_PAGE_PREVIEW_ID) {
                controller.previewNewWorkspacePageForDrag(state.activePageId)
            } else {
                controller.previewWorkspacePageForDrag(destination)
            }
        }
    }

    private fun isRenderedDragPage(drag: EdgePageDrag): Boolean =
        drag.requestedPage?.let { renderedState.isDragPagePreview && renderedState.activePageId == it }
            ?: !renderedState.isDragPagePreview

    /** A cell of the previewed page that accepts the pin: empty, an app to group with, or a folder to join. */
    private fun previewDropCell(source: DragSource, drag: EdgePageDrag, localX: Float, localY: Float): Int? {
        if (layoutLocked || !isRenderedDragPage(drag)) return null
        val cell = workspace.cellAtLocal(localX, localY) ?: return null
        // The preview can return to the source page, where the dragged pin's own cell is no target.
        val ownCell = drag.requestedPage == source.pageId && cell == source.fromCell
        return if (ownCell || workspace.isCellReserved(cell)) null else cell
    }

    private fun clearEdgeDrag() {
        val drag = edgePageDrag ?: return
        edgePageDrag = null
        drag.dwell?.cancel()
        if (drag.requestedPage != null) controller.cancelWorkspacePagePreview()
        backCallback.isEnabled = drawer.isOpen || rootDragMoved
    }

    private fun submitEdgeDrop(
        drag: EdgePageDrag,
        destination: Long,
        move: suspend () -> Boolean,
        finish: (Boolean) -> Unit = {},
    ) {
        drag.dwell?.cancel()
        drag.submitting = true
        lifecycleScope.launch {
            val placed = move()
            if (edgePageDrag !== drag) return@launch
            if (placed && destination != NEW_WORKSPACE_PAGE_PREVIEW_ID) controller.finishWorkspacePagePreview(destination)
            clearEdgeDrag()
            finish(placed)
        }
    }

    private fun dropHomePinOnPreview(
        drag: EdgePageDrag,
        source: DragSource,
        x: Float,
        y: Float,
    ) {
        val destination = drag.requestedPage ?: return
        val cell = previewDropCell(source, drag, x - workspace.left, y - workspace.top)
        val pinId = drag.stablePinId
        if (pinId == null || cell == null) {
            clearEdgeDrag()
            return
        }
        // The bound page is still the preview, so its occupant tells which change to save.
        val occupant = workspace.contentAt(cell)
        val grid = renderedState.grid
        val folderSource = source.folderDropSource()
        val defaultTitle = getString(R.string.folder_default_title)
        submitEdgeDrop(drag, destination, move = {
            when (occupant) {
                is CellContent.App -> controller.groupWithCell(source.app, cell, defaultTitle, folderSource, destination)
                is CellContent.FolderCell -> controller.joinFolder(source.app, occupant.folder.id, folderSource)
                // A blank page only has empty cells, so a new page is always a plain move.
                null -> if (destination == NEW_WORKSPACE_PAGE_PREVIEW_ID) {
                    controller.finishNewWorkspacePagePreview(pinId, source.pageId, source.fromCell, cell, grid)
                } else {
                    controller.moveWorkspacePinToCell(pinId, source.pageId, source.fromCell, destination, cell)
                }
                // A widget cell is covered/reserved; a drop that slips past the
                // mask must not place a pin over a live widget.
                is CellContent.WidgetCell -> false
            }
        })
    }

    private fun updateWholeFolderDrag(x: Float, y: Float): Boolean {
        if (dragSource != null || edgePageDrag?.submitting == true) return false
        val drag = edgePageDrag ?: EdgePageDrag(wholeFolder = true).also {
            edgePageDrag = it
            backCallback.isEnabled = true
        }
        updateEdgeDwell(drag, x, y)
        return !layoutLocked && isRenderedDragPage(drag)
    }

    private fun finishWholeFolderDrag(folder: Folder, cell: Int?) {
        val drag = edgePageDrag ?: return
        if (drag.submitting) return
        if (cell == null || !isRenderedDragPage(drag)) {
            clearEdgeDrag()
            return
        }
        val destination = drag.requestedPage
        val grid = renderedState.grid
        if (destination != null) {
            submitEdgeDrop(drag, destination, move = {
                if (destination == NEW_WORKSPACE_PAGE_PREVIEW_ID) {
                    controller.finishNewWorkspacePagePreviewWithFolder(
                        folder.id, folder.pageId, folder.cellIndex, cell, grid,
                    )
                } else {
                    controller.moveFolderToCell(folder, destination, cell, grid)
                }
            })
        } else {
            clearEdgeDrag()
            lifecycleScope.launch { controller.moveFolderToCell(folder, folder.pageId, cell, grid) }
        }
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
        val edge = edgePageDrag?.takeIf { it.homePin != null && it.homePin === source }
        if (edge?.requestedPage != null && !cancelled && moved && source != null) {
            dropHomePinOnPreview(edge, source, x, y)
            return
        }
        if (edge != null) clearEdgeDrag()
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
        if (edgePageDrag?.homePin != null && edgePageDrag?.submitting == false) clearEdgeDrag()
        dragSource = null
        workspace.showDropHighlight(null)
        dock.showSlotHighlight(null)
        rootDragMoved = false
        backCallback.isEnabled = drawer.isOpen
    }

    // The dock background extends behind navigation, but the drop area does not.
    // A hidden dock is not a drop target.
    private fun withinDockDropBounds(x: Float, y: Float): Boolean =
        dock.isVisible && x >= dock.left && x < dock.right && y < dock.bottom - dockBottomInset

    private fun dockDropColumn(source: DragSource, x: Float): Int? =
        renderedState.dock.dropColumn(
            DockPages.globalSlot(dock.currentPage, dock.slotAtLocalX(x - dock.left), dockPreferences.capacity),
            reordering = source.container == Container.DOCK,
            capacityPerPage = dockPreferences.capacity,
        )

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
            is CellContent.WidgetCell -> false        }
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
        if (layoutLocked || dragSource != null || folderMemberDrag != null || edgePageDrag != null ||
            state.isDragPagePreview ||
            state.activePageId != folder.pageId ||
            state.folderContents[folder.id].orEmpty().none { it.id == pinId }
        ) return false
        folderMemberDrag = FolderMemberDrag(folder.id, pinId, folder.pageId)
        edgePageDrag = EdgePageDrag()
        return true
    }

    private fun folderMemberDropCell(source: FolderMemberDrag, localX: Float, localY: Float): Int? {
        if (layoutLocked) return null
        val drag = edgePageDrag
        if (drag != null && !isRenderedDragPage(drag)) return null
        if (drag?.requestedPage == null && renderedState.activePageId != source.pageId) return null
        return workspace.folderDropTarget(localX, localY)
    }

    private fun updateFolderMemberDrag(rawX: Float, rawY: Float) {
        val source = folderMemberDrag ?: return
        workspace.getLocationOnScreen(workspaceScreenLocation)
        val x = rawX - workspaceScreenLocation[0]
        val y = rawY - workspaceScreenLocation[1]
        edgePageDrag?.let { updateEdgeDwell(it, x, y) }
        workspace.showDropHighlight(folderMemberDropCell(source, x, y))
    }

    private fun clearFolderMemberDrag(keepPreview: Boolean = false) {
        if (folderMemberDrag == null) return
        folderMemberDrag = null
        if (!keepPreview) clearEdgeDrag()
        workspace.showDropHighlight(null)
    }

    private fun finishFolderMemberDrag(rawX: Float, rawY: Float, cancelled: Boolean): Boolean {
        val source = folderMemberDrag ?: return false
        val dialog = folderDialog
        workspace.getLocationOnScreen(workspaceScreenLocation)
        val cell = if (cancelled || dialog == null) null else {
            folderMemberDropCell(source, rawX - workspaceScreenLocation[0], rawY - workspaceScreenLocation[1])
        }
        val drag = edgePageDrag
        val destination = drag?.requestedPage
        clearFolderMemberDrag(keepPreview = cell != null && destination != null)
        if (cell == null || dialog == null) return false
        if (drag != null && destination != null) {
            val grid = renderedState.grid
            submitEdgeDrop(
                drag, destination,
                move = {
                    if (destination == NEW_WORKSPACE_PAGE_PREVIEW_ID) {
                        controller.finishNewWorkspacePagePreviewFromFolder(
                            source.pinId, source.folderId, source.pageId, cell, grid,
                        )
                    } else {
                        controller.moveFolderMemberToCell(source.pinId, source.folderId, source.pageId, cell, destination)
                    }
                },
                finish = { placed -> if (dialog === folderDialog) dialog.finishMemberDrop(placed) },
            )
        } else {
            lifecycleScope.launch {
                val placed = controller.moveFolderMemberToCell(source.pinId, source.folderId, source.pageId, cell)
                if (dialog === folderDialog) dialog.finishMemberDrop(placed)
            }
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
        dialog.show(homeContent, themeMode, palette)
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
        popup.add(R.string.folder_add_apps) { promptFolderAppPicker(folder, anchor.hasFocus()) }
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
        popup.add(R.string.folder_ungroup) {
            removeFolderItems(folder.pageId) { controller.removeFolder(folder.id) }
        }
        val keyboardMenu = !anchor.isInTouchMode && anchor.hasFocus()
        popup.add(R.string.folder_unpin) { unpinFolderWithUndo(folder, keyboardMenu) }
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
                folder == null -> dismissFolderDialog()
                folder.pageId != state.activePageId && (!state.isDragPagePreview || folderDialog == null) ->
                    dismissFolderDialog()
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
        homeUiState.folderAppSelection?.let { selection ->
            if (editableFolder(selection.target, state) == null) {
                dismissFolderAppPicker()
            } else if (folderAppPickerHost == null) {
                showFolderAppPicker(selection)
            }
        }
    }

    private fun showHomeRename(rename: HomeRename) {
        val host = ComposeView(this)
        renameHost = host
        host.setContent {
            VesnaTheme(dark = themeMode.isDark(isSystemInDarkTheme()), palette = palette) {
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

    private fun editableFolder(target: ShortcutFocus.WorkspaceFolder, state: LauncherUiState): Folder? {
        if (layoutLocked || state.activePageId != target.pageId) return null
        val folder = (state.workspaceCells[target.cell] as? CellContent.FolderCell)?.folder
        return folder?.takeIf { it.id == target.id }
    }

    private fun promptFolderAppPicker(folder: Folder, restoreKeyboardFocus: Boolean) {
        val target = ShortcutFocus.WorkspaceFolder(folder.pageId, folder.cellIndex, folder.id)
        if (editableFolder(target, renderedState) == null) return
        val members = renderedState.folderContents[folder.id].orEmpty().mapTo(HashSet()) { it.app.key }
        val apps = renderedState.apps.filterNot { it.key in members }
        val selection = FolderAppSelection(target, apps, restoreKeyboardFocus)
        homeUiState.folderAppSelection = selection
        showFolderAppPicker(selection)
    }

    private fun showFolderAppPicker(selection: FolderAppSelection) {
        val host = ComposeView(this)
        folderAppPickerHost = host
        host.setContent {
            VesnaTheme(dark = themeMode.isDark(isSystemInDarkTheme()), palette = palette) {
                FolderAddAppsDialog(
                    apps = selection.apps,
                    selected = selection.selected.value,
                    onToggle = { key ->
                        val selected = selection.selected.value
                        selection.selected.value = if (key in selected) selected - key else selected + key
                    },
                    onDismiss = { dismissFolderAppPicker(restoreFocus = true) },
                    onSave = save@{
                        if (homeUiState.folderAppSelection !== selection) return@save
                        val folder = editableFolder(selection.target, renderedState)
                        val selected = selection.selected.value
                        dismissFolderAppPicker(restoreFocus = true)
                        if (folder != null && selected.isNotEmpty()) {
                            // Append in the catalogue's displayed order, not the order of taps.
                            val keys = selection.apps.mapNotNull { it.key.takeIf(selected::contains) }
                            controller.addAppsToFolder(folder, keys)
                        }
                    },
                )
            }
        }
        homeContent.addView(host, 0, 0)
    }

    private fun dismissFolderAppPicker(restoreFocus: Boolean = false) {
        val selection = homeUiState.folderAppSelection
        homeUiState.folderAppSelection = null
        val host = folderAppPickerHost ?: return
        folderAppPickerHost = null
        homeContent.removeView(host)
        if (restoreFocus && selection?.restoreKeyboardFocus == true) requestHomeShortcutFocus(selection.target)
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
            // A committed widget cell is reserved; drops never legitimately land here.
            is CellContent.WidgetCell -> {}
        }
    }

    private fun DragSource.folderDropSource(): FolderDropSource = when {
        fromDrawer -> FolderDropSource.Drawer
        container == Container.WORKSPACE -> FolderDropSource.Workspace(fromCell, pageId)
        else -> FolderDropSource.Dock(dockPinId)
    }

    private fun launchApp(app: LauncherApp): Boolean {
        val launched = controller.launch(app)
        if (launched && sessionUnlocked && workspacePreferences.autoRelock) {
            // #177: launching an app relocks an automatically-relocking session.
            relockWorkspaceForSession()
        }
        if (launched) return true
        val pausedProfile = pausedWorkProfile
        val pausedLaunch = pausedProfile != null &&
            userManager.getSerialNumberForUser(pausedProfile) == app.key.userSerial
        showSnackbar(
            if (pausedLaunch) R.string.drawer_work_paused else R.string.app_launch_failed,
            folderDialog,
        )
        return false
    }

    /** Hands the search query to another app; the intent was resolved when the row was shown. */
    private fun launchSearchAction(intent: Intent) {
        try {
            startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            showSnackbar(R.string.app_launch_failed, folderDialog)
        }
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

    private fun unpinFolderWithUndo(folder: Folder, keyboardMenu: Boolean) {
        if (layoutLocked || renderedState.activePageId != folder.pageId) return
        snackbarJob?.cancel()
        snackbarJob = lifecycleScope.launch {
            // Acquire the removal token even if Home stops just as the Room transaction commits.
            val removed = withContext(NonCancellable) { controller.unpinFolderWithUndo(folder) } ?: return@launch
            var undo = false
            val result: FolderRestoreResult?
            try {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    undo = homeSnackbar.showSnackbar(
                        FolderUndoVisuals(
                            getString(R.string.folder_unpinned),
                            getString(R.string.folder_unpin_undo),
                            focusAction = keyboardMenu,
                        ),
                    ) == SnackbarResult.ActionPerformed
                }
            } finally {
                result = withContext(NonCancellable) { controller.finishFolderUnpin(removed, undo) }
            }
            val notice = when (result) {
                FolderRestoreResult.NO_ROOM -> R.string.folder_restore_no_room
                FolderRestoreResult.UNAVAILABLE -> R.string.folder_restore_unavailable
                FolderRestoreResult.LOCKED -> R.string.workspace_layout_locked
                FolderRestoreResult.RESTORED, null -> return@launch
            }
            homeSnackbar.showSnackbar(getString(notice))
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
            VesnaTheme(dark = themeMode.isDark(isSystemInDarkTheme()), palette = palette) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    SnackbarHost(state) { data ->
                        val undo = data.visuals as? FolderUndoVisuals
                        if (undo == null) {
                            Snackbar(data)
                        } else {
                            val action = remember { FocusRequester() }
                            // Use the standard Material action and dismiss affordance on
                            // their own row so enlarged localized text can use the full width.
                            Snackbar(data, Modifier.focusRequester(action), actionOnNewLine = true)
                            if (undo.focusAction) LaunchedEffect(data) { action.requestFocus() }
                        }
                    }
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
            LauncherMessage.FOLDER_APPS_UNAVAILABLE -> return showSnackbar(R.string.folder_add_apps_unavailable, null)
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
        // The lift must follow the whole dock area: the search bar (#186) stacks
        // below the dock and changes the bottom zone without touching the dock.
        findViewById<View>(R.id.home_dock_area).addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
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
        // The whole dock area (dock plus the optional search bar) owns the bottom;
        // a hidden dock or a hidden bar gives its space back automatically.
        val areaHeight = findViewById<View>(R.id.home_dock_area).height
        val params = homeControls.layoutParams as FrameLayout.LayoutParams
        if (params.bottomMargin != areaHeight) {
            params.bottomMargin = areaHeight
            homeControls.layoutParams = params
        }
        val bottom = areaHeight + params.height
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
                if (dock.isVisible) popup.add(R.string.menu_add_to_dock) { controller.addToDock(app) }
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
            if (dock.isVisible) popup.add(R.string.menu_add_to_dock) { controller.addToDock(app) }
        }
        popup.add(R.string.menu_app_info) { controller.openAppInfo(app) }
        showPopup(popup)
    }

    /** The placed-widget options (ENG-102 slice 2b): remove with undo. */
    private fun showWidgetOptions(widget: dev.vladon.vesna.launcher.data.WidgetEntity, anchor: View) {
        val popup = VesnaPopupMenu(anchor)
        popup.add(R.string.widget_remove) { removeWidgetWithUndo(widget) }
        showPopup(popup)
    }

    /**
     * ADR-WIDGET-1 removal order: the Room record goes first (the widget
     * disappears immediately), the host binding is released only after the
     * undo window closes. Undo re-inserts the same record; the live binding
     * makes the widget reappear with no rebind dialog.
     */
    private fun removeWidgetWithUndo(widget: dev.vladon.vesna.launcher.data.WidgetEntity) {
        snackbarJob?.cancel()
        snackbarJob = lifecycleScope.launch {
            val dao = VesnaApplication.graph(this@HomeActivity).widgetDao
            withContext(NonCancellable) { dao.deleteById(widget.id) }
            var undo = false
            try {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    undo = homeSnackbar.showSnackbar(
                        FolderUndoVisuals(
                            getString(R.string.widget_removed),
                            getString(R.string.widget_removed_undo),
                            focusAction = false,
                        ),
                    ) == SnackbarResult.ActionPerformed
                }
            } finally {
                if (undo) {
                    withContext(NonCancellable) { dao.insert(widget) }
                } else {
                    widgetHostManager.deleteId(widget.appWidgetId)
                }
            }
        }
    }
    /**
     * The searchable widget catalog (#149): the installed widgets grouped by
     * provider with a preview, a name, and the grid size. Browsing binds
     * nothing; picking runs the same placement flow as the slice-2a popup.
     */
    private fun showWidgetPicker(anchor: View) {
        val manager = AppWidgetManager.getInstance(this)
        val density = resources.displayMetrics.densityDpi
        val appContext: android.content.Context = this
        val entries = manager.getInstalledProvidersForProfile(processUserHandle())
            .filter { it.provider != null }
            .mapNotNull { info ->
                val providerLabel = info.loadLabel(packageManager)?.toString() ?: return@mapNotNull null
                val widgetLabel = info.loadLabel(packageManager)?.toString() ?: providerLabel
                val preview = if (android.os.Build.VERSION.SDK_INT >= 31) {
                    try {
                        info.loadPreviewImage(appContext, density)
                    } catch (t: Throwable) {
                        null
                    }
                } else null
                WidgetPickerEntry(
                    info = info,
                    providerLabel = providerLabel,
                    widgetLabel = widgetLabel,
                    icon = info.loadIcon(appContext, density),
                    preview = preview,
                    spanText = widgetSpanText(info, 4, 5),
                )
            }
        if (entries.isEmpty()) {
            showSnackbar(R.string.widget_picker_empty, null)
            return
        }
        // The Compose content needs the ViewTree owners a plain Dialog window
        // does not install; share Home's so the recomposer resolves.
        val pickerCompose = androidx.compose.ui.platform.ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@HomeActivity)
            setViewTreeSavedStateRegistryOwner(this@HomeActivity)
            setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
            )
            setContent {
                VesnaTheme(dark = themeMode.isDark(isSystemInDarkTheme()), palette = palette) {
                    WidgetPickerDialog(
                        entries = entries,
                        onPick = { info ->
                            placeWidget(info)
                        },
                        onDismiss = { },
                    )
                }
            }
        }
        val dialog = android.app.Dialog(this).apply {
            setContentView(pickerCompose)
            window?.setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT,
            )
            setOnDismissListener { pickerCompose.disposeComposition() }
        }
        dialog.show()
    }

    private fun processUserHandle(): android.os.UserHandle =
        android.os.Process.myUserHandle()

    /** The human grid size of a widget, from the modern spans or the dp fallback. */
    private fun widgetSpanText(
        info: android.appwidget.AppWidgetProviderInfo,
        columns: Int,
        rows: Int,
    ): String {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            val w = info.targetCellWidth
            val h = info.targetCellHeight
            if (w > 0 && h > 0) return "$w × $h"
        }
        val cellDp = 70
        val spanW = ((info.minWidth + cellDp - 1) / cellDp).coerceAtMost(columns)
        val spanH = ((info.minHeight + cellDp - 1) / cellDp).coerceAtMost(rows)
        return "$spanW × $spanH"
    }


    /** Allocates a host ID, binds the provider, then commits the placement. */
    private fun placeWidget(info: android.appwidget.AppWidgetProviderInfo) {
        if (!menuAnchor.isShown) return
        val id = widgetHostManager.allocateId()
        val manager = AppWidgetManager.getInstance(this)
        val bound = manager.bindAppWidgetIdIfAllowed(id, info.provider)
        if (!bound) {
            pendingWidgetBind = id
            val grant = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            widgetBindRequest.launch(grant)
            return
        }
        commitWidget(id, info)
    }

    /** ADR-WIDGET-1 order: bind first, then the Room row; cancellation releases the ID. */
    private fun commitWidget(id: Int, info: android.appwidget.AppWidgetProviderInfo) {
        lifecycleScope.launch {
            val state = renderedState
            val grid = state.grid
            // API 31+ reports the provider default spans; earlier devices fall back to 2x2.
            val defaultSpanX = if (android.os.Build.VERSION.SDK_INT >= 31) info.targetCellWidth else 0
            val defaultSpanY = if (android.os.Build.VERSION.SDK_INT >= 31) info.targetCellHeight else 0
            val spanX = defaultSpanX.takeIf { it > 0 }?.coerceIn(1, grid.columns) ?: 2
            val spanY = defaultSpanY.takeIf { it > 0 }?.coerceIn(1, grid.rows) ?: 2
            val anchor = firstFreeCellForWidget(state, spanX, spanY)
            if (anchor == null) {
                widgetHostManager.deleteId(id)
                showSnackbar(R.string.widget_picker_no_room, null)
                return@launch
            }
            VesnaApplication.graph(this@HomeActivity).widgetDao.insert(
                dev.vladon.vesna.launcher.data.WidgetEntity(
                    appWidgetId = id,
                    providerPackage = info.provider.packageName,
                    providerClass = info.provider.className,
                    pageId = state.activePageId,
                    cellIndex = anchor,
                    spanX = spanX,
                    spanY = spanY,
                ),
            )
        }
    }

    /** First row-major 2x2-fitting anchor clear of visible cells and reservations. */
    private fun firstFreeCellForWidget(state: dev.vladon.vesna.launcher.workspace.LauncherUiState, spanX: Int, spanY: Int): Int? {
        val grid = state.grid
        val columns = grid.columns
        val reserved = state.reservedWorkspaceCellsMask
        for (cell in 0 until columns * grid.rows) {
            if (grid.rowOf(cell) + spanY > grid.rows) continue
            if (grid.columnOf(cell) + spanX > columns) continue
            var fits = true
            loop@ for (row in 0 until spanY) {
                for (column in 0 until spanX) {
                    val covered = cell + row * columns + column
                    if (reserved and (1 shl covered) != 0 || state.workspaceCells.containsKey(covered)) {
                        fits = false
                        break@loop
                    }
                }
            }
            if (fits) return cell
        }
        return null
    }
    private fun showLauncherMenu() {
        if (renderedState.isDragPagePreview) return
        val popup = VesnaPopupMenu(menuAnchor)
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
            popup.add(R.string.workspace_choose_page) { schedulePageMenu { showPagePicker(menuAnchor) } }
        }
        val mainPage = controller.mainPageId()
        if (page.activePageId != mainPage) {
            popup.add(R.string.menu_make_main_page) {
                controller.makePageMain(page.activePageId)
                showSnackbar(R.string.menu_make_main_page_done, null)
            }
        }
        if (!layoutLocked) {
            popup.add(R.string.menu_add_widget) { showWidgetPicker(menuAnchor) }
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
        if (layoutLocked || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || !workspace.isShown) return

        val popup = VesnaPopupMenu(menuAnchor)
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
