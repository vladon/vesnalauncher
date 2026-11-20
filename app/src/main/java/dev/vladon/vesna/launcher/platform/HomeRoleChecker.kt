package dev.vladon.vesna.launcher.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.app.role.RoleManager

/**
 * Detects whether Vesna is the current default home app and provides intents that
 * ask the user to select it through the system UI.
 */
class HomeRoleChecker(private val appContext: Context) {

    fun isDefaultHome(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = appContext.getSystemService(RoleManager::class.java) ?: return false
            return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = appContext.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == appContext.packageName
    }

    /**
     * An intent that opens the system "choose a home app" flow (role request on
     * Android 10+, the system home settings screen on older versions), or null when
     * no such flow is available.
     */
    fun defaultHomeRequestIntent(): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = appContext.getSystemService(RoleManager::class.java) ?: return null
            if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) return null
            return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        }
        return Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
            .takeIf { it.resolveActivity(appContext.packageManager) != null }
    }
}
