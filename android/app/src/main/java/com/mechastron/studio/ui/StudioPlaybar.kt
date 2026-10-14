package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fullscreen
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Exact replica of Alight Motion's activity_edit_playbar.xml:
 * Height: 46dp, Background: A10 (#1C1C1C)
 * Level meter at bottom: 2dp (#00FFA8)
 * [Undo] [Redo] [PrevFrame] [Play/Pause] [NextFrame] [CopyPaste] [Bookmark] [Graph] [Keyframe◆] [PreviewMode]
 */
@Composable
fun StudioPlaybar(
    isPlaying: Boolean,
    hasKeyframeAtPlayhead: Boolean,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPrevFrameClick: () -> Unit,
    onNextFrameClick: () -> Unit,
    onCopyPasteMenuClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onOpenGraphClick: () -> Unit,
    onToggleKeyframeClick: () -> Unit,
    onPreviewModeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(StudioPalette.CanvasMatte),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // buttonUndo
            IconButton(onClick = onUndoClick, modifier = Modifier.size(34.dp)) {
                Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = StudioPalette.TextPrimary, modifier = Modifier.size(19.dp))
            }

            // buttonRedo
            IconButton(onClick = onRedoClick, modifier = Modifier.size(34.dp)) {
                Icon(Icons.AutoMirrored.Filled.Redo, "Redo", tint = StudioPalette.TextPrimary, modifier = Modifier.size(19.dp))
            }

            // buttonSkipBack (ic_edit_prevframe)
            IconButton(onClick = onPrevFrameClick, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.SkipPrevious, "Previous Frame", tint = StudioPalette.TextPrimary, modifier = Modifier.size(20.dp))
            }

            // buttonPlay (ac_ic_play)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(StudioPalette.ButtonBg)
                    .clickable { onPlayPauseClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = StudioPalette.Accent,
                    modifier = Modifier.size(20.dp),
                )
            }

            // buttonSkipFwd (ic_edit_nextframe)
            IconButton(onClick = onNextFrameClick, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.SkipNext, "Next Frame", tint = StudioPalette.TextPrimary, modifier = Modifier.size(20.dp))
            }

            // buttonCopyPasteOverflow (ic_layer_copy_paste_menu)
            IconButton(onClick = onCopyPasteMenuClick, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.ContentCopy, "Copy/Paste Menu", tint = StudioPalette.TextDim, modifier = Modifier.size(18.dp))
            }

            // buttonBookmark (ic_bookmark_add)
            IconButton(onClick = onBookmarkClick, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.BookmarkBorder, "Bookmark", tint = StudioPalette.TextDim, modifier = Modifier.size(18.dp))
            }

            // Graph / Easing Curve Editor Button (The iconic curve icon)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(StudioPalette.ButtonBg)
                    .clickable { onOpenGraphClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.ShowChart, "Graph Editor", tint = StudioPalette.Accent, modifier = Modifier.size(16.dp))
            }

            // Diamond Keyframe Button (+ / - Diamond)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (hasKeyframeAtPlayhead) StudioPalette.Accent else StudioPalette.ButtonBg)
                    .clickable { onToggleKeyframeClick() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (hasKeyframeAtPlayhead) "◆-" else "◆+",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
                    color = if (hasKeyframeAtPlayhead) Color.Black else StudioPalette.Accent,
                )
            }

            // buttonPreviewMode (ic_viewmode_panel_close)
            IconButton(onClick = onPreviewModeClick, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Fullscreen, "Fullscreen Preview", tint = StudioPalette.TextDim, modifier = Modifier.size(20.dp))
            }
        }

        // levelMeterView: 2dp audio level meter line at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(StudioPalette.VuMeter),
        )
    }
}
