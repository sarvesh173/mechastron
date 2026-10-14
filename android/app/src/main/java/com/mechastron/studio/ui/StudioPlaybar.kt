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
 * Playbar per blueprint section 'Playbar' (activity_edit_playbar.xml) — 46dp tall
 *
 * Layout: [levelMeterView 2dp] + [controls row]
 * Controls (left to right):
 *   buttonUndo / buttonRedo / buttonSkipBack / buttonPlay / buttonSkipFwd
 *   buttonCopyPasteOverflow (with "NEW" badge area)
 *   buttonBookmark (activated=5% white bg, pressed=6.3% white bg)
 *   buttonPreviewMode
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
    onPreviewModeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(StudioPalette.A10),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // levelMeterView: 2dp audio level meter strip at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(StudioPalette.VuMeter),
        )

        // Controls row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // buttonUndo
            IconButton(onClick = onUndoClick, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = StudioPalette.W1,
                    modifier = Modifier.size(19.dp),
                )
            }

            // buttonRedo
            IconButton(onClick = onRedoClick, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = StudioPalette.W1,
                    modifier = Modifier.size(19.dp),
                )
            }

            // buttonSkipBack (prev frame)
            IconButton(onClick = onPrevFrameClick, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous Frame",
                    tint = StudioPalette.W1,
                    modifier = Modifier.size(20.dp),
                )
            }

            // buttonPlay — circular accent button
            PlayButton(
                isPlaying = isPlaying,
                onClick = onPlayPauseClick,
            )

            // buttonSkipFwd (next frame)
            IconButton(onClick = onNextFrameClick, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next Frame",
                    tint = StudioPalette.W1,
                    modifier = Modifier.size(20.dp),
                )
            }

            // buttonCopyPasteOverflowContainer — with "NEW" badge area
            CopyPasteButton(onClick = onCopyPasteMenuClick)

            // buttonBookmark — activated = 5% white bg, pressed = 6.3% white bg
            BookmarkButton(
                onClick = onBookmarkClick,
            )

            // buttonPreviewMode
            IconButton(onClick = onPreviewModeClick, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen Preview",
                    tint = StudioPalette.D2,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun PlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(StudioPalette.S8)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = StudioPalette.Y1,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun CopyPasteButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy/Paste Menu",
            tint = StudioPalette.D2,
            modifier = Modifier.size(18.dp),
        )
        // "NEW" badge placeholder — CardView with 4.5dp corner radius, shown when applicable
        // Actual badge rendering deferred to parent; this button opens the overflow menu
    }
}

@Composable
private fun BookmarkButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        // buttonBookmark uses bg playbar_toggle_button_bg: 4.5dp inset rounded rect
        // activated = 5% white (#0DFFFFFF), pressed = 6.3% white (#10FFFFFF)
        Icon(
            imageVector = Icons.Default.BookmarkBorder,
            contentDescription = "Bookmark",
            tint = StudioPalette.D2,
            modifier = Modifier.size(18.dp),
        )
    }
}