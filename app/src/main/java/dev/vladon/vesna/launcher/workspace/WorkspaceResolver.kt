package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp

/** A rendered dock tile keeps the ID of its persisted pin. */
data class DockPin(val id: Long, val app: LauncherApp)

/** A rendered folder tile keeps the ID of its persisted membership. */
data class FolderPin(val id: Long, val app: LauncherApp)

/** Saved order includes unavailable pins; the visible dock packs the remaining icons. */
data class DockLayout(val savedOrder: List<Long>, val pins: List<DockPin>) {
    internal fun dropColumn(
        column: Int,
        reordering: Boolean,
        capacityPerPage: Int = GridCanvas.DOCK_CAPACITY,
    ): Int? {
        val last = if (reordering) pins.lastIndex else {
            if (savedOrder.size >= DockPages.totalCapacity(capacityPerPage)) return null
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
 *
 * A pin's [WorkspaceItem.customLabel] rename is projected onto the rendered
 * [LauncherApp] of every surface (grid, dock, folders) while the catalogue entry
 * itself stays untouched; keys with an [AppKey.shortcutId] resolve against the
 * combined app map the caller passes in, exactly like ordinary apps.
 * [WorkspaceItem.shortcutLabel] is a storage-level fallback consumed when the
 * platform re-synthesizes saved actions, not projected here.
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
        grid: GridCanvas = GridCanvas.DEFAULT,
    ): WorkspaceLayout {
        val cells = LinkedHashMap<Int, CellContent>()
        var reservedCellsMask = 0
        for (item in items) {
            if (item.container != Container.WORKSPACE) continue
            if (item.folderId != null || item.pageId != pageId) continue
            val cell = item.cellIndex
            if (!grid.isValidCell(cell) || cells.containsKey(cell)) continue
            val app = appsByKey[item.key]
            val bit = 1 shl cell
            if (app == null) {
                reservedCellsMask = reservedCellsMask or bit
            } else {
                cells[cell] = CellContent.App(app.withVisibleLabel(item.customLabel))
                reservedCellsMask = reservedCellsMask and bit.inv()
            }
        }
        for (folder in folders) {
            if (folder.pageId != pageId) continue
            if (!grid.isValidCell(folder.cellIndex)) continue
            if (cells.containsKey(folder.cellIndex)) continue
            val members = items.count { it.folderId == folder.id }
            cells[folder.cellIndex] = CellContent.FolderCell(folder, members)
            reservedCellsMask = reservedCellsMask and (1 shl folder.cellIndex).inv()
        }
        return WorkspaceLayout(cells, reservedCellsMask)
    }

    private val folderMemberOrder = compareBy<WorkspaceItem> { it.cellIndex }.thenBy { it.id }

    /** Visible pins inside each folder, ordered by their saved positions. */
    fun folderContents(
        items: List<WorkspaceItem>,
        appsByKey: Map<AppKey, LauncherApp>,
    ): Map<Long, List<FolderPin>> {
        return items.asSequence()
            .filter { it.folderId != null }
            .groupBy { it.folderId!! }
            .mapValues { (_, members) ->
                members.sortedWith(folderMemberOrder).mapNotNull { item ->
                    appsByKey[item.key]?.let { FolderPin(item.id, it.withVisibleLabel(item.customLabel)) }
                }
            }
    }

    /** Only top-level pins consume dock capacity; unavailable pins are not drawn. */
    fun dockLayout(items: List<WorkspaceItem>, appsByKey: Map<AppKey, LauncherApp>): DockLayout {
        val saved = items.filter { it.container == Container.DOCK && it.folderId == null }
            .sortedBy { it.cellIndex }
        return DockLayout(
            savedOrder = saved.map { it.id },
            pins = saved.mapNotNull { item ->
                appsByKey[item.key]?.let { DockPin(item.id, it.withVisibleLabel(item.customLabel)) }
            },
        )
    }
}
