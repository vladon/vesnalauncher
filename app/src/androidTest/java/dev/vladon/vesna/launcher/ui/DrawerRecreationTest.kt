package dev.vladon.vesna.launcher.ui

import android.view.View
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.R
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
                activity.findViewById<View>(R.id.home_open_drawer).performClick()
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
                activity.findViewById<View>(R.id.home_open_drawer).performClick()
                assertTrue(search.text.isEmpty())
            }
        }
    }
}
