package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pixels
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Assembly of the entire edit screen per the blueprint's
 * 'Edit Screen Layout Hierarchy' + 'Interaction Map'.
 *
 * Vertical stack:
 *   1. StudioTopBar (42dp)
 *   2. previewGuide (weight 1f, bg A11) + viewmodeHolder (50dp, bg S9)
 *   3. StudioPlaybar (46dp)
 *   4. StudioInfoBar (46dp)
 *   5. TimelinePanel (280dp)
 *   6. StudioElementInspector (200dp)
 *   Overlays: timelineOverflowButton (40x40dp), buttonAdd FAB (73dp),
 *             AddElementPopup (251dp), modal panels.
 *
 * Interaction Map (buttons -> menus, no feature logic):
 *   buttonAdd (FAB)         -> showAddElementPopup
 *   buttonColorAndFill      -> showColorFillPanel
 *   buttonBorderAndShadow   -> showBorderShadowPanel
 *   buttonBlendingAndOpacity-> showBlendingOpacityPanel
 *   buttonMoveAndTransform  -> showMoveTransformPanel
 *   buttonEffects           -> showEffectPanel
 *   timelineOverflowButton  -> showTimelineOverflow
 */
/** Sample timeline layers matching the 6-row gradient cycling of the reference. */
private val sampleTimelineLayers = listOf(
    TimelineLayer("l1", "Rectangle", LayerType.SHAPE, 0L, 4000L, hasKeyframes = true, hasMask = false),
    TimelineLayer("l2", "Circle", LayerType.SHAPE, 500L, 3000L, hasKeyframes = false, hasMask = false),
    TimelineLayer("l3", "Background", LayerType.MEDIA, 0L, 4000L, hasKeyframes = false, hasMask = true),
    TimelineLayer("l4", "Text", LayerType.TEXT, 1000L, 2000L, hasKeyframes = true, hasMask = false),
    TimelineLayer("l5", "Music", LayerType.AUDIO, 0L, 4000L, hasKeyframes = false, hasMask = false),
    TimelineLayer("l6", "Group", LayerType.SHAPE, 2000L, 1500L, hasKeyframes = false, hasMask = false),
)

