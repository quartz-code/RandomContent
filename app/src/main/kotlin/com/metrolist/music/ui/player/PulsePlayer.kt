package com.metrolist.music.ui.player

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.media3.common.Player
import com.metrolist.music.R
import com.metrolist.music.constants.PlayerDesign
import com.metrolist.music.ui.theme.Oswald
import com.metrolist.music.ui.theme.PtMono
import com.metrolist.music.utils.makeTimeString

/*
 * Pulse: the player designs from the Figma file "Pulse — плеер, варианты".
 *  - CLEAN («Чистый», default): dark, background tinted by the artwork, thin slider, round white button.
 *  - RADIO («Эфир»): Shortwave frequency dial with a red needle, amber knob, labelled panel.
 *  - SOFT («Светлый»): light and calm, everything centred, the artwork's own colour on the play button.
 */

private val Cream = Color(0xFFEFE8D6)
private val Amber = Color(0xFFF0A43A)
private val Needle = Color(0xFFE0482F)
private val Grey = Color(0xFF85887C)
private val Ink = Color(0xFF1B1A16)
private val Olive = Color(0xFF22261D)
private val Night = Color(0xFF121310)
private val CleanBottom = Color(0xFF101215)
private val CleanFallbackTop = Color(0xFF3E4A5E)
private val SoftInk = Color(0xFF1D1B20)
private val SoftPaper = Color(0xFFF7F5F2)
private val SoftFallbackAccent = Color(0xFF4A5D7A)

/** The artwork's main colour while a song plays; only the light design uses it for its controls. */
internal val LocalPulseArtworkColor = compositionLocalOf<Color?> { null }

@Immutable
internal data class PulsePalette(
    val content: Color,
    val muted: Color,
    val accent: Color,
    val like: Color,
    val play: Color,
    val onPlay: Color,
    val panel: Color,
    val button: Color,
    val tileActive: Color,
    val outline: Color,
    val track: Color,
    val tick: Color,
)

internal val PlayerDesign.palette: PulsePalette
    get() = when (this) {
        PlayerDesign.CLEAN -> PulsePalette(
            content = Color.White,
            muted = Color.White.copy(alpha = 0.6f),
            accent = Color.White,
            like = Color.White,
            play = Color.White,
            onPlay = CleanBottom,
            panel = Color.White.copy(alpha = 0.12f),
            button = Color.White.copy(alpha = 0.16f),
            tileActive = Color.White.copy(alpha = 0.26f),
            outline = Color.White.copy(alpha = 0.2f),
            track = Color.White.copy(alpha = 0.22f),
            tick = Color.White.copy(alpha = 0.3f),
        )
        PlayerDesign.RADIO -> PulsePalette(
            content = Cream,
            muted = Grey,
            accent = Amber,
            like = Needle,
            play = Amber,
            onPlay = Ink,
            panel = Color(0xFF1C1E19),
            button = Color(0xFF2A2C25),
            tileActive = Color(0xFF4A3417),
            outline = Color(0xFF3E4137),
            track = Color(0xFF3E4137),
            tick = Color(0xFF4A4D42),
        )
        PlayerDesign.SOFT -> softPalette(SoftFallbackAccent)
    }

private fun softPalette(accent: Color) = PulsePalette(
    content = SoftInk,
    muted = SoftInk.copy(alpha = 0.55f),
    accent = accent,
    like = accent,
    play = accent,
    onPlay = Color.White,
    panel = SoftInk.copy(alpha = 0.05f),
    button = SoftInk.copy(alpha = 0.06f),
    tileActive = accent.copy(alpha = 0.14f),
    outline = SoftInk.copy(alpha = 0.1f),
    track = SoftInk.copy(alpha = 0.12f),
    tick = SoftInk.copy(alpha = 0.2f),
)

