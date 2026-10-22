package com.metrolist.music.ui.screens.recap

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.pulse.recap.PartOfDay
import com.metrolist.music.pulse.recap.Recap
import com.metrolist.music.pulse.recap.RecapPeriod
import com.metrolist.music.pulse.recap.RecapRepository
import com.metrolist.music.ui.player.LocalPulseArtworkColor
import com.metrolist.music.ui.player.PlayerTheme
import com.metrolist.music.ui.player.PulseMotionLayer
import com.metrolist.music.ui.player.PulsePalette
import com.metrolist.music.ui.player.PulsePlayerBackground
import com.metrolist.music.ui.player.baseBackground
import com.metrolist.music.ui.player.pulseArtBorder
import com.metrolist.music.ui.player.pulseArtShape
import com.metrolist.music.ui.player.pulsePalette
import com.metrolist.music.ui.player.rememberArtworkColors
import com.metrolist.music.ui.player.rememberPlayerTheme
import com.metrolist.music.ui.theme.Oswald
import com.metrolist.music.ui.theme.PtMono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

/**
 * Pulse: a month's or a year's recap as stories, drawn in the player theme (its background,
 * fonts, artwork shape and accent), with the top song's artwork colours. The last page is a card
 * to share as a picture.
 */
@Composable
fun RecapScreen(navController: NavController, key: String) {
    val period = remember(key) { RecapPeriod.parse(key) }
    val database = LocalDatabase.current
    val recap by produceState<Recap?>(null, period) {
        value = period?.let { runCatching { RecapRepository.load(database, it) }.getOrNull() }
    }
    val theme = rememberPlayerTheme()
    val loaded = recap
    Box(Modifier.fillMaxSize().background(theme.baseBackground)) {
        if (loaded == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            RecapStories(theme, loaded, onClose = navController::navigateUp)
        }
    }
}

@Composable
internal fun RecapStories(theme: PlayerTheme, recap: Recap, onClose: () -> Unit, initialPage: Int = 0) {
    val top = recap.songs.firstOrNull()
    val colors = rememberArtworkColors(top?.thumbnailUrl)
    val pages = remember(recap) { recapPages(recap) }
    val pager = rememberPagerState(initialPage) { pages.size }
    val scope = rememberCoroutineScope()
    CompositionLocalProvider(LocalPulseArtworkColor provides colors.firstOrNull()) {
        val palette = pulsePalette(theme)
        Box(Modifier.fillMaxSize()) {
            PulsePlayerBackground(theme, colors, top?.thumbnailUrl)
            if (theme.motion == PlayerTheme.Motion.FLOW) {
                PulseMotionLayer(theme, colors, palette.accent, { FloatArray(0) }, active = true, modifier = Modifier.fillMaxSize())
            }
            HorizontalPager(
                state = pager,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pages.size) {
                        // Like stories: a tap on the right goes on, on the left goes back.
                        detectTapGestures { offset ->
                            val next = if (offset.x > size.width / 3) pager.currentPage + 1 else pager.currentPage - 1
                            if (next in pages.indices) scope.launch { pager.animateScrollToPage(next) }
                        }
                    },
            ) { index ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom))
                        .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 16.dp),
                ) {
                    RecapPageContent(pages[index], recap, theme, palette)
                }
            }
            Column(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top))) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    pages.indices.forEach { i ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(palette.content.copy(alpha = if (i <= pager.currentPage) 0.9f else 0.25f)),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(R.drawable.close), contentDescription = stringResource(R.string.close), tint = palette.content)
                    }
                }
            }
        }
    }
}

internal enum class RecapPage { INTRO, MINUTES, SONG, ARTIST, ALBUM, TIME, MONTHS, HABITS, CARD }

internal fun recapPages(recap: Recap): List<RecapPage> = buildList {
    add(RecapPage.INTRO)
    add(RecapPage.MINUTES)
    if (recap.songs.isNotEmpty()) add(RecapPage.SONG)
    if (recap.artists.isNotEmpty()) add(RecapPage.ARTIST)
    if (recap.album != null) add(RecapPage.ALBUM)
    add(RecapPage.TIME)
    if (recap.period.isYear) add(RecapPage.MONTHS)
    add(RecapPage.HABITS)
    add(RecapPage.CARD)
}

