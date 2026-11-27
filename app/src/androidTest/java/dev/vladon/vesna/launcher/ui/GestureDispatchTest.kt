package dev.vladon.vesna.launcher.ui

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.settings.GestureAction
import dev.vladon.vesna.launcher.settings.GestureId
import dev.vladon.vesna.launcher.settings.WorkspacePreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Home gesture bindings (#183 phase 1): the two-finger swipe up, the
 * pinch in and out on an empty grid dispatch the bound action and never
 * fire during a one-finger page swipe. The events are constructed
 * programmatically because the shell injection cannot drive two pointers.
 */
class GestureDispatchTest {

    private fun twoPointerEvent(
        action: Int,
        x0: Float, y0: Float,
        x1: Float?, y1: Float?,
    ): MotionEvent {
        val props0 = MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER }
        val coords0 = MotionEvent.PointerCoords().apply { x = x0; y = y0; pressure = 1f; size = 1f }
        if (x1 == null || y1 == null) {
            return MotionEvent.obtain(0, System.currentTimeMillis(), action, x0, y0, 0)
        }
        val props1 = MotionEvent.PointerProperties().apply { id = 1; toolType = MotionEvent.TOOL_TYPE_FINGER }
        val coords1 = MotionEvent.PointerCoords().apply { x = x1; y = y1; pressure = 1f; size = 1f }
        return MotionEvent.obtain(
            0, System.currentTimeMillis(), action, 2, arrayOf(props0, props1),
            arrayOf(coords0, coords1), 0, 0, 1f, 1f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
    }

    private fun moveEvent(action: Int, x0: Float, y0: Float, x1: Float, y1: Float): MotionEvent {
        val props0 = MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER }
        val props1 = MotionEvent.PointerProperties().apply { id = 1; toolType = MotionEvent.TOOL_TYPE_FINGER }
        val coords0 = MotionEvent.PointerCoords().apply { x = x0; y = y0; pressure = 1f; size = 1f }
        val coords1 = MotionEvent.PointerCoords().apply { x = x1; y = y1; pressure = 1f; size = 1f }
        return MotionEvent.obtain(
            0, System.currentTimeMillis(), action, 2, arrayOf(props0, props1),
            arrayOf(coords0, coords1), 0, 0, 1f, 1f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
    }

    @Test
    fun twoFingerSwipeUpRunsBoundDrawerAction() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        WorkspacePreferences(context).setGestureBinding(GestureId.TWO_FINGER_SWIPE_UP, GestureAction.OPEN_DRAWER)
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val workspace = activity.findViewById<View>(R.id.workspace)
                listOf(
                    twoPointerEvent(MotionEvent.ACTION_DOWN, 500f, 1200f, null, null),
                    twoPointerEvent(MotionEvent.ACTION_POINTER_DOWN, 500f, 1200f, 620f, 1200f),
                    moveEvent(MotionEvent.ACTION_MOVE, 500f, 1150f, 620f, 1150f),
                    moveEvent(MotionEvent.ACTION_MOVE, 500f, 1050f, 620f, 1050f),
                    moveEvent(MotionEvent.ACTION_MOVE, 500f, 950f, 620f, 950f),
                    twoPointerEvent(MotionEvent.ACTION_UP, 500f, 950f, null, null),
                ).forEach(workspace::dispatchTouchEvent)
                val drawer = activity.findViewById<DrawerView>(R.id.drawer)
                assertTrue(drawer.isOpen)
            }
        }
    }

    @Test
    fun pinchInRunsBoundDrawerAction() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        WorkspacePreferences(context).setGestureBinding(GestureId.PINCH_IN, GestureAction.OPEN_DRAWER)
        WorkspacePreferences(context).setGestureBinding(GestureId.TWO_FINGER_SWIPE_UP, GestureAction.NONE)
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val workspace = activity.findViewById<WorkspaceView>(R.id.workspace)
                listOf(
                    twoPointerEvent(MotionEvent.ACTION_DOWN, 300f, 1200f, null, null),
                    twoPointerEvent(MotionEvent.ACTION_POINTER_DOWN, 300f, 1200f, 840f, 1200f),
                    moveEvent(MotionEvent.ACTION_MOVE, 350f, 1200f, 790f, 1200f),
                    moveEvent(MotionEvent.ACTION_MOVE, 420f, 1200f, 720f, 1200f),
                    moveEvent(MotionEvent.ACTION_MOVE, 480f, 1200f, 660f, 1200f),
                    moveEvent(MotionEvent.ACTION_MOVE, 520f, 1200f, 620f, 1200f),
                    twoPointerEvent(MotionEvent.ACTION_UP, 520f, 1200f, null, null),
                ).forEach(workspace::dispatchTouchEvent)
                val drawer = activity.findViewById<DrawerView>(R.id.drawer)
                assertEquals(dev.vladon.vesna.launcher.settings.GestureId.PINCH_IN, workspace.lastFiredGesture)
                val bound = WorkspacePreferences(context).gestureBinding(GestureId.PINCH_IN)
                assertTrue(
                    "binding=" + bound + " isOpen=" + drawer.isOpen + " vis=" + drawer.visibility,
                    drawer.isOpen && bound == GestureAction.OPEN_DRAWER,
                )
            }
        }
    }

    @Test
    fun unassignedPinchOutDoesNothing() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        WorkspacePreferences(context).setGestureBinding(GestureId.PINCH_OUT, GestureAction.NONE)
        WorkspacePreferences(context).setGestureBinding(GestureId.TWO_FINGER_SWIPE_UP, GestureAction.NONE)
        WorkspacePreferences(context).setGestureBinding(GestureId.PINCH_IN, GestureAction.NONE)
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val workspace = activity.findViewById<View>(R.id.workspace)
                listOf(
                    twoPointerEvent(MotionEvent.ACTION_DOWN, 500f, 1200f, null, null),
                    twoPointerEvent(MotionEvent.ACTION_POINTER_DOWN, 500f, 1200f, 560f, 1200f),
                    moveEvent(MotionEvent.ACTION_MOVE, 460f, 1200f, 640f, 1200f),
                    moveEvent(MotionEvent.ACTION_MOVE, 420f, 1200f, 700f, 1200f),
                    moveEvent(MotionEvent.ACTION_MOVE, 380f, 1200f, 760f, 1200f),
                    twoPointerEvent(MotionEvent.ACTION_UP, 380f, 1200f, null, null),
                ).forEach(workspace::dispatchTouchEvent)
                val drawer = activity.findViewById<DrawerView>(R.id.drawer)
                assertFalse(drawer.isOpen)
            }
        }
    }

    @Test
    fun secondFingerDuringPageSwipeSuppressesGestures() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        WorkspacePreferences(context).setGestureBinding(GestureId.TWO_FINGER_SWIPE_UP, GestureAction.OPEN_DRAWER)
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val workspace = activity.findViewById<View>(R.id.workspace)
                // A one-finger page swipe starts (past the 56 dp threshold)…
                workspace.dispatchTouchEvent(twoPointerEvent(MotionEvent.ACTION_DOWN, 900f, 1600f, null, null))
                workspace.dispatchTouchEvent(moveEvent(MotionEvent.ACTION_MOVE, 850f, 1600f, 850f, 1600f))
                workspace.dispatchTouchEvent(twoPointerEvent(MotionEvent.ACTION_MOVE, 700f, 1600f, 700f, 1600f))
                // …then the second finger arrives and both travel upward.
                workspace.dispatchTouchEvent(twoPointerEvent(MotionEvent.ACTION_POINTER_DOWN, 700f, 1600f, 820f, 1620f))
                workspace.dispatchTouchEvent(moveEvent(MotionEvent.ACTION_MOVE, 700f, 1500f, 820f, 1520f))
                workspace.dispatchTouchEvent(moveEvent(MotionEvent.ACTION_MOVE, 700f, 1350f, 820f, 1370f))
                workspace.dispatchTouchEvent(moveEvent(MotionEvent.ACTION_MOVE, 700f, 1200f, 820f, 1220f))
                workspace.dispatchTouchEvent(twoPointerEvent(MotionEvent.ACTION_UP, 700f, 1200f, null, null))
                val drawer = activity.findViewById<DrawerView>(R.id.drawer)
                assertFalse(drawer.isOpen)
            }
        }
    }
}
