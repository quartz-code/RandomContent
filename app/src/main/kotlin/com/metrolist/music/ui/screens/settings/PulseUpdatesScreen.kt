package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.constants.PulseTestBuildsKey
import com.metrolist.music.constants.PulseUpdateCheckKey
import com.metrolist.music.pulse.update.PulseReleases
import com.metrolist.music.pulse.update.PulseUpdater
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch

/**
 * Pulse: "Settings → Pulse → Updates" — which build is installed, whether to look for new ones at
 * start and whether test builds count, and a check right now.
 */
@Composable
fun PulseUpdatesScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var checkOnStart by rememberPreference(PulseUpdateCheckKey, true)
    var testBuilds by rememberPreference(PulseTestBuildsKey, false)
    val state by PulseUpdater.state.collectAsState()
    val build = PulseUpdater.currentBuild

    PulseScreenScaffold(navController, stringResource(R.string.pulse_updates)) {
        IntroCard(stringResource(R.string.pulse_updates_intro))
        Spacer(Modifier.height(16.dp))
        Text(
            if (build > 0) stringResource(R.string.pulse_update_installed, build) else stringResource(R.string.pulse_update_local),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(12.dp))
        SwitchRow(stringResource(R.string.pulse_update_auto), null, checkOnStart) { checkOnStart = it }
        SwitchRow(stringResource(R.string.pulse_update_test), stringResource(R.string.pulse_update_test_desc), testBuilds) { testBuilds = it }
        Spacer(Modifier.height(16.dp))

        when (val now = state) {
            is PulseUpdater.State.Available -> UpdateCard(now.update)
            is PulseUpdater.State.Downloading -> UpdateCard(now.update)
            is PulseUpdater.State.Ready -> UpdateCard(now.update)
            is PulseUpdater.State.Failed -> now.update?.let { UpdateCard(it) }
            else -> {}
        }
        if (state !is PulseUpdater.State.Available && state !is PulseUpdater.State.Downloading && state !is PulseUpdater.State.Ready) {
            val status = when (val now = state) {
                is PulseUpdater.State.Checking -> R.string.pulse_update_checking
                is PulseUpdater.State.UpToDate -> R.string.pulse_update_latest
                is PulseUpdater.State.Failed -> if (now.update == null) R.string.pulse_update_failed else null
                else -> null
            }
            status?.let {
                Text(stringResource(it), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
            }
            OutlinedButton(
                enabled = build > 0 && state !is PulseUpdater.State.Checking,
                onClick = { scope.launch { PulseUpdater.check(testBuilds) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.pulse_update_check)) }
        }
    }
}

@Composable
private fun SwitchRow(title: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** The update on the settings screen: the same words and button as the start-up dialog. */
@Composable
private fun UpdateCard(update: PulseReleases.Update) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.pulse_update_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            UpdateBody(update)
            Spacer(Modifier.height(12.dp))
            InstallButton(update, Modifier.fillMaxWidth())
        }
    }
}

/**
 * Offered at start when a newer build is out. "Later" closes it until the next start; closing it
 * while downloading lets the download finish, and the installer opens when it is done.
 */
@Composable
fun PulseUpdateDialog(update: PulseReleases.Update, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pulse_update_title)) },
        text = { UpdateBody(update) },
        confirmButton = { InstallButton(update) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.pulse_update_later)) } },
    )
}

@Composable
private fun UpdateBody(update: PulseReleases.Update) {
    val state by PulseUpdater.state.collectAsState()
    Column {
        Text(stringResource(R.string.pulse_update_build, update.release.build), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        if (update.notes.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.pulse_update_whats_new), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                update.notes.forEach { note ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("•  ", style = MaterialTheme.typography.bodyMedium)
                        Text(note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        when (val now = state) {
            is PulseUpdater.State.Downloading -> {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { now.progress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.pulse_update_downloading, (now.progress * 100).toInt()), style = MaterialTheme.typography.bodySmall)
            }
            is PulseUpdater.State.Ready -> {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.pulse_update_ready), style = MaterialTheme.typography.bodySmall)
            }
            is PulseUpdater.State.Failed -> {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.pulse_update_download_failed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            else -> {}
        }
    }
}

@Composable
private fun InstallButton(update: PulseReleases.Update, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state by PulseUpdater.state.collectAsState()
    val label = when (state) {
        is PulseUpdater.State.Ready -> R.string.pulse_update_install_now
        is PulseUpdater.State.Failed -> R.string.pulse_update_retry
        else -> R.string.pulse_update_install
    }
    Button(
        enabled = state !is PulseUpdater.State.Downloading,
        onClick = { PulseUpdater.install(context, update) },
        modifier = modifier,
    ) { Text(stringResource(label)) }
}
