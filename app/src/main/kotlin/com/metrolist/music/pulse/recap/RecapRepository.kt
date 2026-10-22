package com.metrolist.music.pulse.recap

import com.metrolist.music.db.MusicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class RecapSong(val id: String, val title: String, val artist: String, val thumbnailUrl: String?, val plays: Int, val ms: Long)

data class RecapArtist(val id: String, val name: String, val thumbnailUrl: String?, val ms: Long)

data class RecapAlbum(val id: String, val title: String, val artist: String, val thumbnailUrl: String?, val ms: Long)

data class Recap(
    val period: RecapPeriod,
    val numbers: RecapNumbers,
    val songs: List<RecapSong>,
    val artists: List<RecapArtist>,
    val album: RecapAlbum?,
    val songCount: Int,
    val artistCount: Int,
    val newSongs: Int,
)

/** A month or year with enough music for a recap, for the list of recaps. */
data class RecapEntry(val period: RecapPeriod, val totalMs: Long)

object RecapRepository {
    suspend fun load(database: MusicDatabase, period: RecapPeriod): Recap = withContext(Dispatchers.IO) {
        val from = period.start
        val to = period.end
        // The stats queries take an inclusive end; a millisecond off keeps the next period out.
        val toInclusive = to.minusNanos(1_000_000)
        val plays = database.recapPlays(from, to).map { Play(it.timestamp, it.playTime) }
        Recap(
            period = period,
            numbers = RecapCalculator.numbers(plays, period),
            songs = database.mostPlayedSongsStats(from, limit = 5, toTimeStamp = toInclusive).first().map {
                RecapSong(
                    id = it.id,
                    title = it.title,
                    artist = it.artists.joinToString { a -> a.name }.ifBlank { it.artistName.orEmpty() },
                    thumbnailUrl = it.thumbnailUrl,
                    plays = it.songCountListened,
                    ms = it.timeListened ?: 0L,
                )
            },
            artists = database.mostPlayedArtists(from, limit = 5, toTimeStamp = toInclusive).first().map {
                RecapArtist(it.id, it.artist.name, it.artist.thumbnailUrl, (it.timeListened ?: 0).toLong())
            },
            album = database.mostPlayedAlbums(from, limit = 1, toTimeStamp = toInclusive).first().firstOrNull()?.let {
                RecapAlbum(it.id, it.album.title, it.artists.joinToString { a -> a.name }, it.album.thumbnailUrl, it.timeListened ?: 0L)
            },
            songCount = database.getUniqueSongCountInRange(from, toInclusive).first(),
            artistCount = database.getUniqueArtistCountInRange(from, toInclusive).first(),
            newSongs = database.recapNewSongs(from, to),
        )
    }

    /** Years and months with a recap, newest first; a year comes before its months. */
    suspend fun entries(database: MusicDatabase): List<RecapEntry> = withContext(Dispatchers.IO) {
        val months = database.recapMonths().mapNotNull { row -> RecapPeriod.parse(row.month)?.let { RecapEntry(it, row.totalMs) } }
        listRecaps(months)
    }

    suspend fun minutesIn(database: MusicDatabase, period: RecapPeriod): Long =
        withContext(Dispatchers.IO) { database.recapTotalMs(period.start, period.end) / 60_000 }
}

/** Groups monthly totals into the list shown to the user (pure, so it is tested). */
fun listRecaps(months: List<RecapEntry>): List<RecapEntry> {
    val enough = months.filter { it.totalMs / 60_000 >= RecapCalculator.MIN_MINUTES }
    return months.groupBy { it.period.year }.toSortedMap(compareByDescending { it }).flatMap { (year, inYear) ->
        val total = inYear.sumOf { it.totalMs }
        val yearEntry = RecapEntry(RecapPeriod(year), total).takeIf { total / 60_000 >= RecapCalculator.MIN_MINUTES }
        listOfNotNull(yearEntry) + enough.filter { it.period.year == year }.sortedByDescending { it.period.month }
    }
}
