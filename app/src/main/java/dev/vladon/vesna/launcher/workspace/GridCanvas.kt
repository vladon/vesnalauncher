package dev.vladon.vesna.launcher.workspace

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
}
