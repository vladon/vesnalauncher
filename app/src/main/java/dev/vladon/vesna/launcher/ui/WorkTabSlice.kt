package dev.vladon.vesna.launcher.ui

import dev.vladon.vesna.launcher.apps.LauncherApp

/**
 * The Work tab slice (#197): with the switch on and a managed profile present,
 * the Work tab lists only managed-profile apps and the Apps tab excludes them.
 */
internal object WorkTabSlice {
    fun apply(
        apps: List<LauncherApp>,
        tabEnabled: Boolean,
        workProfileUserSerial: Long?,
        workTabActive: Boolean,
    ): List<LauncherApp> {
        if (!tabEnabled || workProfileUserSerial == null) return apps
        return apps.filter { (it.key.userSerial == workProfileUserSerial) == workTabActive }
    }
}
