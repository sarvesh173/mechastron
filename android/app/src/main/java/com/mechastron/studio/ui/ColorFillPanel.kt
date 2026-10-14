package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vrpano
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ColorFillPanel per blueprint section 'Color & Fill' (fragment_color_and_fill.xml).
 * Background: A9 (#151515).
 *
 * Elements:
 * - tab_holder (5 tabs): btn_none / btn_fill / btn_gradation / btn_media / btn_intrinsic
 * - color_picker placeholder: ColorPickerWidget as Canvas
 * - Gradation holder: gradation_type1/2/3 buttons + gradation_bar + start/end color circles
 * - Media holder: selected_media ("Select media" label) + Fill / Fit / Stretch buttons
 */
@Composable
fun ColorFillPanel(
    // None tab — shown when no fill applied
    onNoneClick: () -> Unit = {},
    // Fill tab — color picker placeholder
    currentFillColor: Color = Color.Red,
    onFillColorChange: (Color) -> Unit = {},
    // Gradation tab
    gradationType: Int = 1,   // 1, 2, or 3
    startColor: Color = Color.White,
    endColor: Color = Color.Blue,
    onGradationTypeChange: (Int) -> Unit = {},
    onStartColorChange: (Color) -> Unit = {},
    onEndColorChange: (Color) -> Unit = {},
    // Media tab
    selectedMediaName: String? = null,
    mediaCropMode: MediaCropMode = MediaCropMode.FILL,
    onMediaCropModeChange: (MediaCropMode) -> Unit = {},
    onSelectMediaClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioPalette.A9)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {

        /* ── Tab holder: None / Fill / Gradation / Media / Intrinsic ── */
        val tabs = listOf(
            TabItem("None", Icons.Default.Tune),
            TabItem("Fill", Icons.Default.ColorLens),
            TabItem("Gradation", Icons.Default.Gradient),
            TabItem("Media", Icons.Default.Image),
            TabItem("Intrinsic", Icons.Default.Vrpano),
        )
        var selectedTab by remember { mutableIntStateOf(1) } // default to Fill

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = StudioPalette.S9,
            contentColor = StudioPalette.W1,
            indicator = { tabPositions ->
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedTab])
                        .height(2.dp)
                        .padding(horizontal = 2.dp)
                        .background(StudioPalette.Accent),
                )
            },
            modifier = Modifier.height(32.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.height(32.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (selectedTab == index) StudioPalette.Accent else StudioPalette.TextDim,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                            color = if (selectedTab == index) StudioPalette.W1 else StudioPalette.TextDim,
                        )
                    }
                }
            }
        }

        /* ── Tab content ───────────────────────────────────────── */
        when (selectedTab) {
            0 -> NoneTabContent(onClick = onNoneClick)
            1 -> FillTabContent(
                currentColor = currentFillColor,
                onColorChange = onFillColorChange,
            )
            2 -> GradationTabContent(
                gradationType = gradationType,
                startColor = startColor,
                endColor = endColor,
                onTypeChange = onGradationTypeChange,
                onStartColorChange = onStartColorChange,
                onEndColorChange = onEndColorChange,
            )
            3 -> MediaTabContent(
                mediaName = selectedMediaName,
                cropMode = mediaCropMode,
                onCropModeChange = onMediaCropModeChange,
                onSelectMediaClick = onSelectMediaClick,
            )
            4 -> IntrinsicTabPlaceholder()
        }
    }
}

/* ─────────────────────────────────────────────────────────────
 * None tab
 * ───────────────────────────────────────────────────────────── */

@Composable
private fun NoneTabContent(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StudioPalette.S9)
            .border(1.dp, StudioPalette.Stroke, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "None",
                tint = StudioPalette.TextDim,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "No Fill",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = StudioPalette.TextDim,
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────
 * Fill tab — ColorPickerWidget placeholder as Canvas
 * ───────────────────────────────────────────────────────────── */

@Composable
private fun FillTabContent(
    currentColor: Color,
    onColorChange: (Color) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ColorPickerWidget placeholder: Canvas-based color picker area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StudioPalette.S10)
                .border(1.dp, StudioPalette.Stroke, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            // Placeholder canvas representation of a color picker widget
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Current color swatch
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(2.dp, StudioPalette.W1a30, CircleShape),
                )
                Text(
                    text = colorToHex(currentColor),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    ),
                    color = StudioPalette.W1,
                )
                Text(
                    text = "Color Picker Widget",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = StudioPalette.TextDim,
                )
            }
        }

        // Quick color swatches row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val quickColors = listOf(
                Color.White, Color.Black, Color.Red,
                Color.Green, Color.Blue, Color.Yellow,
            )
            quickColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                        .border(
                            width = if (color == currentColor) 2.dp else 1.dp,
                            color = if (color == currentColor) StudioPalette.Accent else StudioPalette.Stroke,
                            shape = RoundedCornerShape(4.dp),
                        )
                        .clickable { onColorChange(color) },
                )
            }
        }
    }
}
