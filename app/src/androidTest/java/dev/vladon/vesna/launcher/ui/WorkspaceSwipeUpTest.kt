package dev.vladon.vesna.launcher.ui

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.CellContent
import dev.vladon.vesna.launcher.workspace.GridCanvas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/** The drawer opens for a fast fling and for a long slow drag; a moving finger never opens the workspace menu. */
@RunWith(AndroidJUnit4::class)
class WorkspaceSwipeUpTest {

    @Test
    fun longSwipeNeedsItsDistanceLittleDriftAndASingleFinger() = withWorkspace { swipe ->
        swipe.slow(distanceDp = 300, durationMs = 1500)
        assertEquals("A slow swipe must travel the full distance", 0, swipe.opens.get())
        swipe.slow(distanceDp = 370, durationMs = 1500)
        assertEquals(1, swipe.opens.get())
        swipe.slow(distanceDp = 370, durationMs = 1500, driftDp = 120)
        assertEquals("A drifting drag is not a drawer swipe", 1, swipe.opens.get())
        swipe.slow(distanceDp = 370, durationMs = 1500, secondFinger = true)
        assertEquals("A second finger cancels the long swipe", 1, swipe.opens.get())
    }

    @Test
    fun aLongFastSwipeOpensTheDrawerOnlyOnce() = withWorkspace { swipe ->
        swipe.slow(distanceDp = 400, durationMs = 150)
        assertEquals(1, swipe.opens.get())
    }

    @Test
    fun aShortWorkspaceAsksForHalfItsHeight() = withWorkspace(heightDp = 240) { swipe ->
        swipe.slow(distanceDp = 100, durationMs = 800)
        assertEquals(0, swipe.opens.get())
        swipe.slow(distanceDp = 150, durationMs = 800)
        assertEquals(1, swipe.opens.get())
    }

    @Test
    fun slowLongSwipeOpensTheDrawerOnceWithoutTheWorkspaceMenu() = onHome { swipe ->
        swipe.slow(distanceDp = 400, durationMs = 800)
        assertEquals(1, swipe.opens.get())
        assertEquals("A moving finger must not open the workspace menu", 0, swipe.menus.get())
    }

    @Test
    fun slowShortSwipeOpensNeitherSurface() = onHome { swipe ->
        swipe.slow(distanceDp = 150, durationMs = 800)
        assertEquals(0, swipe.opens.get())
        assertEquals(0, swipe.menus.get())
    }

    @Test
    fun fastShortSwipeKeepsOpeningTheDrawer() = onHome { swipe ->
        swipe.slow(distanceDp = 150, durationMs = 100)
        assertEquals(1, swipe.opens.get())
        assertEquals(0, swipe.menus.get())
    }

    @Test
    fun stationaryHoldStillOpensTheWorkspaceMenu() = onHome { swipe ->
        swipe.hold()
        assertEquals(0, swipe.opens.get())
        assertEquals(1, swipe.menus.get())
    }

    @Test
    fun aTileHoldStillLiftsWhenTheFingerWandersInsideTheTile() {
        var swipe: TouchStream? = null
        ActivityScenario.launch(HomeActivity::class.java).use { home ->
            SystemClock.sleep(600) // Let the saved layout render before the grid is replaced.
            var tile: IntArray? = null
            home.onActivity { activity ->
                val workspace = activity.findViewById<WorkspaceView>(R.id.workspace)
                workspace.bind(
                    GridCanvas.DEFAULT,
                    mapOf(0 to CellContent.App(LauncherApp(AppKey("qa.hold", "Main"), "Tile"))),
                )
                swipe = TouchStream(workspace, realTime = true)
            }
            val deadline = SystemClock.uptimeMillis() + 5_000
            do {
                home.onActivity { activity ->
                    val view = activity.findViewById<WorkspaceView>(R.id.workspace).findChildForCell(0)
                    if (view != null && view.width > 0) tile = intArrayOf(view.left, view.top, view.width, view.height)
                }
                if (tile == null) SystemClock.sleep(50)
            } while (tile == null && SystemClock.uptimeMillis() < deadline)
            val (left, top, width, height) = requireNotNull(tile) { "The tile never laid out" }
            val x = left + width / 2f
            val y = top + height / 2f
            val stream = swipe!!

            stream.press(x, y)
            SystemClock.sleep(150)
            // Past the touch slop, but still inside the tile: the hold must stay armed.
            stream.move(x + 15 * stream.density, y)
            SystemClock.sleep(700)
            var dragging = false
            home.onActivity { dragging = it.findViewById<WorkspaceView>(R.id.workspace).isDragging }
            assertTrue("A wandering hold must still lift the tile", dragging)
            stream.cancel(x, y)
            home.onActivity { dragging = it.findViewById<WorkspaceView>(R.id.workspace).isDragging }
            assertFalse(dragging)
        }
    }

