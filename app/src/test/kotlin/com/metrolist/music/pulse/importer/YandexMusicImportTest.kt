package com.metrolist.music.pulse.importer

import com.metrolist.music.pulse.importer.YandexMusicImport.Source
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YandexMusicImportTest {
    private fun link(s: String) = YandexMusicImport.parseLink(s)

    @Test
    fun recognisesAllLinkKinds() {
        assertEquals(Source.UserPlaylist("kostya", "3"), link("https://music.yandex.ru/users/kostya/playlists/3?utm_source=web"))
        assertEquals(Source.Likes("kostya"), link("https://music.yandex.ru/users/kostya/tracks"))
        assertEquals(Source.Track("456", "123"), link("https://music.yandex.ru/album/123/track/456"))
        assertEquals(Source.Album("123"), link("https://music.yandex.com/album/123"))
        assertEquals(Source.Artist("79215"), link("https://music.yandex.ru/artist/79215/tracks"))
        assertEquals(Source.UuidPlaylist("lk.4a5e6f70-1b2c-4d3e-8f90-1234567890ab"), link("https://music.yandex.ru/playlists/lk.4a5e6f70-1b2c-4d3e-8f90-1234567890ab?ref_id=x"))
    }

    @Test
    fun findsLinkInsideShareText() {
        assertEquals(
            Source.UserPlaylist("kostya", "1001"),
            link("Слушай плейлист «Для дороги» в Яндекс Музыке https://music.yandex.ru/users/kostya/playlists/1001 🎧"),
        )
    }

    @Test(expected = YandexMusicImport.Failure.BadLink::class)
    fun rejectsOtherSites() {
        link("https://vk.com/music/playlist/1_2")
    }

    @Test
    fun parsesRichPlaylistAndCompletesBareIds() {
        val body = Json.parseToJsonElement(
            """{"title":"Для дороги","trackCount":3,"tracks":[
                {"id":1,"track":{"title":"Группа крови","durationMs":285000,"artists":[{"name":"Кино"}]}},
                {"id":2,"track":{"title":"Believer","version":"Live","durationMs":204500,"artists":[{"name":"Imagine Dragons"}]}},
                {"id":"3","albumId":"9"}
            ]}""",
        )
        var asked: List<String> = emptyList()
        val list = YandexMusicImport.parsePlaylist(body) { ids -> asked = ids; listOf(ImportedTrack("Кукушка", listOf("Кино"), 400)) }
        assertEquals("Для дороги", list.title)
        assertEquals(listOf("3:9"), asked)
        assertEquals(ImportedTrack("Группа крови", listOf("Кино"), 285), list.tracks[0])
        assertEquals(ImportedTrack("Believer (Live)", listOf("Imagine Dragons"), 204), list.tracks[1])
        assertEquals(3, list.tracks.size)
    }

    @Test(expected = YandexMusicImport.Failure.Private::class)
    fun hiddenPlaylistIsReportedAsPrivate() {
        YandexMusicImport.parsePlaylist(Json.parseToJsonElement("""{"title":"x","trackCount":5,"tracks":[]}""")) { emptyList() }
    }

    @Test
    fun parsesAlbumVolumes() {
        val list = YandexMusicImport.parseAlbum(
            Json.parseToJsonElement("""{"title":"Звезда по имени Солнце","volumes":[[{"title":"Звезда по имени Солнце","artists":[{"name":"Кино"}],"durationMs":225000}],[{"title":"Песня без слов","artists":[{"name":"Кино"}]}]]}"""),
        )
        assertEquals(2, list.tracks.size)
        assertTrue(list.tracks[1].durationSec == null)
    }
}
