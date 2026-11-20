package dev.vladon.vesna.launcher.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.vladon.vesna.launcher.VesnaApplication

/** The app entry point for default launcher setup and About. */
class SettingsActivity : ComponentActivity() {
    private var defaultHome by mutableStateOf(DefaultHomeState.UNAVAILABLE)
    private var setupIntent: Intent? = null
    private val homeRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refreshDefaultHome() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val version = readVersionName()
        setContent {
            VesnaTheme {
                SettingsScreen(versionName = version, defaultHome = defaultHome, onSetDefaultHome = ::requestDefaultHome)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDefaultHome()
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
