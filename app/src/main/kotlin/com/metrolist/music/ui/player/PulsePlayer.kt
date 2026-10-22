package com.metrolist.music.ui.player

import android.os.Build
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import coil3.compose.AsyncImage
import com.metrolist.music.R
import com.metrolist.music.ui.player.PlayerTheme.BottomButton
import com.metrolist.music.ui.theme.Oswald
import com.metrolist.music.ui.theme.PtMono
import com.metrolist.music.utils.makeTimeString
import kotlin.math.PI
import kotlin.math.sin

/*
 * Pulse: the player drawn from a [PlayerTheme]. The three designs from the Figma file
 * ("Чистый", "Эфир", "Светлый") are presets; the player builder can change any detail.
 */

private val SoftInk = Color(0xFF1D1B20)
private val SoftPaper = Color(0xFFF7F5F2)
private val CleanBottom = Color(0xFF101215)
private val CleanFallbackTop = Color(0xFF3E4A5E)
private val Needle = Color(0xFFE0482F)

/** The artwork's main colour while a song plays, for themes that take their colours from it. */
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

/** Colours without the artwork, for places that are not composed (lyrics, settings sketches). */
internal val PlayerTheme.staticPalette: PulsePalette
    get() = paletteFor(this, Color(accent))

internal val PlayerTheme.contentColor: Color
    get() = textColor?.let(::Color) ?: if (light) SoftInk else Color.White

private fun paletteFor(theme: PlayerTheme, accent: Color): PulsePalette {
    val content = theme.contentColor
    val onPlay = if (accent.luminance() > 0.55f) (if (theme.light) SoftInk else CleanBottom) else Color.White
    return PulsePalette(
        content = content,
        muted = content.copy(alpha = if (theme.light) 0.55f else 0.6f),
        accent = accent,
        like = theme.likeColor?.let(::Color) ?: accent,
        play = accent,
        onPlay = onPlay,
        panel = content.copy(alpha = if (theme.light) 0.05f else 0.06f),
        button = content.copy(alpha = 0.1f),
        tileActive = accent.copy(alpha = 0.16f),
        outline = content.copy(alpha = 0.16f),
        track = content.copy(alpha = if (theme.light) 0.12f else 0.22f),
        tick = content.copy(alpha = 0.28f),
    )
}

/** The palette as drawn right now: the accent can follow the artwork of the current song. */
@Composable
internal fun pulsePalette(theme: PlayerTheme): PulsePalette {
    val artwork = LocalPulseArtworkColor.current
    val target = if (theme.accentFromArtwork && artwork != null) {
        softTone(artwork, lightness = if (theme.light) 0.42f else 0.7f, minSaturation = 0.32f)
    } else {
        Color(theme.accent)
    }
    val accent by animateColorAsState(target, tween(800), label = "pulseAccent")
    return paletteFor(theme, accent)
}

/** Keeps the colour's hue but moves it to the given lightness. */
internal fun softTone(color: Color, lightness: Float, minSaturation: Float, maxSaturation: Float = 0.75f): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[1] = hsl[1].coerceIn(minSaturation, maxSaturation)
    hsl[2] = lightness
    return Color(ColorUtils.HSLToColor(hsl))
}

/** Height of the queue sheet's peek, which holds the bottom row. */
internal val PlayerTheme.peekHeight: Dp
    get() = when (bottomStyle) {
        PlayerTheme.BottomStyle.ICONS -> 72.dp
        PlayerTheme.BottomStyle.QUIET -> 76.dp
        PlayerTheme.BottomStyle.PANEL -> 84.dp
    }

internal val PlayerTheme.artPadding: Dp get() = artMargin.dp

/** Large enough to make any artwork round. */
internal val PlayerTheme.artCornerDp: Dp get() = if (artRound) 1000.dp else artCorner.dp

/** Colour under the background while the sheet opens, so the app theme never flashes through. */
internal val PlayerTheme.baseBackground: Color
    get() = when {
        light -> SoftPaper
        background == PlayerTheme.Background.SOLID -> Color(color1)
        background == PlayerTheme.Background.GRADIENT -> Color(color2)
        background == PlayerTheme.Background.BLUR -> Color.Black
        else -> CleanBottom
    }

