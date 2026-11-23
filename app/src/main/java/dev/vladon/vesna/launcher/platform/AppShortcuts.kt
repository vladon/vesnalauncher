package dev.vladon.vesna.launcher.platform

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import android.os.UserManager
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp

internal data class ShortcutPublisher(val packageName: String, val userSerial: Long) {
    constructor(key: AppKey) : this(key.packageName, key.userSerial)
}

/** Public Android shortcut-host APIs, scoped to the publisher and Android profile. */
internal class AppShortcuts(private val context: Context) {
    private val launcher = context.getSystemService(LauncherApps::class.java)
    private val users = context.getSystemService(UserManager::class.java)

    fun canQuery(): Boolean = available { launcher?.hasShortcutHostPermission() == true } == true

    fun published(app: LauncherApp): List<LauncherApp> = available {
        val service = launcher ?: return@available emptyList()
        val key = app.key
        if (key.shortcutId != null || !service.hasShortcutHostPermission()) return@available emptyList()
        val user = users.getUserForSerialNumber(key.userSerial) ?: return@available emptyList()
        val query = ShortcutQuery()
            .setPackage(key.packageName)
            .setActivity(ComponentName(key.packageName, key.className))
            .setQueryFlags(PUBLISHED)
        service.getShortcuts(query, user).orEmpty().asSequence()
            .filter { it.isEnabled }
            .sortedWith(compareBy<ShortcutInfo> { it.rank }.thenBy { it.id })
            .map { descriptor(it, key.copy(shortcutId = it.id), user, app) }
            .toList()
    }.orEmpty()

    /** A removed or disabled action remains reserved in Room, not in the visible grid. */
    fun resolvePinned(
        savedLabels: Map<AppKey, String>,
        apps: Map<AppKey, LauncherApp>,
    ): Map<AppKey, LauncherApp> = buildMap {
        val service = launcher ?: return@buildMap
        val host = canQuery()
        for ((publisher, entries) in savedLabels.entries.groupBy { ShortcutPublisher(it.key) }) {
            val user = users.getUserForSerialNumber(publisher.userSerial) ?: continue
            val current = if (host) available {
                val query = ShortcutQuery().setPackage(publisher.packageName)
                    .setShortcutIds(entries.mapNotNull { it.key.shortcutId }.distinct())
                    .setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED)
                service.getShortcuts(query, user).orEmpty().associateBy { it.id }
            } ?: continue else emptyMap()
            for ((key, label) in entries) {
                val parent = apps[key.copy(shortcutId = null)]
                    ?: apps.values.firstOrNull { it.key.packageName == key.packageName && it.key.userSerial == key.userSerial }
                    ?: continue
                val shortcut = current[key.shortcutId]
                if (shortcut != null && shortcut.isEnabled) {
                    available { descriptor(shortcut, key, user, parent) }?.let { put(key, it) }
                } else if (!host) {
                    // Android still lets the previous Home app start its already pinned actions.
                    put(key, LauncherApp(key, label, parent.icon, badgedLabel(label, user)))
                }
            }
        }
    }

    fun launch(key: AppKey): Boolean = available {
        val service = launcher ?: return@available false
        val id = key.shortcutId ?: return@available false
        val user = users.getUserForSerialNumber(key.userSerial) ?: return@available false
        service.startShortcut(key.packageName, id, null, null, user)
        true
    } == true

    /** pinShortcuts replaces the whole publisher list. Check a newly selected action before and after. */
    fun replacePins(publisher: ShortcutPublisher, ids: Set<String>, newId: String? = null): Boolean = available {
        val service = launcher ?: return@available false
        if (!service.hasShortcutHostPermission()) return@available false
        val user = users.getUserForSerialNumber(publisher.userSerial) ?: return@available false
        val query = ShortcutQuery().setPackage(publisher.packageName)
            .setQueryFlags(PUBLISHED or ShortcutQuery.FLAG_MATCH_PINNED)
        val shortcuts = service.getShortcuts(query, user) ?: return@available false
        if (newId != null && shortcuts.none { it.id == newId && it.isEnabled && (it.isDynamic || it.isDeclaredInManifest) }) {
            return@available false
        }
        val previous = shortcuts.filter { it.isPinned }.mapTo(linkedSetOf()) { it.id }
        val desired = shortcuts.filter { it.id in ids && (it.isEnabled || it.isPinned) }.mapTo(linkedSetOf()) { it.id }
        if (previous != desired) service.pinShortcuts(publisher.packageName, desired.toList(), user)
        if (newId != null && service.getShortcuts(
                ShortcutQuery().setPackage(publisher.packageName).setShortcutIds(listOf(newId))
                    .setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED), user,
            ).orEmpty().none { it.id == newId && it.isEnabled && it.isPinned }
        ) {
            if (previous != desired) service.pinShortcuts(publisher.packageName, previous.toList(), user)
            return@available false
        }
        true
    } == true

    /** Only this launcher's pins are returned. Called at startup or when the Home role returns. */
    fun pinnedPublishers(): Set<ShortcutPublisher> = buildSet {
        val service = launcher ?: return@buildSet
        if (!canQuery()) return@buildSet
        val profiles = available { service.profiles }.orEmpty()
        for (user in profiles) {
            val serial = users.getSerialNumberForUser(user)
            val query = ShortcutQuery().setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED or ShortcutQuery.FLAG_GET_KEY_FIELDS_ONLY)
            for (shortcut in available { service.getShortcuts(query, user) }.orEmpty()) {
                add(ShortcutPublisher(shortcut.`package`, serial))
            }
        }
    }

    private fun descriptor(info: ShortcutInfo, key: AppKey, user: UserHandle, parent: LauncherApp): LauncherApp {
        val label = info.shortLabel?.toString()?.takeUnless(String::isBlank) ?: parent.label
        val icon = launcher?.getShortcutBadgedIconDrawable(info, context.resources.displayMetrics.densityDpi) ?: parent.icon
        return LauncherApp(key, label, icon, badgedLabel(label, user))
    }

    private fun badgedLabel(label: String, user: UserHandle): String? =
        context.packageManager.getUserBadgedLabel(label, user).toString().takeUnless { it == label }

    private inline fun <T> available(block: () -> T): T? = try {
        block()
    } catch (_: SecurityException) {
        null // No longer Home or the profile is not accessible.
    } catch (_: IllegalStateException) {
        null // The user or work profile is locked.
    } catch (_: IllegalArgumentException) {
        null // The publisher ceased to be eligible while we were acting.
    } catch (_: ActivityNotFoundException) {
        null // The publisher removed or disabled the shortcut.
    }

    private companion object {
        const val PUBLISHED = ShortcutQuery.FLAG_MATCH_DYNAMIC or ShortcutQuery.FLAG_MATCH_MANIFEST
    }
}