/** The palette as drawn right now: the light design takes its accent from the artwork. */
@Composable
internal fun pulsePalette(design: PlayerDesign): PulsePalette {
    if (design != PlayerDesign.SOFT) return design.palette
    val accent by animateColorAsState(
        targetValue = LocalPulseArtworkColor.current?.let { softTone(it, lightness = 0.42f, minSaturation = 0.32f) } ?: SoftFallbackAccent,
        animationSpec = tween(800),
        label = "pulseSoftAccent",
    )
    return softPalette(accent)
}

/** Keeps the artwork's hue but moves it to a lightness that works on (or as) a light background. */
private fun softTone(color: Color, lightness: Float, minSaturation: Float, maxSaturation: Float = 0.75f): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[1] = hsl[1].coerceIn(minSaturation, maxSaturation)
    hsl[2] = lightness
    return Color(ColorUtils.HSLToColor(hsl))
}

/** Height of the queue sheet's peek, which holds the design's bottom row. */
internal val PlayerDesign.peekHeight: Dp
    get() = when (this) {
        PlayerDesign.CLEAN -> 72.dp
        PlayerDesign.RADIO -> 84.dp
        PlayerDesign.SOFT -> 76.dp
    }

internal val PlayerDesign.artPadding: Dp
    get() = when (this) {
        PlayerDesign.CLEAN -> 24.dp
        PlayerDesign.RADIO -> 32.dp
        PlayerDesign.SOFT -> 28.dp
    }

internal val PlayerDesign.artCorner: Dp
    get() = when (this) {
        PlayerDesign.CLEAN -> 14.dp
        PlayerDesign.RADIO -> 8.dp
        PlayerDesign.SOFT -> 20.dp
    }

internal val PlayerDesign.isLight: Boolean
    get() = this == PlayerDesign.SOFT

/** Colour under the background while the sheet opens, so the app theme never flashes through. */
internal val PlayerDesign.baseBackground: Color
    get() = when (this) {
        PlayerDesign.CLEAN -> CleanBottom
        PlayerDesign.RADIO -> Night
        PlayerDesign.SOFT -> SoftPaper
    }

@Composable
internal fun PulsePlayerBackground(
    design: PlayerDesign,
    artworkColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    when (design) {
        PlayerDesign.CLEAN -> {
            // Artwork colours can be light; darkening the top keeps white text readable.
            val top by animateColorAsState(
                targetValue = lerp(artworkColors.firstOrNull() ?: CleanFallbackTop, Color.Black, 0.35f),
                animationSpec = tween(800),
                label = "pulseCleanTop",
            )
            Box(
                modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0f to top, 0.72f to CleanBottom)),
            )
        }
        PlayerDesign.RADIO -> Box(
            modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to Olive, 0.5f to Night)),
        )
        PlayerDesign.SOFT -> {
            // A pale wash of the artwork's colour that fades into paper.
            val top by animateColorAsState(
                targetValue = artworkColors.firstOrNull()?.let { softTone(it, lightness = 0.9f, minSaturation = 0.2f, maxSaturation = 0.45f) }
                    ?: Color(0xFFE6EAF0),
                animationSpec = tween(800),
                label = "pulseSoftTop",
            )
            Box(
                modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0f to top, 0.7f to SoftPaper)),
            )
        }
    }
}

// ---------------------------------------------------------------- 1 · header

@Composable
internal fun PulsePlayerHeader(
    design: PlayerDesign,
    listenTogetherLabel: String?,
    source: String?,
    onCollapse: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = pulsePalette(design)
    val label = listenTogetherLabel
        ?: stringResource(if (design == PlayerDesign.RADIO) R.string.pulse_player_on_air else R.string.now_playing)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
    ) {
        PulseIconButton(R.drawable.expand_more, null, p.content.copy(alpha = 0.85f), onCollapse, iconSize = 28.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            if (design == PlayerDesign.RADIO) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .shadow(4.dp, CircleShape, ambientColor = Needle, spotColor = Needle)
                            .background(Needle, CircleShape),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(label.uppercase(), color = Amber, style = monoStyle(11, 2.0), maxLines = 1)
                }
            } else {
                Text(
                    text = label.uppercase(),
                    color = p.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                    maxLines = 1,
                )
            }
            if (!source.isNullOrBlank()) {
                Text(
                    text = source,
                    color = if (design == PlayerDesign.RADIO) p.content.copy(alpha = 0.8f) else p.content,
                    style = if (design == PlayerDesign.RADIO) {
                        TextStyle(fontFamily = Oswald, fontSize = 15.sp)
                    } else {
                        TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    },
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
            }
        }
        PulseIconButton(R.drawable.more_vert, null, p.content.copy(alpha = 0.85f), onMore, iconSize = 24.dp)
    }
}

