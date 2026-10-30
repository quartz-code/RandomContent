package com.metrolist.music.ui.screens.pulse

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.metrolist.music.pulse.feed.PulseSettings

/*
 * Pulse: the "My pulse" page itself, the same on the phone and on the computer. What differs —
 * where the words and icons come from, the player behind it — comes in as [PulseWords],
 * [PulseIcons] and callbacks.
 */

/** Everything "My pulse" says, in the listener's language: the phone reads it from resources, the computer from its strings. */
class PulseWords(
    val title: String,
    val listen: String,
    val tagline: String,
    val tune: String,
    val like: String,
    val dislike: String,
    val next: String,
    val signInHint: String,
    val characters: Map<PulseSettings.Character, String>,
    val characterHints: Map<PulseSettings.Character, String>,
    val languages: Map<PulseSettings.Language, String>,
    val dayParts: Map<PulseSettings.DayPart, String>,
    val character: String,
    val language: String,
    val mood: String,
    val moodAny: String,
    val moodsUnavailable: String,
    val apply: String,
    val pages: Map<TunePage, String>,
    val colors: String,
    val palettes: Map<PulseLook.Palette, String>,
    val artworkLights: String,
    val artworkLightsHint: String,
    val shape: String,
    val shapes: Map<PulseLook.Shape, String>,
    val echoes: String,
    val background: String,
    val backgrounds: Map<PulseLook.Background, String>,
    val motion: String,
    val motions: Map<PulseLook.Motion, String>,
    val share: String,
    val paste: String,
    /** The slider for how strongly the page answers the music, with its two ends. */
    val reaction: String,
    val reactionLess: String,
    val reactionMore: String,
    val scheduleSwitch: String,
    val scheduleHint: String,
    val partHours: (from: Int, to: Int) -> String,
    val now: String,
    val asUsual: String,
    val moodAsUsual: String,
)

/** The icons the page draws, from each platform's own resources. */
class PulseIcons(
    val logo: Painter,
    /** The logo's size in the header; the phone's drawing fills two thirds of its square. */
    val logoSize: Dp,
    val play: Painter,
    val pause: Painter,
    val like: Painter,
    val liked: Painter,
    val dislike: Painter,
    val next: Painter,
    val tune: Painter,
    val account: Painter,
    val characters: Map<PulseSettings.Character, Painter>,
    val share: Painter,
    val paste: Painter,
)

/** What the pulse plays now, for the words under the heart. */
data class NowPlaying(val title: String, val artist: String, val liked: Boolean)

/**
 * The character a tap on the page means "now": with the schedule changing the character of this
 * part of the day, that part's choice changes, otherwise the main one.
 */
fun PulseSettings.withCharacterNow(character: PulseSettings.Character, hour: Int): PulseSettings {
    val part = PulseSettings.DayPart.at(hour)
    val slot = schedule.slots[part]
    return if (schedule.enabled && slot?.character != null) {
        copy(schedule = schedule.copy(slots = schedule.slots + (part to slot.copy(character = character))))
    } else {
        copy(character = character)
    }
}

