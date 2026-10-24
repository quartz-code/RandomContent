package app.pulse.desktop

import java.io.File
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicLong
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread

/**
 * Pulse desktop: plays one downloaded file. FFmpeg (inside the app, no system install) turns any
 * YouTube format into 48 kHz 16-bit stereo, which goes to the system's sound device through Java
 * Sound (WASAPI/DirectSound on Windows, ALSA/PulseAudio/PipeWire on Linux).
 */
class AudioEngine {
    private val lock = Object()
    @Volatile private var session: Session? = null

    /** 0..1, applied to the samples so it works the same on every sound device. */
    @Volatile var volume: Float = 1f

    /** Interleaved 16-bit samples just played, for the bars and wave behind the player. */
    @Volatile var onSamples: ((ShortArray, Int) -> Unit)? = null

    val positionMs: Long get() = session?.positionMs() ?: 0L
    val durationMs: Long get() = session?.durationMs ?: 0L
    val isPlaying: Boolean get() = session?.let { !it.paused && !it.finished } ?: false

    fun play(file: File, startMs: Long, onEnded: () -> Unit, onError: (Throwable) -> Unit) {
        stop()
        val s = Session(file, startMs, onEnded, onError)
        session = s
        s.start()
    }

    fun pause() = session?.changePaused(true)
    fun resume() = session?.changePaused(false)
    fun seek(ms: Long) = session?.seek(ms)
    fun stop() {
        session?.stop()
        session = null
    }

    private inner class Session(
        private val file: File,
        startMs: Long,
        private val onEnded: () -> Unit,
        private val onError: (Throwable) -> Unit,
    ) {
        @Volatile var paused = false
        @Volatile var finished = false
        @Volatile private var stopped = false
        @Volatile var durationMs = 0L
        private val seekRequest = AtomicLong(startMs.coerceAtLeast(0))
        @Volatile private var basePositionMs = startMs
        @Volatile private var baseFrames = 0L
        @Volatile private var line: SourceDataLine? = null

        fun positionMs(): Long {
            val l = line ?: return basePositionMs
            val pending = seekRequest.get()
            if (pending >= 0) return pending
            return basePositionMs + (l.longFramePosition - baseFrames) * 1000 / Decoder.SAMPLE_RATE
        }

        fun changePaused(value: Boolean) = synchronized(lock) {
            paused = value
            lock.notifyAll()
        }

        fun seek(ms: Long) = synchronized(lock) {
            seekRequest.set(ms.coerceIn(0, durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE))
            lock.notifyAll()
        }

        fun stop() = synchronized(lock) {
            stopped = true
            lock.notifyAll()
        }

        fun start() {
            thread(name = "pulse-audio", isDaemon = true) {
                try {
                    run()
                } catch (t: Throwable) {
                    if (!stopped) onError(t)
                }
            }
        }

        private fun run() {
            val grabber = Decoder.open(file)
            durationMs = grabber.lengthInTime / 1000
            val format = AudioFormat(Decoder.SAMPLE_RATE.toFloat(), 16, Decoder.CHANNELS, true, false)
            val out = AudioSystem.getSourceDataLine(format)
            // About 200 ms of buffer: short enough that pause and seek feel instant.
            out.open(format, Decoder.SAMPLE_RATE * Decoder.CHANNELS * 2 / 5)
            out.start()
            line = out
            var bytes = ByteArray(0)
            var shorts = ShortArray(0)
            try {
                while (!stopped) {
                    val seekTo = seekRequest.getAndSet(-1)
                    if (seekTo >= 0) {
                        if (seekTo > 0 || grabber.timestamp > 0) grabber.setTimestamp(seekTo * 1000, true)
                        out.flush()
                        basePositionMs = seekTo
                        baseFrames = out.longFramePosition
                    }
                    if (paused) {
                        out.stop()
                        synchronized(lock) {
                            while (paused && !stopped && seekRequest.get() < 0) lock.wait()
                        }
                        if (!paused) out.start()
                        continue
                    }
                    val frame = grabber.grabSamples() ?: break
                    val buffer = frame.samples?.firstOrNull() as? ShortBuffer ?: continue
                    val n = buffer.remaining()
                    if (shorts.size < n) shorts = ShortArray(n)
                    if (bytes.size < n * 2) bytes = ByteArray(n * 2)
                    buffer.get(shorts, 0, n)
                    val v = volume
                    for (i in 0 until n) {
                        val s = if (v >= 0.999f) shorts[i].toInt() else (shorts[i] * v).toInt()
                        bytes[2 * i] = s.toByte()
                        bytes[2 * i + 1] = (s shr 8).toByte()
                    }
                    onSamples?.invoke(shorts, n)
                    out.write(bytes, 0, n * 2)
                }
                if (!stopped) {
                    out.drain()
                    finished = true
                    onEnded()
                }
            } finally {
                line = null
                out.close()
                grabber.stop()
                grabber.release()
            }
        }
    }
}