// ---------------------------------------------------------------- pages

@Composable
private fun ColumnScope.RecapPageContent(page: RecapPage, recap: Recap, theme: PlayerTheme, p: PulsePalette) {
    when (page) {
        RecapPage.INTRO -> {
            Kicker(stringResource(R.string.recap_kicker), p)
            Big(periodTitle(recap.period), theme, p, 52.sp)
            Spacer(Modifier.height(10.dp))
            Body(stringResource(if (recap.period.isYear) R.string.recap_intro_year else R.string.recap_intro_month), p)
            Spacer(Modifier.weight(1f))
            CoverGrid(recap, theme)
            Spacer(Modifier.weight(1f))
            Hint(stringResource(R.string.recap_tap_hint), p)
        }
        RecapPage.MINUTES -> {
            Spacer(Modifier.weight(1f))
            Kicker(stringResource(R.string.recap_minutes_kicker), p)
            Big(formatNumber(recap.numbers.minutes), theme, p, 96.sp, accent = true)
            Body(pluralStringResource(R.plurals.recap_minutes_word, recap.numbers.minutes.toPluralCount()), p, size = 22.sp)
            Spacer(Modifier.height(8.dp))
            Body(hoursText(recap.numbers.totalMs), p)
            Spacer(Modifier.height(28.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.panel)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Stat(formatNumber(recap.numbers.plays.toLong()), pluralStringResource(R.plurals.recap_plays_word, recap.numbers.plays), theme, p)
                Stat(formatNumber(recap.songCount.toLong()), pluralStringResource(R.plurals.recap_songs_word, recap.songCount), theme, p)
                Stat(formatNumber(recap.artistCount.toLong()), pluralStringResource(R.plurals.recap_artists_word, recap.artistCount), theme, p)
            }
            Spacer(Modifier.weight(1f))
        }
        RecapPage.SONG -> {
            val song = recap.songs.first()
            Kicker(stringResource(if (recap.period.isYear) R.string.recap_song_year else R.string.recap_song_month), p)
            Spacer(Modifier.height(16.dp))
            Artwork(song.thumbnailUrl, theme, Modifier.fillMaxWidth(0.8f).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(18.dp))
            Big(song.title, theme, p, 30.sp, maxLines = 2)
            Body(song.artist, p)
            Spacer(Modifier.height(6.dp))
            Body(
                pluralStringResource(R.plurals.recap_times, song.plays, song.plays) + " · " + minutesText(song.ms),
                p,
                accent = true,
            )
            Spacer(Modifier.height(18.dp))
            recap.songs.drop(1).forEachIndexed { i, s -> Ranked(i + 2, s.title, s.artist, s.thumbnailUrl, theme, p, round = false) }
        }
        RecapPage.ARTIST -> {
            val artist = recap.artists.first()
            Kicker(stringResource(if (recap.period.isYear) R.string.recap_artist_year else R.string.recap_artist_month), p)
            Spacer(Modifier.height(16.dp))
            AsyncImage(
                model = artist.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(p.panel),
            )
            Spacer(Modifier.height(18.dp))
            Big(artist.name, theme, p, 34.sp, maxLines = 2)
            Body(minutesText(artist.ms), p, accent = true)
            Spacer(Modifier.height(18.dp))
            recap.artists.drop(1).forEachIndexed { i, a -> Ranked(i + 2, a.name, minutesText(a.ms), a.thumbnailUrl, theme, p, round = true) }
        }
        RecapPage.ALBUM -> {
            val album = recap.album!!
            Spacer(Modifier.weight(1f))
            Kicker(stringResource(R.string.recap_album), p)
            Spacer(Modifier.height(16.dp))
            Artwork(album.thumbnailUrl, theme, Modifier.fillMaxWidth(0.85f).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(18.dp))
            Big(album.title, theme, p, 30.sp, maxLines = 2)
            Body(album.artist, p)
            Body(minutesText(album.ms), p, accent = true)
            Spacer(Modifier.weight(1f))
        }
        RecapPage.TIME -> {
            val favourite = recap.numbers.favouritePart ?: PartOfDay.EVENING
            Spacer(Modifier.weight(1f))
            Kicker(stringResource(R.string.recap_time_kicker), p)
            Big(stringResource(partName(favourite)), theme, p, 56.sp, accent = true)
            Body(stringResource(partLine(favourite)), p)
            Spacer(Modifier.height(28.dp))
            Bars(
                values = recap.numbers.byPartOfDay,
                labels = PartOfDay.entries.map { stringResource(partShort(it)) },
                p = p,
                height = 170.dp,
            )
            recap.numbers.busiestDay?.let { day ->
                Spacer(Modifier.height(24.dp))
                Body(stringResource(R.string.recap_busiest_day, dayText(day), hoursText(recap.numbers.busiestDayMs)), p)
            }
            Spacer(Modifier.weight(1f))
        }
        RecapPage.MONTHS -> {
            Spacer(Modifier.weight(1f))
            Kicker(stringResource(R.string.recap_months_kicker), p)
            val best = recap.numbers.busiestMonth
            if (best != null) Big(monthName(best, standalone = true), theme, p, 52.sp, accent = true)
            Spacer(Modifier.height(28.dp))
            Bars(
                values = recap.numbers.byMonth,
                labels = (1..12).map { monthName(it, standalone = true).take(1) },
                p = p,
                height = 200.dp,
            )
            Spacer(Modifier.weight(1f))
        }
        RecapPage.HABITS -> {
            Spacer(Modifier.weight(1f))
            Kicker(stringResource(R.string.recap_new_kicker), p)
            Big(formatNumber(recap.newSongs.toLong()), theme, p, 80.sp, accent = true)
            Body(pluralStringResource(R.plurals.recap_new_songs, recap.newSongs), p, size = 20.sp)
            Spacer(Modifier.height(36.dp))
            Kicker(stringResource(R.string.recap_streak_kicker), p)
            Big(formatNumber(recap.numbers.longestStreak.toLong()), theme, p, 64.sp)
            Body(pluralStringResource(R.plurals.recap_streak_days, recap.numbers.longestStreak), p, size = 20.sp)
            Spacer(Modifier.height(10.dp))
            Body(pluralStringResource(R.plurals.recap_active_days, recap.numbers.activeDays, recap.numbers.activeDays), p)
            Spacer(Modifier.weight(1f))
        }
        RecapPage.CARD -> ShareablePage(recap, theme, p)
    }
}

