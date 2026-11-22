package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp

/** A rendered dock tile keeps the ID of its persisted pin. */
data class DockPin(val id: Long, val app: LauncherApp)

/** A rendered folder tile keeps the ID of its persisted membership. */
data class FolderPin(val id: Long, val app: LauncherApp)

/** Saved order includes unavailable pins; the visible dock packs the remaining icons. */
data class DockLayout(val savedOrder: List<Long>, val pins: List<DockPin>) {
    internal fun dropColumn(column: Int, reordering: Boolean): Int? {
        val last = if (reordering) pins.lastIndex else {
            if (savedOrder.size >= GridCanvas.DOCK_CAPACITY) return null
            pins.size
        }
        return if (last < 0) null else column.coerceIn(0, last)
    }

    /** Convert a visual destination to its index in the saved order, or append. */
    internal fun savedSlot(column: Int): Int =
        pins.getOrNull(column)?.let { savedOrder.indexOf(it.id) } ?: savedOrder.size
}

/** Visible cells and a bit per cell held by an unavailable shortcut on the same page. */
internal data class WorkspaceLayout(val cells: Map<Int, CellContent>, val reservedCellsMask: Int)

/**
 * Pure projection of persisted layout items onto live apps. Unavailable apps
 * are hidden without changing their stored positions. Items inside folders
 * (non-null folderId) are excluded from the grid and dock views.
 */
internal object WorkspaceResolver {

    /**
     * Project one page's visible shortcuts and folders while keeping unavailable
     * top-level shortcuts reserved. Folder members do not occupy grid cells.
     */
    fun workspaceLayout(
        items: List<WorkspaceItem>,
        appsByKey: Map<AppKey, LauncherApp>,
        folders: List<Folder>,
        pageId: Long = DEFAULT_WORKSPACE_PAGE_ID,
    ): WorkspaceLayout {
        val cells = LinkedHashMap<Int, CellContent>()
        var reservedCellsMask = 0
        for (item in items) {
            if (item.container != Container.WORKSPACE) continue
            if (item.folderId != null || item.pageId != pageId) continue
            val cell = item.cellIndex
            if (!GridCanvas.isValidCell(cell) || cells.containsKey(cell)) continue
            val app = appsByKey[item.key]
            val bit = 1 shl cell
            if (app == null) {
                reservedCellsMask = reservedCellsMask or bit
            } else {
                cells[cell] = CellContent.App(app)
                reservedCellsMask = reservedCellsMask and bit.inv()
            }
        }
        for (folder in folders) {
            if (folder.pageId != pageId) continue
            if (!GridCanvas.isValidCell(folder.cellIndex)) continue
            if (cells.containsKey(folder.cellIndex)) continue
            val members = items.count { it.folderId == folder.id }
            cells[folder.cellIndex] = CellContent.FolderCell(folder, members)
            reservedCellsMask = reservedCellsMask and (1 shl folder.cellIndex).inv()
        }
        return WorkspaceLayout(cells, reservedCellsMask)
    }

    /** Visible pins inside each folder, ordered by their saved positions. */
    fun folderContents(
        items: List<WorkspaceItem>,
        appsByKey: Map<AppKey, LauncherApp>,
    ): Map<Long, List<FolderPin>> {
        return items.asSequence()
            .filter { it.folderId != null }
            .groupBy { it.folderId!! }
            .mapValues { (_, members) ->
                members.sortedBy { it.cellIndex }.mapNotNull { item ->
                    appsByKey[item.key]?.let { FolderPin(item.id, it) }
                }
            }
    }

    /** Only top-level pins consume dock capacity; unavailable pins are not drawn. */
    fun dockLayout(items: List<WorkspaceItem>, appsByKey: Map<AppKey, LauncherApp>): DockLayout {
        val saved = items.filter { it.container == Container.DOCK && it.folderId == null }
            .sortedBy { it.cellIndex }
        return DockLayout(
            savedOrder = saved.map { it.id },
            pins = saved.mapNotNull { item -> appsByKey[item.key]?.let { DockPin(item.id, it) } },
        )
    }
}
