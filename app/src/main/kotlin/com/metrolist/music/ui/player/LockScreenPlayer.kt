package com.metrolist.music.ui.player

import android.text.format.DateFormat
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.ui.theme.Oswald
import com.metrolist.music.ui.theme.PtMono
import kotlinx.coroutines.delay
import java.util.Date

/**
 * Pulse: the player over the lock screen, in the player theme. A swipe up asks Android to unlock
 * (and closes this screen); everything else works without unlocking, as on the player itself.
 */
@Composable
fun LockScreenPlayer(onUnlock: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val theme = rememberPlayerTheme()
    val metadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val shuffle by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val canPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val artworkColors = rememberArtworkColors(metadata?.thumbnailUrl)

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(isPlaying, metadata?.id) {
        while (true) {
            position = playerConnection.player.currentPosition
            duration = playerConnection.player.duration.coerceAtLeast(0L)
            if (!isPlaying) break
            delay(500)
        }
    }

    val sessionId = remember(metadata?.id) { playerConnection.player.audioSessionId }
    val levels = rememberAudioLevels(sessionId, isPlaying && theme.usesVisualizer)
    val song = metadata
    val isEpisode = currentSong?.song?.isEpisode == true
    LockScreenLayout(
        theme = theme,
        artworkColors = artworkColors,
        thumbnailUrl = song?.thumbnailUrl,
        track = song?.let {
            LockTrack(
                title = it.title,
                artists = it.artists.filter { a -> a.name.isNotBlank() }.joinToString { a -> a.name },
                isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true,
            )
        },
        isPlaying = isPlaying,
        position = seeking ?: position,
        duration = duration,
        shuffle = shuffle,
        repeatMode = repeatMode,
        canPrevious = canPrevious,
        canNext = canNext,
        levels = levels,
        onUnlock = onUnlock,
        onLike = playerConnection::toggleLike,
        onSeek = { seeking = it },
        onSeekFinished = {
            seeking?.let {
                playerConnection.player.seekTo(it)
                position = it
            }
            seeking = null
        },
        onPlayPause = playerConnection::togglePlayPause,
        onPrevious = playerConnection::seekToPrevious,
        onNext = playerConnection::seekToNext,
        onShuffle = { playerConnection.player.shuffleModeEnabled = !shuffle },
        onRepeat = { playerConnection.player.toggleRepeatMode() },
    )
}

internal data class LockTrack(val title: String, val artists: String, val isFavorite: Boolean)

/** The lock screen's look, without the player behind it (so it can be drawn on its own). */
@Composable
internal fun LockScreenLayout(
    theme: PlayerTheme,
    artworkColors: List<androidx.compose.ui.graphics.Color>,
    thumbnailUrl: String?,
    track: LockTrack?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    shuffle: Boolean,
    repeatMode: Int,
    canPrevious: Boolean,
    canNext: Boolean,
    levels: () -> FloatArray,
    onUnlock: () -> Unit,
    onLike: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    artwork: (@Composable (Modifier) -> Unit)? = null,
) {
    // Follows the drag, so the screen visibly lifts before it unlocks.
    var lift by remember { mutableFloatStateOf(0f) }
    val unlockDistance = with(LocalDensity.current) { 140.dp.toPx() }

    CompositionLocalProvider(LocalPulseArtworkColor provides artworkColors.firstOrNull()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = -lift
                    alpha = 1f - (lift / (unlockDistance * 2)).coerceIn(0f, 0.6f)
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (lift > unlockDistance) onUnlock()
                            lift = 0f
                        },
                        onDragCancel = { lift = 0f },
                    ) { _, dy -> lift = (lift - dy).coerceAtLeast(0f) }
                },
        ) {
            PulsePlayerBackground(theme, artworkColors, thumbnailUrl)
            if (theme.motion != PlayerTheme.Motion.NONE) {
                PulseMotionLayer(theme, artworkColors, pulsePalette(theme).accent, levels, isPlaying, Modifier.fillMaxSize())
            }
            val palette = pulsePalette(theme)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
            ) {
                Spacer(Modifier.height(24.dp))
                LockClock(theme, palette)
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val shape = pulseArtShape(theme)
                    val border = pulseArtBorder(theme)
                    val artModifier = Modifier
                        .padding(horizontal = theme.artPadding + 24.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .then(if (theme.artShadow) Modifier.shadow(18.dp, shape) else Modifier)
                        .clip(shape)
                        .then(border?.let { Modifier.border(it, shape) } ?: Modifier)
                    if (artwork != null) {
                        artwork(artModifier)
                    } else {
                        AsyncImage(model = thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = artModifier)
                    }
                }
                if (track != null) {
                    PulseControlsColumn(theme) {
                        PulseTrackInfo(
                            theme = theme,
                            title = track.title,
                            artists = track.artists,
                            isFavorite = track.isFavorite,
                            onLike = onLike,
                            onTitleClick = {},
                            onTitleLongClick = {},
                            onArtistsClick = {},
                            onArtistsLongClick = {},
                        )
                        PulseProgress(
                            theme = theme,
                            position = position,
                            duration = duration,
                            enabled = true,
                            onSeek = onSeek,
                            onSeekFinished = onSeekFinished,
                        )
                        PulseTransport(
                            theme = theme,
                            playIcon = if (isPlaying) R.drawable.pulse_pause else R.drawable.pulse_play,
                            playLabel = stringResource(if (isPlaying) R.string.player_pause else R.string.play),
                            canPrevious = canPrevious,
                            canNext = canNext,
                            controlsEnabled = true,
                            shuffleOn = shuffle,
                            repeatMode = repeatMode,
                            onPlayPause = onPlayPause,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onShuffle = onShuffle,
                            onRepeat = onRepeat,
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.expand_less),
                        contentDescription = null,
                        tint = palette.muted,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(stringResource(R.string.lock_screen_swipe), color = palette.muted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun LockClock(theme: PlayerTheme, palette: PulsePalette) {
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000L - value % 60_000L)
        }
    }
    val time = remember(now) { DateFormat.getTimeFormat(context).format(Date(now)) }
    val date = remember(now) { DateFormat.format("EEEE, d MMMM", now).toString().replaceFirstChar { it.uppercase() } }
    val family = when (theme.font) {
        PlayerTheme.Font.SYSTEM -> null
        PlayerTheme.Font.OSWALD -> Oswald
        PlayerTheme.Font.MONO -> PtMono
    }
    Text(
        time,
        style = TextStyle(fontFamily = family, fontWeight = FontWeight.Light, fontSize = 76.sp, lineHeight = 80.sp, textAlign = TextAlign.Center),
        color = palette.content,
    )
    Text(date, style = TextStyle(fontFamily = family, fontSize = 16.sp), color = palette.muted)
}
