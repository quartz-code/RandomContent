package com.metrolist.music.pulse.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VkMusicImportTest {
    @Test
    fun parsesLoadSectionPage() {
        val body = """{"data":[{"type":"playlist","title":"Рок","list":[
            [456,123,"","Группа крови","Кино",285,0,0,"",0,2,"","[]","a/b/c/d//e/"],
            [457,123,"","Song &amp; Dance","A, B feat. C",200,0,0,"",0,2]
          ],"hasMore":true,"nextOffset":2000}]}"""
        val page = VkMusicImport.parseSectionResponse(body)!!
        assertEquals(2, page.tracks.size)
        assertEquals(ImportedTrack("Группа крови", listOf("Кино"), 285), page.tracks[0])
        assertEquals("Song & Dance", page.tracks[1].title)
        assertEquals(listOf("A", "B", "C"), page.tracks[1].artists)
        assertTrue(page.hasMore)
        assertEquals(2000, page.nextOffset)
        assertEquals("Рок", page.title)
    }

    @Test
    fun parsesAlAudioPayloadWithCommentPrefix() {
        val body = """<!--{"payload":[0,[{"list":[[1,5,"","Кукушка","Кино",400]],"hasMore":false}]]}"""
        val page = VkMusicImport.parseSectionResponse(body)!!
        assertEquals(listOf(ImportedTrack("Кукушка", listOf("Кино"), 400)), page.tracks)
        assertFalse(page.hasMore)
    }

    @Test
    fun emptyLibraryIsNotAnError() {
        val page = VkMusicImport.parseSectionResponse("""{"data":[{"list":[],"hasMore":false}]}""")
        assertEquals(0, page!!.tracks.size)
    }

    @Test
    fun loginPageIsNotMusic() {
        assertNull(VkMusicImport.parseSectionResponse("<html><form action=\"https://login.vk.ru\"></form></html>"))
    }

    @Test
    fun parsesPlaylistsPage() {
        val html = """
            <div class="audioPlaylistsPage__item"><a class="audioPlaylistsPage__itemLink" href="/audio?act=audio_playlist123_45&amp;access_hash=abc123">
            <div class="audioPlaylistsPage__title">Для дороги</div><div class="audioPlaylistsPage__author">Костя</div>
            <div class="audioPlaylistsPage__stats">12 аудиозаписей</div></a></div>
            <div class="audioPlaylistsPage__item"><a class="audioPlaylistsPage__itemLink" href="/audio?act=audio_playlist-2000_7">
            <div class="audioPlaylistsPage__title">Чарт &amp; новинки</div></a></div>
        """
        val list = VkMusicImport.parsePlaylistsHtml(html)
        assertEquals(2, list.size)
        assertEquals(VkMusicImport.VkPlaylist(123, 45, "abc123", "Для дороги", "Костя", 12), list[0])
        assertEquals(-2000L, list[1].ownerId)
        assertEquals("Чарт & новинки", list[1].title)
        assertNull(list[1].accessHash)
    }

    @Test
    fun parsesPlaylistLinks() {
        fun parse(s: String) = VkMusicImport.parsePlaylistLink(s)
        assertEquals(Triple(-147845620L, 2949L, "ab12cd"), parse("https://vk.com/music/playlist/-147845620_2949_ab12cd")!!.let { Triple(it.ownerId, it.id, it.accessHash) })
        assertEquals(Triple(123L, 4L, null), parse("https://vk.ru/music/album/123_4")!!.let { Triple(it.ownerId, it.id, it.accessHash) })
        assertEquals(Triple(123L, 4L, "zz9"), parse("https://m.vk.com/audio?act=audio_playlist123_4&access_hash=zz9")!!.let { Triple(it.ownerId, it.id, it.accessHash) })
        assertEquals(Triple(123L, 4L, "q1"), parse("https://vk.com/audios123?z=audio_playlist123_4%2Fq1")!!.let { Triple(it.ownerId, it.id, it.accessHash) })
        assertNull(parse("https://music.yandex.ru/album/1"))
    }

    @Test
    fun readsOAuthRedirect() {
        assertEquals(
            VkMusicImport.Redirect.Success(777),
            VkMusicImport.parseRedirect("https://oauth.vk.com/blank.html#access_token=x&expires_in=0&user_id=777"),
        )
        assertTrue(VkMusicImport.parseRedirect("https://oauth.vk.com/blank.html#error=access_denied&error_description=user+denied") is VkMusicImport.Redirect.Error)
        assertNull(VkMusicImport.parseRedirect("https://id.vk.ru/auth"))
    }

    @Test
    fun audioCookiesAreAddedOnce() {
        val cookies = VkMusicImport.withAudioCookies("remixsid=abc; remixlang=0;")
        assertTrue(cookies.contains("remixsid=abc"))
        assertTrue(cookies.contains("remixaudio_show_alert_today=0"))
        assertTrue(cookies.contains("remixmdevice=1920/1080/2/!!-!!!!"))
        assertEquals(cookies, VkMusicImport.withAudioCookies(cookies))
        assertEquals("remixaudio_show_alert_today=1; remixmdevice=1920/1080/2/!!-!!!!", VkMusicImport.withAudioCookies("remixaudio_show_alert_today=1"))
    }

    @Test
    fun cookiesFromTheSiteAreMerged() {
        val merged = VkMusicImport.mergeCookies(
            "remixsid=abc; remixlang=0",
            listOf("remixlang=3; expires=Fri, 01 Jan 2027 00:00:00 GMT; path=/; domain=.vk.ru", "remixstid=77; path=/", "remixsid=DELETED; path=/"),
        )
        assertEquals("remixlang=3; remixstid=77", merged)
    }

    @Test
    fun answerExcerptIsShortPlainText() {
        val html = "<html><head><script>var a = 1;</script><style>b{}</style></head><body><div class=\"x\">Музыка&nbsp;недоступна</div></body></html>"
        assertEquals("Музыка недоступна", VkMusicImport.answerExcerpt(html))
        assertEquals(300, VkMusicImport.answerExcerpt("я".repeat(1000)).length)
    }

    @Test
    fun loginHopIsRecognised() {
        // What VK sent to a signed-in user in October 2026, with the hashes shortened.
        val answer = """{"location":"https:\/\/login.vk.ru\/?role=pda_frame&_origin=https%3A%2F%2Fm.vk.ru&ip_h=401ece&to=aHR0cHM6Ly9tLnZrLnJ1L2F1ZGlv&lrt=P1uV&_phash=c018ce","version":"dc3975"}"""
        val url = VkMusicImport.handshakeUrl(answer)
        assertTrue(url!!.startsWith("https://login.vk.ru/?role=pda_frame"))
        assertNull(VkMusicImport.handshakeUrl("""{"location":"https://evil.example/login"}"""))
        assertNull(VkMusicImport.handshakeUrl("""{"data":[{"list":[]}]}"""))
        assertNull(VkMusicImport.handshakeUrl("<html>login</html>"))
    }
}
