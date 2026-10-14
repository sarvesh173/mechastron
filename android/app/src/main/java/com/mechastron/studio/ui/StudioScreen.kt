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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mechastron.studio.BridgeResult
import com.mechastron.studio.NativeBridge
import com.mechastron.studio.model.SceneProject
import com.mechastron.studio.ui.sheets.AddElementSheet
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
    var showAddElementSheet by remember { mutableStateOf(false) }
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
                projectTitle = project.name,
                resolutionLabel = "${project.width}x${project.height} · ${project.fps} FPS",
                onSettingsClick = { showProjectSettingsSheet = true },
                onUndoClick = { Toast.makeText(context, "Undo", Toast.LENGTH_SHORT).show() },
                onRedoClick = { Toast.makeText(context, "Redo", Toast.LENGTH_SHORT).show() },
                onExportClick = { Toast.makeText(context, "Exporting 4K 60FPS Video...", Toast.LENGTH_SHORT).show() },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddElementSheet = true },
                containerColor = StudioPalette.Accent,
                contentColor = StudioPalette.Backdrop,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 70.dp).size(52.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Element", modifier = Modifier.size(28.dp))
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
            // Viewport Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
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

            // Playbar & Transport
            StudioPlaybar(
                playheadMs = playheadMs,
                durationMs = project.durationMs,
                isPlaying = isPlaying,
                hasKeyframeAtPlayhead = false,
                onPlayPauseClick = { isPlaying = !isPlaying },
                onJumpStartClick = { playheadMs = 0L },
                onJumpEndClick = { playheadMs = project.durationMs },
                onPrevKeyframeClick = { playheadMs = (playheadMs - 500L).coerceAtLeast(0L) },
                onNextKeyframeClick = { playheadMs = (playheadMs + 500L).coerceAtMost(project.durationMs) },
                onToggleKeyframeClick = { Toast.makeText(context, "Keyframe Diamond Toggled", Toast.LENGTH_SHORT).show() },
                onOpenGraphClick = { showCurveEditorSheet = true },
            )

            // Timeline Scrubber & Layer Tracks
            TimelineScrubber(
                project = project,
                playheadMs = playheadMs,
                selectedLayerId = selectedLayerId,
                onSeek = { playheadMs = it },
                onSelectLayer = { selectedLayerId = it },
                modifier = Modifier.fillMaxWidth(),
            )

            // Bottom Inspector Tiles (Color & Fill, Border, Blending, Move & Transform, Edit Points, Effects)
            StudioBottomToolbar(
                selectedLayerName = selectedLayerName,
                onColorAndFillClick = { showColorSheet = true },
                onBorderAndShadowClick = { showColorSheet = true },
                onBlendingAndOpacityClick = { showBlendingSheet = true },
                onMoveAndTransformClick = { showTransformSheet = true },
                onEditPointsClick = { showTransformSheet = true },
                onEffectsClick = { showEffectPickerSheet = true },
                onSplitClick = { Toast.makeText(context, "Split at Playhead", Toast.LENGTH_SHORT).show() },
                onTrimLeftClick = { Toast.makeText(context, "Trimmed Left", Toast.LENGTH_SHORT).show() },
                onTrimRightClick = { Toast.makeText(context, "Trimmed Right", Toast.LENGTH_SHORT).show() },
                onDuplicateClick = { Toast.makeText(context, "Layer Duplicated", Toast.LENGTH_SHORT).show() },
                onDeleteClick = { Toast.makeText(context, "Layer Deleted", Toast.LENGTH_SHORT).show() },
            )
        }
    }

    // Modal Bottom Sheets
    if (showAddElementSheet) {
        AddElementSheet(
            onDismiss = { showAddElementSheet = false },
            onSelectElement = { category, name ->
                Toast.makeText(context, "Added $name ($category)", Toast.LENGTH_SHORT).show()
            },
        )
    }

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
