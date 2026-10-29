package app.pulse.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The artwork's main colour, for themes that take their colours from it. Android uses Palette;
 * here a small average weighted towards saturated pixels gives a close enough "vibrant" colour.
 */
@Composable
fun rememberArtworkColor(url: String?): Color? {
    val color by produceState<Color?>(null, url) {
        value = url?.let { loadColor(it) }
    }
    return color
}

private suspend fun loadColor(url: String): Color? = withContext(Dispatchers.IO) {
    runCatching {
        val context = PlatformContext.INSTANCE
        val request = ImageRequest.Builder(context).data(url).size(48).build()
        val bitmap = SingletonImageLoader.get(context).execute(request).image?.toBitmap() ?: return@runCatching null
        var r = 0.0
        var g = 0.0
        var b = 0.0
        var weight = 0.0
        val step = maxOf(1, bitmap.width / 24)
        for (y in 0 until bitmap.height step step) for (x in 0 until bitmap.width step step) {
            val c = bitmap.getColor(x, y)
            val cr = (c shr 16 and 0xFF) / 255.0
            val cg = (c shr 8 and 0xFF) / 255.0
            val cb = (c and 0xFF) / 255.0
            val mx = maxOf(cr, cg, cb)
            val mn = minOf(cr, cg, cb)
            val w = 0.05 + (mx - mn) * (1 - kotlin.math.abs(mx + mn - 1))
            r += cr * w; g += cg * w; b += cb * w; weight += w
        }
        if (weight == 0.0) null else Color((r / weight).toFloat(), (g / weight).toFloat(), (b / weight).toFloat())
    }.getOrNull()
}
