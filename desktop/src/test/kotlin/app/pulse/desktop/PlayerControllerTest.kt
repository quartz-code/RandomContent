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

    private fun withController(block: (PlayerController) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            block(PlayerController(scope))
        } finally {
            scope.cancel()
        }
    }
}
