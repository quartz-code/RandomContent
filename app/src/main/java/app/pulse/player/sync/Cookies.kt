package app.pulse.player.sync

import android.content.Context
import android.webkit.CookieManager
import java.io.File

/** Stores the YouTube session as a Netscape cookies.txt that yt-dlp reads with --cookies. */
object Cookies {
    private const val FILE = "cookies.txt"
    private val AUTH_COOKIES = listOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PSID", "SID")

    fun file(context: Context): File = File(context.filesDir, FILE)

    fun isLoggedIn(context: Context): Boolean {
        val f = file(context)
        return f.exists() && AUTH_COOKIES.any { f.readText().contains("\t$it\t") }
    }

    /** True when the WebView already has a signed-in YouTube session. */
    fun webViewHasSession(): Boolean {
        val raw = CookieManager.getInstance().getCookie("https://music.youtube.com") ?: return false
        return AUTH_COOKIES.any { raw.contains("$it=") }
    }

    fun saveFromWebView(context: Context): Boolean {
        val manager = CookieManager.getInstance()
        manager.flush()
        val cookies = linkedMapOf<String, String>()
        for (url in listOf("https://www.youtube.com", "https://music.youtube.com")) {
            manager.getCookie(url)?.split(";")?.forEach { pair ->
                val idx = pair.indexOf('=')
                if (idx > 0) cookies[pair.substring(0, idx).trim()] = pair.substring(idx + 1).trim()
            }
        }
        if (AUTH_COOKIES.none { it in cookies }) return false

        // WebView does not expose expiry, so give everything a year; YouTube refreshes them anyway.
        val expiry = System.currentTimeMillis() / 1000 + 365L * 24 * 3600
        val text = buildString {
            appendLine("# Netscape HTTP Cookie File")
            for ((name, value) in cookies) {
                appendLine(listOf(".youtube.com", "TRUE", "/", "TRUE", expiry, name, value).joinToString("\t"))
            }
        }
        file(context).writeText(text)
        return true
    }

    /** Import a cookies.txt exported from a desktop browser. */
    fun import(context: Context, text: String): Boolean {
        if (!text.contains("youtube.com") || AUTH_COOKIES.none { text.contains("\t$it\t") }) return false
        file(context).writeText(text)
        return true
    }

    fun logout(context: Context) {
        file(context).delete()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }
}
