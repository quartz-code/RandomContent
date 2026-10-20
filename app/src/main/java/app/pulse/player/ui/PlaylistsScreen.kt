package app.pulse.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pulse.player.PulseApp
import app.pulse.player.data.Playlist
import app.pulse.player.player.PlayerConnection
import kotlinx.coroutines.launch

@Composable
fun PlaylistsScreen(onOpen: (Long) -> Unit) {
    val dao = PulseApp.instance.db.playlists()
    val playlists by dao.playlists().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        val list = playlists
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyState(
                Icons.AutoMirrored.Rounded.QueueMusic,
                "Нет плейлистов",
                "Собирай свои подборки: меню трека → «Добавить в плейлист».",
            )
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                item {
                    Text(
                        "Плейлисты",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(list, key = { it.id }) { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpen(p.id) }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Cover(p.coverPath, 64.dp, corner = 12.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${p.trackCount} треков",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { creating = true },
            icon = { Icon(Icons.Rounded.Add, null) },
            text = { Text("Новый") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (creating) {
        NameDialog("Новый плейлист", "", onDismiss = { creating = false }) { name ->
            creating = false
            scope.launch { dao.insert(Playlist(name = name)) }
        }
    }
}

@Composable
fun PlaylistDetailScreen(playlistId: Long, onBack: () -> Unit) {
    val dao = PulseApp.instance.db.playlists()
    val playlist by dao.playlist(playlistId).collectAsState(initial = null)
    val tracks by dao.tracks(playlistId).collectAsState(initial = emptyList())
    val player by PlayerConnection.state.collectAsState()
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Меню") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Переименовать") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = { menu = false; renaming = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить плейлист") },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) },
                            onClick = { menu = false; deleting = true },
                        )
                    }
                }
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
                Cover(tracks.firstOrNull()?.coverPath, 160.dp, corner = 16.dp)
                Spacer(Modifier.padding(8.dp))
                Text(playlist?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${tracks.size} треков · ${formatDuration(tracks.sumOf { it.durationSec.toLong() })}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.padding(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = tracks.isNotEmpty(), onClick = { PlayerConnection.play(tracks) }) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Играть")
                    }
                    FilledTonalButton(enabled = tracks.isNotEmpty(), onClick = { PlayerConnection.play(tracks, shuffle = true) }) {
                        Icon(Icons.Rounded.Shuffle, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Перемешать")
                    }
                }
                Spacer(Modifier.padding(6.dp))
            }
        }
        itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
            TrackRow(
                track,
                playing = track.id == player.mediaId,
                onClick = { PlayerConnection.play(tracks, index) },
                onRemoveFromPlaylist = { scope.launch { dao.removeEntry(playlistId, track.id) } },
            )
        }
    }

    if (renaming) {
        NameDialog("Переименовать", playlist?.name.orEmpty(), onDismiss = { renaming = false }) { name ->
            renaming = false
            scope.launch { dao.rename(playlistId, name) }
        }
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("Удалить плейлист?") },
            text = { Text("Сами треки останутся в библиотеке.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = false
                    scope.launch { dao.delete(playlistId) }
                    onBack()
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Отмена") } },
        )
    }
}
