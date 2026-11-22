package dev.vladon.vesna.launcher.settings

import android.content.Intent
import androidx.annotation.StringRes
import dev.vladon.vesna.launcher.R

internal enum class SettingsPage(private val path: String, @StringRes val title: Int) {
    ROOT("/", R.string.menu_launcher_settings),
    DEFAULT_LAUNCHER("/default-launcher", R.string.settings_default_launcher),
    HOME_SCREEN("/home-screen", R.string.settings_home_screen),
    APP_DRAWER("/app-drawer", R.string.settings_app_drawer),
    APPEARANCE("/appearance", R.string.settings_appearance),
    ABOUT("/about", R.string.settings_about),
    ;

    companion object {
        fun fromIntent(intent: Intent): SettingsPage {
            val link = intent.data
            if (intent.action != Intent.ACTION_VIEW || link?.scheme != "vesna" || link.authority != "settings") {
                return ROOT
            }
            return entries.firstOrNull { it.path == link.encodedPath } ?: ROOT
        }
    }
}
