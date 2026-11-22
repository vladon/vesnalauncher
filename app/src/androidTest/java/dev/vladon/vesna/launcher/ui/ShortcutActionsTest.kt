package dev.vladon.vesna.launcher.ui

import android.graphics.Rect
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
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

    @Test
    fun longMenuActionGrowsAndRemainsAccessible() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val label = List(3) { "Rename the folder containing shared work files" }.joinToString(" ")
        val shortLabel = "Cancel"
        var performed = false
        ActivityScenario.launch(HomeActivity::class.java).use { home ->
            home.onActivity { activity ->
                val anchor = activity.findViewById<View>(R.id.home_options)
                VesnaPopupMenu(anchor).apply {
                    add(label) { performed = true }
                    add(shortLabel) {}
                    show {}
                }
            }
            fun row(text: String) = automation.rootInActiveWindow?.findMenuAction(text)
            assertTrue("The long and short actions were not both visible", until {
                row(label) != null && row(shortLabel) != null
            })
            val longBounds = Rect().apply { row(label)!!.getBoundsInScreen(this) }
            val shortBounds = Rect().apply { row(shortLabel)!!.getBoundsInScreen(this) }
            assertTrue("The long label was constrained to one line", longBounds.height() > shortBounds.height())
            assertTrue(row(label)!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            home.onActivity { assertTrue("Accessibility did not activate the wrapped action", performed) }
        }
    }

    @Test
    fun returningHomeDismissesHomeOptionsWithoutRestartingTheActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val automation = instrumentation.uiAutomation
        val settings = context.getString(R.string.menu_launcher_settings)
        ActivityScenario.launch(HomeActivity::class.java).use { home ->
            lateinit var original: HomeActivity
            home.onActivity {
                original = it
                it.findViewById<View>(R.id.home_options).performClick()
            }
            fun originalHasWindowFocus(): Boolean {
                var focused = false
                home.onActivity {
                    assertSame(original, it)
                    focused = it.hasWindowFocus()
                }
                return focused
            }

            assertTrue("Home options did not open", until {
                automation.rootInActiveWindow?.findAccessibilityNodeInfosByText(settings)
                    ?.any { it.text?.toString() == settings } == true
            })
            assertTrue("Home options did not take window focus", until { !originalHasWindowFocus() })

            val command = "am start -W -a android.intent.action.MAIN -c android.intent.category.HOME " +
                "-f 0x10000000 -n ${context.packageName}/.ui.HomeActivity"
            ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use {
                val result = it.bufferedReader().readText()
                assertTrue(result, result.contains("Status: ok"))
            }
            // The active accessibility root can lag behind popup dismissal on Android 8.
            assertTrue("Home did not regain window focus after dismissing options", until(::originalHasWindowFocus))
        }
    }

    @Test
    fun rejectedDrawerLaunchAnnouncesFailureAndKeepsTheSearchEditable() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        // Present a stale drawer result without installing or disabling any package.
        // The actual Home click handler still calls Android to start the activity.
        val stale = LauncherApp(AppKey("dev.vladon.qa.missing", "dev.vladon.qa.missing.Main"), "Stale app")
        ActivityScenario.launch(HomeActivity::class.java).use { home ->
            home.onActivity { it.findViewById<View>(R.id.home_open_drawer).performClick() }
            assertTrue("The installed app list did not load", until {
                var catalogueLoaded = false
                home.onActivity {
                    catalogueLoaded = it.findViewById<RecyclerView>(R.id.drawer_list).childCount > 0
                }
                catalogueLoaded
            })
            home.onActivity { activity ->
                val drawer = activity.findViewById<DrawerView>(R.id.drawer)
                val search = drawer.findViewById<EditText>(R.id.drawer_search)
                drawer.submitApps(listOf(stale))
                search.setText(stale.label)
                assertTrue(search.requestFocus())
            }
            assertTrue("The stale result was not displayed", until {
                var shown = false
                home.onActivity {
                    val tile = it.findViewById<RecyclerView>(R.id.drawer_list).getChildAt(0)
                    shown = tile?.contentDescription == stale.label
                }
                shown
            })
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
            val message = instrumentation.targetContext.getString(R.string.app_launch_failed)
            assertTrue("Launch failure message was not visible to accessibility", until {
                automation.rootInActiveWindow?.hasVisibleText(message) == true
            })
            home.onActivity { activity ->
                val search = activity.findViewById<EditText>(R.id.drawer_search)
                assertTrue(search.isShown && search.hasFocus())
                assertEquals(stale.label, search.text.toString())
            }
        }
    }

    @Test
    fun cancelledDrawerHoldDoesNotTakeOverTheNextScroll() {
        val apps = List(96) { index ->
            val pkg = "dev.vladon.qa.drawer$index"
            LauncherApp(AppKey(pkg, "$pkg.Main"), "Fixture $index")
        }
        ActivityScenario.launch(HomeActivity::class.java).use { home ->
            home.onActivity { it.findViewById<View>(R.id.home_open_drawer).performClick() }
            fun withList(action: (RecyclerView) -> Unit) {
                home.onActivity { action(it.findViewById(R.id.drawer_list)) }
            }
            assertTrue("The installed app list did not load", until {
                var loaded = false
                withList { loaded = it.childCount > 0 }
                loaded
            })
            home.onActivity { it.findViewById<DrawerView>(R.id.drawer).submitApps(apps) }
            var ready = false
            assertTrue("The scroll fixture did not become visible", until {
                withList { list ->
                    val first = list.getChildAt(0) ?: return@withList
                    val host = list.rootView.findViewById<DrawerView>(R.id.drawer)
                    if (first.contentDescription == apps[0].label && host.translationY == 0f &&
                        list.canScrollVertically(1) && list.hasWindowFocus()
                    ) {
                        ready = true
                    }
                }
                ready
            })

            home.onActivity { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.drawer_list)
                val tile = list.getChildAt(0)
                val root = activity.findViewById<LauncherDragLayer>(R.id.launcher_root)
                val host = activity.findViewById<DrawerView>(R.id.drawer)
                val startDrag = checkNotNull(host.onItemDragStarted)

                fun bounds(view: View): Rect = Rect(0, 0, view.width, view.height).also {
                    root.offsetDescendantRectToMyCoords(view, it)
                }
                val viewport = bounds(list)

                fun send(action: Int, x: Int, y: Int, down: Long) {
                    val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x.toFloat(), y.toFloat(), 0)
                    try {
                        event.source = InputDevice.SOURCE_TOUCHSCREEN
                        root.dispatchTouchEvent(event)
                    } finally {
                        event.recycle()
                    }
                }

                val hold = SystemClock.uptimeMillis()
                val x = viewport.centerX()
                val bottom = viewport.top + viewport.height() * 3 / 4
                val top = viewport.top + viewport.height() / 4
                send(MotionEvent.ACTION_DOWN, x, bottom, hold)
                // Start at the handoff after the tile recognized the hold. The
                // drawer still owns the down stream because no move has occurred.
                startDrag(apps.first(), tile)
                send(MotionEvent.ACTION_CANCEL, x, bottom, hold)

                val swipe = SystemClock.uptimeMillis()
                send(MotionEvent.ACTION_DOWN, x, bottom, swipe)
                try {
                    send(MotionEvent.ACTION_MOVE, x, (bottom + top) / 2, swipe)
                    send(MotionEvent.ACTION_MOVE, x, top, swipe)
                    assertNotEquals(
                        "The next swipe must not close the drawer",
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
                        host.importantForAccessibility,
                    )
                    assertTrue("The next swipe must scroll", list.canScrollVertically(-1))
                } finally {
                    // Never commit a drop on a build where the regression still exists.
                    send(MotionEvent.ACTION_CANCEL, x, top, swipe)
                }
            }
        }
    }

    private fun AccessibilityNodeInfo.hasVisibleText(expected: String): Boolean =
        (isVisibleToUser && text?.toString() == expected) ||
            (0 until childCount).any { getChild(it)?.hasVisibleText(expected) == true }

    private fun AccessibilityNodeInfo.findMenuAction(expected: String): AccessibilityNodeInfo? {
        if (isVisibleToUser && isClickable && text?.toString() == expected) return this
        for (index in 0 until childCount) {
            getChild(index)?.findMenuAction(expected)?.let { return it }
        }
        return null
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
