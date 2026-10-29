package app.pulse.desktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.RandomAccessFile

/**
 * Pulse desktop: what was listened to on this computer, for recaps. One line per play, appended,
 * so a crash loses at most the song that was playing. The phone keeps its own history; the two
 * are not merged.
 */
class History(private val file: File) {
    @Serializable
    data class Entry(
        val id: String,
        val title: String,
        val artist: String,
        val thumbnail: String? = null,
        /** When the song started, milliseconds since 1970. */
        val at: Long,
        /** How long it was actually heard. */
        val ms: Long,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private var cache: List<Entry>? = null

    @Synchronized
    fun add(entry: Entry) {
        runCatching {
            file.parentFile?.mkdirs()
            // After a crash mid-write the last line has no end; start a fresh one rather than glue onto it.
            val broken = file.length() > 0 && RandomAccessFile(file, "r").use { it.seek(it.length() - 1); it.read() != '\n'.code }
            file.appendText((if (broken) "\n" else "") + json.encodeToString(Entry.serializer(), entry) + "\n")
        }
        cache = cache?.plus(entry)
    }

    @Synchronized
    fun all(): List<Entry> = cache ?: runCatching {
        // A half-written last line (the app was killed mid-write) is skipped, not fatal.
        file.readLines().mapNotNull { line -> line.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString(Entry.serializer(), it) }.getOrNull() } }
    }.getOrDefault(emptyList()).also { cache = it }

    companion object {
        /** A song counts once it was heard this long, as on the phone. */
        const val MIN_PLAY_MS = 30_000L

        val default: History by lazy { History(File(AppDirs.data, "history.jsonl")) }
    }
}
