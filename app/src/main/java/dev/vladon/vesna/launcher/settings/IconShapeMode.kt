package dev.vladon.vesna.launcher.settings

/** The mask for adaptive app icons; stored by stable name, not ordinal. [SYSTEM] is the device's own mask. */
internal enum class IconShapeMode(val preferenceValue: String) {
    SYSTEM("system"),
    ROUNDED_SQUARE("rounded_square"),
}
