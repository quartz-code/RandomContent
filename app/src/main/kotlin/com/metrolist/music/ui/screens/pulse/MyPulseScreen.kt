package com.metrolist.music.ui.screens.pulse

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.PulseLookKey
import com.metrolist.music.playback.queues.PulseQueue
import com.metrolist.music.pulse.feed.MyPulse
import com.metrolist.music.pulse.feed.PulseSettings
import com.metrolist.music.ui.player.rememberArtworkColors
import com.metrolist.music.ui.player.rememberAudioLevels
import com.metrolist.music.ui.screens.pulse.PulseLook.Companion.toCode
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.safeDataStoreEdit
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class MyPulseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val pulse: MyPulse,
) : ViewModel() {
    val settings = pulse.settings.stateIn(viewModelScope, SharingStarted.Eagerly, PulseSettings())
    val look = context.dataStore.data.map { PulseLook.fromJson(it[PulseLookKey]) }.stateIn(viewModelScope, SharingStarted.Eagerly, PulseLook())
    var moods by mutableStateOf<List<PulseSettings.Mood>?>(null)
        private set

    fun loadMoods() {
        if (moods != null) return
        viewModelScope.launch { moods = runCatching { pulse.moods() }.getOrDefault(emptyList()) }
    }

    fun save(settings: PulseSettings) {
        viewModelScope.launch { pulse.saveSettings(settings) }
    }

    fun saveLook(look: PulseLook) {
        viewModelScope.launch { context.safeDataStoreEdit { it[PulseLookKey] = look.toJson() } }
    }
}

