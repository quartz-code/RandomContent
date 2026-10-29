package com.metrolist.music.ui.screens.pulse

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.pulse.feed.PulseSettings
import com.metrolist.music.ui.screens.pulse.PulseLook.Companion.toCode

/** The three pages of "Tune": what plays, how the page looks, and when what plays. */
enum class TunePage { MUSIC, LOOK, SCHEDULE }

/**
 * Pulse: the "Tune" sheet. The music and the schedule apply together with one button, since they
 * restart the pulse; the look changes as you touch it, so it can be judged on the page behind.
 */
@Composable
internal fun TuneSheet(
    initial: PulseSettings,
    moods: List<PulseSettings.Mood>?,
    look: PulseLook,
    hour: Int,
    onLook: (PulseLook) -> Unit,
    onApply: (PulseSettings) -> Unit,
    startPage: TunePage = TunePage.MUSIC,
) {
    var chosen by remember(initial) { mutableStateOf(initial) }
    var page by rememberSaveable { mutableStateOf(startPage) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 20.dp)) {
        Text(stringResource(R.string.my_pulse_tune), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Segmented(TunePage.entries, page, { stringResource(pageName(it)) }) { page = it }

        when (page) {
            TunePage.MUSIC -> MusicPage(chosen, moods) { chosen = it }
            TunePage.LOOK -> LookPage(look, onLook)
            TunePage.SCHEDULE -> SchedulePage(chosen, moods, hour) { chosen = it }
        }

        if (page != TunePage.LOOK) {
            Spacer(Modifier.height(26.dp))
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(50)).background(LocalPulseTint.current.gradient).clickable(role = Role.Button) { onApply(chosen) },
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.my_pulse_apply), color = PulseInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MusicPage(chosen: PulseSettings, moods: List<PulseSettings.Mood>?, onChange: (PulseSettings) -> Unit) {
    Section(stringResource(R.string.my_pulse_character))
    PulseSettings.Character.entries.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            pair.forEach { character ->
                CharacterCard(character, chosen.character == character, Modifier.weight(1f).fillMaxHeight()) {
                    onChange(chosen.copy(character = character))
                }
            }
        }
    }

    Section(stringResource(R.string.my_pulse_language))
    Segmented(PulseSettings.Language.entries, chosen.language, { stringResource(languageName(it)) }) { onChange(chosen.copy(language = it)) }

    Section(stringResource(R.string.my_pulse_mood))
    when {
        moods == null -> CircularProgressIndicator(color = LocalPulseTint.current.first, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        moods.isEmpty() -> Text(stringResource(R.string.my_pulse_moods_unavailable), fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
        else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(null, stringResource(R.string.my_pulse_mood_any), chosen.mood == null) { onChange(chosen.copy(mood = null)) }
            moods.forEach { mood ->
                Pill(null, mood.title, chosen.mood.sameAs(mood)) { onChange(chosen.copy(mood = mood)) }
            }
        }
    }
}

