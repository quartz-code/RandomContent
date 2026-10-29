package app.pulse.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import app.pulse.desktop.res.Res
import app.pulse.desktop.res.close
import app.pulse.desktop.res.download
import coil3.compose.AsyncImage
import com.metrolist.music.pulse.recap.PartOfDay
import com.metrolist.music.pulse.recap.RecapEntry
import com.metrolist.music.pulse.recap.RecapPeriod
import com.metrolist.music.ui.player.PlayerTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/** The list of recaps: years first, then their months, as on the phone. */
@Composable
fun RecapsDialog(entries: List<RecapEntry>, onOpen: (RecapPeriod) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.recaps) },
        text = {
            Column(Modifier.widthIn(min = 420.dp)) {
                Text(if (entries.isEmpty()) Strings.recapsEmpty else Strings.recapsIntro, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.height(if (entries.isEmpty()) 0.dp else 360.dp)) {
                    items(entries, key = { it.period.key }) { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(if (entry.period.isYear) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent)
                                .clickable { onOpen(entry.period) }
                                .padding(horizontal = 12.dp, vertical = if (entry.period.isYear) 14.dp else 10.dp),
                        ) {
                            Text(
                                Strings.periodTitle(entry.period.year, entry.period.month),
                                style = if (entry.period.isYear) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Text(Strings.hoursMinutes(entry.totalMs), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(Strings.close) } },
    )
}

private enum class Page { INTRO, MINUTES, SONG, ARTIST, TIME, MONTHS, HABITS, CARD }

private fun pages(recap: Recaps.Recap): List<Page> = buildList {
    add(Page.INTRO)
    add(Page.MINUTES)
    if (recap.songs.isNotEmpty()) add(Page.SONG)
    if (recap.artists.isNotEmpty()) add(Page.ARTIST)
    add(Page.TIME)
    if (recap.period.isYear) add(Page.MONTHS)
    add(Page.HABITS)
    add(Page.CARD)
}

/**
 * A recap as stories over the whole window, drawn in the player theme like the phone's. A click
 * on the right or → goes on, on the left or ← goes back, Esc closes.
 */
@Composable
fun RecapStories(recap: Recaps.Recap, theme: PlayerTheme, fonts: ThemeFonts, onClose: () -> Unit) {
    val list = remember(recap) { pages(recap) }
    var page by remember(recap) { mutableStateOf(0) }
    val focus = remember { FocusRequester() }
    val artworkUrl = recap.songs.firstOrNull()?.thumbnail?.let(Track::largeThumbnail)
    val artwork = rememberArtworkColor(artworkUrl)
    val p = palette(theme, artwork)
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        Modifier.fillMaxSize().background(theme.baseBackground)
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionRight, Key.Spacebar -> { if (page < list.lastIndex) page++; true }
                    Key.DirectionLeft -> { if (page > 0) page--; true }
                    Key.Escape -> { onClose(); true }
                    else -> false
                }
            },
    ) {
        ThemeBackground(theme, artwork, artworkUrl)
        BoxWithConstraints(Modifier.fillMaxSize().pointerInput(list.size) {
            detectTapGestures { offset -> if (offset.x > size.width / 3) { if (page < list.lastIndex) page++ } else if (page > 0) page-- }
        }, contentAlignment = Alignment.TopCenter) {
            Column(Modifier.width(minOf(maxWidth, 520.dp)).fillMaxHeight().padding(horizontal = 24.dp, vertical = 20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    list.indices.forEach { i ->
                        Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(p.content.copy(alpha = if (i <= page) 0.9f else 0.25f)))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Column(Modifier.weight(1f).fillMaxWidth()) { PageContent(list[page], recap, theme, fonts, p) }
            }
        }
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
            Icon(painterResource(Res.drawable.close), Strings.close, tint = p.content)
        }
    }
}

