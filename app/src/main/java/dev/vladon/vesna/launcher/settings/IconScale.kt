package dev.vladon.vesna.launcher.settings

import kotlin.math.abs

/** Icon artwork sizes offered in Settings, as percentages of the default artwork. */
internal object IconScale {
    const val DEFAULT = 100
    val steps = listOf(80, 90, 100, 110, 120, 130)

    /** The offered step closest to a stored value, so an unknown number never reaches a view. */
    fun coerce(percent: Int): Int = steps.minByOrNull { abs(it - percent) } ?: DEFAULT
}
