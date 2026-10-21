package app.pulse.player.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

data class LikedEntry(val id: String, val title: String, val artist: String, val durationSec: Int)

data class DownloadedTrack(
    val title: String,
    val artist: String,
    val album: String?,
    val durationSec: Int,
    val filePath: String,
    val coverPath: String?,
)

object YtDlp {
    const val LIKED_URL = "https://music.youtube.com/playlist?list=LM"
    private const val LIKED_VIDEOS_URL = "https://www.youtube.com/playlist?list=LL"

    @Volatile private var ready = false

    @Synchronized
    fun init(context: Context) {
        if (ready) return
        YoutubeDL.init(context.applicationContext)
        FFmpeg.init(context.applicationContext)
        ready = true
    }

    fun version(context: Context): String? = YoutubeDL.versionName(context)

    /** Returns true when a newer yt-dlp was installed. */
    fun update(context: Context): Boolean {
        init(context)
        return YoutubeDL.updateYoutubeDL(context.applicationContext, YoutubeDL.UpdateChannel.STABLE) ==
            YoutubeDL.UpdateStatus.DONE
    }

    fun musicDir(context: Context) = File(context.filesDir, "music").apply { mkdirs() }
    fun coverDir(context: Context) = File(context.filesDir, "covers").apply { mkdirs() }

    /**
     * The liked songs playlist, most recently liked first. Falls back to the plain YouTube "Liked videos"
     * list, which also receives every like made in YouTube Music.
     */
    fun fetchLiked(context: Context, cookies: File): List<LikedEntry> {
        init(context)
        val music = runCatching { fetchPlaylist(LIKED_URL, cookies) }
        music.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }
        val videos = runCatching { fetchPlaylist(LIKED_VIDEOS_URL, cookies) }
        videos.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }
        // Report the YouTube Music error: it is the list we actually wanted.
        music.exceptionOrNull()?.let { throw it }
        videos.exceptionOrNull()?.let { throw it }
        return emptyList()
    }

    private fun fetchPlaylist(url: String, cookies: File): List<LikedEntry> {
        val request = YoutubeDLRequest(url)
            .addOption("--flat-playlist")
            .addOption("--dump-single-json")
            .addOption("--cookies", cookies.absolutePath)
        val response = YoutubeDL.execute(request)
        val entries = JSONObject(response.out).optJSONArray("entries") ?: JSONArray()
        return (0 until entries.length()).mapNotNull { i ->
            val e = entries.optJSONObject(i) ?: return@mapNotNull null
            val id = e.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            LikedEntry(
                id = id,
                title = e.optString("title").ifBlank { id },
                artist = artistOf(e),
                durationSec = e.optDouble("duration", 0.0).toInt(),
            )
        }
    }

    fun download(
        context: Context,
        id: String,
        cookies: File,
        processId: String,
        onProgress: (Float) -> Unit,
    ): DownloadedTrack {
        init(context)
        val dir = musicDir(context)
        dir.listFiles { f -> f.name.startsWith("$id.") }?.forEach { it.delete() }
        // yt-dlp writes the cookie jar back on exit, so parallel downloads each get their own copy.
        val cookieCopy = File(context.cacheDir, "cookies-$processId.txt")
        cookies.copyTo(cookieCopy, overwrite = true)

        val request = YoutubeDLRequest("https://music.youtube.com/watch?v=$id")
            .addOption("-f", "bestaudio/best")
            .addOption("-o", "${dir.absolutePath}/%(id)s.%(ext)s")
            .addOption("--cookies", cookieCopy.absolutePath)
            .addOption("--no-playlist")
            .addOption("--no-mtime")
            .addOption("--no-simulate")
            .addOption("--progress")
            .addOption("--newline")
            .addOption(
                "--print",
                "after_move:%(.{id,title,track,artist,artists,creator,album,duration,thumbnail,uploader,channel})j",
            )
        val response = try {
            YoutubeDL.execute(request, processId) { progress, _, _ -> onProgress(progress / 100f) }
        } finally {
            cookieCopy.delete()
        }

        val info = response.out.lineSequence().map { it.trim() }.lastOrNull { it.startsWith("{") }
            ?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject()
        val file = dir.listFiles { f -> f.name.startsWith("$id.") && !f.name.endsWith(".part") }?.firstOrNull()
            ?: throw YoutubeDLException("Файл не найден после скачивания")

        val artist = artistOf(info)
        val title = cleanTitle(info.optString("track").ifBlank { info.optString("title") }.ifBlank { id }, artist)
        val cover = info.optString("thumbnail").takeIf { it.isNotBlank() }
            ?.let { runCatching { saveCover(it, File(coverDir(context), "$id.jpg")) }.getOrNull() }
            ?: runCatching {
                saveCover("https://i.ytimg.com/vi/$id/hqdefault.jpg", File(coverDir(context), "$id.jpg"))
            }.getOrNull()

        return DownloadedTrack(
            title = title,
            artist = artist,
            album = info.optString("album").takeIf { it.isNotBlank() },
            durationSec = info.optDouble("duration", 0.0).toInt(),
            filePath = file.absolutePath,
            coverPath = cover,
        )
    }

    fun cancel(processId: String) {
        runCatching { YoutubeDL.destroyProcessById(processId) }
    }

    /** First "ERROR:" line from yt-dlp output, which is what is worth showing to a human. */
    fun shortError(t: Throwable): String {
        val msg = t.message.orEmpty()
        return msg.lineSequence().firstOrNull { it.startsWith("ERROR:") }?.removePrefix("ERROR:")?.trim()
            ?: msg.lineSequence().lastOrNull { it.isNotBlank() }?.take(300)
            ?: t.javaClass.simpleName
    }

    private fun artistOf(o: JSONObject): String {
        o.optJSONArray("artists")?.let { arr ->
            val names = (0 until arr.length()).mapNotNull { i ->
                when (val v = arr.opt(i)) {
                    is String -> v
                    is JSONObject -> v.optString("name").takeIf { it.isNotBlank() }
                    else -> null
                }
            }
            if (names.isNotEmpty()) return names.joinToString(", ")
        }
        for (key in listOf("artist", "creator", "channel", "uploader")) {
            val v = o.optString(key)
            if (v.isNotBlank() && v != "null") return v.removeSuffix(" - Topic").trim()
        }
        return "Неизвестный исполнитель"
    }

    /** Music videos are often titled "Artist - Song (Official Video)". */
    private fun cleanTitle(title: String, artist: String): String {
        var t = title
        val prefix = "$artist - "
        if (t.startsWith(prefix, ignoreCase = true)) t = t.substring(prefix.length)
        t = t.replace(Regex("""\s*[(\[](official\s+)?(music\s+)?(video|audio|lyric video|lyrics|visualizer)[)\]]""", RegexOption.IGNORE_CASE), "")
        return t.trim().ifBlank { title }
    }

    /** Downloads the thumbnail and center-crops it to a square cover. */
    private fun saveCover(url: String, out: File): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        val bitmap = conn.inputStream.use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalStateException("bad image")
        val side = min(bitmap.width, bitmap.height)
        val square = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
        val scaled = if (side > 720) Bitmap.createScaledBitmap(square, 720, 720, true) else square
        out.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return out.absolutePath
    }
}
