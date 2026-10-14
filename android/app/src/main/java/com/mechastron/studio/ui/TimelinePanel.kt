package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TimelineLayer(
    val id: String,
    val name: String,
    val type: LayerType,
    val startMs: Long,
    val durationMs: Long,
    val hasKeyframes: Boolean,
    val hasMask: Boolean,
)

enum class LayerType {
    SHAPE, MEDIA, AUDIO, TEXT, CAMERA, NULL
}

/**
 * Replica of activity_edit_timeline.xml:
 * - Height: 280dp
 * - Rows: 28dp each
 * - Grip zones: 35dp wide left/right with 30dp trim grip
 * - 6 gradient colors cycling per ElementGrad1..6 in StudioPalette
 */
@Composable
fun TimelinePanel(
    layers: List<TimelineLayer>,
    selectedId: String? = null,
    playheadMs: Long = 0L,
    modifier: Modifier = Modifier,
    onLayerClick: (String) -> Unit = {},
    onTrimLeftDrag: (String, Float) -> Unit = { _, _ -> },
    onTrimRightDrag: (String, Float) -> Unit = { _, _ -> },
    onVisibilityToggle: (String, Boolean) -> Unit = { _, _ -> },
) {
    Column(modifier = modifier.height(280.dp)) {
        // Timeline header
        TimelineHeader()

        // Layer rows
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            content = {
                items(layers) { layer ->
                    TimelineRow(
                        layer = layer,
                        isSelected = layer.id == selectedId,
                        playheadMs = playheadMs,
                        onLayerClick = onLayerClick,
                        onTrimLeftDrag = onTrimLeftDrag,
                        onTrimRightDrag = onTrimRightDrag,
                        onVisibilityToggle = onVisibilityToggle,
                    )
                }
            },
        )
    }
}

@Composable
private fun TimelineHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(StudioPalette.S9),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .width(70.dp)
                .height(28.dp)
                .background(StudioPalette.A11),
        ) {
            Text(
                text = "Layers",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = StudioPalette.TextDim,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        // Time ruler placeholder
        Box(
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
                .background(StudioPalette.S9),
        ) {
            // Time markers would go here in full implementation
            Text(
                text = "0:00:00",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = StudioPalette.TextDim,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun TimelineRow(
    layer: TimelineLayer,
    isSelected: Boolean,
    playheadMs: Long,
    onLayerClick: (String) -> Unit,
    onTrimLeftDrag: (String, Float) -> Unit,
    onTrimRightDrag: (String, Float) -> Unit,
    onVisibilityToggle: (String, Boolean) -> Unit,
) {
    val gradientColors = getGradientForLayer(layer)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(StudioPalette.S9)
                .clickable { onLayerClick(layer.id) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Layer info column (left 70dp)
            TimelineLayerInfo(
                layer = layer,
                isSelected = isSelected,
                onVisibilityToggle = onVisibilityToggle,
            )

            // Element frame (gradient body)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 4.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = gradientColors,
                        ),
                    )
                    .then(
                        if (isSelected) Modifier.border(1.5.dp, StudioPalette.Y1, RoundedCornerShape(1.5.dp))
                        else Modifier,
                    ),
            ) {
                // Keyframe diamonds
                if (layer.hasKeyframes) {
                    KeyframeMarkers(playheadMs = playheadMs)
                }
            }

            // Trim grips (35dp each side)
            TrimGrip(
                side = TrimGripSide.Left,
                layerId = layer.id,
                onDrag = { delta -> onTrimLeftDrag(layer.id, delta) },
            )
            TrimGrip(
                side = TrimGripSide.Right,
                layerId = layer.id,
                onDrag = { delta -> onTrimRightDrag(layer.id, delta) },
            )
        }
    }
}

@Composable
private fun TimelineLayerInfo(
    layer: TimelineLayer,
    isSelected: Boolean,
    onVisibilityToggle: (String, Boolean) -> Unit = { _, _ -> },
) {
    Box(
        modifier = Modifier
            .width(70.dp)
            .height(28.dp)
            .background(if (isSelected) StudioPalette.AddPopupSideTabBgSel else StudioPalette.A11),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = layerTypeIcon(layer.type),
                contentDescription = layer.name,
                tint = StudioPalette.TextPrimary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = layer.name,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = StudioPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (layer.hasMask) {
                Icon(
                    imageVector = Icons.Default.CropSquare,
                    contentDescription = "Mask",
                    tint = StudioPalette.TextDim,
                    modifier = Modifier.size(12.dp),
                )
            }
            IconButton(
                onClick = { onVisibilityToggle(layer.id, !isSelected) },
                modifier = Modifier.size(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Toggle visibility",
                    tint = StudioPalette.TextDim,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun KeyframeMarkers(playheadMs: Long) {
    // Simplified keyframe representation - in real implementation would be based on layer data
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (playheadMs % 1000 < 500) {
            // Draw diamond shape for keyframe
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(StudioPalette.KeyframeMarker),
            )
        }
    }
}

private enum class TrimGripSide { Left, Right }

@Composable
private fun TrimGrip(
    side: TrimGripSide,
    layerId: String,
    onDrag: (Float) -> Unit,
) {
    val gripWidth = 35.dp
    val trimGripWidth = 30.dp
    
    Box(
        modifier = Modifier
            .width(gripWidth)
            .height(28.dp)
            .background(if (side == TrimGripSide.Left) StudioPalette.AddPopupSideTabBg else StudioPalette.AddPopupSideTabBg)
            .clickable(enabled = false) {},
        contentAlignment = if (side == TrimGripSide.Left) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        // 30dp trim grip indicator
        Box(
            modifier = Modifier
                .width(trimGripWidth)
                .height(28.dp)
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable { /* Trim action */ },
        ) {
            // Grip handle visual
            Icon(
                imageVector = if (side == TrimGripSide.Left) Icons.Default.Layers else Icons.Default.Layers,
                contentDescription = "Trim grip",
                tint = StudioPalette.TextDim,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.Center),
            )
        }
    }
}

private fun getGradientForLayer(layer: TimelineLayer): List<Color> {
    // Cycle through 6 gradient pairs based on layer index
    return when (layer.id.hashCode() % 6) {
        0 -> listOf(StudioPalette.ElementGrad1Start, StudioPalette.ElementGrad1End)
        1 -> listOf(StudioPalette.ElementGrad2Start, StudioPalette.ElementGrad2End)
        2 -> listOf(StudioPalette.ElementGrad3Start, StudioPalette.ElementGrad3End)
        3 -> listOf(StudioPalette.ElementGrad4Start, StudioPalette.ElementGrad4End)
        4 -> listOf(StudioPalette.ElementGrad5Start, StudioPalette.ElementGrad5End)
        else -> listOf(StudioPalette.ElementGrad6Start, StudioPalette.ElementGrad6End)
    }
}

private fun layerTypeIcon(type: LayerType): ImageVector = when (type) {
    LayerType.SHAPE -> Icons.Default.CropSquare
    LayerType.MEDIA -> Icons.Default.Layers
    LayerType.AUDIO -> Icons.Default.Layers
    LayerType.TEXT -> Icons.Default.Layers
    LayerType.CAMERA -> Icons.Default.Layers
    LayerType.NULL -> Icons.Default.Layers
}