/** The page without the player and the settings store, so it can be drawn on its own. */
@Composable
fun MyPulseContent(
    words: PulseWords,
    icons: PulseIcons,
    settings: PulseSettings,
    hour: Int,
    look: PulseLook,
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
    /** The artwork as the background, blurred where the platform can; null keeps the lights. */
    backgroundArtwork: (@Composable () -> Unit)? = null,
    artworkShade: Float = 0.55f,
) {
    val energy = remember(levels) {
        {
            val bands = levels()
            var sum = 0f
            for (i in 0 until minOf(8, bands.size)) sum += bands[i]
            sum / 8f
        }
    }
    val playingNow = settings.at(hour)
    CompositionLocalProvider(LocalPulseTint provides rememberPulseTint(look, artworkColors)) {
        Box(Modifier.fillMaxSize().background(PulseInk)) {
            PulseAura(look, artworkColors, time, energy, Modifier.fillMaxSize(), artwork = backgroundArtwork.takeIf { now != null }, artworkShade = artworkShade)
            Column(Modifier.fillMaxSize().padding(contentPadding), horizontalAlignment = Alignment.CenterHorizontally) {
                Header(words.title, words.tune, icons) { onTune() }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val heart = min(maxWidth * 0.84f, maxHeight * 0.64f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PulseHeart(look.shape, look.echoes, look.reaction, time, levels, onListen, Modifier.size(heart)) {
                            HeartFace(starting, pulsePlaying, isPlaying, now != null, words.listen, icons, artwork, if (look.shape == PulseLook.Shape.HEART) HeartOutline else CircleShape)
                        }
                        Spacer(Modifier.height(4.dp))
                        AnimatedContent(now, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "now") { playing ->
                            Caption(playing, words.listen, describe(words, playingNow, hour))
                        }
                    }
                }
                if (now != null) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 36.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        PulseAction(icons.dislike, words.dislike, onDislike)
                        PulseAction(if (now.liked) icons.liked else icons.like, words.like, onLike, highlighted = now.liked)
                        PulseAction(icons.next, words.next, onNext)
                    }
                } else {
                    Text(
                        words.tagline,
                        color = Color.White.copy(alpha = 0.62f),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 40.dp),
                    )
                }
                Spacer(Modifier.height(22.dp))
                // Centred on a wide window, scrolling on a narrow phone.
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PulseSettings.Character.entries.forEach { character ->
                            Pill(icons.characters[character], words.characters.getValue(character), playingNow.character == character) { onCharacter(character) }
                        }
                    }
                }
                if (!signedIn) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.padding(horizontal = 20.dp).fillMaxWidth().glass(RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icons.account, null, tint = LocalPulseTint.current.second, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(words.signInHint, color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun Header(title: String, tune: String, icons: PulseIcons, onTune: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(icons.logo, null, modifier = Modifier.size(icons.logoSize))
        Spacer(Modifier.width(if (icons.logoSize > 40.dp) 2.dp else 10.dp))
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(44.dp).clip(CircleShape).glass(CircleShape).clickable(role = Role.Button, onClick = onTune),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icons.tune, tune, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

/** Inside the heart: the play sign before the start, then the artwork of what plays. */
@Composable
private fun HeartFace(starting: Boolean, pulsePlaying: Boolean, isPlaying: Boolean, hasSong: Boolean, listen: String, icons: PulseIcons, artwork: @Composable (Modifier) -> Unit, frame: Shape) {
    when {
        starting -> CircularProgressIndicator(color = PulseInk, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
        pulsePlaying && hasSong -> Box(contentAlignment = Alignment.Center) {
            artwork(Modifier.fillMaxSize(0.84f).clip(frame).border(3.dp, PulseInk.copy(alpha = 0.35f), frame))
            if (!isPlaying) {
                Box(Modifier.fillMaxSize(0.84f).clip(frame).background(PulseInk.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                    Icon(icons.play, null, tint = Color.White, modifier = Modifier.size(56.dp))
                }
            }
        }
        pulsePlaying -> Icon(if (isPlaying) icons.pause else icons.play, null, tint = PulseInk, modifier = Modifier.size(60.dp))
        else -> Icon(icons.play, listen, tint = PulseInk, modifier = Modifier.size(64.dp))
    }
}

@Composable
private fun Caption(now: NowPlaying?, listen: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
        if (now == null) {
            Text(listen, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.66f), fontSize = 15.sp, textAlign = TextAlign.Center)
        } else {
            Text(subtitle.uppercase(), color = LocalPulseTint.current.second, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(now.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(now.artist, color = Color.White.copy(alpha = 0.68f), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun Pill(icon: Painter?, label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val tint = LocalPulseTint.current
    Row(
        Modifier.clip(shape).glass(shape, if (selected) tint.gradient else null).clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (selected) PulseInk else Color.White
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** "Evening · Popular · Russian": with the schedule on, the part of the day comes first. */
fun describe(words: PulseWords, settings: PulseSettings, hour: Int): String = listOfNotNull(
    settings.schedule.takeIf { it.enabled }?.let { words.dayParts[PulseSettings.DayPart.at(hour)] },
    words.characters[settings.character],
    settings.language.takeIf { it != PulseSettings.Language.ANY }?.let { words.languages[it] },
    settings.mood?.title,
).joinToString(" · ")
