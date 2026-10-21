package com.metrolist.music.pulse.importer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.constants.VkUserIdKey
import com.metrolist.music.ui.component.StepGuide
import com.metrolist.music.ui.screens.settings.IntroCard
import com.metrolist.music.ui.screens.settings.PulseScreenScaffold
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch

internal fun collectVkCookies(): String {
    val manager = CookieManager.getInstance()
    val merged = linkedMapOf<String, String>()
    for (host in VkMusicImport.COOKIE_HOSTS) {
        manager.getCookie(host)?.split(';')?.forEach { part ->
            val eq = part.indexOf('=')
            if (eq > 0) merged[part.substring(0, eq).trim()] = part.substring(eq + 1).trim()
        }
    }
    return merged.entries.joinToString("; ") { "${it.key}=${it.value}" }
}

/** Removes only VK's cookies: the Google sign-in lives in the same cookie store. */
private fun clearVkCookies() {
    val manager = CookieManager.getInstance()
    for (host in VkMusicImport.COOKIE_HOSTS) {
        val root = if (host.endsWith("vk.ru")) "vk.ru" else "vk.com"
        val names = manager.getCookie(host)?.split(';')?.mapNotNull { it.substringBefore('=').trim().ifBlank { null } }.orEmpty()
        for (name in names) {
            manager.setCookie(host, "$name=; Max-Age=0; Path=/")
            manager.setCookie(host, "$name=; Max-Age=0; Path=/; Domain=.$root")
        }
    }
    manager.flush()
}

private fun failureText(e: Throwable): Int = when (e) {
    is VkMusicImport.Failure.NotLoggedIn -> R.string.vk_error_session
    is VkMusicImport.Failure.AccessDenied -> R.string.vk_error_denied
    is VkMusicImport.Failure.BadLink -> R.string.vk_error_link
    else -> R.string.vk_error_network
}

/** Pulse: import "My music" or a playlist from VK, then review the match on YouTube Music. */
@Composable
fun VkImportScreen(navController: NavController, viewModel: ImportViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val (userId, onUserIdChange) = rememberPreference(VkUserIdKey, defaultValue = 0L)
    var cookieVersion by remember { mutableIntStateOf(0) }
    val cookies = remember(cookieVersion, userId) { collectVkCookies() }
    val loggedIn = userId > 0 && VkMusicImport.hasSession(cookies)
    var showLogin by remember { mutableStateOf(false) }

    var playlists by remember { mutableStateOf<List<VkMusicImport.VkPlaylist>?>(null) }
    var playlistsError by remember { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var loaded by remember { mutableStateOf<ImportedList?>(null) }
    var link by remember { mutableStateOf("") }

    fun load(block: suspend () -> ImportedList) {
        loading = true
        error = null
        scope.launch {
            try {
                val list = block()
                if (list.tracks.isEmpty()) error = R.string.vk_import_empty else loaded = list
            } catch (e: Exception) {
                error = failureText(e)
                if (e is VkMusicImport.Failure.NotLoggedIn) onUserIdChange(0L)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(loggedIn) {
        if (loggedIn && playlists == null) {
            runCatching { VkMusicImport.loadPlaylists(userId, cookies) }
                .onSuccess { playlists = it }
                .onFailure { playlistsError = failureText(it); playlists = emptyList() }
        }
    }

    if (showLogin) {
        VkLoginView(
            onDone = { id ->
                showLogin = false
                if (id != null) {
                    onUserIdChange(id)
                    playlists = null
                    cookieVersion++
                }
            },
        )
        return
    }

    PulseScreenScaffold(navController, stringResource(R.string.vk_import)) {
        IntroCard(stringResource(R.string.vk_intro))
        Spacer(Modifier.height(16.dp))

        val ready = loaded
        when {
            ready != null -> {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.vk_loaded, ready.title, ready.tracks.size), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.vk_next_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            viewModel.session.start(ready)
                            loaded = null
                            navController.navigate("pulse/import_review")
                        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.yandex_import_find)) }
                        TextButton(onClick = { loaded = null }) { Text(stringResource(R.string.vk_pick_other)) }
                    }
                }
            }
            !loggedIn -> {
                StepGuide(
                    title = stringResource(R.string.guide_how_to_connect),
                    steps = listOf(
                        stringResource(R.string.vk_setup_1),
                        stringResource(R.string.vk_setup_2),
                        stringResource(R.string.vk_setup_3),
                        stringResource(R.string.vk_setup_4),
                    ),
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = { showLogin = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.vk_login)) }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.vk_logged_in), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        clearVkCookies()
                        onUserIdChange(0L)
                        playlists = null
                        cookieVersion++
                    }) { Text(stringResource(R.string.vk_logout)) }
                }
                if (loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.vk_loading), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                error?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }

                SourceRow(
                    title = stringResource(R.string.vk_my_music),
                    subtitle = stringResource(R.string.vk_my_music_desc),
                    enabled = !loading,
                    onClick = { load { VkMusicImport.loadMyMusic(userId, cookies) } },
                )
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.vk_playlists), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                val list = playlists
                when {
                    list == null -> Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.vk_playlists_loading))
                    }
                    list.isEmpty() -> Text(
                        stringResource(playlistsError ?: R.string.vk_playlists_empty),
                        Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> list.forEach { pl ->
                        SourceRow(
                            title = pl.title,
                            subtitle = listOfNotNull(pl.author, pl.trackCount?.let { stringResource(R.string.vk_tracks_count, it) }).joinToString(" · "),
                            enabled = !loading,
                            onClick = { load { VkMusicImport.loadPlaylist(cookies, pl) } },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.vk_by_link), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it; error = null },
                    singleLine = true,
                    placeholder = { Text("vk.com/music/playlist/…") },
                    supportingText = { Text(stringResource(R.string.vk_by_link_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    enabled = link.isNotBlank() && !loading,
                    onClick = {
                        val pl = VkMusicImport.parsePlaylistLink(link)
                        if (pl == null) error = R.string.vk_error_link else load { VkMusicImport.loadPlaylist(cookies, pl) }
                    },
                ) { Text(stringResource(R.string.vk_load)) }
            }
        }

        Spacer(Modifier.height(24.dp))
        StepGuide(
            title = stringResource(R.string.guide_good_to_know),
            steps = listOf(
                stringResource(R.string.vk_note_1),
                stringResource(R.string.vk_note_2),
                stringResource(R.string.vk_note_3),
            ),
            numbered = false,
        )
    }
}

