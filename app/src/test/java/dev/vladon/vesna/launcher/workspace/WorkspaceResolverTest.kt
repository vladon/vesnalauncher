package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceResolverTest {

    private fun app(key: AppKey) = LauncherApp(key = key, label = key.packageName)

    private val keyA = AppKey("com.a", "com.a.Main")
    private val keyB = AppKey("com.b", "com.b.Main")
    private val keyC = AppKey("com.c", "com.c.Main")

    @Test
    fun `workspace joins persisted cells with live apps`() {
        val items = listOf(
            WorkspaceItem(1, keyA, Container.WORKSPACE, cellIndex = 0),
            WorkspaceItem(2, keyB, Container.WORKSPACE, cellIndex = 5),
        )
        val apps = listOf(app(keyA), app(keyB)).associateBy { it.key }
        assertEquals(
            mapOf(0 to CellContent.App(apps.getValue(keyA)), 5 to CellContent.App(apps.getValue(keyB))),
            WorkspaceResolver.workspaceCells(items, apps, emptyList()),
        )
    }

    @Test
    fun `items without a live app are not rendered`() {
        val items = listOf(
            WorkspaceItem(1, keyA, Container.WORKSPACE, cellIndex = 0),
            WorkspaceItem(2, keyC, Container.WORKSPACE, cellIndex = 3),
        )
        val resolved = WorkspaceResolver.workspaceCells(items, mapOf(keyA to app(keyA)), emptyList())
        assertEquals(mapOf(0 to CellContent.App(app(keyA))), resolved)
    }

    @Test
    fun `dock items are ordered and stale entries skipped`() {
        val items = listOf(
            WorkspaceItem(1, keyB, Container.DOCK, cellIndex = 1),
            WorkspaceItem(2, keyC, Container.DOCK, cellIndex = 2),
            WorkspaceItem(3, keyA, Container.DOCK, cellIndex = 0),
            WorkspaceItem(4, keyA, Container.DOCK, cellIndex = 3),
        )
        val resolved = WorkspaceResolver.dockPins(items, mapOf(keyA to app(keyA), keyB to app(keyB)))
        assertEquals(listOf(DockPin(3, app(keyA)), DockPin(1, app(keyB)), DockPin(4, app(keyA))), resolved)
    }

    @Test
    fun `duplicate cells keep the first persisted item`() {
        val items = listOf(
            WorkspaceItem(1, keyA, Container.WORKSPACE, cellIndex = 2),
            WorkspaceItem(2, keyB, Container.WORKSPACE, cellIndex = 2),
        )
        val resolved = WorkspaceResolver.workspaceCells(items, mapOf(keyA to app(keyA), keyB to app(keyB)), emptyList())
        assertEquals(CellContent.App(app(keyA)), resolved.getValue(2))
    }

    @Test
    fun `invalid cells are never rendered`() {
        val items = listOf(WorkspaceItem(1, keyA, Container.WORKSPACE, cellIndex = GridCanvas.CAPACITY))
        assertTrue(WorkspaceResolver.workspaceCells(items, mapOf(keyA to app(keyA)), emptyList()).isEmpty())
    }
}
