package app.pulse.desktop

import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class DecoderTest {
    private fun fixture(name: String) = File(javaClass.getResource("/$name")!!.toURI())

    @Test
    fun opusInWebmDecodes() {
        val seconds = Decoder.decodeSeconds(fixture("sine_opus.webm"), 10.0)
        assertTrue(abs(seconds - 3.0) < 0.2, "decoded $seconds s")
    }

    @Test
    fun fragmentedAacDecodes() {
        // YouTube's AAC streams are fragmented MP4, the way DASH serves them.
        val seconds = Decoder.decodeSeconds(fixture("sine_aac_fragmented.m4a"), 10.0)
        assertTrue(abs(seconds - 3.0) < 0.2, "decoded $seconds s")
    }
}
