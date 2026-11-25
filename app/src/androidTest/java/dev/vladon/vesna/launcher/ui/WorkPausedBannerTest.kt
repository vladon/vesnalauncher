package dev.vladon.vesna.launcher.ui

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The paused-work banner (#198): with a managed profile in quiet mode the drawer
 * shows a notice with a resume action instead of silent tiles; without a pause
 * the banner stays hidden and no resume callback is armed.
 */
@RunWith(AndroidJUnit4::class)
class WorkPausedBannerTest {

    @Test
    fun bannerHiddenWithoutAPause() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = activity.findViewById<DrawerView>(R.id.drawer)
                drawer.workAppsPaused = false
                assertEquals(View.GONE, activity.findViewById<View>(R.id.drawer_work_paused).visibility)
                assertNull("No resume callback without a pause", drawer.onResumeWorkApps)
            }
        }
    }

    @Test
    fun pauseShowsBannerWithTheResumeAction() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = activity.findViewById<DrawerView>(R.id.drawer)
                var resumed = false
                drawer.onResumeWorkApps = { resumed = true }
                drawer.workAppsPaused = true
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.drawer_work_paused).visibility)
                assertEquals(
                    "The notice explains the paused state",
                    instrumentation.targetContext.getString(R.string.drawer_work_paused),
                    activity.findViewById<TextView>(R.id.drawer_work_paused_text).text,
                )
                val resume = activity.findViewById<Button>(R.id.drawer_work_resume)
                assertEquals(
                    "The resume action matches the acceptance wording",
                    instrumentation.targetContext.getString(R.string.drawer_work_resume),
                    resume.text,
                )
                resume.performClick()
                assertTrue("The resume action routes to the caller", resumed)
                assertFalse("The banner does not change the drawer's open state", drawer.isOpen)
            }
        }
    }
}
