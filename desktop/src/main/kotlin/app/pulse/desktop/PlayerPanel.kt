package app.pulse.desktop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pulse.desktop.res.Res
import app.pulse.desktop.res.favorite
import app.pulse.desktop.res.favorite_border
import app.pulse.desktop.res.music_note
import app.pulse.desktop.res.pulse_pause
import app.pulse.desktop.res.pulse_play
import app.pulse.desktop.res.pulse_skip_next
import app.pulse.desktop.res.pulse_skip_previous
import app.pulse.desktop.res.repeat
import app.pulse.desktop.res.repeat_on
import app.pulse.desktop.res.repeat_one_on
import app.pulse.desktop.res.shuffle
import app.pulse.desktop.res.shuffle_on
import app.pulse.desktop.res.volume_off
import app.pulse.desktop.res.volume_up
import coil3.compose.AsyncImage
import com.metrolist.music.ui.player.PlayerTheme
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.sin

/** The player on the right of the window, drawn from the phone's [PlayerTheme]. */
@Composable
fun PlayerPanel(
    theme: PlayerTheme,
    state: PlayerState,
    controller: PlayerController,
    fonts: ThemeFonts,
    modifier: Modifier = Modifier,
    /** Null when signed out: there is nothing to like into. */
    liked: Boolean? = null,
    onLike: () -> Unit = {},
) {
    val track = state.current
    val artworkUrl = track?.artwork
    val artwork = rememberArtworkColor(artworkUrl)
    val p = palette(theme, artwork)
    // Clipped: the flowing colours are drawn larger than the panel.
    Box(modifier.clipToBounds().background(theme.baseBackground)) {
        ThemeBackground(theme, artwork, artworkUrl)
        ThemeMotion(theme, p.accent, artwork, controller.levels, state.isPlaying)
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp)) {
            Header(theme, p, fonts)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight) * (1f - (theme.artMargin - 8) / 112f)
                Artwork(theme, p, artworkUrl, state.isPlaying, Modifier.size(side))
            }
            Spacer(Modifier.height(16.dp))
            TrackInfo(theme, p, fonts, track, liked, onLike)
            Spacer(Modifier.height(14.dp))
            Progress(theme, p, fonts, state, controller::seekTo)
            Spacer(Modifier.height(10.dp))
            // A guest of a room hears the host's choices: the buttons stay, faded, and do nothing.
            Box(Modifier.alpha(if (state.followingHost != null) 0.35f else 1f)) { Transport(theme, p, state, controller) }
            state.followingHost?.let { host ->
                Text(Strings.hostControls(host), color = p.muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
            Spacer(Modifier.height(12.dp))
            VolumeRow(p, state.volume, controller::setVolume)
            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = p.like, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

class ThemeFonts(val oswald: FontFamily, val mono: FontFamily)

@Composable
private fun Header(theme: PlayerTheme, p: Palette, fonts: ThemeFonts) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        if (theme.onAirHeader) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFE0482F)))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            (if (theme.onAirHeader) Strings.onAir else Strings.nowPlaying).uppercase(),
            style = TextStyle(fontFamily = fonts.mono, fontSize = 11.sp, letterSpacing = 2.sp),
            color = if (theme.onAirHeader) p.accent else p.muted,
        )
    }
}

@Composable
private fun Artwork(theme: PlayerTheme, p: Palette, url: String?, playing: Boolean, modifier: Modifier) {
    val rotation = remember { Animatable(0f) }
    val spinning = theme.artSpin && theme.canSpin && playing
    LaunchedEffect(spinning) {
        while (spinning) {
            rotation.animateTo(rotation.value + 360f, tween(18_000, easing = LinearEasing))
            rotation.snapTo(rotation.value % 360f)
        }
    }
    BoxWithConstraints(modifier) {
        val shape = theme.artShapeFor(maxWidth)
        val base = Modifier
            .fillMaxSize()
            .rotate(rotation.value)
            .then(if (theme.artShadow) Modifier.shadow(24.dp, shape) else Modifier)
            .clip(shape)
            .background(p.panel)
            .then(if (theme.artBorder > 0) Modifier.border(BorderStroke(theme.artBorder.dp, p.accent), shape) else Modifier)
        if (url != null) {
            AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = base)
        } else {
            Box(base, contentAlignment = Alignment.Center) {
                Icon(painterResource(Res.drawable.music_note), null, tint = p.muted, modifier = Modifier.size(64.dp))
            }
        }
    }
}

