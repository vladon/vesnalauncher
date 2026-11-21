package dev.vladon.vesna.launcher.ui

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspacePageSwipeTest {
    @Test
    fun replacingTheFirstFingerDoesNotStartAPageSwipe() = withWorkspace { swipe, pages ->
        swipe.send(MotionEvent.ACTION_DOWN, swipe.right)
        swipe.send(SECOND_DOWN, swipe.right, swipe.left)
        swipe.send(MotionEvent.ACTION_POINTER_UP, swipe.right, swipe.left)
        swipe.send(MotionEvent.ACTION_MOVE, swipe.left + 3, firstId = 1)
        swipe.send(MotionEvent.ACTION_UP, swipe.left + 3, firstId = 1)
        assertTrue("Changing fingers must not start a page swipe", pages.isEmpty())
    }

    @Test
    fun addingASecondFingerDoesNotFinishAnArmedPageSwipe() = withWorkspace { swipe, pages ->
        swipe.send(MotionEvent.ACTION_DOWN, swipe.right)
        swipe.send(MotionEvent.ACTION_MOVE, swipe.left)
        swipe.send(SECOND_DOWN, swipe.left, swipe.right)
        swipe.send(SECOND_UP, swipe.left, swipe.right)
        swipe.send(MotionEvent.ACTION_UP, swipe.left)
        assertTrue("A swipe interrupted by a second finger must not change the page", pages.isEmpty())
    }

    @Test
    fun singleFingerSwipeStillPagesInBothLayoutDirections() = withWorkspace { swipe, pages ->
        swipe.send(MotionEvent.ACTION_DOWN, swipe.right)
        swipe.send(MotionEvent.ACTION_MOVE, swipe.left)
        swipe.send(MotionEvent.ACTION_UP, swipe.left)
        swipe.workspace.layoutDirection = View.LAYOUT_DIRECTION_RTL
        swipe.send(MotionEvent.ACTION_DOWN, swipe.right)
        swipe.send(MotionEvent.ACTION_MOVE, swipe.left)
        swipe.send(MotionEvent.ACTION_UP, swipe.left)
        assertEquals(listOf(true, false), pages)
    }

    private fun withWorkspace(check: (TouchStream, MutableList<Boolean>) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var result: Result<Unit>? = null
        instrumentation.runOnMainSync {
            result = runCatching {
                val workspace = WorkspaceView(instrumentation.targetContext)
                val width = (320 * workspace.resources.displayMetrics.density).toInt()
                val height = width * 2
                workspace.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                workspace.layout(0, 0, width, height)
                val pages = mutableListOf<Boolean>()
                workspace.onSwipePage = { pages += it }
                check(TouchStream(workspace), pages)
            }
        }
        result!!.getOrThrow()
    }

    private class TouchStream(val workspace: WorkspaceView) {
        val left = workspace.width / 4f
        val right = workspace.width * 3 / 4f
        private var downTime = 0L
        private var eventTime = 0L

        fun send(action: Int, x: Float, secondX: Float? = null, firstId: Int = 0) {
            if (action == MotionEvent.ACTION_DOWN) {
                downTime = SystemClock.uptimeMillis()
                eventTime = downTime
            } else {
                eventTime += 20
            }
            val count = if (secondX == null) 1 else 2
            val pointers = Array(count) { i ->
                MotionEvent.PointerProperties().apply { id = if (i == 0) firstId else 1; toolType = MotionEvent.TOOL_TYPE_FINGER }
            }
            val coords = Array(count) { i ->
                MotionEvent.PointerCoords().apply {
                    this.x = if (i == 0) x else checkNotNull(secondX)
                    y = workspace.height / 2f
                    pressure = 1f
                    size = 1f
                }
            }
            val event = MotionEvent.obtain(downTime, eventTime, action, count, pointers, coords,
                0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
            try {
                workspace.dispatchTouchEvent(event)
            } finally {
                event.recycle()
            }
        }
    }

    private companion object {
        const val SECOND_DOWN = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        const val SECOND_UP = MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
    }
}
