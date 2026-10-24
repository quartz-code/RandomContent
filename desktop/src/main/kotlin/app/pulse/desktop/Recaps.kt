package app.pulse.desktop

import com.metrolist.music.pulse.recap.Play
import com.metrolist.music.pulse.recap.RecapCalculator
import com.metrolist.music.pulse.recap.RecapEntry
import com.metrolist.music.pulse.recap.RecapNumbers
import com.metrolist.music.pulse.recap.RecapPeriod
import com.metrolist.music.pulse.recap.listRecaps
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Pulse desktop: a month's or a year's recap from this computer's [History], with the phone's
 * calculator for the numbers. What the phone gets from its database (top songs and artists, new
 * songs) is counted here from the same plays.
 */
object Recaps {
    data class Song(val id: String, val title: String, val artist: String, val thumbnail: String?, val plays: Int, val ms: Long)

    /** An artist has no picture of their own here; their most played song's artwork stands in. */
    data class Artist(val name: String, val thumbnail: String?, val ms: Long)

    data class Recap(
        val period: RecapPeriod,
        val numbers: RecapNumbers,
        val songs: List<Song>,
        val artists: List<Artist>,
        val songCount: Int,
        val artistCount: Int,
        val newSongs: Int,
    )

    fun build(history: List<History.Entry>, period: RecapPeriod, zone: ZoneId = ZoneId.systemDefault()): Recap {
        val timed = history.map { it to it.at.toLocal(zone) }
        val inPeriod = timed.filter { (_, time) -> time >= period.start && time < period.end }.map { it.first }
        val numbers = RecapCalculator.numbers(timed.map { (e, time) -> Play(time, e.ms) }, period)

        val songs = inPeriod.groupBy { it.id }.map { (id, plays) ->
            val last = plays.last()
            Song(id, last.title, last.artist, last.thumbnail, plays.size, plays.sumOf { it.ms })
        }.sortedWith(compareByDescending<Song> { it.ms }.thenByDescending { it.plays })

        // Each artist of a song gets its time, as the phone counts artists.
        val byArtist = linkedMapOf<String, Long>()
        val artwork = mutableMapOf<String, Pair<String?, Long>>()
        for (song in songs) {
            for (name in artistsOf(song.artist)) {
                byArtist[name] = (byArtist[name] ?: 0L) + song.ms
                if ((artwork[name]?.second ?: -1L) < song.ms) artwork[name] = song.thumbnail to song.ms
            }
        }
        val artists = byArtist.entries.sortedByDescending { it.value }.map { (name, ms) -> Artist(name, artwork[name]?.first, ms) }

        val firstHeard = history.groupBy { it.id }.mapValues { (_, plays) -> plays.minOf { it.at }.toLocal(zone) }
        val newSongs = songs.count { song -> firstHeard[song.id]?.let { it >= period.start && it < period.end } == true }

        return Recap(period, numbers, songs.take(TOP), artists.take(TOP), songs.size, byArtist.size, newSongs)
    }

    /** Months and years with enough music for a recap, newest first, as the phone lists them. */
    fun list(history: List<History.Entry>, zone: ZoneId = ZoneId.systemDefault()): List<RecapEntry> {
        val months = history.groupBy { RecapPeriod.monthOf(it.at.toLocal(zone)) }.map { (period, plays) -> RecapEntry(period, plays.sumOf { it.ms }) }
        return listRecaps(months)
    }

    /** The recap to offer on start: last month's (and last year's in January), once each. */
    fun toOffer(history: List<History.Entry>, lastOffered: String?, now: LocalDateTime = LocalDateTime.now(), zone: ZoneId = ZoneId.systemDefault()): RecapPeriod? =
        RecapCalculator.toOffer(now, lastOffered) { period ->
            history.filter { it.at.toLocal(zone).let { t -> t >= period.start && t < period.end } }.sumOf { it.ms } / 60_000
        }

    internal fun artistsOf(joined: String): List<String> =
        joined.split(", ", " & ", " feat. ", " ft. ").map { it.trim() }.filter { it.isNotEmpty() }.distinct().ifEmpty { listOf(joined) }

    private fun Long.toLocal(zone: ZoneId): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zone)

    private const val TOP = 5
}
