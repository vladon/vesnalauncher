package dev.vladon.vesna.launcher.settings

import kotlin.math.abs

/**
 * Home and dock label typography (#173): a size scale in percent of the
 * default label text and a small set of font faces. Typography only — the
 * icon artwork, cells, touch targets and placements are never affected.
 */
enum class LabelFont(val preferenceValue: String, val fontFamily: String) {
    INTER_MEDIUM("inter_medium", "inter_medium"),
    INTER_REGULAR("inter_regular", "inter_regular"),
    NUNITO_REGULAR("nunito_regular", "nunito_regular");

    companion object {
        const val DEFAULT_PREFERENCE_VALUE = "inter_medium"

        fun fromPreference(value: String?): LabelFont =
            entries.firstOrNull { it.preferenceValue == value } ?: INTER_MEDIUM
    }
}

internal object LabelFontScale {
    const val DEFAULT = 100
    val steps = listOf(80, 90, 100, 110, 120, 130)

    /** The offered step closest to a stored value, so an unknown number never reaches a view. */
    fun coerce(percent: Int): Int = steps.minByOrNull { abs(it - percent) } ?: DEFAULT
}
