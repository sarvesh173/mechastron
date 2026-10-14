package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Visibility
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

@Composable
fun StudioBottomToolbar(
    selectedLayerName: String?,
    onColorAndFillClick: () -> Unit,
    onBorderAndShadowClick: () -> Unit,
    onBlendingAndOpacityClick: () -> Unit,
    onMoveAndTransformClick: () -> Unit,
    onEditPointsClick: () -> Unit,
    onEffectsClick: () -> Unit,
    onSplitClick: () -> Unit = {},
    onTrimLeftClick: () -> Unit = {},
    onTrimRightClick: () -> Unit = {},
    onDuplicateClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onToggleVisibilityClick: () -> Unit = {},
    onToggleLockClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioPalette.Surface)
            .padding(vertical = 4.dp),
    ) {
        // Quick Layer Actions Row (Split, Trim, Duplicate, Delete)
        if (selectedLayerName != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedLayerName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = StudioPalette.Accent,
                    ),
                    maxLines = 1,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSplitClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCut, "Split Layer", tint = StudioPalette.TextDim, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDuplicateClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, "Duplicate", tint = StudioPalette.TextDim, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onToggleVisibilityClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Visibility, "Visibility", tint = StudioPalette.TextDim, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onToggleLockClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Lock, "Lock", tint = StudioPalette.TextDim, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, "Delete", tint = StudioPalette.Playhead, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        // The Primary 6 Motion Inspector Tiles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InspectorTile(
                title = "Color & Fill",
                icon = Icons.Default.ColorLens,
                color = Color(0xFFE57373),
                onClick = onColorAndFillClick,
            )
            InspectorTile(
                title = "Border & Shadow",
                icon = Icons.Default.CropSquare,
                color = Color(0xFF64B5F6),
                onClick = onBorderAndShadowClick,
            )
            InspectorTile(
                title = "Blending & Opacity",
                icon = Icons.Default.Layers,
                color = Color(0xFFBA68C8),
                onClick = onBlendingAndOpacityClick,
            )
            InspectorTile(
                title = "Move & Transform",
                icon = Icons.Default.OpenWith,
                color = Color(0xFFFFB74D),
                onClick = onMoveAndTransformClick,
            )
            InspectorTile(
                title = "Edit Points",
                icon = Icons.Default.Edit,
                color = Color(0xFF4DB6AC),
                onClick = onEditPointsClick,
            )
            InspectorTile(
                title = "Effects",
                icon = Icons.Default.AutoAwesome,
                color = Color(0xFFFFD54F),
                onClick = onEffectsClick,
            )
        }
    }
}

@Composable
private fun InspectorTile(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(108.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StudioPalette.SurfaceRaised)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 11.sp,
                ),
                color = StudioPalette.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
