package com.metrolist.music.cloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
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

    private val registry = File(context.filesDir, "telegram_uploaded.txt")
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
            val chatId = chat.opt("id").toString()
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
                sendSong(token, chatId, id, title, artist, song?.song?.thumbnailUrl)
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

    private suspend fun sendSong(token: String, chatId: String, id: String, title: String, artist: String, thumbnailUrl: String?) {
        val dir = File(context.cacheDir, "telegram").apply { mkdirs() }
        val source = File(dir, "$id.src")
        val converted = File(dir, "$id.ogg")
        try {
            exportFromCache(id, source)
            val safeName = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" - ")
                .replace(Regex("""[\\/:*?"<>|]"""), "_").take(120).ifBlank { id }

            val (file, fileName, mime) = when {
                isMp4(source) -> Triple(source, "$safeName.m4a", "audio/mp4")
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && runCatching { remuxToOgg(source, converted) }.getOrDefault(false) ->
                    Triple(converted, "$safeName.ogg", "audio/ogg")
                else -> Triple(source, "$safeName.webm", "audio/webm")
            }
            if (file.length() > 49L * 1024 * 1024) throw TelegramException("file_too_large")

            val thumb = thumbnailUrl?.let { runCatching { thumbnail(it) }.getOrNull() }
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("title", title)
                .addFormDataPart("audio", fileName, file.asRequestBody(mime.toMediaType()))
                .apply {
                    if (artist.isNotBlank()) addFormDataPart("performer", artist)
                    if (thumb != null) addFormDataPart("thumbnail", "cover.jpg", thumb.toRequestBody("image/jpeg".toMediaType()))
                }
                .build()
            withRetry { callMultipart(token, "sendAudio", body) }
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

    // YouTube's best audio is Opus in WebM; Ogg is the container Telegram and phones play for Opus.
    // This only re-packs the stream, so there is no loss of quality.
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun remuxToOgg(src: File, dst: File): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(src.path)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME) == MediaFormat.MIMETYPE_AUDIO_OPUS
            } ?: return false
            extractor.selectTrack(track)
            muxer = MediaMuxer(dst.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG)
            val outTrack = muxer.addTrack(extractor.getTrackFormat(track))
            muxer.start()
            val buffer = ByteBuffer.allocate(512 * 1024)
            val info = MediaCodec.BufferInfo()
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                info.set(0, size, max(0L, extractor.sampleTime), 0)
                muxer.writeSampleData(outTrack, buffer, info)
                extractor.advance()
            }
            muxer.stop()
            return dst.length() > 0
        } finally {
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
