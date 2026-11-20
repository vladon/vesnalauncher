package dev.vladon.vesna.launcher.settings

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit

internal enum class ThemeMode(val preferenceValue: String, val nightOverride: Int?) {
    SYSTEM("system", null),
    LIGHT("light", Configuration.UI_MODE_NIGHT_NO),
    DARK("dark", Configuration.UI_MODE_NIGHT_YES);

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        private const val PREFS_NAME = "vesna_launcher"
        private const val KEY = "theme_mode"

        fun read(context: Context): ThemeMode {
            val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY, null)
            return entries.firstOrNull { it.preferenceValue == saved } ?: SYSTEM
        }

        fun save(context: Context, mode: ThemeMode) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putString(KEY, mode.preferenceValue)
            }
        }
    }
}
