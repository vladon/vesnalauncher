package dev.vladon.vesna.launcher.settings

import android.content.Context
import androidx.core.content.edit
import dev.vladon.vesna.launcher.apps.AppKey

/** Only drawer browsing consumes these keys; the launcher catalogue stays intact. */
internal class DrawerPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("vesna_launcher", Context.MODE_PRIVATE)

    var hiddenApps: Set<AppKey>
        get() = prefs.getStringSet(HIDDEN_APPS, null)?.mapNotNullTo(HashSet(), ::parseDrawerKey).orEmpty()
        set(value) = prefs.edit {
            putStringSet(HIDDEN_APPS, value.mapTo(HashSet(value.size), AppKey::drawerPreferenceKey))
        }

    companion object {
        private const val HIDDEN_APPS = "drawer_hidden_apps"
    }
}

internal fun AppKey.drawerPreferenceKey(): String = "$userSerial/$packageName/$className"

private fun parseDrawerKey(value: String): AppKey? {
    val parts = value.split('/', limit = 3)
    if (parts.size != 3 || parts[1].isEmpty() || parts[2].isEmpty()) return null
    val serial = parts[0].toLongOrNull() ?: return null
    return AppKey(parts[1], parts[2], serial)
}
