package com.metrolist.music.ui.screens.pulse

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.playback.queues.PulseQueue
import com.metrolist.music.pulse.feed.MyPulse
import com.metrolist.music.pulse.feed.PulseSettings
import com.metrolist.music.ui.player.rememberArtworkColors
import com.metrolist.music.ui.player.rememberAudioLevels
import com.metrolist.music.utils.rememberPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
 * Pulse: "My pulse" — endless music picked for the listener: press the heart, steer with a like
 * or "not for me", switch the character right on the page or tune everything in the sheet.
 * The page is its own world, dark and lit from inside, whatever the app theme: like the player.
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
    val starting by viewModel.pulse.starting.collectAsState()
    var tuning by remember { mutableStateOf(false) }
    val title = stringResource(R.string.my_pulse)
    val pulsePlaying = feed != null
    // Everything the pulse's queue plays counts: its own songs, and a song added with "Play next".
    val current = metadata?.takeIf { pulsePlaying }

    fun start() {
        if (starting) return
        scope.launch { playerConnection.playQueue(PulseQueue(viewModel.pulse, viewModel.pulse.newFeed(), title)) }
    }

    fun apply(chosen: PulseSettings) {
        if (chosen == settings) return
        viewModel.save(chosen)
        // Already playing: start over with the new settings, so the change is heard now.
        if (pulsePlaying) {
            scope.launch {
                viewModel.pulse.saveSettings(chosen)
                start()
            }
        }
    }

    val empty by viewModel.pulse.empty.collectAsState()
    LaunchedEffect(empty) {
        if (empty) Toast.makeText(context, R.string.my_pulse_nothing, Toast.LENGTH_LONG).show()
    }

    val beating = pulsePlaying && isPlaying
    val sessionId = remember(metadata?.id) { playerConnection.player.audioSessionId }
    MyPulseContent(
        title = title,
        settings = settings,
        pulsePlaying = pulsePlaying,
        isPlaying = isPlaying,
        starting = starting,
        now = current?.let { NowPlaying(it.title, it.artists.joinToString { a -> a.name }, song?.song?.liked == true) },
        signedIn = signedIn,
        artworkColors = rememberArtworkColors(current?.thumbnailUrl),
        time = rememberPulseClock(beating),
        levels = rememberAudioLevels(sessionId, beating),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        artwork = { modifier -> AsyncImage(current?.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = modifier) },
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
        onCharacter = { apply(settings.copy(character = it)) },
        onTune = {
            tuning = true
            viewModel.loadMoods()
        },
    )

    if (tuning) {
        ModalBottomSheet(
            onDismissRequest = { tuning = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = PulseSheet,
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.25f)) },
        ) {
            TuneSheet(
                initial = settings,
                moods = viewModel.moods,
                onApply = { chosen ->
                    tuning = false
                    apply(chosen)
                },
            )
        }
    }
}

/** What the pulse plays now, for the words under the heart. */
data class NowPlaying(val title: String, val artist: String, val liked: Boolean)

/** The page without the player and the settings store, so it can be drawn on its own. */
@Composable
fun MyPulseContent(
    title: String,
    settings: PulseSettings,
    pulsePlaying: Boolean,
    isPlaying: Boolean,
    starting: Boolean,
    now: NowPlaying?,
    signedIn: Boolean,
    artworkColors: List<Color>,
    time: () -> Float,
    levels: () -> FloatArray,
    contentPadding: PaddingValues,
    artwork: @Composable (Modifier) -> Unit,
    onListen: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onNext: () -> Unit,
    onCharacter: (PulseSettings.Character) -> Unit,
    onTune: () -> Unit,
) {
    val energy = remember(levels) {
        {
            val bands = levels()
            var sum = 0f
            for (i in 0 until minOf(8, bands.size)) sum += bands[i]
            sum / 8f
        }
    }
    Box(Modifier.fillMaxSize().background(PulseInk)) {
        PulseAura(artworkColors, time, energy, Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = contentPadding.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(title, onTune)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val heart = min(maxWidth * 0.84f, maxHeight * 0.64f)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PulseHeart(time, levels, onListen, Modifier.size(heart)) {
                        HeartFace(starting, pulsePlaying, isPlaying, now != null, artwork)
                    }
                    Spacer(Modifier.height(4.dp))
                    AnimatedContent(now, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "now") { playing ->
                        Caption(playing, describe(settings))
                    }
                }
            }
            if (now != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 36.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    PulseAction(R.drawable.thumb_down, stringResource(R.string.my_pulse_dislike), onDislike)
                    PulseAction(if (now.liked) R.drawable.favorite else R.drawable.favorite_border, stringResource(R.string.my_pulse_like), onLike, highlighted = now.liked)
                    PulseAction(R.drawable.skip_next, stringResource(R.string.my_pulse_next), onNext)
                }
            } else {
                Text(
                    stringResource(R.string.my_pulse_tagline),
                    color = Color.White.copy(alpha = 0.62f),
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PulseSettings.Character.entries.forEach { character ->
                    Pill(characterIcon(character), stringResource(characterName(character)), settings.character == character) { onCharacter(character) }
                }
            }
            if (!signedIn) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.padding(horizontal = 20.dp).fillMaxWidth().glass(RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.account), null, tint = PulseAmber, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.my_pulse_sign_in_hint), color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun Header(title: String, onTune: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        // The launcher logo itself; its drawing fills the middle two thirds of the square.
        Image(painterResource(R.drawable.ic_launcher_foreground), null, modifier = Modifier.size(50.dp))
        Spacer(Modifier.width(2.dp))
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(44.dp).clip(CircleShape).glass(CircleShape).clickable(role = Role.Button, onClick = onTune),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.tune), stringResource(R.string.my_pulse_tune), tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

