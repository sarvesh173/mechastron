package com.mechastron.studio.ui.sheets

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mechastron.studio.ui.StudioPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransformSheet(
    layerName: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var posX by remember { mutableFloatStateOf(0f) }
    var posY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(100f) }
    var rotation by remember { mutableFloatStateOf(0f) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Move & Transform · $layerName",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = StudioPalette.TextPrimary,
                )
                IconButton(onClick = {
                    posX = 0f
                    posY = 0f
                    scale = 100f
                    rotation = 0f
                }) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Transform",
                        tint = StudioPalette.TextDim,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Position X & Y Control
            Text("Position (X, Y)", style = MaterialTheme.typography.labelMedium, color = StudioPalette.TextDim)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NumberScrubber("X", posX.toInt()) { posX += it }
                NumberScrubber("Y", posY.toInt()) { posY += it }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scale Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Scale", style = MaterialTheme.typography.labelMedium, color = StudioPalette.TextDim)
                Text("${scale.toInt()}%", style = MaterialTheme.typography.labelMedium, color = StudioPalette.Accent)
            }
            Slider(
                value = scale,
                onValueChange = { scale = it },
                valueRange = 10f..400f,
                colors = SliderDefaults.colors(
                    thumbColor = StudioPalette.Accent,
                    activeTrackColor = StudioPalette.Accent,
                    inactiveTrackColor = StudioPalette.SurfaceRaised,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Rotation Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Rotation", style = MaterialTheme.typography.labelMedium, color = StudioPalette.TextDim)
                Text("${rotation.toInt()}°", style = MaterialTheme.typography.labelMedium, color = StudioPalette.KeyframeMarker)
            }
            Slider(
                value = rotation,
                onValueChange = { rotation = it },
                valueRange = -180f..180f,
                colors = SliderDefaults.colors(
                    thumbColor = StudioPalette.KeyframeMarker,
                    activeTrackColor = StudioPalette.KeyframeMarker,
                    inactiveTrackColor = StudioPalette.SurfaceRaised,
                ),
            )
        }
    }
}

@Composable
private fun NumberScrubber(label: String, value: Int, onDelta: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StudioPalette.SurfaceRaised)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onDelta(-10) }, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Default.Remove, "-", tint = StudioPalette.TextDim, modifier = Modifier.size(16.dp))
        }
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            ),
            color = StudioPalette.TextPrimary,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        IconButton(onClick = { onDelta(10) }, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Default.Add, "+", tint = StudioPalette.TextDim, modifier = Modifier.size(16.dp))
        }
    }
}
