package dev.vladon.vesna.launcher.settings

import android.content.Context
import androidx.core.content.edit

internal class DockPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("vesna_launcher", Context.MODE_PRIVATE)

    /** The dock strip at the bottom of Home; pins are kept while it is hidden. */
    var visible: Boolean
        get() = prefs.getBoolean(DOCK_VISIBLE, true)
        set(value) = prefs.edit { putBoolean(DOCK_VISIBLE, value) }

    /** Dock icon artwork as a percentage of the default; slots, labels and saved pins are not scaled. */
    var iconScalePercent: Int
        get() = IconScale.coerce(prefs.getInt(ICON_SCALE_PERCENT, IconScale.DEFAULT))
        set(value) = prefs.edit { putInt(ICON_SCALE_PERCENT, value) }

    private companion object {
        const val DOCK_VISIBLE = "dock_visible"
        const val ICON_SCALE_PERCENT = "dock_icon_scale_percent"
    }
}
