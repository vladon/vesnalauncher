package dev.vladon.vesna.launcher.apps

/**
 * Stable identifier of a launchable target: package name plus activity class name
 * within an Android profile. [userSerial] is the serial number of the owning user
 * (see UserManager); `0` is always the main profile, which lets most call sites
 * omit it.
 *
 * [shortcutId] identifies an app-published shortcut (action) instead of a plain
 * activity: a key with a non-null [shortcutId] is one saved action of the
 * publisher app, while [className] then only records the component metadata the
 * action was pinned with. Two keys that differ only in [shortcutId] are distinct
 * targets — the same package and profile can hold both the ordinary app pin and
 * any number of its actions.
 */
data class AppKey(
    val packageName: String,
    val className: String,
    val userSerial: Long = 0,
    val shortcutId: String? = null,
)
