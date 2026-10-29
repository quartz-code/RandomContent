package app.pulse.desktop

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

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

    /** For sign-in sessions: a file only the user can read, as a browser keeps its cookies. */
    fun writePrivate(file: File, text: String) {
        runCatching {
            file.writeText(text)
            runCatching {
                Files.setPosixFilePermissions(file.toPath(), setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))
            }.onFailure {
                // Windows has no POSIX modes; %APPDATA% is already private to the user.
                file.setReadable(false, false)
                file.setReadable(true, true)
            }
        }
    }
}
