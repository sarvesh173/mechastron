package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlignHorizontalCenter
import androidx.compose.material.icons.filled.AlignHorizontalLeft
import androidx.compose.material.icons.filled.AlignHorizontalRight
import androidx.compose.material.icons.filled.AlignVerticalBottom
import androidx.compose.material.icons.filled.AlignVerticalCenter
import androidx.compose.material.icons.filled.AlignVerticalTop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Masks
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * SelectBar per blueprint section 'Select Bar' (activity_edit_selectbar.xml).
 * 42dp tall, bg Y1 (#00FFA8 mint-green).
 *
 * Elements:
 * - selectStateMessage — selection count text
 * - buttonGroupSelected / buttonGroupAndMask / buttonGroupAndExclude
 * - buttonDeleteSelected / buttonRightCue
 * - 8 align screen buttons: left / centerH / right / top / centerV / bottom / distributeV / distributeH
 * - cancelSelectMode — exit selection mode (bg Y1)
 */
@Composable
fun StudioSelectBar(
    selectedCount: Int,
    onGroupClick: () -> Unit = {},
    onMaskClick: () -> Unit = {},
    onExcludeClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onRightCueClick: () -> Unit = {},
    // 8 align screen buttons
    onAlignLeftClick: () -> Unit = {},
    onAlignCenterHClick: () -> Unit = {},
    onAlignRightClick: () -> Unit = {},
    onAlignTopClick: () -> Unit = {},
    onAlignCenterVClick: () -> Unit = {},
    onAlignBottomClick: () -> Unit = {},
    onDistributeVClick: () -> Unit = {},
    onDistributeHClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(StudioPalette.Y1)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // selectStateMessage
            Text(
                text = if (selectedCount == 1) "1 selected" else "$selectedCount selected",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.Black,
                modifier = Modifier.padding(end = 4.dp),
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(Color.Black.copy(alpha = 0.2f)),
            )

            // Group / Mask / Exclude buttons
            SelectBarButton(
                icon = Icons.Default.GroupWork,
                label = "Group",
                onClick = onGroupClick,
            )
            SelectBarButton(
                icon = Icons.Default.Masks,
                label = "Mask",
                onClick = onMaskClick,
            )
            SelectBarButton(
                icon = Icons.Default.LayersClear,
                label = "Exclude",
                onClick = onExcludeClick,
            )
            SelectBarButton(
                icon = Icons.Default.Delete,
                label = "Delete",
                onClick = onDeleteClick,
                tint = Color(0xFFFF4444),
            )
            SelectBarButton(
                icon = Icons.Default.ContentCut,
                label = "Cue",
                onClick = onRightCueClick,
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(Color.Black.copy(alpha = 0.2f)),
            )

            // 8 screen align buttons — left/centerH/right
            AlignButton(Icons.Default.AlignHorizontalLeft, "Align Left", onAlignLeftClick)
            AlignButton(Icons.Default.AlignHorizontalCenter, "Align Center H", onAlignCenterHClick)
            AlignButton(Icons.Default.AlignHorizontalRight, "Align Right", onAlignRightClick)

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(Color.Black.copy(alpha = 0.2f)),
            )

            // top/centerV/bottom
            AlignButton(Icons.Default.AlignVerticalTop, "Align Top", onAlignTopClick)
            AlignButton(Icons.Default.AlignVerticalCenter, "Align Center V", onAlignCenterVClick)
            AlignButton(Icons.Default.AlignVerticalBottom, "Align Bottom", onAlignBottomClick)

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(Color.Black.copy(alpha = 0.2f)),
            )

            // distributeV / distributeH
            AlignButton(Icons.Default.Gradient, "Distribute V", onDistributeVClick)
            AlignButton(Icons.Default.GridView, "Distribute H", onDistributeHClick)

            // Spacer
            Box(modifier = Modifier.weight(1f))

            // cancelSelectMode — bg Y1
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.15f))
                    .clickable { onCancelClick() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Selection",
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.Black,
) {
    Column(
        modifier = Modifier
            .width(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun AlignButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(30.dp)
            .clip(RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.Black.copy(alpha = 0.8f),
            modifier = Modifier.size(14.dp),
        )
    }
}
