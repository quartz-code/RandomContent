package com.metrolist.music.ui.screens.pulse

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * Pulse: how "My pulse" looks, on the phone and on the computer alike: plain Compose, nothing of
 * Android here, so the desktop build compiles this very file.
 *
 * How it looks: A dark page lit from inside: soft lights drift behind everything,
 * in the chosen palette until a song plays and then, if the listener likes, in the colours of its
 * artwork; in the middle a living "heart" that breathes when idle and beats with the music while
 * the pulse plays. Everything that moves reads time and loudness in the draw phase, so the page
 * itself never recomposes to the beat.
 */

internal val PulseInk = Color(0xFF111017)
internal val PulseSheet = Color(0xFF1A1822)

/** The chosen palette as colours: [first] and [second] for the heart and buttons, [lights] behind. */
data class PulseTint(val first: Color, val second: Color, val lights: List<Color>) {
    val gradient: Brush = Brush.linearGradient(listOf(first, second))

    companion object {
        fun of(palette: PulseLook.Palette) = PulseTint(
            Color(palette.first),
            Color(palette.second),
            listOf(Color(palette.first), Color(palette.fourth), Color(palette.second), Color(palette.third)),
        )
    }
}

internal val LocalPulseTint = compositionLocalOf { PulseTint.of(PulseLook.Palette.PULSE) }

/** Seconds of motion; runs faster while the pulse plays, at the listener's [speed]. */
@Composable
internal fun rememberPulseClock(fast: Boolean, speed: Float = 1f): () -> Float {
    var seconds by remember { mutableFloatStateOf(0f) }
    val rate by rememberUpdatedState((if (fast) 1f else 0.35f) * speed)
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) seconds += (now - last) / 1_000_000_000f * rate
                last = now
            }
        }
    }
    return remember { { seconds } }
}

/** Artwork colours are often muddy; the lights need them bright enough to glow. */
internal fun glowing(color: Color): Color {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val delta = max - minOf(r, g, b)
    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta).mod(6f))
        max == g -> 60f * ((b - r) / delta + 2f)
        else -> 60f * ((r - g) / delta + 4f)
    }
    val saturation = (if (max == 0f) 0f else delta / max).coerceIn(0.45f, 0.9f)
    return Color.hsv(hue, saturation, max.coerceAtLeast(0.8f), color.alpha)
}

/**
 * What lies behind the page. [energy] (0..1) swells the lights with the music; [artwork] draws
 * the playing song's artwork for [PulseLook.Background.ARTWORK], or nothing.
 */
@Composable
internal fun PulseAura(
    look: PulseLook,
    artworkColors: List<Color>,
    time: () -> Float,
    energy: () -> Float,
    modifier: Modifier = Modifier,
    artwork: (@Composable () -> Unit)? = null,
    /** How dark the artwork is kept; more where it cannot be blurred (Android before 12). */
    artworkShade: Float = 0.55f,
) {
    val tint = LocalPulseTint.current
    val source = if (look.artworkLights && artworkColors.size >= 2) artworkColors.map(::glowing) else tint.lights
    val lights = tint.lights.indices.map { i ->
        animateColorAsState(source[i % source.size], tween(1200), label = "light$i").value
    }
    Box(modifier) {
        if (look.background == PulseLook.Background.ARTWORK && artwork != null) {
            artwork()
            Canvas(Modifier.fillMaxSize()) { drawRect(PulseInk.copy(alpha = artworkShade)) }
        } else {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(PulseInk)
                if (look.background == PulseLook.Background.CALM) drawStill(lights) else drawLights(lights, time(), energy())
            }
        }
        // The lower part darkens again, so text and buttons stay readable over anything.
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.5f to Color.Transparent, 0.82f to PulseInk.copy(alpha = 0.85f), 1f to PulseInk))
            drawRect(Brush.verticalGradient(0f to PulseInk.copy(alpha = 0.55f), 0.18f to Color.Transparent))
        }
    }
}

private fun DrawScope.drawLights(lights: List<Color>, t: Float, e: Float) {
    val w = size.width
    val h = size.height
    lights.forEachIndexed { i, color ->
        val center = Offset(
            w * (0.5f + 0.42f * sin(t * (0.11f + 0.03f * i) + i * 1.9f)),
            h * (0.36f + 0.24f * cos(t * (0.09f + 0.02f * i) + i * 2.3f)),
        )
        val radius = w * (0.62f + 0.08f * sin(t * 0.4f + i) + 0.18f * e)
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = 0.42f), color.copy(alpha = 0.12f), Color.Transparent), center, radius),
            radius,
            center,
        )
    }
}

/** One glow behind the heart and a second one low on the side: light, but nothing moves. */
private fun DrawScope.drawStill(lights: List<Color>) {
    val heart = Offset(size.width / 2, size.height * 0.36f)
    drawCircle(Brush.radialGradient(listOf(lights[0].copy(alpha = 0.34f), Color.Transparent), heart, size.width * 0.8f), size.width * 0.8f, heart)
    val low = Offset(size.width * 0.15f, size.height * 0.7f)
    drawCircle(Brush.radialGradient(listOf(lights[1].copy(alpha = 0.22f), Color.Transparent), low, size.width * 0.6f), size.width * 0.6f, low)
}

