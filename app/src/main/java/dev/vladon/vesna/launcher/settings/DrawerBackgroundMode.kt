package dev.vladon.vesna.launcher.settings

/**
 * The drawer-only background choices; stored by stable name, not ordinal. [THEME] keeps the
 * palette surface. The fixed backgrounds follow the same tone ladder as the palettes
 * (#155): light tone 98 and dark tone 6 with a fixed hue, contrast-checked against the
 * launcher's text colors by DrawerBackgroundContrastTest.
 */
internal enum class DrawerBackgroundMode(val preferenceValue: String, val lightArgb: Int, val darkArgb: Int) {
    THEME("theme", 0, 0),
    ROSE("rose", 0xFFFEF7FD.toInt(), 0xFF171116.toInt()),
    SAGE("sage", 0xFFF5FCF4.toInt(), 0xFF101510.toInt()),
}
