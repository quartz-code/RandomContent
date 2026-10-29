package app.pulse.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metrolist.music.ui.player.PlayerTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Pulse desktop: the phone's player theme, drawn with desktop Compose. The rules follow the
 * Android PulsePlayer.kt (colours, background, shapes), so a theme code from the phone looks the
 * same here; Android-only pieces (Visualizer, Material shapes, resources) are redone in plain Compose.
 */

private val SoftInk = Color(0xFF1D1B20)
private val SoftPaper = Color(0xFFF7F5F2)
private val CleanBottom = Color(0xFF101215)
private val CleanFallbackTop = Color(0xFF3E4A5E)

data class Palette(
    val content: Color,
    val muted: Color,
    val accent: Color,
    val like: Color,
    val onPlay: Color,
    val panel: Color,
    val track: Color,
)

val PlayerTheme.contentColor: Color get() = textColor?.let(::Color) ?: if (light) SoftInk else Color.White

val PlayerTheme.baseBackground: Color
    get() = when {
        light -> SoftPaper
        background == PlayerTheme.Background.SOLID -> Color(color1)
        background == PlayerTheme.Background.GRADIENT -> Color(color2)
        background == PlayerTheme.Background.BLUR -> Color.Black
        else -> CleanBottom
    }

@Composable
fun palette(theme: PlayerTheme, artwork: Color?): Palette {
    val target = if (theme.accentFromArtwork && artwork != null) softTone(artwork, if (theme.light) 0.42f else 0.7f, 0.32f) else Color(theme.accent)
    val accent by animateColorAsState(target, tween(800))
    val content = theme.contentColor
    return Palette(
        content = content,
        muted = content.copy(alpha = if (theme.light) 0.55f else 0.6f),
        accent = accent,
        like = theme.likeColor?.let(::Color) ?: accent,
        onPlay = if (accent.luminance() > 0.55f) (if (theme.light) SoftInk else CleanBottom) else Color.White,
        panel = content.copy(alpha = if (theme.light) 0.06f else 0.08f),
        track = content.copy(alpha = if (theme.light) 0.14f else 0.22f),
    )
}

/** Keeps the colour's hue but moves it to the given lightness (Android's ColorUtils, by hand). */
fun softTone(color: Color, lightness: Float, minSaturation: Float, maxSaturation: Float = 0.75f): Color {
    val r = color.red
    val g = color.green
    val b = color.blue
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val l = (mx + mn) / 2
    val d = mx - mn
    var h = 0f
    var s = 0f
    if (d > 1e-6f) {
        s = d / (1 - kotlin.math.abs(2 * l - 1))
        h = when (mx) {
            r -> ((g - b) / d).mod(6f)
            g -> (b - r) / d + 2
            else -> (r - g) / d + 4
        } * 60f
    }
    return Color.hsl(h.mod(360f), s.coerceIn(minSaturation, maxSaturation), lightness)
}

@Composable
fun ThemeBackground(theme: PlayerTheme, artwork: Color?, artworkUrl: String?, modifier: Modifier = Modifier) {
    val paper = if (theme.light) SoftPaper else CleanBottom
    when (theme.background) {
        PlayerTheme.Background.ARTWORK -> {
            val target = if (theme.light) {
                artwork?.let { softTone(it, 0.9f, 0.2f, 0.45f) } ?: Color(0xFFE6EAF0)
            } else {
                lerp(artwork ?: CleanFallbackTop, Color.Black, 0.35f)
            }
            val top by animateColorAsState(target, tween(800))
            Box(modifier.fillMaxSize().background(Brush.verticalGradient(0f to top, 0.72f to paper)))
        }
        PlayerTheme.Background.BLUR -> Box(modifier.fillMaxSize().background(paper)) {
            if (artworkUrl != null) {
                AsyncImage(artworkUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(80.dp))
            }
            Box(Modifier.fillMaxSize().background(if (theme.light) Color.White.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.35f)))
        }
        PlayerTheme.Background.SOLID -> Box(modifier.fillMaxSize().background(Color(theme.color1)))
        PlayerTheme.Background.GRADIENT -> Box(modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(theme.color1), 0.6f to Color(theme.color2))))
    }
}

