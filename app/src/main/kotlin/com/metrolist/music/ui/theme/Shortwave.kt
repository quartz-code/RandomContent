package com.metrolist.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.metrolist.music.R
import com.metrolist.music.constants.AppStyle

// Pulse styles. "Radio" is borrowed from the Shortwave radio-operator game: a dark operator's
// desk, cream paper, the amber carrier wave and the red tuning needle, with a condensed poster
// face for headings and a typewriter mono for small labels. "Terminal" is a green phosphor CRT.

private val Ink = Color(0xFF1B1A16)
private val Paper = Color(0xFFEFE8D6)
private val Amber = Color(0xFFF0A43A)
private val Needle = Color(0xFFE0482F)

val ShortwaveDarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Ink,
    primaryContainer = Color(0xFF4A3417),
    onPrimaryContainer = Color(0xFFFFD9A8),
    inversePrimary = Color(0xFF8A5A12),
    secondary = Color(0xFFB9BFA5),
    onSecondary = Color(0xFF23261C),
    secondaryContainer = Color(0xFF3A3F30),
    onSecondaryContainer = Color(0xFFDCE2C6),
    tertiary = Needle,
    onTertiary = Color(0xFFFFF4EE),
    tertiaryContainer = Color(0xFF5C1A10),
    onTertiaryContainer = Color(0xFFFFDAD2),
    background = Color(0xFF121310),
    onBackground = Paper,
    surface = Color(0xFF121310),
    onSurface = Paper,
    surfaceVariant = Color(0xFF2A2C25),
    onSurfaceVariant = Color(0xFFBDB8A6),
    surfaceTint = Amber,
    inverseSurface = Paper,
    inverseOnSurface = Ink,
    error = Color(0xFFFF6B57),
    onError = Ink,
    errorContainer = Color(0xFF5C1A10),
    onErrorContainer = Color(0xFFFFDAD2),
    outline = Color(0xFF85887C),
    outlineVariant = Color(0xFF3E4137),
    scrim = Color.Black,
    surfaceBright = Color(0xFF34372F),
    surfaceDim = Color(0xFF121310),
    surfaceContainerLowest = Color(0xFF0C0D0B),
    surfaceContainerLow = Color(0xFF171914),
    surfaceContainer = Color(0xFF1C1E19),
    surfaceContainerHigh = Color(0xFF242720),
    surfaceContainerHighest = Color(0xFF2D3029),
)

val ShortwaveLightColors = lightColorScheme(
    primary = Color(0xFF8F5A0E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6C47A),
    onPrimaryContainer = Color(0xFF2E1C00),
    inversePrimary = Amber,
    secondary = Color(0xFF555B45),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6DCC0),
    onSecondaryContainer = Color(0xFF1A1E10),
    tertiary = Color(0xFFB5321D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD2),
    onTertiaryContainer = Color(0xFF3D0700),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE0D7C0),
    onSurfaceVariant = Color(0xFF55513F),
    surfaceTint = Color(0xFF8F5A0E),
    inverseSurface = Color(0xFF1A1C18),
    inverseOnSurface = Paper,
    outline = Color(0xFF7A7565),
    outlineVariant = Color(0xFFC9BFA8),
    surfaceBright = Color(0xFFF5F0E2),
    surfaceDim = Color(0xFFDCD1B8),
    surfaceContainerLowest = Color(0xFFF8F4E9),
    surfaceContainerLow = Color(0xFFF5F0E2),
    surfaceContainer = Color(0xFFEAE2CD),
    surfaceContainerHigh = Color(0xFFE4DAC4),
    surfaceContainerHighest = Color(0xFFDCD1B8),
)

private val Phosphor = Color(0xFF6BF59A)
private val PhosphorText = Color(0xFFCFF7DA)

val TerminalDarkColors = darkColorScheme(
    primary = Phosphor,
    onPrimary = Color(0xFF00210E),
    primaryContainer = Color(0xFF0F3D22),
    onPrimaryContainer = Color(0xFFA6FFC3),
    inversePrimary = Color(0xFF1E7A3C),
    secondary = Color(0xFF9CC9A8),
    onSecondary = Color(0xFF0B2414),
    secondaryContainer = Color(0xFF1C3524),
    onSecondaryContainer = Color(0xFFC6EBD0),
    tertiary = Color(0xFFFFD34D),
    onTertiary = Color(0xFF2A2000),
    tertiaryContainer = Color(0xFF3F3300),
    onTertiaryContainer = Color(0xFFFFE89A),
    background = Color(0xFF070B08),
    onBackground = PhosphorText,
    surface = Color(0xFF070B08),
    onSurface = PhosphorText,
    surfaceVariant = Color(0xFF16231A),
    onSurfaceVariant = Color(0xFF8FB89A),
    surfaceTint = Phosphor,
    inverseSurface = PhosphorText,
    inverseOnSurface = Color(0xFF07130B),
    error = Color(0xFFFF6E6E),
    onError = Color(0xFF2A0000),
    errorContainer = Color(0xFF4A1010),
    onErrorContainer = Color(0xFFFFD6D6),
    outline = Color(0xFF5E8A6B),
    outlineVariant = Color(0xFF203A28),
    scrim = Color.Black,
    surfaceBright = Color(0xFF223328),
    surfaceDim = Color(0xFF070B08),
    surfaceContainerLowest = Color(0xFF040705),
    surfaceContainerLow = Color(0xFF0B120D),
    surfaceContainer = Color(0xFF0F1912),
    surfaceContainerHigh = Color(0xFF152219),
    surfaceContainerHighest = Color(0xFF1B2B20),
)

