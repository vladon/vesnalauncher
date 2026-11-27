package dev.vladon.vesna.launcher.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-wide state of the notification badge pipeline: the packages that
 * currently hold active notifications (with per-package counts), the user-facing
 * master switch, badge style and size (full Nova parity surface; supersedes the
 * #200 dot-only scope per the 2026-11-26 full-copy directive).
 *
 * The [VesnaNotificationListener] writes the per-package counts from its own
 * notification-key tracking — notification titles, texts and extras are never
 * read (ADR-NOTIF-1 privacy surface preserved for content; counts are the
 * number of active notifications, which the listener already knows).
 *
 * The master switch defaults to ON like Nova's; dots render only once the
 * system notification-access grant is in place.
 */
object NotificationBadges {

    private const val PREF_ENABLED = "notification_dots_enabled"
    private const val PREF_STYLE = "notification_badge_style"
    private const val PREF_SIZE = "notification_badge_size"
    private const val OWN_PACKAGE_SKIP = "dev.vladon.vesna.launcher"

    private lateinit var prefs: SharedPreferences

    /** Badge rendering styles (full-copy surface). */
    enum class BadgeStyle(val preferenceValue: String) {
        NOTIFICATION_DOTS("dots"),
        NUMERIC_DOTS("numeric");

        companion object {
            const val DEFAULT_PREFERENCE_VALUE = "dots"

            fun fromPreference(value: String?): BadgeStyle =
                entries.firstOrNull { it.preferenceValue == value } ?: NOTIFICATION_DOTS
        }
    }

    /** Badge sizes with the dot draw-scale each applies. */
    enum class BadgeSize(val preferenceValue: String, val scale: Float) {
        SMALL("small", 0.8f),
        MEDIUM("medium", 1f),
        LARGE("large", 1.3f);

        companion object {
            const val DEFAULT_PREFERENCE_VALUE = "medium"

            fun fromPreference(value: String?): BadgeSize =
                entries.firstOrNull { it.preferenceValue == value } ?: MEDIUM
        }
    }

    private val _packages = MutableStateFlow(emptySet<String>())

    /** Packages with at least one active notification, excluding Vesna itself. */
    val packages: StateFlow<Set<String>> = _packages

    private val _counts = MutableStateFlow(emptyMap<String, Int>())

    /** Active-notification counts per package (presence + count, no content). */
    val counts: StateFlow<Map<String, Int>> = _counts

    /**
     * Bumps on every visible-state change (counts, style, size or the master
     * switch), so a single collector can rebind tiles without polling.
     */
    private val _updates = MutableStateFlow(0L)
    val updates: StateFlow<Long> = _updates

    private fun bump() {
        _updates.value = _updates.value + 1
    }

    /** Whether tiles should draw badges at all; default ON, gated on the grant. */
    var enabled: Boolean
        get() = prefs.getBoolean(PREF_ENABLED, true)
        set(value) {
            prefs.edit { putBoolean(PREF_ENABLED, value) }
            bump()
        }

    var style: BadgeStyle
        get() = BadgeStyle.fromPreference(prefs.getString(PREF_STYLE, null))
        set(value) {
            prefs.edit { putString(PREF_STYLE, value.preferenceValue) }
            bump()
        }

    var size: BadgeSize
        get() = BadgeSize.fromPreference(prefs.getString(PREF_SIZE, null))
        set(value) {
            prefs.edit { putString(PREF_SIZE, value.preferenceValue) }
            bump()
        }

    /** Called once from [dev.vladon.vesna.launcher.VesnaApplication.onCreate]. */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(
            "vesna_launcher",
            Context.MODE_PRIVATE,
        )
    }

    /** Replaces the tracked notification keys from a listener rebuild. */
    fun setNotifications(keysByPackage: Map<String, Set<String>>) {
        val filtered = keysByPackage - OWN_PACKAGE_SKIP
        _counts.value = filtered.mapValues { it.value.size }
        _packages.value = filtered.keys
        bump()
    }

    /** Adds one posted notification (key metadata only; no content). */
    fun addNotification(key: String, packageName: String) {
        if (packageName == OWN_PACKAGE_SKIP) return
        val perPackage = _counts.value.toMutableMap()
        perPackage[packageName] = (perPackage[packageName] ?: 0) + 1
        _counts.value = perPackage
        _packages.value = perPackage.keys
        bump()
    }

    /** Removes one notification (key metadata only); the count shrinks with it. */
    fun removeNotification(key: String, packageName: String) {
        val perPackage = _counts.value.toMutableMap()
        val left = (perPackage[packageName] ?: 0) - 1
        if (left <= 0) perPackage.remove(packageName) else perPackage[packageName] = left
        _counts.value = perPackage
        _packages.value = perPackage.keys
        bump()
    }

    /** The listener lost its grant (or the toggle demands silence): clear the set. */
    fun clear() {
        _counts.value = emptyMap()
        _packages.value = emptySet()
        bump()
    }

    /** Whether the tile for [packageName] should currently draw a badge. */
    fun shows(packageName: String): Boolean = enabled && packageName in _packages.value

    /** The count rendered for [packageName] in the numeric style. */
    fun count(packageName: String): Int = _counts.value[packageName] ?: 0
}
