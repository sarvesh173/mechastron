package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StudioPlaybar(
    playheadMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    hasKeyframeAtPlayhead: Boolean,
    onPlayPauseClick: () -> Unit,
    onJumpStartClick: () -> Unit,
    onJumpEndClick: () -> Unit,
    onPrevKeyframeClick: () -> Unit,
    onNextKeyframeClick: () -> Unit,
    onToggleKeyframeClick: () -> Unit,
    onOpenGraphClick: () -> Unit,
    onAddBookmarkClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioPalette.Surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Timecode display: 00:00.00 / 00:04.00
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(StudioPalette.SurfaceRaised)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = "${formatTimecode(playheadMs)} / ${formatTimecode(durationMs)}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                ),
                color = StudioPalette.TextPrimary,
            )
        }

        // Keyframe and Graph Editor controls
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Easing Graph Curve Editor Button (The iconic curve icon)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(StudioPalette.SurfaceRaised)
                    .clickable { onOpenGraphClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = "Graph Easing Editor",
                    tint = StudioPalette.Accent,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Diamond Keyframe Button (+ / - Diamond)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (hasKeyframeAtPlayhead) StudioPalette.KeyframeMarker else StudioPalette.SurfaceRaised)
                    .clickable { onToggleKeyframeClick() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (hasKeyframeAtPlayhead) "◆-" else "◆+",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                    ),
                    color = if (hasKeyframeAtPlayhead) StudioPalette.Backdrop else StudioPalette.KeyframeMarker,
                )
            }
        }

        // Transport Scrubbing and Play/Pause Controls
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onJumpStartClick, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Jump to Start",
                    tint = StudioPalette.TextDim,
                    modifier = Modifier.size(16.dp),
                )
            }

            IconButton(onClick = onPrevKeyframeClick, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.FastRewind,
                    contentDescription = "Previous Keyframe",
                    tint = StudioPalette.TextPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Central Play / Pause Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(StudioPalette.Accent)
                    .clickable { onPlayPauseClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }

            IconButton(onClick = onNextKeyframeClick, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "Next Keyframe",
                    tint = StudioPalette.TextPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }

            IconButton(onClick = onJumpEndClick, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Jump to End",
                    tint = StudioPalette.TextDim,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun formatTimecode(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val centis = (ms % 1000) / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, centis)
}
