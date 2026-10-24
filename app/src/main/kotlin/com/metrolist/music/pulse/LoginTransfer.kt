package com.metrolist.music.pulse

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Pulse: carries the YouTube Music sign-in from the phone to Pulse on a computer, which has no
 * Android WebView to sign in with (and Google blocks embedded browsers).
 *
 * The computer shows a short pairing code; the phone encrypts the session with a key derived from
 * it and hands over a "PULSELOGIN1-…" message the user sends to themselves. The code never travels
 * with the message, so a copy left in a chat is useless without it, and the slow key derivation
 * makes guessing the 40-bit code impractical. Plain Kotlin and javax.crypto: the desktop build
 * compiles this same file. The VK fields are optional, so the YouTube part alone keeps working
 * both ways between older and newer versions.
 */
object LoginTransfer {
    @Serializable
    data class Session(
        val cookie: String,
        val visitorData: String,
        val dataSyncId: String = "",
        val authUser: String = "0",
        val accountName: String = "",
        val createdAtMs: Long = 0,
        /** VK, for importing on the computer: the m.vk.ru session of the phone's VK import, if it is passed too. */
        val vkUserId: Long = 0,
        val vkCookies: String = "",
    )

    const val PREFIX = "PULSELOGIN1-"
    /** No 0/O, 1/I/L, U: the code is read off a screen and typed on a phone. */
    private const val ALPHABET = "23456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val CODE_LENGTH = 8
    private const val ITERATIONS = 200_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    /** A message older than this is refused, so an old chat copy cannot be replayed later. */
    const val MAX_AGE_MS = 30 * 60 * 1000L

    private val json = Json { ignoreUnknownKeys = true }
    private val random = SecureRandom()
    private val messageInText = Regex("""PULSELOGIN1-[A-Za-z0-9_-]+""")

    /** "K7QP-9XMB": what the computer shows. */
    fun newPairingCode(): String {
        val chars = CharArray(CODE_LENGTH) { ALPHABET[random.nextInt(ALPHABET.length)] }
        return String(chars, 0, 4) + "-" + String(chars, 4, 4)
    }

    /** What the user typed on the phone, without spaces, dashes or case; null when malformed. */
    fun normalizeCode(input: String): String? {
        val code = input.uppercase().filter { it.isLetterOrDigit() }
        return code.takeIf { it.length == CODE_LENGTH && it.all { c -> c in ALPHABET } }
    }

    fun seal(session: Session, pairingCode: String, nowMs: Long = System.currentTimeMillis()): String {
        val code = requireNotNull(normalizeCode(pairingCode)) { "bad pairing code" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(code, salt), GCMParameterSpec(128, iv))
        val plain = json.encodeToString(Session.serializer(), session.copy(createdAtMs = nowMs)).toByteArray()
        val sealed = salt + iv + cipher.doFinal(plain)
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(sealed)
    }

    sealed interface Opened {
        data class Ok(val session: Session) : Opened
        /** Not a Pulse login message at all. */
        data object NotFound : Opened
        /** Wrong pairing code, or the message was cut or changed. */
        data object WrongCode : Opened
        data object Expired : Opened
    }

    /** Finds the message anywhere in [text] (a whole forwarded message is fine) and opens it. */
    fun open(text: String, pairingCode: String, nowMs: Long = System.currentTimeMillis()): Opened {
        val message = messageInText.find(text)?.value ?: return Opened.NotFound
        val code = normalizeCode(pairingCode) ?: return Opened.WrongCode
        val sealed = runCatching { Base64.getUrlDecoder().decode(message.removePrefix(PREFIX)) }.getOrNull()
            ?.takeIf { it.size > SALT_BYTES + IV_BYTES + 16 } ?: return Opened.WrongCode
        val salt = sealed.copyOfRange(0, SALT_BYTES)
        val iv = sealed.copyOfRange(SALT_BYTES, SALT_BYTES + IV_BYTES)
        val session = runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(code, salt), GCMParameterSpec(128, iv))
            json.decodeFromString(Session.serializer(), String(cipher.doFinal(sealed.copyOfRange(SALT_BYTES + IV_BYTES, sealed.size))))
        }.getOrNull() ?: return Opened.WrongCode
        if (nowMs - session.createdAtMs !in 0..MAX_AGE_MS) return Opened.Expired
        return Opened.Ok(session)
    }

    private fun key(code: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(code.toCharArray(), salt, ITERATIONS, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }
}
