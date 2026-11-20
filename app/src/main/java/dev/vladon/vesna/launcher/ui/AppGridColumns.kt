package dev.vladon.vesna.launcher.ui

import kotlin.math.ceil

internal const val MAX_APP_GRID_COLUMNS = 4
private const val MIN_APP_GRID_CELL_WIDTH_DP = 64

/** Fits complete app tiles into the measured content width, using at least one column. */
internal fun appGridColumnCount(availableWidth: Int, density: Float): Int {
    val minCellWidth = ceil(MIN_APP_GRID_CELL_WIDTH_DP * density).toInt()
    return (availableWidth / minCellWidth).coerceIn(1, MAX_APP_GRID_COLUMNS)
}
