package dev.vladon.vesna.launcher.settings

import android.content.Context
import androidx.core.content.edit

internal class DockPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("vesna_launcher", Context.MODE_PRIVATE)

    /** Dock icon artwork as a percentage of the default; slots, labels and saved pins are not scaled. */
    var iconScalePercent: Int
        get() = IconScale.coerce(prefs.getInt(ICON_SCALE_PERCENT, IconScale.DEFAULT))
        set(value) = prefs.edit { putInt(ICON_SCALE_PERCENT, value) }

    private companion object {
        const val ICON_SCALE_PERCENT = "dock_icon_scale_percent"
    }
}
