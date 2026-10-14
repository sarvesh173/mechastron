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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ─────────────────────────────────────────────────────────────
 * Gradation tab
 * ───────────────────────────────────────────────────────────── */

@Composable
internal fun GradationTabContent(
    gradationType: Int,
    startColor: Color,
    endColor: Color,
    onTypeChange: (Int) -> Unit,
    onStartColorChange: (Color) -> Unit,
    onEndColorChange: (Color) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // gradation_type1 / type2 / type3 buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (type in 1..3) {
                GradationTypeButton(
                    type = type,
                    isSelected = gradationType == type,
                    onClick = { onTypeChange(type) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // gradation_bar — gradient preview bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(startColor, endColor),
                    ),
                )
                .border(1.dp, StudioPalette.Stroke, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Gradation Bar",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = StudioPalette.W1a30,
            )
        }

        // Start / End color circles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // color_start circle
            ColorCircle(
                label = "Start",
                color = startColor,
                onClick = { onStartColorChange(startColor) },
                modifier = Modifier.weight(1f),
            )
            // color_end circle
            ColorCircle(
                label = "End",
                color = endColor,
                onClick = { onEndColorChange(endColor) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GradationTypeButton(
    type: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (type) {
        1 -> "Type 1"
        2 -> "Type 2"
        else -> "Type 3"
    }

    Box(
        modifier = modifier
            .height(32.dp)
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

@Composable
private fun ColorCircle(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, StudioPalette.W1a30, CircleShape)
                .clickable { onClick() },
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = StudioPalette.W1a30,
            )
            Text(
                text = colorToHex(color),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                ),
                color = StudioPalette.W1,
            )
        }
    }
}
