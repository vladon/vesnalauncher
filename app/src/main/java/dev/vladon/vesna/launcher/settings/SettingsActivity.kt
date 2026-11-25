package dev.vladon.vesna.launcher.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import android.os.UserManager
import androidx.core.app.NotificationManagerCompat
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.VesnaApplication
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.LayoutSnapshotV1
import dev.vladon.vesna.launcher.data.LayoutSnapshotException
import dev.vladon.vesna.launcher.platform.IconPackInfo
import dev.vladon.vesna.launcher.notifications.NotificationBadges
import dev.vladon.vesna.launcher.platform.IconPackResolver
import dev.vladon.vesna.launcher.settings.HomeOrientation
import dev.vladon.vesna.launcher.workspace.DockPages
import dev.vladon.vesna.launcher.workspace.GridCanvas
import dev.vladon.vesna.launcher.workspace.LayoutRestoreAttempt
import dev.vladon.vesna.launcher.workspace.WorkspaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Entry point for Vesna's settings directory and named pages. */
class SettingsActivity : ComponentActivity() {
    private val userManager by lazy { getSystemService(UserManager::class.java) }
    private lateinit var workspace: WorkspacePreferences
    private lateinit var appearance: AppearancePreferences
    private lateinit var dockPreferences: DockPreferences
    private lateinit var drawerPreferences: DrawerPreferences
    private val navigation: SettingsNavigation by viewModels {
        viewModelFactory { initializer { SettingsNavigation(SettingsPage.fromIntent(intent)) } }
    }
    private var defaultHome by mutableStateOf(DefaultHomeState.UNAVAILABLE)
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    private var iconShape by mutableStateOf(IconShapeMode.SYSTEM)
    private var iconPack by mutableStateOf(AppearancePreferences.SYSTEM_ICON_PACK)
    private var iconPackOptions by mutableStateOf<List<IconPackInfo>>(emptyList())
    private var palette by mutableStateOf(LauncherPalette.VESNA)
    private var layoutLocked by mutableStateOf(false)
    /** Null until the persisted grid arrives; the row must not flash the default. */
    private var grid by mutableStateOf<GridCanvas?>(null)
    private var pinNewApps by mutableStateOf(false)
    private var hideHomeAppNames by mutableStateOf(false)
    private var showDock by mutableStateOf(true)
    private var dockCapacity by mutableStateOf(DockPages.DEFAULT_CAPACITY)
    private var dockPinCount by mutableStateOf(0)
    private var homeOrientation by mutableStateOf(HomeOrientation.SYSTEM)
    private var homeIconPercent by mutableStateOf(IconScale.DEFAULT)
    private var dockIconPercent by mutableStateOf(IconScale.DEFAULT)
    private var drawerLayoutMode by mutableStateOf(DrawerLayoutMode.GRID)
    private var closeDrawerOnLaunch by mutableStateOf(false)
    private var rememberDrawerPosition by mutableStateOf(false)
    private var showKeyboardOnDrawerOpen by mutableStateOf(false)
    private var drawerSearchActions by mutableStateOf(false)
    private var drawerBackground by mutableStateOf(DrawerBackgroundMode.THEME)
    private var drawerIconPercent by mutableStateOf(IconScale.DEFAULT)
    private var drawerFollowsHome by mutableStateOf(true)
    private var workTabEnabled by mutableStateOf(false)
    private var hasWorkProfile by mutableStateOf(false)
    private var notificationDots by mutableStateOf(false)
    private var notificationAccessGranted by mutableStateOf(false)
    private var showAppNames by mutableStateOf(true)
    private var drawerApps by mutableStateOf<List<LauncherApp>>(emptyList())
    private var hiddenDrawerApps by mutableStateOf<Set<AppKey>>(emptySet())
    private var setupIntent: Intent? = null
    private val homeRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refreshDefaultHome() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appearance = AppearancePreferences(this)
        workspace = WorkspacePreferences(this)
        dockPreferences = DockPreferences(this)
        drawerPreferences = DrawerPreferences(this)
        readPreferences()
        // View-layer launcher controls take their palette roles from the activity theme.
        if (palette.overlayStyle != 0) theme.applyStyle(palette.overlayStyle, true)
        enableEdgeToEdge()
        val version = readVersionName()
        val workspaceRepository = VesnaApplication.graph(this).workspaceRepository
        setContent {
            LaunchedEffect(workspaceRepository) {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    workspaceRepository.grid.collect { grid = it }
                }
            }
            if (navigation.page == SettingsPage.APP_DRAWER || navigation.page == SettingsPage.HOME_SCREEN) {
                LaunchedEffect(Unit) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        VesnaApplication.graph(this@SettingsActivity).controller.state.collect {
                            drawerApps = it.apps
                            dockPinCount = it.dock.savedOrder.size
                        }
                    }
                }
            }
            val dark = themeMode.isDark(isSystemInDarkTheme())
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            VesnaTheme(dark = dark, palette = palette) {
                key(navigation.entry) {
                    SettingsScreen(
                        page = navigation.page,
                        query = navigation.query,
                        onQueryChange = { navigation.query = it },
                        searchTarget = navigation.searchTarget,
                        onNavigate = navigation::navigate,
                        onResetInputFocus = ::parkWindowFocus,
                        versionName = version,
                        defaultHome = defaultHome,
                        onSetDefaultHome = ::requestDefaultHome,
                        themeMode = themeMode,
                        onThemeSelected = ::selectTheme,
                        iconShape = iconShape,
                        onIconShapeSelected = ::selectIconShape,
                        iconPack = iconPack,
                        iconPackOptions = iconPackOptions,
                        onIconPackSelected = ::selectIconPack,
                        palette = palette,
                        onPaletteSelected = ::selectPalette,
                        showAppNames = showAppNames,
                        onShowAppNamesChanged = ::changeAppNameVisibility,
                        layoutLocked = layoutLocked,
                        onLayoutLockedChanged = { locked ->
                            workspace.layoutLocked = locked
                            layoutLocked = locked
                        },
                        grid = grid,
                        onApplyGridResize = { target -> workspaceRepository.resizeGrid(target) },
                        pinNewApps = pinNewApps,
                        onPinNewAppsChanged = { enabled ->
                            workspace.pinNewApps = enabled
                            pinNewApps = enabled
                        },
                        hideHomeAppNames = hideHomeAppNames,
                        onHideHomeAppNamesChanged = { hidden ->
                            workspace.hideHomeAppNames = hidden
                            hideHomeAppNames = hidden
                        },
                        showDock = showDock,
                        onShowDockChanged = { visible ->
                            dockPreferences.visible = visible
                            showDock = visible
                        },
                        dockCapacity = dockCapacity,
                        onDockCapacityChanged = ::changeDockCapacity,
                        homeOrientation = homeOrientation,
                        onHomeOrientationSelected = ::selectHomeOrientation,
                        homeIconPercent = homeIconPercent,
                        onHomeIconPercentChanged = { percent ->
                            workspace.iconScalePercent = percent
                            homeIconPercent = percent
                        },
                        dockIconPercent = dockIconPercent,
                        onDockIconPercentChanged = { percent ->
                            dockPreferences.iconScalePercent = percent
                            dockIconPercent = percent
                        },
                        onExportLayout = { uriText -> exportLayout(workspaceRepository, uriText) },
                        onRestoreLayout = { uriText -> openRestore(workspaceRepository, uriText) },
                        onConfirmRestore = { preview ->
                            workspaceRepository.applyRestore(preview.snapshot) { pkg ->
                                packageManager.getLaunchIntentForPackage(pkg) != null
                            }
                            true
                        },
                        drawerLayoutMode = drawerLayoutMode,
                        onDrawerLayoutModeSelected = ::changeDrawerLayoutMode,
                        closeDrawerOnLaunch = closeDrawerOnLaunch,
                        onCloseDrawerOnLaunchChanged = { enabled ->
                            drawerPreferences.closeOnLaunch = enabled
                            closeDrawerOnLaunch = enabled
                        },
                        rememberDrawerPosition = rememberDrawerPosition,
                        onRememberDrawerPositionChanged = { enabled ->
                            drawerPreferences.rememberPosition = enabled
                            rememberDrawerPosition = enabled
                        },
                        showKeyboardOnDrawerOpen = showKeyboardOnDrawerOpen,
                        onShowKeyboardOnDrawerOpenChanged = { enabled ->
                            drawerPreferences.keyboardOnOpen = enabled
                            showKeyboardOnDrawerOpen = enabled
                        },
                        drawerSearchActions = drawerSearchActions,
                        onDrawerSearchActionsChanged = { enabled ->
                            drawerPreferences.externalSearchActions = enabled
                            drawerSearchActions = enabled
                        },
                        drawerBackground = drawerBackground,
                        onDrawerBackgroundSelected = { mode ->
                            drawerPreferences.background = mode
                            drawerBackground = mode
                        },
                        drawerIconPercent = drawerIconPercent,
                        onDrawerIconPercentChanged = { percent ->
                            drawerPreferences.iconScalePercent = percent
                            drawerIconPercent = percent
                        },
                        drawerFollowsHome = drawerFollowsHome,
                        onDrawerFollowsHomeChanged = { follow ->
                            drawerPreferences.followsHomeIconScale = follow
                            drawerFollowsHome = follow
                        },
                        workTabEnabled = workTabEnabled,
                        hasWorkProfile = hasWorkProfile,
                        onWorkTabEnabledChanged = { enabled ->
                            drawerPreferences.workTabEnabled = enabled
                            workTabEnabled = enabled
                        },
                        notificationDots = notificationDots,
                        onNotificationDotsChanged = { enabled ->
                            // One pref writer: the badge store owns the key and
                            // bumps the update flow so tiles rebind immediately.
                            NotificationBadges.enabled = enabled
                            notificationDots = enabled
                        },
                        notificationAccessGranted = notificationAccessGranted,
                        onNotificationAccessClicked = {
                            try {
                                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            } catch (e: ActivityNotFoundException) {
                                // The notification-access page is missing on this build.
                            }
                        },
                        drawerApps = drawerApps,
                        hiddenDrawerApps = hiddenDrawerApps,
                        onDrawerAppHiddenChanged = ::changeDrawerVisibility,
                    )
                }
            }
        }
        parkWindowFocus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
        navigation.open(intent)
        parkWindowFocus()
    }

    override fun onResume() {
        super.onResume()
        readPreferences()
        refreshDefaultHome()
        // The grant flips in the system page this row deep-links to.
        notificationAccessGranted = NotificationManagerCompat
            .getEnabledListenerPackages(this)
            .contains(packageName)
    }

    private fun parkWindowFocus() {
        // Keep initial keyboard focus off search, including on Android 8 and in non-touch mode.
        findViewById<ViewGroup>(android.R.id.content).apply {
            descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
            isFocusableInTouchMode = true
            requestFocus()
        }
    }

    private fun readPreferences() {
        themeMode = appearance.themeMode
        iconShape = appearance.iconShape
        iconPack = appearance.iconPack
        palette = appearance.palette
        showAppNames = appearance.showAppNames
        layoutLocked = workspace.layoutLocked
        pinNewApps = workspace.pinNewApps
        hideHomeAppNames = workspace.hideHomeAppNames
        showDock = dockPreferences.visible
        dockCapacity = dockPreferences.capacity
        homeOrientation = workspace.orientation
        drawerLayoutMode = drawerPreferences.layoutMode
        closeDrawerOnLaunch = drawerPreferences.closeOnLaunch
        rememberDrawerPosition = drawerPreferences.rememberPosition
        showKeyboardOnDrawerOpen = drawerPreferences.keyboardOnOpen
        drawerSearchActions = drawerPreferences.externalSearchActions
        drawerBackground = drawerPreferences.background
        homeIconPercent = workspace.iconScalePercent
        dockIconPercent = dockPreferences.iconScalePercent
        drawerIconPercent = drawerPreferences.iconScalePercent
        drawerFollowsHome = drawerPreferences.followsHomeIconScale
        workTabEnabled = drawerPreferences.workTabEnabled
        hasWorkProfile = userManager.userProfiles.any { it != android.os.Process.myUserHandle() }
        notificationDots = drawerPreferences.notificationDots
        hiddenDrawerApps = drawerPreferences.hiddenApps
        lifecycleScope.launch {
            iconPackOptions = withContext(Dispatchers.IO) {
                IconPackResolver(applicationContext, appearance).installedPacks()
            }
        }
    }

    private fun selectTheme(mode: ThemeMode) {
        if (mode == themeMode) return
        appearance.themeMode = mode
        themeMode = mode
    }

    private fun selectIconShape(shape: IconShapeMode) {
        if (shape == iconShape) return
        appearance.iconShape = shape
        iconShape = shape
    }

    /** Catalogue artwork rebuilds when Home resumes; this only records the choice. */
    private fun selectIconPack(packageName: String) {
        if (packageName == iconPack) return
        appearance.iconPack = packageName
        iconPack = packageName
    }

    private fun selectPalette(chosen: LauncherPalette) {
        if (chosen == palette) return
        appearance.palette = chosen
        palette = chosen
    }

    private fun changeAppNameVisibility(visible: Boolean) {
        appearance.showAppNames = visible
        showAppNames = visible
    }

    /** Rejects a reduction the two dock pages could not hold, keeping the saved slots. */
    private fun changeDockCapacity(slots: Int) {
        if (slots == dockCapacity) return
        if (!DockPages.canHold(dockPinCount, slots)) {
            Toast.makeText(this, R.string.toast_dock_capacity_pins, Toast.LENGTH_LONG).show()
            return
        }
        dockPreferences.capacity = slots
        dockCapacity = slots
    }

    /** Home applies the choice on its next resume; launched apps keep the device orientation. */
    private fun selectHomeOrientation(orientation: HomeOrientation) {
        if (orientation == homeOrientation) return
        workspace.orientation = orientation
        homeOrientation = orientation
    }

    private fun changeDrawerLayoutMode(mode: DrawerLayoutMode) {
        if (mode == drawerLayoutMode) return
        drawerPreferences.layoutMode = mode
        drawerLayoutMode = mode
    }

    private fun changeDrawerVisibility(app: AppKey, hidden: Boolean) {
        val updated = if (hidden) hiddenDrawerApps + app else hiddenDrawerApps - app
        drawerPreferences.hiddenApps = updated
        hiddenDrawerApps = updated
    }

    private fun refreshDefaultHome() {
        val checker = VesnaApplication.graph(this).homeRoleChecker
        val active = checker.isDefaultHome()
        setupIntent = if (active) null else checker.defaultHomeRequestIntent()
        defaultHome = when {
            active -> DefaultHomeState.ACTIVE
            setupIntent != null -> DefaultHomeState.AVAILABLE
            else -> DefaultHomeState.UNAVAILABLE
        }
    }

    private fun requestDefaultHome() {
        setupIntent?.let { homeRequest.launch(it) }
    }

    private fun readVersionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
    } catch (e: PackageManager.NameNotFoundException) {
        ""
    }

    /**
     * Writes the exported snapshot to the SAF location the user picked
     * (ADR-BACKUP-1 part 1). The document is written in one pass from the
     * fully built snapshot, so a failed write never leaves a torn layout
     * behind — the restore side validates the file again anyway.
     */
    private suspend fun exportLayout(repository: WorkspaceRepository, uriText: String): Boolean = try {
        val json = repository.exportLayout().toJson()
        contentResolver.openOutputStream(Uri.parse(uriText))?.use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
            stream.flush()
        } != null
    } catch (e: Exception) {
        false
    }

    /**
     * Reads and validates a snapshot file for the restore preview. Any parse
     * or validation failure maps to a rejection; the layout stays untouched
     * until the previewed confirmation.
     */
    private suspend fun openRestore(repository: WorkspaceRepository, uriText: String): LayoutRestoreAttempt = try {
        val text = contentResolver.openInputStream(Uri.parse(uriText))?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: return LayoutRestoreAttempt.Rejected()
        val snapshot = LayoutSnapshotV1.fromJson(text)
        LayoutRestoreAttempt.Ready(
            repository.restorePreview(snapshot) { pkg ->
                packageManager.getLaunchIntentForPackage(pkg) != null
            }
        )
    } catch (e: LayoutSnapshotException) {
        LayoutRestoreAttempt.Rejected(e.message)
    } catch (e: Exception) {
        LayoutRestoreAttempt.Rejected()
    }
}

/** The current Settings session survives configuration changes, not a new entry. */
private class SettingsNavigation(initialPage: SettingsPage) : ViewModel() {
    var page by mutableStateOf(initialPage)
        private set
    var query by mutableStateOf("")
    var searchTarget by mutableStateOf<Int?>(null)
        private set
    var entry by mutableIntStateOf(0)
        private set

    fun navigate(destination: SettingsPage, target: Int?) {
        query = ""
        searchTarget = target
        page = destination
    }

    fun open(intent: Intent) {
        navigate(SettingsPage.fromIntent(intent), target = null)
        entry++
    }
}
