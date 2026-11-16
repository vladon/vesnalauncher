package dev.vladon.vesna.launcher.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridCanvasTest {

    @Test
    fun `empty grid places at cell zero`() {
        assertEquals(0, GridCanvas.firstFreeCell(emptySet()))
    }

    @Test
    fun `first gap after occupied prefix is found`() {
        assertEquals(2, GridCanvas.firstFreeCell(setOf(0, 1, 3)))
    }

    @Test
    fun `full grid has no free cell`() {
        val full = (0 until GridCanvas.CAPACITY).toSet()
        assertNull(GridCanvas.firstFreeCell(full))
    }

    @Test
    fun `out of range cells are ignored`() {
        assertEquals(0, GridCanvas.firstFreeCell(setOf(-1, GridCanvas.CAPACITY, 99)))
        assertNull(GridCanvas.firstFreeCell((0..99).toSet()))
    }

    @Test
    fun `row and column conversions match linear indexing`() {
        assertEquals(0, GridCanvas.rowOf(0))
        assertEquals(0, GridCanvas.columnOf(0))
        assertEquals(1, GridCanvas.rowOf(GridCanvas.COLUMNS + 1))
        assertEquals(1, GridCanvas.columnOf(GridCanvas.COLUMNS + 1))
        assertEquals(GridCanvas.ROWS - 1, GridCanvas.rowOf(GridCanvas.CAPACITY - 1))
        assertEquals(GridCanvas.COLUMNS - 1, GridCanvas.columnOf(GridCanvas.CAPACITY - 1))
    }

    @Test
    fun `validity range covers the grid only`() {
        assertTrue(GridCanvas.isValidCell(0))
        assertTrue(GridCanvas.isValidCell(GridCanvas.CAPACITY - 1))
        assertFalse(GridCanvas.isValidCell(-1))
        assertFalse(GridCanvas.isValidCell(GridCanvas.CAPACITY))
    }

    @Test
    fun `dock aligns with workspace columns`() {
        assertEquals(GridCanvas.COLUMNS, GridCanvas.DOCK_CAPACITY)
    }

    @Test
    fun `cellAtPoint maps coordinates onto cells`() {
        val cellW = 100f
        val cellH = 120f
        // Cell (row 1, col 2) starts at (200, 120).
        assertEquals(6, GridCanvas.cellAtPoint(205f, 125f, 0f, 0f, cellW, cellH))
        assertEquals(6, GridCanvas.cellAtPoint(250f, 200f, 0f, 0f, cellW, cellH))
    }

    @Test
    fun `cellAtPoint respects grid origin offset`() {
        assertEquals(
            0,
            GridCanvas.cellAtPoint(505f, 610f, 500f, 600f, 100f, 120f),
        )
    }

    @Test
    fun `cellAtPoint returns null outside the grid`() {
        assertNull(GridCanvas.cellAtPoint(-1f, 10f, 0f, 0f, 100f, 100f))
        assertNull(GridCanvas.cellAtPoint(100 * GridCanvas.COLUMNS + 1f, 10f, 0f, 0f, 100f, 100f))
        assertNull(GridCanvas.cellAtPoint(10f, 100 * GridCanvas.ROWS + 1f, 0f, 0f, 100f, 100f))
    }

    @Test
    fun `cellAtPoint returns null for degenerate cell sizes`() {
        assertNull(GridCanvas.cellAtPoint(10f, 10f, 0f, 0f, 0f, 100f))
        assertNull(GridCanvas.cellAtPoint(10f, 10f, 0f, 0f, 100f, -1f))
    }
}
