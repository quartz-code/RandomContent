package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.db.entities.Song
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.StepGuide
import com.metrolist.music.ui.menu.AddToPlaylistDialogOnline
import com.metrolist.music.ui.menu.LoadingScreen
import com.metrolist.music.ui.menu.YandexImportDialog
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.viewmodels.BackupRestoreViewModel

// Pulse: own screens for the Telegram cloud and the Yandex import, reachable from the top of
// Settings and from the library, so nobody has to know they live under Backup & restore.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PulseScreenScaffold(
    navController: NavController,
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        content()
        Spacer(Modifier.height(24.dp))
    }

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}

@Composable
private fun IntroCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun TelegramCloudScreen(
    navController: NavController,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    PulseScreenScaffold(navController, stringResource(R.string.telegram_cloud)) {
        IntroCard(stringResource(R.string.telegram_intro))
        Spacer(Modifier.height(16.dp))
        TelegramCloudSettings(telegramCloud = viewModel.telegramCloud)
    }
}

@Composable
fun YandexImportScreen(navController: NavController) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var importedTitle by remember { mutableStateOf("") }
    val importedSongs = remember { mutableStateListOf<Song>() }
    var showChoosePlaylist by rememberSaveable { mutableStateOf(false) }
    var isProgressStarted by rememberSaveable { mutableStateOf(false) }
    var progressPercentage by rememberSaveable { mutableIntStateOf(0) }
    var currentSong by remember { mutableStateOf("") }

    PulseScreenScaffold(navController, stringResource(R.string.yandex_import)) {
        IntroCard(stringResource(R.string.yandex_intro))
        Spacer(Modifier.height(16.dp))
        StepGuide(
            title = stringResource(R.string.guide_how_to_connect),
            steps = listOf(
                stringResource(R.string.yandex_setup_1),
                stringResource(R.string.yandex_setup_2),
                stringResource(R.string.yandex_setup_3),
                stringResource(R.string.yandex_setup_4),
                stringResource(R.string.yandex_setup_5),
            ),
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.playlist_add), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.yandex_import_start))
        }
        Spacer(Modifier.height(24.dp))
        StepGuide(
            title = stringResource(R.string.guide_good_to_know),
            steps = listOf(
                stringResource(R.string.yandex_note_1),
                stringResource(R.string.yandex_note_2),
                stringResource(R.string.yandex_note_3),
            ),
            numbered = false,
        )
    }

    if (showDialog) {
        YandexImportDialog(
            showGuide = false,
            onDismiss = { showDialog = false },
            onLoaded = { title, songs ->
                showDialog = false
                importedTitle = title
                importedSongs.clear()
                importedSongs.addAll(songs)
                showChoosePlaylist = true
            },
        )
    }

    AddToPlaylistDialogOnline(
        isVisible = showChoosePlaylist,
        allowSyncing = false,
        initialTextFieldValue = importedTitle,
        songs = importedSongs,
        onDismiss = { showChoosePlaylist = false },
        onProgressStart = { isProgressStarted = it },
        onPercentageChange = { progressPercentage = it },
        onSongChange = { currentSong = it },
    )

    LoadingScreen(
        isVisible = isProgressStarted,
        value = progressPercentage,
        songTitle = currentSong,
    )
}