@Composable
internal fun PulsePlayerBackground(
    theme: PlayerTheme,
    artworkColors: List<Color>,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    val paper = if (theme.light) SoftPaper else CleanBottom
    when (theme.background) {
        PlayerTheme.Background.ARTWORK -> {
            val art = artworkColors.firstOrNull()
            // Artwork colours can be too light for white text, or too dark under black text.
            val target = if (theme.light) {
                art?.let { softTone(it, lightness = 0.9f, minSaturation = 0.2f, maxSaturation = 0.45f) } ?: Color(0xFFE6EAF0)
            } else {
                lerp(art ?: CleanFallbackTop, Color.Black, 0.35f)
            }
            val top by animateColorAsState(target, tween(800), label = "pulseBgTop")
            Box(
                modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0f to top, 0.72f to paper)),
            )
        }
        PlayerTheme.Background.BLUR -> Box(modifier.fillMaxSize().background(paper)) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 80.dp else 0.dp),
                )
            }
            // Older Android cannot blur, so the veil is thicker there.
            val strong = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        if (theme.light) Color.White.copy(alpha = if (strong) 0.8f else 0.55f)
                        else Color.Black.copy(alpha = if (strong) 0.7f else 0.35f),
                    ),
            )
        }
        PlayerTheme.Background.SOLID -> Box(modifier.fillMaxSize().background(Color(theme.color1)))
        PlayerTheme.Background.GRADIENT -> Box(
            modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to Color(theme.color1), 0.6f to Color(theme.color2))),
        )
    }
}

// ---------------------------------------------------------------- 1 · header

