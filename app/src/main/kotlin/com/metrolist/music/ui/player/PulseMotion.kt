package com.metrolist.music.ui.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/*
 * Pulse: what moves behind the player. FLOW needs nothing; BARS and WAVE listen to the player
 * through android.media.audiofx.Visualizer, which Android only allows with the microphone
 * permission (nothing is recorded). Without it they move to a made-up beat instead.
 */

internal const val PulseBands = 24

internal fun Context.canVisualize(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/**
 * Loudness of [PulseBands] frequency bands, 0..1, low to high. Runs a frame loop only while
 * [active], so a closed or paused player costs nothing.
 */
@Composable
internal fun rememberAudioLevels(audioSessionId: Int, active: Boolean): () -> FloatArray {
    val context = LocalContext.current
    val target = remember { FloatArray(PulseBands) }
    val current = remember { FloatArray(PulseBands) }
    val frame = remember { mutableLongStateOf(0L) }
    var listening by remember { mutableStateOf(false) }

    DisposableEffect(audioSessionId, active) {
        val visualizer = if (active && audioSessionId > 0 && context.canVisualize()) {
            runCatching {
                Visualizer(audioSessionId).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
                    setDataCaptureListener(
                        object : Visualizer.OnDataCaptureListener {
                            override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, rate: Int) = Unit

                            override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, rate: Int) {
                                if (fft != null) fftToBands(fft, target)
                            }
                        },
                        Visualizer.getMaxCaptureRate() / 2,
                        false,
                        true,
                    )
                    enabled = true
                }
            }.getOrNull()
        } else {
            null
        }
        listening = visualizer != null
        onDispose {
            runCatching {
                visualizer?.enabled = false
                visualizer?.release()
            }
        }
    }

    LaunchedEffect(active, listening) {
        if (!active) {
            target.fill(0f)
        }
        var lastBeat = 0L
        var settled = false
        while (!settled) {
            withFrameNanos { now ->
                if (active && !listening && now - lastBeat > 140_000_000L) {
                    lastBeat = now
                    madeUpBeat(target)
                }
                var moving = false
                for (i in 0 until PulseBands) {
                    // Rise fast, fall slowly: reads as music rather than noise.
                    val speed = if (target[i] > current[i]) 0.45f else 0.12f
                    current[i] += (target[i] - current[i]) * speed
                    if (current[i] > 0.002f) moving = true
                }
                frame.longValue = now
                settled = !active && !moving
            }
        }
    }
    // Reading the frame counter makes whoever draws the levels redraw on the next frame.
    return remember { { current.also { frame.longValue } } }
}

/** FFT bytes come as (re, im) pairs after DC and Nyquist; bands are spaced like hearing. */
internal fun fftToBands(fft: ByteArray, out: FloatArray) {
    val bins = fft.size / 2
    val first = 1
    val last = (bins * 0.45f).toInt().coerceAtLeast(first + out.size)
    for (b in out.indices) {
        val from = (first * (last.toFloat() / first).pow(b.toFloat() / out.size)).toInt().coerceAtLeast(first)
        val to = (first * (last.toFloat() / first).pow((b + 1).toFloat() / out.size)).toInt().coerceAtLeast(from + 1)
        var peak = 0f
        for (k in from until to.coerceAtMost(bins)) {
            val re = fft[2 * k].toFloat()
            val im = fft[2 * k + 1].toFloat()
            peak = max(peak, hypot(re, im))
        }
        // Highs are quieter in every song; lift them so the right side moves too.
        val lift = 1f + b.toFloat() / out.size
        out[b] = (ln(1f + peak * lift) / ln(1f + 96f)).coerceIn(0f, 1f)
    }
}

private fun madeUpBeat(out: FloatArray) {
    val kick = Random.nextFloat()
    for (i in out.indices) {
        val bass = 1f - i.toFloat() / out.size
        out[i] = (0.15f + 0.55f * bass * kick + 0.35f * Random.nextFloat()).coerceIn(0f, 1f)
    }
}

/**
 * Drawn over [PulsePlayerBackground]. [levels] is read inside the draw phase only, so the player
 * itself never recomposes to the beat.
 */
