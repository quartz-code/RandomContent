package com.metrolist.music.pulse.update

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.metrolist.music.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pulse: updates without hunting for the APK. Asks GitHub for a newer `build-N`, downloads its
 * Pulse.apk into the cache and hands it to the system installer; every build is signed with the
 * same key, so it installs over this one and keeps everything.
 *
 * One state for the whole app: the download goes on when the dialog is closed or the screen
 * turns, and the dialog and the settings screen show the same progress.
 */
object PulseUpdater {
    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        data class Available(val update: PulseReleases.Update) : State
        data class Downloading(val update: PulseReleases.Update, val progress: Float) : State
        data class Ready(val update: PulseReleases.Update, val file: File) : State
        data class Failed(val update: PulseReleases.Update?) : State
    }

    /** This build's number from CI; 0 for a build made by hand, which is never offered updates. */
    val currentBuild: Int get() = BuildConfig.PULSE_BUILD

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /** The start-up offer is made once a run: "Later" holds until Pulse is started again. */
    var offeredThisRun = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var download: Job? = null

    /** Looks for a newer build; null when there is none or GitHub could not be reached. */
    suspend fun check(testBuilds: Boolean): PulseReleases.Update? = withContext(Dispatchers.IO) {
        if (download?.isActive == true) return@withContext (state.value as? State.Downloading)?.update
        _state.value = State.Checking
        val update = runCatching {
            PulseReleases.update(PulseReleases.parse(get(PulseReleases.API)), currentBuild, testBuilds, needApk = true)
        }
        _state.value = update.fold(
            onSuccess = { found -> found?.let { State.Available(it) } ?: State.UpToDate },
            onFailure = { State.Failed(null) },
        )
        update.getOrNull()
    }

    /** Downloads the update, then opens the installer; a file already downloaded is reused. */
    fun install(context: Context, update: PulseReleases.Update) {
        val app = context.applicationContext
        (state.value as? State.Ready)?.takeIf { it.update.release.build == update.release.build }?.let {
            openInstaller(app, it.file)
            return
        }
        if (download?.isActive == true) return
        download = scope.launch {
            val file = File(app.cacheDir, "updates/Pulse-${update.release.build}.apk")
            _state.value = State.Downloading(update, 0f)
            val done = runCatching { fetch(requireNotNull(update.release.apk), file, update.release.apkSize) { _state.value = State.Downloading(update, it) } }
            if (done.isSuccess) {
                _state.value = State.Ready(update, file)
                openInstaller(app, file)
            } else {
                file.delete()
                _state.value = State.Failed(update)
            }
        }
    }

    /**
     * The system installer. Android asks once whether Pulse may install apps: until then the
     * settings switch for that opens instead, and the listener comes back and taps install again.
     */
    private fun openInstaller(context: Context, file: File) {
        val intent = if (!context.packageManager.canRequestPackageInstalls()) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
        } else {
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file), "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            // GitHub refuses API requests without a User-Agent.
            setRequestProperty("User-Agent", "Pulse/${currentBuild}")
        }

    private fun get(url: String): String {
        val connection = open(url)
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "GitHub: ${connection.responseCode}" }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** The release file; GitHub redirects it to its storage, which HttpURLConnection follows. */
    private fun fetch(url: String, target: File, expected: Long, onProgress: (Float) -> Unit) {
        target.parentFile?.listFiles()?.forEach { it.delete() }
        target.parentFile?.mkdirs()
        val partial = File(target.path + ".part")
        val connection = open(url)
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "download: ${connection.responseCode}" }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: expected
            var read = 0L
            var shown = 0f
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        val progress = if (total > 0) (read.toFloat() / total).coerceAtMost(1f) else 0f
                        if (progress - shown >= 0.01f) {
                            shown = progress
                            onProgress(progress)
                        }
                    }
                }
            }
            // A cut-off download would fail in the installer with a vague "problem parsing".
            check(total <= 0 || read == total) { "download: $read of $total" }
            check(partial.renameTo(target)) { "download: rename" }
        } finally {
            connection.disconnect()
            partial.delete()
        }
    }
}
