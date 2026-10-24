package app.pulse.desktop

import com.metrolist.music.ui.player.PlayerTheme
import java.io.File
import java.util.Properties

/** Pulse desktop: the player theme (as on the phone), the volume and the name in rooms, kept between launches. */
object Settings {
    private val themeFile get() = File(AppDirs.data, "player_theme.json")
    private val propsFile get() = File(AppDirs.data, "settings.properties")
    private val props = Properties().apply { runCatching { propsFile.inputStream().use(::load) } }

    fun loadTheme(): PlayerTheme = runCatching { PlayerTheme.fromJson(themeFile.readText()) }.getOrNull()?.sanitized() ?: PlayerTheme.Clean

    fun saveTheme(theme: PlayerTheme) {
        runCatching { themeFile.writeText(theme.sanitized().toJson()) }
    }

    var togetherName: String?
        get() = props.getProperty("togetherName")
        set(value) {
            props.setProperty("togetherName", value.orEmpty())
            save()
        }

    var volume: Float
        get() = props.getProperty("volume")?.toFloatOrNull() ?: 0.8f
        set(value) {
            props.setProperty("volume", value.toString())
            save()
        }

    private fun save() {
        runCatching { propsFile.outputStream().use { props.store(it, null) } }
    }
}
