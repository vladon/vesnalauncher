package dev.vladon.vesna.launcher.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import java.util.Locale

/**
 * Discovers launchable applications using public PackageManager APIs.
 *
 * Package visibility on Android 10+ is granted by the single `<queries>` intent
 * declaration in the manifest; the app deliberately does not request
 * [android.Manifest.permission.QUERY_ALL_PACKAGES].
 *
 * Implementations must be callable from any thread.
 */
interface AppDiscovery {
    fun discover(): List<LauncherApp>
}

class PackageManagerAppDiscovery(private val appContext: Context) : AppDiscovery {

    override fun discover(): List<LauncherApp> {
        val pm = appContext.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val self = appContext.packageName
        val result = ArrayList<LauncherApp>()
        for (info in pm.queryIntentActivities(mainIntent, 0)) {
            val packageName = info.activityInfo.packageName
            // Vesna's own settings entry is reachable from the workspace menu; it does
            // not belong in the drawer.
            if (packageName == self || info.activityInfo.name == null) continue
            result.add(
                LauncherApp(
                    key = AppKey(packageName, info.activityInfo.name),
                    label = info.loadLabel(pm).toString().ifBlank { packageName },
                    icon = info.loadIcon(pm),
                ),
            )
        }
        result.sortBy { it.label.lowercase(Locale.ROOT) }
        return result
    }
}

/**
 * Starts a launchable activity through the standard public intent mechanism, which
 * works regardless of whether Vesna currently holds the default-home role.
 */
class AppLauncher(private val appContext: Context) {

    fun launch(app: LauncherApp) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(app.key.packageName, app.key.className)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        appContext.startActivity(intent)
    }
}
