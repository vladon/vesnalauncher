package dev.vladon.vesna.launcher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.VesnaApplication
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.DockPin
import dev.vladon.vesna.launcher.workspace.GridCanvas
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DockDropTargetTest {
    @Test
    fun bothEdgesOfEachDrawnTileResolveToItsSavedSlot() = withDock { dock, _ ->
        for (slot in 0 until dock.childCount) {
            val tile = dock.getChildAt(slot)
            assertEquals("Left edge of $slot in direction ${dock.layoutDirection}", slot, dock.slotAtLocalX(tile.left + 0.25f))
            assertEquals("Right edge of $slot in direction ${dock.layoutDirection}", slot, dock.slotAtLocalX(tile.right - 0.25f))
        }
    }

    @Test
    fun highlightedEmptyDropSlotMatchesItsPositionInTheFullDock() = withDock { dock, pins ->
        val target = dock.getChildAt(pins.lastIndex)
        val targetX = target.left + target.width / 6
        val occupied = dock.getChildAt(0)
        val occupiedX = occupied.left + occupied.width / 6
        val y = dock.height / 2
        dock.bind(pins.take(2))
        layOut(dock)
        val before = dock.pixels(targetX, occupiedX, y)

        assertEquals(pins.lastIndex, dock.slotAtLocalX(targetX.toFloat()))
        dock.showSlotHighlight(pins.lastIndex)
        val after = dock.pixels(targetX, occupiedX, y)
        assertNotEquals("The empty target should be highlighted", before.first, after.first)
        assertEquals("The occupied slot is not the drop target", before.second, after.second)
    }

    @Test
    fun smallMovementDuringAHoldStillReleasesToTheShortcutMenu() = withDock { dock, _ ->
        var release: Pair<Boolean, Boolean>? = null
        var preview: Float? = null
        dock.onDragProgress = { x, _ -> preview = x }
        dock.onDragReleased = { _, _, moved, cancelled, _, _ -> release = moved to cancelled }

        val tile = dock.getChildAt(0)
        val x = (tile.left + tile.right) / 2f
        val y = (tile.top + tile.bottom) / 2f
        val slop = ViewConfiguration.get(dock.context).scaledTouchSlop
        fun dispatch(action: Int, toX: Float, down: Long) {
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, toX, y, 0)
            try {
                assertTrue(dock.dispatchTouchEvent(event))
            } finally {
                event.recycle()
            }
        }
        fun hold(): Long {
            val down = SystemClock.uptimeMillis()
            dispatch(MotionEvent.ACTION_DOWN, x, down)
            assertTrue(tile.performLongClick(tile.width / 2f, tile.height / 2f))
            return down
        }

        val stationary = hold()
        val near = x + slop / 2f
        dispatch(MotionEvent.ACTION_MOVE, near, stationary)
        dispatch(MotionEvent.ACTION_UP, near, stationary)
        assertEquals("A small movement must leave the shortcut menu available", false to false, release)
        assertNull("A small movement must not preview a drop", preview)

        val moving = hold()
        val target = dock.getChildAt(1)
        val targetX = (target.left + target.right) / 2f
        dispatch(MotionEvent.ACTION_MOVE, targetX, moving)
        assertEquals("A real drag must still preview the neighboring slot", targetX, preview)
        dispatch(MotionEvent.ACTION_MOVE, x, moving)
        dispatch(MotionEvent.ACTION_UP, x, moving)
        assertEquals("Returning to the origin must not turn a drag into a menu", true to false, release)
    }

    @Test
    fun movingIntoTheSystemNavigationInsetClearsTheDockDropPreview() = runBlocking<Unit> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val controller = VesnaApplication.graph(instrumentation.targetContext).controller
        withTimeout(10_000) { controller.state.first { it.dock.pins.isNotEmpty() } }
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val root = activity.findViewById<View>(R.id.launcher_root)
                val dock = activity.findViewById<DockView>(R.id.dock)
                val source = controller.state.value.dock.pins.first()
                val previous = requireNotNull(ViewCompat.getRootWindowInsets(root))
                val navigation = WindowInsetsCompat.Type.navigationBars()
                val reserved = (32 * dock.resources.displayMetrics.density).toInt()
                val withNavigation = WindowInsetsCompat.Builder(previous)
                    .setInsets(navigation, Insets.of(0, 0, 0, reserved))
                    .setVisible(navigation, true)
                    .build()
                ViewCompat.dispatchApplyWindowInsets(root, withNavigation)
                try {
                    dock.measure(
                        View.MeasureSpec.makeMeasureSpec(dock.width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(dock.height, View.MeasureSpec.EXACTLY),
                    )
                    dock.layout(dock.left, dock.top, dock.right, dock.bottom)
                    val tile = dock.getChildAt(0)
                    val x = tile.left + tile.width / 8
                    val y = (tile.top + tile.bottom) / 2
                    val before = dock.pixels(x, x, y).first
                    requireNotNull(dock.onDragStarted)(source, tile)
                    try {
                        requireNotNull(dock.onDragProgress)(x.toFloat(), y.toFloat())
                        assertNotEquals("The dock body must show the drop preview", before, dock.pixels(x, x, y).first)
                        requireNotNull(dock.onDragProgress)(x.toFloat(), (dock.height - reserved / 2).toFloat())
                        assertEquals("The navigation inset must clear the preview", before, dock.pixels(x, x, y).first)
                    } finally {
                        requireNotNull(dock.onDragReleased)(x.toFloat(), y.toFloat(), true, true, source.app, tile)
                    }
                } finally {
                    ViewCompat.dispatchApplyWindowInsets(root, previous)
                }
            }
        }
    }

    private fun withDock(check: (DockView, List<DockPin>) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val pins = List(GridCanvas.DOCK_CAPACITY) {
            DockPin(it + 1L, LauncherApp(AppKey("qa.dock$it", "Main"), "Pin $it"))
        }
        var result: Result<Unit>? = null
        instrumentation.runOnMainSync {
            result = runCatching {
                for (direction in intArrayOf(View.LAYOUT_DIRECTION_RTL, View.LAYOUT_DIRECTION_LTR)) {
                    val dock = DockView(instrumentation.targetContext).apply {
                        layoutDirection = direction
                        setPadding(7, 0, 19, 0)
                        bind(pins)
                    }
                    layOut(dock)
                    check(dock, pins)
                }
            }
        }
        result!!.getOrThrow()
    }

    private fun layOut(dock: DockView) {
        val density = dock.resources.displayMetrics.density
        // Exercise unequal integer slot widths and asymmetric padding.
        val width = (100 * density).toInt() * GridCanvas.DOCK_CAPACITY + 2 + dock.paddingLeft + dock.paddingRight
        val height = (96 * density).toInt()
        dock.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        dock.layout(0, 0, width, height)
    }

    private fun DockView.pixels(targetX: Int, occupiedX: Int, y: Int): Pair<Int, Int> {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return try {
            draw(Canvas(bitmap))
            bitmap.getPixel(targetX, y) to bitmap.getPixel(occupiedX, y)
        } finally {
            bitmap.recycle()
        }
    }
}
