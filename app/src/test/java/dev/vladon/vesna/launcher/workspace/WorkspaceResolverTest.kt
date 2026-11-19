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
        val apps = listOf(app(keyA), app(keyB))
        assertEquals(mapOf(0 to apps[0], 5 to apps[1]), WorkspaceResolver.workspaceApps(items, apps))
    }

    @Test
    fun `items of uninstalled apps are dropped`() {
        val items = listOf(
            WorkspaceItem(1, keyA, Container.WORKSPACE, cellIndex = 0),
            WorkspaceItem(2, keyC, Container.WORKSPACE, cellIndex = 3),
        )
        val resolved = WorkspaceResolver.workspaceApps(items, listOf(app(keyA)))
        assertEquals(mapOf(0 to app(keyA)), resolved)
    }

    @Test
    fun `dock items are ordered and stale entries skipped`() {
        val items = listOf(
            WorkspaceItem(1, keyB, Container.DOCK, cellIndex = 1),
            WorkspaceItem(2, keyC, Container.DOCK, cellIndex = 2),
            WorkspaceItem(3, keyA, Container.DOCK, cellIndex = 0),
        )
        val resolved = WorkspaceResolver.dockApps(items, listOf(app(keyA), app(keyB)))
        assertEquals(listOf("com.a", "com.b"), resolved.map { it.packageName })
    }

    @Test
    fun `duplicate cells keep the first persisted item`() {
        val items = listOf(
            WorkspaceItem(1, keyA, Container.WORKSPACE, cellIndex = 2),
            WorkspaceItem(2, keyB, Container.WORKSPACE, cellIndex = 2),
        )
        val resolved = WorkspaceResolver.workspaceApps(items, listOf(app(keyA), app(keyB)))
        assertEquals(keyA, resolved.getValue(2).key)
    }

    @Test
    fun `invalid cells are never rendered`() {
        val items = listOf(WorkspaceItem(1, keyA, Container.WORKSPACE, cellIndex = GridCanvas.CAPACITY))
        assertTrue(WorkspaceResolver.workspaceApps(items, listOf(app(keyA))).isEmpty())
    }
}
