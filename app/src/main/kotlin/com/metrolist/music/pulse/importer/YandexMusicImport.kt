package com.metrolist.music.pulse.importer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Pulse: reads a public Yandex Music playlist, album, track, artist or liked list so its tracks can
 * be matched on YouTube Music. Uses the public api.music.yandex.net endpoints the way the
 * MarshalX/yandex-music-api client does; no account is needed for public lists.
 */
object YandexMusicImport {
    sealed class Failure(message: String) : IOException(message) {
        /** Yandex answers 451 to non-Russian addresses, e.g. when the phone is behind a VPN. */
        class Region : Failure("region")
        class Private : Failure("private")
        class BadLink : Failure("bad link")
        class Network(cause: Throwable) : Failure(cause.message ?: "network")
    }

    internal sealed interface Source {
        data class UserPlaylist(val owner: String, val kind: String) : Source
        data class UuidPlaylist(val uuid: String) : Source
        data class Album(val id: String) : Source
        data class Track(val trackId: String, val albumId: String) : Source
        data class Artist(val id: String) : Source
        data class Likes(val owner: String) : Source
    }

    private const val API = "https://api.music.yandex.net"
    private const val TRACKS_BATCH = 100
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val linkInText = Regex("""https?://music\.yandex\.[a-z]+/\S+""", RegexOption.IGNORE_CASE)
    private val userPlaylist = Regex("""/users/([^/?#]+)/playlists/(\d+)""")
    private val userLikes = Regex("""/users/([^/?#]+)/tracks(?:[/?#]|$)""")
    private val trackInAlbum = Regex("""/album/(\d+)/track/(\d+)""")
    private val album = Regex("""/album/(\d+)""")
    private val artist = Regex("""/artist/(\d+)""")
    private val uuidPlaylist = Regex("""/playlists?/([A-Za-z0-9.\-]{6,})""")

    fun isYandexLink(text: String) = text.contains("music.yandex.", ignoreCase = true)

