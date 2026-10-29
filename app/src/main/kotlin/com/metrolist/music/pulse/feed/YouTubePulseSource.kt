package com.metrolist.music.pulse.feed

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint

/**
 * Pulse: where "My pulse" finds songs — YouTube Music's own radios, the account's mix, the mood
 * pages and the charts. Plain innertube calls, so the phone and the computer share it.
 */
class YouTubePulseSource : PulseFeed.Source {
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

    companion object {
        /** YouTube Music's own endless mix of the account ("My Supermix"). */
        private const val MIX = "RDTMAK5uy_kset8DisdE7LSD4TNjEVvrKRTmG7a56sY"
        private const val MOOD_PLAYLISTS = 3
        private const val MOOD_SONGS_EACH = 15

        /** YouTube Music's own moods and genres, in the language set in YouTube; empty when it does not answer. */
        suspend fun moods(): List<PulseSettings.Mood> =
            YouTube.moodAndGenres().getOrNull().orEmpty().flatMap { section ->
                section.items.map { PulseSettings.Mood(it.title, it.endpoint.browseId, it.endpoint.params) }
            }
    }
}