/** The summary as a card, and the button that sends it as a picture. */
@Composable
private fun ColumnScope.ShareablePage(recap: Recap, theme: PlayerTheme, p: PulsePalette) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    Kicker(stringResource(R.string.recap_card_kicker), p)
    Spacer(Modifier.height(12.dp))
    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .aspectRatio(9f / 14f, matchHeightConstraintsFirst = true)
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
        ) {
            RecapCard(recap, theme)
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { scope.launch { shareCard(context, layer, recap.period) } },
        colors = ButtonDefaults.buttonColors(containerColor = p.play, contentColor = p.onPlay),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(painterResource(R.drawable.share), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.recap_share))
    }
}

/** The card itself: everything important on one picture, sized to look right in a chat or a story. */
@Composable
internal fun RecapCard(recap: Recap, theme: PlayerTheme) {
    val top = recap.songs.firstOrNull()
    val colors = rememberArtworkColors(top?.thumbnailUrl)
    CompositionLocalProvider(LocalPulseArtworkColor provides colors.firstOrNull()) {
        val p = pulsePalette(theme)
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(theme.baseBackground),
        ) {
            PulsePlayerBackground(theme, colors, top?.thumbnailUrl)
            // Everything is measured from the card's width, so the picture looks the same on every phone.
            val unit = maxWidth / 320
            Column(Modifier.fillMaxSize().padding(unit * 20)) {
                Text(
                    stringResource(R.string.recap_card_brand).uppercase(),
                    style = TextStyle(fontFamily = PtMono, fontSize = (11 * unit.value).sp, letterSpacing = (2 * unit.value).sp),
                    color = p.accent,
                )
                Text(
                    periodTitle(recap.period),
                    style = headline(theme, (30 * unit.value).sp),
                    color = p.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(unit * 14))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Artwork(top?.thumbnailUrl, theme, Modifier.size(unit * 96))
                    Spacer(Modifier.width(unit * 14))
                    Column {
                        Text(formatNumber(recap.numbers.minutes), style = headline(theme, (40 * unit.value).sp), color = p.content)
                        Text(
                            pluralStringResource(R.plurals.recap_minutes_word, recap.numbers.minutes.toPluralCount()),
                            fontSize = (14 * unit.value).sp,
                            color = p.muted,
                        )
                    }
                }
                Spacer(Modifier.height(unit * 16))
                CardLabel(stringResource(R.string.recap_card_songs), unit.value, p)
                recap.songs.take(5).forEachIndexed { i, s -> CardLine(i + 1, s.title, s.artist, unit.value, p) }
                Spacer(Modifier.height(unit * 12))
                CardLabel(stringResource(R.string.recap_card_artists), unit.value, p)
                recap.artists.take(3).forEachIndexed { i, a -> CardLine(i + 1, a.name, null, unit.value, p) }
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(
                        R.string.recap_card_footer,
                        pluralStringResource(R.plurals.recap_count_songs, recap.songCount, recap.songCount),
                        pluralStringResource(R.plurals.recap_count_artists, recap.artistCount, recap.artistCount),
                    ),
                    fontSize = (11 * unit.value).sp,
                    color = p.muted,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- pieces

@Composable
private fun CoverGrid(recap: Recap, theme: PlayerTheme) {
    val covers = recap.songs.map { it.thumbnailUrl }.take(4)
    if (covers.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        covers.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { url -> Artwork(url, theme, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Artwork(url: String?, theme: PlayerTheme, modifier: Modifier) {
    val shape = pulseArtShape(theme)
    val border = pulseArtBorder(theme)
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.2f))
            .then(border?.let { Modifier.border(it, shape) } ?: Modifier),
    )
}

@Composable
private fun Ranked(rank: Int, title: String, subtitle: String, url: String?, theme: PlayerTheme, p: PulsePalette, round: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text("$rank", style = headline(theme, 18.sp), color = p.muted, modifier = Modifier.width(28.dp))
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(if (round) CircleShape else RoundedCornerShape(6.dp)).background(p.panel),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = p.content, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = p.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Stat(value: String, label: String, theme: PlayerTheme, p: PulsePalette) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(vertical = 6.dp)) {
        Text(value, style = headline(theme, 24.sp), color = p.content, maxLines = 1)
        Spacer(Modifier.width(10.dp))
        Text(label, color = p.muted, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 2.dp))
    }
}

@Composable
private fun Bars(values: List<Long>, labels: List<String>, p: PulsePalette, height: androidx.compose.ui.unit.Dp) {
    val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1L
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth().height(height),
    ) {
        values.forEachIndexed { i, v ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight((v.toFloat() / max).coerceAtLeast(0.03f))
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (v == max) p.accent else p.content.copy(alpha = 0.22f)),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(labels[i], color = p.muted, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Kicker(text: String, p: PulsePalette) {
    Text(
        text.uppercase(),
        style = TextStyle(fontFamily = PtMono, fontSize = 12.sp, letterSpacing = 2.sp),
        color = p.accent,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun Big(text: String, theme: PlayerTheme, p: PulsePalette, size: TextUnit, accent: Boolean = false, maxLines: Int = 3) {
    Text(text, style = headline(theme, size), color = if (accent) p.accent else p.content, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun Body(text: String, p: PulsePalette, size: TextUnit = 16.sp, accent: Boolean = false) {
    Text(text, fontSize = size, lineHeight = size * 1.3f, color = if (accent) p.accent else p.content.copy(alpha = 0.85f))
}

@Composable
private fun ColumnScope.Hint(text: String, p: PulsePalette) {
    Text(text, fontSize = 13.sp, color = p.muted, modifier = Modifier.align(Alignment.CenterHorizontally))
}

@Composable
private fun CardLabel(text: String, unit: Float, p: PulsePalette) {
    Text(
        text.uppercase(),
        style = TextStyle(fontFamily = PtMono, fontSize = (10 * unit).sp, letterSpacing = (1.5f * unit).sp),
        color = p.muted,
        modifier = Modifier.padding(bottom = (4 * unit).dp),
    )
}

@Composable
private fun CardLine(rank: Int, title: String, subtitle: String?, unit: Float, p: PulsePalette) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = (2 * unit).dp)) {
        Text("$rank", fontSize = (13 * unit).sp, color = p.accent, fontWeight = FontWeight.Bold, modifier = Modifier.width((18 * unit).dp))
        Text(
            if (subtitle.isNullOrBlank()) title else "$title — $subtitle",
            fontSize = (13 * unit).sp,
            color = p.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun headline(theme: PlayerTheme, size: TextUnit): TextStyle = when (theme.font) {
    PlayerTheme.Font.SYSTEM -> TextStyle(fontWeight = FontWeight.Bold, fontSize = size, lineHeight = size * 1.1f)
    PlayerTheme.Font.OSWALD -> TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Medium, fontSize = size, lineHeight = size * 1.15f)
    PlayerTheme.Font.MONO -> TextStyle(fontFamily = PtMono, fontSize = size, lineHeight = size * 1.15f)
}

// ---------------------------------------------------------------- words and numbers

@Composable
internal fun periodTitle(period: RecapPeriod): String =
    if (period.month == null) stringResource(R.string.recap_year_title, period.year)
    else stringResource(R.string.recap_month_title, monthName(period.month, standalone = true), period.year)

internal fun monthName(month: Int, standalone: Boolean): String =
    Month.of(month).getDisplayName(if (standalone) DateTextStyle.FULL_STANDALONE else DateTextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

private fun dayText(day: LocalDate): String = day.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))

internal fun formatNumber(n: Long): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(n)

/** Plural rules only need the last digits, and pluralStringResource takes an Int. */
private fun Long.toPluralCount(): Int = (this % 1_000_000).toInt()

@Composable
private fun minutesText(ms: Long): String {
    val minutes = (ms / 60_000).toInt()
    return pluralStringResource(R.plurals.recap_minutes, minutes, minutes)
}

@Composable
internal fun hoursText(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = (totalMinutes / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()
    return if (hours == 0) pluralStringResource(R.plurals.recap_minutes, minutes, minutes)
    else stringResource(R.string.recap_hours_minutes, hours, minutes)
}

private fun partName(part: PartOfDay) = when (part) {
    PartOfDay.MORNING -> R.string.recap_part_morning
    PartOfDay.DAY -> R.string.recap_part_day
    PartOfDay.EVENING -> R.string.recap_part_evening
    PartOfDay.NIGHT -> R.string.recap_part_night
}

private fun partLine(part: PartOfDay) = when (part) {
    PartOfDay.MORNING -> R.string.recap_part_morning_line
    PartOfDay.DAY -> R.string.recap_part_day_line
    PartOfDay.EVENING -> R.string.recap_part_evening_line
    PartOfDay.NIGHT -> R.string.recap_part_night_line
}

private fun partShort(part: PartOfDay) = when (part) {
    PartOfDay.MORNING -> R.string.recap_part_morning_short
    PartOfDay.DAY -> R.string.recap_part_day_short
    PartOfDay.EVENING -> R.string.recap_part_evening_short
    PartOfDay.NIGHT -> R.string.recap_part_night_short
}

private suspend fun shareCard(context: Context, layer: GraphicsLayer, period: RecapPeriod) {
    val bitmap = runCatching { layer.toImageBitmap().asAndroidBitmap() }.getOrNull() ?: return
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "recaps").apply { mkdirs() }
        val file = File(dir, "pulse-${period.key}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
