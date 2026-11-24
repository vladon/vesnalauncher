package dev.vladon.vesna.launcher.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dev.vladon.vesna.launcher.workspace.DockPages

internal class DockPreferences internal constructor(private val prefs: SharedPreferences) {
    /** Home and Settings share the launcher preference file; the repository reuses its own. */
    constructor(context: Context) : this(context.getSharedPreferences("vesna_launcher", Context.MODE_PRIVATE))

    /** The dock strip at the bottom of Home; pins are kept while it is hidden. */
    var visible: Boolean
        get() = prefs.getBoolean(DOCK_VISIBLE, true)
        set(value) = prefs.edit { putBoolean(DOCK_VISIBLE, value) }

    /** Dock icon artwork as a percentage of the default; slots, labels and saved pins are not scaled. */
    var iconScalePercent: Int
        get() = IconScale.coerce(prefs.getInt(ICON_SCALE_PERCENT, IconScale.DEFAULT))
        set(value) = prefs.edit { putInt(ICON_SCALE_PERCENT, value) }

    /** Favorite slots per dock page; extra pins flow onto the second page in order. */
    var capacity: Int
        get() = DockPages.coerce(prefs.getInt(DOCK_CAPACITY, DockPages.DEFAULT_CAPACITY))
        set(value) = prefs.edit { putInt(DOCK_CAPACITY, DockPages.coerce(value)) }

    private companion object {
        const val DOCK_VISIBLE = "dock_visible"
        const val ICON_SCALE_PERCENT = "dock_icon_scale_percent"
        const val DOCK_CAPACITY = "dock_capacity"
    }
}
