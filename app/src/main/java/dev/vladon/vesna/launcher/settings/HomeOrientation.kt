package dev.vladon.vesna.launcher.settings

/**
 * The Home screen orientation choices; stored by stable name, not ordinal.
 * [SYSTEM] follows the device rotation exactly as before the setting existed;
 * [PORTRAIT] keeps the launcher window (workspace, dock, page controls and the
 * drawer inside it) portrait regardless of the device.
 */
internal enum class HomeOrientation(val preferenceValue: String) {
    SYSTEM("system"),
    PORTRAIT("portrait"),
}
