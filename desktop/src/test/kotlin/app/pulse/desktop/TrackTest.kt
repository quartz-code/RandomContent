package app.pulse.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class TrackTest {
    @Test
    fun artworkAsksForTheLargeThumbnail() {
        assertEquals(
            "https://lh3.googleusercontent.com/abc=w544-h544-l90-rj",
            Track.largeThumbnail("https://lh3.googleusercontent.com/abc=w60-h60-l90-rj"),
        )
        assertEquals("https://i.ytimg.com/vi/x/hqdefault.jpg", Track.largeThumbnail("https://i.ytimg.com/vi/x/mqdefault.jpg"))
    }
}
