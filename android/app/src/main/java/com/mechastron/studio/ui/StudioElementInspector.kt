package com.mechastron.studio.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Exact replica of Alight Motion's fragment_element_edit.xml:
 * Background: A9 (#151515)
 * Row 1: [Speed] [TrimLeft] [Split] [TrimRight] [Audio]
 * Row 2: [Color & Fill] [Border & Shadow] [Blending & Opacity]
 * Row 3: [Move & Transform] [Edit Shape/Points] [Effects]
 */
@Composable
fun StudioElementInspector(
    selectedLayerName: String,
    onSpeedClick: () -> Unit,
    onTrimLeftClick: () -> Unit,
    onSplitClick: () -> Unit,
    onTrimRightClick: () -> Unit,
    onAudioClick: () -> Unit,
    onColorAndFillClick: () -> Unit,
    onBorderAndShadowClick: () -> Unit,
    onBlendingAndOpacityClick: () -> Unit,
    onMoveAndTransformClick: () -> Unit,
    onEditPointsClick: () -> Unit,
    onEffectsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioPalette.Surface)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Row 1: Action row [Speed] [TrimLeft] [Split] [TrimRight] [Audio]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // buttonSpeedControl (ac_ic_speed)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(StudioPalette.ButtonBg)
                    .clickable { onSpeedClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Speed, "Speed", tint = StudioPalette.TextPrimary, modifier = Modifier.size(18.dp))
            }

            // Grouped trim buttons with continuous center styling
            Row(
                modifier = Modifier
                    .weight(3f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(StudioPalette.ButtonBg),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onTrimLeftClick, modifier = Modifier.weight(1f)) {
                    Text("⇤", color = StudioPalette.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Box(modifier = Modifier.size(1.dp, 20.dp).background(StudioPalette.Stroke))
                IconButton(onClick = onSplitClick, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ContentCut, "Split", tint = StudioPalette.TextPrimary, modifier = Modifier.size(16.dp))
                }
                Box(modifier = Modifier.size(1.dp, 20.dp).background(StudioPalette.Stroke))
                IconButton(onClick = onTrimRightClick, modifier = Modifier.weight(1f)) {
                    Text("⇥", color = StudioPalette.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // buttonAudio (ac_ic_audio)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(StudioPalette.ButtonBg)
                    .clickable { onAudioClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.VolumeUp, "Audio", tint = StudioPalette.TextPrimary, modifier = Modifier.size(18.dp))
            }
        }

        // Row 2: [Color & Fill] [Border & Shadow] [Blending & Opacity]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PanelButton(
                title = "Color & Fill",
                icon = Icons.Default.ColorLens,
                iconTint = Color(0xFFE57373),
                onClick = onColorAndFillClick,
                modifier = Modifier.weight(1f),
            )
            PanelButton(
                title = "Border & Shadow",
                icon = Icons.Default.CropSquare,
                iconTint = Color(0xFF64B5F6),
                onClick = onBorderAndShadowClick,
                modifier = Modifier.weight(1f),
            )
            PanelButton(
                title = "Blending & Opacity",
                icon = Icons.Default.Layers,
                iconTint = Color(0xFFBA68C8),
                onClick = onBlendingAndOpacityClick,
                modifier = Modifier.weight(1f),
            )
        }

        // Row 3: [Move & Transform] [Edit Shape / Points] [Effects]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PanelButton(
                title = "Move & Transform",
                icon = Icons.Default.OpenWith,
                iconTint = StudioPalette.Accent,
                onClick = onMoveAndTransformClick,
                modifier = Modifier.weight(1f),
            )
            PanelButton(
                title = "Edit Shape",
                icon = Icons.Default.Edit,
                iconTint = Color(0xFF4DB6AC),
                onClick = onEditPointsClick,
                modifier = Modifier.weight(1f),
            )
            PanelButton(
                title = "Effects",
                icon = Icons.Default.AutoAwesome,
                iconTint = Color(0xFFFFD54F),
                onClick = onEffectsClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PanelButton(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StudioPalette.ButtonBg)
            .border(1.dp, StudioPalette.Stroke, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(19.dp),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 10.sp,
                ),
                color = StudioPalette.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
