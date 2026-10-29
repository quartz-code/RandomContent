package app.pulse.desktop

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.pulse.importer.ImportedList
import com.metrolist.music.pulse.importer.ImportedTrack
import com.metrolist.music.pulse.importer.TrackMatcher
import com.metrolist.music.pulse.importer.VkMusicImport
import com.metrolist.music.pulse.importer.YandexMusicImport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pulse desktop: import from Yandex Music or VK, as on the phone — read the list, find each song on
 * YouTube Music with the phone's matcher, let the user check, then keep it. The computer has no
 * library of its own, so "keep" means the YouTube Music account: a new playlist or likes.
 */
class ImportModel(
    private val scope: CoroutineScope,
    private val matcher: suspend (ImportedTrack) -> TrackMatcher.Match = TrackMatcher::match,
    private val account: AccountWriter = YouTubeAccountWriter,
) {
    /** What saving writes to; the YouTube Music account, or a fake in tests. */
    interface AccountWriter {
        suspend fun createPlaylist(title: String): String
        suspend fun addToPlaylist(playlistId: String, videoId: String): Boolean
        suspend fun like(videoId: String): Boolean
    }

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
        data object Queue : Destination
        data class NewPlaylist(val name: String) : Destination
        data object Liked : Destination
    }

    enum class Problem { YANDEX_REGION, YANDEX_PRIVATE, BAD_LINK, NETWORK, VK_SESSION, VK_DENIED, EMPTY, SAVE_FAILED }

    sealed interface Phase {
        data object Idle : Phase
        data object Loading : Phase
        data class Matching(val done: Int, val total: Int) : Phase
        /** YouTube Music stopped answering; matching waits for [resume]. */
        data class Offline(val done: Int, val total: Int) : Phase
        data object Review : Phase
        data class Saving(val done: Int, val total: Int) : Phase
        data class Done(val added: Int, val destination: Destination) : Phase
        data class Failed(val problem: Problem, val detail: String? = null) : Phase
    }

    private var job: Job? = null
    private val _list = MutableStateFlow<ImportedList?>(null)
    val list: StateFlow<ImportedList?> = _list.asStateFlow()
    private val _rows = MutableStateFlow<List<Row>>(emptyList())
    val rows: StateFlow<List<Row>> = _rows.asStateFlow()
    private val _phase = MutableStateFlow<Phase>(Phase.Idle)
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    /** Saving failed: the review stays, with this said above the buttons, so nothing is lost. */
    private val _saveFailed = MutableStateFlow(false)
    val saveFailed: StateFlow<Boolean> = _saveFailed.asStateFlow()

    fun loadYandex(link: String) = load { YandexMusicImport.load(link) }

    fun loadVkMusic(vk: VkAccount.Saved) = load { VkMusicImport.loadMyMusic(vk.userId, vk.cookies) }

    fun loadVkPlaylist(vk: VkAccount.Saved, playlist: VkMusicImport.VkPlaylist) = load { VkMusicImport.loadPlaylist(vk.cookies, playlist) }

    fun load(read: suspend () -> ImportedList) {
        job?.cancel()
        _phase.value = Phase.Loading
        job = scope.launch {
            val imported = try {
                read()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _phase.value = failure(e)
                return@launch
            }
            if (imported.tracks.isEmpty()) {
                _phase.value = Phase.Failed(Problem.EMPTY)
                return@launch
            }
            start(imported)
        }
    }

    fun start(imported: ImportedList) {
        job?.cancel()
        _list.value = imported
        _rows.value = imported.tracks.mapIndexed { i, t -> Row(i, t) }
        matchPending()
    }

    /** After [Phase.Offline]: only songs not looked up yet are searched. */
    fun resume() = matchPending()

    private fun matchPending() {
        job?.cancel()
        val total = _rows.value.size
        val pending = _rows.value.filter { it.match == null }
        val done = AtomicInteger(total - pending.size)
        _phase.value = Phase.Matching(done.get(), total)
        job = scope.launch(Dispatchers.IO) {
            val failuresInRow = AtomicInteger(0)
            // A few searches at a time: fast enough, and gentle with YouTube.
            val gate = Semaphore(4)
            pending.map { row ->
                async {
                    gate.withPermit {
                        if (failuresInRow.get() >= OFFLINE_AFTER_FAILURES) return@withPermit
                        val match = try {
                            matcher(row.source).also { failuresInRow.set(0) }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: TrackMatcher.Unreachable) {
                            failuresInRow.incrementAndGet()
                            return@withPermit
                        } catch (e: Exception) {
                            TrackMatcher.Match(TrackMatcher.Status.NOT_FOUND, emptyList())
                        }
                        // The YouTube client swallows cancellation into its result: a search still
                        // running when the user stopped or loaded another list must not write.
                        ensureActive()
                        val best = match.best?.takeIf { match.status != TrackMatcher.Status.NOT_FOUND }
                        updateRow(row.index) { it.copy(match = match, chosen = best?.song, included = best != null) }
                        _phase.value = Phase.Matching(done.incrementAndGet(), total)
                    }
                }
            }.awaitAll()
            _phase.value = if (_rows.value.any { it.match == null }) Phase.Offline(done.get(), total) else Phase.Review
        }
    }

    /** Stops searching; what was not looked up counts as not found and can be searched by hand. */
    fun stopMatching() {
        job?.cancel()
        _rows.update { rows ->
            rows.map { if (it.match == null) it.copy(match = TrackMatcher.Match(TrackMatcher.Status.NOT_FOUND, emptyList())) else it }
        }
        _phase.value = Phase.Review
    }

    fun reset() {
        job?.cancel()
        _saveFailed.value = false
        _list.value = null
        _rows.value = emptyList()
        _phase.value = Phase.Idle
    }

    /** For drawing the window in tests without the network. */
    internal fun showForPreview(imported: ImportedList, previewRows: List<Row>, previewPhase: Phase) {
        _list.value = imported
        _rows.value = previewRows
        _phase.value = previewPhase
    }

    fun setIncluded(index: Int, included: Boolean) = updateRow(index) { it.copy(included = included && it.chosen != null) }

    fun setAllIncluded(included: Boolean) = _rows.update { rows -> rows.map { it.copy(included = included && it.chosen != null) } }

    fun choose(index: Int, song: SongItem) = updateRow(index) { it.copy(chosen = song, included = true, pickedByUser = true) }

    suspend fun search(index: Int, query: String): List<TrackMatcher.Candidate> {
        val row = _rows.value.getOrNull(index) ?: return emptyList()
        return TrackMatcher.search(query, row.source).sortedByDescending { it.score }
    }

    /** The songs that will be kept, in the source's order, each once. */
    fun chosenSongs(): List<SongItem> =
        _rows.value.filter { it.included && it.chosen != null }.mapNotNull { it.chosen }.distinctBy { it.id }

    /** [Destination.Queue] is handled by the caller (it is the player's); this keeps the rest. */
    fun save(destination: Destination, alreadyLiked: Set<String> = emptySet()) {
        val chosen = chosenSongs()
        if (chosen.isEmpty()) return
        if (destination == Destination.Queue) {
            _phase.value = Phase.Done(chosen.size, destination)
            return
        }
        job?.cancel()
        _saveFailed.value = false
        _phase.value = Phase.Saving(0, chosen.size)
        job = scope.launch(Dispatchers.IO) {
            var added = 0
            when (destination) {
                is Destination.NewPlaylist -> {
                    val name = destination.name.ifBlank { _list.value?.title ?: "Импорт" }
                    val playlistId = runCatching { account.createPlaylist(name) }.getOrNull()
                    if (playlistId == null) {
                        backToReview()
                        return@launch
                    }
                    chosen.forEachIndexed { i, song ->
                        if (account.addToPlaylist(playlistId, song.id)) added++
                        _phase.value = Phase.Saving(i + 1, chosen.size)
                        delay(ADD_DELAY_MS)
                    }
                }
                Destination.Liked -> {
                    // Oldest first, so the top of the source list ends up as the most recent like.
                    chosen.asReversed().forEachIndexed { i, song ->
                        if (song.id in alreadyLiked) {
                            added++
                        } else if (account.like(song.id)) {
                            added++
                            // YouTube rate-limits bursts of likes.
                            delay(LIKE_DELAY_MS)
                        }
                        _phase.value = Phase.Saving(i + 1, chosen.size)
                    }
                }
                Destination.Queue -> Unit
            }
            if (added == 0) backToReview() else _phase.value = Phase.Done(added, destination)
        }
    }

    private fun backToReview() {
        _saveFailed.value = true
        _phase.value = Phase.Review
    }

    private fun failure(e: Exception): Phase.Failed = when (e) {
        is YandexMusicImport.Failure.Region -> Phase.Failed(Problem.YANDEX_REGION)
        is YandexMusicImport.Failure.Private -> Phase.Failed(Problem.YANDEX_PRIVATE)
        is YandexMusicImport.Failure.BadLink, is VkMusicImport.Failure.BadLink -> Phase.Failed(Problem.BAD_LINK)
        is VkMusicImport.Failure.NotLoggedIn, is VkMusicImport.Failure.NeedsHandshake -> Phase.Failed(Problem.VK_SESSION)
        is VkMusicImport.Failure.AccessDenied -> Phase.Failed(Problem.VK_DENIED, e.answer)
        else -> Phase.Failed(Problem.NETWORK)
    }

    private fun updateRow(index: Int, block: (Row) -> Row) {
        _rows.update { rows -> rows.map { if (it.index == index) block(it) else it } }
    }

    private object YouTubeAccountWriter : AccountWriter {
        override suspend fun createPlaylist(title: String): String = YouTube.createPlaylist(title)
        override suspend fun addToPlaylist(playlistId: String, videoId: String) = YouTube.addToPlaylist(playlistId, videoId).isSuccess
        override suspend fun like(videoId: String) = YouTube.likeVideo(videoId, true).isSuccess
    }

    private companion object {
        const val OFFLINE_AFTER_FAILURES = 4
        const val ADD_DELAY_MS = 150L
        const val LIKE_DELAY_MS = 350L
    }
}
