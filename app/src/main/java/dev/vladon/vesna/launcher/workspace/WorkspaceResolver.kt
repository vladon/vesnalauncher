package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp

/**
 * Pure projection of persisted layout items onto live apps: joins items with the
 * current app list so entries of uninstalled apps never reach the UI. Items inside
 * folders (non-null folderId) are excluded from the grid and dock views.
 */
object WorkspaceResolver {

    /** Workspace cell index to app; stale items and duplicate cells are ignored. */
    fun workspaceApps(items: List<WorkspaceItem>, apps: List<LauncherApp>): Map<Int, LauncherApp> {
        val appByKey = apps.associateBy { it.key }
        val result = LinkedHashMap<Int, LauncherApp>()
        for (item in items) {
            if (item.container != Container.WORKSPACE) continue
            if (item.folderId != null) continue
            if (!GridCanvas.isValidCell(item.cellIndex)) continue
            if (result.containsKey(item.cellIndex)) continue
            appByKey[item.key]?.let { result[item.cellIndex] = it }
        }
        return result
    }

    /**
     * Workspace cell contents for rendering: top-level apps plus folders occupying
     * their cells (with member counts). Folder members are not shown at top level.
     */
    fun workspaceCells(
        items: List<WorkspaceItem>,
        apps: List<LauncherApp>,
        folders: List<Folder>,
    ): Map<Int, CellContent> {
        val cells = LinkedHashMap<Int, CellContent>()
        for ((cell, app) in workspaceApps(items, apps)) {
            cells[cell] = CellContent.App(app)
        }
        for (folder in folders) {
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
        apps: List<LauncherApp>,
    ): Map<Long, List<LauncherApp>> {
        val appByKey = apps.associateBy { it.key }
        return items.asSequence()
            .filter { it.folderId != null }
            .groupBy { it.folderId!! }
            .mapValues { (_, members) ->
                members.sortedBy { it.cellIndex }.mapNotNull { appByKey[it.key] }
            }
    }

    /** Dock apps ordered by position; stale entries and folder members are skipped. */
    fun dockApps(items: List<WorkspaceItem>, apps: List<LauncherApp>): List<LauncherApp> {
        val appByKey = apps.associateBy { it.key }
        return items.asSequence()
            .filter { it.container == Container.DOCK && it.folderId == null }
            .sortedBy { it.cellIndex }
            .mapNotNull { appByKey[it.key] }
            .toList()
    }

    /** Keys referenced by persisted items that are no longer installed. */
    fun staleKeys(items: List<WorkspaceItem>, liveKeys: Set<AppKey>): Set<AppKey> =
        items.map { it.key }.filter { it !in liveKeys }.toSet()
}
