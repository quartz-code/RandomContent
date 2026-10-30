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
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
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
import kotlin.math.exp
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

        /** The look's palette; for [PulseLook.Palette.CUSTOM] the listener's own two colours. */
        fun of(look: PulseLook): PulseTint {
            if (look.palette != PulseLook.Palette.CUSTOM) return of(look.palette)
            val first = Color(look.customFirst)
            val second = Color(look.customSecond)
            return PulseTint(first, second, listOf(first, lerp(second, PulseInk, 0.3f), second, lerp(first, PulseInk, 0.35f)))
        }

        /**
         * For a black-and-white cover: silver, as the cover is. Its greys have no hue, and
         * brightening them would make up a colour that is not on the cover.
         */
        internal val silver = PulseTint(
            Color(0xFFE8EBF2),
            Color(0xFF9EA8BC),
            listOf(Color(0xFFB8C2D6), Color(0xFF4A5468), Color(0xFF8C96AA), Color(0xFF5E6478)),
        )
    }
}

internal val LocalPulseTint = compositionLocalOf { PulseTint.of(PulseLook.Palette.PULSE) }

/**
 * The colours the page is drawn in: the chosen palette, or for [PulseLook.Palette.ARTWORK] the
 * playing song's artwork, brightened so the heart still glows and dark text still reads on it.
 * They change over a second when the song does, not at once.
 */
@Composable
internal fun rememberPulseTint(look: PulseLook, artworkColors: List<Color>): PulseTint {
    val target = remember(look.palette, look.customFirst, look.customSecond, artworkColors) {
        if (look.palette == PulseLook.Palette.ARTWORK && artworkColors.isNotEmpty()) fromArtwork(artworkColors) else PulseTint.of(look)
    }
    val first by animateColorAsState(target.first, tween(900), label = "first")
    val second by animateColorAsState(target.second, tween(900), label = "second")
    val lights = target.lights.mapIndexed { i, c -> animateColorAsState(c, tween(1200), label = "tint$i").value }
    return remember(first, second, lights) { PulseTint(first, second, lights) }
}

private fun fromArtwork(artworkColors: List<Color>): PulseTint {
    val colors = vivid(artworkColors)
    if (colors.isEmpty()) return PulseTint.silver
    val first = glowing(colors[0])
    // A one-colour cover would give a flat heart: the second colour is then the first, turned a little.
    val second = colors.drop(1).map(::glowing).firstOrNull { hueDistance(it, first) > 25f } ?: turned(first, 38f)
    val third = colors.getOrNull(2)?.let(::glowing) ?: turned(first, -55f)
    return PulseTint(first, second, listOf(first, lerp(third, PulseInk, 0.25f), second, lerp(first, PulseInk, 0.35f)))
}

/** Artwork colours with a hue to speak of; the greys and near-blacks of a cover are left out. */
internal fun vivid(colors: List<Color>): List<Color> = colors.filter { hsvOf(it).let { (_, saturation, value) -> saturation > 0.2f && value > 0.15f } }

private fun hsvOf(color: Color): FloatArray {
    val max = maxOf(color.red, color.green, color.blue)
    val delta = max - minOf(color.red, color.green, color.blue)
    val hue = when {
        delta == 0f -> 0f
        max == color.red -> 60f * (((color.green - color.blue) / delta).mod(6f))
        max == color.green -> 60f * ((color.blue - color.red) / delta + 2f)
        else -> 60f * ((color.red - color.green) / delta + 4f)
    }
    return floatArrayOf(hue, if (max == 0f) 0f else delta / max, max)
}

private fun hueDistance(a: Color, b: Color): Float {
    val d = abs(hsvOf(a)[0] - hsvOf(b)[0])
    return minOf(d, 360f - d)
}

private fun turned(color: Color, degrees: Float): Color {
    val (h, sat, v) = hsvOf(color).let { Triple(it[0], it[1], it[2]) }
    return Color.hsv((h + degrees).mod(360f), sat, v)
}

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
    val (hue, saturation, value) = hsvOf(color).let { Triple(it[0], it[1], it[2]) }
    return Color.hsv(hue, saturation.coerceIn(0.45f, 0.9f), value.coerceAtLeast(0.8f), color.alpha)
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
    val vivid = vivid(artworkColors).map(::glowing)
    val source = when {
        !look.artworkLights || vivid.isEmpty() -> tint.lights
        vivid.size == 1 -> listOf(vivid[0], turned(vivid[0], 38f))
        else -> vivid
    }
    val motion = remember { AuraMotion() }
    val lights = tint.lights.indices.map { i ->
        animateColorAsState(source[i % source.size], tween(1200), label = "light$i").value
    }
    val reaction = look.reaction.coerceIn(0f, 2f)
    Box(modifier) {
        if (look.background == PulseLook.Background.ARTWORK && artwork != null) {
            artwork()
            Canvas(Modifier.fillMaxSize()) { drawRect(PulseInk.copy(alpha = artworkShade)) }
        } else {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(PulseInk)
                val t = time()
                motion.step(t, (energy() * reaction).coerceAtMost(1.5f))
                val e = motion.energy
                when (look.background) {
                    PulseLook.Background.CALM -> drawStill(lights)
                    PulseLook.Background.PARTICLES -> {
                        drawStill(lights, dim = 0.6f)
                        drawParticles(lights, t, motion.travel, e)
                    }
                    PulseLook.Background.AURORA -> {
                        drawStill(lights, dim = 0.45f)
                        drawAurora(lights, t, e)
                    }
                    PulseLook.Background.WAVES -> {
                        drawStill(lights, dim = 0.6f)
                        drawWaves(lights, t, e)
                    }
                    else -> drawLights(lights, t, e)
                }
            }
        }
        // The lower part darkens again, so text and buttons stay readable over anything.
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.5f to Color.Transparent, 0.82f to PulseInk.copy(alpha = 0.85f), 1f to PulseInk))
            drawRect(Brush.verticalGradient(0f to PulseInk.copy(alpha = 0.55f), 0.18f to Color.Transparent))
        }
    }
}

