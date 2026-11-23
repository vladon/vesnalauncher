package dev.vladon.vesna.launcher.workspace

/** A top-level app or folder placement. Members and dock pins are never reflowed. */
internal class GridPlacement(
    val id: Long,
    val folder: Boolean,
    val oldPage: Int,
    val oldCell: Int,
) {
    var page = -1
    var cell = -1
}

/** Plans every destination before Room writes anything. Returns null when it cannot all fit. */
internal fun reflowWorkspaceGrid(
    from: GridCanvas,
    to: GridCanvas,
    pageCount: Int,
    placements: MutableList<GridPlacement>,
): Int? {
    if (placements.size > MAX_WORKSPACE_PAGES * to.capacity) return null
    // Reserve valid old coordinates first, then fill holes on the original page before spilling.
    placements.sortWith(compareBy<GridPlacement> { it.oldPage }.thenBy { it.oldCell }.thenBy { it.folder }.thenBy { it.id })
    // Max grid is 30 cells, so a mask keeps each page's occupancy and first-free lookup compact.
    val fullMask = (1 shl to.capacity) - 1
    val occupied = IntArray(MAX_WORKSPACE_PAGES)
    fun reserve(placement: GridPlacement, page: Int, cell: Int) {
        occupied[page] = occupied[page] or (1 shl cell)
        placement.page = page
        placement.cell = cell
    }

    for (placement in placements) {
        if (placement.oldPage < 0 || !from.isValidCell(placement.oldCell)) continue
        val row = from.rowOf(placement.oldCell)
        val column = from.columnOf(placement.oldCell)
        if (row >= to.rows || column >= to.columns) continue
        val cell = row * to.columns + column
        if (occupied[placement.oldPage] and (1 shl cell) == 0) reserve(placement, placement.oldPage, cell)
    }
    for (placement in placements) {
        if (placement.page >= 0 || placement.oldPage < 0) continue
        val free = fullMask xor occupied[placement.oldPage]
        if (free != 0) reserve(placement, placement.oldPage, free.countTrailingZeroBits())
    }
    var firstAvailable = 0
    var usedPages = pageCount
    for (placement in placements) {
        if (placement.page >= 0) continue
        while (occupied[firstAvailable] == fullMask) firstAvailable++
        reserve(placement, firstAvailable, (fullMask xor occupied[firstAvailable]).countTrailingZeroBits())
        usedPages = maxOf(usedPages, firstAvailable + 1)
    }
    return usedPages
}