/** Inside the heart: the play sign before the start, then the artwork of what plays. */
@Composable
private fun HeartFace(starting: Boolean, pulsePlaying: Boolean, isPlaying: Boolean, hasSong: Boolean, artwork: @Composable (Modifier) -> Unit) {
    when {
        starting -> CircularProgressIndicator(color = PulseInk, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
        pulsePlaying && hasSong -> Box(contentAlignment = Alignment.Center) {
            artwork(Modifier.fillMaxSize(0.84f).clip(CircleShape).border(3.dp, PulseInk.copy(alpha = 0.35f), CircleShape))
            if (!isPlaying) {
                Box(Modifier.fillMaxSize(0.84f).clip(CircleShape).background(PulseInk.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.pulse_play), null, tint = Color.White, modifier = Modifier.size(56.dp))
                }
            }
        }
        pulsePlaying -> Icon(painterResource(if (isPlaying) R.drawable.pulse_pause else R.drawable.pulse_play), null, tint = PulseInk, modifier = Modifier.size(60.dp))
        else -> Icon(painterResource(R.drawable.pulse_play), stringResource(R.string.my_pulse_listen), tint = PulseInk, modifier = Modifier.size(64.dp))
    }
}

@Composable
private fun Caption(now: NowPlaying?, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
        if (now == null) {
            Text(stringResource(R.string.my_pulse_listen), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.66f), fontSize = 15.sp)
        } else {
            Text(subtitle.uppercase(), color = PulseAmber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp)
            Spacer(Modifier.height(6.dp))
            Text(now.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(now.artist, color = Color.White.copy(alpha = 0.68f), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Pill(icon: Int?, label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.clip(shape).glass(shape, selected).clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (selected) PulseInk else Color.White
        if (icon != null) {
            Icon(painterResource(icon), null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TuneSheet(initial: PulseSettings, moods: List<PulseSettings.Mood>?, onApply: (PulseSettings) -> Unit) {
    var chosen by remember(initial) { mutableStateOf(initial) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 20.dp)) {
        Text(stringResource(R.string.my_pulse_tune), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Section(stringResource(R.string.my_pulse_character))
        PulseSettings.Character.entries.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { character ->
                    CharacterCard(character, chosen.character == character, Modifier.weight(1f).fillMaxHeight()) {
                        chosen = chosen.copy(character = character)
                    }
                }
            }
        }

        Section(stringResource(R.string.my_pulse_language))
        val track = RoundedCornerShape(50)
        Row(Modifier.fillMaxWidth().glass(track).padding(4.dp)) {
            PulseSettings.Language.entries.forEach { language ->
                val selected = chosen.language == language
                Box(
                    Modifier
                        .weight(1f)
                        .clip(track)
                        .then(if (selected) Modifier.background(PulseGradient) else Modifier)
                        .clickable(role = Role.RadioButton) { chosen = chosen.copy(language = language) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(languageName(language)), color = if (selected) PulseInk else Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Section(stringResource(R.string.my_pulse_mood))
        when {
            moods == null -> CircularProgressIndicator(color = PulseCoral, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            moods.isEmpty() -> Text(stringResource(R.string.my_pulse_moods_unavailable), fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
            else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(null, stringResource(R.string.my_pulse_mood_any), chosen.mood == null) { chosen = chosen.copy(mood = null) }
                moods.forEach { mood ->
                    Pill(null, mood.title, chosen.mood?.browseId == mood.browseId && chosen.mood?.params == mood.params) { chosen = chosen.copy(mood = mood) }
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        Box(
            Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(50)).background(PulseGradient).clickable(role = Role.Button) { onApply(chosen) },
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.my_pulse_apply), color = PulseInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CharacterCard(character: PulseSettings.Character, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(PulseCoral.copy(alpha = 0.14f), shape).border(1.5.dp, PulseGradient, shape)
                } else {
                    Modifier.glass(shape)
                },
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (selected) PulseGradient else SolidColor(Color.White.copy(alpha = 0.1f))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(characterIcon(character)), null, tint = if (selected) PulseInk else Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(characterName(character)), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(stringResource(characterHint(character)), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(),
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
    )
}

private fun characterIcon(c: PulseSettings.Character) = when (c) {
    PulseSettings.Character.MIX -> R.drawable.shuffle
    PulseSettings.Character.UNFAMILIAR -> R.drawable.explore_outlined
    PulseSettings.Character.FAVORITE -> R.drawable.favorite
    PulseSettings.Character.POPULAR -> R.drawable.trending_up
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
