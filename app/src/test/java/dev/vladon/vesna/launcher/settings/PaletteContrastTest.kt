package dev.vladon.vesna.launcher.settings

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Every curated palette must keep readable text and controls in both themes: 4.5:1 for the
 * text and icon pairs, 3:1 for outlines against the surface. Values come from
 * design/system/tokens.json (see the palette section recorded for #155).
 */
class PaletteContrastTest {
    private fun Color.relativeLuminance(): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    }

    private fun ratio(foreground: Color, background: Color): Double {
        val a = foreground.relativeLuminance()
        val b = background.relativeLuminance()
        val lighter = maxOf(a, b)
        val darker = minOf(a, b)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun pairs(scheme: androidx.compose.material3.ColorScheme): List<Pair<String, Pair<Color, Color>>> = listOf(
        "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
        "onPrimaryContainer/primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
        "onSecondary/secondary" to (scheme.onSecondary to scheme.secondary),
        "onSecondaryContainer/secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
        "onTertiary/tertiary" to (scheme.onTertiary to scheme.tertiary),
        "onTertiaryContainer/tertiaryContainer" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
        "onSurface/surface" to (scheme.onSurface to scheme.surface),
        "onBackground/background" to (scheme.onBackground to scheme.background),
        "onSurfaceVariant/surfaceVariant" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
    )

    @Test
    fun `all palettes keep contrast-checked pairs in both themes`() {
        for (palette in LauncherPalette.entries) {
            for ((label, scheme) in listOf("light" to palette.lightScheme, "dark" to palette.darkScheme)) {
                for ((name, pair) in pairs(scheme)) {
                    val value = ratio(pair.first, pair.second)
                    assertTrue(
                        "${palette.preferenceValue} $label $name is $value:1, needs >= 4.5:1",
                        value >= 4.5,
                    )
                }
                val outline = ratio(scheme.outline, scheme.surface)
                assertTrue(
                    "${palette.preferenceValue} $label outline is $outline:1, needs >= 3:1",
                    outline >= 3.0,
                )
            }
        }
    }
}
