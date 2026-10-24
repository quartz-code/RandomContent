package app.pulse.desktop

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * Loudness of [BANDS] frequency bands, 0..1, from the samples the player is playing. On Android
 * this needs the microphone permission (Visualizer); here the player decodes the sound itself.
 */
class AudioLevels {
    companion object {
        const val BANDS = 24
        private const val SIZE = 1024
    }

    private val window = FloatArray(SIZE) { (0.5 - 0.5 * cos(2 * PI * it / (SIZE - 1))).toFloat() }
    private val mono = FloatArray(SIZE)
    private var filled = 0
    private val target = FloatArray(BANDS)
    private val re = FloatArray(SIZE)
    private val im = FloatArray(SIZE)

    /** Interleaved stereo from the audio thread. */
    fun feed(samples: ShortArray, count: Int) {
        var i = 0
        while (i + 1 < count) {
            mono[filled++] = (samples[i] + samples[i + 1]) / 65536f
            if (filled == SIZE) {
                analyse()
                filled = 0
            }
            i += 2
        }
    }

    fun reset() {
        filled = 0
        synchronized(target) { target.fill(0f) }
    }

    /** Copies the latest levels into [out]. */
    fun read(out: FloatArray) = synchronized(target) { target.copyInto(out) }

    private fun analyse() {
        for (k in 0 until SIZE) {
            re[k] = mono[k] * window[k]
            im[k] = 0f
        }
        fft(re, im)
        val bins = SIZE / 2
        val first = 1
        val last = (bins * 0.45f).toInt()
        val levels = FloatArray(BANDS)
        for (b in 0 until BANDS) {
            val from = (first * (last.toFloat() / first).pow(b.toFloat() / BANDS)).toInt().coerceAtLeast(first)
            val to = (first * (last.toFloat() / first).pow((b + 1).toFloat() / BANDS)).toInt().coerceAtLeast(from + 1)
            var peak = 0f
            for (k in from until to.coerceAtMost(bins)) peak = max(peak, hypot(re[k], im[k]))
            // Highs are quieter in every song; lift them so the right side moves too.
            val lift = 1f + 2f * b / BANDS
            levels[b] = (ln(1f + peak * lift * 4f) / ln(1f + 60f)).coerceIn(0f, 1f)
        }
        synchronized(target) { levels.copyInto(target) }
    }

    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        var len = 2
        while (len <= n) {
            val angle = -2 * PI / len
            val wr = cos(angle).toFloat()
            val wi = sin(angle).toFloat()
            var i = 0
            while (i < n) {
                var cr = 1f
                var ci = 0f
                for (k in 0 until len / 2) {
                    val ur = re[i + k]
                    val ui = im[i + k]
                    val vr = re[i + k + len / 2] * cr - im[i + k + len / 2] * ci
                    val vi = re[i + k + len / 2] * ci + im[i + k + len / 2] * cr
                    re[i + k] = ur + vr
                    im[i + k] = ui + vi
                    re[i + k + len / 2] = ur - vr
                    im[i + k + len / 2] = ui - vi
                    val nr = cr * wr - ci * wi
                    ci = cr * wi + ci * wr
                    cr = nr
                }
                i += len
            }
            len = len shl 1
        }
    }
}
