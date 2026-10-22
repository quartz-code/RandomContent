package com.metrolist.music.ui.screens.settings

import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.metrolist.music.constants.LockScreenPlayerKey
import com.metrolist.music.pulse.LockScreenLauncher
import android.app.TimePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.metrolist.music.constants.DefaultNightFrom
import com.metrolist.music.constants.DefaultNightTo
import com.metrolist.music.constants.PlayerNightFromKey
import com.metrolist.music.constants.PlayerNightModeKey
import com.metrolist.music.constants.PlayerNightToKey
import com.metrolist.music.ui.player.NightMode
import com.metrolist.music.ui.player.PlayerThemeGallery
import com.metrolist.music.ui.player.SavedTheme
import com.metrolist.music.ui.player.editThemeGallery
import com.metrolist.music.ui.player.rememberDayPlayerTheme
import com.metrolist.music.ui.player.rememberNightPlayerTheme
import com.metrolist.music.ui.player.rememberThemeGallery
import com.metrolist.music.ui.player.saveNightPlayerTheme
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.metrolist.music.ui.player.PulseMotionLayer
import com.metrolist.music.ui.player.canVisualize
import com.metrolist.music.ui.player.lyricsPosition
import com.metrolist.music.ui.player.pulseArtBorder
import com.metrolist.music.ui.player.pulseArtShape
import com.metrolist.music.ui.player.pulseLyricsColor
import com.metrolist.music.ui.player.pulseLyricsFont
import com.metrolist.music.ui.player.rememberAudioLevels
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.ui.player.LocalPulseArtworkColor
import com.metrolist.music.ui.player.MiniTheme
import com.metrolist.music.ui.player.PlayerTheme
import com.metrolist.music.ui.player.PlayerTheme.Companion.toCode
import com.metrolist.music.ui.player.PulseBottomActions
import com.metrolist.music.ui.player.PulseBottomRow
import com.metrolist.music.ui.player.PulseControlsColumn
import com.metrolist.music.ui.player.PulsePlayerBackground
import com.metrolist.music.ui.player.PulsePlayerHeader
import com.metrolist.music.ui.player.PulseProgress
import com.metrolist.music.ui.player.PulseTrackInfo
import com.metrolist.music.ui.player.PulseTransport
import com.metrolist.music.ui.player.artCornerDp
import com.metrolist.music.ui.player.artPadding
import com.metrolist.music.ui.player.baseBackground
import com.metrolist.music.ui.player.contentColor
import com.metrolist.music.ui.player.peekHeight
import com.metrolist.music.ui.player.pulsePalette
import com.metrolist.music.ui.player.savePlayerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Colours offered in the builder: the Shortwave palette plus a few calm and bright ones. */
private val Swatches = listOf(
    0xFFFFFFFF, 0xFFEFE8D6, 0xFFF0A43A, 0xFFE0482F, 0xFFF06292, 0xFF9575CD, 0xFF5C8DF6,
    0xFF4A5D7A, 0xFF26A69A, 0xFF6BF59A, 0xFF22261D, 0xFF3E4A5E, 0xFF1D1B20, 0xFF101215, 0xFFF7F5F2,
)

private val SampleArtwork = Color(0xFF7C93B8)

