package app.pulse.desktop

import com.metrolist.innertube.YouTube
import com.metrolist.music.pulse.LoginTransfer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

/**
 * Pulse desktop: the YouTube Music account, carried over from the phone (see LoginTransfer).
 * The session is kept in a file only the user can read, like a browser keeps its cookies.
 */
object Account {
    data class Info(val name: String, val avatar: String?)

    private val file get() = File(AppDirs.data, "account.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val _info = MutableStateFlow<Info?>(null)
    val info: StateFlow<Info?> = _info.asStateFlow()
    val signedIn: Boolean get() = _info.value != null

    /** At start: puts a saved session back into the YouTube client. */
    suspend fun restore() {
        val session = runCatching { json.decodeFromString(LoginTransfer.Session.serializer(), file.readText()) }.getOrNull() ?: return
        apply(session)
        _info.value = Info(session.accountName, null)
        YouTube.accountInfo().onSuccess { _info.value = Info(it.name, it.thumbnailUrl) }
    }

    sealed interface Result {
        data object Ok : Result
        data object NotFound : Result
        data object WrongCode : Result
        data object Expired : Result
        data object Rejected : Result
    }

    suspend fun signIn(message: String, pairingCode: String): Result {
        // The key derivation is slow on purpose; keep it off the window's thread.
        val opened = withContext(Dispatchers.Default) { LoginTransfer.open(message, pairingCode) }
        val session = when (opened) {
            is LoginTransfer.Opened.Ok -> opened.session
            LoginTransfer.Opened.NotFound -> return Result.NotFound
            LoginTransfer.Opened.WrongCode -> return Result.WrongCode
            LoginTransfer.Opened.Expired -> return Result.Expired
        }
        apply(session)
        val account = YouTube.accountInfo().getOrNull()
        if (account == null) {
            clear()
            return Result.Rejected
        }
        save(session.copy(accountName = account.name))
        _info.value = Info(account.name, account.thumbnailUrl)
        return Result.Ok
    }

    fun signOut() {
        clear()
        runCatching { file.delete() }
        _info.value = null
    }

    private fun apply(session: LoginTransfer.Session) {
        YouTube.cookie = session.cookie
        YouTube.visitorData = session.visitorData
        YouTube.dataSyncId = session.dataSyncId.ifBlank { null }
        YouTube.authUser = session.authUser
    }

    private fun clear() {
        YouTube.cookie = null
        YouTube.dataSyncId = null
        YouTube.authUser = "0"
    }

    private fun save(session: LoginTransfer.Session) {
        runCatching {
            file.writeText(json.encodeToString(LoginTransfer.Session.serializer(), session))
            runCatching {
                Files.setPosixFilePermissions(file.toPath(), setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))
            }.onFailure {
                // Windows has no POSIX modes; %APPDATA% is already private to the user.
                file.setReadable(false, false)
                file.setReadable(true, true)
            }
        }
    }
}
