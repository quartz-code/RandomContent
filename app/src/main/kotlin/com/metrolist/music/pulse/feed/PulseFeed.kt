package com.metrolist.music.pulse.feed

import com.metrolist.innertube.models.SongItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/** Pulse: how "My pulse" chooses. Stored as JSON, so new options keep old choices readable. */
@Serializable
data class PulseSettings(
    val character: Character = Character.MIX,
    val language: Language = Language.ANY,
    /** A mood or genre from YouTube Music's own list, or none. */
    val mood: Mood? = null,
) {
    enum class Character {
        /** Mostly new to you, now and then something you know. */
        MIX,
        /** Only songs you have not liked or played. */
        UNFAMILIAR,
        /** Close to what you like, familiar songs welcome. */
        FAVORITE,
        /** Starting from what is popular now. */
        POPULAR,
    }

    enum class Language { ANY, RUSSIAN, FOREIGN }

    @Serializable
    data class Mood(val title: String, val browseId: String, val params: String? = null)

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String?): PulseSettings =
            text?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() } ?: PulseSettings()
    }
}

/**
 * What the listener told the pulse with "not for me": the songs, and how often each artist was
 * turned down. Two refusals and an artist stops coming.
 */
@Serializable
data class PulseMemory(
    val dislikedSongs: List<String> = emptyList(),
    val artistStrikes: Map<String, Int> = emptyMap(),
) {
    val bannedArtists: Set<String> get() = artistStrikes.filterValues { it >= BAN_AFTER }.keys

    fun dislike(song: SongItem): PulseMemory = copy(
        dislikedSongs = (dislikedSongs - song.id + song.id).takeLast(MAX_SONGS),
        artistStrikes = song.artists.map { PulseMixer.artistKey(it.name) }.filter { it.isNotEmpty() }
            .fold(artistStrikes) { strikes, key -> strikes + (key to (strikes[key] ?: 0) + 1) },
    )

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        const val BAN_AFTER = 2
        private const val MAX_SONGS = 1000
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String?): PulseMemory =
            text?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() } ?: PulseMemory()
    }
}

/**
 * Pulse: picks the next songs out of what the sources offered. Pure, so it is tested: no repeats
 * in a session, nothing turned down, the chosen character and language, and variety — the same
 * artist never twice in a row, at most twice in one batch.
 */
object PulseMixer {
    /** How many picks back an artist may not come again. */
    private const val ARTIST_GAP = 3
    private const val PER_ARTIST = 2

    fun artistKey(name: String): String = name.trim().lowercase()

    fun hasCyrillic(text: String): Boolean = text.any { it in 'Ѐ'..'ӿ' }

    /** Whether [song] may play at all under [settings]; independent of what was picked before. */
    fun acceptable(song: SongItem, settings: PulseSettings, known: Set<String>, memory: PulseMemory, banned: Set<String> = memory.bannedArtists): Boolean {
        if (song.id in memory.dislikedSongs) return false
        if (song.artists.any { artistKey(it.name) in banned }) return false
        if (settings.character == PulseSettings.Character.UNFAMILIAR && song.id in known) return false
        val text = song.title + " " + song.artists.joinToString(" ") { it.name }
        return when (settings.language) {
            PulseSettings.Language.ANY -> true
            PulseSettings.Language.RUSSIAN -> hasCyrillic(text)
            PulseSettings.Language.FOREIGN -> !hasCyrillic(text)
        }
    }

