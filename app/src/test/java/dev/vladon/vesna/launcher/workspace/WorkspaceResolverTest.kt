package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            WorkspaceResolver.workspaceLayout(items, apps, emptyList()).cells,
        )
    }

    @Test
    fun `unavailable pins reserve only valid top level cells on their page`() {
        val items = listOf(
            WorkspaceItem(1, keyA, Container.WORKSPACE, cellIndex = 0),
            WorkspaceItem(2, keyC, Container.WORKSPACE, cellIndex = 3),
            WorkspaceItem(3, keyC, Container.WORKSPACE, cellIndex = 4, pageId = 2),
            WorkspaceItem(4, keyC, Container.WORKSPACE, cellIndex = 5, folderId = 7),
            WorkspaceItem(5, keyC, Container.DOCK, cellIndex = 1),
            WorkspaceItem(6, keyC, Container.WORKSPACE, cellIndex = -1),
            WorkspaceItem(7, keyC, Container.WORKSPACE, cellIndex = GridCanvas.DEFAULT.capacity),
        )
        val available = mapOf(keyA to app(keyA))
        val first = WorkspaceResolver.workspaceLayout(items, available, emptyList())
        assertEquals(mapOf(0 to CellContent.App(app(keyA))), first.cells)
        assertEquals(1 shl 3, first.reservedCellsMask)
        val second = WorkspaceResolver.workspaceLayout(items, available, emptyList(), pageId = 2)
        assertTrue(second.cells.isEmpty())
        assertEquals(1 shl 4, second.reservedCellsMask)
    }

    @Test
    fun `dock items are ordered and stale entries skipped`() {
        val items = listOf(
            WorkspaceItem(1, keyB, Container.DOCK, cellIndex = 1),
            WorkspaceItem(2, keyC, Container.DOCK, cellIndex = 2),
            WorkspaceItem(3, keyA, Container.DOCK, cellIndex = 0),
            WorkspaceItem(4, keyA, Container.DOCK, cellIndex = 3),
        )
        val dock = WorkspaceResolver.dockLayout(items, mapOf(keyA to app(keyA), keyB to app(keyB)))
        assertEquals(listOf(DockPin(3, app(keyA)), DockPin(1, app(keyB)), DockPin(4, app(keyA))), dock.pins)
        assertNull("The hidden pin still occupies the fourth saved slot", dock.dropColumn(3, reordering = false))

        val destination = requireNotNull(dock.dropColumn(3, reordering = true))
        val reordered = DockOrdering.moveTo(dock.savedOrder, fromSlot = 0, toSlot = dock.savedSlot(destination))
        assertEquals(listOf(1L, 4L, 3L), reordered.filter { it != 2L })
    }

    @Test
    fun `the highlighted dock destination accounts for hidden pins and packs towards start`() {
        val items = listOf(
            WorkspaceItem(1, keyA, Container.DOCK, cellIndex = 0),
            WorkspaceItem(2, keyC, Container.DOCK, cellIndex = 1),
            WorkspaceItem(3, keyB, Container.DOCK, cellIndex = 3),
            WorkspaceItem(4, keyA, Container.DOCK, cellIndex = 2, folderId = 7),
        )
        val dock = WorkspaceResolver.dockLayout(items, mapOf(keyA to app(keyA), keyB to app(keyB)))
        val between = requireNotNull(dock.dropColumn(1, reordering = false))
        val trailing = requireNotNull(dock.dropColumn(3, reordering = false))
        assertEquals(2, trailing)
        assertEquals(listOf(1L, 2L, 10L, 3L), DockOrdering.insertAt(dock.savedOrder, 10, dock.savedSlot(between)))
        assertEquals(listOf(1L, 2L, 3L, 10L), DockOrdering.insertAt(dock.savedOrder, 10, dock.savedSlot(trailing)))
    }

    @Test
    fun `duplicate cells keep the first persisted item`() {
        val items = listOf(
            WorkspaceItem(1, keyA, Container.WORKSPACE, cellIndex = 2),
            WorkspaceItem(2, keyB, Container.WORKSPACE, cellIndex = 2),
        )
        val resolved = WorkspaceResolver.workspaceLayout(items, mapOf(keyA to app(keyA), keyB to app(keyB)), emptyList()).cells
        assertEquals(CellContent.App(app(keyA)), resolved.getValue(2))
    }

    @Test
    fun `invalid cells are never rendered`() {
        val items = listOf(WorkspaceItem(1, keyA, Container.WORKSPACE, cellIndex = GridCanvas.DEFAULT.capacity))
        assertTrue(WorkspaceResolver.workspaceLayout(items, mapOf(keyA to app(keyA)), emptyList()).cells.isEmpty())
    }

    @Test
    fun `custom label projects onto workspace dock and folder pins`() {
        val items = listOf(
            WorkspaceItem(1, keyA, Container.WORKSPACE, cellIndex = 0, customLabel = "Grid alias"),
            WorkspaceItem(2, keyB, Container.DOCK, cellIndex = 0, customLabel = "Dock alias"),
            WorkspaceItem(3, keyC, Container.WORKSPACE, cellIndex = 0, folderId = 7L, customLabel = "Folder alias"),
            WorkspaceItem(4, keyA, Container.DOCK, cellIndex = 1),
        )
        val apps = listOf(app(keyA), app(keyB), app(keyC)).associateBy { it.key }
        val folder = Folder(id = 7L, title = "Work", cellIndex = 3)

        assertEquals(
            CellContent.App(app(keyA).copy(label = "Grid alias", canonicalLabel = "com.a")),
            WorkspaceResolver.workspaceLayout(items, apps, listOf(folder)).cells.getValue(0),
        )
        assertEquals(
            listOf(
                DockPin(2, app(keyB).copy(label = "Dock alias", canonicalLabel = "com.b")),
                DockPin(4, app(keyA)),
            ),
            WorkspaceResolver.dockLayout(items, apps).pins,
        )
        assertEquals(
            listOf(FolderPin(3, app(keyC).copy(label = "Folder alias", canonicalLabel = "com.c"))),
            WorkspaceResolver.folderContents(items, apps).getValue(7L),
        )
    }

    @Test
    fun `renamed pin keeps canonical name and badged accessibility`() {
        val badged = LauncherApp(key = keyA, label = "Original", badgedLabel = "Original (work)")

        // Without a rename nothing changes: badged name only.
        assertEquals("Original (work)", badged.accessibilityLabel)

        val renamed = badged.withVisibleLabel("Renamed")
        assertEquals("Renamed", renamed.label)
        assertEquals("The canonical publisher name survives the projection", "Original", renamed.publisherLabel)
        assertEquals("Visible custom name first, badged app name second", "Renamed, Original (work)", renamed.accessibilityLabel)

        // A renamed pin without a badge announces just the visible name.
        val renamedNoBadge = LauncherApp(key = keyB, label = "Plain").withVisibleLabel("Custom")
        assertEquals("Custom", renamedNoBadge.accessibilityLabel)
        assertEquals("Plain", renamedNoBadge.publisherLabel)

        // Unlabelled apps keep the plain catalogue label for accessibility.
        assertEquals("Original", LauncherApp(key = keyC, label = "Original").accessibilityLabel)
    }

    @Test
    fun `canonical name survives chained projections`() {
        val once = LauncherApp(key = keyA, label = "Share").withVisibleLabel("Send it")
        assertEquals("Send it", once.label)
        assertEquals("Share", once.publisherLabel)

        val twice = once.withVisibleLabel("Again")
        assertEquals("Again", twice.label)
        assertEquals("The first canonical name wins across re-projections", "Share", twice.publisherLabel)

        // A null override leaves the already-projected app untouched.
        assertEquals(once, once.withVisibleLabel(null))
    }
}
