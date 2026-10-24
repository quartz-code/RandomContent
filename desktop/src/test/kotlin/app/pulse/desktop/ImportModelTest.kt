package app.pulse.desktop

import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.pulse.importer.ImportSource
import com.metrolist.music.pulse.importer.ImportedList
import com.metrolist.music.pulse.importer.ImportedTrack
import com.metrolist.music.pulse.importer.TrackMatcher
import com.metrolist.music.pulse.importer.YandexMusicImport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportModelTest {
    private fun song(id: String) = SongItem(id = id, title = "Song $id", artists = listOf(Artist("Artist", null)), thumbnail = "")

    private val list = ImportedList(
        "Мой плейлист", ImportSource.YANDEX,
        listOf(ImportedTrack("found", listOf("A")), ImportedTrack("unsure", listOf("A")), ImportedTrack("missing", listOf("A")), ImportedTrack("found2", listOf("A"))),
    )

    /** "found…" is a sure match, "unsure" a possible one, anything else is not on YouTube Music. */
    private val matcher: suspend (ImportedTrack) -> TrackMatcher.Match = { track ->
        when {
            track.title.startsWith("found") -> TrackMatcher.Match(TrackMatcher.Status.MATCHED, listOf(TrackMatcher.Candidate(song("yt_${track.title}"), 0.95)))
            track.title == "unsure" -> TrackMatcher.Match(TrackMatcher.Status.UNSURE, listOf(TrackMatcher.Candidate(song("yt_unsure"), 0.6), TrackMatcher.Candidate(song("yt_other"), 0.5)))
            else -> TrackMatcher.Match(TrackMatcher.Status.NOT_FOUND, emptyList())
        }
    }

    private class FakeAccount : ImportModel.AccountWriter {
        val playlists = mutableMapOf<String, MutableList<String>>()
        val likes = mutableListOf<String>()
        override suspend fun createPlaylist(title: String) = "pl_$title".also { playlists[it] = mutableListOf() }
        override suspend fun addToPlaylist(playlistId: String, videoId: String) = playlists[playlistId]!!.add(videoId)
        override suspend fun like(videoId: String) = likes.add(videoId)
    }

    @Test
    fun matchesThenKeepsTheChosenAsAPlaylist() = withModel { model, account ->
        model.start(list)
        until { model.phase.value == ImportModel.Phase.Review }
        val rows = model.rows.value
        assertEquals(listOf(TrackMatcher.Status.MATCHED, TrackMatcher.Status.UNSURE, TrackMatcher.Status.NOT_FOUND, TrackMatcher.Status.MATCHED), rows.map { it.status })
        // Sure and possible matches are ticked; a song that is not there cannot be.
        assertEquals(listOf(true, true, false, true), rows.map { it.included })
        model.setIncluded(2, true)
        assertEquals(false, model.rows.value[2].included)

        // The user prefers the other version, and finds the missing one by hand.
        model.choose(1, song("yt_other"))
        model.choose(2, song("yt_by_hand"))
        model.setIncluded(3, false)
        assertEquals(listOf("yt_found", "yt_other", "yt_by_hand"), model.chosenSongs().map { it.id })

        model.save(ImportModel.Destination.NewPlaylist("Из Яндекса"))
        until { model.phase.value is ImportModel.Phase.Done }
        assertEquals<ImportModel.Phase>(ImportModel.Phase.Done(3, ImportModel.Destination.NewPlaylist("Из Яндекса")), model.phase.value)
        assertEquals<List<String>?>(listOf("yt_found", "yt_other", "yt_by_hand"), account.playlists["pl_Из Яндекса"])
    }

    @Test
    fun likesOldestFirstAndSkipsWhatIsLikedAlready() = withModel { model, account ->
        model.start(list)
        until { model.phase.value == ImportModel.Phase.Review }
        model.save(ImportModel.Destination.Liked, alreadyLiked = setOf("yt_unsure"))
        until { model.phase.value is ImportModel.Phase.Done }
        assertEquals(listOf("yt_found2", "yt_found"), account.likes)
        assertEquals<ImportModel.Phase>(ImportModel.Phase.Done(3, ImportModel.Destination.Liked), model.phase.value)
    }

    @Test
    fun youtubeGoingQuietPausesAndResumeFinishes() {
        var online = false
        val flaky: suspend (ImportedTrack) -> TrackMatcher.Match = { track ->
            if (!online) throw TrackMatcher.Unreachable(null)
            matcher(track)
        }
        withModel(flaky) { model, _ ->
            model.start(list)
            until { model.phase.value is ImportModel.Phase.Offline }
            online = true
            model.resume()
            until { model.phase.value == ImportModel.Phase.Review }
            assertTrue(model.rows.value.all { it.match != null })
        }
    }

    @Test
    fun failuresAreNamed() = withModel { model, _ ->
        model.load { throw YandexMusicImport.Failure.Region() }
        until { model.phase.value is ImportModel.Phase.Failed }
        assertEquals(ImportModel.Problem.YANDEX_REGION, (model.phase.value as ImportModel.Phase.Failed).problem)

        model.load { ImportedList("Пусто", ImportSource.VK, emptyList()) }
        until { (model.phase.value as? ImportModel.Phase.Failed)?.problem == ImportModel.Problem.EMPTY }

        model.reset()
        assertEquals(ImportModel.Phase.Idle, model.phase.value)
        assertNull(model.list.value)
    }

    private fun withModel(match: suspend (ImportedTrack) -> TrackMatcher.Match = matcher, block: suspend (ImportModel, FakeAccount) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val account = FakeAccount()
        try {
            runBlocking { block(ImportModel(scope, match, account), account) }
        } finally {
            scope.cancel()
        }
    }

    private suspend fun until(check: () -> Boolean) = withTimeout(10_000) {
        while (!check()) delay(20)
    }
}
