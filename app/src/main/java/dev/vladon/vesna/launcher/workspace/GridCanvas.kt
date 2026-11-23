package dev.vladon.vesna.launcher.workspace

import kotlin.math.floor

/** Workspace geometry, indexed row by row from the reading start edge. */
data class GridCanvas(val columns: Int, val rows: Int) {
    init {
        require(columns in MIN_COLUMNS..MAX_COLUMNS && rows in MIN_ROWS..MAX_ROWS)
    }

    val capacity: Int = columns * rows

    fun isValidCell(cellIndex: Int): Boolean = cellIndex in 0 until capacity

    fun rowOf(cellIndex: Int): Int = cellIndex / columns

    fun columnOf(cellIndex: Int): Int = cellIndex % columns

    /**
     * The first unoccupied cell scanning row by row, or null when the grid is full.
     * Out-of-range indexes in the input are ignored.
     */
    fun firstFreeCell(occupiedCells: Set<Int>): Int? =
        (0 until capacity).firstOrNull { it !in occupiedCells }

    /**
     * The grid cell containing the point, given the grid origin and cell size used
     * by the layout pass; null when the point is outside the grid or the cell size
     * is degenerate. Used by drag-and-drop to find the drop target.
     */
    fun cellAtPoint(
        x: Float,
        y: Float,
        gridLeft: Float,
        gridTop: Float,
        cellWidth: Float,
        cellHeight: Float,
    ): Int? {
        if (cellWidth <= 0f || cellHeight <= 0f) return null
        // Floor (not truncate): points left/above the grid origin must map to
        // "outside", not to the first cell.
        val column = floor((x - gridLeft) / cellWidth).toInt()
        val row = floor((y - gridTop) / cellHeight).toInt()
        return if (column in 0 until columns && row in 0 until rows) row * columns + column else null
    }

    companion object {
        const val MIN_COLUMNS = 2
        const val MAX_COLUMNS = 5
        const val MIN_ROWS = 2
        const val MAX_ROWS = 6
        val DEFAULT = GridCanvas(columns = 4, rows = 5)

        /** Favorites use a separate fixed capacity, regardless of workspace size. */
        const val DOCK_CAPACITY = 4
    }
}
