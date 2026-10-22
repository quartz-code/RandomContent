package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.constants.PlayerDesign
import com.metrolist.music.ui.player.palette

private val PlayerDesign.label: Int
    get() = when (this) {
        PlayerDesign.CLEAN -> R.string.player_design_clean
        PlayerDesign.RADIO -> R.string.player_design_radio
        PlayerDesign.SOFT -> R.string.player_design_soft
    }

private val PlayerDesign.description: Int
    get() = when (this) {
        PlayerDesign.CLEAN -> R.string.player_design_clean_desc
        PlayerDesign.RADIO -> R.string.player_design_radio_desc
        PlayerDesign.SOFT -> R.string.player_design_soft_desc
    }

/** Pulse: the three player designs, each shown as a tiny player so the choice needs no guessing. */
@Composable
fun PlayerDesignPicker(
    selected: PlayerDesign,
    onSelect: (PlayerDesign) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.player_design),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Clean first: it is the default.
            PlayerDesign.entries.forEach { design ->
                PlayerDesignCard(
                    design = design,
                    selected = design == selected,
                    onClick = { onSelect(design) },
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
private fun PlayerDesignCard(
    design: PlayerDesign,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outerShape = RoundedCornerShape(14.dp)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(outerShape)
            .border(if (selected) 2.dp else 1.dp, borderColor, outerShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(156.dp)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            Canvas(Modifier.matchParentSize()) { drawMiniPlayer(design) }
            if (selected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Text(
            text = stringResource(design.label),
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
        )
    }
}

/** A thumbnail-sized sketch of the player: artwork, title, the design's progress and its buttons. */
private fun DrawScope.drawMiniPlayer(design: PlayerDesign) {
    val p = design.palette
    val needle = Color(0xFFE0482F)
    when (design) {
        PlayerDesign.CLEAN -> drawRect(Brush.verticalGradient(0f to Color(0xFF2E3747), 0.75f to Color(0xFF101215)))
        PlayerDesign.RADIO -> drawRect(Brush.verticalGradient(0f to Color(0xFF22261D), 0.5f to Color(0xFF121310)))
        PlayerDesign.SOFT -> drawRect(Brush.verticalGradient(0f to Color(0xFFDCE4EF), 0.7f to Color(0xFFF7F5F2)))
    }
    val w = size.width
    val pad = w * when (design) {
        PlayerDesign.CLEAN -> 0.12f
        PlayerDesign.RADIO -> 0.16f
        PlayerDesign.SOFT -> 0.14f
    }
    // Capped by height so wide screens keep the sketch inside the card.
    val art = minOf(w - pad * 2, size.height * 0.52f)
    val left = (w - art) / 2
    val artTop = 10.dp.toPx()
    val corner = when (design) {
        PlayerDesign.CLEAN -> 6.dp.toPx()
        PlayerDesign.RADIO -> 3.dp.toPx()
        PlayerDesign.SOFT -> 8.dp.toPx()
    }
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFFC9D6EA),
            0.55f to Color(0xFF7C93B8),
            1f to Color(0xFF34548A),
            startY = artTop,
            endY = artTop + art,
        ),
        topLeft = Offset(left, artTop),
        size = Size(art, art),
        cornerRadius = CornerRadius(corner),
    )

    var y = artTop + art + 8.dp.toPx()
    val inner = left
    val innerWidth = w - inner * 2
    // The light design centres its title.
    val titleX = if (design == PlayerDesign.SOFT) inner + innerWidth * 0.15f else inner
    val artistX = if (design == PlayerDesign.SOFT) inner + innerWidth * 0.29f else inner
    drawRoundRect(p.content, Offset(titleX, y), Size(innerWidth * 0.7f, 5.dp.toPx()), CornerRadius(2.dp.toPx()))
    drawRoundRect(
        if (design == PlayerDesign.RADIO) p.accent else p.muted,
        Offset(artistX, y + 8.dp.toPx()),
        Size(innerWidth * 0.42f, 3.dp.toPx()),
        CornerRadius(1.5.dp.toPx()),
    )

    y += 22.dp.toPx()
    val x = inner + innerWidth * 0.35f
    when (design) {
        PlayerDesign.CLEAN, PlayerDesign.SOFT -> {
            drawRoundRect(p.track, Offset(inner, y), Size(innerWidth, 2.dp.toPx()), CornerRadius(1.dp.toPx()))
            drawRoundRect(p.accent, Offset(inner, y), Size(x - inner, 2.dp.toPx()), CornerRadius(1.dp.toPx()))
            drawCircle(p.accent, 3.dp.toPx(), Offset(x, y + 1.dp.toPx()))
        }
        PlayerDesign.RADIO -> {
            var tick = inner
            var i = 0
            while (tick <= inner + innerWidth) {
                drawRect(
                    if (tick <= x) p.accent else p.tick,
                    Offset(tick, y - 2.dp.toPx()),
                    Size(1f, (if (i % 5 == 0) 6 else 3).dp.toPx()),
                )
                tick += 3.dp.toPx()
                i++
            }
            drawRect(needle, Offset(x - 0.75.dp.toPx(), y - 4.dp.toPx()), Size(1.5.dp.toPx(), 10.dp.toPx()))
        }
    }

    y += 18.dp.toPx()
    val cx = w / 2
    when (design) {
        PlayerDesign.CLEAN, PlayerDesign.SOFT -> {
            drawCircle(p.play, 9.dp.toPx(), Offset(cx, y))
            drawCircle(p.content.copy(alpha = 0.7f), 3.dp.toPx(), Offset(cx - 20.dp.toPx(), y))
            drawCircle(p.content.copy(alpha = 0.7f), 3.dp.toPx(), Offset(cx + 20.dp.toPx(), y))
        }
        PlayerDesign.RADIO -> {
            drawCircle(p.panel, 11.dp.toPx(), Offset(cx, y))
            drawCircle(p.play, 8.dp.toPx(), Offset(cx, y))
            drawCircle(p.content.copy(alpha = 0.7f), 3.dp.toPx(), Offset(cx - 22.dp.toPx(), y))
            drawCircle(p.content.copy(alpha = 0.7f), 3.dp.toPx(), Offset(cx + 22.dp.toPx(), y))
        }
    }
}
