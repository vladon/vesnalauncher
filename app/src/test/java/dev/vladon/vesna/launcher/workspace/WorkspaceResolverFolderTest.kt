package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Folder semantics of [WorkspaceResolver]: folder members must not appear at the
 * top level, folder cells must render with member counts, and stale members
 * (uninstalled apps) must not surface.
 */
class WorkspaceResolverFolderTest {

    private fun app(key: AppKey) = LauncherApp(key = key, label = key.packageName)

    private val keyA = AppKey("com.a", "com.a.Main")
    private val keyB = AppKey("com.b", "com.b.Main")
    private val keyC = AppKey("com.c", "com.c.Main")
    private val keyD = AppKey("com.d", "com.d.Main")

    private fun item(key: AppKey, cell: Int, folderId: Long? = null, id: Long = 0) =
        WorkspaceItem(id = id, key = key, container = Container.WORKSPACE, cellIndex = cell, folderId = folderId)

    @Test
    fun `folder members are excluded from top level cells`() {
        val items = listOf(
            item(keyA, cell = 0),
            item(keyB, cell = 1, folderId = 7L),
        )
        val resolved = WorkspaceResolver.workspaceCells(items, mapOf(keyA to app(keyA), keyB to app(keyB)), emptyList())
        assertEquals(mapOf(0 to CellContent.App(app(keyA))), resolved)
    }

    @Test
    fun `folder cell renders with member count`() {
        val folder = Folder(id = 7L, title = "Work", cellIndex = 3)
        val items = listOf(
            item(keyA, cell = 3, folderId = 7L),
            item(keyB, cell = 5, folderId = 7L),
        )
        val cells = WorkspaceResolver.workspaceCells(items, mapOf(keyA to app(keyA), keyB to app(keyB)), listOf(folder))
        val content = cells[3]
        assertTrue(content is CellContent.FolderCell)
        assertEquals(2, (content as CellContent.FolderCell).memberCount)
        assertEquals(folder, content.folder)
    }

    @Test
    fun `folder at invalid cell is skipped`() {
        val folder = Folder(id = 7L, title = "Work", cellIndex = GridCanvas.CAPACITY)
        val cells = WorkspaceResolver.workspaceCells(emptyList(), emptyMap(), listOf(folder))
        assertTrue(cells.isEmpty())
    }

    @Test
    fun `folder does not override an app already at its cell`() {
        val folder = Folder(id = 7L, title = "Work", cellIndex = 0)
        val items = listOf(item(keyA, cell = 0))
        val cells = WorkspaceResolver.workspaceCells(items, mapOf(keyA to app(keyA)), listOf(folder))
        assertEquals(CellContent.App::class, cells.getValue(0)::class)
    }

    @Test
    fun `folderContents keeps each copy of an app in saved position order`() {
        val items = listOf(
            item(keyA, cell = 5, folderId = 7L, id = 17L),
            item(keyC, cell = 3, folderId = 7L, id = 25L),
            item(keyA, cell = 1, folderId = 7L, id = 30L),
        )
        val apps = mapOf(keyA to app(keyA), keyC to app(keyC))
        val contents = WorkspaceResolver.folderContents(items, apps)
        assertEquals(
            listOf(FolderPin(30L, app(keyA)), FolderPin(25L, app(keyC)), FolderPin(17L, app(keyA))),
            contents.getValue(7L),
        )
    }

    @Test
    fun `folderContents drops stale members`() {
        val items = listOf(
            item(keyA, cell = 1, folderId = 7L),
            item(keyD, cell = 2, folderId = 7L),
        )
        val contents = WorkspaceResolver.folderContents(items, mapOf(keyA to app(keyA)))
        assertEquals(listOf(keyA), contents.getValue(7L).map { it.app.key })
    }

    @Test
    fun `folderContents is empty without folders`() {
        assertTrue(WorkspaceResolver.folderContents(emptyList(), emptyMap()).isEmpty())
    }
}