/**
 * The music as the background follows it, kept from frame to frame: loudness eased, so nothing
 * flickers with every drum hit, and how far the sparks have flown. Their speed changes with the
 * music, but where they are is the sum of all speeds so far: a loud bar speeds them up rather
 * than throwing them along their path, which time × speed did.
 */
private class AuraMotion {
    private var last = Float.NaN
    var energy = 0f
        private set
    var travel = 0f
        private set

    fun step(t: Float, target: Float) {
        val dt = if (last.isNaN()) 0f else (t - last).coerceIn(0f, 0.1f)
        last = t
        // Rises with a beat in about a tenth of a second, falls back over half a second.
        energy += (target - energy) * (1f - exp(-dt / if (target > energy) 0.1f else 0.5f))
        travel += dt * (1f + 1.5f * energy)
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
private fun DrawScope.drawStill(lights: List<Color>, dim: Float = 1f) {
    val heart = Offset(size.width / 2, size.height * 0.36f)
    drawCircle(Brush.radialGradient(listOf(lights[0].copy(alpha = 0.34f * dim), Color.Transparent), heart, size.width * 0.8f), size.width * 0.8f, heart)
    val low = Offset(size.width * 0.15f, size.height * 0.7f)
    drawCircle(Brush.radialGradient(listOf(lights[1].copy(alpha = 0.22f * dim), Color.Transparent), low, size.width * 0.6f), size.width * 0.6f, low)
}

/** A stable "random" 0..1 for particle [i], so each spark keeps its own path frame to frame. */
private fun noise(i: Int, salt: Float): Float {
    val x = sin(i * 12.9898f + salt * 78.233f) * 43758.547f
    return x - kotlin.math.floor(x)
}

private fun DrawScope.drawParticles(lights: List<Color>, t: Float, travel: Float, e: Float) {
    val unit = size.minDimension / 400f
    for (i in 0 until 54) {
        val speed = 0.035f + 0.07f * noise(i, 1f)
        val p = (travel * speed + noise(i, 2f)) % 1f
        val x = size.width * noise(i, 3f) + sin(t * 0.7f + i) * size.width * 0.025f
        val y = size.height * (1.04f - 1.1f * p)
        val fade = sin(p * PI.toFloat())
        val r = unit * (1.2f + 2.8f * noise(i, 4f)) * (1f + 0.9f * e)
        val color = lights[i % lights.size]
        val c = Offset(x, y)
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f * fade), Color.Transparent), c, r * 5f), r * 5f, c)
        drawCircle(Color.White.copy(alpha = (0.35f + 0.5f * e).coerceAtMost(0.9f) * fade), r * 0.6f, c)
    }
}

private fun DrawScope.drawAurora(lights: List<Color>, t: Float, e: Float) {
    val path = Path()
    for (band in 0 until 3) {
        val color = lights[(band * 2) % lights.size]
        val top = size.height * (0.12f + 0.11f * band)
        val thick = size.height * (0.2f + 0.08f * e)
        path.reset()
        val steps = 48
        for (k in 0..steps) {
            val fx = k.toFloat() / steps
            val y = top + size.height * (0.06f * sin(fx * 6.28f * (0.8f + 0.3f * band) + t * (0.25f + 0.08f * band)) + 0.025f * sin(fx * 17f - t * 0.6f))
            if (k == 0) path.moveTo(0f, y) else path.lineTo(fx * size.width, y)
        }
        for (k in steps downTo 0) {
            val fx = k.toFloat() / steps
            val y = top + thick + size.height * 0.04f * sin(fx * 9f + t * 0.4f + band)
            path.lineTo(fx * size.width, y)
        }
        path.close()
        drawPath(path, Brush.verticalGradient(listOf(color.copy(alpha = 0.5f + 0.2f * e), color.copy(alpha = 0.12f), Color.Transparent), top - size.height * 0.05f, top + thick * 1.1f))
    }
}

