package com.metrolist.music.pulse.importer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

/**
 * Pulse: reads "My music" and playlists from VK.
 *
 * VK closed audio in its public API, so this works the way VK's own mobile site does: the user
 * signs in on VK's page inside a WebView (the password never reaches Pulse), and the music lists
 * are requested from m.vk.ru with that session's cookies. The OAuth redirect is only used to learn
 * the user's id; its token is not kept. Endpoints and formats follow open clients that were
 * checked on devices in 2025–2026 (python273/vk_api, Andry495/vk_music).
 */
object VkMusicImport {
    /** VK's sign-in page answers to the Kate Mobile client id with the audio scope. */
    private const val CLIENT_ID = "2685278"
    const val USER_AGENT = "KateMobileAndroid/72.1 lite-535 (Android 11; SDK 30; arm64-v8a; Google Pixel 5; ru)"
    const val REDIRECT_URL = "https://oauth.vk.com/blank.html"

    val COOKIE_HOSTS = listOf(
        "https://vk.ru", "https://m.vk.ru", "https://login.vk.ru", "https://id.vk.ru",
        "https://vk.com", "https://m.vk.com", "https://login.vk.com", "https://id.vk.com",
        "https://oauth.vk.com",
    )

    private const val PAGE_SIZE = 2000
    private const val MAX_PAGES = 50
    // VK starts refusing when sections are requested back to back.
    private const val PAGE_DELAY_MS = 1_500L

    data class VkPlaylist(
        val ownerId: Long,
        val id: Long,
        val accessHash: String?,
        val title: String,
        val author: String? = null,
        val trackCount: Int? = null,
    )

    sealed class Failure(message: String) : IOException(message) {
        class NotLoggedIn : Failure("not_logged_in")
        class AccessDenied : Failure("access_denied")
        class BadLink : Failure("bad_link")
        class Network(cause: Throwable) : Failure(cause.message ?: "network")
    }

    sealed interface Redirect {
        data class Success(val userId: Long) : Redirect
        data class Error(val message: String) : Redirect
    }

