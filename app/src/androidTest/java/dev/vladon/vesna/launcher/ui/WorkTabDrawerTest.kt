package dev.vladon.vesna.launcher.ui

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Work tab: the tab bar appears for any managed profile (full Nova parity —
 * there is no opt-in switch); switching tabs re-slices the list (the slice logic
 * is covered by the JVM DrawerWorkTabSliceTest); the Turn-off row shows on the
 * Work tab while the profile runs and hides when paused or when no resume
 * affordance is armed (pre-API-28 wiring).
 */
class WorkTabDrawerTest {

    @Test
    fun tabBarHiddenWithoutWorkProfile() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = activity.findViewById<DrawerView>(R.id.drawer)
                drawer.workProfileUserSerial = null
                assertEquals(View.GONE, activity.findViewById<View>(R.id.drawer_work_tab_bar).visibility)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.drawer_turn_off_row).visibility)
                assertFalse(drawer.workTabActive)
            }
        }
    }

    @Test
    fun tabBarShowsForAWorkProfile() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = activity.findViewById<DrawerView>(R.id.drawer)
                drawer.workProfileUserSerial = 11L
                assertTrue(activity.findViewById<View>(R.id.drawer_work_tab_bar).visibility == View.VISIBLE)
                assertFalse(drawer.workTabActive)
            }
        }
    }

    @Test
    fun switchingBackToAppsKeepsTheBarAndRestoresTheMergedList() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = activity.findViewById<DrawerView>(R.id.drawer)
                drawer.workProfileUserSerial = 11L
                drawer.selectWorkTab(true)
                assertTrue(drawer.workTabActive)
                drawer.selectWorkTab(false)
                assertFalse(drawer.workTabActive)
                // The tab bar stays — the drawer remains tabbed on a managed profile.
                assertTrue(activity.findViewById<View>(R.id.drawer_work_tab_bar).visibility == View.VISIBLE)
            }
        }
    }
}
