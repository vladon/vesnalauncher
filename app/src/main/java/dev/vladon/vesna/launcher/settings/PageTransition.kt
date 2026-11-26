package dev.vladon.vesna.launcher.settings

/** Home page transition effects (#180). Instant keeps today's behaviour. */
enum class PageTransition(val preferenceValue: String) {
    INSTANT("instant"),
    SLIDE("slide");

    companion object {
        fun fromPreference(value: String?): PageTransition =
            entries.firstOrNull { it.preferenceValue == value } ?: INSTANT
    }
}