@Composable
internal fun PulseMotionLayer(
    theme: PlayerTheme,
    artworkColors: List<Color>,
    accent: Color,
    levels: () -> FloatArray,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    when (theme.motion) {
        PlayerTheme.Motion.NONE -> Unit
        PlayerTheme.Motion.FLOW -> FlowLayer(theme, artworkColors, accent, active, modifier)
        PlayerTheme.Motion.BARS -> Canvas(modifier) { drawBars(levels(), accent, theme.light) }
        PlayerTheme.Motion.WAVE -> {
            val time = rememberMotionTime(active)
            Canvas(modifier) { drawWaves(levels(), accent, time(), theme.light) }
        }
    }
}

/** Seconds since start, ticking only while [active]; the picture freezes on pause. */
@Composable
private fun rememberMotionTime(active: Boolean): () -> Float {
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) seconds += (now - last) / 1_000_000_000f
                last = now
            }
        }
    }
    return remember { { seconds } }
}

@Composable
private fun FlowLayer(theme: PlayerTheme, artworkColors: List<Color>, accent: Color, active: Boolean, modifier: Modifier) {
    val time = rememberMotionTime(active)
    val colors = remember(artworkColors, accent, theme.color1, theme.color2) {
        // The accent goes first: artwork colours alone are often too close to the background to see.
        val picked = (listOf(accent) + artworkColors + Color(theme.color1) + Color(theme.color2)).distinct()
        List(3) { picked[it % picked.size] }
    }
    val strength = if (theme.light) 0.45f else 0.8f
    Canvas(modifier) {
        val t = time()
        val r = size.maxDimension * 0.55f
        colors.forEachIndexed { i, color ->
            // Slow, out-of-step loops, so the picture never visibly repeats.
            val speed = 0.07f + i * 0.023f
            val phase = i * 2.1f
            val center = Offset(
                size.width * (0.5f + 0.38f * sin(t * speed * 2 * PI.toFloat() + phase)),
                size.height * (0.35f + 0.3f * cos(t * speed * 1.3f * PI.toFloat() + phase * 1.7f)),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = strength), color.copy(alpha = 0f)),
                    center = center,
                    radius = r,
                ),
                radius = r,
                center = center,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBars(levels: FloatArray, accent: Color, light: Boolean) {
    val gap = size.width * 0.012f
    val barWidth = (size.width - gap * (levels.size + 1)) / levels.size
    // Kept low and faint: the bars sit behind the buttons and must not compete with them.
    val maxHeight = size.height * 0.3f
    val alpha = if (light) 0.16f else 0.2f
    levels.forEachIndexed { i, level ->
        val h = (maxHeight * level).coerceAtLeast(barWidth * 0.4f)
        val left = gap + i * (barWidth + gap)
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(accent.copy(alpha = alpha), accent.copy(alpha = 0f)),
                startY = size.height - h,
                endY = size.height,
            ),
            topLeft = Offset(left, size.height - h),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaves(levels: FloatArray, accent: Color, t: Float, light: Boolean) {
    fun band(from: Int, to: Int): Float {
        var sum = 0f
        for (i in from until to) sum += levels[i]
        return sum / (to - from)
    }
    val bass = band(0, PulseBands / 4)
    val mids = band(PulseBands / 4, PulseBands * 2 / 3)
    val highs = band(PulseBands * 2 / 3, PulseBands)
    val layers = listOf(
        Triple(bass, 1.2f, 0.62f),
        Triple(mids, 2.1f, 0.7f),
        Triple(highs, 3.4f, 0.78f),
    )
    val base = if (light) 0.16f else 0.22f
    layers.forEachIndexed { i, (level, waves, height) ->
        val y0 = size.height * height
        val amp = size.height * (0.015f + 0.07f * level)
        val path = Path().apply {
            moveTo(0f, size.height)
            val steps = 48
            for (s in 0..steps) {
                val x = size.width * s / steps
                val y = y0 + amp * sin(x / size.width * waves * 2 * PI.toFloat() + t * (0.6f + i * 0.35f) + i)
                lineTo(x, y)
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(
            path,
            Brush.verticalGradient(
                listOf(accent.copy(alpha = base - i * 0.04f), accent.copy(alpha = 0f)),
                startY = y0 - amp,
                endY = size.height,
            ),
        )
    }
}
