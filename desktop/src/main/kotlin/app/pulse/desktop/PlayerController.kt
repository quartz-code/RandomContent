package app.pulse.desktop

import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class Track(
    val videoId: String,
    val title: String,
    val artists: String,
    val thumbnail: String?,
    val durationSec: Int?,
    /** Who suggested it, in a Listen Together room. */
    val suggestedBy: String? = null,
) {
    /** YouTube Music thumbnails carry their size in the URL; the player wants a big one. */
    val artwork: String? get() = thumbnail?.let(::largeThumbnail)

    companion object {
        fun from(song: SongItem) = Track(
            videoId = song.id,
            title = song.title,
            artists = song.artists.joinToString { it.name },
            thumbnail = song.thumbnail,
            durationSec = song.duration,
        )

        internal fun largeThumbnail(url: String): String =
            url.replace(Regex("""=w\d+-h\d+"""), "=w544-h544")
                .replace(Regex("""/(default|mqdefault|hqdefault|sddefault)\.jpg"""), "/hqdefault.jpg")
    }
}

enum class RepeatMode { OFF, ALL, ONE }

data class PlayerState(
    val queue: List<Track> = emptyList(),
    val index: Int = -1,
    val isPlaying: Boolean = false,
    /** 0..1 while the song downloads; null when not loading. */
    val loading: Float? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val volume: Float = 0.8f,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val error: String? = null,
    /** In a Listen Together room as a guest: the host's name. The host drives; local controls are off. */
    val followingHost: String? = null,
) {
    val current: Track? get() = queue.getOrNull(index)
}

/**
 * Pulse desktop: the queue and everything between "play this" and sound — resolve the stream,
 * download it, hand it to [AudioEngine], move on when it ends. The next song is downloaded in
 * the background, so changing songs is instant.
 */
class PlayerController(private val scope: CoroutineScope, private val history: History? = null) : RoomPlayer {
    /** A queue that never ends ("My pulse"): asked for more songs as the queue runs low. */
    fun interface Stream {
        suspend fun more(): List<Track>
    }

    private val _stream = MutableStateFlow<Stream?>(null)
    /** The endless queue playing now, or null for an ordinary one. */
    val stream: StateFlow<Stream?> = _stream.asStateFlow()
    private var moreJob: Job? = null
    @Volatile private var afterMore: (() -> Unit)? = null

    private val engine = AudioEngine()
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()
    val levels = AudioLevels()
    private val _seeks = MutableSharedFlow<Long>(extraBufferCapacity = 8)
    override val seeks: SharedFlow<Long> = _seeks
    override val livePositionMs: Long get() = if (_state.value.loading == null) engine.positionMs else 0L
    override val livePlaying: Boolean get() = engine.isPlaying

    /** A guest of a room listens to the host's choices; buttons and media keys do nothing. */
    private val following: Boolean get() = _state.value.followingHost != null

    private var loadJob: Job? = null
    private var prefetchJob: Job? = null
    private var shuffleOrder: List<Int> = emptyList()

    /** The song being heard, for the recap history: only time with sound counts. */
    private class Listening(val track: Track, val startedAt: Long) {
        var heardMs = 0L
    }
    @Volatile private var listening: Listening? = null
    /** Songs in a row that could not be fetched; the queue stops instead of cycling through them for ever. */
    @Volatile private var failuresInRow = 0

    init {
        engine.volume = _state.value.volume
        engine.onSamples = levels::feed
        scope.launch {
            var tick = System.nanoTime()
            while (isActive) {
                val playing = engine.isPlaying
                val now = System.nanoTime()
                if (playing) listening?.let { it.heardMs += (now - tick) / 1_000_000 }
                tick = now
                _state.update { it.copy(isPlaying = playing, positionMs = if (it.loading == null) engine.positionMs else it.positionMs, durationMs = engine.durationMs.takeIf { d -> d > 0 } ?: it.durationMs) }
                delay(if (playing) 200 else 500)
            }
        }
    }

    fun playQueue(tracks: List<Track>, start: Int, stream: Stream? = null) {
        if (tracks.isEmpty() || following) return
        moreJob?.cancel()
        afterMore = null
        _stream.value = stream
        _state.update { it.copy(queue = tracks, index = start.coerceIn(tracks.indices), error = null) }
        rebuildShuffle()
        startCurrent()
    }

    fun enqueue(track: Track) {
        if (following) return
        val wasEmpty = _state.value.queue.isEmpty()
        _state.update { it.copy(queue = it.queue + track) }
        rebuildShuffle()
        if (wasEmpty) playAt(0)
    }

    override fun playNext(track: Track) {
        if (following) return
        val s = _state.value
        if (s.queue.isEmpty()) return enqueue(track)
        val insertAt = s.index + 1
        _state.update { it.copy(queue = it.queue.toMutableList().apply { add(insertAt, track) }) }
        // With shuffle on, "next" still means next: the order moves up past the new song, which goes
        // right after the current one, instead of being shuffled in somewhere.
        val shifted = shuffleOrder.map { if (it >= insertAt) it + 1 else it }.toMutableList()
        shifted.add(shifted.indexOf(s.index) + 1, insertAt)
        shuffleOrder = shifted
    }

