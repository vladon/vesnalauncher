package dev.vladon.vesna.launcher.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Paging arithmetic behind the dock capacity setting (#115 stage 2). */
class DockPagesTest {

    @Test
    fun totalCapacityDoublesThePerPageSlots() {
        assertEquals(8, DockPages.totalCapacity(4))
        assertEquals(4, DockPages.totalCapacity(2))
        assertEquals(16, DockPages.totalCapacity(8))
    }

    @Test
    fun canHoldTracksThePageLimit() {
        assertTrue(DockPages.canHold(8, 4))
        assertFalse(DockPages.canHold(9, 4))
        assertTrue(DockPages.canHold(4, 2))
        assertFalse(DockPages.canHold(5, 2))
        assertTrue(DockPages.canHold(0, 2))
    }

    @Test
    fun coerceClampsIntoTheSupportedRange() {
        assertEquals(2, DockPages.coerce(0))
        assertEquals(2, DockPages.coerce(1))
        assertEquals(4, DockPages.coerce(4))
        assertEquals(8, DockPages.coerce(9))
        assertEquals(DockPages.MIN_CAPACITY, DockPages.coerce(Int.MIN_VALUE))
        assertEquals(DockPages.DEFAULT_CAPACITY, DockPages.coerce(DockPages.DEFAULT_CAPACITY))
    }

    @Test
    fun pageCountNeverExceedsTwoPages() {
        assertEquals(1, DockPages.pageCount(0, 4))
        assertEquals(1, DockPages.pageCount(4, 4))
        assertEquals(2, DockPages.pageCount(5, 4))
        assertEquals(2, DockPages.pageCount(9, 4))
        assertEquals(1, DockPages.pageCount(1, 8))
    }

    @Test
    fun pageSlicesConsecutivePins() {
        val pins = (1L..5L).toList()
        assertEquals(listOf(1L, 2L, 3L, 4L), DockPages.page(pins, 0, 4))
        assertEquals(listOf(5L), DockPages.page(pins, 1, 4))
        assertEquals(emptyList<Long>(), DockPages.page(pins, 2, 4))
        assertEquals(emptyList<Long>(), DockPages.page(emptyList<Long>(), 0, 4))
    }

    @Test
    fun coercePageStaysInsideTheFilledPages() {
        assertEquals(0, DockPages.coercePage(0, 4, 4))
        assertEquals(0, DockPages.coercePage(1, 4, 4))
        assertEquals(1, DockPages.coercePage(1, 5, 4))
        assertEquals(1, DockPages.coercePage(5, 5, 4))
        assertEquals(0, DockPages.coercePage(-1, 0, 4))
    }

    @Test
    fun globalSlotMapsAPageColumnOntoTheSavedOrder() {
        assertEquals(0, DockPages.globalSlot(0, 0, 4))
        assertEquals(3, DockPages.globalSlot(0, 3, 4))
        assertEquals(4, DockPages.globalSlot(1, 0, 4))
        assertEquals(7, DockPages.globalSlot(1, 3, 4))
    }
}