@Composable
internal fun PulsePlayerHeader(
    theme: PlayerTheme,
    listenTogetherLabel: String?,
    source: String?,
    onCollapse: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = pulsePalette(theme)
    val label = listenTogetherLabel
        ?: stringResource(if (theme.onAirHeader) R.string.pulse_player_on_air else R.string.now_playing)
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
            if (theme.onAirHeader) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .shadow(4.dp, CircleShape, ambientColor = Needle, spotColor = Needle)
                            .background(Needle, CircleShape),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(label.uppercase(), color = p.accent, style = monoStyle(11, 2.0), maxLines = 1)
                }
            } else {
                Text(
                    text = label.uppercase(),
                    color = p.muted,
                    style = if (theme.font == PlayerTheme.Font.SYSTEM) {
                        TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp)
                    } else {
                        monoStyle(10, 1.5)
                    },
                    maxLines = 1,
                )
            }
            if (!source.isNullOrBlank()) {
                Text(
                    text = source,
                    color = p.content.copy(alpha = if (theme.font == PlayerTheme.Font.SYSTEM) 1f else 0.8f),
                    style = when (theme.font) {
                        PlayerTheme.Font.SYSTEM -> TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        PlayerTheme.Font.OSWALD -> TextStyle(fontFamily = Oswald, fontSize = 15.sp)
                        PlayerTheme.Font.MONO -> monoStyle(13, 0.0)
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
    theme: PlayerTheme,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = modifier
            .fillMaxWidth()
            // Very small artwork margins would push the buttons to the screen edge.
            .padding(horizontal = theme.artMargin.coerceAtLeast(20).dp),
        content = content,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PulseTrackInfo(
    theme: PlayerTheme,
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
    val p = pulsePalette(theme)
    // With a small artwork in front (lyrics open) the title always reads from the left.
    val centred = theme.titleCentered && leading == null
    val size = theme.titleSize
    val titleStyle = when (theme.font) {
        PlayerTheme.Font.SYSTEM -> TextStyle(
            fontSize = size.sp,
            lineHeight = (size * 1.25f).sp,
            fontWeight = if (theme.light) FontWeight.SemiBold else FontWeight.Bold,
        )
        PlayerTheme.Font.OSWALD -> TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Medium, fontSize = size.sp, lineHeight = (size * 1.27f).sp)
        PlayerTheme.Font.MONO -> TextStyle(fontFamily = PtMono, fontSize = (size * 0.85f).sp, lineHeight = (size * 1.1f).sp)
    }
    val artistStyle = when (theme.font) {
        PlayerTheme.Font.SYSTEM -> TextStyle(fontSize = (size * 0.65f).coerceIn(13f, 18f).sp, color = p.muted)
        PlayerTheme.Font.OSWALD -> monoStyle(14, 0.5).copy(color = p.accent)
        PlayerTheme.Font.MONO -> monoStyle(13, 0.3).copy(color = p.muted)
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
            PulseIconButton(icon, null, if (isFavorite) p.like else p.content.copy(alpha = 0.75f), onLike, iconSize = 27.dp)
        }
    }
}

/** The same trailing slot as the like button, used for the lyrics menu while lyrics are shown. */
@Composable
internal fun PulseTrailingButton(
    theme: PlayerTheme,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
) {
    PulseIconButton(icon, null, pulsePalette(theme).content.copy(alpha = 0.85f), onClick, iconSize = 26.dp)
}

// ---------------------------------------------------------------- 4 · progress

@Composable
internal fun PulseProgress(
    theme: PlayerTheme,
    position: Long,
    duration: Long,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = pulsePalette(theme)
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
    val grow by animateFloatAsState(if (dragging) 1f else 0f, label = "pulseThumb")

    Column(modifier.fillMaxWidth()) {
        when (theme.slider) {
            PlayerTheme.Slider.DIAL -> Canvas(
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
                    drawRect(
                        color = if (tick <= x) p.accent else p.tick,
                        topLeft = Offset(tick, line + 1.dp.toPx()),
                        size = Size(1.dp.toPx(), (if (i % 5 == 0) 16 else 8).dp.toPx()),
                    )
                    tick += step
                    i++
                }
                val glow = (9 + 6 * grow).dp.toPx()
                drawRoundRect(Needle.copy(alpha = 0.25f), Offset(x - glow / 2, 0f), Size(glow, size.height), CornerRadius(glow / 2))
                drawRoundRect(Needle, Offset(x - 1.5.dp.toPx(), 1.dp.toPx()), Size(3.dp.toPx(), size.height - 2.dp.toPx()), CornerRadius(1.5.dp.toPx()))
            }
            PlayerTheme.Slider.THICK -> Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .then(gestures),
            ) {
                val x = fraction * size.width
                val h = 10.dp.toPx()
                val y = (size.height - h) / 2
                drawRoundRect(p.track, Offset(0f, y), Size(size.width, h), CornerRadius(h / 2))
                drawRoundRect(p.accent, Offset(0f, y), Size(x, h), CornerRadius(h / 2))
                if (grow > 0f) drawCircle(p.content, radius = 9.dp.toPx() * grow, center = Offset(x, size.height / 2))
            }
            PlayerTheme.Slider.WAVE -> Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .then(gestures),
            ) {
                // The played part ripples; what is left stays a calm line.
                val x = fraction * size.width
                val mid = size.height / 2
                val amplitude = 3.dp.toPx()
                val wave = 18.dp.toPx()
                val path = Path().apply {
                    moveTo(0f, mid)
                    var px = 0f
                    while (px <= x) {
                        lineTo(px, mid + amplitude * sin(2 * PI * px / wave).toFloat())
                        px += 2f
                    }
                }
                drawLine(p.track, Offset(x, mid), Offset(size.width, mid), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                drawPath(path, p.accent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(p.accent, radius = (6 + 3 * grow).dp.toPx(), center = Offset(x, mid))
            }
            PlayerTheme.Slider.THIN -> Canvas(
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
                drawCircle(p.accent, radius = (7 + 2 * grow).dp.toPx(), center = Offset(x, size.height / 2))
            }
        }
        if (theme.showTime) {
            val mono = theme.font != PlayerTheme.Font.SYSTEM
            val timeStyle = if (mono) monoStyle(12, 0.0) else TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
            val padded = theme.slider == PlayerTheme.Slider.DIAL
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (theme.slider == PlayerTheme.Slider.DIAL) 6.dp else 2.dp),
            ) {
                Text(pulseTime(position, padded), style = timeStyle, color = if (mono) p.accent else p.muted)
                Text(if (duration > 0) pulseTime(duration, padded) else "", style = timeStyle, color = p.muted)
            }
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
    theme: PlayerTheme,
    @DrawableRes playIcon: Int,
    playLabel: String,
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
    val p = pulsePalette(theme)
    val skipIcon = if (theme.playButton == PlayerTheme.PlayButton.KNOB) 34.dp else 38.dp
    val previous: @Composable () -> Unit = {
        PressableBox(56.dp, 56.dp, CircleShape, Color.Transparent, canPrevious, onPrevious) {
            PulseIcon(R.drawable.pulse_skip_previous, p.content, skipIcon)
        }
    }
    val next: @Composable () -> Unit = {
        PressableBox(56.dp, 56.dp, CircleShape, Color.Transparent, canNext, onNext) {
            PulseIcon(R.drawable.pulse_skip_next, p.content, skipIcon)
        }
    }
    val play: @Composable () -> Unit = { PlayButton(theme, p, playIcon, playLabel, onPlayPause, playModifier) }

    if (theme.showShuffleRepeat) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth(),
        ) {
            ToggleButton(R.drawable.shuffle, shuffleOn, controlsEnabled, p, onShuffle)
            previous()
            play()
            next()
            ToggleButton(repeatIcon(repeatMode), repeatMode != Player.REPEAT_MODE_OFF, controlsEnabled, p, onRepeat)
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth(),
        ) {
            previous()
            play()
            next()
        }
    }
}

