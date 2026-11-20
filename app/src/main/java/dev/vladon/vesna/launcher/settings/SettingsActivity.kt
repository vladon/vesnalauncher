package dev.vladon.vesna.launcher.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import dev.vladon.vesna.launcher.VesnaApplication

/** The app entry point for default launcher setup, appearance, and About. */
class SettingsActivity : ComponentActivity() {
    private lateinit var appearance: AppearancePreferences
    private var defaultHome by mutableStateOf(DefaultHomeState.UNAVAILABLE)
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    private var showAppNames by mutableStateOf(true)
    private var setupIntent: Intent? = null
    private val homeRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refreshDefaultHome() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appearance = AppearancePreferences(this)
        readAppearance()
        enableEdgeToEdge()
        val version = readVersionName()
        setContent {
            val dark = themeMode.isDark(isSystemInDarkTheme())
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            VesnaTheme(dark = dark) {
                SettingsScreen(
                    versionName = version,
                    defaultHome = defaultHome,
                    onSetDefaultHome = ::requestDefaultHome,
                    themeMode = themeMode,
                    onThemeSelected = ::selectTheme,
                    showAppNames = showAppNames,
                    onShowAppNamesChanged = ::changeAppNameVisibility,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        readAppearance()
        refreshDefaultHome()
    }

    private fun readAppearance() {
        themeMode = appearance.themeMode
        showAppNames = appearance.showAppNames
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
