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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ─────────────────────────────────────────────────────────────
 * Start / End cap shape picker
 * ───────────────────────────────────────────────────────────── */

@Composable
internal fun ShapeCapPicker(
    label: String,
    selected: ShapeCap,
    onSelect: (ShapeCap) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = StudioPalette.W1a30),
        )
        Spacer(Modifier.height(4.dp))
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = StudioPalette.S10),
        ) {
            Row(
                modifier = Modifier.padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                ShapeCap.entries.forEach { cap ->
                    val isSelected = cap == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) StudioPalette.SurfaceRaised
                                else Color.Transparent
                            )
                            .clickable { onSelect(cap) }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = cap.icon,
                            contentDescription = cap.displayName,
                            tint = if (isSelected) StudioPalette.Accent else StudioPalette.TextDim,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
 * Shadow settings placeholder (shadowList RecyclerView stub)
 * ───────────────────────────────────────────────────────────── */

@Composable
internal fun ShadowSettingsPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = StudioPalette.SurfaceRaised),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(StudioPalette.Bl1a20),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = StudioPalette.TextDim,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Drop Shadow",
                    style = MaterialTheme.typography.labelMedium.copy(color = StudioPalette.TextPrimary),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "X:0 Y:4 Blur:8",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = StudioPalette.W1a30,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    ),
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
 * Enums — stroke cap / join / shape cap
 * ───────────────────────────────────────────────────────────── */

enum class StrokeCap(val displayName: String) {
    Butt("Butt"),
    Round("Round"),
    Square("Square"),
}

enum class StrokeJoin(val displayName: String) {
    Bevel("Bevel"),
    Round("Round"),
    Miter("Miter"),
}

enum class ShapeCap(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    None("None", Icons.Default.ArrowForward),
    Arrow("Arrow", Icons.Default.ArrowForward),
    Round("Round", Icons.Default.Circle),
    Square("Square", Icons.Default.Square),
}
