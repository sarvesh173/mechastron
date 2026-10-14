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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ─────────────────────────────────────────────────────────────
 * Media tab
 * ───────────────────────────────────────────────────────────── */

@Composable
internal fun MediaTabContent(
    mediaName: String?,
    cropMode: MediaCropMode,
    onCropModeChange: (MediaCropMode) -> Unit,
    onSelectMediaClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // selected_media — "Select media" button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(StudioPalette.S10)
                .border(1.dp, StudioPalette.Stroke, RoundedCornerShape(5.dp))
                .clickable { onSelectMediaClick() },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Media",
                    tint = StudioPalette.TextDim,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = mediaName ?: "Select media",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = if (mediaName != null) StudioPalette.W1 else StudioPalette.TextDim,
                )
            }
        }

        // mediaCropButtons: Fill / Fit / Stretch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CropModeButton(
                label = "Fill",
                isSelected = cropMode == MediaCropMode.FILL,
                onClick = { onCropModeChange(MediaCropMode.FILL) },
                modifier = Modifier.weight(1f),
            )
            CropModeButton(
                label = "Fit",
                isSelected = cropMode == MediaCropMode.FIT,
                onClick = { onCropModeChange(MediaCropMode.FIT) },
                modifier = Modifier.weight(1f),
            )
            CropModeButton(
                label = "Stretch",
                isSelected = cropMode == MediaCropMode.STRETCH,
                onClick = { onCropModeChange(MediaCropMode.STRETCH) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CropModeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) StudioPalette.S10 else StudioPalette.S9)
            .border(
                width = 1.dp,
                color = if (isSelected) StudioPalette.Accent else StudioPalette.Stroke,
                shape = RoundedCornerShape(6.dp),
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (isSelected) StudioPalette.Accent else StudioPalette.W1,
        )
    }
}

/* ─────────────────────────────────────────────────────────────
 * Intrinsic tab placeholder
 * ───────────────────────────────────────────────────────────── */

@Composable
internal fun IntrinsicTabPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StudioPalette.S9)
            .border(1.dp, StudioPalette.Stroke, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Intrinsic",
                tint = StudioPalette.TextDim,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Intrinsic Color — layer source color",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = StudioPalette.TextDim,
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────
 * Utility
 * ───────────────────────────────────────────────────────────── */

private data class TabItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

internal fun colorToHex(color: Color): String {
    val argb = color.hashCode()
    return String.format("#%08X", argb.toLong() and 0xFFFFFFFF)
}

enum class MediaCropMode {
    FILL,
    FIT,
    STRETCH,
}
