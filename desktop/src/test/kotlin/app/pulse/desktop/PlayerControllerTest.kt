package app.pulse.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerControllerTest {
    private val songs = (1..8).map { Track("id$it", "Song $it", "Artist", null, 200) }
    private val pick = Track("pick", "Pick", "Friend", null, 200)

    @Test
    fun playNextIsNextEvenWhenShuffling() = withController { c ->
        c.showForPreview(PlayerState(queue = songs, index = 2))
        c.toggleShuffle()
        c.playNext(pick)
        val upcoming = c.upcoming()
        assertEquals("pick", upcoming.first().videoId)
        // Nothing lost or doubled: everything but the current song is still ahead.
        assertEquals((songs + pick).map { it.videoId }.toSet() - "id3", upcoming.map { it.videoId }.toSet())
        assertEquals(songs.size, upcoming.size)
    }

    @Test
    fun aGuestsButtonsDoNothing() = withController { c ->
        c.showForPreview(PlayerState(queue = songs, index = 0))
        c.follow("Host")
        c.playNext(pick)
        c.removeAt(3)
        c.toggleShuffle()
        assertEquals(songs, c.state.value.queue)
        assertEquals(false, c.state.value.shuffle)
        c.follow(null)
        c.playNext(pick)
        assertEquals("pick", c.upcoming().first().videoId)
    }

    @Test
    fun anEndlessQueueTopsItselfUpAndEndsWithTheNextQueue() = withController { c ->
        var asked = 0
        val endless = PlayerController.Stream {
            asked++
            (1..3).map { Track("more$asked-$it", "More", "Pulse", null, 200) }
        }
        c.playQueue(songs.take(2), 0, endless)
        // Two songs left of an endless queue: it asks for more at once and adds them at the end.
        waitFor { c.state.value.queue.size >= 5 }
        assertEquals(listOf("id1", "id2", "more1-1", "more1-2", "more1-3"), c.state.value.queue.take(5).map { it.videoId })
        assertEquals(endless, c.stream.value)
        // Any other queue is an ordinary one: the endless one stops being asked.
        c.playQueue(songs, 0)
        assertEquals(null, c.stream.value)
    }

    private fun waitFor(condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < until) { "timed out" }
            Thread.sleep(20)
        }
    }

    private fun withController(block: (PlayerController) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            block(PlayerController(scope))
        } finally {
            scope.cancel()
        }
    }
}
