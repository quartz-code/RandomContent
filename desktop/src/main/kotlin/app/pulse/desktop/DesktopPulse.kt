package app.pulse.desktop

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.pulse.feed.PulseFeed
import com.metrolist.music.pulse.feed.PulseMemory
import com.metrolist.music.pulse.feed.PulseSettings
import com.metrolist.music.pulse.feed.YouTubePulseSource
import com.metrolist.music.ui.screens.pulse.PulseLook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalTime

/**
 * Pulse desktop: "My pulse" on the computer — the phone's [PulseFeed] with what this computer
 * knows: the account's likes and the history of what played here. It plays through the
 * [PlayerController] as an endless queue; any other queue started there ends it.
 */
class DesktopPulse(
    private val scope: CoroutineScope,
    private val controller: PlayerController,
    private val library: LibraryModel,
    private val history: History = History.default,
    private val dir: File = AppDirs.data,
) {
    private val settingsFile get() = File(dir, "pulse_settings.json")
    private val lookFile get() = File(dir, "pulse_look.json")
    private val memoryFile get() = File(dir, "pulse_memory.json")

    private val _settings = MutableStateFlow(PulseSettings.fromJson(runCatching { settingsFile.readText() }.getOrNull()))
    val settings: StateFlow<PulseSettings> = _settings.asStateFlow()
    private val _look = MutableStateFlow(PulseLook.fromJson(runCatching { lookFile.readText() }.getOrNull()))
    val look: StateFlow<PulseLook> = _look.asStateFlow()

    private val _feed = MutableStateFlow<PulseFeed?>(null)
    /** The pulse playing now, or null when the player plays something else. */
    val feed: StateFlow<PulseFeed?> = _feed.asStateFlow()
    private val _starting = MutableStateFlow(false)
    val starting: StateFlow<Boolean> = _starting.asStateFlow()
    /** The last start found nothing: no internet, or nothing fits the settings. */
    private val _empty = MutableStateFlow(false)
    val empty: StateFlow<Boolean> = _empty.asStateFlow()

    private val lock = Mutex()
    /** Likes already passed on to the feed, so each steers it once. */
    private val steeredBy = HashSet<String>()
    private var stream: PlayerController.Stream? = null

    init {
        // Another queue in the player (a search result, a playlist, a room) and the pulse is over.
        scope.launch {
            controller.stream.collect { playing -> if (playing == null || playing !== stream) _feed.value = null }
        }
    }

    fun saveSettings(settings: PulseSettings) {
        _settings.value = settings
        runCatching { settingsFile.writeText(settings.toJson()) }
    }

    fun saveLook(look: PulseLook) {
        _look.value = look
        runCatching { lookFile.writeText(look.toJson()) }
    }

    /** A new pulse with the settings for this hour; it plays once its first songs are in. */
    fun start() {
        if (_starting.value) return
        _starting.value = true
        _empty.value = false
        scope.launch {
            val feed = PulseFeed(YouTubePulseSource(), Library(), Memory(), _settings.value.at(LocalTime.now().hour))
            val first = next(feed)
            _starting.value = false
            if (first.isEmpty()) {
                _empty.value = true
                return@launch
            }
            val endless = PlayerController.Stream { if (_feed.value === feed) next(feed) else emptyList() }
            stream = endless
            _feed.value = feed
            controller.playQueue(first, 0, endless)
        }
    }

    /** The heart: liked in the account, and the pulse turns towards it. */
    fun like(track: Track) {
        val liking = track.videoId !in library.likedIds.value
        library.toggleLike(track)
        val feed = _feed.value ?: return
        if (liking) scope.launch { lock.withLock { if (steeredBy.add(track.videoId)) feed.liked(track.videoId) } }
    }

    /** "Not for me": remembered here, told to YouTube Music too when signed in, and skipped. */
    fun dislike(track: Track) {
        val song = SongItem(id = track.videoId, title = track.title, artists = track.artists.split(", ").map { Artist(it, null) }, thumbnail = track.thumbnail.orEmpty())
        val feed = _feed.value
        controller.next()
        scope.launch(Dispatchers.IO) {
            if (feed != null) lock.withLock { feed.disliked(song) } else Memory().let { it.save(it.load().dislike(song)) }
            if (Account.signedIn) YouTube.dislikeVideo(track.videoId)
        }
    }

    suspend fun moods(): List<PulseSettings.Mood> = withContext(Dispatchers.IO) { runCatching { YouTubePulseSource.moods() }.getOrDefault(emptyList()) }

    private suspend fun next(feed: PulseFeed): List<Track> = withContext(Dispatchers.IO) {
        lock.withLock {
            // A heart pressed in the player on one of the pulse's songs steers it too.
            library.likedIds.value.filter { it in feed.played && steeredBy.add(it) }.forEach(feed::liked)
            // The schedule: the morning gives way to the day while the pulse plays on.
            feed.retune(_settings.value.at(LocalTime.now().hour))
            runCatching { feed.next() }.getOrDefault(emptyList()).map(Track::from)
        }
    }

    /** What this computer knows: the account's likes, newest first, and what was heard here. */
    private inner class Library : PulseFeed.Library {
        override suspend fun knownIds(): Set<String> = library.likedIds.value + history.all().map { it.id }

        override suspend fun seedIds(): List<String> {
            val likes = library.liked.value.orEmpty().map { it.videoId }.take(SEEDS)
            val since = System.currentTimeMillis() - RECENT_DAYS * 24 * 3600 * 1000L
            val played = history.all().filter { it.at >= since }.groupBy { it.id }
                .mapValues { (_, plays) -> plays.sumOf { it.ms } }
                .entries.sortedByDescending { it.value }.map { it.key }.take(SEEDS)
            // Alternate, so neither the latest likes nor the old favourites take over.
            return (0 until maxOf(likes.size, played.size)).flatMap { listOfNotNull(likes.getOrNull(it), played.getOrNull(it)) }.distinct()
        }
    }

    private inner class Memory : PulseFeed.MemoryStore {
        override suspend fun load(): PulseMemory = PulseMemory.fromJson(runCatching { memoryFile.readText() }.getOrNull())

        override suspend fun save(memory: PulseMemory) {
            runCatching { memoryFile.writeText(memory.toJson()) }
        }
    }

    private companion object {
        const val SEEDS = 25
        const val RECENT_DAYS = 60
    }
}
