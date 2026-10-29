package app.pulse.desktop

import com.metrolist.innertube.YouTube
import com.metrolist.innertubex.models.YouTubeLocale
import java.io.File
import java.util.Locale

/** What the Android app does at start: region and language from the system, and a visitor id. */
object YouTubeSetup {
    private val visitorFile get() = File(AppDirs.data, "visitor_data")

    suspend fun init() {
        val locale = Locale.getDefault()
        YouTube.locale = YouTubeLocale(
            gl = locale.country.takeIf { it.length == 2 } ?: "US",
            hl = locale.language.takeIf { it.isNotBlank() } ?: "en",
        )
        YouTube.visitorData = visitorFile.takeIf { it.exists() }?.readText()?.trim()?.ifBlank { null }
            ?: YouTube.visitorData().getOrNull()?.also { runCatching { visitorFile.writeText(it) } }
    }
}
