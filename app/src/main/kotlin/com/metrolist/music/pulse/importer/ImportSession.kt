package com.metrolist.music.pulse.importer

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.DownloadUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pulse: one import from another service: match every track on YouTube Music, let the user review
 * the result, then save it to a new playlist or to Liked songs. Lives as a singleton so matching
 * keeps going while the user moves between screens.
 */
@Singleton
class ImportSession @Inject constructor(
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil,
) {
    data class Row(
        val index: Int,
        val source: ImportedTrack,
        val match: TrackMatcher.Match? = null,
        val chosen: SongItem? = null,
        val included: Boolean = false,
        val pickedByUser: Boolean = false,
    ) {
        val status: TrackMatcher.Status?
            get() = when {
                pickedByUser && chosen != null -> TrackMatcher.Status.MATCHED
                chosen == null && match != null -> TrackMatcher.Status.NOT_FOUND
                else -> match?.status
            }
    }

    sealed interface Destination {
        data class NewPlaylist(val name: String) : Destination
        data object Liked : Destination
    }

    sealed interface Phase {
        data object Idle : Phase
        data class Matching(val done: Int, val total: Int) : Phase
        /** YouTube Music did not answer; matching is paused until [ImportSession.resume]. */
        data class Offline(val done: Int, val total: Int) : Phase
        data object Review : Phase
        data class Saving(val done: Int, val total: Int) : Phase
        data class Done(val added: Int, val destination: Destination, val playlistId: String?) : Phase
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _list = MutableStateFlow<ImportedList?>(null)
    val list: StateFlow<ImportedList?> = _list.asStateFlow()

    private val _rows = MutableStateFlow<List<Row>>(emptyList())
    val rows: StateFlow<List<Row>> = _rows.asStateFlow()

    private val _phase = MutableStateFlow<Phase>(Phase.Idle)
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    fun start(imported: ImportedList) {
        job?.cancel()
        _list.value = imported
        _rows.value = imported.tracks.mapIndexed { i, t -> Row(i, t) }
        matchPending()
    }

    /** Continues after [Phase.Offline]: only tracks that were not looked up yet are searched. */
    fun resume() = matchPending()

    private fun matchPending() {
        job?.cancel()
        val total = _rows.value.size
        val pending = _rows.value.filter { it.match == null }
        val done = AtomicInteger(total - pending.size)
        _phase.value = Phase.Matching(done.get(), total)
        job = scope.launch {
            val failuresInRow = AtomicInteger(0)
            // A few searches at a time: fast enough, and gentle with YouTube.
            val gate = Semaphore(4)
            pending.map { row ->
                async {
                    gate.withPermit {
                        if (failuresInRow.get() >= OFFLINE_AFTER_FAILURES) return@withPermit
                        val match = try {
                            TrackMatcher.match(row.source).also { failuresInRow.set(0) }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: TrackMatcher.Unreachable) {
                            failuresInRow.incrementAndGet()
                            return@withPermit
                        } catch (e: Exception) {
                            Timber.w(e, "Matching failed for ${row.source.title}")
                            TrackMatcher.Match(TrackMatcher.Status.NOT_FOUND, emptyList())
                        }
                        val best = match.best?.takeIf { match.status != TrackMatcher.Status.NOT_FOUND }
                        updateRow(row.index) { it.copy(match = match, chosen = best?.song, included = best != null) }
                        _phase.value = Phase.Matching(done.incrementAndGet(), total)
                    }
                }
            }.awaitAll()
            _phase.value = if (_rows.value.any { it.match == null }) Phase.Offline(done.get(), total) else Phase.Review
        }
    }

    /** Stops searching; what was not looked up yet counts as not found and can be searched by hand. */
    fun cancel() {
        job?.cancel()
        _rows.update { rows ->
            rows.map { if (it.match == null) it.copy(match = TrackMatcher.Match(TrackMatcher.Status.NOT_FOUND, emptyList())) else it }
        }
        _phase.value = Phase.Review
    }

    fun reset() {
        job?.cancel()
        _list.value = null
        _rows.value = emptyList()
        _phase.value = Phase.Idle
    }

    fun setIncluded(index: Int, included: Boolean) =
        updateRow(index) { it.copy(included = included && it.chosen != null) }

    fun choose(index: Int, song: SongItem) =
        updateRow(index) { it.copy(chosen = song, included = true, pickedByUser = true) }

    suspend fun search(index: Int, query: String): List<TrackMatcher.Candidate> {
        val row = _rows.value.getOrNull(index) ?: return emptyList()
        return TrackMatcher.search(query, row.source).sortedByDescending { it.score }
    }

    fun save(destination: Destination, download: Boolean) {
        val chosen = _rows.value.filter { it.included && it.chosen != null }.mapNotNull { it.chosen }.distinctBy { it.id }
        if (chosen.isEmpty()) return
        job?.cancel()
        _phase.value = Phase.Saving(0, chosen.size)
        job = scope.launch {
            val metadata = chosen.map { it.toMediaMetadata() }
            metadata.forEach { runCatching { database.insert(it) } }
            var playlistId: String? = null
            when (destination) {
                is Destination.NewPlaylist -> {
                    val entity = PlaylistEntity(name = destination.name.ifBlank { _list.value?.title ?: "Импорт" })
                    database.insert(entity)
                    playlistId = entity.id
                    database.playlist(entity.id).firstOrNull()?.let { playlist ->
                        database.addSongsToPlaylist(playlist, chosen.map { it.id to null })
                    }
                    _phase.value = Phase.Saving(chosen.size, chosen.size)
                }
                Destination.Liked -> {
                    // Oldest first, so the top of the source list ends up as the most recent like.
                    chosen.asReversed().forEachIndexed { i, song ->
                        database.getSongByIdBlocking(song.id)?.song?.takeIf { !it.liked }?.let { entity ->
                            database.update(entity.toggleLike(syncToYouTube = false))
                            YouTube.likeVideo(song.id, true).onFailure { Timber.w(it, "Like failed for ${song.id}") }
                            // YouTube rate-limits bursts of likes.
                            delay(350)
                        }
                        _phase.value = Phase.Saving(i + 1, chosen.size)
                    }
                }
            }
            if (download) metadata.forEach { downloadUtil.download(it) }
            _phase.value = Phase.Done(chosen.size, destination, playlistId)
        }
    }

    private companion object {
        const val OFFLINE_AFTER_FAILURES = 4
    }

    private fun updateRow(index: Int, block: (Row) -> Row) {
        _rows.update { rows -> rows.map { if (it.index == index) block(it) else it } }
    }
}