// ---------------------------------------------------------------- 3 · title, artist, like

/** Groups the title, progress and buttons under the artwork. */
@Composable
internal fun PulseControlsColumn(
    design: PlayerDesign,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = design.artPadding),
        content = content,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PulseTrackInfo(
    design: PlayerDesign,
    title: String,
    artists: String,
    isFavorite: Boolean,
    onLike: () -> Unit,
    onTitleClick: () -> Unit,
    onTitleLongClick: () -> Unit,
    onArtistsClick: () -> Unit,
    onArtistsLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val p = pulsePalette(design)
    // The light design centres the title; with a small artwork in front (lyrics open) it reads left.
    val centred = design == PlayerDesign.SOFT && leading == null
    val titleStyle = when (design) {
        PlayerDesign.CLEAN -> TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
        PlayerDesign.RADIO -> TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 38.sp)
        PlayerDesign.SOFT -> TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    }
    val artistStyle = when (design) {
        PlayerDesign.CLEAN -> TextStyle(fontSize = 16.sp, color = p.muted)
        PlayerDesign.RADIO -> monoStyle(14, 0.5).copy(color = Amber)
        PlayerDesign.SOFT -> TextStyle(fontSize = 15.sp, color = p.muted)
    }
    val align = if (centred) TextAlign.Center else TextAlign.Start
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        } else if (centred) {
            // Balances the like button so the text sits in the middle of the screen.
            Spacer(Modifier.width(60.dp))
        }
        Column(
            horizontalAlignment = if (centred) Alignment.CenterHorizontally else Alignment.Start,
            modifier = Modifier.weight(1f),
        ) {
            AnimatedContent(
                targetState = title,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "pulseTitle",
            ) { text ->
                Text(
                    text = text,
                    style = titleStyle,
                    color = p.content,
                    textAlign = align,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onTitleClick,
                            onLongClick = onTitleLongClick,
                        ),
                )
            }
            if (artists.isNotBlank()) {
                Text(
                    text = artists,
                    style = artistStyle,
                    textAlign = align,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onArtistsClick,
                            onLongClick = onArtistsLongClick,
                        ),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        if (trailing != null) {
            trailing()
        } else {
            val icon = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border
            val tint = if (isFavorite) p.like else p.content.copy(alpha = if (design == PlayerDesign.RADIO) 0.7f else 0.8f)
            PulseIconButton(icon, null, tint, onLike, iconSize = if (design == PlayerDesign.RADIO) 26.dp else 28.dp)
        }
    }
}

/** The same trailing slot as the like button, used for the lyrics menu while lyrics are shown. */
@Composable
internal fun PulseTrailingButton(
    design: PlayerDesign,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
) {
    PulseIconButton(icon, null, pulsePalette(design).content.copy(alpha = 0.85f), onClick, iconSize = 26.dp)
}

// ---------------------------------------------------------------- 4 · progress

