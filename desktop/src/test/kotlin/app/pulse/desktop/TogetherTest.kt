package app.pulse.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class TogetherTest {
    @Test
    fun findsTheRoomCodeInAnInvite() {
        val phone = "Заходи слушать вместе в Pulse 🎧\nОткрой Together → Войти, код комнаты: K7Q2M8XA\n\nСкопируй это сообщение — Pulse сам подставит код."
        assertEquals("K7Q2M8XA", TogetherSession.extractCode(phone))
        assertEquals("K7Q2M8XA", TogetherSession.extractCode("Open Together → Join, room code: K7Q2M8XA"))
        assertEquals("K7Q2M8XA", TogetherSession.extractCode("https://metrolist.cc/listen?code=k7q2m8xa"))
        assertEquals("K7Q2M8XA", TogetherSession.extractCode("  k7q2m8xa "))
        assertNull(TogetherSession.extractCode("привет, как дела"))
        assertNull(TogetherSession.extractCode(""))
    }

    @Test
    fun knocksOnTheHealthPage() {
        assertEquals("https://pulse-together.onrender.com/health", TogetherSession.healthUrl(TogetherSession.DEFAULT_SERVER))
        assertEquals("http://127.0.0.1:8080/health", TogetherSession.healthUrl("ws://127.0.0.1:8080/ws"))
        assertNull(TogetherSession.healthUrl("https://example.com"))
    }

    @Test
    fun songsTravelAsThePhonesTrackCards() {
        val song = Track("abc", "Title", "Artist", "https://i.ytimg.com/x.jpg", 215, suggestedBy = "Вика")
        val info = song.toTrackInfo()
        assertEquals(215_000L, info.duration)
        assertEquals(song, info.toTrack())
        // The server wants a duration; an unknown one goes as three minutes.
        assertEquals(180_000L, song.copy(durationSec = null).toTrackInfo().duration)
        assertNull(Track("x", "T", "A", null, null).toTrackInfo().toTrack().thumbnail)
    }

    /**
     * A host and a guest in one room on a real server. Runs when PULSE_TOGETHER_SERVER names one,
     * e.g. ws://127.0.0.1:8080/ws for metroserver started from server/ in this repository.
     */
    @Test
    fun aGuestFollowsTheHost() {
        val server = System.getenv("PULSE_TOGETHER_SERVER") ?: return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val hostPlayer = FakePlayer(scope)
        val guestPlayer = FakePlayer(scope)
        val host = TogetherSession(scope, hostPlayer, server)
        val guest = TogetherSession(scope, guestPlayer, server)
        val a = Track("aaaaaaaaaaa", "Song A", "Artist", null, 240)
        val b = Track("bbbbbbbbbbb", "Song B", "Artist", null, 200)
        val c = Track("ccccccccccc", "Song C", "Friend's pick", null, 180)
        try {
            runBlocking {
                host.create("Host")
                val code = until("room created") { host.state.value.roomCode }
                hostPlayer.playLocally(listOf(a, b), 0, atMs = 30_000)

                guest.join("Code: $code".let { TogetherSession.extractCode(it)!! }, "Guest")
                val ask = until("join request") { host.state.value.requests.firstOrNull() }
                assertEquals("Guest", ask.name)
                host.approve(ask.userId)
                until("guest in room") { guest.state.value.takeIf { it.inRoom } }
                assertEquals("Host", guestPlayer.state.value.followingHost)

                // The guest loads A, says so, and comes in where the host is.
                until("guest plays A") { guestPlayer.takeIf { it.state.value.current == a && it.livePlaying } }
                assertClose(hostPlayer.livePositionMs, guestPlayer.livePositionMs, "after joining")

                hostPlayer.pauseLocally()
                until("guest pauses") { guestPlayer.takeIf { !it.livePlaying } }
                assertClose(hostPlayer.livePositionMs, guestPlayer.livePositionMs, "paused")

                hostPlayer.resumeLocally()
                hostPlayer.seekLocally(120_000)
                until("guest seeks") { guestPlayer.takeIf { it.livePlaying && it.livePositionMs > 110_000 } }
                assertClose(hostPlayer.livePositionMs, guestPlayer.livePositionMs, "after a seek")

                hostPlayer.playLocally(listOf(a, b), 1, atMs = 0)
                until("guest plays B") { guestPlayer.takeIf { it.state.value.current == b && it.livePlaying } }
                assertClose(hostPlayer.livePositionMs, guestPlayer.livePositionMs, "next song")

                // A suggestion: the host takes it, and it plays right after the current song.
                guest.suggest(c)
                val suggestion = until("suggestion") { host.state.value.suggestions.firstOrNull() }
                assertEquals("Guest", suggestion.from)
                host.acceptSuggestion(suggestion.id)
                until("host queues it") { hostPlayer.upcoming().firstOrNull()?.takeIf { it.videoId == c.videoId } }
                until("guest told") { guest.state.value.notice?.takeIf { it == TogetherSession.Notice.SUGGESTION_ADDED } }
                until("guest sees it in the queue") { guestPlayer.upcoming().firstOrNull()?.takeIf { it.videoId == c.videoId } }

                // The host leaves; the guest's player takes over the room.
                host.leave()
                until("guest becomes host") { guest.state.value.takeIf { it.isHost } }
                assertNull(guestPlayer.state.value.followingHost)
                guest.leave()
                until("guest out") { guest.state.value.takeIf { it.phase == TogetherSession.Phase.IDLE } }
            }
        } finally {
            scope.cancel()
        }
    }

    /** A guest that could not fetch the song tries again on the host's next heartbeat. */
    @Test
    fun aGuestRetriesASongItCouldNotFetch() {
        val server = System.getenv("PULSE_TOGETHER_SERVER") ?: return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val hostPlayer = FakePlayer(scope)
        val guestPlayer = FakePlayer(scope, failLoads = 1)
        val host = TogetherSession(scope, hostPlayer, server)
        val guest = TogetherSession(scope, guestPlayer, server)
        try {
            runBlocking {
                host.create("Host")
                val code = until("room created") { host.state.value.roomCode }
                hostPlayer.playLocally(listOf(Track("aaaaaaaaaaa", "Song A", "Artist", null, 240)), 0, atMs = 10_000)
                guest.join(code, "Guest")
                val ask = until("join request") { host.state.value.requests.firstOrNull() }
                host.approve(ask.userId)
                until("guest plays after a retry") { guestPlayer.takeIf { it.livePlaying } }
                assertClose(hostPlayer.livePositionMs, guestPlayer.livePositionMs, "after the retry")
                host.leave()
                guest.leave()
            }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun aMissingRoomIsSaidSo() {
        val server = System.getenv("PULSE_TOGETHER_SERVER") ?: return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val guest = TogetherSession(scope, FakePlayer(scope), server)
            runBlocking {
                guest.join("ZZZZ2222", "Guest")
                until("refused") { guest.state.value.problem }.let { assertEquals(TogetherSession.Problem.ROOM_NOT_FOUND, it) }
            }
        } finally {
            scope.cancel()
        }
    }

    private fun assertClose(expected: Long, actual: Long, what: String) {
        // Two clocks, a network and polling in between: a quarter of a second is "together".
        assertTrue(abs(expected - actual) < 400, "$what: host at $expected ms, guest at $actual ms")
    }

    private suspend fun <T : Any> until(what: String, check: () -> T?): T =
        runCatching {
            withTimeout(15_000) {
                var value = check()
                while (value == null) {
                    delay(50)
                    value = check()
                }
                value
            }
        }.getOrElse { fail("timed out waiting for: $what") }
}

/** A player without sound: a clock that runs while "playing", and loading that takes a moment. */
private class FakePlayer(private val scope: CoroutineScope, private var failLoads: Int = 0) : RoomPlayer {
    override val state = MutableStateFlow(PlayerState())
    override val seeks = MutableSharedFlow<Long>(extraBufferCapacity = 8)
    private var baseMs = 0L
    private var startedAt: Long? = null
    private fun now() = System.nanoTime() / 1_000_000

    override val livePositionMs: Long get() = synchronized(this) { baseMs + (startedAt?.let { now() - it } ?: 0L) }
    override val livePlaying: Boolean get() = synchronized(this) { startedAt != null }

    override fun upcoming(): List<Track> = state.value.let { s -> s.queue.drop(s.index + 1) }

    override fun playNext(track: Track) = state.update { s -> s.copy(queue = s.queue.toMutableList().apply { add(s.index + 1, track) }) }

    override fun follow(hostName: String?) = state.update { it.copy(followingHost = hostName) }

    override fun roomLoad(track: Track, upcoming: List<Track>, onReady: () -> Unit, onFailed: () -> Unit) {
        synchronized(this) { baseMs = 0; startedAt = null }
        state.update { it.copy(queue = listOf(track) + upcoming, index = 0, loading = 0f, isPlaying = false) }
        scope.launch {
            delay(400)
            state.update { it.copy(loading = null) }
            // A song YouTube refused, the first [failLoads] times.
            if (failLoads-- > 0) onFailed() else onReady()
        }
    }

    override fun roomQueue(upcoming: List<Track>) = state.update { s -> s.copy(queue = listOfNotNull(s.current) + upcoming, index = 0) }

    override fun roomSeek(positionMs: Long) = synchronized(this) {
        baseMs = positionMs
        if (startedAt != null) startedAt = now()
    }

    override fun roomResume() {
        synchronized(this) { if (startedAt == null) startedAt = now() }
        state.update { it.copy(isPlaying = true) }
    }

    override fun roomPause() {
        synchronized(this) {
            baseMs = livePositionMs
            startedAt = null
        }
        state.update { it.copy(isPlaying = false) }
    }

    // What the user does on the host.
    fun playLocally(tracks: List<Track>, index: Int, atMs: Long) {
        synchronized(this) {
            baseMs = atMs
            startedAt = now()
        }
        state.update { it.copy(queue = tracks, index = index, loading = null, isPlaying = true) }
    }

    fun pauseLocally() = roomPause()
    fun resumeLocally() = roomResume()
    fun seekLocally(ms: Long) {
        roomSeek(ms)
        seeks.tryEmit(ms)
    }
}
