package com.metrolist.music.ui.screens.pulse

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.playback.queues.PulseQueue
import com.metrolist.music.pulse.feed.MyPulse
import com.metrolist.music.pulse.feed.PulseSettings
import com.metrolist.music.utils.rememberPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The logo's two colours: the pulse runs from coral to amber. */
internal val PulseCoral = Color(0xFFFF4F7B)
internal val PulseAmber = Color(0xFFFFA24C)
private val PulseInk = Color(0xFF14131A)

@HiltViewModel
class MyPulseViewModel @Inject constructor(val pulse: MyPulse) : ViewModel() {
    val settings = pulse.settings.stateIn(viewModelScope, SharingStarted.Eagerly, PulseSettings())
    var moods by mutableStateOf<List<PulseSettings.Mood>?>(null)
        private set

    fun loadMoods() {
        if (moods != null) return
        viewModelScope.launch { moods = runCatching { pulse.moods() }.getOrDefault(emptyList()) }
    }

    fun save(settings: PulseSettings) {
        viewModelScope.launch { pulse.saveSettings(settings) }
    }
}

/**
 * Pulse: "My pulse" — endless music picked for the listener: press "Listen", steer with a like or
 * "not for me", tune the character, language and mood. Drawn in the colours of the app's logo,
 * whose rings beat here, faster while the pulse plays.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPulseScreen(viewModel: MyPulseViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val feed by viewModel.pulse.feed.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val metadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val song by playerConnection.currentSong.collectAsStateWithLifecycle(null)
    val cookie by rememberPreference(InnerTubeCookieKey, "")
    val signedIn = "SAPISID" in cookie
    var starting by remember { mutableStateOf(false) }
    var tuning by remember { mutableStateOf(false) }
    val title = stringResource(R.string.my_pulse)
    val pulsePlaying = feed != null
    val current = metadata?.takeIf { pulsePlaying && feed?.played?.contains(it.id) == true }

    fun start() {
        if (starting) return
        starting = true
        scope.launch {
            val newFeed = viewModel.pulse.newFeed()
            playerConnection.playQueue(PulseQueue(viewModel.pulse, newFeed, title))
            starting = false
        }
    }

    val empty by viewModel.pulse.empty.collectAsState()
    LaunchedEffect(feed, metadata, empty) {
        // The first songs arrived, or none could be found: the spinner can go.
        if (current != null) starting = false
        if (empty && starting) {
            starting = false
            Toast.makeText(context, R.string.my_pulse_nothing, Toast.LENGTH_LONG).show()
        }
    }

    MyPulseContent(
        title = title,
        subtitle = describe(settings),
        pulsePlaying = pulsePlaying,
        isPlaying = isPlaying,
        starting = starting,
        now = current?.let { NowPlaying(it.title, it.artists.joinToString { a -> a.name }, it.thumbnailUrl, song?.song?.liked == true) },
        signedIn = signedIn,
        onListen = { if (pulsePlaying) playerConnection.togglePlayPause() else start() },
        onLike = {
            current?.let { playing ->
                playerConnection.toggleLike()
                scope.launch { viewModel.pulse.liked(playing.id) }
            }
        },
        onDislike = {
            current?.let { playing ->
                scope.launch { viewModel.pulse.disliked(playing) }
                playerConnection.seekToNext()
                Toast.makeText(context, R.string.my_pulse_disliked, Toast.LENGTH_SHORT).show()
            }
        },
        onNext = playerConnection::seekToNext,
        onTune = {
            tuning = true
            viewModel.loadMoods()
        },
    )

    if (tuning) {
        ModalBottomSheet(onDismissRequest = { tuning = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            TuneSheet(
                initial = settings,
                moods = viewModel.moods,
                onApply = { chosen ->
                    tuning = false
                    viewModel.save(chosen)
                    // Already playing: start over with the new settings, so the change is heard now.
                    if (pulsePlaying && chosen != settings) {
                        scope.launch {
                            viewModel.pulse.saveSettings(chosen)
                            start()
                        }
                    }
                },
            )
        }
    }
}

/** What the pulse plays now, for the card under the rings. */
data class NowPlaying(val title: String, val artist: String, val artwork: String?, val liked: Boolean)

/** The page without the player and the settings store, so it can be drawn on its own. */
@Composable
fun MyPulseContent(
    title: String,
    subtitle: String,
    pulsePlaying: Boolean,
    isPlaying: Boolean,
    starting: Boolean,
    now: NowPlaying?,
    signedIn: Boolean,
    onListen: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onNext: () -> Unit,
    onTune: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF221722), PulseInk, PulseInk))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth(0.86f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                PulseRings(beating = pulsePlaying && isPlaying, modifier = Modifier.fillMaxSize())
                Box(
                    Modifier
                        .fillMaxSize(0.42f)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PulseCoral, PulseAmber)))
                        .clickable(onClick = onListen),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        starting -> CircularProgressIndicator(color = PulseInk, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                        pulsePlaying -> Icon(painterResource(if (isPlaying) R.drawable.pulse_pause else R.drawable.pulse_play), null, tint = PulseInk, modifier = Modifier.size(48.dp))
                        else -> Text(stringResource(R.string.my_pulse_listen), color = PulseInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            if (now != null) {
                NowInPulse(now, onLike = onLike, onDislike = onDislike, onNext = onNext)
            } else {
                Text(
                    stringResource(R.string.my_pulse_tagline),
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                )
            }
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onTune) {
                Icon(painterResource(R.drawable.tune), null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.my_pulse_tune), color = Color.White)
            }
            if (!signedIn) {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.my_pulse_sign_in_hint), color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.navigationBarsPadding().height(96.dp))
        }
    }
}