    fun pick(
        candidates: List<SongItem>,
        settings: PulseSettings,
        known: Set<String>,
        played: Set<String>,
        memory: PulseMemory,
        recentArtists: List<String>,
        limit: Int,
        random: Random = Random.Default,
    ): List<SongItem> {
        val banned = memory.bannedArtists
        val fresh = candidates.distinctBy { it.id }.filter { it.id !in played && acceptable(it, settings, known, memory, banned) }
        // Proper songs before videos and uploads, which radios mix in; each kept in the sources' order.
        val ordered = lightlyShuffled(fresh, random).sortedBy { if (it.isVideoSong) 1 else 0 }
        val knownEvery = when (settings.character) {
            PulseSettings.Character.MIX -> 4 // one in four may be a song you know
            PulseSettings.Character.FAVORITE -> 1
            PulseSettings.Character.POPULAR -> 2
            PulseSettings.Character.UNFAMILIAR -> Int.MAX_VALUE
        }
        val queue = if (settings.character == PulseSettings.Character.FAVORITE) ordered.sortedByDescending { it.id in known } else ordered

        val picked = mutableListOf<SongItem>()
        val recent = recentArtists.takeLast(ARTIST_GAP).toMutableList()
        val perArtist = mutableMapOf<String, Int>()
        var knownCount = 0
        fun tryPick(strict: Boolean) {
            for (song in queue) {
                if (picked.size >= limit) return
                if (song in picked) continue
                val keys = song.artists.map { artistKey(it.name) }.ifEmpty { listOf("") }
                if (strict && keys.any { it.isNotEmpty() && it in recent }) continue
                if (keys.any { (perArtist[it] ?: 0) >= PER_ARTIST }) continue
                val isKnown = song.id in known
                // At most one familiar song in every [knownEvery] picks, counting this one.
                if (isKnown && knownEvery > 1 && (knownCount + 1).toLong() * knownEvery > picked.size + knownEvery.toLong()) continue
                picked += song
                if (isKnown) knownCount++
                keys.forEach { perArtist[it] = (perArtist[it] ?: 0) + 1 }
                recent += keys
                while (recent.size > ARTIST_GAP) recent.removeAt(0)
            }
        }
        tryPick(strict = true)
        // Few artists on offer: let one come back sooner rather than stop the music.
        if (picked.size < limit) tryPick(strict = false)
        return picked
    }

    /** Neighbours swap now and then, so two sessions from the same seeds do not sound the same. */
    private fun lightlyShuffled(list: List<SongItem>, random: Random): List<SongItem> {
        val out = list.toMutableList()
        for (i in 0 until out.size - 1) {
            if (random.nextInt(3) == 0) {
                val t = out[i]
                out[i] = out[i + 1]
                out[i + 1] = t
            }
        }
        return out
    }
}

/**
 * Pulse: an endless stream of songs for "My pulse". It starts radios from seed songs (recent likes
 * and favourites, a mood's playlists, or what is popular), takes from the account's own mix when
 * there are no seeds, and lets the listener steer: a like becomes the next seed, "not for me" is
 * remembered. Network and library come in through interfaces, so the whole loop is tested.
 */
