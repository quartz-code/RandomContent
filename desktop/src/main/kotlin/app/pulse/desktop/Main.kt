package app.pulse.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.pulse.desktop.res.Res
import app.pulse.desktop.res.close
import app.pulse.desktop.res.oswald
import app.pulse.desktop.res.palette
import app.pulse.desktop.res.person
import app.pulse.desktop.res.playlist_add
import app.pulse.desktop.res.playlist_play
import app.pulse.desktop.res.pt_mono
import app.pulse.desktop.res.search
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.ui.player.PlayerTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

fun main(args: Array<String>) {
    if ("--self-test" in args) selfTestAndExit(args)
    runWindow()
}

/**
 * `Pulse --self-test`: decodes a sample built into the app and exits 0. CI runs it on the packaged
 * app, so an installer whose trimmed Java or FFmpeg natives cannot play sound fails the build.
 */
private fun selfTestAndExit(args: Array<String>): Nothing {
    val code = runCatching {
        val sample = java.io.File.createTempFile("pulse-self-test", ".webm").apply { deleteOnExit() }
        object {}.javaClass.getResourceAsStream("/self_test.webm")!!.use { input -> sample.outputStream().use { input.copyTo(it) } }
        val seconds = Decoder.decodeSeconds(sample, 10.0)
        println("Pulse self-test: decoded ${"%.1f".format(seconds)} s with FFmpeg")
        if (seconds <= 2.5) return@runCatching 2
        // With --net, also reach YouTube over TLS and resolve a stream (link and cipher).
        if ("--net" in args) {
            kotlinx.coroutines.runBlocking {
                YouTubeSetup.init()
                check(YouTube.visitorData != null) { "no visitor data" }
                val stream = StreamResolver.resolve("Ra0ozaE-oy0")
                println("Pulse self-test: stream from ${stream.clientName}, ${stream.mimeType}")
            }
        }
        0
    }.getOrElse {
        it.printStackTrace()
        1
    }
    kotlin.system.exitProcess(code)
}

private fun runWindow() = application {
    val scope = rememberCoroutineScope()
    val controller = remember { PlayerController(scope).also { it.setVolume(Settings.volume) } }
    val library = remember { LibraryModel(scope) }
    LaunchedEffect(Unit) {
        runCatching { YouTubeSetup.init() }
        runCatching { Account.restore() }
        library.refresh()
    }
    Window(
        onCloseRequest = {
            controller.close()
            exitApplication()
        },
        title = Strings.appName,
        state = rememberWindowState(size = DpSize(1180.dp, 760.dp)),
        onPreviewKeyEvent = { event ->
            // Media keys work while the window has focus.
            if (event.type != KeyEventType.KeyDown) return@Window false
            when (event.key) {
                Key.MediaPlayPause -> { controller.togglePlay(); true }
                Key.MediaNext -> { controller.next(); true }
                Key.MediaPrevious -> { controller.previous(); true }
                else -> false
            }
        },
    ) {
        PulseApp(controller, library)
    }
}

@Composable
fun PulseApp(controller: PlayerController, library: LibraryModel, initialTheme: PlayerTheme = Settings.loadTheme()) {
    val state by controller.state.collectAsState()
    val likedIds by library.likedIds.collectAsState()
    val account by Account.info.collectAsState()
    var theme by remember { mutableStateOf(initialTheme) }
    var showTheme by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    val fonts = ThemeFonts(FontFamily(Font(Res.font.oswald)), FontFamily(Font(Res.font.pt_mono)))
    LaunchedEffect(state.volume) { Settings.volume = state.volume }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFB8C4FF), surface = Color(0xFF15161A), background = Color(0xFF101114))) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Row(Modifier.fillMaxSize()) {
                Library(controller, library, state, onTheme = { showTheme = true }, onAccount = { showAccount = true }, modifier = Modifier.weight(1f).fillMaxHeight())
                PlayerPanel(
                    theme, state, controller, fonts,
                    liked = state.current?.let { it.videoId in likedIds }.takeIf { account != null },
                    onLike = { state.current?.let(library::toggleLike) },
                    modifier = Modifier.width(420.dp).fillMaxHeight(),
                )
            }
        }
        if (showAccount) {
            AccountDialog(library, onDismiss = { showAccount = false })
        }
        if (showTheme) {
            ThemeDialog(
                current = theme,
                onApply = {
                    theme = it
                    Settings.saveTheme(it)
                },
                onDismiss = { showTheme = false },
            )
        }
    }
}

