package com.metrolist.music.listentogether

import android.content.Context
import android.content.Intent
import com.metrolist.music.R

/**
 * Pulse: inviting friends to a Listen Together room. The metrolist.cc link only opens Metrolist,
 * so the invite is a plain message with the code, and Pulse picks the code up from the clipboard
 * when the friend opens Together.
 */
object PulseInvite {
    private val fromLink = Regex("""[?&](?:code|room)=([A-Za-z0-9]{8})\b""")
    // "код комнаты: K7Q2M8XA", "room code K7Q2M8XA": a few words may sit between.
    private val afterWord = Regex("""(?:[Кк]од|[Cc]ode)[^A-Z0-9]{0,16}([A-Z0-9]{8})\b""")
    private val bare = Regex("""^[A-Z0-9]{8}$""")

    /** The room code in a copied invite, link or bare code; null when there is none. */
    fun extractCode(text: String?): String? {
        if (text.isNullOrBlank()) return null
        fromLink.find(text)?.let { return it.groupValues[1].uppercase() }
        afterWord.find(text)?.let { return it.groupValues[1] }
        return text.trim().takeIf { bare.matches(it) }
    }

    fun message(context: Context, code: String): String = context.getString(R.string.listen_together_invite_text, code)

    fun share(context: Context, code: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message(context, code))
        }
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
