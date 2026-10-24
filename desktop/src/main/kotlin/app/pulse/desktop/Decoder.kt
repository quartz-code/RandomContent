package app.pulse.desktop

import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegLogCallback
import java.io.File
import java.nio.ShortBuffer

/** FFmpeg set up to hand out 48 kHz 16-bit stereo, whatever the file (Opus/WebM or AAC/M4A). */
object Decoder {
    const val SAMPLE_RATE = 48_000
    const val CHANNELS = 2

    init {
        avutil.av_log_set_level(if (System.getenv("PULSE_DEBUG") != null) avutil.AV_LOG_WARNING else avutil.AV_LOG_ERROR)
        FFmpegLogCallback.set()
    }

    fun open(file: File): FFmpegFrameGrabber = FFmpegFrameGrabber(file).apply {
        sampleRate = SAMPLE_RATE
        audioChannels = CHANNELS
        sampleFormat = avutil.AV_SAMPLE_FMT_S16
        start()
    }

    /** For the probe: decodes up to [limit] seconds and says how much came out. */
    fun decodeSeconds(file: File, limit: Double): Double {
        val grabber = open(file)
        var samples = 0L
        try {
            while (samples < limit * SAMPLE_RATE) {
                val frame = grabber.grabSamples() ?: break
                val buffer = frame.samples?.firstOrNull() as? ShortBuffer ?: continue
                samples += buffer.remaining() / CHANNELS
            }
        } finally {
            grabber.stop()
            grabber.release()
        }
        return samples.toDouble() / SAMPLE_RATE
    }
}
