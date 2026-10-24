package app.pulse.desktop

import com.metrolist.innertubex.extraction.ExtractedStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Pulse desktop: a song is downloaded whole before it plays (a few MB, a second or two), which
 * makes seeking instant and leaves a cache for replays. Requests mirror the Android app's: the
 * stream's headers, and bounded Range chunks when the stream asks for them.
 */
object AudioDownloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val FALLBACK_CHUNK = 1L shl 20

    fun cached(videoId: String): File? = AppDirs.audioCache.listFiles()?.firstOrNull { it.name.startsWith("$videoId.") && !it.name.endsWith(".part") }

    suspend fun download(videoId: String, stream: ExtractedStream, onProgress: (Long, Long?) -> Unit): File = withContext(Dispatchers.IO) {
        cached(videoId)?.let { return@withContext it }
        val ext = if (stream.mimeType.orEmpty().contains("webm")) "webm" else "m4a"
        val target = File(AppDirs.audioCache, "$videoId.$ext")
        val part = File(AppDirs.audioCache, "$videoId.$ext.part")
        val total = stream.contentLengthBytes?.takeIf { it > 0 }
        val chunked = stream.useRangeChunks || stream.requireBoundedRange
        val chunk = stream.rangeChunkSizeBytes.takeIf { it > 0 } ?: FALLBACK_CHUNK
        part.outputStream().use { out ->
            var position = 0L
            while (true) {
                ensureActive()
                val end = if (chunked) position + chunk - 1 else null
                val range = if (end != null) "bytes=$position-${total?.let { minOf(end, it - 1) } ?: end}" else "bytes=$position-"
                val request = Request.Builder().url(stream.audioUrl).apply {
                    stream.headers.forEach { (k, v) -> header(k, v) }
                    header("Range", range)
                }.build()
                val read = client.newCall(request).execute().use { response ->
                    if (response.code == 416) return@use 0L
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    var n = 0L
                    response.body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val r = input.read(buffer)
                            if (r < 0) break
                            out.write(buffer, 0, r)
                            n += r
                            onProgress(position + n, total)
                        }
                    }
                    n
                }
                position += read
                if (read == 0L || !chunked || (total != null && position >= total) || read < chunk) break
            }
        }
        if (part.length() == 0L) throw IOException("empty stream")
        part.renameTo(target)
        target
    }
}
