package dev.vladon.vesna.launcher.apps

/**
 * Stable identifier of a launchable activity: package name plus activity class name
 * within an Android profile. [userSerial] is the serial number of the owning user
 * (see UserManager); `0` is always the main profile, which lets most call sites
 * omit it.
 */
data class AppKey(
    val packageName: String,
    val className: String,
    val userSerial: Long = 0,
)
