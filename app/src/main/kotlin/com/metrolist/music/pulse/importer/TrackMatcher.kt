package com.metrolist.music.pulse.importer

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Pulse: finds an imported track on YouTube Music.
 *
 * Instead of trusting the first search result, several candidates are scored on title, artist and
 * length, with the noise typical of VK uploads ("[новинка 2024]", "(speed up)", "Official Video",
 * emoji, artist repeated in the title) removed first and Cyrillic/Latin spellings compared too.
 */
object TrackMatcher {
    enum class Status { MATCHED, UNSURE, NOT_FOUND }

    data class Candidate(val song: SongItem, val score: Double)

    data class Match(val status: Status, val candidates: List<Candidate>) {
        val best: Candidate? get() = candidates.firstOrNull()
    }

    const val CONFIDENT = 0.78
    const val POSSIBLE = 0.55
    private const val CANDIDATES_PER_QUERY = 6

    suspend fun match(track: ImportedTrack): Match {
        val cleanTitle = cleanTitle(track.title, track.artists)
        val artist = track.artists.firstOrNull().orEmpty()
        val queries = buildList {
            add(listOf(artist, cleanTitle).filter { it.isNotBlank() }.joinToString(" "))
            add(listOf(cleanTitle, track.artists.joinToString(" ")).filter { it.isNotBlank() }.joinToString(" "))
            add(cleanTitle)
        }.distinct().filter { it.isNotBlank() }

        val seen = linkedMapOf<String, Candidate>()
        var answered = false
        var lastError: Throwable? = null
        for (query in queries) {
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
            result.exceptionOrNull()?.let { lastError = it }
            if (result.isSuccess) answered = true
            val songs = result.getOrNull()
                ?.items?.filterIsInstance<SongItem>()?.take(CANDIDATES_PER_QUERY).orEmpty()
            for (song in songs) {
                if (song.id in seen) continue
                seen[song.id] = Candidate(song, score(track, song.title, song.artists.map { it.name }, song.duration))
            }
            if (seen.values.any { it.score >= CONFIDENT }) break
        }
        // Not a single answer: YouTube is unreachable (offline or VPN off), not "no such song".
        if (!answered) throw Unreachable(lastError)
        return classify(seen.values.toList())
    }

    class Unreachable(cause: Throwable?) : Exception(cause)

    /** Free-text search for the manual "find it myself" option. */
    suspend fun search(query: String, track: ImportedTrack): List<Candidate> =
        YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
            ?.items?.filterIsInstance<SongItem>()?.take(15).orEmpty()
            .map { Candidate(it, score(track, it.title, it.artists.map { a -> a.name }, it.duration)) }

    internal fun classify(candidates: List<Candidate>): Match {
        val sorted = candidates.sortedByDescending { it.score }
        val best = sorted.firstOrNull()?.score ?: 0.0
        val status = when {
            best >= CONFIDENT -> Status.MATCHED
            best >= POSSIBLE -> Status.UNSURE
            else -> Status.NOT_FOUND
        }
        return Match(status, sorted.take(8))
    }

    // ---- scoring (pure, unit-tested) ----

    internal fun score(source: ImportedTrack, title: String, artists: List<String>, durationSec: Int?): Double {
        // Compare the songs' base names; versions are handled by the tag check below.
        val srcTitle = normalize(baseTitle(cleanTitle(source.title, source.artists)))
        val dstTitle = normalize(baseTitle(cleanTitle(title, artists)))
        val titleScore = similarity(srcTitle, dstTitle)

        val artistScore = if (source.artists.isEmpty()) {
            0.5
        } else {
            source.artists.maxOf { a ->
                artists.maxOfOrNull { b -> similarity(normalize(a), normalize(b)) } ?: 0.0
            }.let { direct ->
                // Artist written into the title on one side ("Кино - Группа крови").
                if (direct < 0.6 && artists.any { normalize(title).contains(normalize(it)) && normalize(it).length > 2 }) {
                    max(direct, 0.6)
                } else {
                    direct
                }
            }
        }

        val src = source.durationSec?.takeIf { it > 0 }
        val dst = durationSec?.takeIf { it > 0 }
        var total = if (src != null && dst != null) {
            val diff = abs(src - dst)
            val durationScore = when {
                diff <= 3 -> 1.0
                diff <= 7 -> 0.75
                diff <= 15 -> 0.4
                diff <= 40 -> 0.1
                else -> 0.0
            }
            0.5 * titleScore + 0.32 * artistScore + 0.18 * durationScore
        } else {
            0.6 * titleScore + 0.4 * artistScore
        }

        // A remix, live or sped-up version is a different song for the listener.
        val srcTags = versionTags(source.title)
        val dstTags = versionTags(title)
        if (srcTags != dstTags) total *= if (srcTags.isEmpty() || dstTags.isEmpty()) 0.85 else 0.75
        return total.coerceIn(0.0, 1.0)
    }

