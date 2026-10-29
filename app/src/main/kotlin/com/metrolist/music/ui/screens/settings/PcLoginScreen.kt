package com.metrolist.music.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeAuthUserKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.constants.VkUserIdKey
import com.metrolist.music.pulse.LoginTransfer
import com.metrolist.music.pulse.importer.VkMusicImport
import com.metrolist.music.pulse.importer.collectVkCookies
import com.metrolist.music.ui.component.StepGuide
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pulse: signs Pulse on a computer in with this phone's YouTube Music account. The computer shows
 * a pairing code; the session is encrypted with it, so the message the user sends themselves is
 * useless to anyone who sees the chat but not the computer's screen.
 */
@Composable
fun PcLoginScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cookie by rememberPreference(InnerTubeCookieKey, "")
    val visitorData by rememberPreference(VisitorDataKey, "")
    val dataSyncId by rememberPreference(DataSyncIdKey, "")
    val authUser by rememberPreference(InnerTubeAuthUserKey, "0")
    val accountName by rememberPreference(AccountNameKey, "")
    val signedIn = "SAPISID" in cookie
    // VK goes along when the phone's VK import is signed in: the computer has no browser to sign in with.
    val vkUserId by rememberPreference(VkUserIdKey, 0L)
    val vkCookies = remember(vkUserId) { collectVkCookies() }
    val vkSignedIn = vkUserId > 0 && VkMusicImport.hasSession(vkCookies)
    var withVk by remember { mutableStateOf(true) }
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    val validCode = LoginTransfer.normalizeCode(code) != null

    PulseScreenScaffold(navController, stringResource(R.string.pc_login)) {
        IntroCard(stringResource(R.string.pc_login_intro))
        Spacer(Modifier.height(16.dp))
        if (!signedIn && !vkSignedIn) {
            Text(stringResource(R.string.pc_login_not_signed_in), color = MaterialTheme.colorScheme.error)
            return@PulseScreenScaffold
        }
        StepGuide(
            title = stringResource(R.string.guide_how_to_connect),
            steps = listOf(
                stringResource(R.string.pc_login_step_1),
                stringResource(R.string.pc_login_step_2),
                stringResource(R.string.pc_login_step_3),
            ),
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = code,
            onValueChange = {
                code = it.take(12)
                message = null
            },
            enabled = !working,
            label = { Text(stringResource(R.string.pc_login_code)) },
            placeholder = { Text("K7QP-9XMB") },
            singleLine = true,
            isError = code.isNotBlank() && !validCode,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
        )
        if (vkSignedIn) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.pc_login_with_vk), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = withVk, onCheckedChange = { withVk = it })
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            enabled = validCode && !working && (signedIn || withVk),
            onClick = {
                // The code as it is now: the field stays editable while the slow sealing runs.
                val sealCode = LoginTransfer.normalizeCode(code) ?: return@Button
                working = true
                scope.launch {
                    // The key derivation is slow on purpose; keep it off the main thread.
                    val vk = withVk && vkSignedIn
                    val session = LoginTransfer.Session(
                        cookie = if (signedIn) cookie else "",
                        visitorData = visitorData,
                        dataSyncId = dataSyncId,
                        authUser = authUser,
                        accountName = accountName,
                        vkUserId = if (vk) vkUserId else 0,
                        vkCookies = if (vk) vkCookies else "",
                    )
                    val sealed = withContext(Dispatchers.Default) { LoginTransfer.seal(session, sealCode) }
                    val text = context.getString(R.string.pc_login_message, sealed)
                    message = text
                    copy(context, text)
                    working = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.pc_login_make)) }
        message?.let { text ->
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.pc_login_copied), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.pc_login_send)) }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.pc_login_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Pulse", text))
    Toast.makeText(context, R.string.pc_login_copied_toast, Toast.LENGTH_SHORT).show()
}
