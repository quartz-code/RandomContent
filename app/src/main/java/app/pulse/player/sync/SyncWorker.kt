package app.pulse.player.sync

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.pulse.player.PulseApp
import app.pulse.player.data.Track
import app.pulse.player.data.TrackStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val app = context.applicationContext as PulseApp
    private val db = app.db
    private val prefs = app.prefs

    override suspend fun doWork(): Result {
        val cookies = Cookies.file(applicationContext)
        if (!Cookies.isLoggedIn(applicationContext)) {
            prefs.lastSyncResult = "Нужно войти в аккаунт"
            return Result.failure()
        }
        report(Phase.PREPARE, "Подготовка…")

        return try {
            runInterruptible { YtDlp.init(applicationContext) }
            maybeUpdateEngine()

            report(Phase.FETCH, "Загружаю список лайков…")
            val liked = runInterruptible { YtDlp.fetchLiked(applicationContext, cookies) }
            if (liked.isEmpty()) {
                // An empty list almost always means the session expired, not that every like was removed.
                prefs.lastSyncResult = "Список лайков пуст — возможно, нужно войти заново"
                return Result.failure()
            }
            applyLikedList(liked)

            val queue = db.tracks().toDownload()
            val (ok, failed) = downloadAll(queue, cookies)

            prefs.lastSync = System.currentTimeMillis()
            prefs.lastSyncResult = buildString {
                append("Лайков: ${liked.size}. Скачано новых: $ok")
                if (failed > 0) append(", ошибок: $failed")
            }
            Result.success()
        } catch (e: CancellationException) {
            prefs.lastSyncResult = "Синхронизация остановлена"
            throw e
        } catch (e: Throwable) {
            prefs.lastSyncResult = "Ошибка: ${YtDlp.shortError(e)}"
            Result.failure()
        }
    }

    private suspend fun maybeUpdateEngine() {
        val day = 24 * 3600 * 1000L
        if (System.currentTimeMillis() - prefs.lastEngineUpdate < day) return
        report(Phase.PREPARE, "Обновляю yt-dlp…")
        runCatching { runInterruptible { YtDlp.update(applicationContext) } }
            .onSuccess { prefs.lastEngineUpdate = System.currentTimeMillis() }
    }

    private suspend fun applyLikedList(liked: List<LikedEntry>) {
        val dao = db.tracks()
        val removedLikes = mutableListOf<Track>()
        db.withTransaction {
            val existing = dao.all().associateBy { it.id }
            val likedIds = liked.mapTo(HashSet()) { it.id }
            val updates = liked.mapIndexed { index, e ->
                existing[e.id]?.copy(likedIndex = index, liked = true)
                    ?: Track(id = e.id, title = e.title, artist = e.artist, durationSec = e.durationSec, likedIndex = index)
            }.toMutableList()
            for (t in existing.values) {
                if (t.id !in likedIds && t.liked) {
                    updates += t.copy(liked = false)
                    removedLikes += t
                }
            }
            dao.upsert(updates)
        }
        if (prefs.deleteUnliked) {
            for (t in removedLikes) {
                t.filePath?.let { File(it).delete() }
                t.coverPath?.let { File(it).delete() }
                db.playlists().removeTrackEverywhere(t.id)
                dao.delete(t.id)
            }
        }
    }

    private suspend fun downloadAll(queue: List<Track>, cookies: File): Pair<Int, Int> = coroutineScope {
        if (queue.isEmpty()) return@coroutineScope 0 to 0
        val total = queue.size
        val done = AtomicInteger(0)
        val ok = AtomicInteger(0)
        val reportLock = Mutex()
        val semaphore = Semaphore(prefs.parallelDownloads.coerceIn(1, 4))

        queue.mapIndexed { index, track ->
            async {
                semaphore.withPermit {
                    val processId = "dl-$index-${track.id}"
                    reportLock.withLock {
                        report(Phase.DOWNLOAD, "${track.artist} — ${track.title}", done.get(), total)
                    }
                    try {
                        val result = runInterruptible {
                            YtDlp.download(applicationContext, track.id, cookies, processId) {}
                        }
                        db.tracks().upsert(
                            listOf(
                                track.copy(
                                    title = result.title,
                                    artist = result.artist,
                                    album = result.album,
                                    durationSec = result.durationSec.takeIf { it > 0 } ?: track.durationSec,
                                    filePath = result.filePath,
                                    coverPath = result.coverPath,
                                    status = TrackStatus.DOWNLOADED,
                                    error = null,
                                )
                            )
                        )
                        ok.incrementAndGet()
                    } catch (e: CancellationException) {
                        YtDlp.cancel(processId)
                        throw e
                    } catch (e: Throwable) {
                        db.tracks().setStatus(track.id, TrackStatus.FAILED, YtDlp.shortError(e))
                    }
                    val finished = done.incrementAndGet()
                    reportLock.withLock {
                        report(Phase.DOWNLOAD, "${track.artist} — ${track.title}", finished, total)
                    }
                }
            }
        }.awaitAll()
        ok.get() to (total - ok.get())
    }

    private suspend fun report(phase: Phase, text: String, done: Int = 0, total: Int = 0) {
        setProgress(workDataOf(KEY_PHASE to phase.name, KEY_TEXT to text, KEY_DONE to done, KEY_TOTAL to total))
        runCatching { setForeground(foregroundInfo(text, done, total)) }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo("Синхронизация…", 0, 0)

    private fun foregroundInfo(text: String, done: Int, total: Int): ForegroundInfo {
        val title = if (total > 0) "Скачиваю музыку: $done из $total" else "Синхронизация лайков"
        val notification: Notification = NotificationCompat.Builder(applicationContext, PulseApp.SYNC_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(total, done, total == 0)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    enum class Phase { PREPARE, FETCH, DOWNLOAD }

    companion object {
        const val KEY_PHASE = "phase"
        const val KEY_TEXT = "text"
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        private const val NOTIFICATION_ID = 42
    }
}
