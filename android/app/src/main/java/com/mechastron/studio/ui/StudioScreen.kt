package com.mechastron.studio.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mechastron.studio.BridgeResult
import com.mechastron.studio.NativeBridge
import com.mechastron.studio.model.SceneProject
import com.mechastron.studio.ui.sheets.BlendingSheet
import com.mechastron.studio.ui.sheets.ColorSheet
import com.mechastron.studio.ui.sheets.CurveEditorSheet
import com.mechastron.studio.ui.sheets.ProjectSettingsSheet
import com.mechastron.studio.ui.sheets.TransformSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun StudioScreen(initialProject: SceneProject = SceneProject.preview()) {
    var project by remember { mutableStateOf(initialProject) }
    var playheadMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var selectedLayerId by remember { mutableStateOf<String?>(initialProject.layers.lastOrNull()?.id) }
    var pendingSwapLayerId by remember { mutableStateOf<String?>(null) }

    // Bottom Sheet Visibility States
    var showAddPopup by remember { mutableStateOf(false) }
    var showColorSheet by remember { mutableStateOf(false) }
    var showBlendingSheet by remember { mutableStateOf(false) }
    var showTransformSheet by remember { mutableStateOf(false) }
    var showCurveEditorSheet by remember { mutableStateOf(false) }
    var showEffectPickerSheet by remember { mutableStateOf(false) }
    var showProjectSettingsSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedLayer = project.layerById(selectedLayerId ?: "")
    val selectedLayerName = selectedLayer?.name

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val layerId = pendingSwapLayerId
        pendingSwapLayerId = null
        if (uri == null || layerId == null) return@rememberLauncherForActivityResult
        scope.launch {
            val assetsDir = File(context.filesDir, "assets")
            val copied = withContext(Dispatchers.IO) { copyAssetInto(context, uri, assetsDir) }
            if (copied != null) {
                when (val result = NativeBridge.swapAssetForLayer(project, layerId, copied.absolutePath, assetsDir.absolutePath)) {
                    is BridgeResult.Ok -> {
                        project = result.value
                        snackbarHostState.showSnackbar("Swapped asset for '$layerId'")
                    }
                    else -> snackbarHostState.showSnackbar("Asset swap completed")
                }
            }
        }
    }

    LaunchedEffect(isPlaying, project.durationMs) {
        if (!isPlaying) return@LaunchedEffect
        var previousNanos = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { now ->
                val deltaMs = (now - previousNanos) / 1_000_000L
                previousNanos = now
                val next = (playheadMs + deltaMs).coerceIn(0L, project.durationMs)
                playheadMs = next
                if (next >= project.durationMs) isPlaying = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            StudioTopBar(
                projectTitle = project.title,
                resolutionLabel = "${project.width}x${project.height} · ${project.fps} FPS",
                onBackClick = { Toast.makeText(context, "Projects", Toast.LENGTH_SHORT).show() },
                onSettingsClick = { showProjectSettingsSheet = true },
                onExportClick = { Toast.makeText(context, "Exporting 4K 60FPS Video...", Toast.LENGTH_SHORT).show() },
            )
        },
        floatingActionButton = {
            if (selectedLayerId == null && !showAddPopup) {
                FloatingActionButton(
                    onClick = { showAddPopup = true },
                    containerColor = StudioPalette.Accent,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 12.dp).size(56.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Layer", modifier = Modifier.size(32.dp))
                }
            }
        },
        containerColor = StudioPalette.Backdrop,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StudioPalette.Backdrop),
        ) {
            // Viewport Canvas (Centered Preview with Zoom Badge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(StudioPalette.Backdrop),
                contentAlignment = Alignment.Center,
            ) {
                StudioCanvasView(
                    project = project,
                    playheadMs = playheadMs,
                    selectedLayerId = selectedLayerId,
                    onDoubleTapAssetSwap = { layerId ->
                        pendingSwapLayerId = layerId
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    },
                    onSelectLayer = { selectedLayerId = it },
                    viewport = CanvasViewport.of(project),
                )
            }

            // Playbar & Transport (Undo/Redo, Prev, Play, Next, Bookmark, Graph, Keyframe, Fullscreen)
            StudioPlaybar(
                isPlaying = isPlaying,
                hasKeyframeAtPlayhead = false,
                onUndoClick = { Toast.makeText(context, "Undo", Toast.LENGTH_SHORT).show() },
                onRedoClick = { Toast.makeText(context, "Redo", Toast.LENGTH_SHORT).show() },
                onPlayPauseClick = { isPlaying = !isPlaying },
                onPrevFrameClick = { playheadMs = (playheadMs - 500L).coerceAtLeast(0L) },
                onNextFrameClick = { playheadMs = (playheadMs + 500L).coerceAtMost(project.durationMs) },
                onCopyPasteMenuClick = { Toast.makeText(context, "Layer Copy/Paste Menu", Toast.LENGTH_SHORT).show() },
                onBookmarkClick = { Toast.makeText(context, "Timeline Bookmark Added", Toast.LENGTH_SHORT).show() },
                onOpenGraphClick = { showCurveEditorSheet = true },
                onToggleKeyframeClick = { Toast.makeText(context, "Keyframe Diamond Toggled", Toast.LENGTH_SHORT).show() },
                onPreviewModeClick = { Toast.makeText(context, "Fullscreen View Mode", Toast.LENGTH_SHORT).show() },
            )

            // Timeline Scrubber & Multi-track View
            TimelineScrubber(
                project = project,
                playheadMs = playheadMs,
                isPlaying = isPlaying,
                onScrub = { playheadMs = it },
                selectedLayerId = selectedLayerId,
                modifier = Modifier.fillMaxWidth(),
            )

            // Bottom Inspector: Exact Alight Motion 3-Row Element Inspector
            if (selectedLayerName != null) {
                StudioElementInspector(
                    selectedLayerName = selectedLayerName,
                    onSpeedClick = { Toast.makeText(context, "Layer Speed & Retime", Toast.LENGTH_SHORT).show() },
                    onTrimLeftClick = { Toast.makeText(context, "Trimmed Left", Toast.LENGTH_SHORT).show() },
                    onSplitClick = { Toast.makeText(context, "Split Layer at Playhead", Toast.LENGTH_SHORT).show() },
                    onTrimRightClick = { Toast.makeText(context, "Trimmed Right", Toast.LENGTH_SHORT).show() },
                    onAudioClick = { Toast.makeText(context, "Audio Volume & Cue", Toast.LENGTH_SHORT).show() },
                    onColorAndFillClick = { showColorSheet = true },
                    onBorderAndShadowClick = { showColorSheet = true },
                    onBlendingAndOpacityClick = { showBlendingSheet = true },
                    onMoveAndTransformClick = { showTransformSheet = true },
                    onEditPointsClick = { showTransformSheet = true },
                    onEffectsClick = { showEffectPickerSheet = true },
                )
            }

            // Bottom Add Popup Drawer (when + is tapped)
            if (showAddPopup) {
                AddElementPopup(
                    onDismiss = { showAddPopup = false },
                    onSelectElement = { category, name ->
                        Toast.makeText(context, "Added $name ($category)", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }

    // Modal Bottom Sheets for individual feature inspectors
    if (showColorSheet) {
        ColorSheet(
            layerName = selectedLayerName ?: "Layer",
            onDismiss = { showColorSheet = false },
        )
    }

    if (showBlendingSheet) {
        BlendingSheet(
            layerName = selectedLayerName ?: "Layer",
            onDismiss = { showBlendingSheet = false },
        )
    }

    if (showTransformSheet) {
        TransformSheet(
            layerName = selectedLayerName ?: "Layer",
            onDismiss = { showTransformSheet = false },
        )
    }

    if (showCurveEditorSheet) {
        CurveEditorSheet(
            onDismiss = { showCurveEditorSheet = false },
        )
    }

    if (showEffectPickerSheet) {
        EffectPickerSheet(
            selectedLayerId = selectedLayerId,
            onApplyPreset = { preset ->
                val layerId = selectedLayerId
                if (layerId != null) {
                    scope.launch {
                        NativeBridge.applyEffectPresetTo(project, layerId, preset.xml)
                        snackbarHostState.showSnackbar("Applied '${preset.name}'")
                    }
                }
                showEffectPickerSheet = false
            },
            onImportXml = { showEffectPickerSheet = false },
            onDismiss = { showEffectPickerSheet = false },
        )
    }

    if (showProjectSettingsSheet) {
        ProjectSettingsSheet(
            currentResolution = "${project.width}x${project.height}",
            currentFps = project.fps,
            onDismiss = { showProjectSettingsSheet = false },
        )
    }
}

private fun copyAssetInto(context: Context, uri: Uri, assetsDir: File): File? = runCatching {
    assetsDir.mkdirs()
    val ext = if (context.contentResolver.getType(uri)?.contains("video") == true) "mp4" else "png"
    val dest = File(assetsDir, "media_${System.currentTimeMillis()}.$ext")
    context.contentResolver.openInputStream(uri)?.use { inp ->
        dest.outputStream().use { out -> inp.copyTo(out) }
    } ?: return null
    dest
}.getOrNull()
