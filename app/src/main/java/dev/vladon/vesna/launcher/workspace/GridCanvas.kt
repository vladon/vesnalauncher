package dev.vladon.vesna.launcher.workspace

import kotlin.math.floor

/**
 * Geometry rules of the workspace grid and dock. Pure Kotlin, free of Android
 * dependencies, so placement logic is unit testable on the JVM.
 */
object GridCanvas {
    const val COLUMNS = 4
    const val ROWS = 5
    const val CAPACITY = COLUMNS * ROWS

    /** The dock aligns with the workspace columns. */
    const val DOCK_CAPACITY = COLUMNS

    fun isValidCell(cellIndex: Int): Boolean = cellIndex in 0 until CAPACITY

    fun rowOf(cellIndex: Int): Int = cellIndex / COLUMNS

    fun columnOf(cellIndex: Int): Int = cellIndex % COLUMNS

    /**
     * The first unoccupied cell scanning row by row, or null when the grid is full.
     * Out-of-range indexes in the input are ignored.
     */
    fun firstFreeCell(occupiedCells: Set<Int>): Int? =
        (0 until CAPACITY).firstOrNull { it !in occupiedCells }

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
        if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS) return null
        return row * COLUMNS + column
    }
}
