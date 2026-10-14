package com.mechastron.studio.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import com.mechastron.studio.BridgeResult
import com.mechastron.studio.NativeBridge
import com.mechastron.studio.model.LayerType
import com.mechastron.studio.model.SceneProject
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
    var statusLine by remember { mutableStateOf("Ready · ${NativeBridge.unavailableReason ?: "native engine linked"}") }
    var pendingSwapLayerId by remember { mutableStateOf<String?>(null) }
    var isEffectSheetOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val layerId = pendingSwapLayerId
        pendingSwapLayerId = null
        if (uri == null || layerId == null) return@rememberLauncherForActivityResult
        scope.launch {
            val assetsDir = File(context.filesDir, "assets")
            val copied = withContext(Dispatchers.IO) { copyAssetInto(context, uri, assetsDir) }
            if (copied == null) {
                statusLine = "Could not read selected media"
                return@launch
            }
            when (val result = NativeBridge.swapAssetForLayer(project, layerId, copied.absolutePath, assetsDir.absolutePath)) {
                is BridgeResult.Ok -> {
                    project = result.value
                    statusLine = "Swapped asset for '$layerId'"
                }
                is BridgeResult.Unavailable ->
                    statusLine = "Native swap unavailable · ${result.reason}"
                is BridgeResult.Failed ->
                    statusLine = "Swap failed · ${result.reason}"
            }
        }
    }

    fun applyEffect(effectXml: String, label: String) {
        val layerId = selectedLayerId
        if (layerId == null) {
            statusLine = "Select a layer before applying an effect"
            return
        }
        scope.launch {
            statusLine = when (val result = NativeBridge.applyEffectPresetTo(project, layerId, effectXml)) {
                is BridgeResult.Ok -> {
                    project = result.value
                    "Applied '$label' to '$layerId'"
                }
                is BridgeResult.Unavailable ->
                    "Effect engine unavailable · ${result.reason}"
                is BridgeResult.Failed ->
                    "Effect failed · ${result.reason}"
            }
        }
    }

    // Registered at the screen level rather than inside the sheet: launching an
    // external picker can tear the sheet down, and the result must still land.
    val importEffectXml = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val xml = withContext(Dispatchers.IO) { readEffectXml(context, uri) }
            if (xml == null) {
                statusLine = "Could not read the selected file"
                return@launch
            }
            applyEffect(xml, effectNameFromXml(xml) ?: "imported effect")
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioPalette.Backdrop)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StudioCanvasView(
            project = project,
            playheadMs = playheadMs,
            selectedLayerId = selectedLayerId,
            onDoubleTapAssetSwap = { layerId ->
                pendingSwapLayerId = layerId
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onSelectLayer = { layerId ->
                selectedLayerId = layerId
                statusLine = layerId?.let { "Selected '$it'" } ?: "Selection cleared"
            },
            modifier = Modifier.fillMaxWidth(),
        )

        TransportBar(
            project = project,
            playheadMs = playheadMs,
            isPlaying = isPlaying,
            onTogglePlay = { isPlaying = !isPlaying },
            onStepFrames = { delta ->
                playheadMs = (playheadMs + delta * project.frameDurationMs)
                    .coerceIn(0L, project.durationMs)
            },
            onJumpToStart = { playheadMs = 0L },
            onJumpToEnd = { playheadMs = project.durationMs },
            onExport = {
                scope.launch {
                    val out = File(context.filesDir, "exports/${project.title}.mepack")
                    statusLine = when (val r = NativeBridge.exportPackageTo(
                        project,
                        File(context.filesDir, "assets").absolutePath,
                        out.absolutePath,
                    )) {
                        is BridgeResult.Ok -> "Exported ${out.name}"
                        is BridgeResult.Unavailable -> "Export unavailable · ${r.reason}"
                        is BridgeResult.Failed -> "Export failed · ${r.reason}"
                    }
                }
            },
        )

        TimelineScrubber(
            project = project,
            playheadMs = playheadMs,
            isPlaying = isPlaying,
            onScrub = { ms -> playheadMs = ms.coerceIn(0L, project.durationMs) },
            selectedLayerId = selectedLayerId,
        )

        Text(
            text = statusLine,
            style = MaterialTheme.typography.labelSmall,
            color = StudioPalette.TextDim,
        )

        LayerPanel(
            project = project,
            selectedLayerId = selectedLayerId,
            onSelect = { id ->
                selectedLayerId = id
                playheadMs = project.layerById(id)?.startTimeMs ?: playheadMs
            },
            onOpenEffects = { isEffectSheetOpen = true },
        )

        if (isEffectSheetOpen) {
            EffectPickerSheet(
                selectedLayerId = selectedLayerId,
                onApplyPreset = { preset ->
                    isEffectSheetOpen = false
                    applyEffect(preset.xml, preset.name)
                },
                onImportXml = {
                    isEffectSheetOpen = false
                    importEffectXml.launch(EFFECT_XML_MIME_TYPES)
                },
                onDismiss = { isEffectSheetOpen = false },
            )
        }
    }
}

