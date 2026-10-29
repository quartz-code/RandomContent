package app.pulse.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.pulse.importer.TrackMatcher
import com.metrolist.music.pulse.importer.VkMusicImport
import kotlinx.coroutines.launch

/** The "Import" tab: from Yandex Music or VK into the YouTube Music account, as on the phone. */
@Composable
fun ImportTab(model: ImportModel, songs: Songs, library: LibraryModel, onOpenLibrary: () -> Unit) {
    val phase by model.phase.collectAsState()
    when (val p = phase) {
        ImportModel.Phase.Idle, is ImportModel.Phase.Failed -> Sources(model, library, (p as? ImportModel.Phase.Failed)?.let(::problemText))
        ImportModel.Phase.Loading -> Busy(Strings.reading)
        is ImportModel.Phase.Matching -> Matching(model, p.done, p.total, offline = false)
        is ImportModel.Phase.Offline -> Matching(model, p.done, p.total, offline = true)
        ImportModel.Phase.Review -> Review(model, songs, library)
        is ImportModel.Phase.Saving -> Busy(Strings.saving(p.done, p.total), p.done.toFloat() / p.total.coerceAtLeast(1))
        is ImportModel.Phase.Done -> Done(model, p, onOpenLibrary)
    }
}

@Composable
private fun Sources(model: ImportModel, library: LibraryModel, problem: String?) {
    val vk by VkAccount.saved.collectAsState()
    var yandexLink by remember { mutableStateOf("") }
    var vkLink by remember { mutableStateOf("") }
    var passing by remember { mutableStateOf(false) }
    var playlists by remember { mutableStateOf<List<VkMusicImport.VkPlaylist>?>(null) }
    var playlistsLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val vkPlaylistLink = VkMusicImport.parsePlaylistLink(vkLink)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(Strings.importIntro, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f), modifier = Modifier.widthIn(max = 640.dp))
        problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Card(Strings.yandex) {
            Text(Strings.yandexHint, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = yandexLink,
                    onValueChange = { yandexLink = it },
                    placeholder = { Text("https://music.yandex.ru/…") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                Button(enabled = yandexLink.isNotBlank(), onClick = { model.loadYandex(yandexLink.trim()) }) { Text(Strings.load) }
            }
        }
        Card(Strings.vk) {
            val session = vk
            if (session == null) {
                Text(Strings.vkNeedsPhone, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                Spacer(Modifier.height(10.dp))
                Button(onClick = { passing = true }) { Text(Strings.vkSignIn) }
                return@Card
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.vkSignedIn, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                TextButton(onClick = { VkAccount.signOut(); playlists = null }) { Text(Strings.vkSignOut) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { model.loadVkMusic(session) }) { Text(Strings.vkMyMusic) }
                OutlinedButton(enabled = !playlistsLoading, onClick = {
                    playlistsLoading = true
                    scope.launch {
                        playlists = runCatching { VkMusicImport.loadPlaylists(session.userId, session.cookies) }.getOrDefault(emptyList())
                        playlistsLoading = false
                    }
                }) { Text(Strings.vkPlaylists) }
            }
            if (playlistsLoading) {
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator(Modifier.size(24.dp))
            }
            playlists?.let { list ->
                Spacer(Modifier.height(8.dp))
                if (list.isEmpty()) Text(Strings.vkNoPlaylists, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                list.forEach { playlist ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { model.loadVkPlaylist(session, playlist) }.padding(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Text(playlist.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        val about = listOfNotNull(playlist.author, playlist.trackCount?.let(Strings::songsCount)).joinToString(" · ")
                        if (about.isNotEmpty()) Text(about, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = vkLink,
                    onValueChange = { vkLink = it },
                    label = { Text(Strings.vkLinkHint) },
                    singleLine = true,
                    isError = vkLink.isNotBlank() && vkPlaylistLink == null,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                Button(enabled = vkPlaylistLink != null, onClick = { vkPlaylistLink?.let { model.loadVkPlaylist(session, it) } }) { Text(Strings.load) }
            }
        }
    }
    if (passing) AccountDialog(library, onDismiss = { passing = false }, passFromPhone = true)
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.widthIn(max = 640.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.04f)).padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun Busy(text: String, progress: Float? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (progress == null) CircularProgressIndicator() else LinearProgressIndicator(progress = { progress }, modifier = Modifier.width(320.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
    }
}

@Composable
private fun Matching(model: ImportModel, done: Int, total: Int, offline: Boolean) {
    val list by model.list.collectAsState()
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        list?.let { Text(it.title, style = MaterialTheme.typography.titleMedium) }
        LinearProgressIndicator(progress = { done.toFloat() / total.coerceAtLeast(1) }, modifier = Modifier.width(320.dp))
        Text(Strings.matching(done, total), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
        if (offline) {
            Text(Strings.matchingOffline, color = MaterialTheme.colorScheme.error)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = model::resume) { Text(Strings.resume) }
                TextButton(onClick = model::stopMatching) { Text(Strings.stop) }
            }
        } else {
            TextButton(onClick = model::stopMatching) { Text(Strings.stop) }
        }
    }
}

private enum class Filter { ALL, FOUND, UNSURE, NOT_FOUND }

@Composable
private fun Review(model: ImportModel, songs: Songs, library: LibraryModel) {
    val rows by model.rows.collectAsState()
    val list by model.list.collectAsState()
    val account by Account.info.collectAsState()
    val likedIds by library.likedIds.collectAsState()
    val player by songs.controller.state.collectAsState()
    val saveFailed by model.saveFailed.collectAsState()
    var filter by remember { mutableStateOf(Filter.ALL) }
    var playlistName by remember(list) { mutableStateOf(list?.title.orEmpty()) }
    var fixing by remember { mutableStateOf<ImportModel.Row?>(null) }
    val counts = rows.groupingBy { it.status }.eachCount()
    val chosen = rows.count { it.included && it.chosen != null }
    val shown = rows.filter {
        when (filter) {
            Filter.ALL -> true
            Filter.FOUND -> it.status == TrackMatcher.Status.MATCHED
            Filter.UNSURE -> it.status == TrackMatcher.Status.UNSURE
            Filter.NOT_FOUND -> it.status == TrackMatcher.Status.NOT_FOUND
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
            Text(list?.title.orEmpty(), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            TextButton(onClick = model::reset) { Text(Strings.newImport) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(filter == Filter.ALL, onClick = { filter = Filter.ALL }, label = { Text("${Strings.all} · ${rows.size}") })
            FilterChip(filter == Filter.FOUND, onClick = { filter = Filter.FOUND }, label = { Text("${Strings.found} · ${counts[TrackMatcher.Status.MATCHED] ?: 0}") })
            FilterChip(filter == Filter.UNSURE, onClick = { filter = Filter.UNSURE }, label = { Text("${Strings.unsure} · ${counts[TrackMatcher.Status.UNSURE] ?: 0}") })
            FilterChip(filter == Filter.NOT_FOUND, onClick = { filter = Filter.NOT_FOUND }, label = { Text("${Strings.notFound} · ${counts[TrackMatcher.Status.NOT_FOUND] ?: 0}") })
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp)) {
            items(shown, key = { it.index }) { row -> ReviewRow(model, row, onFix = { fixing = row }) }
        }
        // What to do with the selection.
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.chosenCount(chosen), modifier = Modifier.weight(1f))
                TextButton(onClick = { model.setAllIncluded(true) }) { Text(Strings.selectAll) }
                TextButton(onClick = { model.setAllIncluded(false) }) { Text(Strings.selectNone) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(enabled = chosen > 0 && player.followingHost == null, onClick = {
                    songs.controller.playQueue(model.chosenSongs().map(Track::from), 0)
                    model.save(ImportModel.Destination.Queue)
                }) { Text(Strings.playChosen) }
                OutlinedButton(enabled = chosen > 0 && account != null, onClick = { model.save(ImportModel.Destination.Liked, likedIds) }) { Text(Strings.likeAll) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text(Strings.playlistName) },
                    singleLine = true,
                    enabled = account != null,
                    modifier = Modifier.weight(1f),
                )
                Button(enabled = chosen > 0 && account != null, onClick = { model.save(ImportModel.Destination.NewPlaylist(playlistName.trim())) }) { Text(Strings.saveAsPlaylist) }
            }
            if (saveFailed) Text(Strings.importSaveFailed, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            if (account == null) Text(Strings.saveNeedsSignIn, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
    fixing?.let { row -> FixDialog(model, row, onDismiss = { fixing = null }) }
}

@Composable
private fun ReviewRow(model: ImportModel, row: ImportModel.Row, onFix: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val song = row.chosen
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Checkbox(checked = row.included, enabled = song != null, onCheckedChange = { model.setIncluded(row.index, it) })
        Box(Modifier.size(10.dp).clip(CircleShape).background(statusColor(row.status)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(row.source.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(row.source.artists.joinToString(", "), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Text("→", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 10.dp))
        Box(Modifier.weight(1.2f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { if (row.match?.candidates.isNullOrEmpty()) onFix() else menu = true }.padding(4.dp),
            ) {
                if (song != null) {
                    AsyncImage(song.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.06f)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artists.joinToString { it.name }, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                } else {
                    Text(Strings.notFoundRow, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 10.dp))
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                row.match?.candidates.orEmpty().forEach { candidate ->
                    DropdownMenuItem(
                        text = { CandidateText(candidate) },
                        onClick = {
                            model.choose(row.index, candidate.song)
                            menu = false
                        },
                    )
                }
                DropdownMenuItem(text = { Text(Strings.findMyself, color = MaterialTheme.colorScheme.primary) }, onClick = {
                    menu = false
                    onFix()
                })
            }
        }
    }
}

@Composable
private fun CandidateText(candidate: TrackMatcher.Candidate) {
    Column {
        Text(candidate.song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            listOfNotNull(candidate.song.artists.joinToString { it.name }, candidate.song.duration?.let { "%d:%02d".format(it / 60, it % 60) }, "${(candidate.score * 100).toInt()}%").joinToString(" · "),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

/** "Look it up": a search of one's own for a song the matcher missed. */
@Composable
private fun FixDialog(model: ImportModel, row: ImportModel.Row, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(listOf(row.source.artists.firstOrNull().orEmpty(), row.source.title).filter { it.isNotBlank() }.joinToString(" ")) }
    var results by remember { mutableStateOf<List<TrackMatcher.Candidate>?>(null) }
    var searching by remember { mutableStateOf(false) }
    fun search() {
        searching = true
        scope.launch {
            results = model.search(row.index, query.trim())
            searching = false
        }
    }
    LaunchedEffect(Unit) { search() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.findSong) },
        text = {
            Column(Modifier.widthIn(min = 420.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = query.isNotBlank() && !searching, onClick = ::search) { Text(Strings.searchTab) }
                }
                Spacer(Modifier.height(8.dp))
                when {
                    searching -> CircularProgressIndicator(Modifier.padding(16.dp))
                    results.isNullOrEmpty() -> Text(Strings.searchNothing, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(8.dp))
                    else -> LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(results!!, key = { it.song.id }) { candidate ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                                    model.choose(row.index, candidate.song)
                                    onDismiss()
                                }.padding(6.dp),
                            ) {
                                AsyncImage(candidate.song.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)))
                                Spacer(Modifier.width(10.dp))
                                CandidateText(candidate)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(Strings.close) } },
    )
}

@Composable
private fun Done(model: ImportModel, done: ImportModel.Phase.Done, onOpenLibrary: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            when (done.destination) {
                ImportModel.Destination.Queue -> Strings.playingImported(done.added)
                is ImportModel.Destination.NewPlaylist -> Strings.savedPlaylist(done.added)
                ImportModel.Destination.Liked -> Strings.savedLikes(done.added)
            },
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (done.destination != ImportModel.Destination.Queue) Button(onClick = onOpenLibrary) { Text(Strings.openMyMusic) }
            OutlinedButton(onClick = model::reset) { Text(Strings.newImport) }
        }
    }
}

private fun statusColor(status: TrackMatcher.Status?): Color = when (status) {
    TrackMatcher.Status.MATCHED -> Color(0xFF6FCF8E)
    TrackMatcher.Status.UNSURE -> Color(0xFFF0A43A)
    TrackMatcher.Status.NOT_FOUND -> Color(0xFFE0574F)
    null -> Color.Gray
}

private fun problemText(failed: ImportModel.Phase.Failed): String = when (failed.problem) {
    ImportModel.Problem.YANDEX_REGION -> Strings.importYandexRegion
    ImportModel.Problem.YANDEX_PRIVATE -> Strings.importYandexPrivate
    ImportModel.Problem.BAD_LINK -> Strings.importBadLink
    ImportModel.Problem.NETWORK -> Strings.importNetwork
    ImportModel.Problem.VK_SESSION -> Strings.importVkSession
    ImportModel.Problem.VK_DENIED -> Strings.importVkDenied(failed.detail)
    ImportModel.Problem.EMPTY -> Strings.importEmpty
    ImportModel.Problem.SAVE_FAILED -> Strings.importSaveFailed
}