@Composable
private fun ColumnScope.PageContent(page: Page, recap: Recaps.Recap, theme: PlayerTheme, fonts: ThemeFonts, p: Palette) {
    val n = recap.numbers
    val year = recap.period.isYear
    when (page) {
        Page.INTRO -> {
            Kicker(Strings.recaps, p)
            Big(Strings.periodTitle(recap.period.year, recap.period.month), theme, fonts, p, 52.sp)
            Spacer(Modifier.height(10.dp))
            Body(if (year) Strings.recapIntroYear else Strings.recapIntroMonth, p)
            Spacer(Modifier.weight(1f))
            CoverGrid(recap, theme, Modifier.fillMaxWidth(0.8f).align(Alignment.CenterHorizontally))
            Spacer(Modifier.weight(1f))
            Body(Strings.recapHint, p, size = 13.sp)
        }
        Page.MINUTES -> {
            Spacer(Modifier.weight(1f))
            Kicker(Strings.recapMinutesKicker, p)
            Big("%,d".format(n.minutes).replace(',', ' '), theme, fonts, p, 96.sp, accent = true)
            Body(Strings.recapMinutesWord(n.minutes), p, size = 22.sp)
            Spacer(Modifier.height(8.dp))
            Body(Strings.hoursMinutes(n.totalMs), p)
            Spacer(Modifier.height(28.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.panel).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Stat("${n.plays}", Strings.playsWord(n.plays), theme, fonts, p)
                Stat("${recap.songCount}", Strings.songsWord(recap.songCount), theme, fonts, p)
                Stat("${recap.artistCount}", Strings.artistsWord(recap.artistCount), theme, fonts, p)
            }
            Spacer(Modifier.weight(1f))
        }
        Page.SONG -> {
            val song = recap.songs.first()
            Kicker(Strings.songOf(year), p)
            Spacer(Modifier.height(16.dp))
            Artwork(song.thumbnail, theme, Modifier.fillMaxWidth(0.7f).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(18.dp))
            Big(song.title, theme, fonts, p, 30.sp, maxLines = 2)
            Body(song.artist, p)
            Spacer(Modifier.height(6.dp))
            Body("${Strings.timesCount(song.plays)} · ${Strings.minutesOf(song.ms)}", p, accent = true)
            Spacer(Modifier.height(18.dp))
            recap.songs.drop(1).forEachIndexed { i, s -> Ranked(i + 2, s.title, s.artist, s.thumbnail, p, round = false) }
        }
        Page.ARTIST -> {
            val artist = recap.artists.first()
            Kicker(Strings.artistOf(year), p)
            Spacer(Modifier.height(16.dp))
            AsyncImage(
                artist.thumbnail?.let(Track::largeThumbnail), null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth(0.6f).aspectRatio(1f).align(Alignment.CenterHorizontally).clip(CircleShape).background(p.panel),
            )
            Spacer(Modifier.height(18.dp))
            Big(artist.name, theme, fonts, p, 34.sp, maxLines = 2)
            Body(Strings.minutesOf(artist.ms), p, accent = true)
            Spacer(Modifier.height(18.dp))
            recap.artists.drop(1).forEachIndexed { i, a -> Ranked(i + 2, a.name, Strings.minutesOf(a.ms), a.thumbnail, p, round = true) }
        }
        Page.TIME -> {
            val favourite = n.favouritePart ?: PartOfDay.EVENING
            Spacer(Modifier.weight(1f))
            Kicker(Strings.recapTimeKicker, p)
            Big(Strings.partNames[favourite.ordinal], theme, fonts, p, 56.sp, accent = true)
            Body(Strings.partLines[favourite.ordinal], p)
            Spacer(Modifier.height(28.dp))
            Bars(n.byPartOfDay, Strings.partNames, p, 170.dp)
            n.busiestDay?.let { day ->
                Spacer(Modifier.height(24.dp))
                Body(Strings.busiestDay(day, n.busiestDayMs), p)
            }
            Spacer(Modifier.weight(1f))
        }
        Page.MONTHS -> {
            Spacer(Modifier.weight(1f))
            Kicker(Strings.recapMonthsKicker, p)
            n.busiestMonth?.let { Big(Strings.monthName(it), theme, fonts, p, 52.sp, accent = true) }
            Spacer(Modifier.height(28.dp))
            Bars(n.byMonth, (1..12).map { Strings.monthName(it).take(1) }, p, 200.dp)
            Spacer(Modifier.weight(1f))
        }
        Page.HABITS -> {
            Spacer(Modifier.weight(1f))
            Kicker(Strings.recapNewKicker, p)
            Big("${recap.newSongs}", theme, fonts, p, 80.sp, accent = true)
            Body(Strings.newSongsWord(recap.newSongs), p, size = 20.sp)
            Spacer(Modifier.height(36.dp))
            Kicker(Strings.recapStreakKicker, p)
            Big("${n.longestStreak}", theme, fonts, p, 64.sp)
            Body(Strings.streakWord(n.longestStreak), p, size = 20.sp)
            Spacer(Modifier.height(10.dp))
            Body(Strings.activeDays(n.activeDays), p)
            Spacer(Modifier.weight(1f))
        }
        Page.CARD -> CardPage(recap, theme, fonts, p)
    }
}

/** The summary as a card, and the button that saves it as a picture to send anywhere. */
@Composable
private fun ColumnScope.CardPage(recap: Recaps.Recap, theme: PlayerTheme, fonts: ThemeFonts, p: Palette) {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<String?>(null) }
    Kicker(Strings.recapCardKicker, p)
    Spacer(Modifier.height(12.dp))
    // The card is laid out at the size it is saved in, then scaled down to fit: what is shown is the picture.
    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        val scale = minOf(maxWidth / CARD_WIDTH, maxHeight / CARD_HEIGHT, 1f)
        Box(Modifier.size(CARD_WIDTH * scale, CARD_HEIGHT * scale)) {
            Box(
                Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(CARD_WIDTH, CARD_HEIGHT).graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                },
            ) { RecapCard(recap, theme, fonts) }
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Button(onClick = {
            scope.launch {
                val saved = RecapImage.save(recap, theme, fonts)
                result = when {
                    saved == null -> null
                    saved.isSuccess -> Strings.savedTo(saved.getOrThrow().absolutePath)
                    else -> Strings.saveFailed
                }
            }
        }) {
            Icon(painterResource(Res.drawable.download), null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(Strings.saveImage)
        }
    }
    result?.let {
        Spacer(Modifier.height(8.dp))
        Body(it, p, size = 13.sp)
    }
}

