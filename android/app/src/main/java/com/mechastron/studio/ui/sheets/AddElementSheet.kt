package com.mechastron.studio.ui.sheets

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.mechastron.studio.ui.StudioPalette

data class ElementOption(val name: String, val icon: ImageVector, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddElementSheet(
    onDismiss: () -> Unit,
    onSelectElement: (String, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Shape", "Media", "Audio", "Text", "Elements")

    val shapes = listOf(
        ElementOption("Rectangle", Icons.Default.CropSquare, Color(0xFF64B5F6)),
        ElementOption("Circle", Icons.Default.RadioButtonUnchecked, Color(0xFF81C784)),
        ElementOption("Star", Icons.Default.Star, Color(0xFFFFD54F)),
        ElementOption("Polygon", Icons.Default.ChangeHistory, Color(0xFFFF8A65)),
        ElementOption("Vector Pen", Icons.Default.Edit, Color(0xFF4DB6AC)),
    )

    val media = listOf(
        ElementOption("Photo Gallery", Icons.Default.Image, Color(0xFFBA68C8)),
        ElementOption("Video Clip", Icons.Default.Videocam, Color(0xFFE57373)),
    )

    val audio = listOf(
        ElementOption("Sound Track", Icons.Default.Audiotrack, Color(0xFF4FC3F7)),
    )

    val text = listOf(
        ElementOption("Standard Text", Icons.Default.TextFields, Color(0xFFFFF176)),
    )

    val elements = listOf(
        ElementOption("Null Controller", Icons.Default.Category, Color(0xFF90A4AE)),
        ElementOption("Group / Precompose", Icons.Default.Layers, Color(0xFFAED581)),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioPalette.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Add to Project",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = StudioPalette.TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = StudioPalette.SurfaceRaised,
                contentColor = StudioPalette.Accent,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                                color = if (selectedTab == index) StudioPalette.Accent else StudioPalette.TextDim,
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentItems = when (selectedTab) {
                0 -> shapes
                1 -> media
                2 -> audio
                3 -> text
                else -> elements
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(currentItems) { item ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioPalette.SurfaceRaised)
                            .clickable {
                                onSelectElement(tabs[selectedTab], item.name)
                                onDismiss()
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.name,
                                tint = item.color,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = StudioPalette.TextPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}
