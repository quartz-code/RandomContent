package com.metrolist.music.pulse.feed

import android.content.Context
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.constants.PulseFeedMemoryKey
import com.metrolist.music.constants.PulseFeedSettingsKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.safeDataStoreEdit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pulse: "My pulse" on the phone — the [PulseFeed] wired to YouTube Music, the library and the
 * stored settings. The player gets it as a [com.metrolist.music.playback.queues.PulseQueue]; the
 * page talks to it for likes, "not for me" and the settings.
 */
@Singleton
class MyPulse @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val lock = Mutex()
    private val _feed = MutableStateFlow<PulseFeed?>(null)

    /** The pulse playing now, or null when the player plays something else. */
    val feed: StateFlow<PulseFeed?> = _feed.asStateFlow()

    /** The last start found nothing to play: no internet, or nothing fits the settings. */
    private val _empty = MutableStateFlow(false)
    val empty: StateFlow<Boolean> = _empty.asStateFlow()

    val settings: Flow<PulseSettings> = context.dataStore.data.map { PulseSettings.fromJson(it[PulseFeedSettingsKey]) }

    private suspend fun storedSettings(): PulseSettings = PulseSettings.fromJson(context.dataStore.data.first()[PulseFeedSettingsKey])

    suspend fun saveSettings(settings: PulseSettings) {
        context.safeDataStoreEdit { it[PulseFeedSettingsKey] = settings.toJson() }
    }

    /** A start is under way: the first songs are being picked (with a slow VPN, for a while). */
    private val _starting = MutableStateFlow(false)
    val starting: StateFlow<Boolean> = _starting.asStateFlow()

    /** The pulse being started; another queue played meanwhile cancels it. */
    @Volatile
    private var pending: PulseFeed? = null

    /** A new pulse with the stored settings; it becomes "the one playing" once its first songs arrive. */
    suspend fun newFeed(): PulseFeed {
        _empty.value = false
        _starting.value = true
        val current = storedSettings().at(LocalTime.now().hour)
        return PulseFeed(YouTubePulseSource(), DatabaseLibrary(), DataStoreMemory(), current).also { pending = it }
    }

    /**
     * The first songs of [feed]. Only with songs in hand is the pulse "the one playing": before,
     * the player still plays what it played, and the page must not take that song for the pulse's.
     * Empty when nothing came (no internet, VPN) or another queue started meanwhile.
     */
    suspend fun begin(feed: PulseFeed): List<SongItem> {
        val batch = runCatching { next(feed) }.getOrDefault(emptyList())
        val stillWanted = pending === feed
        if (stillWanted) {
            pending = null
            _starting.value = false
            if (batch.isEmpty()) _empty.value = true else _feed.value = feed
        }
        return if (stillWanted) batch else emptyList()
    }

    /**
     * The pulse that played when the app was closed, back with the same songs in the player: it
     * goes on from where it was instead of becoming a plain list the page does not know.
     */
    suspend fun restored(songIds: List<String>): PulseFeed {
        val current = storedSettings().at(LocalTime.now().hour)
        return PulseFeed(YouTubePulseSource(), DatabaseLibrary(), DataStoreMemory(), current).also { it.alreadyPlayed(songIds) }
    }

    /** The restored pulse is in the player again: it is the one playing, unless a start came first. */
    fun resumed(feed: PulseFeed) {
        if (pending == null) _feed.value = feed
    }

    /** More songs, only while [feed] is still the one playing. */
    suspend fun more(feed: PulseFeed): List<SongItem> =
        if (_feed.value === feed) runCatching { next(feed) }.getOrDefault(emptyList()) else emptyList()

    /** The player moved on to an album, a playlist, a search result: the pulse is not playing. */
    fun stopped() {
        _feed.value = null
        if (pending != null) {
            pending = null
            _starting.value = false
        }
    }

    /** Likes already passed on to the feed, so each steers it once. */
    private val steeredBy = HashSet<String>()

    private suspend fun next(feed: PulseFeed, limit: Int = PulseFeed.BATCH): List<SongItem> =
        withContext(Dispatchers.IO) {
            lock.withLock {
                // A heart pressed anywhere (player, notification) on one of the pulse's songs steers it.
                runCatching { database.pulseRecentLikes(RECENT_LIKES) }.getOrDefault(emptyList())
                    .filter { it in feed.played && steeredBy.add(it) }
                    .forEach(feed::liked)
                // The schedule: the morning gives way to the day while the pulse plays on.
                feed.retune(storedSettings().at(LocalTime.now().hour))
                feed.next(limit)
            }
        }

    suspend fun liked(songId: String) {
        val feed = _feed.value ?: return
        lock.withLock { if (steeredBy.add(songId)) feed.liked(songId) }
    }

    /** "Not for me": remembered here and, signed in, told to YouTube Music too. */
    suspend fun disliked(song: MediaMetadata) {
        val item = SongItem(id = song.id, title = song.title, artists = song.artists.map { Artist(it.name, it.id) }, thumbnail = song.thumbnailUrl.orEmpty())
        val feed = _feed.value
        withContext(Dispatchers.IO) {
            if (feed != null) lock.withLock { feed.disliked(item) } else DataStoreMemory().let { it.save(it.load().dislike(item)) }
            YouTube.dislikeVideo(song.id)
        }
    }

    /** YouTube Music's own moods and genres, in the app's language; empty when it does not answer. */
    suspend fun moods(): List<PulseSettings.Mood> = withContext(Dispatchers.IO) { YouTubePulseSource.moods() }

    private inner class DatabaseLibrary : PulseFeed.Library {
        override suspend fun knownIds(): Set<String> = database.pulseKnownSongIds().toSet()

        override suspend fun seedIds(): List<String> {
            val likes = database.pulseRecentLikes(SEEDS)
            val played = database.pulseMostPlayed(LocalDateTime.now().minusDays(60), SEEDS)
            // Alternate, so neither the latest likes nor the old favourites take over.
            return (0 until maxOf(likes.size, played.size)).flatMap { listOfNotNull(likes.getOrNull(it), played.getOrNull(it)) }.distinct()
        }
    }

    private inner class DataStoreMemory : PulseFeed.MemoryStore {
        override suspend fun load(): PulseMemory = PulseMemory.fromJson(context.dataStore.data.first()[PulseFeedMemoryKey])

        override suspend fun save(memory: PulseMemory) {
            context.safeDataStoreEdit { it[PulseFeedMemoryKey] = memory.toJson() }
        }
    }

    private companion object {
        const val SEEDS = 25
        const val RECENT_LIKES = 5
    }
}
