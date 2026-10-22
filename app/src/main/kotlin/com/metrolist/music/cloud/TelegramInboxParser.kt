package com.metrolist.music.cloud

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Pulse: reads what the user sent to their bot. Pure, so it is unit-tested; the network side is in
 * [TelegramInbox].
 */
internal object TelegramInboxParser {
    sealed interface Item {
        val updateId: Long

        /** A music file, sent or forwarded. */
        data class Audio(
            override val updateId: Long,
            val messageId: Long,
            val fileId: String,
            val fileUniqueId: String,
            val fileSize: Long?,
            val title: String?,
            val performer: String?,
            val fileName: String?,
            val durationSec: Int?,
            val thumbFileId: String?,
        ) : Item

        /** A song name or a link to find on YouTube Music. */
        data class Query(override val updateId: Long, val messageId: Long, val text: String) : Item

        data class Command(override val updateId: Long, val messageId: Long, val command: String) : Item

        /** Anything else, or a message from another chat: only moves the offset on. */
        data class Skip(override val updateId: Long) : Item
    }

    private val audioExtensions = setOf("mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav", "alac", "mp4")

    fun parse(updates: JsonArray, chatId: String): List<Item> = updates.mapNotNull { element ->
        val update = element as? JsonObject ?: return@mapNotNull null
        val updateId = update.long("update_id") ?: return@mapNotNull null
        val message = update.obj("message") ?: update.obj("channel_post") ?: return@mapNotNull Item.Skip(updateId)
        // Only the chat Pulse was connected to: anyone else who finds the bot is ignored.
        if (message.obj("chat")?.get("id")?.primitive() != chatId) return@mapNotNull Item.Skip(updateId)
        val messageId = message.long("message_id") ?: return@mapNotNull Item.Skip(updateId)

        val audio = message.obj("audio")
            ?: message.obj("document")?.takeIf { isAudioDocument(it) }
        if (audio != null) {
            val fileId = audio.str("file_id") ?: return@mapNotNull Item.Skip(updateId)
            return@mapNotNull Item.Audio(
                updateId = updateId,
                messageId = messageId,
                fileId = fileId,
                fileUniqueId = audio.str("file_unique_id") ?: fileId,
                fileSize = audio.long("file_size"),
                title = audio.str("title")?.takeIf { it.isNotBlank() },
                performer = audio.str("performer")?.takeIf { it.isNotBlank() },
                fileName = audio.str("file_name"),
                durationSec = audio.int("duration")?.takeIf { it > 0 },
                thumbFileId = (audio.obj("thumbnail") ?: audio.obj("thumb"))?.str("file_id"),
            )
        }

        val text = message.str("text")?.trim().orEmpty()
        when {
            text.isEmpty() -> Item.Skip(updateId)
            text.startsWith("/") -> Item.Command(updateId, messageId, text.substringBefore(' ').substringBefore('@').lowercase())
            else -> Item.Query(updateId, messageId, text)
        }
    }

    private fun isAudioDocument(document: JsonObject): Boolean {
        if (document.str("mime_type")?.startsWith("audio/") == true) return true
        val extension = document.str("file_name")?.substringAfterLast('.', "")?.lowercase()
        return extension != null && extension in audioExtensions
    }

    /** "Artist - Title.mp3" → (Artist, Title); without a dash the whole name is the title. */
    fun splitFileName(fileName: String): Pair<String?, String> {
        val base = fileName.substringBeforeLast('.').replace('_', ' ').trim()
        val parts = base.split(Regex("""\s+[-–—]\s+"""), limit = 2)
        return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            parts[0].trim() to parts[1].trim()
        } else {
            null to base
        }
    }

    private val youtubeId = Regex("""(?:youtube\.com/(?:watch\?(?:.*&)?v=|shorts/|embed/)|youtu\.be/)([A-Za-z0-9_-]{11})""")

    fun youtubeVideoId(text: String): String? = youtubeId.find(text)?.groupValues?.get(1)

    /** "Artist - Title" typed by hand, split the same way as file names. */
    fun splitQuery(text: String): Pair<String?, String> = splitFileName("$text.x")

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull
    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
    private fun JsonElement.primitive(): String? = (this as? JsonPrimitive)?.contentOrNull
}
