package dev.vladon.vesna.launcher.settings

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * The fixed drawer backgrounds (#158) must keep the launcher's text readable in both themes:
 * 4.5:1 against the Vesna palette's surface text roles, which is what the drawer draws on top.
 */
class DrawerBackgroundContrastTest {
    private fun Color.relativeLuminance(): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    }

    private fun ratio(foreground: Color, backgroundArgb: Int): Double {
        val a = foreground.relativeLuminance()
        val b = Color(backgroundArgb).relativeLuminance()
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    @Test
    fun `fixed drawer backgrounds keep text contrast in both themes`() {
        val vesna = LauncherPalette.VESNA
        val light = listOf(vesna.lightScheme.onSurface, vesna.lightScheme.onSurfaceVariant)
        val dark = listOf(vesna.darkScheme.onSurface, vesna.darkScheme.onSurfaceVariant)
        for (mode in DrawerBackgroundMode.entries) {
            if (mode == DrawerBackgroundMode.THEME) continue
            for (text in light) {
                val value = ratio(text, mode.lightArgb)
                assertTrue(
                    "${mode.preferenceValue} light is $value:1 against argb ${mode.lightArgb.toHexString()}, needs >= 4.5:1",
                    value >= 4.5,
                )
            }
            for (text in dark) {
                val value = ratio(text, mode.darkArgb)
                assertTrue(
                    "${mode.preferenceValue} dark is $value:1 against argb ${mode.darkArgb.toHexString()}, needs >= 4.5:1",
                    value >= 4.5,
                )
            }
        }
    }

    private fun Int.toHexString(): String = "0x%08X".format(this)
}
