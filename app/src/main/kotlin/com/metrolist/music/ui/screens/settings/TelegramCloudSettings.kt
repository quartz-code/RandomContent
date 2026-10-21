package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.cloud.TelegramCloud
import com.metrolist.music.constants.TelegramAutoUploadKey
import com.metrolist.music.constants.TelegramChatTitleKey
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch

/** Pulse: Telegram as a second, free place for downloaded songs. */
@Composable
fun TelegramCloudSettings(telegramCloud: TelegramCloud) {
    val scope = rememberCoroutineScope()
    val chatTitle by rememberPreference(TelegramChatTitleKey, defaultValue = "")
    val (autoUpload, onAutoUploadChange) = rememberPreference(TelegramAutoUploadKey, defaultValue = false)
    val state by telegramCloud.state.collectAsState()
    var showConnect by remember { mutableStateOf(false) }
    val connected = chatTitle.isNotBlank()

    val progressText = when (val s = state) {
        is TelegramCloud.State.Uploading ->
            stringResource(R.string.telegram_uploading, s.done, s.total, s.current)
        is TelegramCloud.State.Finished ->
            if (s.failed == 0) stringResource(R.string.telegram_finished, s.sent)
            else stringResource(R.string.telegram_finished_errors, s.sent, s.failed, s.error.orEmpty())
        TelegramCloud.State.Idle -> null
    }
    val counts = remember(state, connected) {
        telegramCloud.uploadedCount().coerceAtMost(telegramCloud.downloadedIds().size) to telegramCloud.downloadedIds().size
    }

    Material3SettingsGroup(
        title = stringResource(R.string.telegram_cloud),
        items = buildList {
            if (!connected) {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.backup),
                        title = { Text(stringResource(R.string.telegram_connect)) },
                        description = { Text(stringResource(R.string.telegram_connect_desc)) },
                        onClick = { showConnect = true },
                    ),
                )
            } else {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.backup),
                        title = { Text(stringResource(R.string.telegram_connected_to, chatTitle)) },
                        description = {
                            Text(progressText ?: stringResource(R.string.telegram_sent_count, counts.first, counts.second))
                        },
                        onClick = { telegramCloud.uploadPending() },
                    ),
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.sync),
                        title = { Text(stringResource(R.string.telegram_auto_upload)) },
                        description = { Text(stringResource(R.string.telegram_auto_upload_desc)) },
                        trailingContent = {
                            Switch(
                                checked = autoUpload,
                                onCheckedChange = {
                                    onAutoUploadChange(it)
                                    if (it) telegramCloud.uploadPending()
                                },
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(if (autoUpload) R.drawable.check else R.drawable.close),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = {
                            onAutoUploadChange(!autoUpload)
                            if (!autoUpload) telegramCloud.uploadPending()
                        },
                    ),
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.backup),
                        title = { Text(stringResource(R.string.telegram_upload_now)) },
                        description = { Text(stringResource(R.string.telegram_upload_now_desc)) },
                        onClick = { telegramCloud.uploadPending() },
                    ),
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.close),
                        title = { Text(stringResource(R.string.telegram_disconnect)) },
                        onClick = { scope.launch { telegramCloud.disconnect() } },
                    ),
                )
            }
        },
    )

    if (showConnect) {
        TelegramConnectDialog(telegramCloud = telegramCloud, onDismiss = { showConnect = false })
    }
}

@Composable
private fun TelegramConnectDialog(telegramCloud: TelegramCloud, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var token by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val noChatError = stringResource(R.string.telegram_error_no_chat)
    val tokenError = stringResource(R.string.telegram_error_token)

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.telegram_connect)) },
        text = {
            Column {
                Text(stringResource(R.string.telegram_steps), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { uriHandler.openUri("https://t.me/BotFather") }) {
                    Text(stringResource(R.string.telegram_open_botfather))
                }
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it; error = null },
                    singleLine = true,
                    enabled = !busy,
                    label = { Text(stringResource(R.string.telegram_token)) },
                    placeholder = { Text("123456789:AA…") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (busy) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.telegram_checking))
                    }
                }
                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = token.isNotBlank() && !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        telegramCloud.connect(token)
                            .onSuccess { onDismiss() }
                            .onFailure { e ->
                                error = when {
                                    e.message == "no_chat" -> noChatError
                                    e.message?.contains("Unauthorized", ignoreCase = true) == true ||
                                        e.message?.contains("Not Found", ignoreCase = true) == true -> tokenError
                                    else -> e.message
                                }
                            }
                        busy = false
                    }
                },
            ) { Text(stringResource(R.string.telegram_connect_button)) }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
