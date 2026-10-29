package com.metrolist.music.pulse.feed

import android.content.Context
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
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

    suspend fun saveSettings(settings: PulseSettings) {
        context.safeDataStoreEdit { it[PulseFeedSettingsKey] = settings.toJson() }
    }

    /** A new pulse with the stored settings; it becomes "the one playing" once the player takes it. */
    suspend fun newFeed(): PulseFeed {
        _empty.value = false
        val current = PulseSettings.fromJson(context.dataStore.data.first()[PulseFeedSettingsKey])
        return PulseFeed(YouTubeSource(), DatabaseLibrary(), DataStoreMemory(), current)
    }

    fun playing(feed: PulseFeed) {
        _feed.value = feed
    }

    /** The player moved on to an album, a playlist, a search result: the pulse is not playing. */
    fun stopped() {
        _feed.value = null
    }

    /** Likes already passed on to the feed, so each steers it once. */
    private val steeredBy = HashSet<String>()

    suspend fun next(feed: PulseFeed, limit: Int = PulseFeed.BATCH): List<SongItem> =
        withContext(Dispatchers.IO) {
            lock.withLock {
                // A heart pressed anywhere (player, notification) on one of the pulse's songs steers it.
                runCatching { database.pulseRecentLikes(RECENT_LIKES) }.getOrDefault(emptyList())
                    .filter { it in feed.played && steeredBy.add(it) }
                    .forEach(feed::liked)
                feed.next(limit).also { if (feed.played.isEmpty()) _empty.value = it.isEmpty() }
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
    suspend fun moods(): List<PulseSettings.Mood> = withContext(Dispatchers.IO) {
        YouTube.moodAndGenres().getOrNull().orEmpty().flatMap { section ->
            section.items.map { PulseSettings.Mood(it.title, it.endpoint.browseId, it.endpoint.params) }
        }
    }

    private class YouTubeSource : PulseFeed.Source {
        private val endpoints = HashMap<String, WatchEndpoint>()

        override suspend fun radio(seedId: String, continuation: String?): PulseFeed.Page {
            val endpoint = endpoints[seedId] ?: WatchEndpoint(videoId = seedId, playlistId = "RDAMVM$seedId")
            val result = YouTube.next(endpoint, continuation).getOrThrow()
            endpoints[seedId] = result.endpoint
            return PulseFeed.Page(result.items, result.continuation)
        }

        override suspend fun personalMix(continuation: String?): PulseFeed.Page {
            val endpoint = endpoints[MIX] ?: WatchEndpoint(playlistId = MIX)
            val result = YouTube.next(endpoint, continuation).getOrThrow()
            endpoints[MIX] = result.endpoint
            return PulseFeed.Page(result.items, result.continuation)
        }

        override suspend fun moodSongs(mood: PulseSettings.Mood): List<SongItem> {
            val page = YouTube.browse(mood.browseId, mood.params).getOrThrow()
            val items = page.items.flatMap { it.items }
            val direct = items.filterIsInstance<SongItem>()
            // A mood page lists playlists: a few of them give the songs to start from.
            val fromPlaylists = items.filterIsInstance<PlaylistItem>().take(MOOD_PLAYLISTS).flatMap { playlist ->
                YouTube.playlist(playlist.id).getOrNull()?.songs.orEmpty().take(MOOD_SONGS_EACH)
            }
            return (direct + fromPlaylists).distinctBy { it.id }
        }

        override suspend fun popularSongs(): List<SongItem> {
            val charts = YouTube.getChartsPage().getOrNull()?.sections.orEmpty().flatMap { it.items }.filterIsInstance<SongItem>()
            return charts.ifEmpty { personalMix(null).items }
        }
    }

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
        /** YouTube Music's own endless mix of the account ("My Supermix"). */
        const val MIX = "RDTMAK5uy_kset8DisdE7LSD4TNjEVvrKRTmG7a56sY"
        const val SEEDS = 25
        const val RECENT_LIKES = 5
        const val MOOD_PLAYLISTS = 3
        const val MOOD_SONGS_EACH = 15
    }
}