    private val VERSION_WORDS = mapOf(
        "remix" to "remix", "ремикс" to "remix", "rmx" to "remix",
        "live" to "live", "концерт" to "live", "acoustic" to "acoustic", "акустика" to "acoustic",
        "instrumental" to "instrumental", "минус" to "instrumental", "karaoke" to "karaoke", "караоке" to "karaoke",
        "cover" to "cover", "кавер" to "cover",
        "sped up" to "fast", "speed up" to "fast", "speedup" to "fast", "nightcore" to "fast",
        "slowed" to "slow", "slow" to "slow", "reverb" to "slow",
        "mashup" to "mashup", "edit" to "edit", "bootleg" to "remix",
    )

    internal fun versionTags(title: String): Set<String> {
        val t = " " + title.lowercase().replace('ё', 'е') + " "
        return VERSION_WORDS.filterKeys { Regex("""(?<![\p{L}])${Regex.escape(it)}(?![\p{L}])""").containsMatchIn(t) }
            .values.toSet()
    }

    /** Removes upload noise but keeps words that change what the song is (remix, live...). */
    internal fun cleanTitle(title: String, artists: List<String>): String {
        var t = title
        // "Artist - Title" written by the uploader.
        for (artist in artists) {
            val prefix = Regex("""^\s*${Regex.escape(artist)}\s*[-–—:|]\s*""", RegexOption.IGNORE_CASE)
            t = t.replace(prefix, "")
        }
        val keep = VERSION_WORDS.keys
        t = Regex("""[\(\[\{]([^\)\]\}]*)[\)\]\}]""").replace(t) { m ->
            val inner = m.groupValues[1].lowercase()
            if (keep.any { inner.contains(it) }) " (${m.groupValues[1].trim()}) " else " "
        }
        // Tails like " | Official Video" or " // новинка".
        t = t.replace(Regex("""\s+(\||//)\s+.*$"""), "")
        t = t.replace(Regex("""(?i)\bofficial\s+(music\s+)?(video|audio)\b"""), " ")
        t = t.replace(Regex("""(?i)\s+(feat\.?|ft\.?|featuring)\s+.*$"""), "")
        t = t.replace(Regex("""[^\p{L}\p{N}\s'’().&+-]"""), " ")
        return t.replace(Regex("\\s+"), " ").trim().ifBlank { title.trim() }
    }

    internal fun baseTitle(title: String): String {
        var t = Regex("""[\(\[\{][^\)\]\}]*[\)\]\}]""").replace(title, " ")
        t = t.replace(Regex("""\s+[-–—]\s+.*$"""), "")
        for (word in VERSION_WORDS.keys) {
            t = t.replace(Regex("""(?i)(?<![\p{L}])${Regex.escape(word)}(?![\p{L}])"""), " ")
        }
        return t.replace(Regex("""[+&]\s*$"""), "").replace(Regex("\\s+"), " ").trim().ifBlank { title }
    }

    internal fun normalize(s: String): String {
        val lower = Normalizer.normalize(s.lowercase().replace('ё', 'е'), Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}"), "")
        return lower.replace(Regex("""[^\p{L}\p{N}]+"""), " ").trim()
    }

    /** Best of direct and transliterated comparison, by word overlap and by edit distance. */
    internal fun similarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        val direct = max(tokenScore(a, b), editScore(a, b))
        val ta = translit(a)
        val tb = translit(b)
        val translit = if (ta != a || tb != b) max(tokenScore(ta, tb), editScore(ta, tb)) else 0.0
        return max(direct, translit * 0.97)
    }

    private fun tokenScore(a: String, b: String): Double {
        val ta = a.split(' ').filter { it.isNotBlank() }.toSet()
        val tb = b.split(' ').filter { it.isNotBlank() }.toSet()
        if (ta.isEmpty() || tb.isEmpty()) return 0.0
        val common = ta.intersect(tb).size.toDouble()
        return common / max(ta.size, tb.size)
    }

    private fun editScore(a: String, b: String): Double {
        val x = a.take(80)
        val y = b.take(80)
        val longest = max(x.length, y.length)
        if (longest == 0) return 1.0
        return 1.0 - levenshtein(x, y).toDouble() / longest
    }

    private fun levenshtein(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = min(min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    private val TRANSLIT = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ж' to "zh", 'з' to "z",
        'и' to "i", 'й' to "i", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p",
        'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f", 'х' to "h", 'ц' to "ts", 'ч' to "ch",
        'ш' to "sh", 'щ' to "sch", 'ъ' to "", 'ы' to "y", 'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya",
    )

    internal fun translit(s: String): String {
        val sb = StringBuilder()
        for (c in s) sb.append(TRANSLIT[c] ?: c.toString())
        // Common Latin spellings of the same sounds.
        return sb.toString().replace("kh", "h").replace("ks", "x").replace("iy", "y").replace("yi", "y")
    }
}