@Composable
private fun TrackInfo(theme: PlayerTheme, p: Palette, fonts: ThemeFonts, track: Track?, liked: Boolean?, onLike: () -> Unit) {
    val align = if (theme.titleCentered) TextAlign.Center else TextAlign.Start
    Row(verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
    Text(
        track?.title ?: Strings.nothingPlaying,
        style = theme.headline(theme.titleSize.sp, fonts.oswald, fonts.mono),
        color = p.content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        modifier = Modifier.fillMaxWidth(),
    )
    if (track != null) {
        Text(
            track.artists,
            style = if (theme.font == PlayerTheme.Font.MONO) TextStyle(fontFamily = fonts.mono, fontSize = 14.sp) else TextStyle(fontSize = 15.sp),
            color = if (theme.font == PlayerTheme.Font.SYSTEM) p.muted else p.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = align,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        )
    }
    }
    if (track != null && liked != null) {
        IconTap(painterResource(if (liked) Res.drawable.favorite else Res.drawable.favorite_border), Strings.like, if (liked) p.like else p.muted, onLike)
    }
    }
}

@Composable
private fun Progress(theme: PlayerTheme, p: Palette, fonts: ThemeFonts, state: PlayerState, onSeek: (Long) -> Unit) {
    state.loading?.let { progress ->
        LinearProgressIndicator(progress = { progress }, color = p.accent, trackColor = p.track, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
        Text(Strings.loading, color = p.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        return
    }
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(1)
    val fraction = dragging ?: (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    val height = when (theme.slider) {
        PlayerTheme.Slider.THICK -> 14.dp
        PlayerTheme.Slider.DIAL -> 30.dp
        PlayerTheme.Slider.WAVE -> 22.dp
        PlayerTheme.Slider.THIN -> 18.dp
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(duration) {
                detectTapGestures { onSeek((it.x / size.width * duration).toLong()) }
            }
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = (it.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { dragging?.let { f -> onSeek((f * duration).toLong()) }; dragging = null },
                    onDragCancel = { dragging = null },
                ) { change, _ -> dragging = (change.position.x / size.width).coerceIn(0f, 1f) }
            },
    ) {
        val cy = size.height / 2
        val x = size.width * fraction
        when (theme.slider) {
            PlayerTheme.Slider.THIN -> {
                drawLine(p.track, Offset(0f, cy), Offset(size.width, cy), 4f, StrokeCap.Round)
                drawLine(p.accent, Offset(0f, cy), Offset(x, cy), 4f, StrokeCap.Round)
                drawCircle(p.accent, 7.dp.toPx(), Offset(x, cy))
            }
            PlayerTheme.Slider.THICK -> {
                val r = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(p.track, cornerRadius = r)
                drawRoundRect(p.accent, size = Size(x.coerceAtLeast(size.height), size.height), cornerRadius = r)
            }
            PlayerTheme.Slider.WAVE -> {
                val amp = size.height * 0.28f
                val path = Path()
                val steps = 120
                for (s in 0..steps) {
                    val px = size.width * s / steps
                    val py = cy + amp * sin(px / 14.dp.toPx() * PI.toFloat())
                    if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                drawPath(path, p.track, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                clipRectSafe(x) { drawPath(path, p.accent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)) }
                drawCircle(p.accent, 6.dp.toPx(), Offset(x, cy + amp * sin(x / 14.dp.toPx() * PI.toFloat())))
            }
            PlayerTheme.Slider.DIAL -> {
                // A tuning scale with a red needle, as in the "On air" design.
                val ticks = 60
                for (i in 0..ticks) {
                    val tx = size.width * i / ticks
                    val tall = i % 5 == 0
                    val color = if (tx <= x) p.accent else p.track
                    drawLine(color, Offset(tx, cy - (if (tall) 10 else 5).dp.toPx()), Offset(tx, cy + (if (tall) 10 else 5).dp.toPx()), 2f)
                }
                drawLine(Color(0xFFE0482F), Offset(x, 0f), Offset(x, size.height), 3.dp.toPx(), StrokeCap.Round)
            }
        }
    }
    if (theme.showTime) {
        val timeStyle = if (theme.font == PlayerTheme.Font.SYSTEM) TextStyle(fontSize = 12.sp) else TextStyle(fontFamily = fonts.mono, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(clock(((dragging ?: fraction) * duration).toLong()), style = timeStyle, color = p.muted)
            Spacer(Modifier.weight(1f))
            Text(clock(state.durationMs), style = timeStyle, color = p.muted)
        }
    }
}

private inline fun androidx.compose.ui.graphics.drawscope.DrawScope.clipRectSafe(right: Float, block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    drawContext.canvas.save()
    drawContext.canvas.clipRect(0f, 0f, right.coerceAtLeast(0f), size.height)
    block()
    drawContext.canvas.restore()
}

private fun clock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun Transport(theme: PlayerTheme, p: Palette, state: PlayerState, controller: PlayerController) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
        if (theme.showShuffleRepeat) {
            IconTap(painterResource(if (state.shuffle) Res.drawable.shuffle_on else Res.drawable.shuffle), Strings.shuffle, if (state.shuffle) p.accent else p.muted, controller::toggleShuffle)
        }
        IconTap(painterResource(Res.drawable.pulse_skip_previous), Strings.previous, p.content, controller::previous, size = 34)
        PlayButton(theme, p, state.isPlaying, controller::togglePlay)
        IconTap(painterResource(Res.drawable.pulse_skip_next), Strings.next, p.content, controller::next, size = 34)
        if (theme.showShuffleRepeat) {
            val icon = when (state.repeat) {
                RepeatMode.OFF -> Res.drawable.repeat
                RepeatMode.ALL -> Res.drawable.repeat_on
                RepeatMode.ONE -> Res.drawable.repeat_one_on
            }
            IconTap(painterResource(icon), Strings.repeat, if (state.repeat == RepeatMode.OFF) p.muted else p.accent, controller::cycleRepeat)
        }
    }
}

@Composable
private fun PlayButton(theme: PlayerTheme, p: Palette, playing: Boolean, onClick: () -> Unit) {
    val icon = painterResource(if (playing) Res.drawable.pulse_pause else Res.drawable.pulse_play)
    val label = if (playing) Strings.pause else Strings.play
    when (theme.playButton) {
        PlayerTheme.PlayButton.PILL -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(p.accent).clickable(onClick = onClick).padding(horizontal = 26.dp, vertical = 14.dp),
        ) {
            Icon(icon, label, tint = p.onPlay, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = p.onPlay, fontWeight = FontWeight.SemiBold)
        }
        PlayerTheme.PlayButton.KNOB -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(78.dp).clip(CircleShape).border(2.dp, p.accent.copy(alpha = 0.35f), CircleShape).padding(7.dp).clip(CircleShape).background(p.accent).clickable(onClick = onClick),
        ) { Icon(icon, label, tint = p.onPlay, modifier = Modifier.size(32.dp)) }
        PlayerTheme.PlayButton.SQUIRCLE -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(68.dp).clip(RoundedCornerShape(22.dp)).background(p.accent).clickable(onClick = onClick),
        ) { Icon(icon, label, tint = p.onPlay, modifier = Modifier.size(32.dp)) }
        PlayerTheme.PlayButton.CIRCLE -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(68.dp).clip(CircleShape).background(p.accent).clickable(onClick = onClick),
        ) { Icon(icon, label, tint = p.onPlay, modifier = Modifier.size(32.dp)) }
    }
}

@Composable
private fun IconTap(icon: Painter, label: String, tint: Color, onClick: () -> Unit, size: Int = 26) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size((size + 18).dp).clip(CircleShape).clickable(onClick = onClick)) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(size.dp))
    }
}

@Composable
private fun VolumeRow(p: Palette, volume: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(painterResource(if (volume <= 0.001f) Res.drawable.volume_off else Res.drawable.volume_up), Strings.volume, tint = p.muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Slider(
            value = volume,
            onValueChange = onChange,
            colors = SliderDefaults.colors(thumbColor = p.content, activeTrackColor = p.content.copy(alpha = 0.7f), inactiveTrackColor = p.track),
            modifier = Modifier.weight(1f),
        )
    }
}