/**
 * Pulse: the player builder. Start from a preset, change any detail with a live preview on top, and
 * pass the result to a friend as a code; Pulse picks a copied code up from the clipboard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerBuilderScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val day = rememberDayPlayerTheme()
    val night = rememberNightPlayerTheme()
    val gallery = rememberThemeGallery()
    var nightMode by rememberEnumPreference(PlayerNightModeKey, NightMode.OFF)
    var nightFrom by rememberPreference(PlayerNightFromKey, DefaultNightFrom)
    var nightTo by rememberPreference(PlayerNightToKey, DefaultNightTo)
    var editNight by remember { mutableStateOf(false) }
    val editingNight = editNight && night != null
    // The edit is held here as well as saved, so sliders do not wait for the round trip to storage.
    var edited by remember { mutableStateOf<Pair<Boolean, PlayerTheme>?>(null) }
    val current = edited?.takeIf { it.first == editingNight }?.second ?: (if (editingNight) night!! else day)
    fun update(next: PlayerTheme) {
        edited = editingNight to next
        val toNight = editingNight
        scope.launch { if (toNight) context.saveNightPlayerTheme(next) else context.savePlayerTheme(next) }
    }

    /** A theme from a friend's code: applied, and kept in "My themes" to come back to. */
    fun receive(received: PlayerTheme) {
        update(received)
        val name = PlayerThemeGallery.freeName(gallery, context.getString(R.string.player_builder_gallery_friend_name))
        scope.launch { context.editThemeGallery { PlayerThemeGallery.add(it, SavedTheme(name = name, theme = received, fromFriend = true)) } }
    }
    var naming by remember { mutableStateOf<SavedTheme?>(null) }

    var lockScreen by rememberPreference(LockScreenPlayerKey, false)
    // The permission is granted in system settings, so it is checked again on coming back.
    var canOpenOverLock by remember { mutableStateOf(LockScreenLauncher.canOpen(context)) }
    LifecycleResumeEffect(Unit) {
        canOpenOverLock = LockScreenLauncher.canOpen(context)
        onPauseOrDispose { }
    }
    fun askOverlay() {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
        }
    }

    // Bars and wave need the microphone permission to hear the player; asked when picked.
    var canHear by remember { mutableStateOf(context.canVisualize()) }
    val askToHear = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { canHear = it }
    fun pickMotion(motion: PlayerTheme.Motion) {
        update(current.copy(motion = motion))
        if ((motion == PlayerTheme.Motion.BARS || motion == PlayerTheme.Motion.WAVE) && !canHear) {
            askToHear.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var found by remember { mutableStateOf<PlayerTheme?>(null) }
    var showPaste by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // The clipboard is readable only once the window has focus.
        delay(400)
        val code = PlayerTheme.fromCode(readClipboard(context))
        if (code != null && code != current) found = code
    }

    PulseScreenScaffold(navController, stringResource(R.string.player_builder)) {
        if (night != null) {
            Choice(
                listOf(false to R.string.player_builder_slot_main, true to R.string.player_builder_slot_night),
                editingNight,
            ) { editNight = it }
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            PlayerPreview(current, modifier = Modifier.width(150.dp))
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.player_builder_presets), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                PlayerTheme.presets.forEach { (id, preset) ->
                    FilterChip(
                        selected = current == preset,
                        onClick = { update(preset) },
                        label = { Text(stringResource(presetName(id))) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(4.dp))
                FilledTonalButton(onClick = { shareTheme(context, current) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.share), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.player_builder_share), maxLines = 1)
                }
                OutlinedButton(onClick = { showPaste = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.player_builder_paste), maxLines = 1)
                }
            }
        }

        found?.let { foundTheme ->
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.player_builder_found), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { found = null }) { Text(stringResource(android.R.string.cancel)) }
                        TextButton(onClick = {
                            receive(foundTheme)
                            found = null
                        }) { Text(stringResource(R.string.player_builder_apply)) }
                    }
                }
            }
        }

        Section(R.string.player_builder_gallery) {
            if (gallery.isEmpty()) {
                Text(
                    stringResource(R.string.player_builder_gallery_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                ) {
                    gallery.forEach { item ->
                        GalleryCard(
                            item = item,
                            selected = item.theme == current,
                            onApply = { update(item.theme) },
                            onRename = { naming = item },
                            onShare = { shareTheme(context, item.theme) },
                            onDelete = { scope.launch { context.editThemeGallery { PlayerThemeGallery.remove(it, item.id) } } },
                        )
                    }
                }
            }
            TextButton(onClick = {
                naming = SavedTheme(
                    name = PlayerThemeGallery.freeName(gallery, context.getString(R.string.player_builder_gallery_default_name)),
                    theme = current,
                )
            }) { Text(stringResource(R.string.player_builder_gallery_save)) }
        }

        Section(R.string.player_builder_night) {
            Choice(
                listOf(
                    NightMode.OFF to R.string.player_builder_night_off,
                    NightMode.SYSTEM to R.string.player_builder_night_system,
                    NightMode.HOURS to R.string.player_builder_night_hours,
                ),
                nightMode,
            ) {
                nightMode = it
                if (it == NightMode.OFF) editNight = false
            }
            if (nightMode == NightMode.HOURS) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedButton(onClick = { pickTime(context, nightFrom) { nightFrom = it } }) {
                        Text(stringResource(R.string.player_builder_night_from, clockText(nightFrom)))
                    }
                    OutlinedButton(onClick = { pickTime(context, nightTo) { nightTo = it } }) {
                        Text(stringResource(R.string.player_builder_night_to, clockText(nightTo)))
                    }
                }
            }
            if (nightMode != NightMode.OFF) {
                Text(
                    stringResource(R.string.player_builder_night_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (!editingNight) {
                    TextButton(onClick = { editNight = true }) { Text(stringResource(R.string.player_builder_night_edit)) }
                }
            }
        }

        Section(R.string.player_builder_background) {
            Choice(
                listOf(
                    PlayerTheme.Background.ARTWORK to R.string.player_builder_bg_artwork,
                    PlayerTheme.Background.BLUR to R.string.player_builder_bg_blur,
                    PlayerTheme.Background.SOLID to R.string.player_builder_bg_solid,
                    PlayerTheme.Background.GRADIENT to R.string.player_builder_bg_gradient,
                ),
                current.background,
            ) { update(current.copy(background = it)) }
            Toggle(R.string.player_builder_light, current.light) { update(current.copy(light = it, textColor = null)) }
            if (current.background == PlayerTheme.Background.SOLID || current.background == PlayerTheme.Background.GRADIENT) {
                SwatchRow(R.string.player_builder_color_top, current.color1) { picked -> picked?.let { update(current.copy(color1 = it)) } }
            }
            if (current.background == PlayerTheme.Background.GRADIENT) {
                SwatchRow(R.string.player_builder_color_bottom, current.color2) { picked -> picked?.let { update(current.copy(color2 = it)) } }
            }
            Labeled(R.string.player_builder_motion) {
                Choice(
                    listOf(
                        PlayerTheme.Motion.NONE to R.string.player_builder_motion_none,
                        PlayerTheme.Motion.FLOW to R.string.player_builder_motion_flow,
                        PlayerTheme.Motion.BARS to R.string.player_builder_motion_bars,
                        PlayerTheme.Motion.WAVE to R.string.player_builder_motion_wave,
                    ),
                    current.motion,
                    ::pickMotion,
                )
            }
            if (current.usesVisualizer && !canHear) {
                Text(
                    stringResource(R.string.player_builder_motion_permission),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextButton(onClick = { askToHear.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text(stringResource(R.string.player_builder_motion_allow))
                }
            }
        }

        Section(R.string.player_builder_colors) {
            Toggle(R.string.player_builder_accent_artwork, current.accentFromArtwork) { update(current.copy(accentFromArtwork = it)) }
            if (!current.accentFromArtwork) {
                SwatchRow(R.string.player_builder_accent, current.accent) { picked -> picked?.let { update(current.copy(accent = it)) } }
            }
            SwatchRow(R.string.player_builder_text, current.textColor, allowAuto = true) { update(current.copy(textColor = it)) }
            SwatchRow(R.string.player_builder_like, current.likeColor, allowAuto = true) { update(current.copy(likeColor = it)) }
        }

        Section(R.string.player_builder_slider) {
            Choice(
                listOf(
                    PlayerTheme.Slider.THIN to R.string.player_builder_slider_thin,
                    PlayerTheme.Slider.DIAL to R.string.player_builder_slider_dial,
                    PlayerTheme.Slider.THICK to R.string.player_builder_slider_thick,
                    PlayerTheme.Slider.WAVE to R.string.player_builder_slider_wave,
                ),
                current.slider,
            ) { update(current.copy(slider = it)) }
            Toggle(R.string.player_builder_show_time, current.showTime) { update(current.copy(showTime = it)) }
        }

        Section(R.string.player_builder_play) {
            Choice(
                listOf(
                    PlayerTheme.PlayButton.CIRCLE to R.string.player_builder_play_circle,
                    PlayerTheme.PlayButton.SQUIRCLE to R.string.player_builder_play_squircle,
                    PlayerTheme.PlayButton.PILL to R.string.player_builder_play_pill,
                    PlayerTheme.PlayButton.KNOB to R.string.player_builder_play_knob,
                ),
                current.playButton,
            ) { update(current.copy(playButton = it)) }
            Toggle(R.string.player_builder_shuffle_repeat, current.showShuffleRepeat) { update(current.copy(showShuffleRepeat = it)) }
        }

        Section(R.string.player_builder_artwork) {
            // Bigger cover = smaller margin, so the slider is drawn the other way round.
            Labeled(R.string.player_builder_art_size) {
                Slider(
                    value = (64 - current.artMargin).toFloat(),
                    onValueChange = { update(current.copy(artMargin = 64 - it.toInt())) },
                    valueRange = 8f..56f,
                )
            }
            Labeled(R.string.player_builder_art_shape) {
                Choice(
                    listOf(
                        PlayerTheme.ArtShape.ROUNDED to R.string.player_builder_shape_rounded,
                        PlayerTheme.ArtShape.ARCH to R.string.player_builder_shape_arch,
                        PlayerTheme.ArtShape.COOKIE to R.string.player_builder_shape_cookie,
                        PlayerTheme.ArtShape.CLOVER to R.string.player_builder_shape_clover,
                        PlayerTheme.ArtShape.SUNNY to R.string.player_builder_shape_sunny,
                    ),
                    current.artShape,
                ) { update(current.copy(artShape = it)) }
            }
            if (current.artShape == PlayerTheme.ArtShape.ROUNDED) {
                Labeled(R.string.player_builder_art_corners) {
                    Slider(
                        value = current.artCorner.toFloat(),
                        onValueChange = { update(current.copy(artCorner = it.toInt())) },
                        valueRange = 0f..PlayerTheme.ART_CIRCLE.toFloat(),
                    )
                }
            }
            Labeled(R.string.player_builder_art_border) {
                Slider(
                    value = current.artBorder.toFloat(),
                    onValueChange = { update(current.copy(artBorder = it.toInt())) },
                    valueRange = 0f..8f,
                    steps = 7,
                )
            }
            Toggle(R.string.player_builder_art_shadow, current.artShadow) { update(current.copy(artShadow = it)) }
            Toggle(
                R.string.player_builder_art_spin,
                current.artSpin,
                enabled = current.canSpin,
                hint = if (!current.canSpin) R.string.player_builder_art_spin_hint else null,
            ) { update(current.copy(artSpin = it)) }
        }

        Section(R.string.player_builder_title) {
            Choice(
                listOf(false to R.string.player_builder_align_left, true to R.string.player_builder_align_center),
                current.titleCentered,
            ) { update(current.copy(titleCentered = it)) }
            Choice(
                listOf(
                    PlayerTheme.Font.SYSTEM to R.string.player_builder_font_system,
                    PlayerTheme.Font.OSWALD to R.string.player_builder_font_poster,
                    PlayerTheme.Font.MONO to R.string.player_builder_font_mono,
                ),
                current.font,
            ) { update(current.copy(font = it)) }
            Labeled(R.string.player_builder_title_size) {
                Slider(
                    value = current.titleSize.toFloat(),
                    onValueChange = { update(current.copy(titleSize = it.toInt())) },
                    valueRange = 18f..34f,
                )
            }
            Toggle(R.string.player_builder_on_air, current.onAirHeader) { update(current.copy(onAirHeader = it)) }
        }

        Section(R.string.player_builder_lyrics) {
            LyricsPreview(current)
            Labeled(R.string.player_builder_lyrics_font) {
                Choice(
                    listOf(
                        PlayerTheme.Font.SYSTEM to R.string.player_builder_font_system,
                        PlayerTheme.Font.OSWALD to R.string.player_builder_font_poster,
                        PlayerTheme.Font.MONO to R.string.player_builder_font_mono,
                    ),
                    current.lyricsFont,
                ) { update(current.copy(lyricsFont = it)) }
            }
            Labeled(R.string.player_builder_lyrics_color) {
                Choice(
                    listOf(false to R.string.player_builder_lyrics_color_text, true to R.string.player_builder_lyrics_color_accent),
                    current.lyricsAccent,
                ) { update(current.copy(lyricsAccent = it)) }
            }
            Labeled(R.string.player_builder_lyrics_align) {
                Choice(
                    listOf(
                        PlayerTheme.LyricsAlign.AUTO to R.string.player_builder_lyrics_align_auto,
                        PlayerTheme.LyricsAlign.LEFT to R.string.player_builder_align_left,
                        PlayerTheme.LyricsAlign.CENTER to R.string.player_builder_align_center,
                        PlayerTheme.LyricsAlign.RIGHT to R.string.player_builder_align_right,
                    ),
                    current.lyricsAlign,
                ) { update(current.copy(lyricsAlign = it)) }
            }
        }

        Section(R.string.player_builder_bottom) {
            Choice(
                listOf(
                    PlayerTheme.BottomStyle.ICONS to R.string.player_builder_bottom_icons,
                    PlayerTheme.BottomStyle.QUIET to R.string.player_builder_bottom_quiet,
                    PlayerTheme.BottomStyle.PANEL to R.string.player_builder_bottom_panel,
                ),
                current.bottomStyle,
            ) { update(current.copy(bottomStyle = it)) }
            Text(
                stringResource(R.string.player_builder_bottom_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayerTheme.BottomButton.entries.forEach { button ->
                    val index = current.buttons.indexOf(button)
                    FilterChip(
                        selected = index >= 0,
                        onClick = {
                            val next = if (index >= 0) current.buttons - button else current.buttons + button
                            if (next.size <= PlayerTheme.MAX_BUTTONS) update(current.copy(buttons = next))
                        },
                        label = { Text(if (index >= 0) "${index + 1}. ${stringResource(buttonName(button))}" else stringResource(buttonName(button))) },
                    )
                }
            }
        }

        Section(R.string.player_builder_mini) {
            MiniPreview(current)
            Spacer(Modifier.height(8.dp))
            Choice(
                listOf(
                    MiniTheme.Background.APP to R.string.player_builder_mini_app,
                    MiniTheme.Background.PLAYER to R.string.player_builder_mini_player,
                    MiniTheme.Background.BLUR to R.string.player_builder_bg_blur,
                    MiniTheme.Background.BLACK to R.string.player_builder_mini_black,
                ),
                current.mini.background,
            ) { update(current.copy(mini = current.mini.copy(background = it))) }
            Labeled(R.string.player_builder_mini_corners) {
                Slider(
                    value = current.mini.corner.toFloat(),
                    onValueChange = { update(current.copy(mini = current.mini.copy(corner = it.toInt()))) },
                    valueRange = 0f..32f,
                )
            }
            Choice(
                listOf(
                    MiniTheme.Progress.RING to R.string.player_builder_mini_ring,
                    MiniTheme.Progress.LINE to R.string.player_builder_mini_line,
                    MiniTheme.Progress.NONE to R.string.player_builder_mini_none,
                ),
                current.mini.progress,
            ) { update(current.copy(mini = current.mini.copy(progress = it))) }
            Toggle(R.string.player_builder_mini_like, current.mini.showLike) { update(current.copy(mini = current.mini.copy(showLike = it))) }
            Toggle(R.string.player_builder_mini_next, current.mini.showNext) { update(current.copy(mini = current.mini.copy(showNext = it))) }
            Toggle(R.string.player_builder_mini_subscribe, current.mini.showSubscribe) { update(current.copy(mini = current.mini.copy(showSubscribe = it))) }
        }

        Section(R.string.lock_screen_player) {
            Toggle(R.string.lock_screen_player_toggle, lockScreen, hint = R.string.lock_screen_player_hint) {
                lockScreen = it
                if (it && !canOpenOverLock) askOverlay()
            }
            if (lockScreen && !canOpenOverLock) {
                Text(
                    stringResource(R.string.lock_screen_permission),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextButton(onClick = ::askOverlay) { Text(stringResource(R.string.lock_screen_permission_open)) }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { update(PlayerTheme.Clean) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.player_builder_reset))
        }
    }

    naming?.let { draft ->
        NameThemeDialog(
            initial = draft.name,
            onDismiss = { naming = null },
            onSave = { name ->
                val exists = gallery.any { it.id == draft.id }
                scope.launch {
                    context.editThemeGallery {
                        if (exists) PlayerThemeGallery.rename(it, draft.id, name) else PlayerThemeGallery.add(it, draft.copy(name = name))
                    }
                }
                if (!exists) Toast.makeText(context, R.string.player_builder_gallery_saved, Toast.LENGTH_SHORT).show()
                naming = null
            },
        )
    }

    if (showPaste) {
        PasteThemeDialog(
            onDismiss = { showPaste = false },
            onApply = {
                receive(it)
                showPaste = false
            },
        )
    }
}

private fun presetName(id: String) = when (id) {
    "on_air" -> R.string.player_design_radio
    "light" -> R.string.player_design_soft
    else -> R.string.player_design_clean
}

private fun buttonName(button: PlayerTheme.BottomButton) = when (button) {
    PlayerTheme.BottomButton.QUEUE -> R.string.pulse_player_queue
    PlayerTheme.BottomButton.LYRICS -> R.string.pulse_player_lyrics
    PlayerTheme.BottomButton.TIMER -> R.string.pulse_player_timer
    PlayerTheme.BottomButton.SHARE -> R.string.pulse_player_share
    PlayerTheme.BottomButton.SHUFFLE -> R.string.pulse_player_shuffle
    PlayerTheme.BottomButton.REPEAT -> R.string.pulse_player_repeat
}

private fun readClipboard(context: Context): String? = runCatching {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
}.getOrNull()

private fun shareTheme(context: Context, theme: PlayerTheme) {
    val text = context.getString(R.string.player_builder_share_text, theme.toCode())
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

@Composable
private fun PasteThemeDialog(onDismiss: () -> Unit, onApply: (PlayerTheme) -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(readClipboard(context).orEmpty()) }
    val parsed = remember(text) { PlayerTheme.fromCode(text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_builder_paste)) },
        text = {
            Column {
                Text(stringResource(R.string.player_builder_paste_hint), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    isError = text.isNotBlank() && parsed == null,
                    supportingText = {
                        if (text.isNotBlank() && parsed == null) Text(stringResource(R.string.player_builder_paste_bad))
                    },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null,
                onClick = {
                    parsed?.let(onApply)
                    Toast.makeText(context, R.string.player_builder_applied, Toast.LENGTH_SHORT).show()
                },
            ) { Text(stringResource(R.string.player_builder_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

private fun clockText(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

private fun pickTime(context: Context, minutes: Int, onPick: (Int) -> Unit) {
    TimePickerDialog(context, { _, hour, minute -> onPick(hour * 60 + minute) }, minutes / 60, minutes % 60, true).show()
}

@Composable
private fun NameThemeDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_builder_gallery_name)) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim()) }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

/** A saved theme: a still preview and its name; tap applies it, long press opens the menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryCard(
    item: SavedTheme,
    selected: Boolean,
    onApply: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onApply, onLongClick = { menu = true })
            .padding(4.dp),
    ) {
        Box {
            PlayerPreview(
                item.theme,
                animate = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier),
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.player_builder_gallery_rename)) }, onClick = { menu = false; onRename() })
                DropdownMenuItem(text = { Text(stringResource(R.string.player_builder_share)) }, onClick = { menu = false; onShare() })
                DropdownMenuItem(text = { Text(stringResource(R.string.player_builder_gallery_delete)) }, onClick = { menu = false; onDelete() })
            }
        }
        Text(
            item.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (item.fromFriend) {
            Text(
                stringResource(R.string.player_builder_gallery_from_friend),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

// ---------------------------------------------------------------- building blocks

@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    Spacer(Modifier.height(20.dp))
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) { content() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choice(options: List<Pair<T, Int>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(stringResource(label)) })
        }
    }
}

@Composable
private fun Toggle(title: Int, checked: Boolean, enabled: Boolean = true, hint: Int? = null, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
            if (hint != null) {
                Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun Labeled(title: Int, content: @Composable () -> Unit) {
    Text(stringResource(title), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
    content()
}

/** A row of colour circles; with [allowAuto] the first circle means "let the theme decide". */
@Composable
private fun SwatchRow(title: Int, selected: Long?, allowAuto: Boolean = false, onSelect: (Long?) -> Unit) {
    Text(stringResource(title), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 4.dp),
    ) {
        if (allowAuto) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(if (selected == null) 3.dp else 1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (selected == null) 1f else 0.4f), CircleShape)
                    .clickable { onSelect(null) },
            ) {
                Text(stringResource(R.string.player_builder_auto), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Clip)
            }
        }
        Swatches.forEach { argb ->
            val isSelected = selected == argb
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(
                        if (isSelected) 3.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    )
                    .clickable { onSelect(argb) },
            )
        }
    }
}

