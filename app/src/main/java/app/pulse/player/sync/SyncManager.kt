package app.pulse.player.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.pulse.player.data.Prefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.concurrent.TimeUnit

data class SyncState(
    val running: Boolean,
    val text: String = "",
    val done: Int = 0,
    val total: Int = 0,
)

object SyncManager {
    private const val MANUAL = "sync"
    private const val PERIODIC = "auto-sync"

    private fun constraints(prefs: Prefs) = Constraints.Builder()
        .setRequiredNetworkType(if (prefs.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
        .build()

    fun syncNow(context: Context, prefs: Prefs) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints(prefs))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(MANUAL, ExistingWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(MANUAL)
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        applyAutoSync(context, Prefs(context))
    }

    fun applyAutoSync(context: Context, prefs: Prefs) {
        val wm = WorkManager.getInstance(context)
        if (!prefs.autoSync) {
            wm.cancelUniqueWork(PERIODIC)
            return
        }
        val request = PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints(prefs))
            .build()
        wm.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun state(context: Context): Flow<SyncState> {
        val wm = WorkManager.getInstance(context)
        return combine(
            wm.getWorkInfosForUniqueWorkFlow(MANUAL),
            wm.getWorkInfosForUniqueWorkFlow(PERIODIC),
        ) { a, b ->
            val running = (a + b).firstOrNull { it.state == WorkInfo.State.RUNNING }
            val queued = (a + b).any { it.state == WorkInfo.State.ENQUEUED && a.contains(it) }
            when {
                running != null -> SyncState(
                    running = true,
                    text = running.progress.getString(SyncWorker.KEY_TEXT) ?: "Синхронизация…",
                    done = running.progress.getInt(SyncWorker.KEY_DONE, 0),
                    total = running.progress.getInt(SyncWorker.KEY_TOTAL, 0),
                )
                queued -> SyncState(running = true, text = "Жду подключения к сети…")
                else -> SyncState(running = false)
            }
        }
    }
}