    fun removeAt(position: Int) {
        val s = _state.value
        if (following || position !in s.queue.indices || position == s.index) return
        _state.update { it.copy(queue = it.queue.filterIndexed { i, _ -> i != position }, index = if (position < it.index) it.index - 1 else it.index) }
        rebuildShuffle()
    }

    fun playAt(position: Int) {
        if (following || position !in _state.value.queue.indices) return
        _state.update { it.copy(index = position, error = null) }
        startCurrent()
    }

    fun togglePlay() {
        val s = _state.value
        when {
            following || s.current == null -> Unit
            s.loading != null -> Unit
            engine.isPlaying -> engine.pause()
            engine.durationMs > 0 && s.positionMs < s.durationMs - 500 -> engine.resume()
            else -> startCurrent()
        }
        _state.update { it.copy(isPlaying = engine.isPlaying) }
    }

    fun next() {
        if (!following) step(+1, userAction = true)
    }

    fun previous() {
        if (following) return
        if (engine.positionMs > 3_000) seekTo(0) else step(-1, userAction = true)
    }

    fun seekTo(ms: Long) {
        if (following) return
        engine.seek(ms)
        _state.update { it.copy(positionMs = ms) }
        _seeks.tryEmit(ms)
    }

    fun setVolume(value: Float) {
        engine.volume = value.coerceIn(0f, 1f)
        _state.update { it.copy(volume = engine.volume) }
    }

    fun toggleShuffle() {
        if (following) return
        _state.update { it.copy(shuffle = !it.shuffle) }
        rebuildShuffle()
    }

