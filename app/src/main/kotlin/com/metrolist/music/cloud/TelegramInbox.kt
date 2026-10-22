package com.metrolist.music.cloud

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.constants.TelegramBotTokenKey
import com.metrolist.music.constants.TelegramChatIdKey
import com.metrolist.music.constants.TelegramInboxKey
import com.metrolist.music.constants.TelegramInboxOffsetKey
import com.metrolist.music.constants.TelegramInboxPlaylistKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.di.DownloadCache
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.DownloadUtil
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.pulse.importer.ImportedTrack
import com.metrolist.music.pulse.importer.TrackMatcher
import com.metrolist.music.pulse.importer.YandexMusicImport
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pulse: the bot works both ways. Besides keeping copies of downloads (see [TelegramCloud]), it
 * takes what the user sends it: a music file goes straight into Downloads, a song name or a
 * YouTube / Yandex Music link is found on YouTube Music and downloaded. Everything lands in the
 * "Из Telegram" playlist and the bot answers in the chat.
 *
 * The bot is checked while Pulse is open (Bot API has no push without a server of our own), so a
 * song sent from Telegram shows up the moment the user switches back.
 */
@Singleton
class TelegramInbox @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    @DownloadCache private val downloadCache: Cache,
    private val downloadUtil: dagger.Lazy<DownloadUtil>,
    private val cloud: TelegramCloud,
) {
    sealed interface State {
        data object Idle : State
        data object Checking : State
        data class Done(val added: Int, val error: String?) : State
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var loop: Job? = null

    /** Starts checking the bot whenever Pulse is on screen; called once from App. */
    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                loop?.cancel()
                loop = scope.launch {
                    while (isActive) {
                        runCatching { checkNow() }.onFailure { Timber.w(it, "Telegram inbox check failed") }
                        delay(POLL_MS)
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                loop?.cancel()
                loop = null
            }
        })
    }

    /** Takes everything new from the bot. Returns how many songs were added. */
    suspend fun checkNow(): Int = mutex.withLock {
        withContext(Dispatchers.IO) {
            val token = context.dataStore.get(TelegramBotTokenKey, "")
            val chatId = context.dataStore.get(TelegramChatIdKey, "")
            if (token.isBlank() || chatId.isBlank() || !context.dataStore.get(TelegramInboxKey, true)) return@withContext 0

            _state.value = State.Checking
            var added = 0
            var error: String? = null
            try {
                val offset = context.dataStore.get(TelegramInboxOffsetKey, 0L)
                val response = cloud.call(
                    token,
                    "getUpdates",
                    buildMap {
                        if (offset > 0) put("offset", offset.toString())
                        put("timeout", "0")
                        put("allowed_updates", """["message","channel_post"]""")
                    },
                )
                val updates = json.parseToJsonElement(response.toString()).let { (it as? kotlinx.serialization.json.JsonObject)?.get("result") as? JsonArray }
                    ?: JsonArray(emptyList())
                for (item in TelegramInboxParser.parse(updates, chatId)) {
                    val handled = handle(token, chatId, item)
                    if (handled < 0) break // YouTube or Telegram unreachable: try this one again later
                    added += handled
                    context.dataStore.edit { it[TelegramInboxOffsetKey] = item.updateId + 1 }
                }
            } catch (e: Exception) {
                Timber.w(e, "Telegram inbox")
                error = e.message
            }
            _state.value = State.Done(added, error)
            added
        }
    }

    /** Number of songs added, or -1 to stop and retry the same message later. */
    private suspend fun handle(token: String, chatId: String, item: TelegramInboxParser.Item): Int = when (item) {
        is TelegramInboxParser.Item.Skip -> 0
        is TelegramInboxParser.Item.Command -> {
            if (item.command == "/start" || item.command == "/help") reply(token, chatId, item.messageId, HELP)
            0
        }
        is TelegramInboxParser.Item.Audio -> handleAudio(token, chatId, item)
        is TelegramInboxParser.Item.Query -> handleQuery(token, chatId, item)
    }

    // ---- files ----

    private suspend fun handleAudio(token: String, chatId: String, item: TelegramInboxParser.Item.Audio): Int {
        if (cloud.wasSentByPulse(item.fileUniqueId)) return 0
        val mediaId = ID_PREFIX + item.fileUniqueId
        database.getSongByIdBlocking(mediaId)?.takeIf { it.song.isDownloaded }?.let { song ->
            reply(token, chatId, item.messageId, "Эта песня уже есть в Pulse: ${song.song.title}")
            return 0
        }
        if ((item.fileSize ?: 0L) > MAX_BOT_DOWNLOAD) {
            reply(token, chatId, item.messageId, "Файл больше 20 МБ — Telegram не даёт ботам скачивать такие. Пришли файл поменьше или название песни.")
            return 0
        }

        val dir = File(context.cacheDir, "telegram_in").apply { mkdirs() }
        val file = File(dir, item.fileUniqueId)
        try {
            downloadFile(token, item.fileId, file)
        } catch (e: IOException) {
            Timber.w(e, "Telegram file download failed")
            return -1
        }

        val tags = readTags(file)
        val fromName = item.fileName?.let(TelegramInboxParser::splitFileName)
        val title = item.title ?: tags.title ?: fromName?.second ?: "Без названия"
        val artist = item.performer ?: tags.artist ?: fromName?.first
        val duration = item.durationSec ?: tags.durationSec ?: -1
        val cover = saveCover(token, item, file)

        // The player and the downloads list both look songs up in the download cache by id, so a
        // file written there under its own id plays offline like any other download.
        CacheWriter(
            CacheDataSource.Factory()
                .setCache(downloadCache)
                .setUpstreamDataSourceFactory(FileDataSource.Factory())
                .createDataSource(),
            DataSpec.Builder().setUri(Uri.fromFile(file)).setKey(mediaId).build(),
            null,
            null,
        ).cache()

        val metadata = MediaMetadata(
            id = mediaId,
            title = title,
            artists = listOfNotNull(artist?.let { MediaMetadata.Artist(id = null, name = it) }),
            duration = duration,
            thumbnailUrl = cover,
        )
        database.insert(metadata)
        addToInboxPlaylist(listOf(mediaId))
        // Already fully cached, so the download "finishes" at once and the song appears in Downloads.
        withContext(Dispatchers.Main) {
            DownloadService.sendAddDownload(
                context,
                ExoDownloadService::class.java,
                DownloadRequest.Builder(mediaId, Uri.fromFile(file))
                    .setCustomCacheKey(mediaId)
                    .setData(title.toByteArray())
                    .build(),
                false,
            )
        }
        scope.launch {
            delay(60_000)
            file.delete()
        }
        reply(token, chatId, item.messageId, "🎵 Добавил в Pulse: ${listOfNotNull(artist, title).joinToString(" — ")}. Она в «Скачанных» и в плейлисте «Из Telegram».")
        return 1
    }

    private fun downloadFile(token: String, fileId: String, out: File) {
        val path = cloud.call(token, "getFile", mapOf("file_id" to fileId))
            .optJSONObject("result")?.optString("file_path")
            ?.takeIf { it.isNotBlank() } ?: throw IOException("no file_path")
        val request = Request.Builder().url("https://api.telegram.org/file/bot$token/$path").build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            out.outputStream().use { response.body.byteStream().copyTo(it) }
        }
        if (out.length() == 0L) throw IOException("empty file")
    }

    private data class Tags(val title: String?, val artist: String?, val durationSec: Int?, val picture: ByteArray?)

    private fun readTags(file: File): Tags = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            Tags(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() },
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() },
                durationSec = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { (it / 1000).toInt() },
                picture = retriever.embeddedPicture,
            )
        } finally {
            retriever.release()
        }
    }.getOrDefault(Tags(null, null, null, null))

    /** The cover Telegram made for the file, or the one embedded in it; kept next to the app data. */
    private fun saveCover(token: String, item: TelegramInboxParser.Item.Audio, file: File): String? {
        val dir = File(context.filesDir, "telegram_covers").apply { mkdirs() }
        val out = File(dir, "${item.fileUniqueId}.jpg")
        val fromTags = runCatching { readTags(file).picture }.getOrNull()
        val ok = when {
            fromTags != null -> runCatching { out.writeBytes(fromTags) }.isSuccess
            item.thumbFileId != null -> runCatching { downloadFile(token, item.thumbFileId, out) }.isSuccess
            else -> false
        }
        return if (ok && out.length() > 0) Uri.fromFile(out).toString() else null
    }

    // ---- names and links ----

    private suspend fun handleQuery(token: String, chatId: String, item: TelegramInboxParser.Item.Query): Int {
        val songs = try {
            resolve(item.text)
        } catch (e: TrackMatcher.Unreachable) {
            return -1
        } catch (e: YandexMusicImport.Failure.Region) {
            reply(token, chatId, item.messageId, "Яндекс Музыка отвечает только из России: выключи VPN на минуту и пришли ссылку ещё раз.")
            return 0
        } catch (e: YandexMusicImport.Failure) {
            reply(token, chatId, item.messageId, "Не получилось открыть эту ссылку Яндекс Музыки. Она точно публичная?")
            return 0
        }
        if (songs.isEmpty()) {
            reply(token, chatId, item.messageId, "Не нашёл «${item.text.take(80)}». Попробуй «Исполнитель — Песня».")
            return 0
        }
        val metadata = songs.map { it.toMediaMetadata() }
        metadata.forEach { runCatching { database.insert(it) } }
        addToInboxPlaylist(metadata.map { it.id })
        withContext(Dispatchers.Main) { metadata.forEach { downloadUtil.get().download(it) } }
        val names = metadata.take(5).joinToString("\n") { "• " + listOfNotNull(it.artists.firstOrNull()?.name, it.title).joinToString(" — ") }
        val more = if (metadata.size > 5) "\n…и ещё ${metadata.size - 5}" else ""
        reply(token, chatId, item.messageId, "🔎 Нашёл и скачиваю в Pulse:\n$names$more")
        return metadata.size
    }

    private suspend fun resolve(text: String): List<SongItem> {
        TelegramInboxParser.youtubeVideoId(text)?.let { id ->
            return YouTube.queue(listOf(id)).getOrNull()?.take(1).orEmpty()
        }
        if (YandexMusicImport.isYandexLink(text)) {
            val list = YandexMusicImport.load(text)
            return list.tracks.take(MAX_LINK_TRACKS).mapNotNull { best(it) }
        }
        val (artist, title) = TelegramInboxParser.splitQuery(text)
        return listOfNotNull(best(ImportedTrack(title, listOfNotNull(artist))))
    }

    private suspend fun best(track: ImportedTrack): SongItem? {
        val match = TrackMatcher.match(track)
        return match.best?.takeIf { match.status != TrackMatcher.Status.NOT_FOUND }?.song
    }

    private suspend fun addToInboxPlaylist(songIds: List<String>) {
        val savedId = context.dataStore.get(TelegramInboxPlaylistKey, "")
        var playlist = savedId.takeIf { it.isNotBlank() }?.let { database.playlist(it).firstOrNull() }
        if (playlist == null) {
            val entity = PlaylistEntity(name = PLAYLIST_NAME)
            database.insert(entity)
            context.dataStore.edit { it[TelegramInboxPlaylistKey] = entity.id }
            playlist = database.playlist(entity.id).firstOrNull() ?: return
        }
        database.addSongsToPlaylist(playlist, songIds.map { it to null }, prepend = true)
    }

    private fun reply(token: String, chatId: String, messageId: Long, text: String) {
        runCatching {
            cloud.call(
                token,
                "sendMessage",
                mapOf(
                    "chat_id" to chatId,
                    "text" to text,
                    "reply_parameters" to """{"message_id":$messageId,"allow_sending_without_reply":true}""",
                ),
            )
        }.onFailure { Timber.w(it, "Telegram reply failed") }
    }

    companion object {
        /** Songs that came from Telegram files; they are never sent back there. */
        const val ID_PREFIX = "tg_"
        const val PLAYLIST_NAME = "Из Telegram"

        // Bot API lets bots download files up to 20 MB.
        private const val MAX_BOT_DOWNLOAD = 20L * 1024 * 1024
        private const val MAX_LINK_TRACKS = 50
        private const val POLL_MS = 30_000L

        private const val HELP =
            "Это твой бот Pulse.\n\n" +
                "• Пришли сюда песню файлом или перешли её из любого чата — она появится в Pulse и будет играть без интернета.\n" +
                "• Напиши «Исполнитель — Песня» или пришли ссылку на YouTube / Яндекс Музыку — Pulse найдёт её и скачает.\n\n" +
                "Всё складывается в плейлист «Из Telegram». Pulse забирает присланное, когда открыт."
    }
}
