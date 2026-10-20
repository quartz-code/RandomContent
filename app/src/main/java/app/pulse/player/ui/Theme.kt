package app.pulse.player.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Accent = Color(0xFFFF5C7A)

private val Dark = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Color(0xFFB4A7FF),
    background = Color(0xFF0E0E12),
    surface = Color(0xFF0E0E12),
    surfaceVariant = Color(0xFF1E1E26),
    surfaceContainer = Color(0xFF17171D),
    surfaceContainerHigh = Color(0xFF1E1E26),
    onSurfaceVariant = Color(0xFFA8A8B8),
)

private val Light = lightColorScheme(
    primary = Color(0xFFD9365A),
    secondary = Color(0xFF6650D8),
)

@Composable
fun PulseTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context).copy(background = Color(0xFF0E0E12), surface = Color(0xFF0E0E12))
            else dynamicLightColorScheme(context)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
