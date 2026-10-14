package com.mechastron.studio.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mechastron.studio.NativeBridge
import com.mechastron.studio.model.CanvasSpec
import com.mechastron.studio.model.Layer
import com.mechastron.studio.model.LayerType
import com.mechastron.studio.model.SceneProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CanvasViewport(val width: Int, val height: Int, val label: String) {
    val aspectRatio: Float get() = width.toFloat() / height.toFloat()

    companion object {
        val FourK = CanvasViewport(CanvasSpec.WIDTH_4K, CanvasSpec.HEIGHT_4K, "4K · 3840×2160")
        val PortraitFullHd = CanvasViewport(1080, 1920, "1080×1920")
        fun of(project: SceneProject): CanvasViewport = CanvasViewport(project.width, project.height, "${project.width}×${project.height}")
    }
}

@Composable
fun StudioCanvasView(
    project: SceneProject,
    playheadMs: Long,
    selectedLayerId: String?,
    onDoubleTapAssetSwap: (String) -> Unit,
    onSelectLayer: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewport: CanvasViewport = CanvasViewport.FourK,
    useNativeRenderer: Boolean = true,
) {
    val stageWidth = viewport.width.coerceAtLeast(1)
    val stageHeight = viewport.height.coerceAtLeast(1)
    var measuredSize by remember { mutableStateOf(Size.Zero) }

    val currentProject by rememberUpdatedState(project)
    val currentPlayhead by rememberUpdatedState(playheadMs)

    val nativeFrame = if (useNativeRenderer) {
        rememberNativeFrame(project, playheadMs, stageWidth, stageHeight)
    } else {
        null
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(viewport.aspectRatio)
                .clip(RoundedCornerShape(10.dp))
                .background(StudioPalette.ViewportBackdrop)
                .onSizeChanged { measuredSize = Size(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(currentProject, stageWidth, stageHeight) {
                    detectTapGestures(
                        onTap = { offset ->
                            val stagePoint = offset.toStage(stageWidth, stageHeight, measuredSize)
                            onSelectLayer(
                                currentProject.hitTest(currentPlayhead, stagePoint.first, stagePoint.second),
                            )
                        },
                        onDoubleTap = { offset ->
                            val stagePoint = offset.toStage(stageWidth, stageHeight, measuredSize)
                            val layerId = currentProject.hitTest(currentPlayhead, stagePoint.first, stagePoint.second)
                            if (layerId != null) {
                                onDoubleTapAssetSwap(layerId)
                            }
                        },
                        onLongPress = { offset ->
                            val stagePoint = offset.toStage(stageWidth, stageHeight, measuredSize)
                            val layerId = currentProject.hitTest(currentPlayhead, stagePoint.first, stagePoint.second)
                            if (layerId != null) {
                                onSelectLayer(layerId)
                            }
                        },
                    )
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawStage(
                    project = project,
                    playheadMs = playheadMs,
                    selectedLayerId = selectedLayerId,
                    stageWidth = stageWidth,
                    stageHeight = stageHeight,
                    nativeFrame = nativeFrame,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = viewport.label,
                style = MaterialTheme.typography.labelSmall,
                color = StudioPalette.TextDim,
            )
            Text(
                text = if (NativeBridge.isAvailable) "Native 4K" else "Preview 4K",
                style = MaterialTheme.typography.labelSmall,
                color = if (NativeBridge.isAvailable) StudioPalette.Accent else StudioPalette.Warning,
            )
        }
    }
}

private fun Offset.toStage(stageWidth: Int, stageHeight: Int, measured: Size): Pair<Float, Float> {
    val w = if (measured.width > 0f) measured.width else x
    val h = if (measured.height > 0f) measured.height else y
    val ratioX = if (w > 0f) w else 1f
    val ratioY = if (h > 0f) h else 1f
    return (x / ratioX * stageWidth) to (y / ratioY * stageHeight)
}

private fun DrawScope.drawStage(
    project: SceneProject,
    playheadMs: Long,
    selectedLayerId: String?,
    stageWidth: Int,
    stageHeight: Int,
    nativeFrame: ImageBitmap?,
) {
    val bitmap = nativeFrame
    if (bitmap != null) {
        drawImage(
            image = bitmap,
            dstSize = IntSize(size.width.roundToIntSafe(), size.height.roundToIntSafe()),
        )
        return
    }

    val scaleX = size.width / stageWidth
    val scaleY = size.height / stageHeight

    project.layers.forEach { layer ->
        if (!layer.isActiveAt(playheadMs)) return@forEach
        val evaluated = layer.evaluate(playheadMs)
        if (evaluated.opacity <= 0.01f) return@forEach

        withTransform({
            translate(size.width / 2f, size.height / 2f)
            scale(scaleX, scaleY, pivot = Offset.Zero)
            translate(evaluated.posX, evaluated.posY)
            rotate(evaluated.rotationDeg, pivot = Offset.Zero)
            scale(evaluated.scaleX, evaluated.scaleY, pivot = Offset.Zero)
        }) {
            drawLayerContent(layer, project, selectedLayerId == layer.id)
        }
    }
}

private fun DrawScope.drawLayerContent(layer: Layer, project: SceneProject, selected: Boolean) {
    val (contentW, contentH) = layer.contentSize(project)
    val topLeft = Offset(-contentW / 2f, -contentH / 2f)
    val boxSize = Size(contentW, contentH)

    when (layer.layerType) {
        LayerType.SolidColor -> drawRect(
            color = StudioPalette.SolidFill,
            topLeft = topLeft,
            size = boxSize,
        )

        LayerType.NullController -> Unit

        else -> {
            drawRect(
                color = StudioPalette.PlaceholderFill,
                topLeft = topLeft,
                size = boxSize,
            )
            drawRect(
                color = StudioPalette.PlaceholderStroke,
                topLeft = topLeft,
                size = boxSize,
                style = Stroke(width = 6f),
            )
        }
    }

    if (selected) {
        drawRect(
            color = StudioPalette.Selection,
            topLeft = topLeft,
            size = boxSize,
            style = Stroke(width = 10f),
        )
    }
}

private fun Float.roundToIntSafe(): Int = kotlin.math.round(this).toInt()

@Composable
private fun rememberNativeFrame(
    project: SceneProject,
    playheadMs: Long,
    width: Int,
    height: Int,
): ImageBitmap? {
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(project, playheadMs, width, height) {
        if (!NativeBridge.isAvailable) {
            frame = null
            return@LaunchedEffect
        }
        try {
            // Render at lightweight quarter preview resolution for mobile display
            val previewW = 960.coerceAtMost(width)
            val previewH = ((previewW.toFloat() / width.toFloat()) * height).toInt().coerceAtLeast(1)
            val result = withContext(Dispatchers.Default) {
                NativeBridge.renderFrame(project, playheadMs, previewW, previewH)
            }
            val rendered = result.valueOrNull() ?: run {
                frame = null
                return@LaunchedEffect
            }
            val bitmap = Bitmap.createBitmap(rendered.width, rendered.height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(rendered.rgba))
            frame = bitmap.asImageBitmap()
        } catch (_: Throwable) {
            frame = null
        }
    }

    return frame
}