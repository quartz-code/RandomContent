package com.metrolist.music.ui.player

import com.metrolist.music.ui.player.PlayerTheme.Companion.toCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerThemeTest {
    @Test
    fun everyPresetSurvivesTheShareCode() {
        PlayerTheme.presets.forEach { (_, preset) ->
            assertEquals(preset, PlayerTheme.fromCode(preset.toCode()))
        }
    }

    @Test
    fun codeIsShortAndFoundInsideAMessage() {
        val theme = PlayerTheme.OnAir.copy(
            slider = PlayerTheme.Slider.WAVE,
            artCorner = PlayerTheme.ART_CIRCLE,
            artSpin = true,
            buttons = listOf(PlayerTheme.BottomButton.SHUFFLE, PlayerTheme.BottomButton.QUEUE),
            mini = MiniTheme(progress = MiniTheme.Progress.LINE, showNext = true),
        )
        val code = theme.toCode()
        assertTrue("code is ${code.length} chars", code.length < 500)
        val message = "Моя тема плеера в Pulse 🎨\n$code\n\nСкопируй это сообщение и открой Pulse."
        assertEquals(theme, PlayerTheme.fromCode(message))
    }

    @Test
    fun brokenOrMissingCodesGiveNothing() {
        assertNull(PlayerTheme.fromCode(null))
        assertNull(PlayerTheme.fromCode("просто текст"))
        assertNull(PlayerTheme.fromCode("PULSE1-not_a_real_code"))
    }

    @Test
    fun valuesFromOutsideAreKeptDrawable() {
        val wild = PlayerTheme(
            artCorner = 400,
            artMargin = -5,
            titleSize = 90,
            buttons = PlayerTheme.BottomButton.entries + PlayerTheme.BottomButton.QUEUE,
            mini = MiniTheme(corner = 99),
        ).sanitized()
        assertEquals(PlayerTheme.ART_CIRCLE, wild.artCorner)
        assertEquals(8, wild.artMargin)
        assertEquals(36, wild.titleSize)
        assertEquals(PlayerTheme.MAX_BUTTONS, wild.buttons.size)
        assertEquals(32, wild.mini.corner)
    }

    @Test
    fun savedJsonReadsBackAndUnknownFieldsAreIgnored() {
        val theme = PlayerTheme.Light.copy(accent = 0xFFF06292)
        assertEquals(theme, PlayerTheme.fromJson(theme.toJson()))
        assertEquals(PlayerTheme.Clean.copy(light = true), PlayerTheme.fromJson("""{"light":true,"fromTheFuture":1}"""))
    }
}
