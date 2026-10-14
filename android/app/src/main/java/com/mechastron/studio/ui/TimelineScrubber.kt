package com.mechastron.studio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mechastron.studio.model.SceneProject
import kotlin.math.abs
import kotlin.math.roundToLong

private val TrackHeight = 56.dp
private val ScaleHeight = 18.dp
private const val MARKER_TOP_PX = 10f
private const val MARKER_BOTTOM_PX = 30f
private const val MIN_DURATION_MS = 1L
private const val MIN_GRID_SPACING_PX = 24f

@Composable
fun TimelineScrubber(
    project: SceneProject,
    playheadMs: Long,
    isPlaying: Boolean,
    onScrub: (Long) -> Unit,
    modifier: Modifier = Modifier,
    selectedLayerId: String? = null,
) {
    val durationMs = project.durationMs.coerceAtLeast(MIN_DURATION_MS)
    val keyframes = remember(project) { project.keyframeTimes().toList() }
    val clampedPlayhead = playheadMs.coerceIn(0L, durationMs)
    val scrub by rememberUpdatedState(onScrub)
    val currentProject by rememberUpdatedState(project)
    val currentSelection by rememberUpdatedState(selectedLayerId)

    Column(modifier = modifier.fillMaxWidth()) {
        TimelineHeader(project = project, playheadMs = clampedPlayhead, isPlaying = isPlaying)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(TrackHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(StudioPalette.TrackBackdrop)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        scrub(offset.toTimeMs(size.width.toFloat(), durationMs))
                    }
                }
                .pointerInput(durationMs) {
                    var accumulatedMs = 0L
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            accumulatedMs = offset.toTimeMs(size.width.toFloat(), durationMs)
                            scrub(accumulatedMs)
                        },
                        onDragEnd = { },
                        onDragCancel = { },
                    ) { change, dragAmount ->
                        change.consume()
                        val deltaMs = (dragAmount / size.width.toFloat() * durationMs).roundToLong()
                        accumulatedMs = (accumulatedMs + deltaMs).coerceIn(0L, durationMs)
                        scrub(accumulatedMs)
                    }
                },
        ) {
            drawTimeline(
                keyframes = keyframes,
                playheadMs = clampedPlayhead,
                durationMs = durationMs,
                activeBand = currentProject.layerById(currentSelection ?: "")?.let { layer ->
                    layer.startTimeMs to layer.endTimeMs
                },
                widthPx = size.width,
            )
        }

        SecondScale(durationMs = durationMs, fps = project.fps)
    }
}

private fun Offset.toTimeMs(widthPx: Float, durationMs: Long): Long {
    if (widthPx <= 0f) return 0L
    val fraction = (x / widthPx).coerceIn(0f, 1f)
    return (fraction * durationMs).roundToLong()
}

@Composable
private fun TimelineHeader(project: SceneProject, playheadMs: Long, isPlaying: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Timeline · ${project.layers.size} layers",
            style = MaterialTheme.typography.labelMedium,
            color = StudioPalette.TextDim,
        )
        Text(
            text = formatTimecode(playheadMs, project.fps),
            style = MaterialTheme.typography.labelMedium,
            color = StudioPalette.TextPrimary,
        )
        Text(
            text = (if (isPlaying) "▶ " else "❚❚ ") + "${project.fps} FPS",
            style = MaterialTheme.typography.labelSmall,
            color = if (isPlaying) StudioPalette.Accent else StudioPalette.TextDim,
        )
    }
}

@Composable
private fun SecondScale(durationMs: Long, fps: Int) {
    val tickCount = remember(durationMs) { (durationMs / 1000.0).toInt().coerceAtLeast(1) }
    Canvas(modifier = Modifier.fillMaxWidth().height(ScaleHeight)) {
        if (tickCount <= 0 || size.width <= 0f) return@Canvas
        val spacing = size.width / tickCount
        if (spacing < MIN_GRID_SPACING_PX) return@Canvas
        for (i in 0..tickCount) {
            val x = (i * spacing).coerceAtMost(size.width)
            val isEdge = i == 0 || i == tickCount
            drawLine(
                color = StudioPalette.TickLine,
                start = Offset(x, 0f),
                end = Offset(x, if (isEdge) size.height else size.height * 0.5f),
                strokeWidth = 1f,
            )
        }
    }
}

private fun DrawScope.drawTimeline(
    keyframes: List<Long>,
    playheadMs: Long,
    durationMs: Long,
    activeBand: Pair<Long, Long>?,
    widthPx: Float,
) {
    val playedFraction = (playheadMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    val playheadX = playedFraction * widthPx

    drawRoundRect(
        color = StudioPalette.PlayedRegion,
        topLeft = Offset.Zero,
        size = Size(playheadX, size.height),
        cornerRadius = CornerRadius(8f, 8f),
    )

    activeBand?.let { (startMs, endMs) ->
        val startX = (startMs.toFloat() / durationMs).coerceIn(0f, 1f) * widthPx
        val endX = (endMs.toFloat() / durationMs).coerceIn(0f, 1f) * widthPx
        if (endX > startX) {
            drawRect(
                color = StudioPalette.ActiveBand,
                topLeft = Offset(startX, MARKER_TOP_PX - 6f),
                size = Size(endX - startX, size.height - (MARKER_TOP_PX - 6f) * 2f),
            )
        }
    }

    drawKeyframeMarkers(keyframes, durationMs, widthPx)
    drawSecondGridlines(durationMs, widthPx)

    drawLine(
        color = StudioPalette.Playhead,
        start = Offset(playheadX, 0f),
        end = Offset(playheadX, size.height),
        strokeWidth = 3f,
    )
    drawCircle(color = StudioPalette.Playhead, radius = 7f, center = Offset(playheadX, size.height / 2f))
}

private fun DrawScope.drawKeyframeMarkers(keyframes: List<Long>, durationMs: Long, widthPx: Float) {
    keyframes.forEach { timeMs ->
        val fraction = (timeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val x = fraction * widthPx
        drawRoundRect(
            color = StudioPalette.KeyframeMarker,
            topLeft = Offset(x - 2f, MARKER_TOP_PX),
            size = Size(4f, MARKER_BOTTOM_PX - MARKER_TOP_PX),
            cornerRadius = CornerRadius(2f, 2f),
        )
        drawLine(
            color = StudioPalette.MarkerGuide,
            start = Offset(x, MARKER_BOTTOM_PX),
            end = Offset(x, size.height),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
        )
    }
}

private fun DrawScope.drawSecondGridlines(durationMs: Long, widthPx: Float) {
    val seconds = (durationMs / 1000.0).toInt().coerceAtLeast(1)
    if (seconds <= 0 || widthPx <= 0f) return
    val spacing = widthPx / seconds
    if (abs(spacing) < MIN_GRID_SPACING_PX) return
    for (i in 1 until seconds) {
        val x = i * spacing
        drawLine(
            color = StudioPalette.TickLine,
            start = Offset(x, MARKER_BOTTOM_PX),
            end = Offset(x, size.height),
            strokeWidth = 1f,
        )
    }
}

fun formatTimecode(timeMs: Long, fps: Int): String {
    val safeFps = fps.coerceAtLeast(1)
    val totalFrames = (timeMs * safeFps / 1000L).coerceAtLeast(0L)
    val frames = totalFrames % safeFps
    val totalSeconds = totalFrames / safeFps
    return "%02d:%02d:%02d".format(totalSeconds / 60, totalSeconds % 60, frames)
}