@Composable
private fun TransportBar(
    project: SceneProject,
    playheadMs: Long,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onStepFrames: (Long) -> Unit,
    onJumpToStart: () -> Unit,
    onJumpToEnd: () -> Unit,
    onExport: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportButton("⏮", onJumpToStart)
        TransportButton("◀|", { onStepFrames(-1) })
        TransportButton(if (isPlaying) "❚❚" else "▶", onTogglePlay, emphasised = true)
        TransportButton("|▶", { onStepFrames(1) })
        TransportButton("⏭", onJumpToEnd)
        Text(
            text = formatTimecode(playheadMs, project.fps),
            style = MaterialTheme.typography.labelMedium,
            color = StudioPalette.TextPrimary,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Button(
            onClick = onExport,
            colors = ButtonDefaults.buttonColors(
                containerColor = StudioPalette.SurfaceRaised,
                contentColor = StudioPalette.TextPrimary,
            ),
        ) {
            Text("Export .mepack", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun TransportButton(label: String, onClick: () -> Unit, emphasised: Boolean = false) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasised) StudioPalette.Accent else StudioPalette.SurfaceRaised,
            contentColor = StudioPalette.TextPrimary,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun LayerPanel(
    project: SceneProject,
    selectedLayerId: String?,
    onSelect: (String) -> Unit,
    onOpenEffects: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Layers",
                style = MaterialTheme.typography.labelMedium,
                color = StudioPalette.TextDim,
            )
            Button(
                onClick = onOpenEffects,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioPalette.SurfaceRaised,
                    contentColor = StudioPalette.TextPrimary,
                ),
            ) {
                val count = project.layerById(selectedLayerId ?: "")?.effectCount ?: 0
                Text(
                    text = if (count > 0) "Effects · $count" else "Effects",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StudioPalette.Surface),
        ) {
            items(project.layers, key = { it.id }) { layer ->
                val selected = layer.id == selectedLayerId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selected) StudioPalette.SurfaceRaised else StudioPalette.Surface)
                        .clickable { onSelect(layer.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = layer.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) StudioPalette.Selection else StudioPalette.TextPrimary,
                    )
                    Text(
                        text = layerTypeGlyph(layer.layerType),
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioPalette.TextDim,
                    )
                }
            }
        }
    }
}

private fun layerTypeGlyph(type: LayerType): String = when (type) {
    LayerType.Image -> "IMAGE"
    LayerType.Text -> "TEXT"
    LayerType.VectorShape -> "VECTOR"
    LayerType.SolidColor -> "SOLID"
    LayerType.Audio -> "AUDIO"
    LayerType.NullController -> "NULL"
}

private fun copyAssetInto(context: Context, uri: Uri, assetsDir: File): File? = runCatching {
    assetsDir.mkdirs()
    val extension = when (context.contentResolver.getType(uri)) {
        "image/png" -> "png"
        "image/jpeg" -> "jpg"
        "image/webp" -> "webp"
        else -> "png"
    }
    val destination = File(assetsDir, "swap_${System.currentTimeMillis()}.$extension")
    context.contentResolver.openInputStream(uri)?.use { input ->
        destination.outputStream().use { output -> input.copyTo(output) }
    } ?: return null
    destination
}.getOrNull()