package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import org.junit.Assert.assertEquals
import org.junit.Test

class SeedPlannerTest {

    private fun app(index: Int) = LauncherApp(
        key = AppKey("pkg$index", "pkg$index.MainActivity"),
        label = "App $index",
    )

    @Test
    fun `dock gets the first apps in order`() {
        val items = SeedPlanner.plan((0 until 30).map(::app))
        val dock = items.filter { it.container == Container.DOCK }
        assertEquals(GridCanvas.DOCK_CAPACITY, dock.size)
        assertEquals((0 until GridCanvas.DOCK_CAPACITY).toList(), dock.map { it.cellIndex })
        assertEquals("pkg0", dock.first().key.packageName)
    }

    @Test
    fun `workspace gets the next page worth of apps`() {
        val items = SeedPlanner.plan((0 until 30).map(::app))
        val workspace = items.filter { it.container == Container.WORKSPACE }
        assertEquals(GridCanvas.CAPACITY, workspace.size)
        assertEquals((0 until GridCanvas.CAPACITY).toList(), workspace.map { it.cellIndex })
        // Workspace starts after the dock apps.
        assertEquals(GridCanvas.DOCK_CAPACITY, workspace.first().key.packageName.removePrefix("pkg").toInt())
    }

    @Test
    fun `fewer apps than the dock only fill the dock`() {
        val items = SeedPlanner.plan((0 until 3).map(::app))
        assertEquals(3, items.size)
        assertEquals(items, items.filter { it.container == Container.DOCK })
    }

    @Test
    fun `dock plus one page exactly fits the grid`() {
        val total = GridCanvas.DOCK_CAPACITY + GridCanvas.CAPACITY
        val items = SeedPlanner.plan((0 until total).map(::app))
        assertEquals(total, items.size)
    }
}
