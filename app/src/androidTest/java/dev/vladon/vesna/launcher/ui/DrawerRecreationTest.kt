package dev.vladon.vesna.launcher.ui

import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.VesnaApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawerRecreationTest {
    @Test
    fun recreationKeepsActiveSearchButNotADismissedDrawer() {
        val query = "Несуществующее приложение 314159"
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<DrawerView>(R.id.drawer).open()
                val search = activity.findViewById<EditText>(R.id.drawer_search)
                assertTrue(search.requestFocus())
                search.setText(query)
                search.setSelection(3)
            }

            scenario.recreate()
            scenario.onActivity { activity ->
                val search = activity.findViewById<EditText>(R.id.drawer_search)
                assertTrue("An open drawer must survive activity recreation", search.isShown)
                assertEquals(query, search.text.toString())
                assertEquals(3, search.selectionStart)
                assertTrue(search.hasFocus())
                assertTrue(activity.findViewById<View>(R.id.drawer_no_results).isShown)
                activity.onBackPressedDispatcher.onBackPressed()
            }

            scenario.recreate()
            scenario.onActivity { activity ->
                val search = activity.findViewById<EditText>(R.id.drawer_search)
                assertFalse("A dismissed drawer must stay closed", search.isShown)
                activity.findViewById<DrawerView>(R.id.drawer).open()
                assertTrue(search.text.isEmpty())
            }
        }
    }

    @Test
    fun recreationKeepsKeyboardFocusOnTheFilteredApp() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val controller = VesnaApplication.graph(instrumentation.targetContext).controller
        val app = withTimeout(10_000) { controller.state.first { it.apps.isNotEmpty() } }.apps.last()
        val query = app.label
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<DrawerView>(R.id.drawer).open()
                activity.findViewById<EditText>(R.id.drawer_search).setText(query)
            }
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
            assertTrue("The filtered app must accept keyboard focus before recreation", await(scenario) { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.drawer_list)
                (0 until list.childCount).asSequence().map(list::getChildAt)
                    .firstOrNull { it.contentDescription?.toString() == app.accessibilityLabel }
                    ?.requestFocus() == true
            })

            scenario.recreate()
            assertTrue("Keyboard focus should return to the same filtered app", await(scenario) { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.drawer_list)
                list.focusedChild?.contentDescription?.toString() == app.accessibilityLabel &&
                    activity.findViewById<EditText>(R.id.drawer_search).text.toString() == query
            })
        }
    }

    private fun await(scenario: ActivityScenario<HomeActivity>, condition: (HomeActivity) -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + 5_000
        do {
            var matched = false
            scenario.onActivity { matched = condition(it) }
            if (matched) return true
            SystemClock.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        return false
    }
}
