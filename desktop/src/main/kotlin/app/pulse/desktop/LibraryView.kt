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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pulse.desktop.res.Res
import app.pulse.desktop.res.arrow_back
import app.pulse.desktop.res.favorite
import app.pulse.desktop.res.shuffle
import coil3.compose.AsyncImage
import com.metrolist.music.pulse.LoginTransfer
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/** "My music": liked songs and the account's playlists, or an invitation to sign in. */
@Composable
fun LibraryTab(library: LibraryModel, songs: Songs, state: PlayerState, onSignIn: () -> Unit) {
    val account by Account.info.collectAsState()
    val controller = songs.controller
    val liked by library.liked.collectAsState()
    val playlists by library.playlists.collectAsState()
    val failed by library.failed.collectAsState()
    var open by remember { mutableStateOf<LibraryModel.Playlist?>(null) }
    LaunchedEffect(account) { if (account != null && liked == null) library.refresh() }

    if (account == null) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(Strings.libraryNeedsSignIn, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(12.dp))
            Button(onClick = onSignIn) { Text(Strings.signInWithPhone) }
        }
        return
    }
    val current = open
    if (current != null) {
        val tracks by produceState<List<Track>?>(null, current.id) {
            value = if (current.id == LIKED_ID) liked else library.playlistTracks(current.id) ?: emptyList()
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { open = null }) { Icon(painterResource(Res.drawable.arrow_back), Strings.back) }
                Text(current.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                val list = tracks.orEmpty()
                // A guest of a room suggests songs one by one; whole playlists are the host's to start.
                val canPlay = list.isNotEmpty() && state.followingHost == null
                FilledTonalButton(enabled = canPlay, onClick = { controller.playQueue(list, 0) }) { Text(Strings.play) }
                Spacer(Modifier.width(8.dp))
                IconButton(enabled = canPlay, onClick = {
                    controller.playQueue(list.shuffled(), 0)
                }) { Icon(painterResource(Res.drawable.shuffle), Strings.shuffle) }
            }
            Spacer(Modifier.height(8.dp))
            val list = tracks
            when {
                list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                list.isEmpty() -> Hint(Strings.playlistEmpty)
                else -> LazyColumn {
                    itemsIndexed(list, key = { i, t -> "${t.videoId}_$i" }) { i, track ->
                        SongRow(track, playing = state.current?.videoId == track.videoId, onClick = { songs.play(list, i) }, actions = { TrackActions(songs, track) })
                    }
                }
            }
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                AsyncImage(account?.avatar, null, contentScale = ContentScale.Crop, modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)))
                Spacer(Modifier.width(12.dp))
                Text(account?.name.orEmpty(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = library::refresh) { Text(Strings.refresh) }
            }
            if (failed) Text(Strings.libraryFailed, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        item {
            PlaylistRow(
                LibraryModel.Playlist(LIKED_ID, Strings.likedSongs, liked?.size?.let { Strings.songsCount(it) }, null),
                liked = true,
                onClick = { open = LibraryModel.Playlist(LIKED_ID, Strings.likedSongs, null, null) },
            )
        }
        item {
            Text(Strings.playlists, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        }
        val list = playlists
        if (list == null) {
            item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else {
            items(list, key = { it.id }) { playlist -> PlaylistRow(playlist, liked = false, onClick = { open = playlist }) }
        }
    }
}

private const val LIKED_ID = "LM"

@Composable
private fun PlaylistRow(playlist: LibraryModel.Playlist, liked: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        if (liked) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(painterResource(Res.drawable.favorite), null, tint = MaterialTheme.colorScheme.primary)
            }
        } else {
            AsyncImage(playlist.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.06f)))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(playlist.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            playlist.subtitle?.let { Text(it, fontSize = 13.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
        }
    }
}

/**
 * Sign-in from the phone: this window shows a code, the phone encrypts its session with it, and
 * the message pasted back here opens only with that code. [passFromPhone] shows that even when
 * signed in, for the VK sign-in the phone passes along.
 */
@Composable
fun AccountDialog(library: LibraryModel, onDismiss: () -> Unit, passFromPhone: Boolean = false) {
    val scope = rememberCoroutineScope()
    val signedIn by Account.info.collectAsState()
    val account = signedIn.takeUnless { passFromPhone }
    var code by remember { mutableStateOf(LoginTransfer.newPairingCode()) }
    var message by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) Strings.signInWithPhone else Strings.account) },
        text = {
            if (account != null) {
                Text(Strings.signedInAs(account!!.name))
                return@AlertDialog
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(Strings.signInSteps, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SelectionContainer {
                        Text(code, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp), color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { code = LoginTransfer.newPairingCode(); error = null }) { Text(Strings.newCode) }
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it; error = null },
                    placeholder = { Text(Strings.pasteMessage) },
                    maxLines = 4,
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            if (account != null) {
                TextButton(onClick = {
                    Account.signOut()
                    library.refresh()
                    onDismiss()
                }) { Text(Strings.signOut) }
            } else {
                TextButton(enabled = message.isNotBlank() && !working, onClick = {
                    working = true
                    scope.launch {
                        val result = Account.signIn(message, code)
                        working = false
                        error = when (result) {
                            Account.Result.Ok -> null
                            Account.Result.NotFound -> Strings.loginNotFound
                            Account.Result.WrongCode -> Strings.loginWrongCode
                            Account.Result.Expired -> Strings.loginExpired
                            Account.Result.Rejected -> Strings.loginRejected
                        }
                        if (result == Account.Result.Ok) {
                            library.refresh()
                            onDismiss()
                        }
                    }
                }) { Text(if (working) Strings.loading else Strings.signIn) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.close) } },
    )
}