@Composable
private fun Library(controller: PlayerController, library: LibraryModel, state: PlayerState, onTheme: () -> Unit, onAccount: () -> Unit, modifier: Modifier) {
    val account by Account.info.collectAsState()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Track>?>(null) }
    var searching by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }

    fun search() {
        val q = query.trim()
        if (q.isEmpty()) return
        tab = 0
        searching = true
        failed = false
        scope.launch {
            val found = YouTube.search(q, YouTube.SearchFilter.FILTER_SONG)
            searching = false
            found.onSuccess { r -> results = r.items.filterIsInstance<SongItem>().map(Track::from) }
                .onFailure { failed = true; results = emptyList() }
        }
    }

    Column(modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Strings.appName, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(end = 16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(Strings.searchHint) },
                leadingIcon = { Icon(painterResource(Res.drawable.search), null, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f).onEnter(::search),
            )
            IconButton(onClick = onTheme) { Icon(painterResource(Res.drawable.palette), Strings.theme) }
            IconButton(onClick = onAccount) {
                if (account?.avatar != null) {
                    AsyncImage(account?.avatar, Strings.account, contentScale = ContentScale.Crop, modifier = Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape))
                } else {
                    Icon(painterResource(Res.drawable.person), Strings.account)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
            Tab(tab == 0, onClick = { tab = 0 }, text = { Text(Strings.searchTab) })
            Tab(tab == 1, onClick = { tab = 1 }, text = { Text(Strings.myMusic) })
            Tab(tab == 2, onClick = { tab = 2 }, text = { Text("${Strings.queue} · ${state.queue.size}") })
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                tab == 1 -> LibraryTab(library, controller, state, onSignIn = onAccount)
                tab == 2 -> QueueList(state, controller)
                searching -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                results == null -> Hint(Strings.searchEmpty)
                results!!.isEmpty() -> Hint(if (failed) Strings.searchFailed else Strings.searchNothing)
                else -> LazyColumn {
                    itemsIndexed(results!!, key = { i, t -> "${t.videoId}_$i" }) { i, track ->
                        SongRow(
                            track = track,
                            playing = state.current?.videoId == track.videoId,
                            onClick = { controller.playQueue(results!!, i) },
                            actions = { TrackActions(controller, track) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueList(state: PlayerState, controller: PlayerController) {
    if (state.queue.isEmpty()) return Hint(Strings.queueEmpty)
    LazyColumn {
        itemsIndexed(state.queue, key = { i, t -> "${t.videoId}_$i" }) { i, track ->
            SongRow(
                track = track,
                playing = i == state.index,
                onClick = { controller.playAt(i) },
                actions = {
                    if (i != state.index) {
                        IconButton(onClick = { controller.removeAt(i) }) { Icon(painterResource(Res.drawable.close), Strings.remove, modifier = Modifier.size(18.dp)) }
                    }
                },
            )
        }
    }
}

@Composable
internal fun TrackActions(controller: PlayerController, track: Track) {
    IconButton(onClick = { controller.playNext(track) }) { Icon(painterResource(Res.drawable.playlist_play), Strings.playNext, modifier = Modifier.size(20.dp)) }
    IconButton(onClick = { controller.enqueue(track) }) { Icon(painterResource(Res.drawable.playlist_add), Strings.addToQueue, modifier = Modifier.size(20.dp)) }
}

@Composable
internal fun SongRow(track: Track, playing: Boolean, onClick: () -> Unit, actions: @Composable () -> Unit) {
    val hover = remember { MutableInteractionSource() }
    val hovered by hover.collectIsHoveredAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (playing) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else if (hovered) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .hoverable(hover)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        AsyncImage(track.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.06f)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(track.artists, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        if (hovered) actions()
        track.durationSec?.let {
            Text("%d:%02d".format(it / 60, it % 60), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
internal fun Hint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

/** The phone's theme on the computer: paste the share message, or start from a preset. */
@Composable
private fun ThemeDialog(current: PlayerTheme, onApply: (PlayerTheme) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val parsed = remember(text) { PlayerTheme.fromCode(text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.theme) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(Strings.themeIntro, fontSize = 14.sp)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(Strings.themeCodeHint) },
                    isError = text.isNotBlank() && parsed == null,
                    supportingText = { if (text.isNotBlank() && parsed == null) Text(Strings.themeBadCode) },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(Strings.presets, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(PlayerTheme.Clean to Strings.presetClean, PlayerTheme.OnAir to Strings.presetOnAir, PlayerTheme.Light to Strings.presetLight).forEach { (preset, name) ->
                        FilterChip(selected = current == preset, onClick = { onApply(preset) }, label = { Text(name) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = {
                parsed?.let(onApply)
                onDismiss()
            }) { Text(Strings.themeApply) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.close) } },
    )
}

private fun Modifier.onEnter(action: () -> Unit): Modifier = onPreviewKeyEvent {
    if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
        action()
        true
    } else {
        false
    }
}
