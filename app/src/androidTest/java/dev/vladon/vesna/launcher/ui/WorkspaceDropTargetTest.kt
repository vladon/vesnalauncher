package dev.vladon.vesna.launcher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.workspace.CellContent
import dev.vladon.vesna.launcher.workspace.Folder
import dev.vladon.vesna.launcher.workspace.GridCanvas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceDropTargetTest {
    @Test
    fun tilesAndHitTestingFollowTheStartEdgeInBothDirections() = withWorkspace { grid, rtl ->
        val columns = GridCanvas.DEFAULT.columns
        val cells = listOf(0, columns - 1, columns)
        grid.bind(GridCanvas.DEFAULT, cells.associateWith { CellContent.App(LauncherApp(AppKey("qa.cell$it", "Main"), "Tile $it")) })
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

    @Test
    fun drawerDragDistinguishesProfilesAndCancelsWhenItsSourceDisappears() {
        val personal = LauncherApp(AppKey("qa.dual", "Main"), "Tile")
        val work = personal.copy(key = personal.key.copy(userSerial = 10))
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val root = activity.findViewById<LauncherDragLayer>(R.id.launcher_root)
                val drawer = activity.findViewById<DrawerView>(R.id.drawer)
                val workspace = activity.findViewById<WorkspaceView>(R.id.workspace)
                activity.updateAppCatalogue(listOf(personal, work))
                workspace.bind(
                    GridCanvas.DEFAULT,
                    mapOf(0 to CellContent.App(personal), 1 to CellContent.App(work)),
                )
                workspace.measure(
                    View.MeasureSpec.makeMeasureSpec(workspace.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(workspace.height, View.MeasureSpec.EXACTLY),
                )
                workspace.layout(workspace.left, workspace.top, workspace.right, workspace.bottom)
                val target = requireNotNull(workspace.findChildForCell(0))
                val other = requireNotNull(workspace.findChildForCell(1))
                val y = (target.top + target.bottom) / 2
                val x = target.left + target.width / 8
                val otherX = other.left + other.width / 8
                val before = workspace.pixels(x, otherX, y)
                val down = SystemClock.uptimeMillis()

                fun send(action: Int, localX: Int) {
                    val event = MotionEvent.obtain(
                        down, SystemClock.uptimeMillis(), action,
                        (workspace.left + localX).toFloat(), (workspace.top + y).toFloat(), 0,
                    )
                    try {
                        if (action == MotionEvent.ACTION_CANCEL) root.dispatchTouchEvent(event) else root.onTouchEvent(event)
                    } finally {
                        event.recycle()
                    }
                }

                requireNotNull(drawer.onItemDragStarted)(personal, drawer)
                try {
                    send(MotionEvent.ACTION_MOVE, x)
                    assertEquals("The app already pinned here must not show a drop preview", before, workspace.pixels(x, otherX, y))
                    send(MotionEvent.ACTION_MOVE, otherX)
                    val after = workspace.pixels(x, otherX, y)
                    assertEquals(before.first, after.first)
                    assertNotEquals("A copy in the other profile is a valid drop target", before.second, after.second)

                    activity.updateAppCatalogue(listOf(personal.copy(label = "Renamed"), work))
                    assertEquals("A catalogue refresh that retains the app keeps the preview", after, workspace.pixels(x, otherX, y))
                    activity.updateAppCatalogue(listOf(work))
                    assertEquals("The other profile cannot keep a disappearing app's drag alive", before, workspace.pixels(x, otherX, y))
                    activity.updateAppCatalogue(listOf(personal, work))
                    send(MotionEvent.ACTION_MOVE, otherX)
                    assertEquals("Returning to the catalogue does not revive the cancelled gesture", before, workspace.pixels(x, otherX, y))
                } finally {
                    send(MotionEvent.ACTION_CANCEL, x)
                }
            }
        }
    }

    @Test
    fun folderDragSkipsTheHiddenPinAndStillDropsInAnEmptyCell() = withWorkspace { grid, rtl ->
        val folder = Folder(1L, "Fixture folder", 0)
        grid.bind(GridCanvas.DEFAULT, mapOf(0 to CellContent.FolderCell(folder, memberCount = 2)))
        grid.reservedCellsMask = 1 shl 1
        layOut(grid)

        val tile = requireNotNull(grid.findChildForCell(0))
        val x = (tile.left + tile.right) / 2
        val y = (tile.top + tile.bottom) / 2
        val cellWidth = (grid.width - grid.paddingLeft - grid.paddingRight -
            2 * (8 * grid.resources.displayMetrics.density).toInt()) / GridCanvas.DEFAULT.columns
        val step = if (rtl) -cellWidth else cellWidth
        val reservedX = x + step
        val emptyX = x + 2 * step
        assertEquals(1, grid.cellAtLocal(reservedX.toFloat(), y.toFloat()))
        assertEquals(2, grid.cellAtLocal(emptyX.toFloat(), y.toFloat()))
        assertNull(grid.findChildForCell(1))
        val before = grid.pixels(reservedX, emptyX, y)
        val drops = mutableListOf<Int>()
        grid.onFolderDrop = { _, cell -> drops += cell }

        fun drag(to: Int, preview: () -> Unit) {
            val down = SystemClock.uptimeMillis()
            fun send(action: Int, at: Int) {
                val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, at.toFloat(), y.toFloat(), 0)
                try {
                    grid.dispatchTouchEvent(event)
                } finally {
                    event.recycle()
                }
            }
            send(MotionEvent.ACTION_DOWN, x)
            try {
                assertTrue(tile.performLongClick())
                send(MotionEvent.ACTION_MOVE, to)
                preview()
                send(MotionEvent.ACTION_UP, to)
            } finally {
                if (grid.isDragging) send(MotionEvent.ACTION_CANCEL, to)
            }
        }

        drag(reservedX) { assertEquals(before, grid.pixels(reservedX, emptyX, y)) }
        assertTrue(drops.isEmpty())
        drag(emptyX) {
            val after = grid.pixels(reservedX, emptyX, y)
            assertEquals(before.first, after.first)
            assertNotEquals(before.second, after.second)
        }
        assertEquals(listOf(2), drops)
    }

    @Test
    fun nonDefaultGeometryMirrorsTilesAndHitTargetsAcrossAResize() = withWorkspace { grid, rtl ->
        fun tile(cell: Int) = CellContent.App(LauncherApp(AppKey("qa.resize.cell$cell", "Main"), "Tile $cell"))
        val narrow = GridCanvas(columns = 3, rows = 4)
        val grown = GridCanvas(columns = 4, rows = 6)
        val cells = mapOf(
            0 to tile(0),
            narrow.columns - 1 to tile(narrow.columns - 1),
            narrow.columns to tile(narrow.columns),
            narrow.capacity - 1 to tile(narrow.capacity - 1),
        )
        grid.bind(narrow, cells)
        layOut(grid)
        assertFalse("Laying out consumes the scheduled resize", grid.isLayoutRequested)

        val first = requireNotNull(grid.findChildForCell(0))
        val rowEnd = requireNotNull(grid.findChildForCell(narrow.columns - 1))
        val nextRow = requireNotNull(grid.findChildForCell(narrow.columns))
        val last = requireNotNull(grid.findChildForCell(narrow.capacity - 1))
        assertTrue(
            "Cell zero belongs at the reading start edge",
            if (rtl) first.left > rowEnd.left else first.left < rowEnd.left,
        )
        assertEquals("A new row starts in the same column", first.left, nextRow.left)
        assertTrue(nextRow.top > first.top)
        assertTrue("The final cell sits on the final row", last.top > nextRow.top)
        for (cell in cells.keys) {
            val tileView = requireNotNull(grid.findChildForCell(cell))
            val y = (tileView.top + tileView.bottom) / 2f
            assertEquals(cell, grid.cellAtLocal(tileView.left + 0.25f, y))
            assertEquals(cell, grid.cellAtLocal(tileView.right - 0.25f, y))
        }
        assertNull(grid.cellAtLocal(0f, (first.top + first.bottom) / 2f))

        // Hit testing resolves the second row's first cell from its center.
        val previousLeft = nextRow.left
        val previousTop = nextRow.top
        val probeX = (nextRow.left + nextRow.right) / 2f
        val probeY = (nextRow.top + nextRow.bottom) / 2f
        assertEquals(narrow.columns, grid.cellAtLocal(probeX, probeY))

        // The same cells with only different geometry must still resize.
        grid.bind(grown, cells)
        assertTrue("A geometry-only change reschedules layout", grid.isLayoutRequested)
        layOut(grid)
        val moved = requireNotNull(grid.findChildForCell(narrow.columns))
        assertTrue(
            "The second row's first cell reflows into the wider grid",
            moved.top != previousTop || moved.left != previousLeft,
        )
        assertTrue(
            "Cell zero keeps the reading start edge after the resize",
            if (rtl) first.left > moved.left else first.left < moved.left,
        )
        assertEquals("Hit testing mirrors the new geometry", narrow.columns, grid.cellAtLocal((moved.left + moved.right) / 2f, (moved.top + moved.bottom) / 2f))
        assertEquals("Old coordinates resolve against the new grid", 2 * grown.columns, grid.cellAtLocal(probeX, probeY))
        assertTrue("The final cell stays on the final row", last.top > moved.top)
    }

    @Test
    fun unchangedGeometryKeepsDragsAndHighlightsWhileAResizeEndsThem() = withWorkspace { grid, rtl ->
        val cells = mapOf(0 to CellContent.App(LauncherApp(AppKey("qa.hold.cell", "Main"), "Tile")))
        grid.bind(GridCanvas.DEFAULT, cells)
        layOut(grid)
        val columns = GridCanvas.DEFAULT.columns
        val rows = GridCanvas.DEFAULT.rows
        val gutter = (8 * grid.resources.displayMetrics.density).toInt()
        val cellWidth = (grid.width - grid.paddingLeft - grid.paddingRight - 2 * gutter) / columns
        val cellHeight = (grid.height - grid.paddingTop - grid.paddingBottom - 2 * gutter) / rows
        // The far end of the bottom row is empty under both geometries below.
        val farColumn = if (rtl) 0 else columns - 1
        val farX = grid.paddingLeft + gutter + farColumn * cellWidth + cellWidth / 2
        val farY = grid.paddingTop + gutter + (rows - 1) * cellHeight + cellHeight / 2
        assertEquals(GridCanvas.DEFAULT.capacity - 1, grid.cellAtLocal(farX.toFloat(), farY.toFloat()))

        grid.showDropHighlight(GridCanvas.DEFAULT.capacity - 1)
        val highlighted = grid.pixel(farX, farY)
        grid.bind(GridCanvas.DEFAULT, cells)
        assertEquals("An unchanged grid keeps the current highlight", highlighted, grid.pixel(farX, farY))
        val resized = GridCanvas(columns = 2, rows = 2)
        grid.bind(resized, cells)
        assertTrue("A geometry-only change reschedules layout", grid.isLayoutRequested)
        layOut(grid)
        assertNotEquals("Stale highlights do not survive the resize", highlighted, grid.pixel(farX, farY))

        val released = mutableListOf<Boolean>()
        grid.onDragReleased = { _, _, _, cancelled, _, _ -> released += cancelled }
        val tile = requireNotNull(grid.findChildForCell(0))
        val x = (tile.left + tile.right) / 2
        val y = (tile.top + tile.bottom) / 2
        val down = SystemClock.uptimeMillis()

        fun send(action: Int) {
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x.toFloat(), y.toFloat(), 0)
            try {
                grid.dispatchTouchEvent(event)
            } finally {
                event.recycle()
            }
        }

        send(MotionEvent.ACTION_DOWN)
        try {
            assertTrue(tile.performLongClick())
            assertTrue(grid.isDragging)
            grid.bind(resized, cells)
            assertTrue("An unchanged grid does not end an eligible drag", grid.isDragging)
            assertTrue(released.isEmpty())
            grid.bind(GridCanvas.DEFAULT, cells)
            assertFalse("A geometry change ends the active drag", grid.isDragging)
            assertEquals(listOf(true), released)
        } finally {
            if (grid.isDragging) send(MotionEvent.ACTION_CANCEL)
        }
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

    private fun WorkspaceView.pixel(x: Int, y: Int): Int {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return try {
            draw(Canvas(bitmap))
            bitmap.getPixel(x, y)
        } finally {
            bitmap.recycle()
        }
    }
}
