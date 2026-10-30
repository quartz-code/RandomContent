package app.pulse.desktop

import com.metrolist.music.pulse.update.PulseReleases
import java.awt.Desktop
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Pulse desktop: tells when a newer build is out. The installers differ by system, so it opens
 * the release page instead of installing; the phone's PulseUpdater installs by itself.
 */
object DesktopUpdates {
    /**
     * This build's number: the installer is versioned 1.0.N by CI, and the packaged launcher
     * passes its version as `jpackage.app-version`. Run from sources there is none, so no offers.
     */
    val currentBuild: Int
        get() = System.getProperty("jpackage.app-version")?.substringAfterLast('.')?.toIntOrNull() ?: 0

    /** A newer regular build, or null when there is none or GitHub could not be reached. */
    fun check(): PulseReleases.Update? = runCatching {
        if (currentBuild <= 0) return null
        val connection = (URL(PulseReleases.API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "Pulse-desktop/$currentBuild")
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            PulseReleases.update(PulseReleases.parse(text), currentBuild, testBuilds = false, needApk = false)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    fun openPage(update: PulseReleases.Update) {
        runCatching { Desktop.getDesktop().browse(URI(update.release.page)) }
    }
}
