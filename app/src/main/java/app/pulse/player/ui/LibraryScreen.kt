package app.pulse.player.ui

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pulse.player.PulseApp
import app.pulse.player.player.PlayerConnection

private val sorts = linkedMapOf(
    "liked" to "Недавно понравившиеся",
    "title" to "По названию",
    "artist" to "По исполнителю",
)

@Composable
fun LibraryScreen(onOpenSync: () -> Unit) {
    val app = PulseApp.instance
    val tracks by app.db.tracks().downloaded().collectAsState(initial = null)
    val player by PlayerConnection.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var sort by remember { mutableStateOf(app.prefs.librarySort) }
    var sortMenu by remember { mutableStateOf(false) }

    val all = tracks ?: return
    val visible = remember(all, query, sort) {
        val q = query.trim()
        val filtered = if (q.isEmpty()) all else all.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) || it.album?.contains(q, true) == true
        }
        when (sort) {
            "title" -> filtered.sortedBy { it.title.lowercase() }
            "artist" -> filtered.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            else -> filtered
        }
    }

    if (all.isEmpty()) {
        EmptyState(
            Icons.Rounded.LibraryMusic,
            "Здесь пока пусто",
            "Войди в YouTube Music и синхронизируй лайки — они скачаются на телефон.",
        ) { Button(onClick = onOpenSync) { Text("К синхронизации") } }
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 12.dp)) {
                Text("Библиотека", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${all.size} треков",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.padding(6.dp))
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Поиск по трекам и исполнителям") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Очистить") }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                Spacer(Modifier.padding(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { PlayerConnection.play(visible, 0) }) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Играть")
                    }
                    FilledTonalButton(onClick = { PlayerConnection.play(visible, shuffle = true) }) {
                        Icon(Icons.Rounded.Shuffle, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Перемешать")
                    }
                    Spacer(Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, "Сортировка") }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            sorts.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(label, color = if (key == sort) MaterialTheme.colorScheme.primary else Color.Unspecified)
                                    },
                                    onClick = {
                                        sort = key
                                        app.prefs.librarySort = key
                                        sortMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.padding(4.dp))
            }
        }
        if (visible.isEmpty()) {
            item {
                Text(
                    "Ничего не найдено",
                    Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(visible, key = { _, t -> t.id }) { index, track ->
            TrackRow(track, playing = track.id == player.mediaId, onClick = { PlayerConnection.play(visible, index) })
        }
    }
}
