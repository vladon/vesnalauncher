package dev.vladon.vesna.launcher.apps

import dev.vladon.vesna.launcher.search.searchKey

/** A searchable snapshot of the installed apps in drawer order. */
internal class AppSearch(
    private val apps: List<LauncherApp>,
    private val hiddenApps: Set<AppKey> = emptySet(),
) {
    private val browsingApps = if (hiddenApps.isEmpty()) apps else apps.filterNot { it.key in hiddenApps }
    private var labelKeys: Array<String>? = null

    fun filter(query: String?): List<LauncherApp> {
        val needle = searchKey(query.orEmpty())
        if (needle.isEmpty()) return browsingApps
        val keys = labelKeys ?: Array(apps.size) { searchKey(apps[it].label) }.also { labelKeys = it }
        val matches = ArrayList<LauncherApp>()
        for (index in keys.indices) {
            val label = keys[index]
            // Hidden entries are still reachable through an explicit full-name search.
            if (label == needle || (label.contains(needle) && apps[index].key !in hiddenApps)) {
                matches.add(apps[index])
            }
        }
        return matches
    }
}
