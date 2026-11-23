package dev.vladon.vesna.launcher.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
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
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.vladon.vesna.launcher.VesnaApplication
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.GridCanvas

/** Entry point for Vesna's settings directory and named pages. */
class SettingsActivity : ComponentActivity() {
    private lateinit var workspace: WorkspacePreferences
    private lateinit var appearance: AppearancePreferences
    private lateinit var drawerPreferences: DrawerPreferences
    private val navigation: SettingsNavigation by viewModels {
        viewModelFactory { initializer { SettingsNavigation(SettingsPage.fromIntent(intent)) } }
    }
    private var defaultHome by mutableStateOf(DefaultHomeState.UNAVAILABLE)
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    private var layoutLocked by mutableStateOf(false)
    /** Null until the persisted grid arrives; the row must not flash the default. */
    private var grid by mutableStateOf<GridCanvas?>(null)
    private var pinNewApps by mutableStateOf(false)
    private var hideHomeAppNames by mutableStateOf(false)
    private var drawerLayoutMode by mutableStateOf(DrawerLayoutMode.GRID)
    private var closeDrawerOnLaunch by mutableStateOf(false)
    private var rememberDrawerPosition by mutableStateOf(false)
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
        drawerPreferences = DrawerPreferences(this)
        readPreferences()
        enableEdgeToEdge()
        val version = readVersionName()
        val workspaceRepository = VesnaApplication.graph(this).workspaceRepository
        setContent {
            LaunchedEffect(workspaceRepository) {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    workspaceRepository.grid.collect { grid = it }
                }
            }
            if (navigation.page == SettingsPage.APP_DRAWER) {
                LaunchedEffect(Unit) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        VesnaApplication.graph(this@SettingsActivity).controller.state.collect { drawerApps = it.apps }
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
            VesnaTheme(dark = dark) {
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
        showAppNames = appearance.showAppNames
        layoutLocked = workspace.layoutLocked
        pinNewApps = workspace.pinNewApps
        hideHomeAppNames = workspace.hideHomeAppNames
        drawerLayoutMode = drawerPreferences.layoutMode
        closeDrawerOnLaunch = drawerPreferences.closeOnLaunch
        rememberDrawerPosition = drawerPreferences.rememberPosition
        hiddenDrawerApps = drawerPreferences.hiddenApps
    }

    private fun selectTheme(mode: ThemeMode) {
        if (mode == themeMode) return
        appearance.themeMode = mode
        themeMode = mode
    }

    private fun changeAppNameVisibility(visible: Boolean) {
        appearance.showAppNames = visible
        showAppNames = visible
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