@Composable
internal fun PulseProgress(
    design: PlayerDesign,
    position: Long,
    duration: Long,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = pulsePalette(design)
    val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    var dragging by remember { mutableStateOf(false) }
    val seek by rememberUpdatedState(onSeek)
    val finish by rememberUpdatedState(onSeekFinished)
    val canSeek = enabled && duration > 0

    val gestures = if (!canSeek) {
        Modifier
    } else {
        Modifier
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    seek((offset.x / size.width).coerceIn(0f, 1f).times(duration).toLong())
                    finish()
                }
            }
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        seek((offset.x / size.width).coerceIn(0f, 1f).times(duration).toLong())
                    },
                    onDragEnd = {
                        dragging = false
                        finish()
                    },
                    onDragCancel = {
                        dragging = false
                        finish()
                    },
                ) { change, _ ->
                    change.consume()
                    seek((change.position.x / size.width).coerceIn(0f, 1f).times(duration).toLong())
                }
            }
    }
    val thumbGrow by animateFloatAsState(if (dragging) 1f else 0f, label = "pulseThumb")

    Column(modifier.fillMaxWidth()) {
        if (design == PlayerDesign.RADIO) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .then(gestures),
            ) {
                // A tuning dial: ticks every 6 dp, every fifth one long; the needle marks the position.
                val x = fraction * size.width
                val line = 8.dp.toPx()
                drawRect(p.track, Offset(0f, line), Size(size.width, 1.dp.toPx()))
                drawRect(p.accent, Offset(0f, line - 1.dp.toPx()), Size(x, 2.dp.toPx()))
                val step = 6.dp.toPx()
                var tick = 3.dp.toPx()
                var i = 0
                while (tick <= size.width) {
                    val long = i % 5 == 0
                    drawRect(
                        color = if (tick <= x) p.accent else p.tick,
                        topLeft = Offset(tick, line + 1.dp.toPx()),
                        size = Size(1.dp.toPx(), (if (long) 16 else 8).dp.toPx()),
                    )
                    tick += step
                    i++
                }
                val glow = (9 + 6 * thumbGrow).dp.toPx()
                drawRoundRect(
                    color = Needle.copy(alpha = 0.25f),
                    topLeft = Offset(x - glow / 2, 0f),
                    size = Size(glow, size.height),
                    cornerRadius = CornerRadius(glow / 2),
                )
                drawRoundRect(
                    color = Needle,
                    topLeft = Offset(x - 1.5.dp.toPx(), 1.dp.toPx()),
                    size = Size(3.dp.toPx(), size.height - 2.dp.toPx()),
                    cornerRadius = CornerRadius(1.5.dp.toPx()),
                )
            }
        } else {
            val soft = design == PlayerDesign.SOFT
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .then(gestures),
            ) {
                val x = fraction * size.width
                val h = 4.dp.toPx()
                val y = (size.height - h) / 2
                drawRoundRect(p.track, Offset(0f, y), Size(size.width, h), CornerRadius(h / 2))
                drawRoundRect(p.accent, Offset(0f, y), Size(x, h), CornerRadius(h / 2))
                val radius = if (soft) 6 + 3 * thumbGrow else 7 + 2 * thumbGrow
                drawCircle(p.accent, radius = radius.dp.toPx(), center = Offset(x, size.height / 2))
            }
        }
        val timeStyle = if (design == PlayerDesign.RADIO) monoStyle(12, 0.0) else TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
        val padded = design == PlayerDesign.RADIO
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (design == PlayerDesign.RADIO) 6.dp else 2.dp),
        ) {
            Text(
                text = pulseTime(position, padded),
                style = timeStyle,
                color = if (design == PlayerDesign.RADIO) Amber else p.muted,
            )
            Text(
                text = if (duration > 0) pulseTime(duration, padded) else "",
                style = timeStyle,
                color = if (design == PlayerDesign.RADIO) Grey else p.muted,
            )
        }
    }
}

private fun pulseTime(ms: Long, padded: Boolean): String {
    val text = makeTimeString(ms)
    return if (padded && text.length == 4) "0$text" else text
}

// ---------------------------------------------------------------- 5 · buttons