    fun cycleRepeat() = _state.update {
        if (following) return@update it
        it.copy(repeat = when (it.repeat) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        })
    }

    fun close() {
        finishListening()
        engine.stop()
    }

    /** Writes the song just heard to the history, if it was heard long enough to count. */
    private fun finishListening() {
        val done = listening ?: return
        listening = null
        if (done.heardMs < History.MIN_PLAY_MS) return
        history?.add(History.Entry(done.track.videoId, done.track.title, done.track.artists, done.track.thumbnail, done.startedAt, done.heardMs))
    }

    override fun upcoming(): List<Track> {
        val s = _state.value
        if (s.index !in s.queue.indices) return emptyList()
        if (!s.shuffle) return s.queue.drop(s.index + 1)
        val at = shuffleOrder.indexOf(s.index)
        return shuffleOrder.drop(at + 1).mapNotNull { s.queue.getOrNull(it) }
    }

    // ---- a guest in a Listen Together room: the room decides, these do as told ----

    override fun follow(hostName: String?) {
        _state.update { it.copy(followingHost = hostName, shuffle = if (hostName != null) false else it.shuffle) }
        if (hostName != null) rebuildShuffle()
    }

    override fun roomLoad(track: Track, upcoming: List<Track>, onReady: () -> Unit, onFailed: () -> Unit) {
        _stream.value = null
        _state.update { it.copy(queue = listOf(track) + upcoming, index = 0, error = null) }
        rebuildShuffle()
        startCurrent(autoPlay = false, onReady = onReady, onFailed = onFailed)
    }

    override fun roomQueue(upcoming: List<Track>) {
        val current = _state.value.current ?: return
        _state.update { it.copy(queue = listOf(current) + upcoming, index = 0) }
        rebuildShuffle()
    }

    override fun roomSeek(positionMs: Long) {
        engine.seek(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
    }

    override fun roomResume() {
        engine.resume()
        _state.update { it.copy(isPlaying = engine.isPlaying) }
    }

    override fun roomPause() {
        engine.pause()
        _state.update { it.copy(isPlaying = false) }
    }

    /** For drawing the window in tests without the network. */
    internal fun showForPreview(preview: PlayerState) = _state.update { preview }

    // ---- inside ----

    private fun rebuildShuffle() {
        val s = _state.value
        val rest = s.queue.indices.filter { it != s.index }.shuffled()
        shuffleOrder = listOfNotNull(s.index.takeIf { it >= 0 }) + rest
    }

    /** Where [direction] leads from the current song, or null at the end of the queue. */
    private fun neighbour(direction: Int): Int? {
        val s = _state.value
        if (s.queue.isEmpty()) return null
        if (s.shuffle) {
            val at = shuffleOrder.indexOf(s.index)
            val n = at + direction
            return shuffleOrder.getOrNull(n) ?: if (s.repeat == RepeatMode.ALL) shuffleOrder.getOrNull(if (direction > 0) 0 else shuffleOrder.lastIndex) else null
        }
        val n = s.index + direction
        return when {
            n in s.queue.indices -> n
            s.repeat == RepeatMode.ALL -> if (direction > 0) 0 else s.queue.lastIndex
            else -> null
        }
    }

    private fun step(direction: Int, userAction: Boolean) {
        if (!userAction && _state.value.repeat == RepeatMode.ONE) {
            startCurrent()
            return
        }
        val target = neighbour(direction)
        // The end of an endless queue: wait for its next songs instead of stopping.
        if (target == null && direction > 0 && _stream.value != null) {
            loadMore { step(direction, userAction) }
            return
        }
        if (target == null) {
            engine.stop()
            _state.update { it.copy(isPlaying = false, positionMs = 0) }
            return
        }
        _state.update { it.copy(index = target, error = null) }
        startCurrent()
    }

    /**
     * Asks the endless queue for more and adds it at the end; [then] runs once they are in. One
     * request at a time: a slow answer is not asked for twice.
     */
    private fun loadMore(then: (() -> Unit)? = null) {
        val source = _stream.value ?: return
        // The queue ran out while a request was already on its way: go on when that one answers.
        if (then != null) afterMore = then
        if (moreJob?.isActive == true) return
        moreJob = scope.launch(Dispatchers.IO) {
            val more = runCatching { source.more() }.getOrDefault(emptyList())
            if (_stream.value !== source) return@launch
            val waiting = afterMore.also { afterMore = null }
            if (more.isEmpty()) {
                // Nothing came (no internet): the end, as for any queue.
                if (waiting != null) {
                    engine.stop()
                    _state.update { it.copy(isPlaying = false, positionMs = 0) }
                }
                return@launch
            }
            _state.update { it.copy(queue = it.queue + more) }
            // Shuffled or not, the new songs come after everything already queued.
            shuffleOrder = shuffleOrder + (shuffleOrder.size until _state.value.queue.size)
            waiting?.invoke()
        }
    }

    private fun startCurrent(autoPlay: Boolean = true, onReady: (() -> Unit)? = null, onFailed: (() -> Unit)? = null) {
        val track = _state.value.current ?: return
        // An endless queue tops itself up while a few songs are still ahead, as the phone does.
        if (_stream.value != null && _state.value.queue.size - _state.value.index <= STREAM_AHEAD) loadMore()
        loadJob?.cancel()
        engine.stop()
        levels.reset()
        // A new song, or the same one again: each time it starts is a play of its own.
        finishListening()
        listening = Listening(track, System.currentTimeMillis())
        _state.update { it.copy(positionMs = 0, durationMs = (track.durationSec ?: 0) * 1000L, loading = 0f, isPlaying = false, error = null) }
        loadJob = scope.launch(Dispatchers.IO) {
            val file = runCatching { fetch(track) { p -> _state.update { it.copy(loading = p) } } }
                .getOrElse { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    _state.update { it.copy(loading = null, error = describe(error)) }
                    onFailed?.invoke()
                    // A song that cannot be fetched should not stop the queue (in a room, the host moves
                    // on). Skipping, not "repeat one", and not round and round a queue that all fails.
                    failuresInRow++
                    delay(1_500)
                    if (_state.value.current == track && !following && failuresInRow < maxOf(3, _state.value.queue.size)) step(+1, userAction = true)
                    return@launch
                }
            failuresInRow = 0
            if (_state.value.current != track) return@launch
            _state.update { it.copy(loading = null) }
            engine.play(
                file,
                startMs = 0,
                onEnded = { scope.launch { if (_state.value.current == track && !following) step(+1, userAction = false) } },
                onError = { error -> _state.update { it.copy(error = describe(error), isPlaying = false) } },
                startPaused = !autoPlay,
            )
            onReady?.invoke()
            prefetchNext()
        }
    }

    private fun prefetchNext() {
        val next = neighbour(+1)?.let { _state.value.queue.getOrNull(it) } ?: return
        if (AudioDownloader.cached(next.videoId) != null) return
        prefetchJob?.cancel()
        prefetchJob = scope.launch(Dispatchers.IO) { runCatching { fetch(next) {} } }
    }

    private suspend fun fetch(track: Track, progress: (Float) -> Unit): File {
        AudioDownloader.cached(track.videoId)?.let { return it }
        val stream = StreamResolver.resolve(track.videoId)
        return AudioDownloader.download(track.videoId, stream) { done, total ->
            if (total != null && total > 0) progress((done.toFloat() / total).coerceIn(0f, 1f))
        }
    }

    private companion object {
        /** Songs left in an endless queue when it asks for more. */
        const val STREAM_AHEAD = 4
    }

    private fun describe(error: Throwable): String = when {
        error.message?.contains("403") == true -> Strings.errorBlocked
        error is javax.sound.sampled.LineUnavailableException || error is IllegalArgumentException && error.message?.contains("line", true) == true -> Strings.errorNoSound
        error is java.net.UnknownHostException || error is java.net.SocketTimeoutException -> Strings.errorNetwork
        else -> Strings.errorGeneric
    }
}
