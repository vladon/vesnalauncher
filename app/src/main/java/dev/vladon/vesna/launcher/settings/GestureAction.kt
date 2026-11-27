package dev.vladon.vesna.launcher.settings

/** Actions a Home gesture can be bound to (#183 phase 1: the swipe down). */
enum class GestureAction(val preferenceValue: String) {
    NONE("none"),
    EXPAND_NOTIFICATIONS("notifications"),
    OPEN_DRAWER("drawer"),
    OPEN_SEARCH("search"),
    OPEN_SETTINGS("settings");

    companion object {
        fun fromPreference(value: String?): GestureAction =
            entries.firstOrNull { it.preferenceValue == value } ?: NONE
    }
}
