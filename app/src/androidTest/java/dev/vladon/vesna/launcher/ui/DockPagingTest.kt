package dev.vladon.vesna.launcher.ui

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.DockPages
import dev.vladon.vesna.launcher.workspace.DockPin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Dock paging (#115 stage 2): [DockPages.MAX_PAGES] pages of [DockView.capacity]
 * slots. Overflow flows onto the second page in saved order, a horizontal sweep
 * flips the page while a dock drag keeps the stream, and page-local columns map
 * back onto saved-order indexes for Home's drop and focus logic.
 */
@RunWith(AndroidJUnit4::class)
class DockPagingTest {

    @Test
    fun overflowPinsWaitOnTheSecondPage() {
        val pins = (1L..3L).map { DockPin(it, LauncherApp(AppKey("qa.page$it", "Main"), "Pin $it")) }
        for (direction in intArrayOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
            withDock(pins, capacity = 2, direction = direction) { dock ->
                assertEquals("A two-slot dock shows the first two pins", 2, dock.childCount)
                assertEquals(0, dock.pinIndexAt(0))
                assertEquals(1, dock.pinIndexAt(1))
                dock.showPage(1)
                assertEquals("The overflow pin fills the second page", 1, dock.childCount)
                assertEquals(2, dock.pinIndexAt(0))
                assertNotNull(dock.childAtPin(2))
                assertNull("Page two hides the first-page pins", dock.childAtPin(0))
                dock.showPage(5)
                assertEquals("A clamped request stays on the last page", 1, dock.currentPage)
                dock.showPage(-3)
                assertEquals(0, dock.currentPage)
            }
        }
    }

    @Test
    fun capacityChangeReflowsThePages() {
        val pins = (1L..5L).map { DockPin(it, LauncherApp(AppKey("qa.reflow$it", "Main"), "Pin $it")) }
        withDock(pins, capacity = 4, direction = View.LAYOUT_DIRECTION_LTR) { dock ->
            assertEquals(4, dock.childCount)
            assertEquals(2, dock.pageCount)
            dock.capacity = 8
            assertEquals("Five pins fit on one eight-slot page", 5, dock.childCount)
            assertEquals(1, dock.pageCount)
            assertEquals(0, dock.currentPage)
            dock.capacity = 2
            assertEquals("The pinned page stays in range after a reduction", 2, dock.childCount)
            assertEquals(0, dock.currentPage)
            dock.showPage(1)
            assertEquals(2, dock.childCount)
        }
    }

    @Test
    fun horizontalSweepFlipsThePage() {
        val pins = (1L..3L).map { DockPin(it, LauncherApp(AppKey("qa.sweep$it", "Main"), "Pin $it")) }
        withDock(pins, capacity = 2, direction = View.LAYOUT_DIRECTION_LTR) { dock ->
            swipe(dock, fromX = dock.width / 2f, toX = 0f, y = dock.height / 2f)
            assertEquals("A leftward sweep opens the second page", 1, dock.currentPage)
            assertEquals(1, dock.childCount)
            swipe(dock, fromX = 4f, toX = dock.width - 4f, y = dock.height / 2f)
            assertEquals("A rightward sweep returns to the first page", 0, dock.currentPage)
            assertEquals(2, dock.childCount)
        }
    }

    @Test
    fun verticalSweepDoesNotPage() {
        val pins = (1L..3L).map { DockPin(it, LauncherApp(AppKey("qa.vertical$it", "Main"), "Pin $it")) }
        withDock(pins, capacity = 2, direction = View.LAYOUT_DIRECTION_LTR) { dock ->
            val x = dock.width / 2f
            val y = dock.height / 2f
            dispatch(dock, MotionEvent.ACTION_DOWN, x, y)
            dispatch(dock, MotionEvent.ACTION_MOVE, x, y - 60f)
            dispatch(dock, MotionEvent.ACTION_MOVE, x, y - 120f)
            dispatch(dock, MotionEvent.ACTION_UP, x, y - 120f)
            assertEquals("A vertical sweep must leave the page alone", 0, dock.currentPage)
            assertEquals(2, dock.childCount)
        }
    }

    @Test
    fun dockDragKeepsTheStreamFromPaging() {
        val pins = (1L..3L).map { DockPin(it, LauncherApp(AppKey("qa.drag$it", "Main"), "Pin $it")) }
        withDock(pins, capacity = 2, direction = View.LAYOUT_DIRECTION_LTR) { dock ->
            var progress: Float? = null
            dock.onDragProgress = { x, _ -> progress = x }
            val tile = dock.getChildAt(0)
            val x = (tile.left + tile.right) / 2f
            val y = (tile.top + tile.bottom) / 2f
            val down = SystemClock.uptimeMillis()
            dispatch(dock, MotionEvent.ACTION_DOWN, x, y, down)
            assertTrue(tile.performLongClick(tile.width / 2f, tile.height / 2f))
            dispatch(dock, MotionEvent.ACTION_MOVE, 0f, y)
            assertNotNull("The drag session must preview across the dock", progress)
            dispatch(dock, MotionEvent.ACTION_UP, 0f, y)
            assertEquals("An active drag must not flip the page", 0, dock.currentPage)
        }
    }

    private fun withDock(
        pins: List<DockPin>,
        capacity: Int,
        direction: Int,
        check: (DockView) -> Unit,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var result: Result<Unit>? = null
        instrumentation.runOnMainSync {
            result = runCatching {
                val dock = DockView(instrumentation.targetContext).apply {
                    layoutDirection = direction
                    setPadding(7, 0, 19, 0)
                    this.capacity = capacity
                    bind(pins)
                }
                layOut(dock)
                check(dock)
            }
        }
        result!!.getOrThrow()
    }

    private fun layOut(dock: DockView) {
        val density = dock.resources.displayMetrics.density
        val width = (100 * density).toInt() * dock.capacity + 2 + dock.paddingLeft + dock.paddingRight
        val height = (96 * density).toInt()
        dock.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        dock.layout(0, 0, width, height)
    }

    private fun dispatch(dock: DockView, action: Int, x: Float, y: Float, down: Long = 0L) {
        val event = MotionEvent.obtain(
            if (down == 0L) SystemClock.uptimeMillis() else down,
            SystemClock.uptimeMillis(),
            action,
            x,
            y,
            0,
        )
        try {
            dock.dispatchTouchEvent(event)
        } finally {
            event.recycle()
        }
    }

    private fun swipe(dock: DockView, fromX: Float, toX: Float, y: Float) {
        dispatch(dock, MotionEvent.ACTION_DOWN, fromX, y)
        val steps = 6
        for (step in 1..steps) {
            val x = fromX + (toX - fromX) * step / steps
            dispatch(dock, MotionEvent.ACTION_MOVE, x, y)
        }
        dispatch(dock, MotionEvent.ACTION_UP, toX, y)
    }
}
