package app.pulse.player.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pulse.player.player.AudioFx

private fun hzLabel(hz: Int) = if (hz >= 1000) "${"%.1f".format(hz / 1000f).removeSuffix(".0")} кГц" else "$hz Гц"

@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val fx by AudioFx.state.collectAsState()
    var selectedPreset by remember { mutableIntStateOf(-1) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
            Text("Эквалайзер", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Switch(checked = fx.enabled, onCheckedChange = AudioFx::setEnabled, modifier = Modifier.padding(end = 12.dp))
        }

        if (!fx.available) {
            Text(
                "Эквалайзер появится, когда начнётся воспроизведение. Если не появляется — телефон его не поддерживает.",
                Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        if (fx.presets.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                fx.presets.forEachIndexed { i, name ->
                    FilterChip(
                        selected = selectedPreset == i,
                        enabled = fx.enabled,
                        onClick = { selectedPreset = i; AudioFx.usePreset(i) },
                        label = { Text(name) },
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                fx.bands.forEach { band ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(hzLabel(band.centerHz), Modifier.width(72.dp), style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = band.level.toFloat(),
                            onValueChange = { selectedPreset = -1; AudioFx.setBand(band.index, it.toInt()) },
                            valueRange = fx.minLevel.toFloat()..fx.maxLevel.toFloat(),
                            enabled = fx.enabled,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "%+.0f дБ".format(band.level / 100f),
                            Modifier.width(56.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (fx.bassSupported) {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Усиление басов", fontWeight = FontWeight.Medium)
                    Slider(
                        value = fx.bassStrength.toFloat(),
                        onValueChange = { AudioFx.setBass(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = fx.enabled,
                    )
                }
            }
        }

        Spacer(Modifier.padding(8.dp))
        TextButton(onClick = { selectedPreset = -1; AudioFx.reset() }, Modifier.padding(horizontal = 8.dp)) {
            Text("Сбросить всё")
        }
    }
}
