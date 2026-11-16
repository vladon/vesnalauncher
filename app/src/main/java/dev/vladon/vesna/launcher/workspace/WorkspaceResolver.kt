package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp

/**
 * Pure projection of persisted layout items onto live apps: joins items with the
 * current app list so entries of uninstalled apps never reach the UI.
 */
object WorkspaceResolver {

    /** Workspace cell index to app; stale items and duplicate cells are ignored. */
    fun workspaceApps(items: List<WorkspaceItem>, apps: List<LauncherApp>): Map<Int, LauncherApp> {
        val appByKey = apps.associateBy { it.key }
        val result = LinkedHashMap<Int, LauncherApp>()
        for (item in items) {
            if (item.container != Container.WORKSPACE) continue
            if (!GridCanvas.isValidCell(item.cellIndex)) continue
            if (result.containsKey(item.cellIndex)) continue
            appByKey[item.key]?.let { result[item.cellIndex] = it }
        }
        return result
    }

    /** Dock apps ordered by position; stale items are ignored. */
    fun dockApps(items: List<WorkspaceItem>, apps: List<LauncherApp>): List<LauncherApp> {
        val appByKey = apps.associateBy { it.key }
        return items.asSequence()
            .filter { it.container == Container.DOCK }
            .sortedBy { it.cellIndex }
            .mapNotNull { appByKey[it.key] }
            .toList()
    }

    /** Keys referenced by persisted items that are no longer installed. */
    fun staleKeys(items: List<WorkspaceItem>, liveKeys: Set<AppKey>): Set<AppKey> =
        items.map { it.key }.filter { it !in liveKeys }.toSet()
}
