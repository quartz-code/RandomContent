package com.metrolist.music.pulse.feed

import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PulseFeedTest {
    private fun song(id: String, artist: String = "Artist $id", title: String = "Song $id", video: Boolean = false) =
        SongItem(id = id, title = title, artists = listOf(Artist(artist, null)), thumbnail = "", musicVideoType = if (video) "MUSIC_VIDEO_TYPE_UGC" else null)

    private val none = PulseMemory()

    @Test
    fun mixerKeepsVarietyAndSkipsWhatWasTurnedDown() {
        val candidates = listOf(song("a1", "A"), song("a2", "A"), song("a3", "A"), song("b1", "B"), song("c1", "C"), song("d1", "D"), song("x", "X"))
        val memory = PulseMemory(dislikedSongs = listOf("x"))
        val picked = PulseMixer.pick(candidates, PulseSettings(), emptySet(), emptySet(), memory, emptyList(), 10, Random(1))
        assertFalse(picked.any { it.id == "x" })
        // Never the same artist twice in a row, and at most twice in a batch.
        picked.zipWithNext().forEach { (a, b) -> assertTrue("$a then $b", a.artists != b.artists) }
        assertEquals(2, picked.count { it.artists.first().name == "A" })
    }

    @Test
    fun characterAndLanguageFilter() {
        val ru = song("ru", "Кино", "Группа крови")
        val en = song("en", "Queen", "Bohemian Rhapsody")
        val known = setOf("en")
        val unfamiliar = PulseSettings(character = PulseSettings.Character.UNFAMILIAR)
        assertEquals(listOf("ru"), PulseMixer.pick(listOf(ru, en), unfamiliar, known, emptySet(), none, emptyList(), 5).map { it.id })
        assertEquals(listOf("ru"), PulseMixer.pick(listOf(ru, en), PulseSettings(language = PulseSettings.Language.RUSSIAN), known, emptySet(), none, emptyList(), 5).map { it.id })
        assertEquals(listOf("en"), PulseMixer.pick(listOf(ru, en), PulseSettings(language = PulseSettings.Language.FOREIGN), known, emptySet(), none, emptyList(), 5).map { it.id })
    }

    @Test
    fun mixLetsInOneFamiliarSongInFour() {
        val songs = (1..40).map { song("s$it") }
        val known = songs.take(20).map { it.id }.toSet()
        val picked = PulseMixer.pick(songs, PulseSettings(), known, emptySet(), none, emptyList(), 12, Random(3))
        assertEquals(12, picked.size)
        assertTrue(picked.count { it.id in known } <= 3)
        // "Favourite" leans the other way: familiar first.
        val favourite = PulseMixer.pick(songs, PulseSettings(character = PulseSettings.Character.FAVORITE), known, emptySet(), none, emptyList(), 12, Random(3))
        assertTrue(favourite.count { it.id in known } >= 10)
    }

    @Test
    fun songsComeBeforeVideosAndUploads() {
        val picked = PulseMixer.pick(listOf(song("v", video = true), song("s")), PulseSettings(), emptySet(), emptySet(), none, emptyList(), 2, Random(0))
        assertEquals("s", picked.first().id)
    }

    @Test
    fun twoRefusalsBanAnArtist() {
        val memory = PulseMemory().dislike(song("1", "Spam")).dislike(song("2", "spam "))
        assertEquals(setOf("spam"), memory.bannedArtists)
        assertFalse(PulseMixer.acceptable(song("3", "SPAM"), PulseSettings(), emptySet(), memory))
        assertEquals(memory, PulseMemory.fromJson(memory.toJson()))
    }

    /** Radios of 30 songs each, two pages per seed; each song named after its seed. */
    private class FakeSource : PulseFeed.Source {
        val radioCalls = mutableListOf<String>()
        var mixCalls = 0
        override suspend fun radio(seedId: String, continuation: String?): PulseFeed.Page {
            radioCalls += seedId
            val page = if (continuation == null) 0 else 1
            val items = (0 until 30).map { i -> SongItem(id = "$seedId-$page-$i", title = "T$i", artists = listOf(Artist("$seedId artist ${i % 10}", null)), thumbnail = "") }
            return PulseFeed.Page(items, if (page == 0) "next" else null)
        }
        override suspend fun personalMix(continuation: String?): PulseFeed.Page {
            mixCalls++
            return PulseFeed.Page((0 until 20).map { SongItem(id = "mix$mixCalls-$it", title = "M$it", artists = listOf(Artist("Mix $it", null)), thumbnail = "") }, "more")
        }
        override suspend fun moodSongs(mood: PulseSettings.Mood) = (0 until 10).map { SongItem(id = "mood$it", title = "Mood $it", artists = listOf(Artist("Mood artist $it", null)), thumbnail = "") }
        override suspend fun popularSongs() = (0 until 10).map { SongItem(id = "top$it", title = "Top $it", artists = listOf(Artist("Top artist $it", null)), thumbnail = "") }
    }

    private class FakeLibrary(private val seeds: List<String>, private val known: Set<String> = emptySet()) : PulseFeed.Library {
        override suspend fun knownIds() = known
        override suspend fun seedIds() = seeds
    }

    private class FakeMemory : PulseFeed.MemoryStore {
        var saved = PulseMemory()
        override suspend fun load() = saved
        override suspend fun save(memory: PulseMemory) { saved = memory }
    }

    @Test
    fun theFeedNeverRunsDryOrRepeats() = runBlocking {
        val source = FakeSource()
        val feed = PulseFeed(source, FakeLibrary(listOf("seedA", "seedB", "seedC")), FakeMemory(), PulseSettings(), Random(7))
        val all = (1..15).flatMap { feed.next() }
        assertTrue("got ${all.size}", all.size >= 15 * 10)
        assertEquals(all.size, all.map { it.id }.toSet().size)
        // Several radios take part, not just the first seed's.
        assertTrue(source.radioCalls.toSet().size >= 3)
    }

    @Test
    fun aLikeSteersAndARefusalIsRemembered() = runBlocking {
        val source = FakeSource()
        val memory = FakeMemory()
        val feed = PulseFeed(source, FakeLibrary(listOf("seedA", "seedB")), memory, PulseSettings(character = PulseSettings.Character.FAVORITE), Random(7))
        val first = feed.next()
        feed.liked("loved")
        source.radioCalls.clear()
        feed.next(30)
        assertEquals("loved", source.radioCalls.first())

        val bad = first.first()
        feed.disliked(bad)
        feed.disliked(SongItem(id = "other", title = "x", artists = bad.artists, thumbnail = ""))
        assertTrue(bad.id in memory.saved.dislikedSongs)
        val later = (1..5).flatMap { feed.next() }
        assertFalse(later.any { s -> s.artists.any { it.name == bad.artists.first().name } })
    }

    @Test
    fun withoutSeedsTheAccountMixPlaysAndAMoodStartsFromItsPlaylists() = runBlocking {
        val source = FakeSource()
        val fresh = PulseFeed(source, FakeLibrary(emptyList()), FakeMemory(), PulseSettings(), Random(1))
        assertTrue(fresh.next().all { it.id.startsWith("mix") })

        val moodFeed = PulseFeed(FakeSource(), FakeLibrary(listOf("seedA")), FakeMemory(), PulseSettings(mood = PulseSettings.Mood("Спокойное", "FEmusic_moods_and_genres_category", "p")), Random(1))
        assertTrue(moodFeed.next().any { it.id.startsWith("mood") })
    }

    @Test
    fun aRadioDoesNotOpenWithTheSongYouAlreadyKnow() = runBlocking {
        val source = object : PulseFeed.Source by FakeSource() {
            override suspend fun radio(seedId: String, continuation: String?) =
                PulseFeed.Page(listOf(SongItem(id = seedId, title = "Seed", artists = listOf(Artist("S", null)), thumbnail = "")) + (0 until 20).map { SongItem(id = "r$it", title = "R", artists = listOf(Artist("A$it", null)), thumbnail = "") }, null)
        }
        val feed = PulseFeed(source, FakeLibrary(listOf("liked1"), known = setOf("liked1")), FakeMemory(), PulseSettings(), Random(1))
        assertFalse(feed.next().any { it.id == "liked1" })
    }

    @Test
    fun whenNothingFitsThePulseFallsBackToWhatIsPopular() = runBlocking {
        // The seeds' radios are all English; the listener asked for Russian.
        val source = object : PulseFeed.Source by FakeSource() {
            override suspend fun popularSongs() = (0 until 15).map { SongItem(id = "ru$it", title = "Песня $it", artists = listOf(Artist("Исполнитель $it", null)), thumbnail = "") }
        }
        val feed = PulseFeed(source, FakeLibrary(listOf("seedA")), FakeMemory(), PulseSettings(language = PulseSettings.Language.RUSSIAN), Random(2))
        val batch = feed.next()
        assertTrue(batch.isNotEmpty())
        assertTrue(batch.all { PulseMixer.hasCyrillic(it.title) })
    }

    @Test
    fun settingsSurviveTheirJson() {
        val settings = PulseSettings(PulseSettings.Character.UNFAMILIAR, PulseSettings.Language.RUSSIAN, PulseSettings.Mood("Для тренировки", "FEmusic_moods_and_genres_category", "abc"))
        assertEquals(settings, PulseSettings.fromJson(settings.toJson()))
        assertEquals(PulseSettings(), PulseSettings.fromJson("не json"))
    }

    @Test
    fun theScheduleChangesTheCharacterAndMoodByTheHour() {
        val calm = PulseSettings.Mood("Спокойное", "FEmusic_moods_and_genres_category", "calm")
        val settings = PulseSettings(
            language = PulseSettings.Language.RUSSIAN,
            schedule = PulseSettings.Schedule(
                enabled = true,
                slots = mapOf(
                    PulseSettings.DayPart.MORNING to PulseSettings.Slot(mood = calm),
                    PulseSettings.DayPart.EVENING to PulseSettings.Slot(character = PulseSettings.Character.POPULAR),
                ),
            ),
        )
        assertEquals(calm, settings.at(8).mood)
        assertEquals(PulseSettings.Character.MIX, settings.at(8).character)
        assertEquals(PulseSettings.Character.POPULAR, settings.at(20).character)
        // The day has no changes of its own; the night runs past midnight.
        assertEquals(PulseSettings.Character.MIX, settings.at(13).character)
        assertEquals(PulseSettings.DayPart.NIGHT, PulseSettings.DayPart.at(2))
        assertEquals(PulseSettings.DayPart.NIGHT, PulseSettings.DayPart.at(23))
        assertEquals(PulseSettings.DayPart.MORNING, PulseSettings.DayPart.at(6))
        // Turned off, the schedule changes nothing; and it survives its JSON.
        assertEquals(PulseSettings.Character.MIX, settings.copy(schedule = settings.schedule.copy(enabled = false)).at(20).character)
        assertEquals(settings, PulseSettings.fromJson(settings.toJson()))
    }

    @Test
    fun retuningSwitchesTheSourceButKeepsWhatWasPlayed() = runBlocking {
        val feed = PulseFeed(FakeSource(), FakeLibrary(listOf("seedA")), FakeMemory(), PulseSettings(), Random(4))
        val before = (1..3).flatMap { feed.next() }
        assertFalse(before.any { it.id.startsWith("mood") })
        feed.retune(PulseSettings(mood = PulseSettings.Mood("Вечеринка", "FEmusic_moods_and_genres_category", "party")))
        val after = feed.next()
        assertTrue(after.any { it.id.startsWith("mood") })
        assertTrue(after.none { song -> before.any { it.id == song.id } })
    }
}
