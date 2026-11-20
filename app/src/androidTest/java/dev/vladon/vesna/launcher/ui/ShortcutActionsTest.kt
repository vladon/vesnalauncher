package dev.vladon.vesna.launcher.ui

import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShortcutActionsTest {
    @Test
    fun accessibilityOpensDrawerAppOptionsWithoutStartingADrag() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val menuItem = instrumentation.targetContext.getString(R.string.menu_add_to_workspace)
        val actionLabel = instrumentation.targetContext.getString(R.string.shortcut_options)
        ActivityScenario.launch(HomeActivity::class.java).use { home ->
            home.onActivity { it.findViewById<View>(R.id.home_open_drawer).performClick() }
            val action = AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK
            var acted = false
            assertTrue("No drawer tile accepted the accessibility action", until {
                home.onActivity { activity ->
                    val list = activity.findViewById<RecyclerView>(R.id.drawer_list)
                    val tile = list.getChildAt(0) ?: return@onActivity
                    val offered = tile.createAccessibilityNodeInfo().actionList.single { it.id == action.id }
                    assertEquals(actionLabel, offered.label.toString())
                    acted = tile.performAccessibilityAction(action.id, null)
                }
                acted
            })
            val automation = instrumentation.uiAutomation
            assertTrue("The drawer's Add to workspace menu did not open", until {
                automation.rootInActiveWindow?.findAccessibilityNodeInfosByText(menuItem)
                    ?.any { it.text?.toString() == menuItem } == true
            })
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            home.onActivity { assertTrue("Back closed the drawer instead of the menu", it.findViewById<View>(R.id.drawer_search).isShown) }
        }
    }

    private fun until(predicate: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + 5_000
        do {
            if (predicate()) return true
            SystemClock.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        return false
    }
}
