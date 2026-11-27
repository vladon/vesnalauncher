package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.data.WidgetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceResolverWidgetTest {

    private fun app(key: AppKey) = LauncherApp(key = key, label = key.packageName)

    private val keyA = AppKey("com.a", "com.a.Main")

    private fun widget(cell: Int, pageId: Long = 1L, spanX: Int = 2, spanY: Int = 2) = WidgetEntity(
        appWidgetId = cell + 100,
        providerPackage = "p",
        providerClass = "c",
        pageId = pageId,
        cellIndex = cell,
        spanX = spanX,
        spanY = spanY,
    )

    @Test
    fun `widget occupies anchor cell and reserves covered cells`() {
        val grid = GridCanvas(columns = 4, rows = 5)
        val layout = WorkspaceResolver.workspaceLayout(
            emptyList(), emptyMap(), emptyList(), pageId = 1L, grid = grid,
            widgets = listOf(widget(cell = 5, pageId = 1L)),
        )
        assertEquals(1, layout.cells.size)
        assertTrue(layout.cells[5] is CellContent.WidgetCell)
        // The 4x5 grid: a 2x2 anchored at 5 (row 1, col 1) covers 5, 6, 9, 10.
        for (covered in intArrayOf(5, 6, 9, 10)) {
            assertTrue("Cell $covered must be reserved", layout.reservedCellsMask and (1 shl covered) != 0)
        }
    }

    @Test
    fun `widget on other page is ignored`() {
        val grid = GridCanvas(columns = 4, rows = 5)
        val layout = WorkspaceResolver.workspaceLayout(
            emptyList(), emptyMap(), emptyList(), pageId = 2L, grid = grid,
            widgets = listOf(widget(cell = 5, pageId = 1L)),
        )
        assertEquals(0, layout.cells.size)
        assertEquals(0, layout.reservedCellsMask)
    }

    @Test
    fun `widget does not displace existing items`() {
        val items = listOf(
            WorkspaceItem(1, keyA, Container.WORKSPACE, cellIndex = 5, pageId = 1L),
        )
        val apps = mapOf(keyA to app(keyA))
        val grid = GridCanvas(columns = 4, rows = 5)
        val layout = WorkspaceResolver.workspaceLayout(
            items, apps, emptyList(), pageId = 1L, grid = grid,
            widgets = listOf(widget(cell = 5, pageId = 1L)),
        )
        // The app keeps its cell; the covered neighbours are still reserved.
        assertTrue(layout.cells[5] is CellContent.App)
        assertTrue(layout.reservedCellsMask and (1 shl 9) != 0)
    }
}
