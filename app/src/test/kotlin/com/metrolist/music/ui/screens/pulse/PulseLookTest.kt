package com.metrolist.music.ui.screens.pulse

import com.metrolist.music.ui.screens.pulse.PulseLook.Companion.toCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseLookTest {
    @Test
    fun aLookTravelsAsACodeInsideAMessage() {
        val look = PulseLook(PulseLook.Palette.OCEAN, artworkLights = false, shape = PulseLook.Shape.STAR, background = PulseLook.Background.ARTWORK, echoes = false, motion = PulseLook.Motion.LIVELY)
        val code = look.toCode()
        assertTrue(code.startsWith("PULSEWAVE1-"))
        assertEquals(look, PulseLook.fromCode("Смотри, какой у меня пульс: $code — вставь в Pulse"))
        assertNull(PulseLook.fromCode("PULSEWAVE1-broken"))
        assertNull(PulseLook.fromCode("PULSE1-a player theme, not a look"))
    }

    @Test
    fun storedLooksSurviveUnknownAndMissingFields() {
        assertEquals(PulseLook(), PulseLook.fromJson(null))
        assertEquals(PulseLook(), PulseLook.fromJson("не json"))
        // A newer build's option is ignored, an unknown value falls back to the default.
        assertEquals(PulseLook(shape = PulseLook.Shape.FLOWER), PulseLook.fromJson("""{"shape":"FLOWER","sparkles":true,"palette":"RAINBOW"}"""))
    }
}
