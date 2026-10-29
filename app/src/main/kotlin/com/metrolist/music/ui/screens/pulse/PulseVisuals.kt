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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Pulse: how "My pulse" looks. A dark page lit from inside: soft lights drift behind everything,
 * in the logo's colours until a song plays and then in the colours of its artwork; in the middle
 * a living "heart" that breathes when idle and beats with the music while the pulse plays.
 * Everything that moves reads time and loudness in the draw phase, so the page itself never
 * recomposes to the beat.
 */

/** The logo's two colours: the pulse runs from coral to amber. */
internal val PulseCoral = Color(0xFFFF4F7B)
internal val PulseAmber = Color(0xFFFFA24C)
internal val PulseInk = Color(0xFF111017)
internal val PulseSheet = Color(0xFF1A1822)
private val PulseBerry = Color(0xFFC2306E)
private val PulseViolet = Color(0xFF5B3FD9)
private val LogoLights = listOf(PulseCoral, PulseViolet, PulseAmber, PulseBerry)

internal val PulseGradient = Brush.linearGradient(listOf(PulseCoral, PulseAmber))

/** Seconds of motion; runs [fast] while the pulse plays and slowly otherwise. */
@Composable
internal fun rememberPulseClock(fast: Boolean): () -> Float {
    var seconds by remember { mutableFloatStateOf(0f) }
    val speed by rememberUpdatedState(if (fast) 1f else 0.35f)
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) seconds += (now - last) / 1_000_000_000f * speed
                last = now
            }
        }
    }
    return remember { { seconds } }
}

/** Artwork colours are often muddy; the lights need them bright enough to glow. */
private fun glowing(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[1] = hsv[1].coerceIn(0.45f, 0.9f)
    hsv[2] = hsv[2].coerceAtLeast(0.8f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** The drifting lights behind the page. [energy] (0..1) swells them with the music. */
@Composable
internal fun PulseAura(colors: List<Color>, time: () -> Float, energy: () -> Float, modifier: Modifier = Modifier) {
    val source = if (colors.size >= 2) colors.map(::glowing) else LogoLights
    val lights = LogoLights.indices.map { i ->
        animateColorAsState(source[i % source.size], tween(1200), label = "light$i").value
    }
    Canvas(modifier) {
        drawRect(PulseInk)
        val t = time()
        val e = energy()
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
        // The lower part darkens again, so text and buttons stay readable over any artwork.
        drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.5f to Color.Transparent, 0.82f to PulseInk.copy(alpha = 0.85f), 1f to PulseInk))
        drawRect(Brush.verticalGradient(0f to PulseInk.copy(alpha = 0.55f), 0.18f to Color.Transparent))
    }
}

/**
 * The heart of the page: an organic shape that is never quite round, with echoes spreading out
 * like the logo's rings. [levels] are the player's frequency bands, 0..1, low to high.
 */
@Composable
internal fun PulseHeart(
    time: () -> Float,
    levels: () -> FloatArray,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val path = remember { Path() }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val t = time()
            val bands = levels()
            val bass = bands.take(6).average().toFloat()
            val mid = bands.drop(6).take(10).average().toFloat()
            val c = Offset(size.width / 2, size.height / 2)
            val base = size.minDimension * 0.31f

            drawCircle(
                Brush.radialGradient(listOf(PulseCoral.copy(alpha = 0.55f + 0.3f * bass), PulseAmber.copy(alpha = 0.18f), Color.Transparent), c, base * 1.9f),
                base * 1.9f,
                c,
            )
            // Echoes: one leaves the heart every beat and fades as it grows.
            for (i in 0 until 3) {
                val p = (t * 0.42f + i / 3f) % 1f
                val r = base * (1.08f + 0.95f * p)
                drawCircle(
                    Brush.linearGradient(listOf(PulseCoral, PulseAmber), Offset(c.x - r, c.y + r), Offset(c.x + r, c.y - r)),
                    r,
                    c,
                    alpha = (1f - p) * (1f - p) * 0.8f,
                    style = Stroke(width = base * (0.06f - 0.04f * p)),
                )
            }

            path.reset()
            val points = 120
            for (k in 0..points) {
                val a = (k.toFloat() / points) * 2f * PI.toFloat()
                val wobble = 0.028f * sin(3 * a + t * 1.1f) +
                    0.018f * sin(2 * a - t * 0.9f) +
                    0.010f * sin(5 * a + t * 1.6f) +
                    0.035f * mid * sin(6 * a + t * 3.1f)
                // Breathes even in silence, a little; swells on the bass.
                val r = base * (1f + wobble + 0.11f * bass + 0.02f * sin(t * 2.4f))
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
                    listOf(PulseCoral, Color(0xFFFF6F6A), PulseAmber),
                    Offset(c.x + reach * cos(turn), c.y + reach * sin(turn)),
                    Offset(c.x - reach * cos(turn), c.y - reach * sin(turn)),
                ),
            )
            // A soft highlight on top gives the shape some volume.
            drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent), Offset(c.x - base * 0.35f, c.y - base * 0.4f), base * 0.9f),
                base * 0.9f,
                Offset(c.x - base * 0.35f, c.y - base * 0.4f),
            )
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

/** A round frosted button with a caption under it, for like, "not for me" and next. */
@Composable
internal fun PulseAction(icon: Int, label: String, onClick: () -> Unit, highlighted: Boolean = false, tint: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(60.dp)
                .clip(CircleShape)
                .then(if (highlighted) Modifier.background(PulseGradient) else Modifier.glass(CircleShape))
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), label, tint = if (highlighted) PulseInk else tint, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** Frosted glass over the lights: a faint fill and a hairline edge. */
internal fun Modifier.glass(shape: androidx.compose.ui.graphics.Shape, selected: Boolean = false): Modifier =
    if (selected) {
        background(PulseGradient, shape)
    } else {
        background(Color.White.copy(alpha = 0.08f), shape).border(1.dp, Color.White.copy(alpha = 0.12f), shape)
    }
