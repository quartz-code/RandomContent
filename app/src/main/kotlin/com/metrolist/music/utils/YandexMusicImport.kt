package com.metrolist.music.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Pulse: reads a public Yandex Music playlist, album or "Мне нравится" list so its tracks can be
 * matched on YouTube Music. Uses the public api.music.yandex.net endpoints, no account needed.
 */
object YandexMusicImport {
    data class YandexTrack(val title: String, val artists: List<String>)

    data class YandexPlaylist(val title: String, val tracks: List<YandexTrack>)

    sealed class Failure(message: String) : IOException(message) {
        /** Yandex answers 451 to non-Russian IPs, e.g. when the phone is behind a VPN. */
        class Region : Failure("region")
        class Private : Failure("private")
        class BadLink : Failure("bad link")
        class Network(cause: Throwable) : Failure(cause.message ?: "network")
    }

    private sealed class Source {
        data class UserPlaylist(val owner: String, val kind: String) : Source()
        data class UuidPlaylist(val uuid: String) : Source()
        data class Album(val id: String) : Source()
        data class Likes(val owner: String) : Source()
    }

    private const val API = "https://api.music.yandex.net"
    private const val TRACKS_BATCH = 100

    private val userPlaylist = Regex("""/users/([^/?#]+)/playlists/(\d+)""")
    private val userLikes = Regex("""/users/([^/?#]+)/tracks""")
    private val uuidPlaylist = Regex("""/playlists/([A-Za-z0-9.\-]+)""")
    private val album = Regex("""/album/(\d+)""")

    fun isYandexLink(text: String) = text.contains("music.yandex.")

    suspend fun load(link: String): YandexPlaylist = withContext(Dispatchers.IO) {
        when (val source = parse(link.trim())) {
            is Source.UserPlaylist -> {
                val result = get("/users/${enc(source.owner)}/playlists/${source.kind}?rich-tracks=true")
                playlistFrom(result)
            }
            is Source.UuidPlaylist -> playlistFrom(get("/playlist/${enc(source.uuid)}?rich-tracks=true"))
            is Source.Album -> {
                val result = get("/albums/${source.id}/with-tracks")
                val tracks = buildList {
                    val volumes = result.optJSONArray("volumes") ?: JSONArray()
                    for (v in 0 until volumes.length()) {
                        val volume = volumes.optJSONArray(v) ?: continue
                        for (t in 0 until volume.length()) volume.optJSONObject(t)?.let(::trackFrom)?.let(::add)
                    }
                }
                YandexPlaylist(result.optString("title"), tracks)
            }
            is Source.Likes -> {
                val library = get("/users/${enc(source.owner)}/likes/tracks").optJSONObject("library")
                    ?: throw Failure.Private()
                val ids = library.optJSONArray("tracks") ?: JSONArray()
                val trackIds = (0 until ids.length()).mapNotNull { i -> ids.optJSONObject(i)?.let(::trackId) }
                YandexPlaylist("Мне нравится", fetchTracks(trackIds))
            }
        }
    }

    private fun parse(link: String): Source {
        if (!isYandexLink(link)) throw Failure.BadLink()
        userPlaylist.find(link)?.let { return Source.UserPlaylist(it.groupValues[1], it.groupValues[2]) }
        userLikes.find(link)?.let { return Source.Likes(it.groupValues[1]) }
        album.find(link)?.let { return Source.Album(it.groupValues[1]) }
        uuidPlaylist.find(link)?.let { return Source.UuidPlaylist(it.groupValues[1]) }
        throw Failure.BadLink()
    }

    // Large playlists may list bare ids without track details; those are fetched in batches.
    private fun playlistFrom(result: JSONObject): YandexPlaylist {
        val items = result.optJSONArray("tracks") ?: JSONArray()
        val tracks = mutableListOf<YandexTrack>()
        val bareIds = mutableListOf<String>()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val track = item.optJSONObject("track")
            when {
                track != null -> trackFrom(track)?.let(tracks::add)
                item.has("title") -> trackFrom(item)?.let(tracks::add)
                item.optString("id").isNotBlank() -> bareIds += trackId(item)
            }
        }
        tracks += fetchTracks(bareIds)
        if (tracks.isEmpty() && result.optInt("trackCount", 0) > 0) throw Failure.Private()
        return YandexPlaylist(result.optString("title"), tracks)
    }

    private fun trackId(item: JSONObject): String {
        val albumId = item.optString("albumId")
        return if (albumId.isNotBlank()) "${item.optString("id")}:$albumId" else item.optString("id")
    }

    private fun fetchTracks(ids: List<String>): List<YandexTrack> = ids.chunked(TRACKS_BATCH).flatMap { chunk ->
        val arr = postTracks(chunk)
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(::trackFrom) }
    }

    private fun trackFrom(track: JSONObject): YandexTrack? {
        val title = track.optString("title").takeIf { it.isNotBlank() } ?: return null
        val version = track.optString("version").takeIf { it.isNotBlank() }
        val artistsJson = track.optJSONArray("artists") ?: JSONArray()
        val artists = (0 until artistsJson.length()).mapNotNull { i ->
            artistsJson.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }
        }
        return YandexTrack(if (version != null) "$title ($version)" else title, artists)
    }

    private fun get(path: String): JSONObject = request(path, null)

    private fun postTracks(ids: List<String>): JSONArray {
        val body = "track-ids=" + enc(ids.joinToString(","))
        val response = requestRaw("/tracks", body)
        return JSONObject(response).optJSONArray("result") ?: JSONArray()
    }

    private fun request(path: String, body: String?): JSONObject =
        JSONObject(requestRaw(path, body)).optJSONObject("result") ?: throw Failure.Private()

    private fun requestRaw(path: String, body: String?): String {
        val connection = try {
            (URL(API + path).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) Pulse")
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
                401, 403, 404 -> throw Failure.Private()
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
