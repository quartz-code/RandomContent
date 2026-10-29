package app.pulse.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.pulse.desktop.res.Res
import app.pulse.desktop.res.content_copy
import app.pulse.desktop.res.content_paste
import app.pulse.desktop.res.explore_outlined
import app.pulse.desktop.res.favorite
import app.pulse.desktop.res.favorite_border
import app.pulse.desktop.res.person
import app.pulse.desktop.res.pulse
import app.pulse.desktop.res.pulse_pause
import app.pulse.desktop.res.pulse_play
import app.pulse.desktop.res.pulse_skip_next
import app.pulse.desktop.res.shuffle
import app.pulse.desktop.res.thumb_down
import app.pulse.desktop.res.trending_up
import app.pulse.desktop.res.tune
import coil3.compose.AsyncImage
import com.metrolist.music.pulse.feed.PulseSettings
import com.metrolist.music.ui.screens.pulse.LocalPulseTint
import com.metrolist.music.ui.screens.pulse.MyPulseContent
import com.metrolist.music.ui.screens.pulse.NowPlaying
import com.metrolist.music.ui.screens.pulse.PulseIcons
import com.metrolist.music.ui.screens.pulse.PulseInk
import com.metrolist.music.ui.screens.pulse.PulseLook
import com.metrolist.music.ui.screens.pulse.PulseLook.Companion.toCode
import com.metrolist.music.ui.screens.pulse.PulseSheet
import com.metrolist.music.ui.screens.pulse.PulseTint
import com.metrolist.music.ui.screens.pulse.PulseWords
import com.metrolist.music.ui.screens.pulse.TunePage
import com.metrolist.music.ui.screens.pulse.TuneSheet
import com.metrolist.music.ui.screens.pulse.rememberPulseClock
import com.metrolist.music.ui.screens.pulse.withCharacterNow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import java.time.LocalTime

/**
 * Pulse desktop: the "My pulse" tab — the phone's very page and "Tune" sheet (shared code), fed
 * by [DesktopPulse] and the computer's player, with the words in [Strings].
 */
