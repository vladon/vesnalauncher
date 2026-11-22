package dev.vladon.vesna.launcher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.CellContent
import dev.vladon.vesna.launcher.workspace.GridCanvas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceDropTargetTest {
    @Test
    fun tilesAndHitTestingFollowTheStartEdgeInBothDirections() = withWorkspace { grid, rtl ->
        val cells = listOf(0, GridCanvas.COLUMNS - 1, GridCanvas.COLUMNS)
        grid.bind(cells.associateWith { CellContent.App(LauncherApp(AppKey("qa.cell$it", "Main"), "Tile $it")) })
        layOut(grid)
        val start = requireNotNull(grid.findChildForCell(cells[0]))
        val end = requireNotNull(grid.findChildForCell(cells[1]))
        val nextRow = requireNotNull(grid.findChildForCell(cells[2]))
        assertTrue("Cell zero should be on the right in RTL and on the left in LTR", if (rtl) start.left > end.left else start.left < end.left)
        assertEquals(start.left, nextRow.left)
        assertTrue(nextRow.top > start.top)
        for (cell in cells) {
            val tile = requireNotNull(grid.findChildForCell(cell))
            val y = (tile.top + tile.bottom) / 2f
            assertEquals(cell, grid.cellAtLocal(tile.left + 0.25f, y))
            assertEquals(cell, grid.cellAtLocal(tile.right - 0.25f, y))
        }
        assertNull(grid.cellAtLocal(0f, (start.top + start.bottom) / 2f))
        assertNull(grid.cellAtLocal(grid.width - 1f, (start.top + start.bottom) / 2f))
    }

    @Test
    fun emptyDropHighlightUsesTheSameStartEdgeAsTheGrid() = withWorkspace { grid, rtl ->
        layOut(grid)
        val left = grid.width / 8
        val right = grid.width * 7 / 8
        val x = if (rtl) right else left
        val otherX = if (rtl) left else right
        val y = grid.height / 10
        val before = grid.pixels(x, otherX, y)
        grid.showDropHighlight(0)
        val after = grid.pixels(x, otherX, y)
        assertNotEquals("The first logical cell should be highlighted at the start edge", before.first, after.first)
        assertEquals("The opposite cell must not be highlighted", before.second, after.second)
        assertEquals(0, grid.cellAtLocal(x.toFloat(), y.toFloat()))
    }

    private fun withWorkspace(check: (WorkspaceView, Boolean) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var result: Result<Unit>? = null
        instrumentation.runOnMainSync {
            result = runCatching {
                for (rtl in listOf(true, false)) {
                    val view = WorkspaceView(instrumentation.targetContext).apply {
                        layoutDirection = if (rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
                        setPadding(7, 11, 19, 23)
                    }
                    check(view, rtl)
                }
            }
        }
        result!!.getOrThrow()
    }

    private fun layOut(grid: WorkspaceView) {
        val density = grid.resources.displayMetrics.density
        val width = (360 * density).toInt() + 3
        val height = (520 * density).toInt() + 2
        grid.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        grid.layout(0, 0, width, height)
    }

    private fun WorkspaceView.pixels(x: Int, otherX: Int, y: Int): Pair<Int, Int> {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return try {
            draw(Canvas(bitmap))
            bitmap.getPixel(x, y) to bitmap.getPixel(otherX, y)
        } finally {
            bitmap.recycle()
        }
    }
}
