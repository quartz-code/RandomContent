package com.metrolist.music.ui.player

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

/** A theme kept in "My themes": made by the user or received from a friend. */
@Serializable
data class SavedTheme(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val theme: PlayerTheme,
    val fromFriend: Boolean = false,
)

/**
 * Pulse: the gallery of saved themes and the evening theme. Plain functions over lists so the
 * rules (no duplicates, friends' themes saved once) are unit-tested without Android.
 */
object PlayerThemeGallery {
    const val MAX_THEMES = 30

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        coerceInputValues = true
    }
    private val listSerializer = ListSerializer(SavedTheme.serializer())

    fun fromJson(text: String?): List<SavedTheme> =
        text?.takeIf { it.isNotBlank() }
            ?.let { runCatching { json.decodeFromString(listSerializer, it) }.getOrNull() }
            ?.map { it.copy(theme = it.theme.sanitized()) }
            .orEmpty()

    fun toJson(themes: List<SavedTheme>): String = json.encodeToString(listSerializer, themes)

    /**
     * Adds the theme at the front. The same look saved twice stays once (the newer name wins),
     * so applying a friend's code again does not fill the gallery with copies.
     */
    fun add(themes: List<SavedTheme>, saved: SavedTheme): List<SavedTheme> =
        (listOf(saved) + themes.filter { it.theme != saved.theme && it.id != saved.id }).take(MAX_THEMES)

    fun rename(themes: List<SavedTheme>, id: String, name: String): List<SavedTheme> =
        themes.map { if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }) else it }

    fun remove(themes: List<SavedTheme>, id: String): List<SavedTheme> = themes.filterNot { it.id == id }

    /** "Тема от друга", "Тема от друга 2", … — the first name not taken yet. */
    fun freeName(themes: List<SavedTheme>, base: String): String {
        val taken = themes.map { it.name }.toSet()
        if (base !in taken) return base
        return generateSequence(2) { it + 1 }.map { "$base $it" }.first { it !in taken }
    }
}

/** When the evening theme takes over from the main one. */
enum class NightMode { OFF, SYSTEM, HOURS }

/**
 * True between [fromMinutes] and [toMinutes] (minutes since midnight); the span may cross
 * midnight, as "21:00–07:00" does.
 */
fun isNightTime(nowMinutes: Int, fromMinutes: Int, toMinutes: Int): Boolean = when {
    fromMinutes == toMinutes -> false
    fromMinutes < toMinutes -> nowMinutes in fromMinutes until toMinutes
    else -> nowMinutes >= fromMinutes || nowMinutes < toMinutes
}

/** What the evening theme starts as: the same design, but dark. */
fun PlayerTheme.nightVersion(): PlayerTheme =
    if (light) copy(light = false, textColor = null, background = if (background == PlayerTheme.Background.SOLID) PlayerTheme.Background.ARTWORK else background)
    else this
