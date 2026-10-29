package app.pulse.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Pulse desktop: the VK session for importing, passed from the phone with the sign-in message.
 * The computer has no browser to sign in to VK with, and VK's music lives behind its mobile site.
 */
object VkAccount {
    @Serializable
    data class Saved(val userId: Long, val cookies: String)

    private val file get() = File(AppDirs.data, "vk.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val _saved = MutableStateFlow(runCatching { json.decodeFromString(Saved.serializer(), file.readText()) }.getOrNull())
    val saved: StateFlow<Saved?> = _saved.asStateFlow()

    fun save(userId: Long, cookies: String) {
        val value = Saved(userId, cookies)
        AppDirs.writePrivate(file, json.encodeToString(Saved.serializer(), value))
        _saved.value = value
    }

    fun signOut() {
        runCatching { file.delete() }
        _saved.value = null
    }
}