// ---------------------------------------------------------------- previews

/** The real player composables at phone size, scaled down; the current song's cover if one plays. */
@Composable
private fun PlayerPreview(theme: PlayerTheme, modifier: Modifier = Modifier, animate: Boolean = true) {
    val playerConnection = LocalPlayerConnection.current
    val metadata = playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val title = metadata?.title ?: "Чёрный день свободы"
    val artist = metadata?.artists?.joinToString { it.name } ?: "этажность"
    val phoneWidth = 412.dp
    val phoneHeight = 915.dp
    Box(
        modifier = modifier
            .aspectRatio(phoneWidth / phoneHeight)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val scale = maxWidth / phoneWidth
            Box(
                Modifier
                    .requiredSize(phoneWidth, phoneHeight)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
                CompositionLocalProvider(LocalPulseArtworkColor provides SampleArtwork) {
                    PreviewPhone(theme, metadata?.thumbnailUrl, title, artist, animate)
                }
            }
        }
    }
}

@Composable
private fun PreviewPhone(theme: PlayerTheme, thumbnailUrl: String?, title: String, artist: String, animate: Boolean) {
    Box(
        Modifier
            .fillMaxSize()
            .background(theme.baseBackground),
    ) {
        PulsePlayerBackground(theme, listOf(SampleArtwork), thumbnailUrl)
        if (theme.motion != PlayerTheme.Motion.NONE) {
            // No song is heard here, so the bars always move to the made-up beat.
            val levels = rememberAudioLevels(audioSessionId = 0, active = animate && theme.usesVisualizer)
            PulseMotionLayer(theme, listOf(SampleArtwork), pulsePalette(theme).accent, levels, active = animate, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.fillMaxSize().padding(top = 32.dp)) {
            Column(Modifier.weight(1f)) {
                PulsePlayerHeader(theme, null, "Pulse", {}, {})
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PreviewArtwork(theme, thumbnailUrl)
                }
            }
            PulseControlsColumn(theme) {
                PulseTrackInfo(theme, title, artist, true, {}, {}, {}, {}, {})
                PulseProgress(theme, 74_000, 265_000, false, {}, {})
                PulseTransport(theme, R.drawable.pulse_pause, stringResource(R.string.player_pause), true, true, true, true, 0, {}, {}, {}, {}, {})
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(theme.peekHeight + 24.dp)) {
                PulseBottomRow(
                    theme,
                    PulseBottomActions(false, null, true, 0, true, {}, {}, {}, {}, {}, {}),
                    Modifier.fillMaxWidth().padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun PreviewArtwork(theme: PlayerTheme, thumbnailUrl: String?) {
    val shape = pulseArtShape(theme)
    val border = pulseArtBorder(theme)
    val modifier = Modifier
        .padding(horizontal = theme.artPadding)
        .fillMaxWidth()
        .aspectRatio(1f)
        .then(if (theme.artShadow) Modifier.graphicsLayer { shadowElevation = 40f; this.shape = shape; clip = false } else Modifier)
        .clip(shape)
        .then(border?.let { Modifier.border(it, shape) } ?: Modifier)
    if (thumbnailUrl != null) {
        AsyncImage(model = thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier.background(Brush.verticalGradient(listOf(Color(0xFFC9D6EA), Color(0xFF7C93B8), Color(0xFF34548A)))))
    }
}

/** Three lines of a song on the player's background, with the theme's lyrics font, colour and alignment. */
@Composable
private fun LyricsPreview(theme: PlayerTheme) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(theme.baseBackground),
    ) {
        CompositionLocalProvider(LocalPulseArtworkColor provides SampleArtwork) {
            PulsePlayerBackground(theme, listOf(SampleArtwork), null)
            val lineColor = pulseLyricsColor(theme)
            val align = when (lyricsPosition(theme, LyricsPosition.CENTER)) {
                LyricsPosition.LEFT -> TextAlign.Start
                LyricsPosition.CENTER -> TextAlign.Center
                LyricsPosition.RIGHT -> TextAlign.End
            }
            val style = TextStyle(fontFamily = pulseLyricsFont(theme), fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, textAlign = align)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            ) {
                Text(stringResource(R.string.player_builder_lyrics_sample_1), style = style, color = lineColor.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.player_builder_lyrics_sample_2), style = style, color = lineColor, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.player_builder_lyrics_sample_3), style = style, color = lineColor.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** A drawing of the mini player with the theme's background, shape, progress and buttons. */
@Composable
private fun MiniPreview(theme: PlayerTheme) {
    val mini = theme.mini
    val p = pulsePalette(theme)
    val shape = RoundedCornerShape(mini.corner.dp)
    val (background, content, accent) = when (mini.background) {
        MiniTheme.Background.APP -> Triple(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.primary)
        MiniTheme.Background.PLAYER -> Triple(theme.baseBackground, theme.contentColor, p.accent)
        MiniTheme.Background.BLUR -> Triple(Color(0xFF3A4458), Color.White, Color.White)
        MiniTheme.Background.BLACK -> Triple(Color.Black, Color.White, Color.White)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, content.copy(alpha = 0.3f), shape),
    ) {
        if (mini.background == MiniTheme.Background.PLAYER) {
            CompositionLocalProvider(LocalPulseArtworkColor provides SampleArtwork) {
                PulsePlayerBackground(theme, listOf(SampleArtwork), null)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .then(if (mini.progress == MiniTheme.Progress.RING) Modifier.border(3.dp, accent, CircleShape) else Modifier),
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color(0xFFC9D6EA), Color(0xFF34548A)))),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Чёрный день свободы", color = content, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text("этажность", color = content.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1)
            }
            if (mini.showSubscribe) MiniIcon(R.drawable.subscribe, content, 20.dp)
            if (mini.showLike) MiniIcon(R.drawable.favorite, p.like.takeIf { mini.background == MiniTheme.Background.PLAYER } ?: accent, 22.dp)
            if (mini.showNext) MiniIcon(R.drawable.pulse_skip_next, content, 24.dp)
        }
        if (mini.progress == MiniTheme.Progress.LINE) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.28f)
                    .height(3.dp)
                    .background(accent),
            )
        }
    }
}

@Composable
private fun MiniIcon(icon: Int, tint: Color, size: Dp) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(size))
    }
}
