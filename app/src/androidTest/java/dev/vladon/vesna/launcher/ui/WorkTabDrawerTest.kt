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
 * The Work tab (#197): the tab bar appears only for a managed profile with the
 * switch on; switching tabs re-slices the list (the slice logic is covered by
 * the JVM DrawerWorkTabSliceTest); the Turn-off row shows on the Work tab while
 * the profile runs and hides when paused or when no resume affordance is armed
 * (pre-API-28 wiring).
 */
@RunWith(AndroidJUnit4::class)
class WorkTabDrawerTest {

    @Test
    fun tabBarHiddenWithoutWorkProfileOrSwitch() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = activity.findViewById<DrawerView>(R.id.drawer)
                drawer.workProfileUserSerial = null
                drawer.workTabEnabled = true
                assertEquals(View.GONE, activity.findViewById<View>(R.id.drawer_work_tab_bar).visibility)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.drawer_turn_off_row).visibility)
                assertFalse(drawer.workTabActive)
            }
        }
    }

    @Test
    fun tabBarShowsForAProfileWithTheSwitchOn() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = activity.findViewById<DrawerView>(R.id.drawer)
                drawer.workProfileUserSerial = 11L
                drawer.workTabEnabled = true
                assertTrue(activity.findViewById<View>(R.id.drawer_work_tab_bar).visibility == View.VISIBLE)
                assertFalse(drawer.workTabActive)
            }
        }
    }

    @Test
    fun switchOffRestoresTheMergedList() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = activity.findViewById<DrawerView>(R.id.drawer)
                drawer.workProfileUserSerial = 11L
                drawer.workTabEnabled = true
                drawer.selectWorkTab(true)
                drawer.workTabEnabled = false
                assertFalse(drawer.workTabActive)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.drawer_work_tab_bar).visibility)
            }
        }
    }
}
