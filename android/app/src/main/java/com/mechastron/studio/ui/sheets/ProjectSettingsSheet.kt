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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mechastron.studio.ui.StudioPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSettingsSheet(
    currentResolution: String = "4K · 3840×2160",
    currentFps: Int = 60,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedRes by remember { mutableStateOf(currentResolution) }
    var selectedRatio by remember { mutableStateOf("16:9") }
    var selectedFps by remember { mutableStateOf(currentFps) }

    val resolutions = listOf("4K · 3840×2160", "1080p · 1920×1080", "720p · 1280×720")
    val ratios = listOf("16:9", "9:16", "1:1", "4:5")
    val fpsOptions = listOf(60, 30, 24)

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
                text = "Project Settings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = StudioPalette.TextPrimary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Resolution", style = MaterialTheme.typography.labelMedium, color = StudioPalette.TextDim)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                resolutions.forEach { res ->
                    val isSel = selectedRes == res
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) StudioPalette.Accent else StudioPalette.SurfaceRaised)
                            .clickable { selectedRes = res }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = res.split("·")[0].trim(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal),
                            color = if (isSel) StudioPalette.Backdrop else StudioPalette.TextPrimary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Aspect Ratio", style = MaterialTheme.typography.labelMedium, color = StudioPalette.TextDim)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ratios.forEach { ratio ->
                    val isSel = selectedRatio == ratio
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) StudioPalette.Accent else StudioPalette.SurfaceRaised)
                            .clickable { selectedRatio = ratio }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = ratio,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal),
                            color = if (isSel) StudioPalette.Backdrop else StudioPalette.TextPrimary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Frame Rate", style = MaterialTheme.typography.labelMedium, color = StudioPalette.TextDim)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fpsOptions.forEach { fps ->
                    val isSel = selectedFps == fps
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) StudioPalette.Accent else StudioPalette.SurfaceRaised)
                            .clickable { selectedFps = fps }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "$fps FPS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal),
                            color = if (isSel) StudioPalette.Backdrop else StudioPalette.TextPrimary,
                        )
                    }
                }
            }
        }
    }
}