@Composable
internal fun PulseTransport(
    design: PlayerDesign,
    @DrawableRes playIcon: Int,
    playDescription: String,
    canPrevious: Boolean,
    canNext: Boolean,
    controlsEnabled: Boolean,
    shuffleOn: Boolean,
    repeatMode: Int,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    playModifier: Modifier = Modifier,
) {
    val p = pulsePalette(design)
    val skipIcon = when (design) {
        PlayerDesign.RADIO -> 34.dp
        PlayerDesign.CLEAN -> 40.dp
        PlayerDesign.SOFT -> 36.dp
    }
    val toggleIcon = if (design == PlayerDesign.RADIO) 22.dp else 24.dp
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        ToggleButton(R.drawable.shuffle, shuffleOn, controlsEnabled, toggleIcon, p, onShuffle)
        PressableBox(56.dp, CircleShape, Color.Transparent, canPrevious, onPrevious) {
            PulseIcon(R.drawable.pulse_skip_previous, p.content, skipIcon)
        }
        when (design) {
            PlayerDesign.RADIO -> {
                // The knob: a dark ring around the amber cap, like on the Sever's front panel.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(92.dp)
                        .background(p.panel, CircleShape)
                        .border(1.dp, p.outline, CircleShape),
                ) {
                    PressableBox(76.dp, CircleShape, p.play, true, onPlayPause, playModifier, glow = p.play) {
                        PulseIcon(playIcon, p.onPlay, 34.dp, playDescription)
                    }
                }
            }
            PlayerDesign.CLEAN -> PressableBox(72.dp, CircleShape, p.play, true, onPlayPause, playModifier) {
                PulseIcon(playIcon, p.onPlay, 34.dp, playDescription)
            }
            PlayerDesign.SOFT -> PressableBox(76.dp, CircleShape, p.play, true, onPlayPause, playModifier, glow = p.play) {
                PulseIcon(playIcon, p.onPlay, 34.dp, playDescription)
            }
        }
        PressableBox(56.dp, CircleShape, Color.Transparent, canNext, onNext) {
            PulseIcon(R.drawable.pulse_skip_next, p.content, skipIcon)
        }
        ToggleButton(repeatIcon(repeatMode), repeatMode != Player.REPEAT_MODE_OFF, controlsEnabled, toggleIcon, p, onRepeat)
    }
}

@Composable
private fun ToggleButton(
    @DrawableRes icon: Int,
    active: Boolean,
    enabled: Boolean,
    iconSize: Dp,
    p: PulsePalette,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Switch, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PulseIcon(icon, if (active) p.accent else p.muted, iconSize)
            Spacer(Modifier.height(3.dp))
            Box(
                Modifier
                    .size(4.dp)
                    .background(if (active) p.accent else Color.Transparent, CircleShape),
            )
        }
    }
}

// ---------------------------------------------------------------- 6 · bottom row (queue peek)

@Composable
internal fun PulseBottomRow(
    design: PlayerDesign,
    lyricsShown: Boolean,
    sleepTimerLeft: String?,
    shuffleOn: Boolean,
    repeatMode: Int,
    controlsEnabled: Boolean,
    onQueue: () -> Unit,
    onLyrics: () -> Unit,
    onSleepTimer: () -> Unit,
    onShare: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = pulsePalette(design)
    val queue = stringResource(R.string.pulse_player_queue)
    val lyrics = stringResource(R.string.pulse_player_lyrics)
    val timer = sleepTimerLeft ?: stringResource(R.string.pulse_player_timer)
    when (design) {
        PlayerDesign.RADIO -> Box(modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 16.dp)) {
            val shape = RoundedCornerShape(14.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(shape)
                    .background(p.panel)
                    .border(1.dp, p.button, shape),
            ) {
                PanelCell(R.drawable.queue_music, queue, false, true, p, onQueue)
                PanelDivider(p)
                PanelCell(R.drawable.lyrics, lyrics, lyricsShown, true, p, onLyrics)
                PanelDivider(p)
                PanelCell(R.drawable.bedtime, timer, sleepTimerLeft != null, controlsEnabled, p, onSleepTimer)
                PanelDivider(p)
                PanelCell(R.drawable.share, stringResource(R.string.pulse_player_share), false, true, p, onShare)
            }
        }
        PlayerDesign.CLEAN -> Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            if (sleepTimerLeft != null) {
                Pill(R.drawable.bedtime, sleepTimerLeft, true, controlsEnabled, p, onSleepTimer)
            } else {
                PulseIconButton(R.drawable.bedtime, timer, p.content.copy(alpha = 0.7f), onSleepTimer, iconSize = 22.dp, enabled = controlsEnabled)
            }
            Pill(R.drawable.lyrics, lyrics, lyricsShown, true, p, onLyrics)
            PulseIconButton(R.drawable.queue_music, queue, p.content.copy(alpha = 0.7f), onQueue, iconSize = 22.dp)
        }
        PlayerDesign.SOFT -> Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .height(76.dp)
                .padding(horizontal = 28.dp, vertical = 8.dp),
        ) {
            QuietButton(R.drawable.lyrics, lyrics, lyricsShown, true, p, onLyrics)
            QuietButton(R.drawable.bedtime, timer, sleepTimerLeft != null, controlsEnabled, p, onSleepTimer)
            QuietButton(R.drawable.queue_music, queue, false, true, p, onQueue)
        }
    }
}

