package com.metrolist.music.ui.player

import com.metrolist.music.ui.player.PlayerTheme.Companion.toCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            artShape = PlayerTheme.ArtShape.COOKIE,
            artBorder = 3,
            motion = PlayerTheme.Motion.BARS,
            lyricsFont = PlayerTheme.Font.MONO,
            lyricsAccent = true,
            lyricsAlign = PlayerTheme.LyricsAlign.LEFT,
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
            artBorder = 40,
            buttons = PlayerTheme.BottomButton.entries + PlayerTheme.BottomButton.QUEUE,
            mini = MiniTheme(corner = 99),
        ).sanitized()
        assertEquals(PlayerTheme.ART_CIRCLE, wild.artCorner)
        assertEquals(8, wild.artMargin)
        assertEquals(36, wild.titleSize)
        assertEquals(8, wild.artBorder)
        assertEquals(PlayerTheme.MAX_BUTTONS, wild.buttons.size)
        assertEquals(32, wild.mini.corner)
    }

    @Test
    fun onlyRoundAndFlowerShapesSpin() {
        assertTrue(PlayerTheme(artCorner = PlayerTheme.ART_CIRCLE).canSpin)
        assertTrue(PlayerTheme(artShape = PlayerTheme.ArtShape.CLOVER).canSpin)
        assertFalse(PlayerTheme(artCorner = 20).canSpin)
        assertFalse(PlayerTheme(artShape = PlayerTheme.ArtShape.ARCH).canSpin)
        // A round corner value no longer means round once another shape is picked.
        assertFalse(PlayerTheme(artShape = PlayerTheme.ArtShape.ARCH, artCorner = PlayerTheme.ART_CIRCLE).artRound)
    }

    @Test
    fun fftBandsStayInRange() {
        val fft = ByteArray(1024) { (if (it % 2 == 0) 127 else -128).toByte() }
        val bands = FloatArray(PulseBands)
        fftToBands(fft, bands)
        assertTrue(bands.all { it in 0f..1f })
        assertTrue(bands.first() > 0.9f)
        fftToBands(ByteArray(1024), bands)
        assertTrue(bands.all { it == 0f })
    }

    @Test
    fun savedJsonReadsBackAndUnknownFieldsAreIgnored() {
        val theme = PlayerTheme.Light.copy(accent = 0xFFF06292)
        assertEquals(theme, PlayerTheme.fromJson(theme.toJson()))
        assertEquals(PlayerTheme.Clean.copy(light = true), PlayerTheme.fromJson("""{"light":true,"fromTheFuture":1}"""))
    }
}