@Composable
fun StudioScreen() {
    var showAddElementPopup by remember { mutableStateOf(false) }
    var showColorFillPanel by remember { mutableStateOf(false) }
    var showBorderShadowPanel by remember { mutableStateOf(false) }
    var showBlendingOpacityPanel by remember { mutableStateOf(false) }
    var showEffectPanel by remember { mutableStateOf(false) }
    var showMoveTransformPanel by remember { mutableStateOf(false) }
    var showTimelineOverflow by remember { mutableStateOf(false) }
    var showSelectBar by remember { mutableStateOf(false) }
    var showSelectBottomBar by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioPalette.A10),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. NavBar — 42dp
            StudioTopBar(
                projectTitle = "Untitled Project",
                timecode = "00:00.00",
                onBackClick = {},
                onSettingsClick = {},
                onExportClick = {},
                onGroupSettingsClick = {},
            )

            // 2. previewGuide (SurfaceView area, bg A11) + viewmodeHolder (50dp, bg S9)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(StudioPalette.A11),
                )
                ViewmodeHolder()
            }

            // 3. Playbar — 46dp
            StudioPlaybar(
                isPlaying = false,
                hasKeyframeAtPlayhead = false,
                onUndoClick = {},
                onRedoClick = {},
                onPlayPauseClick = {},
                onPrevFrameClick = {},
                onNextFrameClick = {},
                onCopyPasteMenuClick = {},
                onBookmarkClick = {},
                onPreviewModeClick = {},
            )

            // 4. InfoBar — 46dp
            StudioInfoBar()

            // timeline_top_space — 42dp spacer per blueprint
            Spacer(modifier = Modifier.height(42.dp))

            // 5. Timeline — 280dp
            TimelinePanel(
                layers = sampleTimelineLayers,
                modifier = Modifier.height(280.dp),
            )

            // 6. Element inspector / Select bars
            if (showSelectBar) {
                StudioSelectBar(selectedCount = 0)
            } else if (showSelectBottomBar) {
                StudioSelectBottomBar()
            } else {
                StudioElementInspector(
                    selectedLayerName = "Layer 1",
                    onSpeedClick = {},
                    onTrimLeftClick = {},
                    onSplitClick = {},
                    onTrimRightClick = {},
                    onAudioClick = {},
                    onColorAndFillClick = { showColorFillPanel = true },
                    onBorderAndShadowClick = { showBorderShadowPanel = true },
                    onBlendingAndOpacityClick = { showBlendingOpacityPanel = true },
                    onMoveAndTransformClick = { showMoveTransformPanel = true },
                    onEditPointsClick = { showMoveTransformPanel = true },
                    onEffectsClick = { showEffectPanel = true },
                )
            }
        }

        // 7. Timeline overflow button (40x40dp), above FAB
        IconButton(
            onClick = { showTimelineOverflow = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 112.dp)
                .size(40.dp),
        ) {
            Icon(Icons.Default.MoreVert, "Timeline Options", tint = StudioPalette.W1)
        }

        // 8. buttonAdd FAB — 73dp, radius 28dp, Y1 background
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp)
                .size(73.dp),
        ) {
            IconButton(
                onClick = { showAddElementPopup = true },
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(28.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(StudioPalette.Y1, RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Add,
                        "Add Element",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }

        // 9. Add popup — 251dp bottom panel
        if (showAddElementPopup) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                AddElementPopup(
                    onDismiss = { showAddElementPopup = false },
                    onSelectElement = { _, _ -> },
                )
            }
        }

        // 10. Modal panels (Interaction Map wiring — all params default, modifier full)
        if (showColorFillPanel) {
            ColorFillPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            )
        }
        if (showBorderShadowPanel) {
            BorderShadowPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            )
        }
        if (showBlendingOpacityPanel) {
            BlendingOpacityPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            )
        }
        if (showEffectPanel) {
            EffectPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            )
        }
        if (showMoveTransformPanel) {
            MoveTransformPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            )
        }
        if (showTimelineOverflow) {
            TimelineOverflowPopup(onDismiss = { showTimelineOverflow = false })
        }
    }
}

/** viewmodeHolder — 50dp right rail, bg S9: pixel/grid/solo/camera/zoom buttons. */
@Composable
private fun ViewmodeHolder() {
    Column(
        modifier = Modifier
            .width(50.dp)
            .background(StudioPalette.S9)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
    ) {
        ViewmodeIcon(Icons.Default.Pixels, "Pixel Mode")
        ViewmodeIcon(Icons.Default.GridOn, "Grid Mode")
        ViewmodeIcon(Icons.Default.RadioButtonUnchecked, "Solo Mode")
        ViewmodeIcon(Icons.Default.CameraAlt, "Camera Mode")
        ViewmodeIcon(Icons.Default.ZoomIn, "Canvas Zoom")
    }
}

@Composable
private fun ViewmodeIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
        Icon(icon, label, tint = StudioPalette.W1, modifier = Modifier.size(20.dp))
    }
}


/** Timeline overflow popup — timeline options menu. */
@Composable
private fun TimelineOverflowPopup(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .background(StudioPalette.S9, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Timeline Options", color = StudioPalette.W1, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Close", tint = StudioPalette.W1a30, modifier = Modifier.size(16.dp))
            }
        }
        TimelineOptionRow("Snap to grid")
        TimelineOptionRow("Show keyframes")
        TimelineOptionRow("Timeline settings")
    }
}

@Composable
private fun TimelineOptionRow(label: String) {
    Text(
        text = label,
        color = StudioPalette.W1a30,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    )
}
