package app.pulse.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pulse.desktop.res.Res
import app.pulse.desktop.res.close
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

/** The "Together" tab: start or join a room, then who is in it and what they ask for. */
@Composable
fun TogetherTab(together: TogetherSession) {
    val s by together.state.collectAsState()
    // A free server sleeps; knocking while the user reads and types hides most of the wake-up.
    LaunchedEffect(Unit) { together.wake() }
    Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        when {
            s.inRoom -> Room(together, s)
            s.phase != TogetherSession.Phase.IDLE -> Connecting(together, s)
            else -> Start(together, s)
        }
    }
}

@Composable
private fun Start(together: TogetherSession, s: TogetherSession.State) {
    val account by Account.info.collectAsState()
    var name by remember { mutableStateOf(Settings.togetherName?.takeIf { it.isNotBlank() } ?: account?.name ?: System.getProperty("user.name").orEmpty()) }
    // A copied invite in the clipboard opens straight on "Join" with its code, as on the phone.
    val invite = remember { TogetherSession.extractCode(clipboardText()) }
    var joining by remember { mutableStateOf(invite != null) }
    var code by remember { mutableStateOf(invite.orEmpty()) }
    val parsedCode = TogetherSession.extractCode(code)
    val nameOk = name.isNotBlank()

    fun go() {
        Settings.togetherName = name.trim()
        if (joining) parsedCode?.let { together.join(it, name) } else together.create(name)
    }

    Column(Modifier.widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(Strings.togetherIntro, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !joining, onClick = { joining = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(Strings.createRoom) }
            SegmentedButton(selected = joining, onClick = { joining = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(Strings.joinRoom) }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(MAX_NAME) },
            label = { Text(Strings.yourName) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (joining) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text(Strings.roomCodeHint) },
                placeholder = { Text("K7Q2M8XA") },
                isError = code.isNotBlank() && parsedCode == null,
                supportingText = { parsedCode?.takeIf { it != code.trim() }?.let { Text("${Strings.roomCode}: $it") } },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        problemText(s)?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
        Button(enabled = nameOk && (!joining || parsedCode != null), onClick = ::go, modifier = Modifier.fillMaxWidth()) {
            Text(if (joining) Strings.join else Strings.create)
        }
    }
}

@Composable
private fun Connecting(together: TogetherSession, s: TogetherSession.State) {
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator()
        Text(
            when (s.phase) {
                TogetherSession.Phase.WAKING -> Strings.waking
                TogetherSession.Phase.WAITING_APPROVAL -> Strings.waitingApproval
                else -> Strings.connecting
            },
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 440.dp),
        )
        TextButton(onClick = { together.leave() }) { Text(Strings.cancel) }
    }
}

@Composable
private fun Room(together: TogetherSession, s: TogetherSession.State) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(3_000)
            copied = false
        }
    }
    LaunchedEffect(s.notice) {
        if (s.notice != null) {
            delay(4_000)
            together.dismissNotice()
        }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.widthIn(max = 620.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(Strings.roomCode, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SelectionContainer {
                        Text(
                            s.roomCode.orEmpty(),
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp),
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(onClick = {
                        copyToClipboard(Strings.inviteText(s.roomCode.orEmpty()))
                        copied = true
                    }) { Text(if (copied) Strings.inviteCopied else Strings.copyInvite) }
                }
                Text(if (s.isHost) Strings.youLead else Strings.hostLeads(s.hostName.orEmpty()), fontSize = 14.sp)
                if (s.reconnecting) Text(Strings.reconnecting, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
            }
        }
        noticeText(s.notice)?.let { text ->
            item { Text(text, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp)) }
        }
        if (s.isHost && s.requests.isNotEmpty()) {
            item { Section(Strings.wantToJoin) }
            items(s.requests, key = { "ask_${it.userId}" }) { ask ->
                Line(ask.name) {
                    Button(onClick = { together.approve(ask.userId) }) { Text(Strings.letIn) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { together.reject(ask.userId) }) { Text(Strings.decline) }
                }
            }
        }
        if (s.isHost && s.suggestions.isNotEmpty()) {
            item { Section(Strings.suggested) }
            items(s.suggestions, key = { "idea_${it.id}" }) { idea ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    AsyncImage(idea.track.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.06f)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(idea.track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${idea.track.artists} · ${Strings.suggestedBy(idea.from)}", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Button(onClick = { together.acceptSuggestion(idea.id) }) { Text(Strings.addNext) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { together.declineSuggestion(idea.id) }) { Text(Strings.decline) }
                }
            }
        }
        item { Section("${Strings.inRoom} · ${s.members.size}") }
        items(s.members, key = { "member_${it.id}" }) { member ->
            val me = member.id == s.myId
            val tags = listOfNotNull(Strings.you.takeIf { me }, Strings.leads.takeIf { member.host }, Strings.offline.takeIf { !member.online })
            Line(member.name, tags.joinToString(" · ").takeIf { it.isNotEmpty() }, dim = !member.online) {
                if (s.isHost && !me) {
                    TextButton(onClick = { together.makeHost(member.id) }) { Text(Strings.handOver) }
                    IconButton(onClick = { together.kick(member.id) }) { Icon(painterResource(Res.drawable.close), Strings.removeFromRoom, modifier = Modifier.size(18.dp)) }
                }
            }
        }
        if (!s.isHost) {
            item { Text(Strings.suggestHint, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(top = 10.dp)) }
        }
        item {
            OutlinedButton(onClick = { together.leave() }, modifier = Modifier.padding(top = 16.dp)) { Text(Strings.leaveRoom) }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 14.dp, bottom = 2.dp))
}

@Composable
private fun Line(title: String, subtitle: String? = null, dim: Boolean = false, actions: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = if (dim) 0.08f else 0.2f)), contentAlignment = Alignment.Center) {
            Text(title.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dim) 0.5f else 1f))
            subtitle?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)) }
        }
        actions()
    }
}

private fun problemText(s: TogetherSession.State): String? = when (s.problem) {
    null -> null
    TogetherSession.Problem.UNREACHABLE -> Strings.togetherUnreachable
    TogetherSession.Problem.ROOM_NOT_FOUND -> Strings.togetherNoRoom
    TogetherSession.Problem.REJECTED -> Strings.togetherRejected
    TogetherSession.Problem.KICKED -> Strings.togetherKicked
    TogetherSession.Problem.ROOM_LOST -> Strings.togetherLost
    TogetherSession.Problem.ROOM_FULL -> Strings.togetherFull
    TogetherSession.Problem.NOT_ALLOWED -> Strings.togetherNotAllowed
    TogetherSession.Problem.OTHER -> Strings.togetherOther(s.problemDetail)
}

private fun noticeText(notice: TogetherSession.Notice?): String? = when (notice) {
    null -> null
    TogetherSession.Notice.SUGGESTION_SENT -> Strings.suggestionSent
    TogetherSession.Notice.SUGGESTION_ADDED -> Strings.suggestionAdded
    TogetherSession.Notice.SUGGESTION_DECLINED -> Strings.suggestionDeclined
}

/** The server cuts names at 50 characters. */
private const val MAX_NAME = 50

private fun clipboardText(): String? = runCatching {
    Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
}.getOrNull()

private fun copyToClipboard(text: String) {
    runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) }
}
