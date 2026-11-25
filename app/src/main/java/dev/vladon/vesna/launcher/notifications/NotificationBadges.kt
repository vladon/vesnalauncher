package dev.vladon.vesna.launcher.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-wide state of the notification badge pipeline (#200): the set of
 * packages that currently hold at least one active notification, and the
 * user-facing on/off switch. The [VesnaNotificationListener] writes the
 * package set; the drawer, dock, and workspace tiles read it when they bind
 * and redraw when the flow reports a change.
 *
 * Only package names ever enter this state: the listener never reads
 * notification titles, texts, or extras, per the recorded #200 scope ("dots,
 * no texts"). Dots also stay presence-only — one dot per package regardless
 * of how many notifications it holds.
 */
object NotificationBadges {

    private const val PREF_ENABLED = "notification_dots_enabled"
    private const val OWN_PACKAGE_SKIP = "dev.vladon.vesna.launcher"

    private lateinit var prefs: SharedPreferences

    private val _packages = MutableStateFlow(emptySet<String>())

    /** Packages with at least one active notification, excluding Vesna itself. */
    val packages: StateFlow<Set<String>> = _packages

    /**
     * Bumps on every visible-state change (package set or the on/off switch),
     * so a single collector can rebind tiles without polling.
     */
    private val _updates = MutableStateFlow(0L)
    val updates: StateFlow<Long> = _updates

    private fun bump() {
        _updates.value = _updates.value + 1
    }

    /** Whether tiles should draw dots at all; dots render only when also granted. */
    var enabled: Boolean
        get() = prefs.getBoolean(PREF_ENABLED, false)
        set(value) {
            prefs.edit { putBoolean(PREF_ENABLED, value) }
            bump()
        }

    /** Called once from [dev.vladon.vesna.launcher.VesnaApplication.onCreate]. */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(
            "vesna_launcher",
            Context.MODE_PRIVATE,
        )
    }

    /** Replaces the tracked package set from a listener rebuild. */
    fun setPackages(pkgs: Set<String>) {
        _packages.value = pkgs - OWN_PACKAGE_SKIP
        bump()
    }

    /** Whether the tile for [packageName] should currently draw its dot. */
    fun shows(packageName: String): Boolean = enabled && packageName in _packages.value

    /** Registers one posted notification's package (presence-only; no content). */
    fun addPackage(packageName: String) {
        if (packageName == OWN_PACKAGE_SKIP || packageName in _packages.value) return
        _packages.value = _packages.value + packageName
        bump()
    }

    /** The listener lost its grant (or the toggle demands silence): clear the set. */
    fun clear() {
        _packages.value = emptySet()
        bump()
    }
}
