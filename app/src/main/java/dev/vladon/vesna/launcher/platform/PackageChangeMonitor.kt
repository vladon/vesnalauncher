package dev.vladon.vesna.launcher.platform

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface PackageEvent {
    /** Refresh the launchable app list without treating the change as an uninstall. */
    data object CatalogueChanged : PackageEvent

    /** Publisher actions can change without a package update. */
    data object ShortcutsChanged : PackageEvent

    /** A fresh install in one Android profile, not a replacing update. */
    data class Added(val packageName: String, val userSerial: Long) : PackageEvent

    /** An uninstall in one Android profile. */
    data class Removed(val packageName: String, val userSerial: Long) : PackageEvent
}

/**
 * Observes install, update, and uninstall events for the launcher process across
 * all accessible Android profiles via [LauncherApps.registerPackageCallback], which
 * supersedes the single-profile PACKAGE_* broadcasts.
 */
class PackageChangeMonitor(private val appContext: Context) {

    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)

    private val _events = MutableSharedFlow<PackageEvent>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<PackageEvent> = _events.asSharedFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) {
            _events.tryEmit(PackageEvent.Added(packageName, userManager.getSerialNumberForUser(user)))
        }

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            _events.tryEmit(PackageEvent.Removed(packageName, userManager.getSerialNumberForUser(user)))
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            // Covers updates (replacing install) and enable/disable state changes.
            catalogueChanged()
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String?>,
            user: UserHandle,
            replacing: Boolean,
        ) = onAvailabilityChanged(packageNames)

        override fun onPackagesUnavailable(
            packageNames: Array<out String?>,
            user: UserHandle,
            replacing: Boolean,
        ) = onAvailabilityChanged(packageNames)

        override fun onShortcutsChanged(packageName: String, shortcuts: MutableList<ShortcutInfo>, user: UserHandle) {
            _events.tryEmit(PackageEvent.ShortcutsChanged)
        }
    }

    private fun onAvailabilityChanged(packageNames: Array<out String?>) {
        // Availability changes are not uninstalls. Refresh the whole catalogue once per batch.
        if (packageNames.any { it != null }) catalogueChanged()
    }

    private fun catalogueChanged() {
        _events.tryEmit(PackageEvent.CatalogueChanged)
    }

    fun start() {
        launcherApps?.registerCallback(callback, Handler(Looper.getMainLooper()))
    }

    fun stop() {
        launcherApps?.unregisterCallback(callback)
    }
}
