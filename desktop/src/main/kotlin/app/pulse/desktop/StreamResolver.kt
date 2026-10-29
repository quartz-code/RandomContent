package app.pulse.desktop

import com.metrolist.innertube.YouTube
import com.metrolist.innertubex.InnerTubeLogLevel
import com.metrolist.innertubex.InnerTubeLogger
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.ExtractedStream
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.extraction.generateClientPlaybackNonce
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.Properties

/**
 * Pulse desktop: the same InnerTubeX extraction chain as the Android app's InnerTubeXPlayer, with
 * two differences. The player config cache lives in a file instead of SharedPreferences, and there
 * is no PoToken (the app makes one in a WebView): InnerTubeX then picks clients that need none.
 */
object StreamResolver {
    private val mutex = Mutex()
    private var extractor: InnerTubeExtractor? = null
    private var generation = -1L

    private val logger = InnerTubeLogger { event ->
        if (System.getenv("PULSE_DEBUG") == null) return@InnerTubeLogger
        if (event.level == InnerTubeLogLevel.DEBUG) return@InnerTubeLogger
        System.err.println("${event.level}/${event.tag}: ${event.message} ${event.details}")
    }

    suspend fun resolve(videoId: String, quality: AudioQuality = AudioQuality.HIGH): ExtractedStream {
        val stream = requireNotNull(
            extractor().extract(
                videoId = videoId,
                hints = ContentHints().withStreamCapabilities(allowHls = false, allowSabr = false, allowBoundedRange = true),
                excludedClients = emptySet(),
                audioQuality = quality,
                clientPlaybackNonce = generateClientPlaybackNonce(),
            ),
        ) { "no playable stream" }
        check(stream.sabrBootstrap == null) { "SABR streams are not supported" }
        return stream
    }

    private suspend fun extractor(): InnerTubeExtractor = mutex.withLock {
        val transport = YouTube.extractionTransport()
        extractor?.takeIf { generation == transport.generation }?.let { return it }
        val store = RemotePlayerConfigStore(transport.httpClient, FileConfigRepository, logger)
        val cipher = YouTubeCipherService(transport.httpClient, store, logger)
        InnerTubeExtractor(
            configParser = YtConfigParserImpl(transport.httpClient, transport.innerTube, store, logger),
            cipherService = cipher,
            innerTube = transport.innerTube,
            logger = logger,
        ).also {
            extractor = it
            generation = transport.generation
        }
    }

    private object FileConfigRepository : PlayerConfigRepository {
        private val file = File(AppDirs.data, "player_config.properties")
        private val props = Properties().apply { if (file.exists()) file.inputStream().use(::load) }

        private fun get(key: String) = props.getProperty(key).orEmpty()
        private fun set(key: String, value: String) {
            props.setProperty(key, value)
            runCatching { file.outputStream().use { props.store(it, null) } }
        }

        override val enabled = true
        override val sourceUrl = PLAYER_CONFIG_URL
        override val defaultSourceUrl = PLAYER_CONFIG_URL
        override var cachedJson: String
            get() = get("json")
            set(value) = set("json", value)
        override var cachedAtMs: Long
            get() = get("cached_at_ms").toLongOrNull() ?: 0L
            set(value) = set("cached_at_ms", value.toString())
        override var cachedSourceUrl: String
            get() = get("source_url")
            set(value) = set("source_url", value)
        override var cachedEtag: String
            get() = get("etag")
            set(value) = set("etag", value)

        // The same source the Android app uses.
        private const val PLAYER_CONFIG_URL =
            "https://raw.githubusercontent.com/ZemerTeam/zemer-cipher/master/library/src/main/assets/player_configs.json"
    }
}