@Composable
private fun PlayButton(
    theme: PlayerTheme,
    p: PulsePalette,
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    when (theme.playButton) {
        PlayerTheme.PlayButton.CIRCLE -> PressableBox(72.dp, 72.dp, CircleShape, p.play, true, onClick, modifier, glow = p.play.takeIf { theme.light }) {
            PulseIcon(icon, p.onPlay, 34.dp, label)
        }
        PlayerTheme.PlayButton.SQUIRCLE -> PressableBox(78.dp, 78.dp, RoundedCornerShape(26.dp), p.play, true, onClick, modifier) {
            PulseIcon(icon, p.onPlay, 36.dp, label)
        }
        PlayerTheme.PlayButton.PILL -> PressableBox(148.dp, 60.dp, RoundedCornerShape(30.dp), p.play, true, onClick, modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulseIcon(icon, p.onPlay, 28.dp)
                Spacer(Modifier.width(8.dp))
                Text(label, color = p.onPlay, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        PlayerTheme.PlayButton.KNOB -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(92.dp)
                .background(p.panel, CircleShape)
                .border(1.dp, p.outline, CircleShape),
        ) {
            // A dark ring around a coloured cap, like the knobs on the Sever's front panel.
            PressableBox(76.dp, 76.dp, CircleShape, p.play, true, onClick, modifier, glow = p.play) {
                PulseIcon(icon, p.onPlay, 34.dp, label)
            }
        }
    }
}

@Composable
private fun ToggleButton(
    @DrawableRes icon: Int,
    active: Boolean,
    enabled: Boolean,
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
            PulseIcon(icon, if (active) p.accent else p.muted, 23.dp)
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

/** What the bottom row's buttons can do; built where the player state lives (the queue sheet). */
internal class PulseBottomActions(
    val lyricsShown: Boolean,
    val sleepTimerLeft: String?,
    val shuffleOn: Boolean,
    val repeatMode: Int,
    val controlsEnabled: Boolean,
    val onQueue: () -> Unit,
    val onLyrics: () -> Unit,
    val onSleepTimer: () -> Unit,
    val onShare: () -> Unit,
    val onShuffle: () -> Unit,
    val onRepeat: () -> Unit,
)

private class BottomItem(
    @DrawableRes val icon: Int,
    val label: String,
    val active: Boolean,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun bottomItem(button: BottomButton, a: PulseBottomActions): BottomItem = when (button) {
    BottomButton.QUEUE -> BottomItem(R.drawable.queue_music, stringResource(R.string.pulse_player_queue), false, true, a.onQueue)
    BottomButton.LYRICS -> BottomItem(R.drawable.lyrics, stringResource(R.string.pulse_player_lyrics), a.lyricsShown, true, a.onLyrics)
    BottomButton.TIMER -> BottomItem(
        R.drawable.bedtime,
        a.sleepTimerLeft ?: stringResource(R.string.pulse_player_timer),
        a.sleepTimerLeft != null,
        a.controlsEnabled,
        a.onSleepTimer,
    )
    BottomButton.SHARE -> BottomItem(R.drawable.share, stringResource(R.string.pulse_player_share), false, true, a.onShare)
    BottomButton.SHUFFLE -> BottomItem(R.drawable.shuffle, stringResource(R.string.pulse_player_shuffle), a.shuffleOn, a.controlsEnabled, a.onShuffle)
    BottomButton.REPEAT -> BottomItem(
        repeatIcon(a.repeatMode),
        stringResource(R.string.pulse_player_repeat),
        a.repeatMode != Player.REPEAT_MODE_OFF,
        a.controlsEnabled,
        a.onRepeat,
    )
}

@Composable
internal fun PulseBottomRow(
    theme: PlayerTheme,
    actions: PulseBottomActions,
    modifier: Modifier = Modifier,
) {
    val p = pulsePalette(theme)
    val items = theme.buttons.map { bottomItem(it, actions) }
    if (items.isEmpty()) return
    val side = theme.artMargin.coerceIn(20, 40).dp
    when (theme.bottomStyle) {
        PlayerTheme.BottomStyle.PANEL -> Box(modifier.padding(start = side, end = side, top = 8.dp, bottom = 16.dp)) {
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
                items.forEachIndexed { index, item ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(p.button),
                        )
                    }
                    PanelCell(item, p)
                }
            }
        }
        PlayerTheme.BottomStyle.QUIET -> Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .height(76.dp)
                .padding(horizontal = side, vertical = 8.dp),
        ) {
            items.forEach { QuietButton(it, p) }
        }
        PlayerTheme.BottomStyle.ICONS -> Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.padding(horizontal = side, vertical = 12.dp),
        ) {
            items.forEach { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PulseIconButton(item.icon, item.label, if (item.active) p.accent else p.content.copy(alpha = 0.7f), item.onClick, iconSize = 22.dp, enabled = item.enabled)
                }
            }
        }
    }
}