    suspend fun load(text: String): ImportedList = withContext(Dispatchers.IO) {
        when (val source = parseLink(text)) {
            is Source.UserPlaylist -> parsePlaylist(get("/users/${enc(source.owner)}/playlists/${source.kind}?rich-tracks=true"))
            is Source.UuidPlaylist -> parsePlaylist(get("/playlist/${enc(source.uuid)}?rich-tracks=true"))
            is Source.Album -> parseAlbum(get("/albums/${source.id}/with-tracks"))
            is Source.Track -> {
                val tracks = fetchTracks(listOf("${source.trackId}:${source.albumId}"))
                val name = tracks.firstOrNull()?.let { t -> listOf(t.artists.joinToString(", "), t.title).filter { it.isNotBlank() }.joinToString(" — ") }
                ImportedList(name ?: "Трек из Яндекс Музыки", ImportSource.YANDEX, tracks)
            }
            is Source.Artist -> {
                val name = runCatching { (get("/artists/${source.id}/brief-info") as? JsonObject)?.obj("artist")?.str("name") }.getOrNull()
                val tracks = parseTrackArray(get("/artists/${source.id}/tracks?page=0&page-size=100").obj()?.get("tracks"))
                ImportedList(name?.let { "$it — популярное" } ?: "Исполнитель", ImportSource.YANDEX, tracks)
            }
            is Source.Likes -> {
                val library = get("/users/${enc(source.owner)}/likes/tracks").obj()?.obj("library") ?: throw Failure.Private()
                val ids = (library["tracks"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.let(::trackId) }
                ImportedList("Мне нравится", ImportSource.YANDEX, fetchTracks(ids))
            }
        }
    }

    // ---- parsing (pure, unit-tested) ----

    internal fun parseLink(text: String): Source {
        val link = linkInText.find(text)?.value ?: text.trim()
        if (!isYandexLink(link)) throw Failure.BadLink()
        val path = runCatching { URLDecoder.decode(link, "UTF-8") }.getOrDefault(link).substringBefore('?').substringBefore('#')
        trackInAlbum.find(path)?.let { return Source.Track(it.groupValues[2], it.groupValues[1]) }
        userPlaylist.find(path)?.let { return Source.UserPlaylist(it.groupValues[1], it.groupValues[2]) }
        userLikes.find(path + "/")?.let { return Source.Likes(it.groupValues[1]) }
        album.find(path)?.let { return Source.Album(it.groupValues[1]) }
        artist.find(path)?.let { return Source.Artist(it.groupValues[1]) }
        uuidPlaylist.find(path)?.let { return Source.UuidPlaylist(it.groupValues[1]) }
        throw Failure.BadLink()
    }

    /** Playlist answer; big playlists may carry bare ids, which are completed with /tracks. */
    internal fun parsePlaylist(result: JsonElement, fetch: (List<String>) -> List<ImportedTrack> = ::fetchTracks): ImportedList {
        val obj = result.obj() ?: throw Failure.Private()
        val tracks = mutableListOf<ImportedTrack>()
        val bare = mutableListOf<String>()
        for (item in (obj["tracks"] as? JsonArray).orEmpty()) {
            val o = item as? JsonObject ?: continue
            val full = o.obj("track") ?: o.takeIf { it.containsKey("title") }
            if (full != null) trackFrom(full)?.let(tracks::add) else trackId(o)?.let(bare::add)
        }
        if (bare.isNotEmpty()) tracks += fetch(bare)
        val declared = (obj["trackCount"] as? JsonPrimitive)?.intOrNull ?: 0
        if (tracks.isEmpty() && declared > 0) throw Failure.Private()
        return ImportedList(obj.str("title") ?: "Плейлист из Яндекс Музыки", ImportSource.YANDEX, tracks)
    }

    internal fun parseAlbum(result: JsonElement): ImportedList {
        val obj = result.obj() ?: throw Failure.Private()
        val tracks = (obj["volumes"] as? JsonArray).orEmpty().flatMap { parseTrackArray(it) }
        return ImportedList(obj.str("title") ?: "Альбом из Яндекс Музыки", ImportSource.YANDEX, tracks)
    }

    internal fun parseTrackArray(element: JsonElement?): List<ImportedTrack> =
        (element as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.let(::trackFrom) }

    internal fun trackFrom(track: JsonObject): ImportedTrack? {
        val title = track.str("title")?.takeIf { it.isNotBlank() } ?: return null
        val version = track.str("version")?.takeIf { it.isNotBlank() }
        val artists = (track["artists"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.str("name")?.takeIf(String::isNotBlank) }
        val durationSec = (track["durationMs"] as? JsonPrimitive)?.longOrNull?.let { (it / 1000).toInt() }
        return ImportedTrack(if (version != null) "$title ($version)" else title, artists, durationSec?.takeIf { it > 0 })
    }

    private fun trackId(item: JsonObject): String? {
        val id = item.str("id")?.takeIf { it.isNotBlank() } ?: return null
        val albumId = item.str("albumId")?.takeIf { it.isNotBlank() }
        return if (albumId != null) "$id:$albumId" else id
    }

    private fun JsonElement.obj(): JsonObject? = this as? JsonObject
    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    // ---- network ----

    private fun fetchTracks(ids: List<String>): List<ImportedTrack> = ids.chunked(TRACKS_BATCH).flatMap { chunk ->
        parseTrackArray(request("/tracks", "track-ids=" + enc(chunk.joinToString(","))))
    }

    private fun get(path: String): JsonElement = request(path, null)

    private fun request(path: String, body: String?): JsonElement {
        val text = requestRaw(path, body)
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() as? JsonObject ?: throw Failure.Private()
        return root["result"] ?: throw Failure.Private()
    }

    private fun requestRaw(path: String, body: String?): String {
        val connection = try {
            (URL(API + path).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Language", "ru")
                setRequestProperty("User-Agent", "Yandex-Music-API")
                setRequestProperty("X-Yandex-Music-Client", "YandexMusicAndroid/24023621")
                if (body != null) {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    outputStream.use { it.write(body.toByteArray()) }
                }
            }
        } catch (e: IOException) {
            throw Failure.Network(e)
        }
        try {
            return when (val code = connection.responseCode) {
                in 200..299 -> connection.inputStream.bufferedReader().use { it.readText() }
                451 -> throw Failure.Region()
                400, 401, 403, 404 -> throw Failure.Private()
                else -> throw Failure.Network(IOException("HTTP $code"))
            }
        } catch (e: Failure) {
            throw e
        } catch (e: IOException) {
            throw Failure.Network(e)
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
