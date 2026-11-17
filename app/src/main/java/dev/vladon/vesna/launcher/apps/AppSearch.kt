package dev.vladon.vesna.launcher.apps

/**
 * Pure filtering of the drawer app list. Free of Android dependencies so it stays
 * unit testable.
 */
object AppSearch {

    /**
     * Filters [apps] by [query]: case-insensitive substring match on the label.
     * A blank query returns the list unchanged. Query is trimmed and null-safe.
     */
    fun filter(apps: List<LauncherApp>, query: String?): List<LauncherApp> {
        val needle = query?.trim().orEmpty()
        if (needle.isEmpty()) return apps
        return apps.filter { it.label.contains(needle, ignoreCase = true) }
    }
}
