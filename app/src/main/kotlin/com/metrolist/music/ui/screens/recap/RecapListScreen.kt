package com.metrolist.music.ui.screens.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.pulse.recap.RecapEntry
import com.metrolist.music.pulse.recap.RecapPeriod
import com.metrolist.music.pulse.recap.RecapRepository
import com.metrolist.music.ui.player.LocalPulseArtworkColor
import com.metrolist.music.ui.player.PlayerTheme
import com.metrolist.music.ui.player.PulsePlayerBackground
import com.metrolist.music.ui.player.baseBackground
import com.metrolist.music.ui.player.pulsePalette
import com.metrolist.music.ui.player.rememberPlayerTheme
import com.metrolist.music.ui.screens.settings.IntroCard
import com.metrolist.music.ui.screens.settings.PulseScreenScaffold

/** Pulse: every month and year with enough music for a recap; a year card is drawn bigger. */
@Composable
fun RecapListScreen(navController: NavController) {
    val database = LocalDatabase.current
    val entries by produceState<List<RecapEntry>?>(null) { value = runCatching { RecapRepository.entries(database) }.getOrDefault(emptyList()) }
    val theme = rememberPlayerTheme()
    PulseScreenScaffold(navController, stringResource(R.string.recap_title)) {
        IntroCard(stringResource(R.string.recap_list_intro))
        Spacer(Modifier.height(16.dp))
        val list = entries
        when {
            list == null -> Unit
            list.isEmpty() -> Text(
                stringResource(R.string.recap_list_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> list.forEach { entry ->
                if (entry.period.isYear) {
                    YearCard(entry, theme) { navController.navigate("pulse/recap/${entry.period.key}") }
                } else {
                    MonthRow(entry) { navController.navigate("pulse/recap/${entry.period.key}") }
                }
            }
        }
    }
}

@Composable
private fun YearCard(entry: RecapEntry, theme: PlayerTheme, onClick: () -> Unit) {
    CompositionLocalProvider(LocalPulseArtworkColor provides null) {
        val p = pulsePalette(theme)
        Box(
            Modifier
                .padding(top = 12.dp, bottom = 8.dp)
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.baseBackground)
                .clickable(onClick = onClick),
        ) {
            PulsePlayerBackground(theme, emptyList(), null)
            Column(Modifier.padding(20.dp).align(Alignment.BottomStart)) {
                Text(periodTitle(entry.period), style = MaterialTheme.typography.headlineMedium, color = p.content)
                Text(hoursText(entry.totalMs), style = MaterialTheme.typography.bodyMedium, color = p.accent)
            }
        }
    }
}

@Composable
private fun MonthRow(entry: RecapEntry, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Column(Modifier.weight(1f)) {
                Text(periodTitle(entry.period), style = MaterialTheme.typography.titleMedium)
                Text(hoursText(entry.totalMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Icon(painterResource(R.drawable.navigate_next), contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

/** Shown once when the app opens in a new month: "October's recap is ready". */
@Composable
fun RecapOfferDialog(period: RecapPeriod, onOpen: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.stats), contentDescription = null) },
        title = { Text(stringResource(R.string.recap_offer_title, periodTitle(period))) },
        text = { Text(stringResource(R.string.recap_offer_text)) },
        confirmButton = { TextButton(onClick = onOpen) { Text(stringResource(R.string.recap_offer_open)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.recap_offer_later)) } },
    )
}
