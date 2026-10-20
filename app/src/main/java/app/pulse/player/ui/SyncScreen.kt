package app.pulse.player.ui

import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pulse.player.PulseApp
import app.pulse.player.data.TrackStatus
import app.pulse.player.sync.Cookies
import app.pulse.player.sync.SyncManager
import app.pulse.player.sync.SyncState
import app.pulse.player.sync.YtDlp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SyncScreen(onLogin: () -> Unit) {
    val context = LocalContext.current
    val app = PulseApp.instance
    val prefs = app.prefs
    val scope = rememberCoroutineScope()

    var loggedIn by remember { mutableStateOf(Cookies.isLoggedIn(context)) }
    val sync by remember { SyncManager.state(context) }.collectAsState(initial = SyncState(false))
    val counts by app.db.tracks().statusCounts().collectAsState(initial = emptyList())
    val failed by app.db.tracks().failed().collectAsState(initial = emptyList())
    var showFailed by remember { mutableStateOf(false) }
    // Prefs are re-read when a sync finishes so the result line stays current.
    var lastResult by remember { mutableStateOf(prefs.lastSyncResult) }
    var lastSync by remember { mutableStateOf(prefs.lastSync) }
    LaunchedEffect(sync.running) {
        lastResult = prefs.lastSyncResult
        lastSync = prefs.lastSync
        loggedIn = Cookies.isLoggedIn(context)
    }

    val importCookies = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull().orEmpty()
        loggedIn = Cookies.import(context, text)
        Toast.makeText(
            context,
            if (loggedIn) "Cookies импортированы" else "В файле нет cookies YouTube",
            Toast.LENGTH_LONG,
        ).show()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Синхронизация", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Account
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (loggedIn) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    if (loggedIn) Spacer(Modifier.width(8.dp))
                    Text(
                        if (loggedIn) "Аккаунт YouTube Music подключён" else "Аккаунт не подключён",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (loggedIn) {
                    OutlinedButton(onClick = { Cookies.logout(context); loggedIn = false }) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Выйти")
                    }
                } else {
                    Text(
                        "Войди в Google прямо в приложении. Пароль никуда не уходит — сохраняются только cookies сессии, и только на этом телефоне.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onLogin) {
                        Icon(Icons.AutoMirrored.Rounded.Login, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Войти через Google")
                    }
                    TextButton(onClick = { importCookies.launch(arrayOf("text/*", "*/*")) }) {
                        Icon(Icons.Rounded.FileOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Или импортировать cookies.txt")
                    }
                }
            }
        }

        // Sync
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val downloaded = counts.firstOrNull { it.status == TrackStatus.DOWNLOADED }?.count ?: 0
                val pending = counts.firstOrNull { it.status == TrackStatus.PENDING }?.count ?: 0
                Text("На телефоне: $downloaded треков", style = MaterialTheme.typography.titleMedium)
                if (pending > 0 || failed.isNotEmpty()) {
                    Text(
                        "В очереди: $pending · с ошибкой: ${failed.size}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (sync.running) {
                    if (sync.total > 0) {
                        LinearProgressIndicator(
                            progress = { sync.done.toFloat() / sync.total },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("${sync.done} из ${sync.total}", style = MaterialTheme.typography.labelLarge)
                    } else {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    Text(sync.text, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { SyncManager.cancel(context) }) {
                        Icon(Icons.Rounded.Stop, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Остановить")
                    }
                } else {
                    if (lastSync > 0) {
                        Text(
                            "Последняя синхронизация: " + DateUtils.getRelativeTimeSpanString(lastSync),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    lastResult?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    Button(
                        enabled = loggedIn,
                        onClick = { SyncManager.syncNow(context, prefs) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Sync, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Синхронизировать лайки")
                    }
                }

                if (failed.isNotEmpty()) {
                    TextButton(onClick = { showFailed = !showFailed }) {
                        Text(if (showFailed) "Скрыть ошибки" else "Показать ошибки (${failed.size})")
                    }
                    if (showFailed) {
                        failed.take(50).forEach { t ->
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Text("${t.artist} — ${t.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    t.error.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Text(
                            "Треки с ошибкой будут повторены при следующей синхронизации.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Settings
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Настройки", style = MaterialTheme.typography.titleMedium)

                var parallel by remember { mutableFloatStateOf(prefs.parallelDownloads.toFloat()) }
                Text("Одновременных загрузок: ${parallel.toInt()}")
                Slider(
                    value = parallel,
                    onValueChange = { parallel = it },
                    onValueChangeFinished = { prefs.parallelDownloads = parallel.toInt() },
                    valueRange = 1f..4f,
                    steps = 2,
                )

                SettingSwitch("Только по Wi-Fi", "Не тратить мобильный трафик", prefs.wifiOnly) {
                    prefs.wifiOnly = it
                    SyncManager.applyAutoSync(context, prefs)
                }
                SettingSwitch("Автосинхронизация", "Проверять новые лайки каждые 12 часов", prefs.autoSync) {
                    prefs.autoSync = it
                    SyncManager.applyAutoSync(context, prefs)
                }
                SettingSwitch(
                    "Удалять снятые лайки",
                    "Если убрать лайк в YouTube Music, трек удалится с телефона",
                    prefs.deleteUnliked,
                ) { prefs.deleteUnliked = it }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                EngineRow()
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, initial: Boolean, onChange: (Boolean) -> Unit) {
    var checked by remember { mutableStateOf(initial) }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { checked = it; onChange(it) })
    }
}

@Composable
private fun EngineRow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(tick) { version = withContext(Dispatchers.IO) { YtDlp.version(context) } }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Движок yt-dlp")
            Text(
                version ?: "встроенная версия",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { YtDlp.update(context) } }
                busy = false
                tick++
                result.onSuccess { PulseApp.instance.prefs.lastEngineUpdate = System.currentTimeMillis() }
                Toast.makeText(
                    context,
                    result.fold({ if (it) "yt-dlp обновлён" else "Уже последняя версия" }, { "Не удалось обновить: ${it.message}" }),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }) { Text(if (busy) "Обновляю…" else "Обновить") }
    }
}
