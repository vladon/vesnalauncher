package dev.vladon.vesna.launcher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.DockPin
import dev.vladon.vesna.launcher.workspace.GridCanvas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
