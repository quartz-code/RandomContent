package com.metrolist.music.ui.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.ui.theme.PlayerColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pulse: the artwork's colours for screens outside the player (lock screen, recaps), the same
 * way the player takes them. Empty until the image is loaded or when [url] is null.
 */
@Composable
fun rememberArtworkColors(url: String?): List<Color> {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.surface.toArgb()
    val colors by produceState(emptyList<Color>(), url) {
        if (url == null) {
            value = emptyList()
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(100, 100)
                .allowHardware(false)
                .build()
            val bitmap = runCatching { context.imageLoader.execute(request) }.getOrNull()?.image?.toBitmap()
                ?: return@withContext emptyList()
            val palette = Palette.from(bitmap).maximumColorCount(8).resizeBitmapArea(100 * 100).generate()
            PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallback)
        }
    }
    return colors
}