private fun DrawScope.drawWaves(lights: List<Color>, t: Float, e: Float) {
    val path = Path()
    for (layer in 0 until 3) {
        val base = size.height * (0.58f + 0.08f * layer)
        val amp = size.height * 0.022f * (1f + 2.2f * e)
        path.reset()
        path.moveTo(0f, size.height)
        val steps = 60
        for (k in 0..steps) {
            val fx = k.toFloat() / steps
            path.lineTo(fx * size.width, base + amp * sin(fx * 6.28f * (1.1f + 0.45f * layer) + t * (0.9f + 0.35f * layer) + layer))
        }
        path.lineTo(size.width, size.height)
        path.close()
        val color = lights[(layer + 1) % lights.size]
        drawPath(path, Brush.verticalGradient(listOf(color.copy(alpha = 0.32f), color.copy(alpha = 0.06f)), base - amp, size.height))
    }
}

/**
 * A point on the edge of [shape] at [a] (0..2π), around the middle, in parts of the radius.
 * Round shapes are a radius per angle; the ♥ is the classic heart curve.
 */
internal fun shapePoint(shape: PulseLook.Shape, a: Float, t: Float, mid: Float): Offset {
    if (shape == PulseLook.Shape.HEART) {
        val sinA = sin(a)
        val x = 16f * sinA * sinA * sinA
        val y = -(13f * cos(a) - 5f * cos(2 * a) - 2f * cos(3 * a) - cos(4 * a))
        // The curve spans x ±16 and y −12…17; scaled to the circle's size and raised to sit centred.
        val wobble = 1f + 0.025f * mid * sin(9 * a + t * 3f)
        return Offset(x / 16.5f * wobble, (y - 2.5f) / 16.5f * wobble)
    }
    val r = 1f + heartEdge(shape, a, t, mid)
    return Offset(r * cos(a), r * sin(a))
}

/** How far the edge of a round [shape] is from its middle at angle [a], as a part of the radius. */
private fun heartEdge(shape: PulseLook.Shape, a: Float, t: Float, mid: Float): Float = when (shape) {
    PulseLook.Shape.DROP -> 0.028f * sin(3 * a + t * 1.1f) + 0.018f * sin(2 * a - t * 0.9f) + 0.010f * sin(5 * a + t * 1.6f) + 0.035f * mid * sin(6 * a + t * 3.1f)
    PulseLook.Shape.CIRCLE -> 0.012f * mid * sin(8 * a + t * 3f)
    // Petals that turn slowly; the music ruffles their tips.
    PulseLook.Shape.FLOWER -> 0.075f * cos(8 * (a + t * 0.08f)) + 0.03f * mid * sin(16 * a + t * 3f)
    // Five points: where |sin| touches zero the edge peaks sharply, between them it dips round.
    PulseLook.Shape.STAR -> 0.3f * (1f - abs(sin(2.5f * (a + t * 0.06f)))).let { it * it * 1.6f } - 0.1f + 0.03f * mid * sin(10 * a + t * 3f)
    // Ripples running round the rim, much deeper with the music: a circle that is a sound wave.
    PulseLook.Shape.WAVE -> 0.022f * sin(12 * a - t * 4f) + 0.07f * mid * sin(22 * a + t * 7f) + 0.03f * mid * sin(5 * a - t * 2f)
    PulseLook.Shape.HEART -> 0f
}

/**
 * The heart of the page in the chosen [shape], with echoes spreading out like the logo's rings
 * when [echoes]. [levels] are the player's frequency bands, 0..1, low to high.
 */
@Composable
internal fun PulseHeart(
    shape: PulseLook.Shape,
    echoes: Boolean,
    reaction: Float,
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
            val strength = reaction.coerceIn(0f, 2f)
            val bass = bands.take(6).average().toFloat() * strength
            val mid = bands.drop(6).take(10).average().toFloat() * strength
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
                val point = shapePoint(shape, a, t, mid) * (base * (1f + 0.11f * bass + 0.02f * sin(t * 2.4f)))
                if (k == 0) path.moveTo(c.x + point.x, c.y + point.y) else path.lineTo(c.x + point.x, c.y + point.y)
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
        // Not clipped to a circle: the artwork inside takes the heart's own shape (♥ included),
        // and the touch ripple spreads round without a box.
        Box(
            Modifier
                .fillMaxSize(0.62f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(bounded = false), role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/**
 * The ♥ as a clip, so the artwork inside the heart takes its shape instead of poking out of it as
 * a circle would. The curve fills the box it clips.
 */
internal val HeartOutline: Shape = GenericShape { size, _ ->
    for (k in 0..120) {
        val p = shapePoint(PulseLook.Shape.HEART, (k / 120f) * 2f * PI.toFloat(), 0f, 0f)
        val x = (p.x / 0.97f + 1f) / 2f * size.width
        val y = (p.y / 0.88f + 1f) / 2f * size.height
        if (k == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
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
            // A little music in the preview, so the wave shows its ripples.
            val point = shapePoint(shape, a, 1.3f, 0.5f) * base
            if (k == 0) path.moveTo(c.x + point.x, c.y + point.y) else path.lineTo(c.x + point.x, c.y + point.y)
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
