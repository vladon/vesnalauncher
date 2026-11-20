package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp

/** A rendered dock tile keeps the ID of its persisted pin. */
data class DockPin(val id: Long, val app: LauncherApp)

/**
 * Pure projection of persisted layout items onto live apps. Unavailable apps
 * are hidden without changing their stored positions. Items inside folders
 * (non-null folderId) are excluded from the grid and dock views.
 */
internal object WorkspaceResolver {

    /**
     * Workspace cell contents for rendering: top-level apps plus folders occupying
     * their cells (with member counts). Folder members are not shown at top level.
     */
    fun workspaceCells(
        items: List<WorkspaceItem>,
        appsByKey: Map<AppKey, LauncherApp>,
        folders: List<Folder>,
        pageId: Long = DEFAULT_WORKSPACE_PAGE_ID,
    ): Map<Int, CellContent> {
        val cells = LinkedHashMap<Int, CellContent>()
        for (item in items) {
            if (item.container != Container.WORKSPACE) continue
            if (item.folderId != null || item.pageId != pageId) continue
            if (!GridCanvas.isValidCell(item.cellIndex)) continue
            if (cells.containsKey(item.cellIndex)) continue
            appsByKey[item.key]?.let { cells[item.cellIndex] = CellContent.App(it) }
        }
        for (folder in folders) {
            if (folder.pageId != pageId) continue
            if (!GridCanvas.isValidCell(folder.cellIndex)) continue
            if (cells.containsKey(folder.cellIndex)) continue
            val members = items.count { it.folderId == folder.id }
            cells[folder.cellIndex] = CellContent.FolderCell(folder, members)
        }
        return cells
    }

    /** Apps inside each folder, ordered by their in-folder position. */
    fun folderContents(
        items: List<WorkspaceItem>,
        appsByKey: Map<AppKey, LauncherApp>,
    ): Map<Long, List<LauncherApp>> {
        return items.asSequence()
            .filter { it.folderId != null }
            .groupBy { it.folderId!! }
            .mapValues { (_, members) ->
                members.sortedBy { it.cellIndex }.mapNotNull { appsByKey[it.key] }
            }
    }

    /** Dock pins ordered by position; unavailable apps and folder members are skipped. */
    fun dockPins(items: List<WorkspaceItem>, appsByKey: Map<AppKey, LauncherApp>): List<DockPin> {
        return items.asSequence()
            .filter { it.container == Container.DOCK && it.folderId == null }
            .sortedBy { it.cellIndex }
            .mapNotNull { item -> appsByKey[item.key]?.let { DockPin(item.id, it) } }
            .toList()
    }
}
