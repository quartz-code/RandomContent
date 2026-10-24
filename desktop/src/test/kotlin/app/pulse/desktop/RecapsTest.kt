package app.pulse.desktop

import com.metrolist.music.pulse.recap.PartOfDay
import com.metrolist.music.pulse.recap.RecapPeriod
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecapsTest {
    private val utc = ZoneOffset.UTC
    private fun at(y: Int, m: Int, d: Int, h: Int) = LocalDateTime.of(y, m, d, h, 0).toInstant(utc).toEpochMilli()
    private fun play(id: String, artist: String, time: Long, minutes: Long) =
        History.Entry(id, "Song $id", artist, "https://i.ytimg.com/vi/$id/mqdefault.jpg", time, minutes * 60_000)

    private val history = listOf(
        // Heard first in September, so it is not new in October.
        play("old", "Кино", at(2026, 9, 28, 20), 20),
        play("old", "Кино", at(2026, 10, 1, 21), 30),
        play("old", "Кино", at(2026, 10, 2, 21), 30),
        play("duet", "Сплин, Би-2", at(2026, 10, 2, 9), 25),
        play("fresh", "Би-2", at(2026, 10, 3, 23), 10),
    )

    @Test
    fun aMonthFromThisComputersPlays() {
        val recap = Recaps.build(history, RecapPeriod(2026, 10), utc)
        assertEquals(95L, recap.numbers.minutes)
        assertEquals(4, recap.numbers.plays)
        assertEquals(listOf("old", "duet", "fresh"), recap.songs.map { it.id })
        assertEquals(2, recap.songs.first().plays)
        // A duet counts for both; Би-2 has the duet and their own song.
        assertEquals(listOf("Кино" to 60L, "Би-2" to 35L, "Сплин" to 25L), recap.artists.map { it.name to it.ms / 60_000 })
        assertEquals(3, recap.songCount)
        assertEquals(3, recap.artistCount)
        assertEquals(2, recap.newSongs)
        assertEquals(3, recap.numbers.longestStreak)
        assertEquals(PartOfDay.EVENING, recap.numbers.favouritePart)
        // The artist's picture is their most played song's artwork.
        assertEquals("https://i.ytimg.com/vi/duet/mqdefault.jpg", recap.artists.first { it.name == "Сплин" }.thumbnail)
    }

    @Test
    fun onlyMonthsWithHalfAnHourAreListedAndOffered() {
        val entries = Recaps.list(history, utc)
        // September has 20 minutes: too little for its own recap, but it counts toward the year.
        assertEquals(listOf(RecapPeriod(2026), RecapPeriod(2026, 10)), entries.map { it.period })
        assertEquals(RecapPeriod(2026, 10), Recaps.toOffer(history, null, LocalDateTime.of(2026, 11, 2, 12, 0), utc))
        assertNull(Recaps.toOffer(history, "2026-10", LocalDateTime.of(2026, 11, 2, 12, 0), utc))
        assertNull(Recaps.toOffer(history, null, LocalDateTime.of(2026, 10, 20, 12, 0), utc))
    }

    @Test
    fun theHistoryFileSurvivesAHalfWrittenLine() {
        val file = File(Files.createTempDirectory("pulse-history").toFile(), "history.jsonl")
        val first = History(file)
        history.forEach(first::add)
        file.appendText("{\"id\":\"cut")
        val reopened = History(file)
        assertEquals(history, reopened.all())
        reopened.add(play("next", "Кино", at(2026, 10, 5, 12), 5))
        assertEquals(history.size + 1, History(file).all().size)
    }

    @Test
    fun artistsAreSplitAsTheSongShowsThem() {
        assertEquals(listOf("Сплин", "Би-2"), Recaps.artistsOf("Сплин, Би-2"))
        assertEquals(listOf("Artist", "Guest"), Recaps.artistsOf("Artist feat. Guest"))
        assertEquals(listOf("Solo"), Recaps.artistsOf("Solo"))
    }
}