@Composable
private fun LookPage(look: PulseLook, onLook: (PulseLook) -> Unit) {
    val context = LocalContext.current

    Section(stringResource(R.string.my_pulse_colors))
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
                        .background(Brush.linearGradient(listOf(Color(palette.first), Color(palette.second), Color(palette.fourth)))),
                )
                Spacer(Modifier.height(6.dp))
                Text(stringResource(paletteName(palette)), color = Color.White.copy(alpha = if (selected) 1f else 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Toggle(stringResource(R.string.my_pulse_artwork_lights), stringResource(R.string.my_pulse_artwork_lights_hint), look.artworkLights) { onLook(look.copy(artworkLights = it)) }

    Section(stringResource(R.string.my_pulse_shape))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PulseLook.Shape.entries.forEach { shape ->
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
                Text(stringResource(shapeName(shape)), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Toggle(stringResource(R.string.my_pulse_echoes), null, look.echoes) { onLook(look.copy(echoes = it)) }

    Section(stringResource(R.string.my_pulse_background))
    Segmented(PulseLook.Background.entries, look.background, { stringResource(backgroundName(it)) }) { onLook(look.copy(background = it)) }

    Section(stringResource(R.string.my_pulse_motion))
    Segmented(PulseLook.Motion.entries, look.motion, { stringResource(motionName(it)) }) { onLook(look.copy(motion = it)) }

    Spacer(Modifier.height(22.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton(R.drawable.share, stringResource(R.string.my_pulse_look_share), Modifier.weight(1f)) { shareLook(context, look) }
        GlassButton(R.drawable.content_paste, stringResource(R.string.my_pulse_look_paste), Modifier.weight(1f)) {
            val pasted = PulseLook.fromCode(readClipboard(context))
            if (pasted != null) onLook(pasted)
            Toast.makeText(context, if (pasted != null) R.string.my_pulse_look_applied else R.string.my_pulse_look_no_code, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun SchedulePage(chosen: PulseSettings, moods: List<PulseSettings.Mood>?, hour: Int, onChange: (PulseSettings) -> Unit) {
    val schedule = chosen.schedule
    Spacer(Modifier.height(18.dp))
    Toggle(stringResource(R.string.my_pulse_schedule_switch), stringResource(R.string.my_pulse_schedule_hint), schedule.enabled) {
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
                Text(stringResource(dayPartName(part)), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                val next = PulseSettings.DayPart.entries[(part.ordinal + 1) % PulseSettings.DayPart.entries.size]
                Text(stringResource(R.string.my_pulse_part_hours, part.fromHour, next.fromHour), color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                if (part == now) {
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.my_pulse_now), color = LocalPulseTint.current.second, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(null, stringResource(R.string.my_pulse_as_usual), slot.character == null) { change(slot.copy(character = null)) }
                PulseSettings.Character.entries.forEach { character ->
                    Pill(characterIcon(character), stringResource(characterName(character)), slot.character == character) { change(slot.copy(character = character)) }
                }
            }
            if (!moods.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(null, stringResource(R.string.my_pulse_mood_as_usual), slot.mood == null) { change(slot.copy(mood = null)) }
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
private fun CharacterCard(character: PulseSettings.Character, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
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
            Icon(painterResource(characterIcon(character)), null, tint = if (selected) PulseInk else Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(characterName(character)), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(stringResource(characterHint(character)), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 16.sp)
    }
}

/** A track of equal parts with the chosen one filled, for short either-or choices. */
@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
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
private fun GlassButton(icon: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier.clip(shape).glass(shape).clickable(role = Role.Button, onClick = onClick).padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, tint = Color.White, modifier = Modifier.size(18.dp))
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

private fun pageName(p: TunePage) = when (p) {
    TunePage.MUSIC -> R.string.my_pulse_page_music
    TunePage.LOOK -> R.string.my_pulse_page_look
    TunePage.SCHEDULE -> R.string.my_pulse_page_schedule
}

private fun paletteName(p: PulseLook.Palette) = when (p) {
    PulseLook.Palette.PULSE -> R.string.my_pulse_palette_pulse
    PulseLook.Palette.OCEAN -> R.string.my_pulse_palette_ocean
    PulseLook.Palette.NEON -> R.string.my_pulse_palette_neon
    PulseLook.Palette.FOREST -> R.string.my_pulse_palette_forest
    PulseLook.Palette.SUNSET -> R.string.my_pulse_palette_sunset
    PulseLook.Palette.ICE -> R.string.my_pulse_palette_ice
}

private fun shapeName(s: PulseLook.Shape) = when (s) {
    PulseLook.Shape.DROP -> R.string.my_pulse_shape_drop
    PulseLook.Shape.CIRCLE -> R.string.my_pulse_shape_circle
    PulseLook.Shape.FLOWER -> R.string.my_pulse_shape_flower
    PulseLook.Shape.STAR -> R.string.my_pulse_shape_star
}

private fun backgroundName(b: PulseLook.Background) = when (b) {
    PulseLook.Background.LIGHTS -> R.string.my_pulse_background_lights
    PulseLook.Background.ARTWORK -> R.string.my_pulse_background_artwork
    PulseLook.Background.CALM -> R.string.my_pulse_background_calm
}

private fun motionName(m: PulseLook.Motion) = when (m) {
    PulseLook.Motion.CALM -> R.string.my_pulse_motion_calm
    PulseLook.Motion.NORMAL -> R.string.my_pulse_motion_normal
    PulseLook.Motion.LIVELY -> R.string.my_pulse_motion_lively
}