@Composable
private fun RowScope.PanelCell(
    @DrawableRes icon: Int,
    label: String,
    active: Boolean,
    enabled: Boolean,
    p: PulsePalette,
    onClick: () -> Unit,
) {
    val color = if (active) p.accent else p.content.copy(alpha = 0.85f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
    ) {
        PulseIcon(icon, color, 20.dp)
        Spacer(Modifier.height(5.dp))
        Text(
            text = label.uppercase(),
            style = monoStyle(10, 1.0),
            color = if (active) p.accent else p.muted,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun PanelDivider(p: PulsePalette) {
    Box(
        Modifier
            .width(1.dp)
            .height(28.dp)
            .background(p.button),
    )
}

/** Light design: an icon with a small caption, tinted when it is on. */
@Composable
private fun RowScope.QuietButton(
    @DrawableRes icon: Int,
    label: String,
    active: Boolean,
    enabled: Boolean,
    p: PulsePalette,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) p.tileActive else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
    ) {
        PulseIcon(icon, if (active) p.accent else p.content.copy(alpha = 0.7f), 22.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (active) p.accent else p.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Pill(
    @DrawableRes icon: Int,
    label: String,
    active: Boolean,
    enabled: Boolean,
    p: PulsePalette,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) p.tileActive else p.panel)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        PulseIcon(icon, p.content.copy(alpha = 0.9f), 18.dp)
        Spacer(Modifier.width(8.dp))
        Text(label, color = p.content.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

// ---------------------------------------------------------------- shared bits

private fun repeatIcon(repeatMode: Int) =
    if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat

private fun monoStyle(size: Int, spacing: Double) =
    TextStyle(fontFamily = PtMono, fontSize = size.sp, letterSpacing = spacing.sp)

@Composable
private fun PulseIcon(
    @DrawableRes icon: Int,
    tint: Color,
    size: Dp,
    description: String? = null,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = description,
        tint = tint,
        modifier = Modifier.size(size),
    )
}

@Composable
private fun PulseIconButton(
    @DrawableRes icon: Int,
    description: String?,
    tint: Color,
    onClick: () -> Unit,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    container: Color = Color.Transparent,
    enabled: Boolean = true,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
    ) {
        PulseIcon(icon, tint, iconSize, description)
    }
}

/** A button that sinks a little under the finger; the transport buttons are pressed a lot. */
@Composable
private fun PressableBox(
    size: Dp,
    shape: androidx.compose.ui.graphics.Shape,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glow: Color? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "pulsePress")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale)
            .then(
                if (glow != null) {
                    Modifier.shadow(14.dp, shape, ambientColor = glow, spotColor = glow)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(color)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .alpha(if (enabled) 1f else 0.4f),
    ) {
        content()
    }
}
