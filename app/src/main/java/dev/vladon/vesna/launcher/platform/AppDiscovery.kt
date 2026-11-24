package dev.vladon.vesna.launcher.platform

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.UserManager
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import java.util.Locale

/**
 * Discovers launchable applications using [LauncherApps], the public API designed
 * for home apps: one call enumerates the calling user's apps plus every managed
 * profile (for example the work profile), already badged for the profile.
 *
 * Package visibility on Android 10+ is granted by the single `<queries>` intent
 * declaration in the manifest; the app deliberately does not request
 * [android.Manifest.permission.QUERY_ALL_PACKAGES].
 *
 * Implementations must be callable from any thread.
 */
interface AppDiscovery {
    fun discover(): List<LauncherApp>

    /** Checks installation independently of whether a package is launchable. */
    fun isPackageInstalled(packageName: String, userSerial: Long): Boolean
}

internal class LauncherAppsAppDiscovery(
    private val appContext: Context,
    private val iconPacks: IconPackResolver,
) : AppDiscovery {

    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)

    init {
        check(launcherApps != null) { "LauncherApps system service is unavailable" }
    }

    override fun discover(): List<LauncherApp> {
        val result = ArrayList<LauncherApp>()
        val packageManager = appContext.packageManager
        val densityDpi = appContext.resources.displayMetrics.densityDpi
        for (profile in launcherApps.profiles) {
            val userSerial = userManager.getSerialNumberForUser(profile)
            // Android returns the original label for an unbadged profile.
            // Once it does, skip the call for the rest of that profile.
            var badgeLabels = true
            for (activity in launcherApps.getActivityList(null, profile)) {
                val component = activity.componentName
                // Vesna's own settings entry is reachable from the workspace menu; it
                // does not belong in the drawer.
                if (component.packageName == appContext.packageName) continue
                val label = activity.label.toString().ifBlank { component.packageName }
                val profileLabel = if (badgeLabels) packageManager.getUserBadgedLabel(label, profile) else label
                badgeLabels = profileLabel !== label
                // Pack artwork replaces the system icon before badging, so a work
                // profile still shows its badge on top of the pack's drawing.
                val packIcon = iconPacks.selectedDrawableFor(component.packageName, component.className, densityDpi)
                val icon = if (packIcon != null) {
                    packageManager.getUserBadgedIcon(packIcon, profile)
                } else {
                    activity.getBadgedIcon(densityDpi)
                }
                result.add(
                    LauncherApp(
                        key = AppKey(component.packageName, component.className, userSerial),
                        label = label,
                        icon = icon,
                        badgedLabel = if (badgeLabels) profileLabel.toString() else null,
                    ),
                )
            }
        }
        result.sortBy { it.label.lowercase(Locale.ROOT) }
        return result
    }

    override fun isPackageInstalled(packageName: String, userSerial: Long): Boolean {
        val profile = userManager.getUserForSerialNumber(userSerial) ?: return false
        return try {
            launcherApps.getApplicationInfo(packageName, PackageManager.MATCH_DISABLED_COMPONENTS, profile)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: SecurityException) {
            // Inaccessible profiles do not establish that a package was uninstalled.
            true
        }
    }
}

/**
 * Starts launchable activities through public APIs. Main-profile apps launch via
 * the classic intent path, which works regardless of whether Vesna holds the
 * default-home role; work-profile apps must go through
 * [LauncherApps.startMainActivity] because their components are not resolvable
 * from the main-profile context.
 */
class AppLauncher(private val appContext: Context) {

    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    internal val shortcuts = AppShortcuts(appContext)

    /**
     * @return false when the activity no longer exists (disabled or uninstalled since
     * the last discovery pass); the caller should refresh its app list. Failures that
     * leave the app installed (for example a locked work profile) also return false
     * without crashing, but the list itself is still correct.
     */
    fun launch(app: LauncherApp): Boolean {
        if (app.key.shortcutId != null) return shortcuts.launch(app.key)
        return tryStart {
            if (app.key.userSerial == MAIN_PROFILE_SERIAL || launcherApps == null) {
                appContext.startActivity(mainProfileIntent(app))
            } else {
                val user = userManager.getUserForSerialNumber(app.key.userSerial) ?: return false
                launcherApps.startMainActivity(ComponentName(app.key.packageName, app.key.className), user, null, null)
            }
        }
    }

    /** Opens the application's Settings page in the profile selected in Vesna. */
    internal fun openAppInfo(app: LauncherApp): Boolean {
        val launcher = launcherApps ?: return false
        val key = app.key
        return tryStart {
            val user = userManager.getUserForSerialNumber(key.userSerial) ?: return false
            launcher.startAppDetailsActivity(ComponentName(key.packageName, key.className), user, null, null)
        }
    }

    private inline fun tryStart(start: () -> Unit): Boolean =
        try {
            start()
            true
        } catch (e: ActivityNotFoundException) {
            // Stale entry: activity was disabled or removed since the last discovery pass.
            false
        } catch (e: SecurityException) {
            // Profile access was revoked or the profile is in restricted mode.
            false
        } catch (e: IllegalStateException) {
            // Work profile is turned off or in quiet mode.
            false
        }

    private fun mainProfileIntent(app: LauncherApp): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(app.key.packageName, app.key.className)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    private companion object {
        const val MAIN_PROFILE_SERIAL = 0L
    }
}
