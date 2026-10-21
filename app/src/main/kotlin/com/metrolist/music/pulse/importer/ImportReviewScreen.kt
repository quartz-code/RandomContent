package com.metrolist.music.pulse.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.utils.backToMain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(val session: ImportSession) : ViewModel()

private enum class Filter { ALL, MATCHED, UNSURE, NOT_FOUND }

private fun formatDuration(sec: Int?): String =
    if (sec == null || sec <= 0) "" else "%d:%02d".format(sec / 60, sec % 60)

private fun SongItem.artistLine() = artists.joinToString(", ") { it.name }

@Composable
private fun statusColor(status: TrackMatcher.Status?): Color = when (status) {
    TrackMatcher.Status.MATCHED -> MaterialTheme.colorScheme.primary
    TrackMatcher.Status.UNSURE -> MaterialTheme.colorScheme.tertiary
    TrackMatcher.Status.NOT_FOUND -> MaterialTheme.colorScheme.error
    null -> MaterialTheme.colorScheme.outline
}

/** Pulse: review an import before it is saved — what was found, what needs a look, what is missing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportReviewScreen(navController: NavController, viewModel: ImportViewModel = hiltViewModel()) {
    val session = viewModel.session
    val list by session.list.collectAsState()
    val rows by session.rows.collectAsState()
    val phase by session.phase.collectAsState()
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    var editing by remember { mutableStateOf<Int?>(null) }
    val sourceName = stringResource(if (list?.source == ImportSource.VK) R.string.import_from_vk else R.string.import_from_yandex)

    Column(Modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current)) {
        when (val p = phase) {
            is ImportSession.Phase.Done -> {
                DoneContent(p, onOpen = {
                    session.reset()
                    navController.popBackStack()
                    if (p.playlistId != null) navController.navigate("local_playlist/${p.playlistId}")
                    else navController.navigate("auto_playlist/liked")
                }, onClose = {
                    session.reset()
                    navController.popBackStack()
                })
                return@Column
            }
            ImportSession.Phase.Idle -> {
                Text(stringResource(R.string.import_nothing), Modifier.padding(24.dp))
                return@Column
            }
            else -> Unit
        }

        val counts = remember(rows) {
            mapOf(
                Filter.ALL to rows.size,
                Filter.MATCHED to rows.count { it.status == TrackMatcher.Status.MATCHED },
                Filter.UNSURE to rows.count { it.status == TrackMatcher.Status.UNSURE },
                Filter.NOT_FOUND to rows.count { it.status == TrackMatcher.Status.NOT_FOUND },
            )
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(list?.title.orEmpty(), style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.import_source_line, sourceName, rows.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            when (val p = phase) {
                is ImportSession.Phase.Matching -> {
                    LinearProgressIndicator(progress = { if (p.total == 0) 0f else p.done / p.total.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.import_matching, p.done, p.total),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = session::cancel) { Text(stringResource(R.string.import_stop)) }
                    }
                }
                is ImportSession.Phase.Offline -> {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(
                                stringResource(R.string.import_offline, p.done, p.total),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row {
                                TextButton(onClick = session::resume) { Text(stringResource(R.string.import_continue)) }
                                TextButton(onClick = session::cancel) { Text(stringResource(R.string.import_stop)) }
                            }
                        }
                    }
                }
                is ImportSession.Phase.Saving -> {
                    LinearProgressIndicator(progress = { if (p.total == 0) 0f else p.done / p.total.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.import_saving, p.done, p.total), style = MaterialTheme.typography.bodyMedium)
                }
                else -> Text(stringResource(R.string.import_review_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Filter.entries) { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = {
                        Text(
                            stringResource(
                                when (f) {
                                    Filter.ALL -> R.string.import_filter_all
                                    Filter.MATCHED -> R.string.import_filter_matched
                                    Filter.UNSURE -> R.string.import_filter_unsure
                                    Filter.NOT_FOUND -> R.string.import_filter_not_found
                                },
                                counts[f] ?: 0,
                            ),
                        )
                    },
                )
            }
        }

        val visible = remember(rows, filter) {
            when (filter) {
                Filter.ALL -> rows
                Filter.MATCHED -> rows.filter { it.status == TrackMatcher.Status.MATCHED }
                Filter.UNSURE -> rows.filter { it.status == TrackMatcher.Status.UNSURE }
                Filter.NOT_FOUND -> rows.filter { it.status == TrackMatcher.Status.NOT_FOUND }
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
            items(visible, key = { it.index }) { row ->
                ImportRowItem(
                    row = row,
                    sourceName = sourceName,
                    onToggle = { session.setIncluded(row.index, it) },
                    onClick = { if (row.match != null) editing = row.index },
                )
            }
        }

        if (phase is ImportSession.Phase.Review || phase is ImportSession.Phase.Offline) {
            SaveBar(
                defaultName = list?.title.orEmpty(),
                selected = rows.count { it.included && it.chosen != null },
                onSave = session::save,
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.import_review_title)) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )

    editing?.let { index ->
        rows.firstOrNull { it.index == index }?.let { row ->
            CandidateDialog(row, sourceName, session, onDismiss = { editing = null })
        }
    }
}

@Composable
private fun ImportRowItem(row: ImportSession.Row, sourceName: String, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    val song = row.chosen
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (song != null) {
                AsyncImage(model = song.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (row.match == null) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(painterResource(R.drawable.music_note), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song?.title ?: row.source.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOf(song?.artistLine() ?: row.source.artists.joinToString(", "), formatDuration(song?.duration ?: row.source.durationSec))
                    .filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val statusText = when (row.status) {
                TrackMatcher.Status.MATCHED -> stringResource(if (row.pickedByUser) R.string.import_status_picked else R.string.import_status_matched)
                TrackMatcher.Status.UNSURE -> stringResource(R.string.import_status_unsure)
                TrackMatcher.Status.NOT_FOUND -> stringResource(R.string.import_status_not_found)
                null -> stringResource(R.string.import_status_searching)
            }
            Text(
                if (song != null) "$statusText · $sourceName: ${row.source.artists.joinToString(", ")} — ${row.source.title}" else statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor(row.status),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Checkbox(checked = row.included, enabled = song != null, onCheckedChange = onToggle)
    }
}

@Composable
private fun SaveBar(defaultName: String, selected: Int, onSave: (ImportSession.Destination, Boolean) -> Unit) {
    var toLiked by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable(defaultName) { mutableStateOf(defaultName) }
    var download by rememberSaveable { mutableStateOf(true) }
    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth().clickable { toLiked = false }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !toLiked, onClick = { toLiked = false })
                Text(stringResource(R.string.import_to_playlist))
            }
            if (!toLiked) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.import_playlist_name)) },
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                )
            }
            Row(Modifier.fillMaxWidth().clickable { toLiked = true }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = toLiked, onClick = { toLiked = true })
                Column {
                    Text(stringResource(R.string.import_to_liked))
                    Text(stringResource(R.string.import_to_liked_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth().clickable { download = !download }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = download, onCheckedChange = { download = it })
                Text(stringResource(R.string.import_download))
            }
            Button(
                enabled = selected > 0,
                onClick = {
                    onSave(if (toLiked) ImportSession.Destination.Liked else ImportSession.Destination.NewPlaylist(name.trim()), download)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.import_add_n, selected)) }
        }
    }
}

@Composable
private fun CandidateDialog(row: ImportSession.Row, sourceName: String, session: ImportSession, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember(row.index) {
        mutableStateOf(listOf(row.source.artists.firstOrNull().orEmpty(), TrackMatcher.cleanTitle(row.source.title, row.source.artists)).filter { it.isNotBlank() }.joinToString(" "))
    }
    var results by remember(row.index) { mutableStateOf(row.match?.candidates.orEmpty()) }
    var searching by remember { mutableStateOf(false) }

    fun runSearch() {
        searching = true
        scope.launch {
            results = runCatching { session.search(row.index, query) }.getOrDefault(emptyList())
            searching = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_pick_title)) },
        text = {
            Column {
                Text(
                    "$sourceName: ${row.source.artists.joinToString(", ")} — ${row.source.title} ${formatDuration(row.source.durationSec)}".trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.import_search_myself)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                    trailingIcon = {
                        androidx.compose.material3.IconButton(onClick = ::runSearch) {
                            Icon(painterResource(R.drawable.search), contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                if (searching) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else if (results.isEmpty()) {
                    Text(stringResource(R.string.import_nothing_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(results, key = { it.song.id }) { candidate ->
                        val song = candidate.song
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                                session.choose(row.index, song)
                                onDismiss()
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = song.thumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (song.id == row.chosen?.id) FontWeight.Bold else FontWeight.Normal)
                                Text(
                                    listOf(song.artistLine(), formatDuration(song.duration)).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                "${(candidate.score * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor(
                                    when {
                                        candidate.score >= TrackMatcher.CONFIDENT -> TrackMatcher.Status.MATCHED
                                        candidate.score >= TrackMatcher.POSSIBLE -> TrackMatcher.Status.UNSURE
                                        else -> TrackMatcher.Status.NOT_FOUND
                                    },
                                ),
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = {
            if (row.included) {
                TextButton(onClick = { session.setIncluded(row.index, false); onDismiss() }) { Text(stringResource(R.string.import_skip_track)) }
            }
        },
    )
}

@Composable
private fun DoneContent(done: ImportSession.Phase.Done, onOpen: () -> Unit, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(R.drawable.done), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        val text = when (val d = done.destination) {
            is ImportSession.Destination.NewPlaylist -> stringResource(R.string.import_done_playlist, done.added, d.name)
            ImportSession.Destination.Liked -> stringResource(R.string.import_done_liked, done.added)
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (done.playlistId != null) R.string.import_open_playlist else R.string.import_open_liked))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.import_close)) }
    }
}
