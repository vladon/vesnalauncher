package dev.vladon.vesna.launcher.settings

/** Workspace page indicator styles (#174). Counter keeps today's dots plus the page counter. */
enum class PageIndicator(val preferenceValue: String) {
    COUNTER("counter"),
    DOT("dot"),
    LINE("line");

    companion object {
        fun fromPreference(value: String?): PageIndicator =
            entries.firstOrNull { it.preferenceValue == value } ?: COUNTER
    }
}
