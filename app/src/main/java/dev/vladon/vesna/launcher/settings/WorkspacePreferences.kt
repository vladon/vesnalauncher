package dev.vladon.vesna.launcher.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

internal class WorkspacePreferences(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("vesna_launcher", Context.MODE_PRIVATE))

    var layoutLocked: Boolean
        get() = prefs.getBoolean(LAYOUT_LOCKED, false)
        set(value) = prefs.edit { putBoolean(LAYOUT_LOCKED, value) }

    var pinNewApps: Boolean
        get() = prefs.getBoolean(PIN_NEW_APPS, false)
        set(value) = prefs.edit { putBoolean(PIN_NEW_APPS, value) }

    /**
     * #177: whether an unlocked session relocks after launching an app or
     * turning the display off. On by default; the stored layout lock itself
     * is never touched by the relock.
     */
    var autoRelock: Boolean
        get() = prefs.getBoolean(WorkspacePreferences.AUTO_RELOCK, true)
        set(value) = prefs.edit { putBoolean(WorkspacePreferences.AUTO_RELOCK, value) }

    /**
     * #186: placement of the optional persistent search bar on Home.
     */
    var searchBarPlacement: HomeSearchBarPlacement
        get() = HomeSearchBarPlacement.fromPreference(prefs.getString(SEARCH_BAR_PLACEMENT, null))
        set(value) = prefs.edit { putString(SEARCH_BAR_PLACEMENT, value.preferenceValue) }

    /**
     * #173: Home and dock label size, as a percent of the default text.
     */
    var labelFontScalePercent: Int
        get() = LabelFontScale.coerce(prefs.getInt(LABEL_FONT_SCALE_PERCENT, LabelFontScale.DEFAULT))
        set(value) = prefs.edit { putInt(LABEL_FONT_SCALE_PERCENT, LabelFontScale.coerce(value)) }

    /**
     * #173: Home and dock label font face.
     */
    var labelFont: LabelFont
        get() = LabelFont.fromPreference(prefs.getString(LABEL_FONT, null))
        set(value) = prefs.edit { putString(LABEL_FONT, value.preferenceValue) }

    var hideHomeAppNames: Boolean
        get() = prefs.getBoolean(HIDE_HOME_APP_NAMES, false)
        set(value) = prefs.edit { putBoolean(HIDE_HOME_APP_NAMES, value) }

    /** Home icon artwork as a percentage of the default; cells and labels are not scaled. */
    var iconScalePercent: Int
        get() = IconScale.coerce(prefs.getInt(ICON_SCALE_PERCENT, IconScale.DEFAULT))
        set(value) = prefs.edit { putInt(ICON_SCALE_PERCENT, value) }

    /** Whether the Home window follows the device rotation or stays portrait. */
    var orientation: HomeOrientation
        get() {
            val saved = prefs.getString(HOME_ORIENTATION, null)
            return HomeOrientation.entries.firstOrNull { it.preferenceValue == saved } ?: HomeOrientation.SYSTEM
        }
        set(value) = prefs.edit { putString(HOME_ORIENTATION, value.preferenceValue) }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val LAYOUT_LOCKED = "workspace_layout_locked"
        private const val PIN_NEW_APPS = "workspace_pin_new_apps"
        private const val HIDE_HOME_APP_NAMES = "workspace_hide_app_names"
        private const val ICON_SCALE_PERCENT = "workspace_icon_scale_percent"
        private const val HOME_ORIENTATION = "home_orientation"

        /** #177: whether an unlocked session relocks after an app launch or the screen turning off. */
        const val AUTO_RELOCK = "workspace_auto_relock"
        const val SEARCH_BAR_PLACEMENT = "home_search_bar_placement"
        const val LABEL_FONT_SCALE_PERCENT = "home_label_font_scale_percent"
        const val LABEL_FONT = "home_label_font"
    }
}
