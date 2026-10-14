package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
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
 * SelectBottomBar per blueprint section 'Select Bottom Bar'
 * (activity_edit_select_bottomvar.xml).
 * 42dp tall, bg S9 (#1D1D1D), rounded 5dp corners.
 *
 * Elements (left to right):
 * - buttonMultiTrimLeft / buttonMultiSplit / buttonMultiTrimRight
 * - buttomMultiExpandEnd / buttomMultiMoveToRight / buttomMultiExpandStart / buttomMultiMoveToLeft
 * - alignLeftTimeline / alignDistributeTimeline / alignRightTimeline
 */
@Composable
fun StudioSelectBottomBar(
    // Multi-trim/split buttons
    onMultiTrimLeftClick: () -> Unit = {},
    onMultiSplitClick: () -> Unit = {},
    onMultiTrimRightClick: () -> Unit = {},
    // Expand/move buttons
    onExpandEndClick: () -> Unit = {},
    onMoveToRightClick: () -> Unit = {},
    onExpandStartClick: () -> Unit = {},
    onMoveToLeftClick: () -> Unit = {},
    // Timeline align buttons
    onAlignLeftTimelineClick: () -> Unit = {},
    onAlignDistributeTimelineClick: () -> Unit = {},
    onAlignRightTimelineClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(StudioPalette.S9)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            /* ── Multi-trim / split group ──────────────────────── */
            BottomBarButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = "Trim L",
                onClick = onMultiTrimLeftClick,
            )
            BottomBarButton(
                icon = Icons.Default.ContentCut,
                label = "Split",
                onClick = onMultiSplitClick,
            )
            BottomBarButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = "Trim R",
                onClick = onMultiTrimRightClick,
            )

            // Divider
            Divider()

            /* ── Expand / move group ─────────────────────────────── */
            BottomBarButton(
                icon = Icons.Default.LastPage,
                label = "Exp End",
                onClick = onExpandEndClick,
            )
            BottomBarButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = "Move R",
                onClick = onMoveToRightClick,
            )
            BottomBarButton(
                icon = Icons.Default.FirstPage,
                label = "Exp Start",
                onClick = onExpandStartClick,
            )
            BottomBarButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = "Move L",
                onClick = onMoveToLeftClick,
            )

            // Divider
            Divider()

            /* ── Timeline align group ───────────────────────────── */
            BottomBarButton(
                icon = Icons.Default.FormatAlignLeft,
                label = "Align L",
                onClick = onAlignLeftTimelineClick,
            )
            BottomBarButton(
                icon = Icons.Default.FormatAlignCenter,
                label = "Distribute",
                onClick = onAlignDistributeTimelineClick,
            )
            BottomBarButton(
                icon = Icons.Default.FormatAlignRight,
                label = "Align R",
                onClick = onAlignRightTimelineClick,
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(22.dp)
            .background(StudioPalette.Stroke),
    )
}

@Composable
private fun BottomBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(42.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(StudioPalette.S8)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = StudioPalette.W1,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = StudioPalette.W1a30,
            maxLines = 1,
        )
    }
}
