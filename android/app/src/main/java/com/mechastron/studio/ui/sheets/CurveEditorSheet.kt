package com.mechastron.studio.ui.sheets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mechastron.studio.ui.StudioPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurveEditorSheet(
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var x1 by remember { mutableFloatStateOf(0.25f) }
    var y1 by remember { mutableFloatStateOf(0.1f) }
    var x2 by remember { mutableFloatStateOf(0.25f) }
    var y2 by remember { mutableFloatStateOf(1.0f) }

    val presets = listOf(
        "Linear" to listOf(0.0f, 0.0f, 1.0f, 1.0f),
        "Ease In" to listOf(0.42f, 0.0f, 1.0f, 1.0f),
        "Ease Out" to listOf(0.0f, 0.0f, 0.58f, 1.0f),
        "Ease In Out" to listOf(0.42f, 0.0f, 0.58f, 1.0f),
        "Fast Out Slow" to listOf(0.4f, 0.0f, 0.2f, 1.0f),
        "Sharp Snap" to listOf(0.15f, 0.85f, 0.35f, 1.2f),
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Keyframe Curve (Cubic Bezier)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = StudioPalette.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Visual Bezier Graph Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioPalette.SurfaceRaised)
                    .padding(16.dp),
            ) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val normX = dragAmount.x / size.width
                                val normY = -dragAmount.y / size.height
                                x1 = (x1 + normX).coerceIn(0f, 1f)
                                y1 = (y1 + normY).coerceIn(-0.5f, 1.5f)
                            }
                        },
                ) {
                    val w = size.width
                    val h = size.height

                    // Grid lines
                    drawLine(Color(0x22FFFFFF), Offset(0f, h), Offset(w, h), strokeWidth = 2f)
                    drawLine(Color(0x22FFFFFF), Offset(0f, 0f), Offset(w, 0f), strokeWidth = 2f)
                    drawLine(Color(0x11FFFFFF), Offset(w / 2f, 0f), Offset(w / 2f, h), strokeWidth = 2f)

                    val p0 = Offset(0f, h)
                    val p1 = Offset(x1 * w, h - y1 * h)
                    val p2 = Offset(x2 * w, h - y2 * h)
                    val p3 = Offset(w, 0f)

                    // Handle control lines
                    drawLine(StudioPalette.KeyframeMarker, p0, p1, strokeWidth = 3f)
                    drawLine(StudioPalette.Accent, p3, p2, strokeWidth = 3f)

                    // Control handle circles
                    drawCircle(StudioPalette.KeyframeMarker, radius = 10f, center = p1)
                    drawCircle(StudioPalette.Accent, radius = 10f, center = p2)

                    // Cubic Bezier curve path
                    val path = Path().apply {
                        moveTo(p0.x, p0.y)
                        cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
                    }
                    drawPath(path, color = Color.White, style = Stroke(width = 6f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preset Curve Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.take(3).forEach { (name, coords) ->
                    PresetChip(name) {
                        x1 = coords[0]
                        y1 = coords[1]
                        x2 = coords[2]
                        y2 = coords[3]
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.drop(3).forEach { (name, coords) ->
                    PresetChip(name) {
                        x1 = coords[0]
                        y1 = coords[1]
                        x2 = coords[2]
                        y2 = coords[3]
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(name: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StudioPalette.SurfaceRaised)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = StudioPalette.TextPrimary,
        )
    }
}
