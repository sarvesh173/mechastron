package com.mechastron.studio.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * InfoBar per blueprint section 'InfoBar' (activity_edit_infobar.xml) — 46dp tall.
 *
 * Two display modes:
 * - **iconMode:** infoModeIcon + infoProjectTime + infoOffsetIcon + infoOffsetTime
 * - **textMode:** 3 label/value column pairs (infoBar1L/infoBar1 … infoBar3L/infoBar6)
 *
 * Background: A10 (#1c1c1c)
 */
@Composable
fun StudioInfoBar(
    mode: InfoBarMode = InfoBarMode.IconMode,
    // iconMode params
    modeIconLabel: String = "Shape",
    projectTime: String = "00:00.00",
    offsetTime: String = "+00:00.00",
    // textMode params — 3 column pairs
    labelValue1: Pair<String, String> = "Position" to "0, 0",
    labelValue2: Pair<String, String> = "Scale" to "100%",
    labelValue3: Pair<String, String> = "Rotation" to "0°",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(StudioPalette.A10),
        contentAlignment = Alignment.Center,
    ) {
        when (mode) {
            InfoBarMode.IconMode -> IconModeContent(
                modeIconLabel = modeIconLabel,
                projectTime = projectTime,
                offsetTime = offsetTime,
            )
            InfoBarMode.TextMode -> TextModeContent(
                labelValue1 = labelValue1,
                labelValue2 = labelValue2,
                labelValue3 = labelValue3,
            )
        }
    }
}

@Composable
private fun IconModeContent(
    modeIconLabel: String,
    projectTime: String,
    offsetTime: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // infoModeIcon — current editing mode label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Mode",
                tint = StudioPalette.Accent,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = modeIconLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = StudioPalette.W1,
            )
        }

        // Spacer
        Box(modifier = Modifier.weight(1f))

        // infoProjectTime — full project timecode
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Project Time",
                tint = StudioPalette.TextDim,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = projectTime,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                ),
                color = StudioPalette.W1,
            )
        }

        // infoOffsetIcon + infoOffsetTime — relative offset
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Offset",
                tint = StudioPalette.TextDim,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = offsetTime,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                ),
                color = StudioPalette.W1a30,
            )
        }
    }
}

@Composable
private fun TextModeContent(
    labelValue1: Pair<String, String>,
    labelValue2: Pair<String, String>,
    labelValue3: Pair<String, String>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Column 1: infoBar1L / infoBar1
        InfoBarColumn(label = labelValue1.first, value = labelValue1.second)

        // Column 2: infoBar2L / infoBar2
        InfoBarColumn(label = labelValue2.first, value = labelValue2.second)

        // Column 3: infoBar3L / infoBar3
        InfoBarColumn(label = labelValue3.first, value = labelValue3.second)
    }
}

@Composable
private fun InfoBarColumn(
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Label row (infoBarXL)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = StudioPalette.W1a30,
        )
        // Value row (infoBarX)
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            ),
            color = StudioPalette.W1,
        )
    }
}

enum class InfoBarMode {
    IconMode,
    TextMode,
}
