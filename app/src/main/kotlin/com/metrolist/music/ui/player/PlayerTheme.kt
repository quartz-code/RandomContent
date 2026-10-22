package com.metrolist.music.ui.player

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Pulse: everything the player builder can change. The three designs from the Figma file are just
 * presets of it, so any detail of one (say, the On air dial) can be put on another (a light screen).
 * Colours are ARGB longs so the theme is plain data that can be stored and shared as a code.
 */
@Serializable
data class PlayerTheme(
    val background: Background = Background.ARTWORK,
    val light: Boolean = false,
    /** Solid colour, or the top of the gradient. */
    val color1: Long = 0xFF3E4A5E,
    /** Bottom of the gradient. */
    val color2: Long = 0xFF101215,
    /** Text and icons; null picks black or white to suit [light]. */
    val textColor: Long? = null,
    val accentFromArtwork: Boolean = false,
    val accent: Long = 0xFFFFFFFF,
    /** Like button; null uses the accent. */
    val likeColor: Long? = null,
    val slider: Slider = Slider.THIN,
    val playButton: PlayButton = PlayButton.CIRCLE,
    /** Corner radius of the artwork in dp; [ART_CIRCLE] and above draws it round. */
    val artCorner: Int = 14,
    /** Space on each side of the artwork in dp: smaller is a bigger cover. */
    val artMargin: Int = 24,
    val artShadow: Boolean = false,
    /** Round artwork turns like a record while music plays. */
    val artSpin: Boolean = false,
    val artShape: ArtShape = ArtShape.ROUNDED,
    /** Outline around the artwork in the accent colour, dp; 0 for none. */
    val artBorder: Int = 0,
    /** What moves behind the player: nothing, flowing artwork colours, or bars / a wave to the music. */
    val motion: Motion = Motion.NONE,
    val lyricsFont: Font = Font.SYSTEM,
    /** Lyrics in the accent colour instead of the text colour. */
    val lyricsAccent: Boolean = false,
    /** AUTO keeps the alignment from the lyrics settings. */
    val lyricsAlign: LyricsAlign = LyricsAlign.AUTO,
    val titleCentered: Boolean = false,
    val font: Font = Font.SYSTEM,
    val titleSize: Int = 24,
    val bottomStyle: BottomStyle = BottomStyle.ICONS,
    val buttons: List<BottomButton> = listOf(BottomButton.TIMER, BottomButton.LYRICS, BottomButton.QUEUE),
    val showShuffleRepeat: Boolean = true,
    val showTime: Boolean = true,
    /** "В эфире" with a red lamp instead of "Сейчас играет". */
    val onAirHeader: Boolean = false,
    val mini: MiniTheme = MiniTheme(),
) {
    @Serializable enum class Background { ARTWORK, BLUR, SOLID, GRADIENT }

    @Serializable enum class Slider { THIN, DIAL, THICK, WAVE }

    @Serializable enum class PlayButton { CIRCLE, SQUIRCLE, PILL, KNOB }

    @Serializable enum class Font { SYSTEM, OSWALD, MONO }

    @Serializable enum class BottomStyle { ICONS, QUIET, PANEL }

    @Serializable enum class BottomButton { QUEUE, LYRICS, TIMER, SHARE, SHUFFLE, REPEAT }

    /** ROUNDED uses [artCorner]; the others are Material shapes. */
    @Serializable enum class ArtShape { ROUNDED, ARCH, COOKIE, CLOVER, SUNNY }

    @Serializable enum class Motion { NONE, FLOW, BARS, WAVE }

    @Serializable enum class LyricsAlign { AUTO, LEFT, CENTER, RIGHT }

    val artRound: Boolean get() = artShape == ArtShape.ROUNDED && artCorner >= ART_CIRCLE

    /** A turning square or arch looks broken, so only round and flower-like shapes spin. */
    val canSpin: Boolean get() = artRound || artShape == ArtShape.COOKIE || artShape == ArtShape.CLOVER || artShape == ArtShape.SUNNY

    val usesVisualizer: Boolean get() = motion == Motion.BARS || motion == Motion.WAVE

    companion object {
        const val ART_CIRCLE = 50
        const val MAX_BUTTONS = 5

        val Clean = PlayerTheme()

        val OnAir = PlayerTheme(
            background = Background.GRADIENT,
            color1 = 0xFF22261D,
            color2 = 0xFF121310,
            textColor = 0xFFEFE8D6,
            accent = 0xFFF0A43A,
            likeColor = 0xFFE0482F,
            slider = Slider.DIAL,
            playButton = PlayButton.KNOB,
            artCorner = 8,
            artMargin = 32,
            font = Font.OSWALD,
            titleSize = 30,
            bottomStyle = BottomStyle.PANEL,
            buttons = listOf(BottomButton.QUEUE, BottomButton.LYRICS, BottomButton.TIMER, BottomButton.SHARE),
            onAirHeader = true,
            mini = MiniTheme(background = MiniTheme.Background.PLAYER, corner = 12),
        )

        val Light = PlayerTheme(
            light = true,
            accentFromArtwork = true,
            accent = 0xFF4A5D7A,
            artCorner = 20,
            artMargin = 28,
            titleCentered = true,
            titleSize = 22,
            bottomStyle = BottomStyle.QUIET,
            buttons = listOf(BottomButton.LYRICS, BottomButton.TIMER, BottomButton.QUEUE),
        )

        val presets: List<Pair<String, PlayerTheme>> = listOf("clean" to Clean, "on_air" to OnAir, "light" to Light)

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            coerceInputValues = true
        }

        private const val CODE_PREFIX = "PULSE1-"
        private val codeInText = Regex("""PULSE1-[A-Za-z0-9_-]+""")

        fun fromJson(text: String?): PlayerTheme? =
            text?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }

        /** A short code to send to a friend: compressed JSON in URL-safe Base64. */
        fun PlayerTheme.toCode(): String {
            val bytes = json.encodeToString(serializer(), this).toByteArray()
            val deflater = Deflater(Deflater.BEST_COMPRESSION, true).apply {
                setInput(bytes)
                finish()
            }
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(512)
            while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
            deflater.end()
            return CODE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
        }

        /** Finds a theme code anywhere in the text (a whole forwarded message is fine). */
        fun fromCode(text: String?): PlayerTheme? {
            val code = text?.let { codeInText.find(it)?.value } ?: return null
            return runCatching {
                val packed = Base64.getUrlDecoder().decode(code.removePrefix(CODE_PREFIX))
                val inflater = Inflater(true).apply { setInput(packed) }
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(512)
                while (!inflater.finished()) {
                    val n = inflater.inflate(buffer)
                    if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                    out.write(buffer, 0, n)
                }
                inflater.end()
                fromJson(out.toString(Charsets.UTF_8.name()))
            }.getOrNull()?.sanitized()
        }
    }

    fun toJson(): String = Json { encodeDefaults = false }.encodeToString(serializer(), this)

    /** Keeps values from a hand-edited or foreign code inside what the player can draw. */
    fun sanitized(): PlayerTheme = copy(
        artCorner = artCorner.coerceIn(0, ART_CIRCLE),
        artMargin = artMargin.coerceIn(8, 64),
        titleSize = titleSize.coerceIn(16, 36),
        artBorder = artBorder.coerceIn(0, 8),
        buttons = buttons.distinct().take(MAX_BUTTONS),
        mini = mini.copy(corner = mini.corner.coerceIn(0, 32)),
    )
}

@Serializable
data class MiniTheme(
    val background: Background = Background.APP,
    /** Corner radius in dp; 32 is the full pill. */
    val corner: Int = 32,
    val progress: Progress = Progress.RING,
    val showLike: Boolean = true,
    val showNext: Boolean = false,
    val showSubscribe: Boolean = true,
) {
    /** APP follows the app theme; PLAYER uses the full player's colours. */
    @Serializable enum class Background { APP, PLAYER, BLUR, BLACK }

    @Serializable enum class Progress { RING, LINE, NONE }
}