@Composable
fun PulseTab(pulse: DesktopPulse, controller: PlayerController, library: LibraryModel, signedIn: Boolean) {
    val state by controller.state.collectAsState()
    val settings by pulse.settings.collectAsState()
    val look by pulse.look.collectAsState()
    val feed by pulse.feed.collectAsState()
    val starting by pulse.starting.collectAsState()
    val empty by pulse.empty.collectAsState()
    val likedIds by library.likedIds.collectAsState()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var tuning by remember { mutableStateOf(false) }
    var moods by remember { mutableStateOf<List<PulseSettings.Mood>?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val words = remember { pulseWords() }
    val icons = pulseIcons()
    val pulsePlaying = feed != null
    val current = state.current?.takeIf { pulsePlaying }
    val hour = remember(current?.videoId, settings) { LocalTime.now().hour }
    val beating = pulsePlaying && state.isPlaying
    val artworkColor = rememberArtworkColor(current?.thumbnail)

    fun apply(chosen: PulseSettings) {
        if (chosen == settings) return
        pulse.saveSettings(chosen)
        // Already playing: start over with the new settings, so the change is heard now.
        if (pulsePlaying) pulse.start()
    }

    LaunchedEffect(empty) { if (empty) notice = Strings.pulseNothing }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(3_000)
            notice = null
        }
    }

    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))) {
        MyPulseContent(
            words = words,
            icons = icons,
            settings = settings,
            hour = hour,
            look = look,
            pulsePlaying = pulsePlaying,
            isPlaying = state.isPlaying,
            starting = starting,
            now = current?.let { NowPlaying(it.title, it.artists, it.videoId in likedIds) },
            signedIn = signedIn,
            // The desktop finds one colour in the artwork; lighter and darker shades of it make the lights.
            artworkColors = artworkColor?.let { listOf(it, lerp(it, Color.White, 0.3f), lerp(it, Color.Black, 0.25f)) }.orEmpty(),
            time = rememberPulseClock(beating, look.motion.speed),
            levels = rememberPulseLevels(controller.levels, beating),
            contentPadding = PaddingValues(top = 6.dp, bottom = 8.dp),
            artwork = { modifier -> AsyncImage(current?.artwork, null, contentScale = ContentScale.Crop, modifier = modifier) },
            backgroundArtwork = { AsyncImage(current?.artwork, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(48.dp)) },
            onListen = { if (pulsePlaying) controller.togglePlay() else pulse.start() },
            onLike = { current?.let(pulse::like) },
            onDislike = {
                current?.let {
                    pulse.dislike(it)
                    notice = Strings.pulseDisliked
                }
            },
            onNext = controller::next,
            onCharacter = { apply(settings.withCharacterNow(it, hour)) },
            onTune = {
                tuning = true
                if (moods.isNullOrEmpty()) scope.launch { moods = pulse.moods() }
            },
        )
        notice?.let { text ->
            Box(Modifier.align(Alignment.TopCenter).padding(top = 64.dp).clip(RoundedCornerShape(50)).background(PulseInk.copy(alpha = 0.9f)).padding(horizontal = 18.dp, vertical = 10.dp)) {
                Text(text, color = Color.White, fontSize = 14.sp)
            }
        }
    }

    if (tuning) {
        Dialog(onDismissRequest = { tuning = false }) {
            Surface(color = PulseSheet, contentColor = Color.White, shape = RoundedCornerShape(24.dp), modifier = Modifier.widthIn(max = 560.dp).heightIn(max = 720.dp)) {
                CompositionLocalProvider(LocalPulseTint provides PulseTint.of(look.palette)) {
                    TuneSheet(
                        words = words,
                        icons = icons,
                        initial = settings,
                        moods = moods,
                        look = look,
                        hour = hour,
                        onLook = pulse::saveLook,
                        onApply = { chosen ->
                            tuning = false
                            apply(chosen)
                        },
                        onShareLook = {
                            clipboard.setText(AnnotatedString(Strings.pulseShareText(it.toCode())))
                            notice = Strings.pulseCopied
                        },
                        onPasteLook = {
                            val pasted = PulseLook.fromCode(clipboard.getText()?.text)
                            if (pasted != null) pulse.saveLook(pasted)
                            notice = if (pasted != null) Strings.pulseApplied else Strings.pulseNoCode
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
                    )
                }
            }
        }
    }
}

/** The player's loudness bands, smoothed per frame: rising fast, falling slowly, as on the phone. */
@Composable
internal fun rememberPulseLevels(levels: AudioLevels, playing: Boolean): () -> FloatArray {
    val shown = remember { FloatArray(AudioLevels.BANDS) }
    val fresh = remember { FloatArray(AudioLevels.BANDS) }
    LaunchedEffect(levels, playing) {
        while (true) {
            withFrameNanos {
                levels.read(fresh)
                for (i in shown.indices) {
                    val goal = if (playing) fresh[i] else 0f
                    shown[i] += (goal - shown[i]) * if (goal > shown[i]) 0.45f else 0.12f
                }
            }
        }
    }
    return remember { { shown } }
}