@Composable
private fun SourceRow(title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.queue_music), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * VK's own sign-in page. After the OAuth redirect the user id is read and the token dropped; if
 * VK did not set a music session cookie yet, its mobile site is opened once so it does.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VkLoginView(onDone: (Long?) -> Unit) {
    val holder = remember { object { var webView: WebView? = null; var userId: Long? = null; var finished = false } }
    var progress by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var noSession by remember { mutableStateOf(false) }

    fun finish(id: Long?) {
        if (holder.finished) return
        holder.finished = true
        onDone(id)
    }

    fun check(view: WebView, url: String?, pageLoaded: Boolean = false) {
        if (holder.finished) return
        when (val r = VkMusicImport.parseRedirect(url)) {
            is VkMusicImport.Redirect.Error -> error = r.message
            is VkMusicImport.Redirect.Success -> {
                if (holder.userId != null) return
                holder.userId = r.userId
                CookieManager.getInstance().flush()
                if (VkMusicImport.hasSession(collectVkCookies())) finish(r.userId) else view.loadUrl("https://m.vk.ru/")
            }
            null -> {
                val id = holder.userId ?: return
                if (pageLoaded && url?.contains("m.vk.") == true) {
                    CookieManager.getInstance().flush()
                    if (VkMusicImport.hasSession(collectVkCookies())) finish(id) else noSession = true
                }
            }
        }
    }

    BackHandler {
        val web = holder.webView
        if (web != null && web.canGoBack()) web.goBack() else finish(null)
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.systemBars)) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { finish(null) }) { Text(stringResource(android.R.string.cancel)) }
            Text(stringResource(R.string.vk_login_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        if (progress in 0..99 && error == null) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp))
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        // Without explicit MATCH_PARENT the page measures itself 0 px tall and stays blank.
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = VkMusicImport.USER_AGENT
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                progress = newProgress
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) = check(view, url)
                            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) = check(view, url)
                            override fun onPageFinished(view: WebView, url: String?) {
                                check(view, url, pageLoaded = true)
                                // The redirect's #fragment is sometimes cut from the callback URL.
                                if (url?.contains("/blank.html") == true && !holder.finished) {
                                    view.evaluateJavascript("window.location.href") { href -> check(view, href?.trim('"')) }
                                }
                            }
                            // VK may try to hand sign-in to its app (intent://, vk://): stay in the page.
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val scheme = request.url.scheme.orEmpty()
                                return scheme != "http" && scheme != "https"
                            }

                            override fun onReceivedError(view: WebView, request: WebResourceRequest, err: WebResourceError) {
                                if (request.isForMainFrame) error = "${err.description} (${err.errorCode})"
                            }
                        }
                        loadUrl(VkMusicImport.authorizeUrl())
                        holder.webView = this
                    }
                },
                onRelease = { it.destroy() },
            )
            val problem = error ?: if (noSession) stringResource(R.string.vk_login_no_session) else null
            if (problem != null) {
                Column(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(problem, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.vk_login_problem_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        error = null
                        noSession = false
                        holder.userId = null
                        holder.webView?.loadUrl(VkMusicImport.authorizeUrl())
                    }) { Text(stringResource(R.string.vk_retry)) }
                }
            }
        }
    }

}