@Composable
private fun NowInPulse(now: NowPlaying, onLike: () -> Unit, onDislike: () -> Unit, onNext: () -> Unit) {
    val (title, artist, artwork, liked) = now
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.07f)).padding(12.dp),
    ) {
        AsyncImage(artwork, null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FilledTonalIconButton(onClick = onDislike) {
            Icon(painterResource(R.drawable.thumb_down), stringResource(R.string.my_pulse_dislike))
        }
        FilledTonalIconButton(onClick = onLike) {
            Icon(painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border), stringResource(R.string.my_pulse_like), tint = if (liked) PulseCoral else Color.Unspecified)
        }
        FilledTonalIconButton(onClick = onNext) {
            Icon(painterResource(R.drawable.skip_next), stringResource(R.string.my_pulse_next))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TuneSheet(initial: PulseSettings, moods: List<PulseSettings.Mood>?, onApply: (PulseSettings) -> Unit) {
    var chosen by remember(initial) { mutableStateOf(initial) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.my_pulse_tune), style = MaterialTheme.typography.titleLarge)

        Section(stringResource(R.string.my_pulse_character))
        PulseSettings.Character.entries.forEach { character ->
            val selected = chosen.character == character
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) PulseCoral.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable { chosen = chosen.copy(character = character) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (selected) PulseCoral else MaterialTheme.colorScheme.outline))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(characterName(character)), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(characterHint(character)), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Section(stringResource(R.string.my_pulse_language))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PulseSettings.Language.entries.forEach { language ->
                FilterChip(selected = chosen.language == language, onClick = { chosen = chosen.copy(language = language) }, label = { Text(stringResource(languageName(language))) })
            }
        }

        Section(stringResource(R.string.my_pulse_mood))
        when {
            moods == null -> CircularProgressIndicator(Modifier.size(24.dp))
            moods.isEmpty() -> Text(stringResource(R.string.my_pulse_moods_unavailable), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = chosen.mood == null, onClick = { chosen = chosen.copy(mood = null) }, label = { Text(stringResource(R.string.my_pulse_mood_any)) })
                moods.forEach { mood ->
                    FilterChip(selected = chosen.mood?.browseId == mood.browseId && chosen.mood?.params == mood.params, onClick = { chosen = chosen.copy(mood = mood) }, label = { Text(mood.title) })
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Button(onClick = { onApply(chosen) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.my_pulse_apply)) }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
}

/** The logo's rings: a beat that spreads outwards, quicker while the pulse plays. */
@Composable
fun PulseRings(beating: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (beating) 1400 else 3200, easing = LinearEasing), RepeatMode.Restart),
        label = "rings",
    )
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxR = size.minDimension / 2
        val brush = Brush.linearGradient(listOf(PulseCoral, PulseAmber), start = Offset(0f, size.height), end = Offset(size.width, 0f))
        // Three rings, each a third of a beat behind the last; they fade as they grow.
        for (i in 0 until 3) {
            val t = (phase + i / 3f) % 1f
            val r = maxR * (0.45f + 0.55f * t)
            drawCircle(brush, radius = r, center = center, alpha = (1f - t) * 0.75f, style = Stroke(width = maxR * (0.07f - 0.04f * t)))
        }
    }
}

private fun characterName(c: PulseSettings.Character) = when (c) {
    PulseSettings.Character.MIX -> R.string.my_pulse_mix
    PulseSettings.Character.UNFAMILIAR -> R.string.my_pulse_unfamiliar
    PulseSettings.Character.FAVORITE -> R.string.my_pulse_favorite
    PulseSettings.Character.POPULAR -> R.string.my_pulse_popular
}

private fun characterHint(c: PulseSettings.Character) = when (c) {
    PulseSettings.Character.MIX -> R.string.my_pulse_mix_hint
    PulseSettings.Character.UNFAMILIAR -> R.string.my_pulse_unfamiliar_hint
    PulseSettings.Character.FAVORITE -> R.string.my_pulse_favorite_hint
    PulseSettings.Character.POPULAR -> R.string.my_pulse_popular_hint
}

private fun languageName(l: PulseSettings.Language) = when (l) {
    PulseSettings.Language.ANY -> R.string.my_pulse_language_any
    PulseSettings.Language.RUSSIAN -> R.string.my_pulse_language_russian
    PulseSettings.Language.FOREIGN -> R.string.my_pulse_language_foreign
}

@Composable
private fun describe(settings: PulseSettings): String = listOfNotNull(
    stringResource(characterName(settings.character)),
    settings.language.takeIf { it != PulseSettings.Language.ANY }?.let { stringResource(languageName(it)) },
    settings.mood?.title,
).joinToString(" · ")
