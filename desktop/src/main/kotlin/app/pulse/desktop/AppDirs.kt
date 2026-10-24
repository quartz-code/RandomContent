package app.pulse.desktop

import java.io.File

/** Where Pulse keeps its files: %APPDATA%\Pulse on Windows, ~/.local/share/pulse on Linux. */
object AppDirs {
    val data: File by lazy {
        val os = System.getProperty("os.name").lowercase()
        val base = when {
            os.contains("win") -> System.getenv("APPDATA")?.let { File(it, "Pulse") }
            else -> (System.getenv("XDG_DATA_HOME")?.let(::File) ?: File(System.getProperty("user.home"), ".local/share")).let { File(it, "pulse") }
        } ?: File(System.getProperty("user.home"), ".pulse")
        base.apply { mkdirs() }
    }

    val audioCache: File by lazy { File(data, "audio-cache").apply { mkdirs() } }
}
