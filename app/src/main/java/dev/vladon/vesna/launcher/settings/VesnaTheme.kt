package dev.vladon.vesna.launcher.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.vladon.vesna.launcher.R

// Fixed roles from design/system/tokens.json (not wallpaper-derived dynamic color).
private val VesnaLightScheme = lightColorScheme(
    primary = Color(0xFF49672E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCAEEA6),
    onPrimaryContainer = Color(0xFF324E18),
    inversePrimary = Color(0xFFAFD18C),
    secondary = Color(0xFF57624A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBE7C9),
    onSecondaryContainer = Color(0xFF3F4A34),
    tertiary = Color(0xFF386664),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBBECE9),
    onTertiaryContainer = Color(0xFF1E4E4C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF9FAEF),
    onBackground = Color(0xFF1A1D16),
    surface = Color(0xFFF9FAEF),
    onSurface = Color(0xFF1A1D16),
    onSurfaceVariant = Color(0xFF44483E),
    surfaceVariant = Color(0xFFE0E4D6),
    surfaceDim = Color(0xFFD9DBD0),
    surfaceBright = Color(0xFFF9FAEF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F5E9),
    surfaceContainer = Color(0xFFEDEFE4),
    surfaceContainerHigh = Color(0xFFE8E9DE),
    surfaceContainerHighest = Color(0xFFE2E3D9),
    surfaceTint = Color(0xFF49672E),
    outline = Color(0xFF74796D),
    outlineVariant = Color(0xFFC4C8BA),
    inverseSurface = Color(0xFF2F312A),
    inverseOnSurface = Color(0xFFF0F2E7),
    scrim = Color.Black,
)

private val VesnaDarkScheme = darkColorScheme(
    primary = Color(0xFFAFD18C),
    onPrimary = Color(0xFF1D3703),
    primaryContainer = Color(0xFF324E18),
    onPrimaryContainer = Color(0xFFCAEEA6),
    inversePrimary = Color(0xFF49672E),
    secondary = Color(0xFFBFCBAE),
    onSecondary = Color(0xFF29341F),
    secondaryContainer = Color(0xFF3F4A34),
    onSecondaryContainer = Color(0xFFDBE7C9),
    tertiary = Color(0xFFA0CFCD),
    onTertiary = Color(0xFF003736),
    tertiaryContainer = Color(0xFF1E4E4C),
    onTertiaryContainer = Color(0xFFBBECE9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF11140E),
    onBackground = Color(0xFFE2E3D9),
    surface = Color(0xFF11140E),
    onSurface = Color(0xFFE2E3D9),
    onSurfaceVariant = Color(0xFFC4C8BA),
    surfaceVariant = Color(0xFF44483E),
    surfaceDim = Color(0xFF11140E),
    surfaceBright = Color(0xFF373A33),
    surfaceContainerLowest = Color(0xFF0C0F09),
    surfaceContainerLow = Color(0xFF1A1D16),
    surfaceContainer = Color(0xFF1E211A),
    surfaceContainerHigh = Color(0xFF282B24),
    surfaceContainerHighest = Color(0xFF33362E),
    surfaceTint = Color(0xFFAFD18C),
    outline = Color(0xFF8E9286),
    outlineVariant = Color(0xFF44483E),
    inverseSurface = Color(0xFFE2E3D9),
    inverseOnSurface = Color(0xFF2F312A),
    scrim = Color.Black,
)

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
internal fun VesnaTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) VesnaDarkScheme else VesnaLightScheme,
        typography = VesnaTypography,
        content = content,
    )
}
