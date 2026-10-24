package app.pulse.desktop

import com.metrolist.music.ui.player.PlayerTheme
import java.io.File
import java.util.Properties

/** Pulse desktop: the player theme (as on the phone) and the volume, kept between launches. */
object Settings {
    private val themeFile get() = File(AppDirs.data, "player_theme.json")
    private val propsFile get() = File(AppDirs.data, "settings.properties")
    private val props = Properties().apply { runCatching { propsFile.inputStream().use(::load) } }

    fun loadTheme(): PlayerTheme = runCatching { PlayerTheme.fromJson(themeFile.readText()) }.getOrNull()?.sanitized() ?: PlayerTheme.Clean

    fun saveTheme(theme: PlayerTheme) {
        runCatching { themeFile.writeText(theme.sanitized().toJson()) }
    }

    var volume: Float
        get() = props.getProperty("volume")?.toFloatOrNull() ?: 0.8f
        set(value) {
            props.setProperty("volume", value.toString())
            runCatching { propsFile.outputStream().use { props.store(it, null) } }
        }
}
