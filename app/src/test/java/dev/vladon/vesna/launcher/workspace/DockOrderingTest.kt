package dev.vladon.vesna.launcher.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class DockOrderingTest {

    private val ids = listOf(100L, 200L, 300L, 400L)

    @Test
    fun `moveTo relocates entry forward`() {
        assertEquals(listOf(200L, 300L, 100L, 400L), DockOrdering.moveTo(ids, 0, 2))
    }

    @Test
    fun `moveTo relocates entry backward`() {
        assertEquals(listOf(100L, 400L, 200L, 300L), DockOrdering.moveTo(ids, 3, 1))
    }

    @Test
    fun `moveTo clamps out of range targets`() {
        // -5 clamps to slot 0, which is the source slot: order unchanged.
        assertEquals(ids, DockOrdering.moveTo(ids, 0, -5))
        assertEquals(listOf(200L, 300L, 400L, 100L), DockOrdering.moveTo(ids, 0, 99))
    }

    @Test
    fun `moveTo with same slot returns same order`() {
        assertEquals(ids, DockOrdering.moveTo(ids, 1, 1))
    }

    @Test
    fun `moveTo ignores invalid source`() {
        assertEquals(ids, DockOrdering.moveTo(ids, 9, 0))
    }

    @Test
    fun `insertAt appends into empty dock`() {
        assertEquals(listOf(7L), DockOrdering.insertAt(emptyList(), 7L, 3))
    }

    @Test
    fun `insertAt places entry at slot shifting others`() {
        assertEquals(listOf(100L, 7L, 200L, 300L, 400L), DockOrdering.insertAt(ids, 7L, 1))
    }

    @Test
    fun `insertAt clamps to append when slot beyond size`() {
        assertEquals(listOf(100L, 200L, 300L, 400L, 7L), DockOrdering.insertAt(ids, 7L, 10))
    }

    @Test
    fun `insertAt ignores duplicate id`() {
        assertEquals(ids, DockOrdering.insertAt(ids, 200L, 0))
    }
}
