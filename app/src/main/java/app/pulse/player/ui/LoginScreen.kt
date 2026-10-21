package app.pulse.player.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.pulse.player.sync.Cookies
import kotlinx.coroutines.delay

// Same entry point other YouTube Music clients use; it lands on music.youtube.com once signed in.
private const val LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true" +
        "&continue=https%3A%2F%2Fmusic.youtube.com%2F"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val holder = remember { object { var webView: WebView? = null; var finished = false } }
    var progress by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var slow by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }

    fun finish(showHint: Boolean) {
        if (holder.finished) return
        if (Cookies.saveFromWebView(context)) {
            holder.finished = true
            Toast.makeText(context, "Вход выполнен", Toast.LENGTH_SHORT).show()
            onDone()
        } else if (showHint) {
            Toast.makeText(context, "Сначала войди в аккаунт", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkUrl(url: String?) {
        if (url != null && url.startsWith("https://music.youtube.com") && Cookies.webViewHasSession()) finish(false)
    }

    val importCookies = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull().orEmpty()
        if (Cookies.import(context, text)) {
            holder.finished = true
            Toast.makeText(context, "Cookies импортированы", Toast.LENGTH_SHORT).show()
            onDone()
        } else {
            Toast.makeText(context, "В файле нет cookies YouTube", Toast.LENGTH_LONG).show()
        }
    }

    // A login page that is still empty after a while is almost always a network problem.
    LaunchedEffect(attempt) {
        slow = false
        delay(15_000)
        if (progress < 100 && error == null) slow = true
    }

    BackHandler {
        val web = holder.webView
        if (web != null && web.canGoBack() && error == null) web.goBack() else onDone()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
            Text("Вход в YouTube Music", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { finish(true) }) { Text("Готово") }
        }
        if (progress in 1..99 && error == null) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp))
        } else if (progress == 0 && error == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth().height(3.dp))
        } else {
            Spacer(Modifier.height(3.dp))
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    try {
                        WebView(ctx).apply {
                            // Without explicit MATCH_PARENT the page measures itself 0 px tall and stays blank.
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    progress = newProgress
                                    if (newProgress == 100) slow = false
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) = checkUrl(url)
                                override fun onPageFinished(view: WebView, url: String?) = checkUrl(url)
                                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) = checkUrl(url)

                                override fun onReceivedError(view: WebView, request: WebResourceRequest, err: WebResourceError) {
                                    if (request.isForMainFrame) error = "${err.description} (код ${err.errorCode})"
                                }
                            }
                            loadUrl(LOGIN_URL)
                            holder.webView = this
                        }
                    } catch (e: Exception) {
                        // No usable system WebView on the device.
                        error = "Не удалось открыть встроенный браузер: ${e.message}"
                        FrameLayout(ctx)
                    }
                },
                onRelease = { view ->
                    (view as? WebView)?.destroy()
                    holder.webView = null
                },
            )

            val problem = error ?: if (slow) "Страница входа долго не грузится." else null
            if (problem != null) {
                Column(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Не получается открыть вход", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(problem, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Проверь интернет. Если YouTube у тебя открывается только через VPN — включи VPN для всего " +
                            "телефона или добавь Pulse в список приложений VPN.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = {
                        error = null
                        progress = 0
                        attempt++
                        holder.webView?.loadUrl(LOGIN_URL)
                    }) {
                        Icon(Icons.Rounded.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Повторить")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { importCookies.launch(arrayOf("text/*", "*/*")) }) {
                        Icon(Icons.Rounded.FileOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Импортировать cookies.txt")
                    }
                    if (error == null) {
                        TextButton(onClick = { slow = false }) { Text("Подождать ещё") }
                    }
                }
            }
        }
    }
}