internal fun pulseWords() = PulseWords(
    title = Strings.pulseTab,
    listen = Strings.pulseListen,
    tagline = Strings.pulseTagline,
    tune = Strings.pulseTune,
    like = Strings.pulseLike,
    dislike = Strings.pulseDislike,
    next = Strings.pulseNext,
    signInHint = Strings.pulseSignInHint,
    characters = mapOf(
        PulseSettings.Character.MIX to Strings.pulseMix,
        PulseSettings.Character.UNFAMILIAR to Strings.pulseUnfamiliar,
        PulseSettings.Character.FAVORITE to Strings.pulseFavorite,
        PulseSettings.Character.POPULAR to Strings.pulsePopular,
    ),
    characterHints = mapOf(
        PulseSettings.Character.MIX to Strings.pulseMixHint,
        PulseSettings.Character.UNFAMILIAR to Strings.pulseUnfamiliarHint,
        PulseSettings.Character.FAVORITE to Strings.pulseFavoriteHint,
        PulseSettings.Character.POPULAR to Strings.pulsePopularHint,
    ),
    languages = mapOf(
        PulseSettings.Language.ANY to Strings.pulseLanguageAny,
        PulseSettings.Language.RUSSIAN to Strings.pulseLanguageRussian,
        PulseSettings.Language.FOREIGN to Strings.pulseLanguageForeign,
    ),
    dayParts = mapOf(
        PulseSettings.DayPart.MORNING to Strings.pulseMorning,
        PulseSettings.DayPart.DAY to Strings.pulseDay,
        PulseSettings.DayPart.EVENING to Strings.pulseEvening,
        PulseSettings.DayPart.NIGHT to Strings.pulseNight,
    ),
    character = Strings.pulseCharacter,
    language = Strings.pulseLanguage,
    mood = Strings.pulseMood,
    moodAny = Strings.pulseMoodAny,
    moodsUnavailable = Strings.pulseMoodsUnavailable,
    apply = Strings.pulseApply,
    pages = mapOf(TunePage.MUSIC to Strings.pulsePageMusic, TunePage.LOOK to Strings.pulsePageLook, TunePage.SCHEDULE to Strings.pulsePageSchedule),
    colors = Strings.pulseColors,
    palettes = mapOf(
        PulseLook.Palette.PULSE to Strings.pulsePalettePulse,
        PulseLook.Palette.OCEAN to Strings.pulsePaletteOcean,
        PulseLook.Palette.NEON to Strings.pulsePaletteNeon,
        PulseLook.Palette.FOREST to Strings.pulsePaletteForest,
        PulseLook.Palette.SUNSET to Strings.pulsePaletteSunset,
        PulseLook.Palette.ICE to Strings.pulsePaletteIce,
    ),
    artworkLights = Strings.pulseArtworkLights,
    artworkLightsHint = Strings.pulseArtworkLightsHint,
    shape = Strings.pulseShape,
    shapes = mapOf(
        PulseLook.Shape.DROP to Strings.pulseShapeDrop,
        PulseLook.Shape.CIRCLE to Strings.pulseShapeCircle,
        PulseLook.Shape.FLOWER to Strings.pulseShapeFlower,
        PulseLook.Shape.STAR to Strings.pulseShapeStar,
    ),
    echoes = Strings.pulseEchoes,
    background = Strings.pulseBackground,
    backgrounds = mapOf(
        PulseLook.Background.LIGHTS to Strings.pulseBackgroundLights,
        PulseLook.Background.ARTWORK to Strings.pulseBackgroundArtwork,
        PulseLook.Background.CALM to Strings.pulseBackgroundCalm,
    ),
    motion = Strings.pulseMotion,
    motions = mapOf(
        PulseLook.Motion.CALM to Strings.pulseMotionCalm,
        PulseLook.Motion.NORMAL to Strings.pulseMotionNormal,
        PulseLook.Motion.LIVELY to Strings.pulseMotionLively,
    ),
    share = Strings.pulseShare,
    paste = Strings.pulsePaste,
    scheduleSwitch = Strings.pulseScheduleSwitch,
    scheduleHint = Strings.pulseScheduleHint,
    partHours = Strings::pulsePartHours,
    now = Strings.pulseNow,
    asUsual = Strings.pulseAsUsual,
    moodAsUsual = Strings.pulseMoodAsUsual,
)

@Composable
internal fun pulseIcons() = PulseIcons(
    logo = painterResource(Res.drawable.pulse),
    logoSize = 34.dp,
    play = painterResource(Res.drawable.pulse_play),
    pause = painterResource(Res.drawable.pulse_pause),
    like = painterResource(Res.drawable.favorite_border),
    liked = painterResource(Res.drawable.favorite),
    dislike = painterResource(Res.drawable.thumb_down),
    next = painterResource(Res.drawable.pulse_skip_next),
    tune = painterResource(Res.drawable.tune),
    account = painterResource(Res.drawable.person),
    characters = mapOf(
        PulseSettings.Character.MIX to painterResource(Res.drawable.shuffle),
        PulseSettings.Character.UNFAMILIAR to painterResource(Res.drawable.explore_outlined),
        PulseSettings.Character.FAVORITE to painterResource(Res.drawable.favorite),
        PulseSettings.Character.POPULAR to painterResource(Res.drawable.trending_up),
    ),
    share = painterResource(Res.drawable.content_copy),
    paste = painterResource(Res.drawable.content_paste),
)
