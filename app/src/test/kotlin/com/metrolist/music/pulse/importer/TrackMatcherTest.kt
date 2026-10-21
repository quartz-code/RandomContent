package com.metrolist.music.pulse.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMatcherTest {
    private fun score(src: ImportedTrack, title: String, artists: List<String>, duration: Int?) =
        TrackMatcher.score(src, title, artists, duration)

    @Test
    fun exactMatchIsConfident() {
        val s = score(ImportedTrack("Группа крови", listOf("Кино"), 285), "Группа крови", listOf("Кино"), 286)
        assertTrue("score=$s", s >= TrackMatcher.CONFIDENT)
    }

    @Test
    fun vkNoiseIsIgnored() {
        val src = ImportedTrack("Кино - Группа крови [новинка 2024] (Official Video) 🔥", listOf("Кино"), 285)
        val s = score(src, "Группа крови", listOf("Кино"), 287)
        assertTrue("score=$s", s >= TrackMatcher.CONFIDENT)
    }

    @Test
    fun latinAndCyrillicSpellingsMatch() {
        val s = score(ImportedTrack("Gruppa krovi", listOf("Kino"), null), "Группа крови", listOf("Кино"), 286)
        assertTrue("score=$s", s >= TrackMatcher.CONFIDENT)
    }

    @Test
    fun featuredArtistsInTitleAreIgnored() {
        val s = score(ImportedTrack("Lose Control feat. Someone", listOf("Teddy Swims"), 210), "Lose Control", listOf("Teddy Swims"), 211)
        assertTrue("score=$s", s >= TrackMatcher.CONFIDENT)
    }

    @Test
    fun otherSongBySameArtistIsRejected() {
        val s = score(ImportedTrack("Кукушка", listOf("Кино"), 400), "Группа крови", listOf("Кино"), 286)
        assertTrue("score=$s", s < TrackMatcher.POSSIBLE)
    }

    @Test
    fun differentVersionNeedsReview() {
        val src = ImportedTrack("Blinding Lights (Slowed + Reverb)", listOf("The Weeknd"), 260)
        val original = score(src, "Blinding Lights", listOf("The Weeknd"), 200)
        val same = score(src, "Blinding Lights (Slowed + Reverb)", listOf("The Weeknd"), 259)
        assertTrue("original=$original", original < TrackMatcher.CONFIDENT)
        assertTrue("same=$same", same >= TrackMatcher.CONFIDENT)
        assertTrue(same > original)
    }

    @Test
    fun wrongArtistLowersScore() {
        val right = score(ImportedTrack("Believer", listOf("Imagine Dragons"), 204), "Believer", listOf("Imagine Dragons"), 204)
        val wrong = score(ImportedTrack("Believer", listOf("Imagine Dragons"), 204), "Believer", listOf("Kane Brown"), 180)
        assertTrue("right=$right wrong=$wrong", right - wrong > 0.3)
        assertTrue("wrong=$wrong", wrong < TrackMatcher.CONFIDENT)
    }

    @Test
    fun cleanTitleDropsArtistPrefixAndNoise() {
        assertEquals("Группа крови", TrackMatcher.cleanTitle("Кино - Группа крови (Official Video)", listOf("Кино")))
        assertEquals("Song (Remix)", TrackMatcher.cleanTitle("Song [Remix] | клип 2024", listOf("X")))
    }

    @Test
    fun versionTags() {
        assertEquals(setOf("remix"), TrackMatcher.versionTags("Song (DJ Remix)"))
        assertEquals(setOf("fast"), TrackMatcher.versionTags("Song speed up"))
        assertEquals(emptySet<String>(), TrackMatcher.versionTags("Alive"))
    }

    @Test
    fun classifyOrdersAndRates() {
        assertEquals(TrackMatcher.Status.NOT_FOUND, TrackMatcher.classify(emptyList()).status)
    }
}
