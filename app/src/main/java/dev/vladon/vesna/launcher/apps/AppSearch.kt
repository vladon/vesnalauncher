package dev.vladon.vesna.launcher.apps

import dev.vladon.vesna.launcher.search.searchKey

/** A searchable snapshot of the installed apps in drawer order. */
internal class AppSearch(private val apps: List<LauncherApp>) {
    private var labelKeys: Array<String>? = null

    fun filter(query: String?): List<LauncherApp> {
        val needle = searchKey(query.orEmpty())
        if (needle.isEmpty()) return apps
        val keys = labelKeys ?: Array(apps.size) { searchKey(apps[it].label) }.also { labelKeys = it }
        val matches = ArrayList<LauncherApp>()
        for (index in keys.indices) {
            if (keys[index].contains(needle)) matches.add(apps[index])
        }
        return matches
    }
}
