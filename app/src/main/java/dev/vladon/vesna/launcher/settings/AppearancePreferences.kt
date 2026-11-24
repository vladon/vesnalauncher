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

    var iconShape: IconShapeMode
        get() {
            val saved = prefs.getString(ICON_SHAPE, null)
            return IconShapeMode.entries.firstOrNull { it.preferenceValue == saved } ?: IconShapeMode.SYSTEM
        }
        set(value) = prefs.edit { putString(ICON_SHAPE, value.preferenceValue) }

    var palette: LauncherPalette
        get() {
            val saved = prefs.getString(PALETTE, null)
            return LauncherPalette.entries.firstOrNull { it.preferenceValue == saved } ?: LauncherPalette.VESNA
        }
        set(value) = prefs.edit { putString(PALETTE, value.preferenceValue) }

    /** Package name of the selected icon pack, or [SYSTEM_ICON_PACK] for system artwork. */
    var iconPack: String
        get() = prefs.getString(ICON_PACK, null) ?: SYSTEM_ICON_PACK
        set(value) = prefs.edit { putString(ICON_PACK, value) }

    var showAppNames: Boolean
        get() = prefs.getBoolean(SHOW_APP_NAMES, true)
        set(value) = prefs.edit { putBoolean(SHOW_APP_NAMES, value) }

    internal companion object {
        const val THEME_MODE = "theme_mode"
        const val SHOW_APP_NAMES = "show_app_names"
        const val ICON_SHAPE = "icon_shape"
        const val PALETTE = "palette"
        private const val ICON_PACK = "icon_pack"

        /** The preference value meaning system artwork, not an installed pack. */
        const val SYSTEM_ICON_PACK = "system"
    }
}
