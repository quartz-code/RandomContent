package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.constants.AppStyle
import com.metrolist.music.ui.theme.MetrolistTheme

private val AppStyle.label: Int
    get() = when (this) {
        AppStyle.RADIO -> R.string.app_style_radio
        AppStyle.CLASSIC -> R.string.app_style_classic
        AppStyle.TERMINAL -> R.string.app_style_terminal
    }

private val AppStyle.description: Int
    get() = when (this) {
        AppStyle.RADIO -> R.string.app_style_radio_desc
        AppStyle.CLASSIC -> R.string.app_style_classic_desc
        AppStyle.TERMINAL -> R.string.app_style_terminal_desc
    }

@Composable
fun AppStylePicker(
    selected: AppStyle,
    darkTheme: Boolean,
    onSelect: (AppStyle) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.app_style),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppStyle.entries.forEach { style ->
                AppStyleCard(
                    style = style,
                    selected = style == selected,
                    darkTheme = darkTheme,
                    onClick = { onSelect(style) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = stringResource(selected.description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AppStyleCard(
    style: AppStyle,
    selected: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outerShape = RoundedCornerShape(14.dp)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .clip(outerShape)
            .border(if (selected) 2.dp else 1.dp, borderColor, outerShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // A miniature screen drawn with the style itself.
        MetrolistTheme(darkTheme = darkTheme, style = style) {
            val colors = MaterialTheme.colorScheme
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.background)
                    .padding(8.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onBackground,
                        maxLines = 1,
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.8f)
                            .height(6.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(colors.surfaceContainerHighest),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .width(28.dp)
                                .height(12.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(colors.primary),
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.tertiary),
                        )
                    }
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(colors.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(style.label),
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
        )
    }
}
