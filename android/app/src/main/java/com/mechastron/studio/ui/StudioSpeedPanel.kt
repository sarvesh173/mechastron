package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Inline expandable speed control panel per blueprint.
 * Background: A9, inner S8 rounded-4dp
 * Contains: retimeExtendStart/retimeReduceStart/retimeReduceEnd/retimeExtendEnd placeholders,
 * Slow/Fast icons, Slider, value pill (bg W1a5)
 */
@Composable
fun StudioSpeedPanel(
    modifier: Modifier = Modifier,
) {
    var speedValue by remember { mutableStateOf(1.0f) }
    var showSlowFast by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Row 1: retime controls (4 buttons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SpeedControlButton(
                title = "Extend Start",
                icon = Icons.Default.Remove,
                onClick = { /* retimeExtendStart */ },
            )
            SpeedControlButton(
                title = "Reduce Start",
                icon = Icons.Default.Remove,
                onClick = { /* retimeReduceStart */ },
            )
            SpeedControlButton(
                title = "Reduce End",
                icon = Icons.Default.Add,
                onClick = { /* retimeReduceEnd */ },
            )
            SpeedControlButton(
                title = "Extend End",
                icon = Icons.Default.Add,
                onClick = { /* retimeExtendEnd */ },
            )
        }

        // Row 2: Slow/Fast icons + Slider + Value pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { showSlowFast = !showSlowFast }) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Slow",
                    tint = StudioPalette.W1,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = { showSlowFast = !showSlowFast }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Fast",
                    tint = StudioPalette.W1,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Slider
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Slider(
                    value = speedValue,
                    onValueChange = { speedValue = it },
                    valueRange = 0.1f..4.0f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = StudioPalette.Y1,
                        activeTrackColor = StudioPalette.Y1,
                        inactiveTrackColor = StudioPalette.S9,
                    ),
                )
            }

            // Value pill (bg W1a5)
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(28.dp)
                    .background(StudioPalette.W1a5, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "%.1fx".format(speedValue),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = StudioPalette.W1,
                )
            }
        }
    }
}

@Composable
private fun SpeedControlButton(
    title: String,
    icon: androidx.compose.material.icons.filled.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .background(StudioPalette.S8, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = StudioPalette.W1,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = StudioPalette.W1,
            )
        }
    }
}