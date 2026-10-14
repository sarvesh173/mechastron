package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackProvider

/**
 * MoveTransformPanel – Move & Transform (fragment_move_transform.xml) control panel.
 *
 * Blueprint strings (verbatim):
 *   "Swipe here to move layer"
 *   "Swipe here to move pivot point"
 *   "Swipe here to adjust Z position"
 *   Width / Height
 *   X SKEW / Y SKEW
 *
 * Fields: X, Y, Z, Width, Height + X/Skew Y spinners
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveTransformPanel(
    layerX: Float = 0f,
    layerY: Float = 0f,
    layerZ: Float = 0f,
    pivotX: Float = 0f,
    pivotY: Float = 0f,
    width: Float = 1920f,
    height: Float = 1080f,
    xSkew: Float = 0f,
    ySkew: Float = 0f,
    modifier: Modifier = Modifier,
    onLayerPositionChanged: (Float, Float) -> Unit = { _, _ -> },
    onPivotChanged: (Float, Float) -> Unit = { _, _ -> },
    onZChanged: (Float) -> Unit = {},
    onWidthChanged: (Float) -> Unit = {},
    onHeightChanged: (Float) -> Unit = {},
    onXSkewChanged: (Float) -> Unit = {},
    onYSkewChanged: (Float) -> Unit = {}
) {
    // S10 background — neutral secondary surface
    val s10Background = StudioPalette.S10

    Column(
        modifier = modifier
            .background(s10Background)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF333842), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        /* ── Swipe trackpad row ──────────────────────────────── */
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            // Move layer trackpad
            MoveTrackpad(
                label = "Swipe here to move layer",
                offsetX = layerX,
                offsetY = layerY,
                onMove = onLayerPositionChanged
            )

            // Move pivot trackpad
            MoveTrackpad(
                label = "Swipe here to move pivot point",
                offsetX = pivotX,
                offsetY = pivotY,
                onMove = onPivotChanged
            )

            // Z-adjust trackpad
            MoveTrackpad(
                label = "Swipe here to adjust Z position",
                offsetX = 0f,       // purely directional hint
                offsetY = layerZ,
                isVertical = true,
                onMove = { _, v -> onZChanged(v) }
            )
        }

        /* ── X / Y / Z numeric fields ───────────────────────── */
        PositionFieldsRow(
            xValue = layerX,
            yValue = layerY,
            zValue = layerZ,
            onXChanged = { onLayerPositionChanged(it, layerY) },
            onYChanged = { onLayerPositionChanged(layerX, it) },
            onZChanged = onZChanged
        )

        /* ── Width / Height fields ──────────────────────────── */
        DimensionFieldsRow(
            width = width,
            height = height,
            onWidthChanged = onWidthChanged,
            onHeightChanged = onHeightChanged
        )

        /* ── X SKEW / Y SKEW spinners ───────────────────────── */
        SkewSpinnersRow(
            xSkew = xSkew,
            ySkew = ySkew,
            onXSkewChanged = onXSkewChanged,
            onYSkewChanged = onYSkewChanged
        )
    }
}

/* ───────────────────────────────────────────────────────────
 * Helper: single swipe trackpad card
 * ─────────────────────────────────────────────────────────── */

