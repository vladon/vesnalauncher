package dev.vladon.vesna.launcher.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.edit
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey

/** How the app drawer arranges apps while browsing; stored by stable name, not ordinal. */
internal enum class DrawerLayoutMode(@StringRes val title: Int, val preferenceValue: String) {
    GRID(R.string.settings_drawer_layout_grid, "grid"),
    LIST(R.string.settings_drawer_layout_list, "list"),
    PAGED(R.string.settings_drawer_layout_paged, "paged"),
}

/** Only drawer browsing consumes these keys; the launcher catalogue stays intact. */
internal class DrawerPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("vesna_launcher", Context.MODE_PRIVATE)

    var layoutMode: DrawerLayoutMode
        get() {
            val saved = prefs.getString(LAYOUT_MODE, null)
            return DrawerLayoutMode.entries.firstOrNull { it.preferenceValue == saved } ?: DrawerLayoutMode.GRID
        }
        set(value) = prefs.edit { putString(LAYOUT_MODE, value.preferenceValue) }

    var closeOnLaunch: Boolean
        get() = prefs.getBoolean(CLOSE_ON_LAUNCH, false)
        set(value) = prefs.edit { putBoolean(CLOSE_ON_LAUNCH, value) }

    var hiddenApps: Set<AppKey>
        get() = prefs.getStringSet(HIDDEN_APPS, null)?.mapNotNullTo(HashSet(), ::parseDrawerKey).orEmpty()
        set(value) = prefs.edit {
            putStringSet(HIDDEN_APPS, value.mapTo(HashSet(value.size), AppKey::drawerPreferenceKey))
        }

    companion object {
        private const val CLOSE_ON_LAUNCH = "drawer_close_on_launch"
        private const val HIDDEN_APPS = "drawer_hidden_apps"
        private const val LAYOUT_MODE = "drawer_layout_mode"
    }
}

internal fun AppKey.drawerPreferenceKey(): String = "$userSerial/$packageName/$className"

private fun parseDrawerKey(value: String): AppKey? {
    val parts = value.split('/', limit = 3)
    if (parts.size != 3 || parts[1].isEmpty() || parts[2].isEmpty()) return null
    val serial = parts[0].toLongOrNull() ?: return null
    return AppKey(parts[1], parts[2], serial)
}
