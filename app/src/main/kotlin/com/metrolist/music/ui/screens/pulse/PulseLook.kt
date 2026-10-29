package com.metrolist.music.ui.screens.pulse

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Pulse: how the "My pulse" page looks, chosen by the listener. Plain data with colours as ARGB,
 * so it is stored as JSON and sent to a friend as a code, like a player theme.
 */
@Serializable
data class PulseLook(
    val palette: Palette = Palette.PULSE,
    /** While a song plays, the lights behind take the colours of its artwork. */
    val artworkLights: Boolean = true,
    val shape: Shape = Shape.DROP,
    val background: Background = Background.LIGHTS,
    /** Rings spreading out from the heart, as on the logo. */
    val echoes: Boolean = true,
    val motion: Motion = Motion.NORMAL,
) {
    /**
     * Two colours for the heart and the buttons, two more for the lights behind. All are bright
     * enough for dark text on top, which the selected buttons use.
     */
    enum class Palette(val first: Long, val second: Long, val third: Long, val fourth: Long) {
        PULSE(0xFFFF4F7B, 0xFFFFA24C, 0xFFC2306E, 0xFF5B3FD9),
        OCEAN(0xFF35D0D6, 0xFF4F8BFF, 0xFF1B4DB8, 0xFF6A3DE8),
        NEON(0xFFFF3EA5, 0xFFB46BFF, 0xFF00C8F0, 0xFF5B2BD9),
        FOREST(0xFF47E08E, 0xFFD4F45A, 0xFF138A5A, 0xFF0F6E6E),
        SUNSET(0xFFFF6A45, 0xFFFFC857, 0xFFE0306A, 0xFF7A2E9E),
        ICE(0xFFC9EBFF, 0xFF8FB4FF, 0xFF4F6BD9, 0xFF4AB8D8),
    }

    enum class Shape { DROP, CIRCLE, FLOWER, STAR }

    enum class Background {
        /** Soft lights drifting behind the page. */
        LIGHTS,
        /** The artwork itself, blurred; the lights while nothing plays. */
        ARTWORK,
        /** One still glow, nothing moving behind. */
        CALM,
    }

    enum class Motion(val speed: Float) { CALM(0.55f), NORMAL(1f), LIVELY(1.6f) }

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            coerceInputValues = true
        }

        private const val CODE_PREFIX = "PULSEWAVE1-"
        private val codeInText = Regex("""PULSEWAVE1-[A-Za-z0-9_-]+""")

        fun fromJson(text: String?): PulseLook =
            text?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() } ?: PulseLook()

        /** A short code to send to a friend: compressed JSON in URL-safe Base64. */
        fun PulseLook.toCode(): String {
            val deflater = Deflater(Deflater.BEST_COMPRESSION, true).apply {
                setInput(toJson().toByteArray())
                finish()
            }
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(256)
            while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
            deflater.end()
            return CODE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
        }

        /** Finds a look code anywhere in the text, so a whole forwarded message works. */
        fun fromCode(text: String?): PulseLook? {
            val code = text?.let { codeInText.find(it)?.value } ?: return null
            return runCatching {
                val inflater = Inflater(true).apply { setInput(Base64.getUrlDecoder().decode(code.removePrefix(CODE_PREFIX))) }
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(256)
                while (!inflater.finished()) {
                    val n = inflater.inflate(buffer)
                    if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                    out.write(buffer, 0, n)
                }
                inflater.end()
                json.decodeFromString(serializer(), out.toString(Charsets.UTF_8.name()))
            }.getOrNull()
        }
    }
}
