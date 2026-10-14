package com.mechastron.studio.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mechastron.studio.ui.StudioPalette

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BlendingSheet(
    layerName: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMode by remember { mutableStateOf("Normal") }
    var opacity by remember { mutableFloatStateOf(100f) }

    val blendModes = listOf(
        "Normal", "Darken", "Multiply", "Color Burn",
        "Lighten", "Screen", "Color Dodge", "Linear Dodge",
        "Overlay", "Soft Light", "Hard Light", "Difference", "Exclusion",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioPalette.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Blending & Opacity · $layerName",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = StudioPalette.TextPrimary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Opacity Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Opacity", style = MaterialTheme.typography.labelMedium, color = StudioPalette.TextDim)
                Text("${opacity.toInt()}%", style = MaterialTheme.typography.labelMedium, color = StudioPalette.Accent)
            }
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = StudioPalette.Accent,
                    activeTrackColor = StudioPalette.Accent,
                    inactiveTrackColor = StudioPalette.SurfaceRaised,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Blend Mode", style = MaterialTheme.typography.labelMedium, color = StudioPalette.TextDim)
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                blendModes.forEach { mode ->
                    val isSelected = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) StudioPalette.Accent else StudioPalette.SurfaceRaised)
                            .clickable { selectedMode = mode }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = mode,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                            ),
                            color = if (isSelected) StudioPalette.Backdrop else StudioPalette.TextPrimary,
                        )
                    }
                }
            }
        }
    }
}
