package dev.vladon.vesna.launcher.platform

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A change affecting an installed application package in a specific Android
 * profile ([userSerial] follows the UserManager serial convention, `0` = main).
 */
sealed interface PackageEvent {
    data class Added(val packageName: String, val userSerial: Long) : PackageEvent
    data class Removed(val packageName: String, val userSerial: Long) : PackageEvent

    /**
     * A package changed in place, including changes to launch availability.
     * Unlike [Removed], this does not establish that the package was uninstalled.
     */
    data class Replaced(val packageName: String, val userSerial: Long) : PackageEvent
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
            emit(PackageEvent.Added(packageName, serialOf(user)))
        }

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            emit(PackageEvent.Removed(packageName, serialOf(user)))
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            // Covers updates (replacing install) and enable/disable state changes.
            emit(PackageEvent.Replaced(packageName, serialOf(user)))
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String?>,
            user: UserHandle,
            replacing: Boolean,
        ) {
            // A package became launchable again (for example its storage returned).
            emitAll(packageNames, user, replacing) { name, serial ->
                PackageEvent.Added(name, serial)
            }
        }

        override fun onPackagesUnavailable(
            packageNames: Array<out String?>,
            user: UserHandle,
            replacing: Boolean,
        ) {
            // Temporarily unavailable storage is not an uninstall.
            emitAll(packageNames, user, replacing) { name, serial ->
                PackageEvent.Replaced(name, serial)
            }
        }
    }

    private fun emitAll(
        packageNames: Array<out String?>,
        user: UserHandle,
        replacing: Boolean,
        build: (String, Long) -> PackageEvent,
    ) {
        val serial = serialOf(user)
        for (name in packageNames) {
            if (name == null) continue
            emit(if (replacing) PackageEvent.Replaced(name, serial) else build(name, serial))
        }
    }

    private fun serialOf(user: UserHandle): Long = userManager.getSerialNumberForUser(user)

    private fun emit(event: PackageEvent) {
        _events.tryEmit(event)
    }

    fun start() {
        launcherApps?.registerCallback(callback, Handler(Looper.getMainLooper()))
    }

    fun stop() {
        launcherApps?.unregisterCallback(callback)
    }
}
