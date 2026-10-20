package app.pulse.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import app.pulse.player.PulseApp
import app.pulse.player.data.Track
import app.pulse.player.player.PlayerConnection
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun MiniPlayer(onOpen: () -> Unit) {
    val state by PlayerConnection.state.collectAsState()
    if (state.mediaId == null) return
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column {
            LinearProgressIndicator(
                progress = { if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                drawStopIndicator = {},
            )
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cover(state.artworkUri?.path, 44.dp, corner = 6.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text(
                        state.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = PlayerConnection::togglePlay) {
                    Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Пауза/играть")
                }
                IconButton(onClick = PlayerConnection::next) { Icon(Icons.Rounded.SkipNext, "Следующий") }
            }
        }
    }
}

@Composable
fun PlayerScreen(onClose: () -> Unit, onEqualizer: () -> Unit) {
    val state by PlayerConnection.state.collectAsState()
    var seeking by remember { mutableStateOf<Float?>(null) }
    var addToPlaylist by remember { mutableStateOf<Track?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.mediaId) { if (state.mediaId == null) onClose() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Blurred cover as a soft backdrop.
        state.artworkUri?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(80.dp),
                alpha = 0.45f,
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background))
            )
        )

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Свернуть", Modifier.size(32.dp)) }
                Spacer(Modifier.weight(1f))
                Text("Сейчас играет", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEqualizer) { Icon(Icons.Rounded.Equalizer, "Эквалайзер") }
            }

            Spacer(Modifier.weight(1f))
            Cover(state.artworkUri?.path, 320.dp, corner = 20.dp, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            Spacer(Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        state.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        state.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = {
                    val id = state.mediaId ?: return@IconButton
                    scope.launch { addToPlaylist = PulseApp.instance.db.tracks().get(id) }
                }) { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, "В плейлист") }
            }

            Spacer(Modifier.height(16.dp))
            val duration = state.durationMs.coerceAtLeast(1)
            Slider(
                value = seeking ?: (state.positionMs.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = { seeking = it },
                onValueChangeFinished = {
                    seeking?.let { PlayerConnection.seekTo((it * duration).toLong()) }
                    seeking = null
                },
            )
            Row(Modifier.fillMaxWidth()) {
                val shown = seeking?.let { (it * duration).toLong() } ?: state.positionMs
                Text(formatDuration(shown / 1000), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text(formatDuration(state.durationMs / 1000), style = MaterialTheme.typography.labelMedium)
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = PlayerConnection::toggleShuffle) {
                    Icon(
                        Icons.Rounded.Shuffle, "Перемешать",
                        tint = if (state.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = PlayerConnection::previous, Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, "Предыдущий", Modifier.size(40.dp))
                }
                Box(
                    Modifier.size(76.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = PlayerConnection::togglePlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        "Пауза/играть",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp),
                    )
                }
                IconButton(onClick = PlayerConnection::next, Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, "Следующий", Modifier.size(40.dp))
                }
                IconButton(onClick = PlayerConnection::cycleRepeat) {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "Повтор",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    addToPlaylist?.let { AddToPlaylistDialog(it) { addToPlaylist = null } }
}