@Composable
private fun RowScope.PanelCell(item: BottomItem, p: PulsePalette) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(enabled = item.enabled, onClick = item.onClick)
            .alpha(if (item.enabled) 1f else 0.4f),
    ) {
        PulseIcon(item.icon, if (item.active) p.accent else p.content.copy(alpha = 0.85f), 20.dp)
        Spacer(Modifier.height(5.dp))
        Text(
            text = item.label.uppercase(),
            style = monoStyle(10, 1.0),
            color = if (item.active) p.accent else p.muted,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

/** An icon with a small caption, tinted when it is on. */
@Composable
private fun RowScope.QuietButton(item: BottomItem, p: PulsePalette) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(if (item.active) p.tileActive else Color.Transparent)
            .clickable(enabled = item.enabled, onClick = item.onClick)
            .alpha(if (item.enabled) 1f else 0.4f),
    ) {
        PulseIcon(item.icon, if (item.active) p.accent else p.content.copy(alpha = 0.7f), 22.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (item.active) p.accent else p.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    enabled: Boolean = true,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
    ) {
        PulseIcon(icon, tint, iconSize, description)
    }
}

/** A button that sinks a little under the finger; the transport buttons are pressed a lot. */
@Composable
private fun PressableBox(
    width: Dp,
    height: Dp,
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
            .size(width, height)
            .scale(scale)
            .then(if (glow != null) Modifier.shadow(14.dp, shape, ambientColor = glow, spotColor = glow) else Modifier)
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