class PulseFeed(
    private val source: Source,
    private val library: Library,
    private val memoryStore: MemoryStore,
    val settings: PulseSettings,
    private val random: Random = Random.Default,
) {
    data class Page(val items: List<SongItem>, val continuation: String?)

    interface Source {
        /** A radio of songs like [seedId]; [continuation] for the next page of the same radio. */
        suspend fun radio(seedId: String, continuation: String?): Page
        /** The account's own endless mix (works signed out too, then it is generic). */
        suspend fun personalMix(continuation: String?): Page
        suspend fun moodSongs(mood: PulseSettings.Mood): List<SongItem>
        suspend fun popularSongs(): List<SongItem>
    }

    interface Library {
        /** Songs liked, in the library or listened to: "familiar". */
        suspend fun knownIds(): Set<String>
        /** Recent likes and most played, best first. */
        suspend fun seedIds(): List<String>
    }

    interface MemoryStore {
        suspend fun load(): PulseMemory
        suspend fun save(memory: PulseMemory)
    }

    private var started = false
    private var known: Set<String> = emptySet()
    private var memory = PulseMemory()
    private val seeds = ArrayDeque<String>()
    private val radios = HashMap<String, String?>()
    private val finishedRadios = HashSet<String>()
    private var mixContinuation: String? = null
    private var mixFinished = false
    private var fetches = 0
    private var popularTried = false
    private val pool = mutableListOf<SongItem>()
    private val emitted = LinkedHashSet<String>()
    private val recentArtists = ArrayDeque<String>()

    /** Songs this pulse has given out, so a song in the player can be recognised as the pulse's. */
    val played: Set<String> get() = emitted

    suspend fun next(limit: Int = BATCH): List<SongItem> {
        start()
        val out = mutableListOf<SongItem>()
        var rounds = 0
        while (out.size < limit && rounds < MAX_ROUNDS) {
            rounds++
            if (pool.size < limit * 2) pool += runCatching { fetch() }.getOrDefault(emptyList())
            val picked = PulseMixer.pick(pool, settings, known, emitted, memory, recentArtists.toList(), limit - out.size, random)
            // Nothing on offer fits (say, "Russian" while the seeds are all English): start over from
            // what is popular where the listener is, once, rather than fall silent.
            if (picked.isEmpty() && !popularTried) {
                popularTried = true
                val popular = runCatching { source.popularSongs() }.getOrDefault(emptyList())
                    .filter { PulseMixer.acceptable(it, settings, known, memory) }
                pool += popular
                popular.take(SEEDS).forEach { seeds.addFirst(it.id) }
                if (popular.isNotEmpty()) continue
            }
            if (picked.isEmpty() && pool.isEmpty() && seeds.isEmpty() && mixFinished) break
            pool.removeAll { song -> picked.any { it.id == song.id } || song.id in emitted || !PulseMixer.acceptable(song, settings, known, memory) }
            for (song in picked) {
                emitted += song.id
                song.artists.forEach { recentArtists.addLast(PulseMixer.artistKey(it.name)) }
                while (recentArtists.size > 6) recentArtists.removeFirst()
            }
            out += picked
        }
        // Wander: a song the pulse found becomes a seed too, so the stream drifts into new music
        // instead of circling the same few radios.
        if (settings.character != PulseSettings.Character.FAVORITE) out.filter { it.id !in known }.randomOrNull(random)?.let { seeds.addLast(it.id) }
        return out
    }

    /** A like steers the pulse: the next songs come from around this one. */
    fun liked(songId: String) {
        seeds.remove(songId)
        seeds.addFirst(songId)
    }

    /** "Not for me": never this song again, and an artist turned down twice stops coming. */
    suspend fun disliked(song: SongItem) {
        memory = memory.dislike(song)
        memoryStore.save(memory)
        seeds.remove(song.id)
        pool.removeAll { !PulseMixer.acceptable(it, settings, known, memory) }
    }

    private suspend fun start() {
        if (started) return
        started = true
        known = runCatching { library.knownIds() }.getOrDefault(emptySet())
        memory = runCatching { memoryStore.load() }.getOrDefault(PulseMemory())
        val mood = settings.mood
        val first: List<String> = when {
            mood != null -> runCatching { source.moodSongs(mood) }.getOrDefault(emptyList()).also { pool += it.shuffled(random) }.map { it.id }
            settings.character == PulseSettings.Character.POPULAR -> runCatching { source.popularSongs() }.getOrDefault(emptyList()).also { pool += it; popularTried = true }.map { it.id }
            else -> runCatching { library.seedIds() }.getOrDefault(emptyList())
        }
        // Not always the same first radio: the top seeds, in a different order each time.
        val top = first.take(SEEDS).shuffled(random)
        seeds.addAll(top)
    }

    private suspend fun fetch(): List<SongItem> {
        fetches++
        // The account's mix every few fetches keeps it fresh; with no seeds at all it is the source.
        val useMix = !mixFinished && (seeds.isEmpty() || (settings.mood == null && settings.character == PulseSettings.Character.MIX && fetches % MIX_EVERY == 0))
        if (useMix) {
            val page = source.personalMix(mixContinuation)
            mixContinuation = page.continuation
            if (page.continuation == null) mixFinished = true
            return page.items
        }
        repeat(seeds.size) {
            val seed = seeds.removeFirst()
            if (seed in finishedRadios) return@repeat
            seeds.addLast(seed)
            val page = source.radio(seed, radios[seed])
            radios[seed] = page.continuation
            if (page.continuation == null) finishedRadios += seed
            // A radio opens with its seed: that song was chosen by the listener, not found for them.
            return page.items.filter { it.id != seed || seed !in known }
        }
        return emptyList()
    }

    companion object {
        const val BATCH = 12
        private const val MAX_ROUNDS = 6
        private const val SEEDS = 12
        private const val MIX_EVERY = 3
    }
}