/** The card itself: everything important on one picture, sized for a chat or a story. */
@Composable
fun RecapCard(recap: Recaps.Recap, theme: PlayerTheme, fonts: ThemeFonts) {
    val artworkUrl = recap.songs.firstOrNull()?.thumbnail?.let(Track::largeThumbnail)
    val artwork = rememberArtworkColor(artworkUrl)
    val p = palette(theme, artwork)
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(theme.baseBackground)) {
        ThemeBackground(theme, artwork, artworkUrl)
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Text(Strings.recapCardBrand.uppercase(), color = p.muted, fontSize = 11.sp, letterSpacing = 2.sp)
            Text(Strings.periodTitle(recap.period.year, recap.period.month), style = theme.headline(30.sp, fonts.oswald, fonts.mono), color = p.content)
            Spacer(Modifier.height(12.dp))
            CoverGrid(recap, theme, Modifier.fillMaxWidth(0.62f).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%,d".format(recap.numbers.minutes).replace(',', ' '), style = theme.headline(40.sp, fonts.oswald, fonts.mono), color = p.accent)
                Spacer(Modifier.width(8.dp))
                Text(Strings.recapMinutesWord(recap.numbers.minutes), color = p.content, fontSize = 15.sp, modifier = Modifier.padding(bottom = 7.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(Strings.recapCardSongs, color = p.muted, fontSize = 12.sp)
                    recap.songs.take(5).forEachIndexed { i, s -> Text("${i + 1}. ${s.title}", color = p.content, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Column(Modifier.weight(1f)) {
                    Text(Strings.recapCardArtists, color = p.muted, fontSize = 12.sp)
                    recap.artists.take(5).forEachIndexed { i, a -> Text("${i + 1}. ${a.name}", color = p.content, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${Strings.partNames[(recap.numbers.favouritePart ?: PartOfDay.EVENING).ordinal]} · ${Strings.streakWord(recap.numbers.longestStreak).let { "${recap.numbers.longestStreak} $it" }} · Pulse",
                color = p.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 4:5, what chats and stories show without cropping; saved at twice this (1080×1350). */
val CARD_WIDTH = 540.dp
val CARD_HEIGHT = 675.dp

@Composable
private fun CoverGrid(recap: Recaps.Recap, theme: PlayerTheme, modifier: Modifier) {
    val covers = recap.songs.mapNotNull { it.thumbnail }.take(4)
    if (covers.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        covers.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { Artwork(it, theme, Modifier.weight(1f)) }
                if (row.size == 1 && covers.size > 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Artwork(url: String?, theme: PlayerTheme, modifier: Modifier) {
    AsyncImage(
        url?.let(Track::largeThumbnail), null, contentScale = ContentScale.Crop,
        modifier = modifier.aspectRatio(1f).clip(theme.artShapeFor(320.dp)).background(Color.White.copy(alpha = 0.08f)),
    )
}

@Composable
private fun Ranked(rank: Int, title: String, subtitle: String, url: String?, p: Palette, round: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$rank", color = p.muted, fontSize = 15.sp, modifier = Modifier.width(26.dp))
        AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(if (round) CircleShape else RoundedCornerShape(6.dp)).background(p.panel))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = p.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = p.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Stat(value: String, label: String, theme: PlayerTheme, fonts: ThemeFonts, p: Palette) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(vertical = 6.dp)) {
        Text(value, style = theme.headline(28.sp, fonts.oswald, fonts.mono), color = p.accent, modifier = Modifier.width(72.dp))
        Text(label, color = p.content, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
    }
}

@Composable
private fun Bars(values: List<Long>, labels: List<String>, p: Palette, height: Dp) {
    val top = values.maxOrNull()?.takeIf { it > 0 } ?: 1L
    Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                val fraction = (v.toFloat() / top).coerceAtLeast(0.03f)
                if (fraction < 1f) Spacer(Modifier.weight(1f - fraction))
                Box(Modifier.fillMaxWidth().weight(fraction, fill = true).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(if (v == top) p.accent else p.accent.copy(alpha = 0.35f)))
                Text(labels.getOrElse(i) { "" }, color = p.muted, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun Kicker(text: String, p: Palette) {
    Text(text.uppercase(), color = p.muted, fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun Big(text: String, theme: PlayerTheme, fonts: ThemeFonts, p: Palette, size: TextUnit, accent: Boolean = false, maxLines: Int = 3) {
    Text(text, style = theme.headline(size, fonts.oswald, fonts.mono), color = if (accent) p.accent else p.content, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun Body(text: String, p: Palette, size: TextUnit = 16.sp, accent: Boolean = false) {
    Text(text, color = if (accent) p.accent else p.content.copy(alpha = 0.85f), fontSize = size, textAlign = TextAlign.Start)
}