/** Flowing colours, or bars / a wave that follow the music, behind the player. */
@Composable
fun ThemeMotion(theme: PlayerTheme, accent: Color, artwork: Color?, levels: AudioLevels, playing: Boolean, modifier: Modifier = Modifier) {
    if (theme.motion == PlayerTheme.Motion.NONE) return
    var time by remember { mutableFloatStateOf(0f) }
    var tick by remember { mutableFloatStateOf(0f) }
    val shown = remember { FloatArray(AudioLevels.BANDS) }
    val fresh = remember { FloatArray(AudioLevels.BANDS) }
    LaunchedEffect(playing) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L && playing) time += (now - last) / 1e9f
                last = now
                levels.read(fresh)
                for (i in shown.indices) {
                    val goal = if (playing) fresh[i] else 0f
                    shown[i] += (goal - shown[i]) * if (goal > shown[i]) 0.45f else 0.12f
                }
                tick = now.toFloat()
            }
        }
    }
    Canvas(modifier.fillMaxSize()) {
        @Suppress("UNUSED_EXPRESSION") tick
        when (theme.motion) {
            PlayerTheme.Motion.FLOW -> {
                val colors = listOf(accent, artwork ?: Color(theme.color1), Color(theme.color2))
                val r = size.maxDimension * 0.55f
                val strength = if (theme.light) 0.45f else 0.8f
                colors.forEachIndexed { i, c ->
                    val speed = 0.07f + i * 0.023f
                    val phase = i * 2.1f
                    val center = Offset(
                        size.width * (0.5f + 0.38f * sin(time * speed * 2 * PI.toFloat() + phase)),
                        size.height * (0.35f + 0.3f * cos(time * speed * 1.3f * PI.toFloat() + phase * 1.7f)),
                    )
                    drawCircle(Brush.radialGradient(listOf(c.copy(alpha = strength), c.copy(alpha = 0f)), center, r), r, center)
                }
            }
            PlayerTheme.Motion.BARS -> {
                val gap = size.width * 0.012f
                val w = (size.width - gap * (shown.size + 1)) / shown.size
                val maxH = size.height * 0.3f
                val alpha = if (theme.light) 0.16f else 0.2f
                shown.forEachIndexed { i, level ->
                    val h = (maxH * level).coerceAtLeast(w * 0.4f)
                    drawRoundRect(
                        Brush.verticalGradient(listOf(accent.copy(alpha = alpha), accent.copy(alpha = 0f)), size.height - h, size.height),
                        Offset(gap + i * (w + gap), size.height - h), Size(w, h), CornerRadius(w / 2, w / 2),
                    )
                }
            }
            PlayerTheme.Motion.WAVE -> {
                fun band(a: Int, b: Int) = (a until b).sumOf { shown[it].toDouble() }.toFloat() / (b - a)
                val bands = listOf(band(0, 6), band(6, 16), band(16, 24))
                val base = if (theme.light) 0.16f else 0.22f
                bands.forEachIndexed { i, level ->
                    val y0 = size.height * (0.62f + i * 0.08f)
                    val amp = size.height * (0.015f + 0.07f * level)
                    val waves = 1.2f + i * 1.1f
                    val path = Path().apply {
                        moveTo(0f, size.height)
                        for (s in 0..48) {
                            val x = size.width * s / 48
                            lineTo(x, y0 + amp * sin(x / size.width * waves * 2 * PI.toFloat() + time * (0.6f + i * 0.35f) + i))
                        }
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(path, Brush.verticalGradient(listOf(accent.copy(alpha = base - i * 0.04f), accent.copy(alpha = 0f)), y0 - amp, size.height))
                }
            }
            PlayerTheme.Motion.NONE -> Unit
        }
    }
}

/** The artwork's outline. Material shapes are Android-only here, so the flowers are drawn by hand. */
fun PlayerTheme.artShapeFor(size: Dp): Shape = when (artShape) {
    PlayerTheme.ArtShape.ROUNDED -> if (artRound) CircleShape else RoundedCornerShape((artCorner * size.value / 320f).dp)
    PlayerTheme.ArtShape.ARCH -> RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomStartPercent = 6, bottomEndPercent = 6)
    PlayerTheme.ArtShape.COOKIE -> wavyCircle(lobes = 9, depth = 0.07f)
    PlayerTheme.ArtShape.CLOVER -> wavyCircle(lobes = 4, depth = 0.2f)
    PlayerTheme.ArtShape.SUNNY -> wavyCircle(lobes = 8, depth = 0.05f, sharp = true)
}

private fun wavyCircle(lobes: Int, depth: Float, sharp: Boolean = false) = GenericShape { size, _ ->
    val cx = size.width / 2
    val cy = size.height / 2
    val r = min(cx, cy)
    val steps = 240
    for (i in 0..steps) {
        val a = 2 * PI * i / steps
        val wave = if (sharp) kotlin.math.abs(cos(lobes * a / 2)).toFloat() else ((1 + cos(lobes * a)) / 2).toFloat()
        val rr = r * (1 - depth + depth * wave)
        val x = cx + rr * cos(a).toFloat()
        val y = cy + rr * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

fun PlayerTheme.headline(size: TextUnit, oswald: FontFamily, mono: FontFamily): TextStyle = when (font) {
    PlayerTheme.Font.SYSTEM -> TextStyle(fontWeight = FontWeight.Bold, fontSize = size)
    PlayerTheme.Font.OSWALD -> TextStyle(fontFamily = oswald, fontWeight = FontWeight.Medium, fontSize = size)
    PlayerTheme.Font.MONO -> TextStyle(fontFamily = mono, fontSize = size)
}
