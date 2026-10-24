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
class PlayerController(private val scope: CoroutineScope) : RoomPlayer {
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

    init {
        engine.volume = _state.value.volume
        engine.onSamples = levels::feed
        scope.launch {
            while (isActive) {
                val playing = engine.isPlaying
                _state.update { it.copy(isPlaying = playing, positionMs = if (it.loading == null) engine.positionMs else it.positionMs, durationMs = engine.durationMs.takeIf { d -> d > 0 } ?: it.durationMs) }
                delay(if (playing) 200 else 500)
            }
        }
    }

    fun playQueue(tracks: List<Track>, start: Int) {
        if (tracks.isEmpty() || following) return
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

    fun close() = engine.stop()

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

    override fun roomLoad(track: Track, upcoming: List<Track>, onReady: () -> Unit) {
        _state.update { it.copy(queue = listOf(track) + upcoming, index = 0, error = null) }
        rebuildShuffle()
        startCurrent(autoPlay = false, onReady = onReady)
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
        if (target == null) {
            engine.stop()
            _state.update { it.copy(isPlaying = false, positionMs = 0) }
            return
        }
        _state.update { it.copy(index = target, error = null) }
        startCurrent()
    }

    private fun startCurrent(autoPlay: Boolean = true, onReady: (() -> Unit)? = null) {
        val track = _state.value.current ?: return
        loadJob?.cancel()
        engine.stop()
        levels.reset()
        _state.update { it.copy(positionMs = 0, durationMs = (track.durationSec ?: 0) * 1000L, loading = 0f, isPlaying = false, error = null) }
        loadJob = scope.launch(Dispatchers.IO) {
            val file = runCatching { fetch(track) { p -> _state.update { it.copy(loading = p) } } }
                .getOrElse { error ->
                    _state.update { it.copy(loading = null, error = describe(error)) }
                    // A song that cannot be fetched should not stop the queue; in a room, the host moves on.
                    delay(1_500)
                    if (_state.value.current == track && !following) step(+1, userAction = false)
                    return@launch
                }
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

    private fun describe(error: Throwable): String = when {
        error.message?.contains("403") == true -> Strings.errorBlocked
        error is javax.sound.sampled.LineUnavailableException || error is IllegalArgumentException && error.message?.contains("line", true) == true -> Strings.errorNoSound
        error is java.net.UnknownHostException || error is java.net.SocketTimeoutException -> Strings.errorNetwork
        else -> Strings.errorGeneric
    }
}