/**
 * Pulse: "My pulse" on the phone — the shared page ([MyPulseContent], [TuneSheet]) wired to the
 * player, the settings store and the app's own words and icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPulseScreen(viewModel: MyPulseViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val look by viewModel.look.collectAsState()
    val feed by viewModel.pulse.feed.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val metadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val song by playerConnection.currentSong.collectAsStateWithLifecycle(null)
    val cookie by rememberPreference(InnerTubeCookieKey, "")
    val signedIn = "SAPISID" in cookie
    val starting by viewModel.pulse.starting.collectAsState()
    var tuning by remember { mutableStateOf(false) }
    val words = pulseWords()
    val icons = pulseIcons()
    val pulsePlaying = feed != null
    // Everything the pulse's queue plays counts: its own songs, and a song added with "Play next".
    val current = metadata?.takeIf { pulsePlaying }
    val hour = remember(metadata?.id, settings) { LocalTime.now().hour }

    fun start() {
        if (starting) return
        scope.launch { playerConnection.playQueue(PulseQueue(viewModel.pulse, viewModel.pulse.newFeed(), words.title)) }
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

    val artworkColors = rememberArtworkColors(current?.thumbnailUrl)
    val beating = pulsePlaying && isPlaying
    val sessionId = remember(metadata?.id) { playerConnection.player.audioSessionId }
    val statusBar = WindowInsets.statusBars.asPaddingValues()
    MyPulseContent(
        words = words,
        icons = icons,
        settings = settings,
        hour = hour,
        look = look,
        pulsePlaying = pulsePlaying,
        isPlaying = isPlaying,
        starting = starting,
        now = current?.let { NowPlaying(it.title, it.artists.joinToString { a -> a.name }, song?.song?.liked == true) },
        signedIn = signedIn,
        artworkColors = artworkColors,
        time = rememberPulseClock(beating, look.motion.speed),
        levels = rememberAudioLevels(sessionId, beating),
        contentPadding = PaddingValues(top = statusBar.calculateTopPadding(), bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()),
        artwork = { modifier -> AsyncImage(current?.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = modifier) },
        backgroundArtwork = { AsyncImage(current?.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(48.dp)) },
        // Blur exists from Android 12; before it the sharp artwork needs more shade to stay a background.
        artworkShade = if (android.os.Build.VERSION.SDK_INT >= 31) 0.55f else 0.78f,
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
        onCharacter = { apply(settings.withCharacterNow(it, hour)) },
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
            CompositionLocalProvider(LocalPulseTint provides rememberPulseTint(look, artworkColors)) {
                TuneSheet(
                    words = words,
                    icons = icons,
                    initial = settings,
                    moods = viewModel.moods,
                    look = look,
                    hour = hour,
                    onLook = viewModel::saveLook,
                    onApply = { chosen ->
                        tuning = false
                        apply(chosen)
                    },
                    onShareLook = { shareLook(context, it) },
                    onPasteLook = {
                        val pasted = PulseLook.fromCode(readClipboard(context))
                        if (pasted != null) viewModel.saveLook(pasted)
                        Toast.makeText(context, if (pasted != null) R.string.my_pulse_look_applied else R.string.my_pulse_look_no_code, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        }
    }
}

@Composable
private fun pulseWords() = PulseWords(
    title = stringResource(R.string.my_pulse),
    listen = stringResource(R.string.my_pulse_listen),
    tagline = stringResource(R.string.my_pulse_tagline),
    tune = stringResource(R.string.my_pulse_tune),
    like = stringResource(R.string.my_pulse_like),
    dislike = stringResource(R.string.my_pulse_dislike),
    next = stringResource(R.string.my_pulse_next),
    signInHint = stringResource(R.string.my_pulse_sign_in_hint),
    characters = PulseSettings.Character.entries.associateWith {
        stringResource(
            when (it) {
                PulseSettings.Character.MIX -> R.string.my_pulse_mix
                PulseSettings.Character.UNFAMILIAR -> R.string.my_pulse_unfamiliar
                PulseSettings.Character.FAVORITE -> R.string.my_pulse_favorite
                PulseSettings.Character.POPULAR -> R.string.my_pulse_popular
            },
        )
    },
    characterHints = PulseSettings.Character.entries.associateWith {
        stringResource(
            when (it) {
                PulseSettings.Character.MIX -> R.string.my_pulse_mix_hint
                PulseSettings.Character.UNFAMILIAR -> R.string.my_pulse_unfamiliar_hint
                PulseSettings.Character.FAVORITE -> R.string.my_pulse_favorite_hint
                PulseSettings.Character.POPULAR -> R.string.my_pulse_popular_hint
            },
        )
    },
    languages = PulseSettings.Language.entries.associateWith {
        stringResource(
            when (it) {
                PulseSettings.Language.ANY -> R.string.my_pulse_language_any
                PulseSettings.Language.RUSSIAN -> R.string.my_pulse_language_russian
                PulseSettings.Language.FOREIGN -> R.string.my_pulse_language_foreign
            },
        )
    },
    dayParts = PulseSettings.DayPart.entries.associateWith {
        stringResource(
            when (it) {
                PulseSettings.DayPart.MORNING -> R.string.my_pulse_morning
                PulseSettings.DayPart.DAY -> R.string.my_pulse_day
                PulseSettings.DayPart.EVENING -> R.string.my_pulse_evening
                PulseSettings.DayPart.NIGHT -> R.string.my_pulse_night
            },
        )
    },
    character = stringResource(R.string.my_pulse_character),
    language = stringResource(R.string.my_pulse_language),
    mood = stringResource(R.string.my_pulse_mood),
    moodAny = stringResource(R.string.my_pulse_mood_any),
    moodsUnavailable = stringResource(R.string.my_pulse_moods_unavailable),
    apply = stringResource(R.string.my_pulse_apply),
    pages = TunePage.entries.associateWith {
        stringResource(
            when (it) {
                TunePage.MUSIC -> R.string.my_pulse_page_music
                TunePage.LOOK -> R.string.my_pulse_page_look
                TunePage.SCHEDULE -> R.string.my_pulse_page_schedule
            },
        )
    },
    colors = stringResource(R.string.my_pulse_colors),
    palettes = PulseLook.Palette.entries.associateWith {
        stringResource(
            when (it) {
                PulseLook.Palette.PULSE -> R.string.my_pulse_palette_pulse
                PulseLook.Palette.OCEAN -> R.string.my_pulse_palette_ocean
                PulseLook.Palette.NEON -> R.string.my_pulse_palette_neon
                PulseLook.Palette.FOREST -> R.string.my_pulse_palette_forest
                PulseLook.Palette.SUNSET -> R.string.my_pulse_palette_sunset
                PulseLook.Palette.ICE -> R.string.my_pulse_palette_ice
                PulseLook.Palette.ARTWORK -> R.string.my_pulse_palette_artwork
            },
        )
    },
    artworkLights = stringResource(R.string.my_pulse_artwork_lights),
    artworkLightsHint = stringResource(R.string.my_pulse_artwork_lights_hint),
    shape = stringResource(R.string.my_pulse_shape),
    shapes = PulseLook.Shape.entries.associateWith {
        stringResource(
            when (it) {
                PulseLook.Shape.DROP -> R.string.my_pulse_shape_drop
                PulseLook.Shape.CIRCLE -> R.string.my_pulse_shape_circle
                PulseLook.Shape.FLOWER -> R.string.my_pulse_shape_flower
                PulseLook.Shape.STAR -> R.string.my_pulse_shape_star
                PulseLook.Shape.HEART -> R.string.my_pulse_shape_heart
                PulseLook.Shape.WAVE -> R.string.my_pulse_shape_wave
            },
        )
    },
    echoes = stringResource(R.string.my_pulse_echoes),
    background = stringResource(R.string.my_pulse_background),
    backgrounds = PulseLook.Background.entries.associateWith {
        stringResource(
            when (it) {
                PulseLook.Background.LIGHTS -> R.string.my_pulse_background_lights
                PulseLook.Background.ARTWORK -> R.string.my_pulse_background_artwork
                PulseLook.Background.CALM -> R.string.my_pulse_background_calm
                PulseLook.Background.PARTICLES -> R.string.my_pulse_background_particles
                PulseLook.Background.AURORA -> R.string.my_pulse_background_aurora
                PulseLook.Background.WAVES -> R.string.my_pulse_background_waves
            },
        )
    },
    motion = stringResource(R.string.my_pulse_motion),
    motions = PulseLook.Motion.entries.associateWith {
        stringResource(
            when (it) {
                PulseLook.Motion.CALM -> R.string.my_pulse_motion_calm
                PulseLook.Motion.NORMAL -> R.string.my_pulse_motion_normal
                PulseLook.Motion.LIVELY -> R.string.my_pulse_motion_lively
            },
        )
    },
    share = stringResource(R.string.my_pulse_look_share),
    paste = stringResource(R.string.my_pulse_look_paste),
    reaction = stringResource(R.string.my_pulse_reaction),
    reactionLess = stringResource(R.string.my_pulse_reaction_less),
    reactionMore = stringResource(R.string.my_pulse_reaction_more),
    scheduleSwitch = stringResource(R.string.my_pulse_schedule_switch),
    scheduleHint = stringResource(R.string.my_pulse_schedule_hint),
    partHours = LocalContext.current.let { context -> { from, to -> context.getString(R.string.my_pulse_part_hours, from, to) } },
    now = stringResource(R.string.my_pulse_now),
    asUsual = stringResource(R.string.my_pulse_as_usual),
    moodAsUsual = stringResource(R.string.my_pulse_mood_as_usual),
)

@Composable
private fun pulseIcons() = PulseIcons(
    // The launcher logo itself; its drawing fills the middle two thirds of the square.
    logo = painterResource(R.drawable.ic_launcher_foreground),
    logoSize = 50.dp,
    play = painterResource(R.drawable.pulse_play),
    pause = painterResource(R.drawable.pulse_pause),
    like = painterResource(R.drawable.favorite_border),
    liked = painterResource(R.drawable.favorite),
    dislike = painterResource(R.drawable.thumb_down),
    next = painterResource(R.drawable.skip_next),
    tune = painterResource(R.drawable.tune),
    account = painterResource(R.drawable.account),
    characters = mapOf(
        PulseSettings.Character.MIX to painterResource(R.drawable.shuffle),
        PulseSettings.Character.UNFAMILIAR to painterResource(R.drawable.explore_outlined),
        PulseSettings.Character.FAVORITE to painterResource(R.drawable.favorite),
        PulseSettings.Character.POPULAR to painterResource(R.drawable.trending_up),
    ),
    share = painterResource(R.drawable.share),
    paste = painterResource(R.drawable.content_paste),
)

private fun readClipboard(context: Context): String? = runCatching {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
}.getOrNull()

private fun shareLook(context: Context, look: PulseLook) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.my_pulse_look_share_text, look.toCode()))
    }
    context.startActivity(Intent.createChooser(intent, null))
}
