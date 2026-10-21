package com.metrolist.music.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramInboxParserTest {
    private fun parse(updates: String, chat: String = "42"): List<TelegramInboxParser.Item> =
        TelegramInboxParser.parse(Json.parseToJsonElement(updates).jsonArray as JsonArray, chat)

    @Test
    fun audioWithTagsAndCover() {
        val items = parse(
            """[{"update_id":7,"message":{"message_id":3,"chat":{"id":42},"audio":{"file_id":"F","file_unique_id":"U",
               "file_size":5000000,"duration":265,"title":"Чёрный день свободы","performer":"этажность",
               "file_name":"x.mp3","thumbnail":{"file_id":"T"}}}}]""",
        )
        val audio = items.single() as TelegramInboxParser.Item.Audio
        assertEquals(7L, audio.updateId)
        assertEquals(3L, audio.messageId)
        assertEquals("U", audio.fileUniqueId)
        assertEquals("Чёрный день свободы", audio.title)
        assertEquals("этажность", audio.performer)
        assertEquals(265, audio.durationSec)
        assertEquals("T", audio.thumbFileId)
    }

    @Test
    fun musicSentAsDocumentCountsAsAudio() {
        val byMime = parse("""[{"update_id":1,"message":{"message_id":1,"chat":{"id":42},"document":{"file_id":"a","mime_type":"audio/flac","file_name":"song.flac"}}}]""")
        val byName = parse("""[{"update_id":2,"message":{"message_id":2,"chat":{"id":42},"document":{"file_id":"b","mime_type":"application/octet-stream","file_name":"Кино - Группа крови.MP3"}}}]""")
        val notMusic = parse("""[{"update_id":3,"message":{"message_id":3,"chat":{"id":42},"document":{"file_id":"c","mime_type":"application/pdf","file_name":"doc.pdf"}}}]""")
        assertTrue(byMime.single() is TelegramInboxParser.Item.Audio)
        assertTrue(byName.single() is TelegramInboxParser.Item.Audio)
        assertTrue(notMusic.single() is TelegramInboxParser.Item.Skip)
    }

    @Test
    fun textIsQueryAndSlashIsCommand() {
        val items = parse(
            """[{"update_id":1,"message":{"message_id":1,"chat":{"id":42},"text":"/start@PulseBot"}},
                {"update_id":2,"message":{"message_id":2,"chat":{"id":42},"text":"  Кино — Группа крови "}}]""",
        )
        assertEquals("/start", (items[0] as TelegramInboxParser.Item.Command).command)
        assertEquals("Кино — Группа крови", (items[1] as TelegramInboxParser.Item.Query).text)
    }

    @Test
    fun otherChatsAreIgnoredButStillMoveTheOffset() {
        val items = parse(
            """[{"update_id":9,"message":{"message_id":1,"chat":{"id":777},"text":"spam"}},
                {"update_id":10,"edited_message":{"message_id":1,"chat":{"id":42},"text":"x"}}]""",
        )
        assertEquals(listOf(TelegramInboxParser.Item.Skip(9), TelegramInboxParser.Item.Skip(10)), items)
    }

    @Test
    fun channelPostsWork() {
        val items = parse("""[{"update_id":5,"channel_post":{"message_id":8,"chat":{"id":-1001234567890},"text":"Молчат Дома - Судно"}}]""", chat = "-1001234567890")
        assertEquals("Молчат Дома - Судно", (items.single() as TelegramInboxParser.Item.Query).text)
    }

    @Test
    fun fileNamesAndQueriesSplitIntoArtistAndTitle() {
        assertEquals("Кино" to "Группа крови", TelegramInboxParser.splitFileName("Кино - Группа крови.mp3"))
        assertEquals("Molchat Doma" to "Sudno", TelegramInboxParser.splitFileName("Molchat_Doma_–_Sudno.m4a"))
        assertEquals(null to "track01", TelegramInboxParser.splitFileName("track01.mp3"))
        assertEquals("Mr. Kitty" to "After Dark", TelegramInboxParser.splitQuery("Mr. Kitty — After Dark"))
        assertEquals(null to "группа крови", TelegramInboxParser.splitQuery("группа крови"))
    }

    @Test
    fun youtubeLinks() {
        assertEquals("dQw4w9WgXcQ", TelegramInboxParser.youtubeVideoId("https://music.youtube.com/watch?v=dQw4w9WgXcQ&si=abc"))
        assertEquals("dQw4w9WgXcQ", TelegramInboxParser.youtubeVideoId("смотри https://youtu.be/dQw4w9WgXcQ?t=10"))
        assertEquals("dQw4w9WgXcQ", TelegramInboxParser.youtubeVideoId("https://www.youtube.com/watch?feature=share&v=dQw4w9WgXcQ"))
        assertNull(TelegramInboxParser.youtubeVideoId("Кино — Группа крови"))
    }
}
