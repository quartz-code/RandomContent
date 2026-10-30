package com.metrolist.music.ui.screens.pulse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.pulse.feed.PulseSettings
import kotlin.math.roundToInt

/** The three pages of "Tune": what plays, how the page looks, and when what plays. */
enum class TunePage { MUSIC, LOOK, SCHEDULE }

/**
 * Pulse: the "Tune" sheet, shared by the phone (in a bottom sheet) and the computer (in a
 * dialog). The music and the schedule apply together with one button, since they restart the
 * pulse; the look changes as you touch it, so it can be judged on the page behind.
 */
@Composable
fun TuneSheet(
    words: PulseWords,
    icons: PulseIcons,
    initial: PulseSettings,
    moods: List<PulseSettings.Mood>?,
    look: PulseLook,
    hour: Int,
    onLook: (PulseLook) -> Unit,
    onApply: (PulseSettings) -> Unit,
    onShareLook: (PulseLook) -> Unit,
    onPasteLook: () -> Unit,
    modifier: Modifier = Modifier,
    startPage: TunePage = TunePage.MUSIC,
) {
    var chosen by remember(initial) { mutableStateOf(initial) }
    var page by rememberSaveable { mutableStateOf(startPage) }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
        Text(words.tune, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Segmented(TunePage.entries, page, { words.pages.getValue(it) }) { page = it }

        when (page) {
            TunePage.MUSIC -> MusicPage(words, icons, chosen, moods) { chosen = it }
            TunePage.LOOK -> LookPage(words, icons, look, onLook, onShareLook, onPasteLook)
            TunePage.SCHEDULE -> SchedulePage(words, icons, chosen, moods, hour) { chosen = it }
        }

        if (page != TunePage.LOOK) {
            Spacer(Modifier.height(26.dp))
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(50)).background(LocalPulseTint.current.gradient).clickable(role = Role.Button) { onApply(chosen) },
                contentAlignment = Alignment.Center,
            ) {
                Text(words.apply, color = PulseInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.MusicPage(words: PulseWords, icons: PulseIcons, chosen: PulseSettings, moods: List<PulseSettings.Mood>?, onChange: (PulseSettings) -> Unit) {
    Section(words.character)
    PulseSettings.Character.entries.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            pair.forEach { character ->
                CharacterCard(icons.characters.getValue(character), words.characters.getValue(character), words.characterHints.getValue(character), chosen.character == character, Modifier.weight(1f).fillMaxHeight()) {
                    onChange(chosen.copy(character = character))
                }
            }
        }
    }

    Section(words.language)
    Segmented(PulseSettings.Language.entries, chosen.language, { words.languages.getValue(it) }) { onChange(chosen.copy(language = it)) }

    Section(words.mood)
    when {
        moods == null -> CircularProgressIndicator(color = LocalPulseTint.current.first, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        moods.isEmpty() -> Text(words.moodsUnavailable, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
        else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(null, words.moodAny, chosen.mood == null) { onChange(chosen.copy(mood = null)) }
            moods.forEach { mood ->
                Pill(null, mood.title, chosen.mood.sameAs(mood)) { onChange(chosen.copy(mood = mood)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.LookPage(words: PulseWords, icons: PulseIcons, look: PulseLook, onLook: (PulseLook) -> Unit, onShare: (PulseLook) -> Unit, onPaste: () -> Unit) {
    Section(words.colors)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        PulseLook.Palette.entries.forEach { palette ->
            val selected = look.palette == palette
            Column(
                Modifier.clip(RoundedCornerShape(14.dp)).clickable(role = Role.RadioButton) { onLook(look.copy(palette = palette)) }.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .then(if (selected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(
                            // "From the artwork" shows every colour, since it may become any of them.
                            if (palette == PulseLook.Palette.ARTWORK) {
                                Brush.sweepGradient(listOf(Color(0xFFFF4F7B), Color(0xFFFFC857), Color(0xFF47E08E), Color(0xFF35D0D6), Color(0xFF5B3FD9), Color(0xFFFF4F7B)))
                            } else {
                                Brush.linearGradient(listOf(Color(palette.first), Color(palette.second), Color(palette.fourth)))
                            },
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(words.palettes.getValue(palette), color = Color.White.copy(alpha = if (selected) 1f else 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Toggle(words.artworkLights, words.artworkLightsHint, look.artworkLights) { onLook(look.copy(artworkLights = it)) }

    Section(words.shape)
    PulseLook.Shape.entries.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { shape ->
                val selected = look.shape == shape
                val corners = RoundedCornerShape(18.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .clip(corners)
                        .then(if (selected) Modifier.background(Color.White.copy(alpha = 0.1f), corners).border(1.5.dp, LocalPulseTint.current.gradient, corners) else Modifier.glass(corners))
                        .clickable(role = Role.RadioButton) { onLook(look.copy(shape = shape)) }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HeartPreview(shape, Modifier.size(44.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(words.shapes.getValue(shape), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Toggle(words.echoes, null, look.echoes) { onLook(look.copy(echoes = it)) }

    Section(words.background)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PulseLook.Background.entries.forEach { background ->
            Pill(null, words.backgrounds.getValue(background), look.background == background) { onLook(look.copy(background = background)) }
        }
    }

    Section(words.reaction)
    val tint = LocalPulseTint.current
    Slider(
        value = look.reaction.coerceIn(0f, 2f),
        onValueChange = { value -> ((value * 10).roundToInt() / 10f).let { if (it != look.reaction) onLook(look.copy(reaction = it)) } },
        valueRange = 0f..2f,
        colors = SliderDefaults.colors(thumbColor = tint.second, activeTrackColor = tint.first, inactiveTrackColor = Color.White.copy(alpha = 0.12f)),
    )
    Row(Modifier.fillMaxWidth()) {
        Text(words.reactionLess, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(words.reactionMore, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
    }

    Section(words.motion)
    Segmented(PulseLook.Motion.entries, look.motion, { words.motions.getValue(it) }) { onLook(look.copy(motion = it)) }

    Spacer(Modifier.height(22.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton(icons.share, words.share, Modifier.weight(1f)) { onShare(look) }
        GlassButton(icons.paste, words.paste, Modifier.weight(1f), onPaste)
    }
}

@Composable
private fun ColumnScope.SchedulePage(words: PulseWords, icons: PulseIcons, chosen: PulseSettings, moods: List<PulseSettings.Mood>?, hour: Int, onChange: (PulseSettings) -> Unit) {
    val schedule = chosen.schedule
    Spacer(Modifier.height(18.dp))
    Toggle(words.scheduleSwitch, words.scheduleHint, schedule.enabled) {
        onChange(chosen.copy(schedule = schedule.copy(enabled = it)))
    }
    if (!schedule.enabled) return
    val now = PulseSettings.DayPart.at(hour)
    PulseSettings.DayPart.entries.forEach { part ->
        val slot = schedule.slots[part] ?: PulseSettings.Slot()
        fun change(next: PulseSettings.Slot) = onChange(chosen.copy(schedule = schedule.copy(slots = schedule.slots + (part to next))))
        val corners = RoundedCornerShape(20.dp)
        Column(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(corners)
                .then(if (part == now) Modifier.background(Color.White.copy(alpha = 0.06f), corners).border(1.5.dp, LocalPulseTint.current.gradient, corners) else Modifier.glass(corners))
                .padding(vertical = 14.dp),
        ) {
            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(words.dayParts.getValue(part), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                val next = PulseSettings.DayPart.entries[(part.ordinal + 1) % PulseSettings.DayPart.entries.size]
                Text(words.partHours(part.fromHour, next.fromHour), color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                if (part == now) {
                    Spacer(Modifier.weight(1f))
                    Text(words.now, color = LocalPulseTint.current.second, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(null, words.asUsual, slot.character == null) { change(slot.copy(character = null)) }
                PulseSettings.Character.entries.forEach { character ->
                    Pill(icons.characters[character], words.characters.getValue(character), slot.character == character) { change(slot.copy(character = character)) }
                }
            }
            if (!moods.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(null, words.moodAsUsual, slot.mood == null) { change(slot.copy(mood = null)) }
                    moods.forEach { mood ->
                        Pill(null, mood.title, slot.mood.sameAs(mood)) { change(slot.copy(mood = mood)) }
                    }
                }
            }
        }
    }
}

private fun PulseSettings.Mood?.sameAs(other: PulseSettings.Mood) = this?.browseId == other.browseId && this.params == other.params

@Composable
private fun CharacterCard(icon: Painter, name: String, hint: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val tint = LocalPulseTint.current
    Column(
        modifier
            .clip(shape)
            .then(if (selected) Modifier.background(tint.first.copy(alpha = 0.14f), shape).border(1.5.dp, tint.gradient, shape) else Modifier.glass(shape))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (selected) tint.gradient else SolidColor(Color.White.copy(alpha = 0.1f))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (selected) PulseInk else Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(hint, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 16.sp)
    }
}

/** A track of equal parts with the chosen one filled, for short either-or choices. */
@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val track = RoundedCornerShape(50)
    val tint = LocalPulseTint.current
    Row(Modifier.fillMaxWidth().glass(track).padding(4.dp)) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(track)
                    .then(if (on) Modifier.background(tint.gradient) else Modifier)
                    .clickable(role = Role.RadioButton) { onSelect(option) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), color = if (on) PulseInk else Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Toggle(title: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val tint = LocalPulseTint.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(role = Role.Switch) { onChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (hint != null) Text(hint, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PulseInk,
                checkedTrackColor = tint.first,
                checkedBorderColor = tint.first,
                uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.25f),
            ),
        )
    }
}

@Composable
private fun GlassButton(icon: Painter, label: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier.clip(shape).glass(shape).clickable(role = Role.Button, onClick = onClick).padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
