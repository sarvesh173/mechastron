package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ─────────────────────────────────────────────────────────────
 * Effect settings holder
 * Header: effect icon + settingsEffectName + buttonFxOverflow + buttonDelete
 * + effectSettingsList (parameter rows — placeholder)
 * ───────────────────────────────────────────────────────────── */

@Composable
internal fun EffectSettingsHolder(
    effect: EffectItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        /* ── Settings header row ────────────────────────────────── */
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = StudioPalette.SurfaceRaised),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Effect icon
                Icon(
                    imageVector = effect.icon,
                    contentDescription = null,
                    tint = StudioPalette.Accent,
                    modifier = Modifier.size(18.dp),
                )

                // Effect name
                Text(
                    text = effect.name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = StudioPalette.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.weight(1f),
                )

                // buttonFxOverflow (MoreVert icon button)
                IconButton(
                    onClick = { /* TODO: Show overflow menu */ },
                    modifier = Modifier.size(26.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Effect Options",
                        tint = StudioPalette.W1a30,
                        modifier = Modifier.size(16.dp),
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Effect",
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        /* ── Parameter settings list placeholder ────────────────── */
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                EffectParameterRow(label = "Intensity", value = "75%", min = 0f, max = 100f, onValueChange = {})
            }
            item {
                EffectParameterRow(label = "Radius", value = "4.0", min = 0f, max = 50f, onValueChange = {})
            }
            item {
                EffectParameterRow(label = "Blend Mode", value = "Normal", min = null, max = null, onValueChange = {})
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
 * Individual parameter row inside effect settings
 * Matches effectSettingsList row pattern: label + slider + value pill
 * ───────────────────────────────────────────────────────────── */

@Composable
private fun EffectParameterRow(
    label: String,
    value: String,
    min: Float?,
    max: Float?,
    onValueChange: (Float) -> Unit,
) {
    var sliderValue by remember { mutableStateOf(value) }

    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = StudioPalette.S10),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(color = StudioPalette.W1a30),
                )
                // Value pill: bl1@20 rounded rect
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StudioPalette.Bl1a20)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = StudioPalette.TextPrimary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                        ),
                    )
                }
            }

            // Slider only shown when min/max are provided
            if (min != null && max != null) {
                androidx.compose.material3.Slider(
                    value = sliderValue.toFloatOrNull() ?: ((min + max) / 2f),
                    onValueChange = {
                        sliderValue = "%.0f".format(it)
                        onValueChange(it)
                    },
                    valueRange = min..max,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = StudioPalette.Accent,
                        activeTrackColor = StudioPalette.Accent,
                        inactiveTrackColor = StudioPalette.SurfaceRaised,
                    ),
                )
            }
        }
    }
}