val TerminalLightColors = lightColorScheme(
    primary = Color(0xFF1E7A3C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F2C6),
    onPrimaryContainer = Color(0xFF00210E),
    inversePrimary = Phosphor,
    secondary = Color(0xFF4C6653),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE8D4),
    onSecondaryContainer = Color(0xFF0A2013),
    tertiary = Color(0xFF7A5E00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF261A00),
    background = Color(0xFFEEF4EE),
    onBackground = Color(0xFF0F2414),
    surface = Color(0xFFEEF4EE),
    onSurface = Color(0xFF0F2414),
    surfaceVariant = Color(0xFFDAE6DC),
    onSurfaceVariant = Color(0xFF3F5546),
    surfaceTint = Color(0xFF1E7A3C),
    inverseSurface = Color(0xFF0F1912),
    inverseOnSurface = PhosphorText,
    outline = Color(0xFF6F8575),
    outlineVariant = Color(0xFFBFD0C3),
    surfaceBright = Color(0xFFF6FAF6),
    surfaceDim = Color(0xFFD6E2D8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F8F3),
    surfaceContainer = Color(0xFFE6EFE7),
    surfaceContainerHigh = Color(0xFFDFEAE1),
    surfaceContainerHighest = Color(0xFFD6E2D8),
)

private val Oswald = FontFamily(
    Font(R.font.oswald, FontWeight.Normal),
    Font(R.font.oswald, FontWeight.Medium),
    Font(R.font.oswald, FontWeight.SemiBold),
    Font(R.font.oswald, FontWeight.Bold),
)

private val PtMono = FontFamily(Font(R.font.pt_mono, FontWeight.Normal))

private fun TextStyle.poster(weight: FontWeight) =
    copy(fontFamily = Oswald, fontWeight = weight, letterSpacing = 0.01.em)

private fun TextStyle.mono(spacing: Double = 0.02) =
    copy(fontFamily = PtMono, fontWeight = FontWeight.Normal, letterSpacing = spacing.em)

// Body text, buttons and navigation labels stay in the regular face so long lists remain easy
// to read; only headings and the smallest labels carry the radio look.
private val RadioTypography = Typography().run {
    copy(
        displayLarge = displayLarge.poster(FontWeight.Bold),
        displayMedium = displayMedium.poster(FontWeight.Bold),
        displaySmall = displaySmall.poster(FontWeight.Bold),
        headlineLarge = headlineLarge.poster(FontWeight.SemiBold),
        headlineMedium = headlineMedium.poster(FontWeight.SemiBold),
        headlineSmall = headlineSmall.poster(FontWeight.SemiBold),
        titleLarge = titleLarge.poster(FontWeight.Medium),
        labelSmall = labelSmall.mono(),
    )
}

private val TerminalTypography = Typography().run {
    copy(
        displayLarge = displayLarge.mono(0.0),
        displayMedium = displayMedium.mono(0.0),
        displaySmall = displaySmall.mono(0.0),
        headlineLarge = headlineLarge.mono(0.0),
        headlineMedium = headlineMedium.mono(0.0),
        headlineSmall = headlineSmall.mono(0.0),
        titleLarge = titleLarge.mono(0.0),
        titleMedium = titleMedium.mono(0.0),
        titleSmall = titleSmall.mono(0.0),
        bodyLarge = bodyLarge.mono(0.0),
        bodyMedium = bodyMedium.mono(0.0),
        bodySmall = bodySmall.mono(0.0),
        labelLarge = labelLarge.mono(),
        labelMedium = labelMedium.mono(),
        labelSmall = labelSmall.mono(),
    )
}

// Equipment panels have small, tight radii rather than soft pills.
private val RadioShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

private val TerminalShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(6.dp),
)

/** The style's own palette, or null when it follows the wallpaper / chosen seed color. */
fun AppStyle.colorScheme(dark: Boolean): ColorScheme? = when (this) {
    AppStyle.RADIO -> if (dark) ShortwaveDarkColors else ShortwaveLightColors
    AppStyle.TERMINAL -> if (dark) TerminalDarkColors else TerminalLightColors
    AppStyle.CLASSIC -> null
}

val AppStyle.typography: Typography
    get() = when (this) {
        AppStyle.RADIO -> RadioTypography
        AppStyle.TERMINAL -> TerminalTypography
        AppStyle.CLASSIC -> Typography()
    }

val AppStyle.shapes: Shapes
    get() = when (this) {
        AppStyle.RADIO -> RadioShapes
        AppStyle.TERMINAL -> TerminalShapes
        AppStyle.CLASSIC -> Shapes()
    }
