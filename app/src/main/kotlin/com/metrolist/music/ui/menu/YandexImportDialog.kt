package com.metrolist.music.ui.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.ui.component.StepGuide
import com.metrolist.music.pulse.importer.ImportedList
import com.metrolist.music.pulse.importer.YandexMusicImport
import kotlinx.coroutines.launch

/**
 * Pulse: step one of the Yandex import. Loads the track list from a Yandex Music link; matching and
 * review happen in the import review screen.
 */
@Composable
fun YandexImportDialog(
    onDismiss: () -> Unit,
    onLoaded: (ImportedList) -> Unit,
    showGuide: Boolean = true,
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var loaded by remember { mutableStateOf<ImportedList?>(null) }

    LaunchedEffect(Unit) {
        clipboard.getText()?.text?.takeIf(YandexMusicImport::isYandexLink)?.let { link = it.trim() }
    }

    fun load() {
        loading = true
        error = null
        scope.launch {
            try {
                val playlist = YandexMusicImport.load(link)
                if (playlist.tracks.isEmpty()) error = R.string.yandex_import_empty else loaded = playlist
            } catch (e: YandexMusicImport.Failure) {
                error = when (e) {
                    is YandexMusicImport.Failure.Region -> R.string.yandex_import_error_region
                    is YandexMusicImport.Failure.Private -> R.string.yandex_import_error_private
                    is YandexMusicImport.Failure.BadLink -> R.string.yandex_import_error_link
                    is YandexMusicImport.Failure.Network -> R.string.yandex_import_error_network
                }
            } catch (e: Exception) {
                error = R.string.yandex_import_error_network
            } finally {
                loading = false
            }
        }
    }

    val playlist = loaded
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        icon = { Icon(painterResource(R.drawable.playlist_add), contentDescription = null) },
        title = { Text(stringResource(R.string.yandex_import)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (playlist == null && showGuide) {
                    StepGuide(
                        title = stringResource(R.string.guide_how_to_connect),
                        steps = listOf(
                            stringResource(R.string.yandex_setup_1),
                            stringResource(R.string.yandex_setup_2),
                            stringResource(R.string.yandex_setup_3),
                            stringResource(R.string.yandex_setup_4),
                            stringResource(R.string.yandex_setup_5),
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (playlist == null) {
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it; error = null },
                        singleLine = true,
                        enabled = !loading,
                        placeholder = { Text("music.yandex.ru/users/…/playlists/…") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (loading) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.yandex_import_loading))
                        }
                    }
                    error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(it), color = MaterialTheme.colorScheme.error)
                    }
                    if (showGuide) {
                        Spacer(Modifier.height(16.dp))
                        StepGuide(
                            title = stringResource(R.string.guide_good_to_know),
                            steps = listOf(
                                stringResource(R.string.yandex_note_1),
                                stringResource(R.string.yandex_note_2),
                                stringResource(R.string.yandex_note_3),
                            ),
                            numbered = false,
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.yandex_import_loaded, playlist.title, playlist.tracks.size),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.yandex_import_next_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            if (playlist == null) {
                TextButton(enabled = link.isNotBlank() && !loading, onClick = ::load) {
                    Text(stringResource(R.string.yandex_import_load))
                }
            } else {
                TextButton(onClick = { onLoaded(playlist) }) {
                    Text(stringResource(R.string.yandex_import_find))
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !loading, onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
