package com.metrolist.music.listentogether

import android.content.Context
import com.metrolist.music.constants.ListenTogetherServerUrlKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Pulse: a server on a free plan sleeps when nobody uses it. A plain request to its health page
 * wakes it, so Together knocks while the user is still typing a name.
 */
object PulseServerWake {
    private val client by lazy {
        OkHttpClient.Builder()
            .callTimeout(75, TimeUnit.SECONDS)
            .readTimeout(75, TimeUnit.SECONDS)
            .build()
    }

    /** "wss://host/ws" → "https://host/health"; null for anything that is not a WebSocket address. */
    fun healthUrl(serverUrl: String): String? {
        val trimmed = serverUrl.trim()
        val scheme = when {
            trimmed.startsWith("wss://", ignoreCase = true) -> "https://"
            trimmed.startsWith("ws://", ignoreCase = true) -> "http://"
            else -> return null
        }
        val host = trimmed.substringAfter("://").substringBefore("/").takeIf { it.isNotBlank() } ?: return null
        return "$scheme$host/health"
    }

    suspend fun knock(context: Context) {
        val url = healthUrl(context.dataStore.get(ListenTogetherServerUrlKey, ListenTogetherServers.defaultServerUrl)) ?: return
        withContext(Dispatchers.IO) {
            runCatching { client.newCall(Request.Builder().url(url).build()).execute().close() }
        }
    }
}
