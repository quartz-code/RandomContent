package com.metrolist.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
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

// Pulse look borrowed from the "Shortwave" radio-operator game: a dark operator's desk, cream
// paper, the amber carrier wave and the red tuning needle, with a condensed poster face for
// headings and a typewriter mono for labels.

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
    onSurfaceVariant = Color(0xFFB5B09F),
    surfaceTint = Amber,
    inverseSurface = Paper,
    inverseOnSurface = Ink,
    error = Color(0xFFFF6B57),
    onError = Ink,
    errorContainer = Color(0xFF5C1A10),
    onErrorContainer = Color(0xFFFFDAD2),
    outline = Color(0xFF7F8277),
    outlineVariant = Color(0xFF3E4137),
    scrim = Color.Black,
    surfaceBright = Color(0xFF34372F),
    surfaceDim = Color(0xFF121310),
    surfaceContainerLowest = Color(0xFF0C0D0B),
    surfaceContainerLow = Color(0xFF171914),
    surfaceContainer = Color(0xFF1A1C18),
    surfaceContainerHigh = Color(0xFF22251F),
    surfaceContainerHighest = Color(0xFF2B2E27),
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
    onSurfaceVariant = Color(0xFF5A564A),
    surfaceTint = Color(0xFF8F5A0E),
    inverseSurface = Color(0xFF1A1C18),
    inverseOnSurface = Paper,
    outline = Color(0xFF7F7A6A),
    outlineVariant = Color(0xFFC9BFA8),
    surfaceBright = Color(0xFFF5F0E2),
    surfaceDim = Color(0xFFDCD1B8),
    surfaceContainerLowest = Color(0xFFF8F4E9),
    surfaceContainerLow = Color(0xFFF5F0E2),
    surfaceContainer = Color(0xFFEAE2CD),
    surfaceContainerHigh = Color(0xFFE4DAC4),
    surfaceContainerHighest = Color(0xFFDCD1B8),
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

private fun TextStyle.mono() = copy(fontFamily = PtMono, fontWeight = FontWeight.Normal, letterSpacing = 0.04.em)

val ShortwaveTypography = Typography().run {
    copy(
        displayLarge = displayLarge.poster(FontWeight.Bold),
        displayMedium = displayMedium.poster(FontWeight.Bold),
        displaySmall = displaySmall.poster(FontWeight.Bold),
        headlineLarge = headlineLarge.poster(FontWeight.SemiBold),
        headlineMedium = headlineMedium.poster(FontWeight.SemiBold),
        headlineSmall = headlineSmall.poster(FontWeight.SemiBold),
        titleLarge = titleLarge.poster(FontWeight.Medium),
        labelLarge = labelLarge.mono(),
        labelMedium = labelMedium.mono(),
        labelSmall = labelSmall.mono(),
    )
}

// Equipment panels have small, tight radii rather than soft pills.
val ShortwaveShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(14.dp),
)