/** How far the edge of [shape] is from its middle at angle [a], as a part of the radius. */
internal fun heartEdge(shape: PulseLook.Shape, a: Float, t: Float, mid: Float): Float = when (shape) {
    PulseLook.Shape.DROP -> 0.028f * sin(3 * a + t * 1.1f) + 0.018f * sin(2 * a - t * 0.9f) + 0.010f * sin(5 * a + t * 1.6f) + 0.035f * mid * sin(6 * a + t * 3.1f)
    PulseLook.Shape.CIRCLE -> 0.012f * mid * sin(8 * a + t * 3f)
    // Petals that turn slowly; the music ruffles their tips.
    PulseLook.Shape.FLOWER -> 0.075f * cos(8 * (a + t * 0.08f)) + 0.03f * mid * sin(16 * a + t * 3f)
    // Five points: where |sin| touches zero the edge peaks sharply, between them it dips round.
    PulseLook.Shape.STAR -> 0.3f * (1f - abs(sin(2.5f * (a + t * 0.06f)))).let { it * it * 1.6f } - 0.1f + 0.03f * mid * sin(10 * a + t * 3f)
}

/**
 * The heart of the page in the chosen [shape], with echoes spreading out like the logo's rings
 * when [echoes]. [levels] are the player's frequency bands, 0..1, low to high.
 */
@Composable
internal fun PulseHeart(
    shape: PulseLook.Shape,
    echoes: Boolean,
    time: () -> Float,
    levels: () -> FloatArray,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val tint = LocalPulseTint.current
    val path = remember { Path() }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val t = time()
            val bands = levels()
            val bass = bands.take(6).average().toFloat()
            val mid = bands.drop(6).take(10).average().toFloat()
            val c = Offset(size.width / 2, size.height / 2)
            val base = size.minDimension * 0.31f
            val middle = lerp(tint.first, tint.second, 0.4f)

            drawCircle(
                Brush.radialGradient(listOf(tint.first.copy(alpha = 0.55f + 0.3f * bass), tint.second.copy(alpha = 0.18f), Color.Transparent), c, base * 1.9f),
                base * 1.9f,
                c,
            )
            // Echoes: one leaves the heart every beat and fades as it grows.
            if (echoes) {
                for (i in 0 until 3) {
                    val p = (t * 0.42f + i / 3f) % 1f
                    val r = base * (1.08f + 0.95f * p)
                    drawCircle(
                        Brush.linearGradient(listOf(tint.first, tint.second), Offset(c.x - r, c.y + r), Offset(c.x + r, c.y - r)),
                        r,
                        c,
                        alpha = (1f - p) * (1f - p) * 0.8f,
                        style = Stroke(width = base * (0.06f - 0.04f * p)),
                    )
                }
            }

            path.reset()
            val points = 160
            for (k in 0..points) {
                val a = (k.toFloat() / points) * 2f * PI.toFloat()
                // Breathes even in silence, a little; swells on the bass.
                val r = base * (1f + heartEdge(shape, a, t, mid) + 0.11f * bass + 0.02f * sin(t * 2.4f))
                val x = c.x + r * cos(a)
                val y = c.y + r * sin(a)
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            val turn = t * 0.5f
            val reach = base * 1.2f
            drawPath(
                path,
                Brush.linearGradient(
                    listOf(tint.first, middle, tint.second),
                    Offset(c.x + reach * cos(turn), c.y + reach * sin(turn)),
                    Offset(c.x - reach * cos(turn), c.y - reach * sin(turn)),
                ),
            )
            // A soft highlight on top gives the shape some volume.
            val shine = Offset(c.x - base * 0.35f, c.y - base * 0.4f)
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent), shine, base * 0.9f), base * 0.9f, shine)
        }
        Box(
            Modifier
                .fillMaxSize(0.62f)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** A still picture of [shape] for the settings, in the palette's gradient. */
@Composable
internal fun HeartPreview(shape: PulseLook.Shape, modifier: Modifier = Modifier) {
    val tint = LocalPulseTint.current
    val path = remember { Path() }
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val base = size.minDimension * 0.4f
        path.reset()
        for (k in 0..120) {
            val a = (k / 120f) * 2f * PI.toFloat()
            val r = base * (1f + heartEdge(shape, a, 1.3f, 0f))
            if (k == 0) path.moveTo(c.x + r * cos(a), c.y + r * sin(a)) else path.lineTo(c.x + r * cos(a), c.y + r * sin(a))
        }
        path.close()
        drawPath(path, tint.gradient)
    }
}

/** A round frosted button with a caption under it, for like, "not for me" and next. */
@Composable
internal fun PulseAction(icon: Painter, label: String, onClick: () -> Unit, highlighted: Boolean = false) {
    val tint = LocalPulseTint.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(60.dp)
                .clip(CircleShape)
                .glass(CircleShape, if (highlighted) tint.gradient else null)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = if (highlighted) PulseInk else Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** Frosted glass over the lights: a faint fill and a hairline edge; [selected] fills it instead. */
internal fun Modifier.glass(shape: Shape, selected: Brush? = null): Modifier =
    if (selected != null) {
        background(selected, shape)
    } else {
        background(Color.White.copy(alpha = 0.08f), shape).border(1.dp, Color.White.copy(alpha = 0.12f), shape)
    }
