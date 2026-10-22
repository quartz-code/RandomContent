package com.metrolist.music.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerThemeGalleryTest {
    @Test
    fun sameLookIsKeptOnceWithTheNewerName() {
        val first = SavedTheme(name = "Тема от друга", theme = PlayerTheme.OnAir, fromFriend = true)
        val again = SavedTheme(name = "Эфир Пети", theme = PlayerTheme.OnAir)
        val other = SavedTheme(name = "Светлая", theme = PlayerTheme.Light)
        val gallery = PlayerThemeGallery.add(PlayerThemeGallery.add(PlayerThemeGallery.add(emptyList(), first), other), again)
        assertEquals(listOf("Эфир Пети", "Светлая"), gallery.map { it.name })
    }

    @Test
    fun galleryRoundTripsAndRenamesAndRemoves() {
        val a = SavedTheme(name = "А", theme = PlayerTheme.Clean.copy(motion = PlayerTheme.Motion.WAVE))
        val b = SavedTheme(name = "Б", theme = PlayerTheme.Light, fromFriend = true)
        val gallery = PlayerThemeGallery.fromJson(PlayerThemeGallery.toJson(listOf(a, b)))
        assertEquals(listOf(a, b), gallery)
        assertEquals("В", PlayerThemeGallery.rename(gallery, a.id, "  В ").first().name)
        assertEquals("А", PlayerThemeGallery.rename(gallery, a.id, "   ").first().name)
        assertEquals(listOf(b), PlayerThemeGallery.remove(gallery, a.id))
        assertEquals(emptyList<SavedTheme>(), PlayerThemeGallery.fromJson("не json"))
    }

    @Test
    fun freeNamesCountUp() {
        val gallery = listOf(
            SavedTheme(name = "Тема от друга", theme = PlayerTheme.Clean),
            SavedTheme(name = "Тема от друга 2", theme = PlayerTheme.Light),
        )
        assertEquals("Тема от друга 3", PlayerThemeGallery.freeName(gallery, "Тема от друга"))
        assertEquals("Моя тема", PlayerThemeGallery.freeName(gallery, "Моя тема"))
    }

    @Test
    fun nightSpanCanCrossMidnight() {
        val from = 21 * 60
        val to = 7 * 60
        assertTrue(isNightTime(23 * 60, from, to))
        assertTrue(isNightTime(3 * 60, from, to))
        assertFalse(isNightTime(12 * 60, from, to))
        assertFalse(isNightTime(7 * 60, from, to))
        assertTrue(isNightTime(14 * 60, 13 * 60, 15 * 60))
        assertFalse(isNightTime(14 * 60, 9 * 60, 9 * 60))
    }

    @Test
    fun eveningVersionOfALightThemeIsDark() {
        val night = PlayerTheme.Light.nightVersion()
        assertFalse(night.light)
        assertEquals(PlayerTheme.Light.artShape, night.artShape)
        assertEquals(PlayerTheme.OnAir, PlayerTheme.OnAir.nightVersion())
    }
}
