package dev.vladon.vesna.launcher.settings

import android.content.Context
import androidx.core.content.edit

internal class AppearancePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("vesna_launcher", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() {
            val saved = prefs.getString(THEME_MODE, null)
            return ThemeMode.entries.firstOrNull { it.preferenceValue == saved } ?: ThemeMode.SYSTEM
        }
        set(value) = prefs.edit { putString(THEME_MODE, value.preferenceValue) }

    var showAppNames: Boolean
        get() = prefs.getBoolean(SHOW_APP_NAMES, true)
        set(value) = prefs.edit { putBoolean(SHOW_APP_NAMES, value) }

    private companion object {
        const val THEME_MODE = "theme_mode"
        const val SHOW_APP_NAMES = "show_app_names"
    }
}
