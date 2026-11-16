package dev.vladon.vesna.launcher.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A change affecting an installed application package. */
sealed interface PackageEvent {
    data class Added(val packageName: String) : PackageEvent
    data class Removed(val packageName: String) : PackageEvent

    /** The package was updated in place; identifiers survive but metadata may change. */
    data class Replaced(val packageName: String) : PackageEvent
}

/**
 * Observes install, update, and uninstall broadcasts for the lifetime of the
 * launcher process.
 *
 * Registered with [ContextCompat.RECEIVER_NOT_EXPORTED]: protected system package
 * broadcasts are still delivered, while other apps cannot target this receiver.
 */
class PackageChangeMonitor(private val appContext: Context) {

    private val _events = MutableSharedFlow<PackageEvent>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<PackageEvent> = _events.asSharedFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val packageName = intent.data?.schemeSpecificPart ?: return
            val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            val event = when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED ->
                    if (replacing) PackageEvent.Replaced(packageName) else PackageEvent.Added(packageName)
                Intent.ACTION_PACKAGE_REMOVED ->
                    // An update reports removal first; the Replaced event covers it.
                    if (replacing) null else PackageEvent.Removed(packageName)
                Intent.ACTION_PACKAGE_REPLACED -> PackageEvent.Replaced(packageName)
                else -> null
            } ?: return
            _events.tryEmit(event)
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun stop() {
        appContext.unregisterReceiver(receiver)
    }
}
