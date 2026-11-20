package dev.vladon.vesna.launcher.settings

import android.content.res.Configuration

internal enum class ThemeMode(val preferenceValue: String, val nightOverride: Int?) {
    SYSTEM("system", null),
    LIGHT("light", Configuration.UI_MODE_NIGHT_NO),
    DARK("dark", Configuration.UI_MODE_NIGHT_YES);

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}
