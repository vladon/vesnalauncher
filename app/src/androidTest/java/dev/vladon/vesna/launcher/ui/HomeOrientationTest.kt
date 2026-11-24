package dev.vladon.vesna.launcher.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.settings.HomeOrientation
import dev.vladon.vesna.launcher.settings.WorkspacePreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Home screen orientation choice (#189): the shipped default follows the
 * device, and the portrait lock applies on resume — the same path that picks up
 * a settings change without a relaunch. Home is the foreground launcher, so
 * scenario launch reuses the running instance and the resume re-read is what
 * the assertions observe.
 */
@RunWith(AndroidJUnit4::class)
class HomeOrientationTest {

    private fun preferences() =
        WorkspacePreferences(InstrumentationRegistry.getInstrumentation().targetContext)

    /** Launches (reusing the running Home) and cycles to RESUMED so the choice re-applies. */
    private fun resumedHome(): ActivityScenario<HomeActivity> {
        val scenario = ActivityScenario.launch(HomeActivity::class.java)
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        return scenario
    }

    @Test
    fun defaultFollowsTheDeviceAndPortraitLockApplies() {
        val preferences = preferences()
        val saved = preferences.orientation
        try {
            preferences.orientation = HomeOrientation.SYSTEM
            resumedHome().use { scenario ->
                scenario.onActivity { activity ->
                    assertEquals(
                        "No preference must follow the device",
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                        activity.requestedOrientation,
                    )
                }
            }
            preferences.orientation = HomeOrientation.PORTRAIT
            resumedHome().use { scenario ->
                scenario.onActivity { activity ->
                    assertEquals(
                        "The portrait lock applies",
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                        activity.requestedOrientation,
                    )
                    assertEquals(Configuration.ORIENTATION_PORTRAIT, activity.resources.configuration.orientation)
                }
            }
            preferences.orientation = HomeOrientation.SYSTEM
            resumedHome().use { scenario ->
                scenario.onActivity { activity ->
                    assertEquals(
                        "Switching back releases the lock",
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                        activity.requestedOrientation,
                    )
                }
            }
        } finally {
            preferences.orientation = saved
        }
    }
}