    internal data class SectionPage(
        val tracks: List<ImportedTrack>,
        val hasMore: Boolean,
        val nextOffset: Int?,
        val title: String?,
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    fun authorizeUrl(): String =
        "https://oauth.vk.com/authorize?client_id=$CLIENT_ID&display=mobile" +
            "&redirect_uri=https%3A%2F%2Foauth.vk.com%2Fblank.html&scope=audio%2Coffline&response_type=token&v=5.131"

    /** Reads the OAuth redirect, or null when [url] is not the redirect page. */
    fun parseRedirect(url: String?): Redirect? {
        if (url == null || !url.contains("/blank.html")) return null
        val params = (url.substringAfter('#', "").ifBlank { url.substringAfter('?', "") })
            .split('&')
            .mapNotNull { part ->
                val eq = part.indexOf('=')
                if (eq <= 0) null else decode(part.substring(0, eq)) to decode(part.substring(eq + 1))
            }
            .toMap()
        params["error"]?.let { return Redirect.Error(params["error_description"] ?: it) }
        val userId = params["user_id"]?.toLongOrNull() ?: return null
        return Redirect.Success(userId)
    }

    fun hasSession(cookies: String) = cookies.contains("remixsid", ignoreCase = true)

    suspend fun loadMyMusic(userId: Long, cookies: String): ImportedList = withContext(Dispatchers.IO) {
        val tracks = loadSection(cookies, ownerId = userId, playlistId = -1, accessHash = null).tracks
        ImportedList("Моя музыка ВКонтакте", ImportSource.VK, tracks)
    }

    suspend fun loadPlaylist(cookies: String, playlist: VkPlaylist): ImportedList = withContext(Dispatchers.IO) {
        val result = loadSection(cookies, playlist.ownerId, playlist.id, playlist.accessHash)
        val title = playlist.title.takeUnless { it.isBlank() || it.startsWith("Плейлист ") }
            ?: result.title?.takeIf { it.isNotBlank() } ?: playlist.title
        ImportedList(title, ImportSource.VK, result.tracks)
    }

    suspend fun loadPlaylists(userId: Long, cookies: String): List<VkPlaylist> = withContext(Dispatchers.IO) {
        if (!hasSession(cookies)) throw Failure.NotLoggedIn()
        val found = linkedMapOf<String, VkPlaylist>()
        var offset = 0
        for (page in 0 until 10) {
            val html = get("https://m.vk.ru/audio?act=audio_playlists$userId&offset=$offset", cookies)
                ?: get("https://m.vk.com/audio?act=audio_playlists$userId&offset=$offset", cookies)
                ?: break
            val fresh = parsePlaylistsHtml(html).filter { "${it.ownerId}_${it.id}" !in found }
            if (fresh.isEmpty()) break
            fresh.forEach { found["${it.ownerId}_${it.id}"] = it }
            offset += 100
            delay(PAGE_DELAY_MS)
        }
        found.values.toList()
    }

    /** Playlist or album links copied from the VK app or site. */
    fun parsePlaylistLink(text: String): VkPlaylist? {
        val link = decode(text.trim())
        if (!link.contains("vk.com") && !link.contains("vk.ru")) return null
        PLAYLIST_PATH.find(link)?.let { m ->
            return VkPlaylist(m.groupValues[1].toLong(), m.groupValues[2].toLong(), m.groupValues[3].ifBlank { null }, "")
        }
        AUDIO_PLAYLIST_PARAM.find(link)?.let { m ->
            val hash = m.groupValues[3].ifBlank { ACCESS_HASH_PARAM.find(link)?.groupValues?.get(1).orEmpty() }
            return VkPlaylist(m.groupValues[1].toLong(), m.groupValues[2].toLong(), hash.ifBlank { null }, "")
        }
        return null
    }

    private suspend fun loadSection(cookies: String, ownerId: Long, playlistId: Long, accessHash: String?): SectionPage {
        if (!hasSession(cookies)) throw Failure.NotLoggedIn()
        val tracks = mutableListOf<ImportedTrack>()
        var title: String? = null
        var offset = 0
        for (page in 0 until MAX_PAGES) {
            val form = FormBody.Builder()
                .add("act", "load_section")
                .add("owner_id", ownerId.toString())
                .add("playlist_id", playlistId.toString())
                .add("offset", offset.toString())
                .add("type", "playlist")
                .add("access_hash", accessHash.orEmpty())
                .add("is_loading_all", "1")
                .build()
            val body = post("https://m.vk.ru/audio", cookies, form) ?: post("https://m.vk.com/audio", cookies, form)
                ?: throw Failure.Network(IOException("no answer"))
            val parsed = parseSectionResponse(body) ?: throw failureFor(body)
            if (title == null) title = parsed.title
            tracks += parsed.tracks
            if (!parsed.hasMore || parsed.tracks.isEmpty()) break
            offset = parsed.nextOffset?.takeIf { it > offset } ?: (offset + PAGE_SIZE)
            delay(PAGE_DELAY_MS)
        }
        if (tracks.isEmpty() && playlistId == -1L) {
            // Older accounts: the web section lists the library when load_section returns nothing.
            val body = post("https://vk.ru/al_audio.php", cookies, sectionForm(ownerId))
                ?: post("https://vk.com/al_audio.php", cookies, sectionForm(ownerId))
            body?.let { parseSectionResponse(it) }?.let { tracks += it.tracks }
        }
        return SectionPage(tracks.distinct(), false, null, title)
    }

    private fun sectionForm(ownerId: Long) = FormBody.Builder()
        .add("al", "1")
        .add("act", "section")
        .add("owner_id", ownerId.toString())
        .add("section", "all")
        .add("claim", "0")
        .build()

    private fun failureFor(body: String): Failure = when {
        body.contains("login", ignoreCase = true) && body.contains("<html", ignoreCase = true) -> Failure.NotLoggedIn()
        else -> Failure.AccessDenied()
    }

    // ---- parsing (pure, unit-tested) ----

    /** Parses a load_section / al_audio answer; null when it is not a music answer at all. */
    internal fun parseSectionResponse(body: String): SectionPage? {
        val text = body.trim().removePrefix("<!--").trim()
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull()
            ?: text.indexOf('{').takeIf { it >= 0 }?.let { runCatching { json.parseToJsonElement(text.substring(it)) }.getOrNull() }
            ?: return null
        val tracks = mutableListOf<ImportedTrack>()
        var hasMore = false
        var nextOffset: Int? = null
        var title: String? = null

        fun walk(node: JsonElement) {
            when (node) {
                is JsonObject -> {
                    (node["hasMore"] as? JsonPrimitive)?.booleanOrNull?.let { hasMore = hasMore || it }
                    (node["nextOffset"] as? JsonPrimitive)?.intOrNull?.let { nextOffset = it }
                    if (title == null && node.containsKey("list")) {
                        title = (node["title"] as? JsonPrimitive)?.contentOrNull?.let(::cleanText)
                    }
                    node.values.forEach(::walk)
                }
                is JsonArray -> {
                    val track = audioFrom(node)
                    if (track != null) tracks += track else node.forEach(::walk)
                }
                else -> Unit
            }
        }
        walk(root)
        if (tracks.isEmpty() && !text.contains("\"list\"")) return null
        return SectionPage(tracks, hasMore, nextOffset, title)
    }

    // VK audio rows are arrays: [id, owner_id, url, title, performer, duration, ...].
    private fun audioFrom(arr: JsonArray): ImportedTrack? {
        if (arr.size < 6) return null
        val id = (arr[0] as? JsonPrimitive)?.longOrNull ?: return null
        val owner = (arr[1] as? JsonPrimitive)?.longOrNull ?: return null
        val title = (arr[3] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return null
        val performer = (arr[4] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return null
        val duration = (arr[5] as? JsonPrimitive)?.let { it.intOrNull ?: it.content.toIntOrNull() } ?: return null
        if (id <= 0 || owner == 0L || title.isBlank()) return null
        val artists = splitArtists(cleanText(performer))
        return ImportedTrack(cleanText(title), artists, duration.takeIf { it > 0 })
    }

    internal fun parsePlaylistsHtml(html: String): List<VkPlaylist> {
        val out = linkedMapOf<String, VkPlaylist>()
        val items = html.split("audioPlaylistsPage__item").drop(1)
        for (item in items) {
            val m = AUDIO_PLAYLIST_PARAM.find(item) ?: continue
            val owner = m.groupValues[1].toLongOrNull() ?: continue
            val id = m.groupValues[2].toLongOrNull()?.takeIf { it > 0 } ?: continue
            val hash = m.groupValues[3].ifBlank { ACCESS_HASH_PARAM.find(item)?.groupValues?.get(1).orEmpty() }
            val title = CLASS_TEXT("audioPlaylistsPage__title").find(item)?.groupValues?.get(1)?.let(::cleanText)
            val author = CLASS_TEXT("audioPlaylistsPage__author").find(item)?.groupValues?.get(1)?.let(::cleanText)
            val count = COUNT.find(item)?.groupValues?.get(1)?.replace(Regex("\\s"), "")?.toIntOrNull()
            out.putIfAbsent(
                "${owner}_$id",
                VkPlaylist(owner, id, hash.ifBlank { null }, title?.ifBlank { null } ?: "Плейлист $id", author?.ifBlank { null }, count),
            )
        }
        return out.values.toList()
    }

    internal fun splitArtists(performer: String): List<String> =
        performer.split(Regex("""\s*(?:,|&|\bfeat\.?|\bft\.?|\bfeaturing\b)\s*""", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(performer.trim()).filter { it.isNotEmpty() } }

    internal fun cleanText(raw: String): String {
        var s = raw.replace(Regex("<[^>]+>"), "")
        s = Regex("&#(x?)([0-9a-fA-F]+);").replace(s) { m ->
            val code = if (m.groupValues[1].isNotEmpty()) m.groupValues[2].toIntOrNull(16) else m.groupValues[2].toIntOrNull()
            code?.let { String(Character.toChars(it)) } ?: m.value
        }
        s = s.replace("&quot;", "\"").replace("&apos;", "'").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&nbsp;", " ").replace("&amp;", "&")
        return s.replace(Regex("\\s+"), " ").trim()
    }

    // ---- network ----

    private fun post(url: String, cookies: String, form: FormBody): String? = execute(
        Request.Builder().url(url).post(form).header("X-Requested-With", "XMLHttpRequest"),
        cookies,
    )

    private fun get(url: String, cookies: String): String? = execute(Request.Builder().url(url).get(), cookies)

    private fun execute(builder: Request.Builder, cookies: String): String? {
        val request = builder
            .header("User-Agent", USER_AGENT)
            .header("Cookie", cookies)
            .header("Referer", "https://m.vk.ru/audio")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.code in 300..399) {
                    // A redirect to the sign-in page means the session is gone.
                    if (response.header("Location").orEmpty().contains("login")) throw Failure.NotLoggedIn()
                    return null
                }
                if (!response.isSuccessful) return null
                response.body.string().takeIf { it.length > 5 }
            }
        } catch (e: Failure) {
            throw e
        } catch (e: IOException) {
            null
        }
    }

    private fun decode(s: String) = runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)

    private val PLAYLIST_PATH = Regex("""/music/(?:playlist|album)/(-?\d+)_(\d+)(?:_([A-Za-z0-9]+))?""")
    private val AUDIO_PLAYLIST_PARAM = Regex("""audio_playlist(-?\d+)_(\d+)(?:(?:/|%2F|&amp;access_hash=|&access_hash=)([A-Za-z0-9]+))?""")
    private val ACCESS_HASH_PARAM = Regex("""access_hash=([A-Za-z0-9]+)""")
    private val COUNT = Regex("""(\d[\d\s]*)\s*(?:аудиозапис|трек|песн|audio|track)""", RegexOption.IGNORE_CASE)
    private fun CLASS_TEXT(cls: String) = Regex("""class="[^"]*\b$cls\b[^"]*"[^>]*>([\s\S]*?)</""")
}
