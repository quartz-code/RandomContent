package com.metrolist.music.cloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.datastore.preferences.core.edit
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.offline.Download
import com.metrolist.music.constants.TelegramAutoUploadKey
import com.metrolist.music.constants.TelegramBotTokenKey
import com.metrolist.music.constants.TelegramChatIdKey
import com.metrolist.music.constants.TelegramChatTitleKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.di.DownloadCache
import com.metrolist.music.playback.DownloadUtil
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Pulse: keeps a second copy of downloaded songs in Telegram. Songs go through a bot the user owns,
 * into the chat with that bot or a channel where it is an admin, so they are stored for free and
 * can be played in Telegram's own music player on any device.
 */
@Singleton
class TelegramCloud @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    @DownloadCache private val downloadCache: Cache,
    private val downloadUtil: dagger.Lazy<DownloadUtil>,
) {
    sealed interface State {
        data object Idle : State
        data class Uploading(val done: Int, val total: Int, val current: String) : State
        data class Finished(val sent: Int, val failed: Int, val error: String?) : State
    }

    class TelegramException(message: String, val retryAfterSec: Int = 0) : IOException(message)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    // v2: songs sent by the first version went out as Ogg voice notes; send them again as music.
    private val registry = File(context.filesDir, "telegram_uploaded_v2.txt")
    private val uploaded: MutableSet<String> by lazy {
        runCatching { registry.readLines().filter { it.isNotBlank() }.toMutableSet() }.getOrDefault(mutableSetOf())
    }

    @Volatile private var running = false
    @Volatile private var rerun = false

    fun uploadedCount(): Int = synchronized(uploaded) { uploaded.size }

    fun downloadedIds(): List<String> = downloadUtil.get().downloads.value.values
        .filter { it.state == Download.STATE_COMPLETED }
        .map { it.request.id }

    /**
     * Checks the token and finds where to store music: the most recent chat that wrote to the bot
     * (the user's private chat after /start, or a channel post where the bot is admin).
     */
    suspend fun connect(token: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = token.trim()
            call(trimmed, "getMe")
            val updates = call(trimmed, "getUpdates").optJSONArray("result")
            var chat: JSONObject? = null
            if (updates != null) {
                for (i in updates.length() - 1 downTo 0) {
                    val u = updates.optJSONObject(i) ?: continue
                    chat = (u.optJSONObject("message") ?: u.optJSONObject("channel_post"))?.optJSONObject("chat")
                    if (chat != null) break
                }
            }
            chat ?: throw TelegramException("no_chat")
            val chatId = chat.opt("id")?.toString() ?: throw TelegramException("no_chat")
            val title = chat.optString("title").ifBlank {
                listOf(chat.optString("first_name"), chat.optString("last_name")).filter { it.isNotBlank() }.joinToString(" ")
            }.ifBlank { chatId }
            call(trimmed, "sendMessage", mapOf("chat_id" to chatId, "text" to "Pulse подключён. Сюда будет складываться музыка 🎧"))
            context.dataStore.edit {
                it[TelegramBotTokenKey] = trimmed
                it[TelegramChatIdKey] = chatId
                it[TelegramChatTitleKey] = title
            }
            title
        }
    }

    suspend fun disconnect() {
        context.dataStore.edit {
            it.remove(TelegramBotTokenKey)
            it.remove(TelegramChatIdKey)
            it.remove(TelegramChatTitleKey)
            it[TelegramAutoUploadKey] = false
        }
    }

    /** Called when a download finishes; uploads it if automatic upload is on. */
    fun onDownloaded() {
        scope.launch {
            if (context.dataStore.get(TelegramAutoUploadKey, false)) uploadPending()
        }
    }

    /** Sends every downloaded song that is not in Telegram yet. Safe to call repeatedly. */
    fun uploadPending() {
        if (running) {
            rerun = true
            return
        }
        running = true
        scope.launch {
            try {
                do {
                    rerun = false
                    uploadBatch()
                } while (rerun)
            } finally {
                running = false
            }
        }
    }

    private suspend fun uploadBatch() {
        val token = context.dataStore.get(TelegramBotTokenKey, "")
        val chatId = context.dataStore.get(TelegramChatIdKey, "")
        if (token.isBlank() || chatId.isBlank()) return

        val pending = downloadedIds().filter { synchronized(uploaded) { it !in uploaded } }
        if (pending.isEmpty()) return
        var sent = 0
        var failed = 0
        var lastError: String? = null
        var failuresInRow = 0

        for ((index, id) in pending.withIndex()) {
            val song = database.getSongByIdBlocking(id)
            val title = song?.song?.title ?: id
            val artist = song?.artists?.joinToString(", ") { it.name }.orEmpty()
            _state.value = State.Uploading(index, pending.size, listOf(artist, title).filter { it.isNotBlank() }.joinToString(" — "))
            try {
                sendSong(token, chatId, id, title, artist, song?.song?.thumbnailUrl, song?.song?.duration ?: -1)
                synchronized(uploaded) {
                    uploaded += id
                    registry.appendText("$id\n")
                }
                sent++
                failuresInRow = 0
            } catch (e: Exception) {
                Timber.w(e, "Telegram upload failed for $id")
                failed++
                failuresInRow++
                lastError = e.message
                // Probably offline or the bot was removed: stop instead of failing every song.
                if (failuresInRow >= 3) break
            }
            // Telegram allows about 20 messages a minute in a channel.
            delay(3_100)
        }
        _state.value = State.Finished(sent, failed, lastError)
    }

    private suspend fun sendSong(
        token: String,
        chatId: String,
        id: String,
        title: String,
        artist: String,
        thumbnailUrl: String?,
        durationSec: Int,
    ) {
        val dir = File(context.cacheDir, "telegram").apply { mkdirs() }
        val source = File(dir, "$id.src")
        val converted = File(dir, "$id.m4a")
        try {
            exportFromCache(id, source)
            val safeName = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" - ")
                .replace(Regex("""[\\/:*?"<>|]"""), "_").take(120).ifBlank { id }

            val m4a = when {
                isMp4(source) -> source
                runCatching { transcodeToM4a(source, converted) }
                    .onFailure { Timber.w(it, "Transcoding $id failed") }
                    .getOrDefault(false) -> converted
                else -> null
            }
            val thumb = thumbnailUrl?.let { runCatching { thumbnail(it) }.getOrNull() }

            val body = if (m4a != null) {
                if (m4a.length() > 49L * 1024 * 1024) throw TelegramException("file_too_large")
                MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", chatId)
                    .addFormDataPart("title", title)
                    .addFormDataPart("audio", "$safeName.m4a", m4a.asRequestBody("audio/mp4".toMediaType()))
                    .apply {
                        if (artist.isNotBlank()) addFormDataPart("performer", artist)
                        if (durationSec > 0) addFormDataPart("duration", durationSec.toString())
                        if (thumb != null) addFormDataPart("thumbnail", "cover.jpg", thumb.toRequestBody("image/jpeg".toMediaType()))
                    }
                    .build()
            } else {
                // Could not convert: keep the original as a plain file rather than a voice message.
                if (source.length() > 49L * 1024 * 1024) throw TelegramException("file_too_large")
                MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", chatId)
                    .addFormDataPart("caption", listOf(artist, title).filter { it.isNotBlank() }.joinToString(" — "))
                    .addFormDataPart("disable_content_type_detection", "true")
                    .addFormDataPart("document", "$safeName.webm", source.asRequestBody("application/octet-stream".toMediaType()))
                    .build()
            }
            withRetry { callMultipart(token, if (m4a != null) "sendAudio" else "sendDocument", body) }
        } finally {
            source.delete()
            converted.delete()
        }
    }

    // Downloads are stored in ExoPlayer's cache under the song id; read them back as one file.
    private fun exportFromCache(id: String, out: File) {
        val dataSource = CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(null)
            .createDataSource()
        val spec = DataSpec.Builder().setUri(id).setKey(id).build()
        try {
            dataSource.open(spec)
            out.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = dataSource.read(buffer, 0, buffer.size)
                    if (read == C.RESULT_END_OF_INPUT) break
                    output.write(buffer, 0, read)
                }
            }
        } finally {
            dataSource.close()
        }
        if (out.length() == 0L) throw IOException("empty_download")
    }

    private fun isMp4(file: File): Boolean = file.inputStream().use { input ->
        val header = ByteArray(12)
        input.read(header) == 12 && String(header, 4, 4, Charsets.US_ASCII) == "ftyp"
    }

    /**
     * Telegram shows any Ogg/Opus file as a voice message, so Opus (YouTube's usual format) is
     * re-encoded to AAC in M4A, which Telegram always shows as music with title and cover.
     * 256 kbps AAC from ~160 kbps Opus is audibly the same; the copy on the phone is untouched.
     */
    private fun transcodeToM4a(src: File, dst: File): Boolean {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(src.path)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return false
            extractor.selectTrack(track)
            val inputFormat = extractor.getTrackFormat(track)
            val dec = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!).also { decoder = it }
            dec.configure(inputFormat, null, null, 0)
            dec.start()
            val mux = MediaMuxer(dst.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4).also { muxer = it }

            val info = MediaCodec.BufferInfo()
            val encInfo = MediaCodec.BufferInfo()
            var muxTrack = -1
            var inputDone = false
            var decodeDone = false
            var encodeDone = false
            var sampleRate = 0
            var bytesPerFrame = 0
            var framesFed = 0L

            fun drainEncoder() {
                val enc = encoder ?: return
                while (true) {
                    val index = enc.dequeueOutputBuffer(encInfo, 0)
                    when {
                        index == MediaCodec.INFO_TRY_AGAIN_LATER -> return
                        index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            muxTrack = mux.addTrack(enc.outputFormat)
                            mux.start()
                        }
                        index >= 0 -> {
                            val out = enc.getOutputBuffer(index)!!
                            if (encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) encInfo.size = 0
                            if (encInfo.size > 0 && muxTrack >= 0) {
                                out.position(encInfo.offset)
                                out.limit(encInfo.offset + encInfo.size)
                                mux.writeSampleData(muxTrack, out, encInfo)
                            }
                            enc.releaseOutputBuffer(index, false)
                            if (encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                encodeDone = true
                                return
                            }
                        }
                    }
                }
            }

            fun feedEncoder(pcm: ByteBuffer, endOfStream: Boolean) {
                val enc = encoder ?: return
                while (pcm.hasRemaining() || endOfStream) {
                    val index = enc.dequeueInputBuffer(10_000)
                    if (index < 0) {
                        drainEncoder()
                        continue
                    }
                    val buffer = enc.getInputBuffer(index)!!
                    buffer.clear()
                    val count = minOf(buffer.remaining(), pcm.remaining())
                    val slice = pcm.duplicate()
                    slice.limit(pcm.position() + count)
                    buffer.put(slice)
                    pcm.position(pcm.position() + count)
                    val pts = framesFed * 1_000_000L / sampleRate
                    framesFed += count / bytesPerFrame
                    val last = endOfStream && !pcm.hasRemaining()
                    enc.queueInputBuffer(index, 0, count, pts, if (last) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                    if (last) return
                }
            }

            while (!encodeDone) {
                if (!inputDone) {
                    val index = dec.dequeueInputBuffer(10_000)
                    if (index >= 0) {
                        val size = extractor.readSampleData(dec.getInputBuffer(index)!!, 0)
                        if (size < 0) {
                            dec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            dec.queueInputBuffer(index, 0, size, max(0L, extractor.sampleTime), 0)
                            extractor.advance()
                        }
                    }
                }
                if (!decodeDone) {
                    val index = dec.dequeueOutputBuffer(info, 10_000)
                    if (index >= 0) {
                        if (encoder == null) {
                            val pcmFormat = dec.outputFormat
                            sampleRate = pcmFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            val channels = pcmFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            bytesPerFrame = 2 * channels
                            val aac = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
                                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                                setInteger(MediaFormat.KEY_BIT_RATE, 256_000)
                                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 256 * 1024)
                            }
                            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                                configure(aac, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                                start()
                            }
                        }
                        val pcm = dec.getOutputBuffer(index)!!
                        pcm.position(info.offset)
                        pcm.limit(info.offset + info.size)
                        val endOfStream = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        feedEncoder(pcm, endOfStream)
                        dec.releaseOutputBuffer(index, false)
                        if (endOfStream) decodeDone = true
                    }
                } else if (encoder == null) {
                    return false
                }
                drainEncoder()
            }
            mux.stop()
            return dst.length() > 0
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            runCatching { muxer?.release() }
            extractor.release()
        }
    }

    private fun thumbnail(url: String): ByteArray {
        val bytes = client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("thumb ${response.code}")
            response.body.bytes()
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IOException("thumb decode")
        val scale = 320f / max(bitmap.width, bitmap.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    }

    private suspend fun <T> withRetry(block: () -> T): T {
        repeat(3) {
            try {
                return block()
            } catch (e: TelegramException) {
                if (e.retryAfterSec <= 0) throw e
                delay(e.retryAfterSec * 1000L + 500)
            }
        }
        return block()
    }

    private fun call(token: String, method: String, params: Map<String, String> = emptyMap()): JSONObject {
        val body = okhttp3.FormBody.Builder().apply { params.forEach { (k, v) -> add(k, v) } }.build()
        return execute(token, method, body)
    }

    private fun callMultipart(token: String, method: String, body: RequestBody): JSONObject = execute(token, method, body)

    private fun execute(token: String, method: String, body: RequestBody): JSONObject {
        val request = Request.Builder().url("https://api.telegram.org/bot$token/$method").post(body).build()
        client.newCall(request).execute().use { response ->
            val json = runCatching { JSONObject(response.body.string()) }.getOrNull()
                ?: throw TelegramException("HTTP ${response.code}")
            if (!json.optBoolean("ok")) {
                val retry = json.optJSONObject("parameters")?.optInt("retry_after", 0) ?: 0
                throw TelegramException(json.optString("description", "HTTP ${response.code}"), retry)
            }
            return json
        }
    }
}
