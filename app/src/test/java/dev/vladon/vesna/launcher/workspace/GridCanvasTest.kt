package dev.vladon.vesna.launcher.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridCanvasTest {
    private val grid = GridCanvas.DEFAULT

    @Test
    fun `empty grid places at cell zero`() {
        assertEquals(0, grid.firstFreeCell(emptySet()))
    }

    @Test
    fun `first gap after occupied prefix is found`() {
        assertEquals(2, grid.firstFreeCell(setOf(0, 1, 3)))
    }

    @Test
    fun `full grid has no free cell`() {
        val full = (0 until grid.capacity).toSet()
        assertNull(grid.firstFreeCell(full))
    }

    @Test
    fun `out of range cells are ignored`() {
        assertEquals(0, grid.firstFreeCell(setOf(-1, grid.capacity, 99)))
        assertNull(grid.firstFreeCell((0..99).toSet()))
    }

    @Test
    fun `row and column conversions match linear indexing`() {
        assertEquals(0, grid.rowOf(0))
        assertEquals(0, grid.columnOf(0))
        assertEquals(1, grid.rowOf(grid.columns + 1))
        assertEquals(1, grid.columnOf(grid.columns + 1))
        assertEquals(grid.rows - 1, grid.rowOf(grid.capacity - 1))
        assertEquals(grid.columns - 1, grid.columnOf(grid.capacity - 1))
    }

    @Test
    fun `validity range covers the grid only`() {
        assertTrue(grid.isValidCell(0))
        assertTrue(grid.isValidCell(grid.capacity - 1))
        assertFalse(grid.isValidCell(-1))
        assertFalse(grid.isValidCell(grid.capacity))
    }

    @Test
    fun `cellAtPoint maps coordinates onto cells`() {
        val cellW = 100f
        val cellH = 120f
        // Cell (row 1, col 2) starts at (200, 120).
        assertEquals(6, grid.cellAtPoint(205f, 125f, 0f, 0f, cellW, cellH))
        assertEquals(6, grid.cellAtPoint(250f, 200f, 0f, 0f, cellW, cellH))
    }

    @Test
    fun `cellAtPoint respects grid origin offset`() {
        assertEquals(
            0,
            grid.cellAtPoint(505f, 610f, 500f, 600f, 100f, 120f),
        )
    }

    @Test
    fun `cellAtPoint returns null outside the grid`() {
        assertNull(grid.cellAtPoint(-1f, 10f, 0f, 0f, 100f, 100f))
        assertNull(grid.cellAtPoint(100 * grid.columns + 1f, 10f, 0f, 0f, 100f, 100f))
        assertNull(grid.cellAtPoint(10f, 100 * grid.rows + 1f, 0f, 0f, 100f, 100f))
    }

    @Test
    fun `cellAtPoint returns null for degenerate cell sizes`() {
        assertNull(grid.cellAtPoint(10f, 10f, 0f, 0f, 0f, 100f))
        assertNull(grid.cellAtPoint(10f, 10f, 0f, 0f, 100f, -1f))
    }
}
