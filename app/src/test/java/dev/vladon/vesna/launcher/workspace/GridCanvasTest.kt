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
}
