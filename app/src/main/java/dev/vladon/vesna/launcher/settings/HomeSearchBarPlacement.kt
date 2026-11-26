package dev.vladon.vesna.launcher.settings

/** Placement of the optional persistent search bar on Home (#186). */
enum class HomeSearchBarPlacement(val preferenceValue: String) {
    BELOW_DOCK("below"),
    ABOVE_DOCK("above"),
    OFF("off");

    companion object {
        fun fromPreference(value: String?): HomeSearchBarPlacement =
            entries.firstOrNull { it.preferenceValue == value } ?: BELOW_DOCK
    }
}
