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

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val LAYOUT_LOCKED = "workspace_layout_locked"
        private const val PIN_NEW_APPS = "workspace_pin_new_apps"
    }
}