    /** A laid-out view: event times are synthetic, so the platform long press can never fire here. */
    private fun withWorkspace(heightDp: Int = 900, check: (TouchStream) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var result: Result<Unit>? = null
        instrumentation.runOnMainSync {
            result = runCatching {
                val workspace = WorkspaceView(instrumentation.targetContext)
                val width = (320 * workspace.resources.displayMetrics.density).toInt()
                val height = (heightDp * workspace.resources.displayMetrics.density).toInt()
                workspace.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                workspace.layout(0, 0, width, height)
                val swipe = TouchStream(workspace, realTime = false)
                workspace.onSwipeUp = { swipe.opens.incrementAndGet() }
                check(swipe)
            }
        }
        result!!.getOrThrow()
    }

    /** The real Home workspace, attached, with real time so the platform long press can fire. */
    private fun onHome(check: (TouchStream) -> Unit) {
        ActivityScenario.launch(HomeActivity::class.java).use { home ->
            SystemClock.sleep(600) // Let the saved layout render before the grid is replaced.
            var swipe: TouchStream? = null
            home.onActivity { activity ->
                val workspace = activity.findViewById<WorkspaceView>(R.id.workspace)
                assertTrue("The workspace must be laid out", workspace.width > 0 && workspace.height > 0)
                // An empty grid lets every touch start on the area whose long press opens the menu.
                workspace.bind(GridCanvas.DEFAULT, emptyMap())
                val created = TouchStream(workspace, realTime = true)
                workspace.onSwipeUp = { created.opens.incrementAndGet() }
                workspace.onEmptyAreaLongClick = { created.menus.incrementAndGet() }
                swipe = created
            }
            check(swipe!!)
        }
    }

    private class TouchStream(val workspace: WorkspaceView, val realTime: Boolean) {
        val opens = AtomicInteger()
        val menus = AtomicInteger()
        val density = workspace.resources.displayMetrics.density
        private val touchSlop = ViewConfiguration.get(workspace.context).scaledTouchSlop
        private val instrumentation = InstrumentationRegistry.getInstrumentation()
        private var downTime = 0L
        private var eventTime = 0L
        private var stepMs = 20L

        /** A straight upward drag at a steady pace, starting on an empty cell of the bottom row. */
        fun slow(distanceDp: Int, durationMs: Int, driftDp: Int = 0, secondFinger: Boolean = false) {
            val steps = 16
            stepMs = durationMs / steps.toLong()
            val startX = workspace.width / 2f - driftDp * density / 2
            val startY = workspace.height * 7 / 10f
            val stepX = driftDp * density / steps
            val stepY = -distanceDp * density / steps
            if (realTime) assertTrue("Every step must pass the touch slop", -stepY > touchSlop)
            press(startX, startY)
            for (step in 1..steps) {
                if (realTime) SystemClock.sleep(stepMs)
                val x = startX + stepX * step
                val y = startY + stepY * step
                if (secondFinger && step == 2) {
                    send(SECOND_DOWN, x, y, second = true)
                    send(SECOND_UP, x, y, second = true)
                }
                move(x, y)
            }
            send(MotionEvent.ACTION_UP, startX + stepX * steps, startY + stepY * steps)
        }

        fun hold() {
            assertTrue("A stationary hold needs real time", realTime)
            val x = workspace.width / 2f
            val y = workspace.height * 7 / 10f
            press(x, y)
            SystemClock.sleep(900)
            send(MotionEvent.ACTION_UP, x, y)
        }

        fun press(x: Float, y: Float) = send(MotionEvent.ACTION_DOWN, x, y)

        fun move(x: Float, y: Float) = send(MotionEvent.ACTION_MOVE, x, y)

        fun cancel(x: Float, y: Float) = send(MotionEvent.ACTION_CANCEL, x, y)

        private fun send(action: Int, x: Float, y: Float, second: Boolean = false) {
            val count = if (second) 2 else 1
            val pointers = Array(count) { index ->
                MotionEvent.PointerProperties().apply { id = index; toolType = MotionEvent.TOOL_TYPE_FINGER }
            }
            val coordinates = Array(count) { index ->
                MotionEvent.PointerCoords().apply {
                    this.x = x + if (index == 0) 0f else density
                    this.y = y
                    pressure = 1f
                    size = 1f
                }
            }
            val dispatch = {
                val time = if (realTime) SystemClock.uptimeMillis() else eventTime
                if (action == MotionEvent.ACTION_DOWN) downTime = time
                val event = MotionEvent.obtain(downTime, time, action, count, pointers, coordinates,
                    0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
                try {
                    workspace.dispatchTouchEvent(event)
                } finally {
                    event.recycle()
                }
                if (!realTime) eventTime += stepMs
            }
            if (realTime) instrumentation.runOnMainSync { dispatch() } else dispatch()
        }
    }

    private companion object {
        const val SECOND_DOWN = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        const val SECOND_UP = MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
    }
}
