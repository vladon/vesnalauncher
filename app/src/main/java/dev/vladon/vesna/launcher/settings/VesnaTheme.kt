package dev.vladon.vesna.launcher.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.vladon.vesna.launcher.R

// Typography stays fixed; the per-palette color schemes live in LauncherPalette.
private val Inter = FontFamily(Font(R.font.inter_regular), Font(R.font.inter_medium, FontWeight.Medium))
private val Nunito = FontFamily(Font(R.font.nunito_regular))
private val VesnaTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Nunito, letterSpacing = (-0.2).sp),
        displayMedium = displayMedium.copy(fontFamily = Nunito),
        displaySmall = displaySmall.copy(fontFamily = Nunito),
        headlineLarge = headlineLarge.copy(fontFamily = Nunito),
        headlineMedium = headlineMedium.copy(fontFamily = Nunito),
        headlineSmall = headlineSmall.copy(fontFamily = Nunito),
        titleLarge = titleLarge.copy(fontFamily = Inter),
        titleMedium = titleMedium.copy(fontFamily = Inter, letterSpacing = 0.2.sp),
        titleSmall = titleSmall.copy(fontFamily = Inter),
        bodyLarge = bodyLarge.copy(fontFamily = Inter),
        bodyMedium = bodyMedium.copy(fontFamily = Inter, letterSpacing = 0.2.sp),
        bodySmall = bodySmall.copy(fontFamily = Inter),
        labelLarge = labelLarge.copy(fontFamily = Inter),
        labelMedium = labelMedium.copy(fontFamily = Inter),
        labelSmall = labelSmall.copy(fontFamily = Inter),
    )
}

@Composable
internal fun VesnaTheme(
    dark: Boolean,
    palette: LauncherPalette = LauncherPalette.VESNA,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (dark) palette.darkScheme else palette.lightScheme,
        typography = VesnaTypography,
        content = content,
    )
}