@Composable
private fun MoveTrackpad(
    label: String,
    offsetX: Float,
    offsetY: Float,
    isVertical: Boolean = false,
    onMove: (Float, Float) -> Unit
) {
    var dragDelta by remember { mutableStateOf(IntOffset.Zero) }
    val haptic = HapticFeedbackProvider()

    Card(
        modifier = Modifier
            .weight(1f, fill = false)
            .fillMaxHeight()
            .draggable(
                orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    dragDelta = IntOffset(if (isVertical) dragDelta.y else (dragDelta.x + delta.toInt()),
                                          if (!isVertical) dragDelta.y else (dragDelta.y + delta.toInt()))
                    onMove(offsetX + dragDelta.x.toFloat(), offsetY + dragDelta.y.toFloat())
                }
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF22262F)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    ),
                    maxLines = 2
                )
                Spacer(Modifier.height(4.dp))
                Icon(
                    imageVector = if (isVertical) Icons.Default.ArrowUpward else Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/* ───────────────────────────────────────────────────────────
 * Helper: X / Y / Z single-line fields
 * ─────────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PositionFieldsRow(
    xValue: Float,
    yValue: Float,
    zValue: Float,
    onXChanged: (Float) -> Unit,
    onYChanged: (Float) -> Unit,
    onZChanged: (Float) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NumericField(label = "X", value = xValue, onValueChange = onXChanged, color = Color(0xFFFF6B6B))
            NumericField(label = "Y", value = yValue, onValueChange = onYChanged, color = Color(0xFF69FFFF))
            NumericField(label = "Z", value = zValue, onValueChange = onZChanged, color = Color(0xFFFFD93D))
        }
    }
}

/* ───────────────────────────────────────────────────────────
 * Helper: Width / Height fields
 * ─────────────────────────────────────────────────────────── */

@Composable
private fun DimensionFieldsRow(
    width: Float,
    height: Float,
    onWidthChanged: (Float) -> Unit,
    onHeightChanged: (Float) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NumericField(label = "Width", value = width, onValueChange = onWidthChanged)
            NumericField(label = "Height", value = height, onValueChange = onHeightChanged)
        }
    }
}

/* ───────────────────────────────────────────────────────────
 * Helper: X SKEW / Y SKEW spinners
 * ─────────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkewSpinnersRow(
    xSkew: Float,
    ySkew: Float,
    onXSkewChanged: (Float) -> Unit,
    onYSkewChanged: (Float) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

            SpinnerField(
                label = "X SKEW",
                value = xSkew,
                onValueChange = onXSkewChanged,
                color = Color(0xFFB8E0FF)
            )
            SpinnerField(
                label = "Y SKEW",
                value = ySkew,
                onValueChange = onYSkewChanged,
                color = Color(0xFFFFC2E0)
            )
        }
    }
}

/* ───────────────────────────────────────────────────────────
 * Reusable numeric field (outlined, editable text)
 * ─────────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumericField(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color = Color.White
) {
    var textFieldValue by remember { mutableStateOf(value.toString()) }
    val lastValue = remember { value }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { raw ->
            // allow digits, optional dot, optional minus
            if (raw.isEmpty() || raw.matches("-?\\d*\\.?\\d*".toRegex())) {
                textFieldValue = raw
                try {
                    onValueChange(raw.toFloatOrNull() ?: lastValue)
                } catch (_: Exception) {
                    onValueChange(lastValue)
                }
            }
        },
        label = { Text(label, fontWeight = FontWeight.Medium, fontSize = 11.sp) },
        textStyle = MaterialTheme.typography.labelMedium.copy(color = color, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
        singleLine = true,
        shape = RoundedCornerShape(6.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = color,
            unfocusedBorderColor = Color(0xFF333842),
            containerColor = StudioPalette.S9
        ),
        modifier = Modifier.weight(1f)
    )
}

/* ───────────────────────────────────────────────────────────
 * Reusable spinner field with +/- buttons
 * ─────────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpinnerField(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color = Color.White
) {
    var textFieldValue by remember { mutableStateOf(value.toString()) }

    Column {
        Text(label, fontWeight = FontWeight.Medium, fontSize = 10.sp, color = color)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            // Decrease button
            IconButton(onClick = {
                val next = value - 1f
                onValueChange(next)
                textFieldValue = "%.1f".format(next)
            }) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Decrease", tint = color, modifier = Modifier.size(18.dp))
            }

            // Center readout
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { raw ->
                    if (raw.isEmpty() || raw.matches("-?\\d*\\.?\\d*".toRegex())) {
                        textFieldValue = raw
                        try { onValueChange(raw.toFloatOrNull() ?: value) } catch (_: Exception) {}
                    }
                },
                textStyle = MaterialTheme.typography.labelMedium.copy(color = color, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                singleLine = true,
                shape = RoundedCornerShape(4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = color,
                    unfocusedBorderColor = Color(0xFF333842),
                    containerColor = StudioPalette.S9
                ),
                modifier = Modifier.weight(1f).height(28.dp)
            )

            // Increase button
            IconButton(onClick = {
                val next = value + 1f
                onValueChange(next)
                textFieldValue = "%.1f".format(next)
            }) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Increase", tint = color, modifier = Modifier.size(18.dp))
            }
        }
    }
}
