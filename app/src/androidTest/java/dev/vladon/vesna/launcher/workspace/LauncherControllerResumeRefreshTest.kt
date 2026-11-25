package dev.vladon.vesna.launcher.workspace

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.VesnaApplication
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The resume-refresh self-heal (#203): a framework that loses package events
 * (observed on a wedged AVD session where installs stopped reaching the
 * launcher and only a framework restart refreshed the drawer) self-heals when
 * Home re-enumerates the catalogue outside the event pipeline. The test drives
 * the same `refreshCatalogue` entry point Home's resume wiring calls and
 * asserts the catalogue reflects an externally installed package.
 */
@RunWith(AndroidJUnit4::class)
class LauncherControllerResumeRefreshTest {

    private fun shell(command: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            automation.executeShellCommand(command),
        ).use { it.readBytes() }
    }

    private fun pollUntil(predicate: () -> Boolean, timeoutMs: Long): Boolean {
        val deadline = android.os.SystemClock.uptimeMillis() + timeoutMs
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            if (predicate()) return true
            Thread.sleep(250)
        }
        return false
    }

    @Test
    fun refreshCatalogueReflectsExternallyInstalledPackages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val controller = VesnaApplication.graph(context).controller
        val fixture = "com.vesnaresearch.enumdiag"

        shell("pm uninstall $fixture")
        controller.refreshCatalogue()
        assertTrue(
            "The fixture starts absent from the catalogue",
            pollUntil({ controller.state.value.apps.none { it.key.packageName == fixture } }, 45_000),
        )

        shell("pm install /data/local/tmp/ed.apk")
        controller.refreshCatalogue()
        assertTrue(
            "The externally installed fixture joins the catalogue without relying on package events",
            pollUntil({ controller.state.value.apps.any { it.key.packageName == fixture } }, 45_000),
        )
    }
}
