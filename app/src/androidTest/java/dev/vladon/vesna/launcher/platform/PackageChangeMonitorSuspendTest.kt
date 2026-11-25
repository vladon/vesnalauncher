package dev.vladon.vesna.launcher.platform

import android.os.Process
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Work-profile suspension (#199, #198): a paused profile changes which activities
 * launch, so suspend/unsuspend callbacks must refresh the catalogue exactly like
 * install and availability events do.
 */
@RunWith(AndroidJUnit4::class)
class PackageChangeMonitorSuspendTest {

    @Test
    fun suspendAndUnsuspendEmitCatalogueRefreshes() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val monitor = PackageChangeMonitor(context)
        monitor.start()
        try {
            val user = Process.myUserHandle()
            monitor.callback.onPackagesSuspended(arrayOf("qa.suspended" as String?), user)
            assertEquals(PackageEvent.CatalogueChanged, withTimeout(5_000) { monitor.events.first() })
            monitor.callback.onPackagesUnsuspended(arrayOf("qa.suspended" as String?), user)
            assertEquals(PackageEvent.CatalogueChanged, withTimeout(5_000) { monitor.events.first() })
        } finally {
            monitor.stop()
        }
    }
}
