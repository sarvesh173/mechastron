package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AddItem(val name: String, val icon: ImageVector, val color: Color)

/**
 * Exact replica of Alight Motion's activity_edit_addpopup.xml:
 * Left: elementTabHolder [Shape, Media, Audio, Objects, Templates] + Tab Content Pager
 * Right: quickAddBar [Freehand, Vector, Text] + buttonCloseAdd (X)
 */
@Composable
fun AddElementPopup(
    onDismiss: () -> Unit,
    onSelectElement: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Shape", "Media", "Audio", "Objects", "Templates")

    val shapes = listOf(
        AddItem("Rectangle", Icons.Default.CropSquare, StudioPalette.Accent),
        AddItem("Circle", Icons.Default.RadioButtonUnchecked, Color(0xFF64B5F6)),
        AddItem("Triangle", Icons.Default.ChangeHistory, Color(0xFFFFD54F)),
        AddItem("Star", Icons.Default.Star, Color(0xFFFF8A65)),
        AddItem("Heart", Icons.Default.Favorite, Color(0xFFE57373)),
        AddItem("Plus", Icons.Default.Add, Color(0xFF81C784)),
    )

    val media = listOf(
        AddItem("Photo Gallery", Icons.Default.Image, Color(0xFFBA68C8)),
        AddItem("Video Clips", Icons.Default.Videocam, Color(0xFF4FC3F7)),
    )

    val audio = listOf(
        AddItem("Audio Track", Icons.Default.Audiotrack, Color(0xFFE040FB)),
    )

    val objects = listOf(
        AddItem("Camera", Icons.Default.CameraAlt, Color(0xFFFFB74D)),
        AddItem("Null Object", Icons.Default.Widgets, Color(0xFF90A4AE)),
        AddItem("Empty Group", Icons.Default.Layers, Color(0xFFAED581)),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(StudioPalette.Surface)
            .border(1.dp, StudioPalette.Stroke, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
    ) {
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            // Left main section: Tabs + Grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                // elementTabHolder: Horizontal tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = StudioPalette.Surface,
                    contentColor = StudioPalette.Accent,
                    edgePadding = 8.dp,
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.5.sp,
                                    ),
                                    color = if (selectedTab == index) StudioPalette.Accent else StudioPalette.TextDim,
                                )
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val items = when (selectedTab) {
                    0 -> shapes
                    1 -> media
                    2 -> audio
                    3 -> objects
                    else -> shapes
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items) { item ->
                        Box(
                            modifier = Modifier
                                .height(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioPalette.ButtonBg)
                                .border(1.dp, StudioPalette.Stroke, RoundedCornerShape(8.dp))
                                .clickable {
                                    onSelectElement(tabs[selectedTab], item.name)
                                    onDismiss()
                                }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(item.icon, item.name, tint = item.color, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = StudioPalette.TextPrimary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            // Divider line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(StudioPalette.Stroke),
            )

            // Right sidebar: quickAddBar [Freehand, Vector, Text] + buttonCloseAdd (X)
            Column(
                modifier = Modifier
                    .width(62.dp)
                    .fillMaxHeight()
                    .background(StudioPalette.CanvasMatte),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    // buttonAddFreehandDrawing
                    IconButton(
                        onClick = { onSelectElement("Draw", "Freehand Drawing"); onDismiss() },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Gesture, "Freehand", tint = StudioPalette.TextPrimary, modifier = Modifier.size(18.dp))
                            Text("Freehand", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = StudioPalette.TextDim)
                        }
                    }

                    // buttonAddVectorDrawing
                    IconButton(
                        onClick = { onSelectElement("Draw", "Vector Drawing"); onDismiss() },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Draw, "Vector", tint = StudioPalette.TextPrimary, modifier = Modifier.size(18.dp))
                            Text("Vector", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = StudioPalette.TextDim)
                        }
                    }

                    // buttonAddText
                    IconButton(
                        onClick = { onSelectElement("Text", "Text Layer"); onDismiss() },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.TextFields, "Text", tint = StudioPalette.TextPrimary, modifier = Modifier.size(18.dp))
                            Text("Text", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = StudioPalette.TextDim)
                        }
                    }
                }

                // buttonCloseAdd (X button at bottom right)
                Box(
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StudioPalette.ButtonBg)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Close, "Close", tint = StudioPalette.TextPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
