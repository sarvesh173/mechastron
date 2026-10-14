package com.mechastron.studio

import android.os.Build
import com.mechastron.studio.model.ProjectCodec
import com.mechastron.studio.model.SceneProject

data class RenderedFrame(
    val rgba: ByteArray,
    val width: Int,
    val height: Int,
    val timeMs: Long,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is RenderedFrame &&
                    width == other.width &&
                    height == other.height &&
                    timeMs == other.timeMs &&
                    rgba.contentEquals(other.rgba)
                )

    override fun hashCode(): Int =
        (31 * (31 * width + height) + timeMs.toInt()) * 31 + rgba.contentHashCode()
}

sealed class BridgeResult<out T> {
    data class Ok<T>(val value: T) : BridgeResult<T>()
    data class Unavailable(val reason: String) : BridgeResult<Nothing>()
    data class Failed(val reason: String) : BridgeResult<Nothing>()

    val isOk: Boolean get() = this is Ok

    @Suppress("UNCHECKED_CAST")
    fun valueOrNull(): T? = (this as? Ok<T>)?.value
}

object NativeBridge {

    private const val LIBRARY_NAME = "mechastron"
    private const val BYTES_PER_PIXEL = 4

    @Volatile
    private var loadAttempted = false

    @Volatile
    private var loadFailure: String? = null

    @Volatile
    private var loadSucceeded = false

    val isAvailable: Boolean
        get() {
            if (!loadAttempted) {
                synchronized(this) {
                    if (!loadAttempted) {
                        loadAttempted = true
                        loadSucceeded = runCatching { System.loadLibrary(LIBRARY_NAME) }.isSuccess
                        loadFailure = if (loadSucceeded) {
                            null
                        } else {
                            "lib$LIBRARY_NAME.so missing for ABI ${Build.SUPPORTED_ABIS.joinToString()}"
                        }
                    }
                }
            }
            return loadSucceeded
        }

    val unavailableReason: String? get() = loadFailure

    private val bufferLock = Any()

    private external fun renderFrame4K(
        projectXml: String,
        timeMs: Long,
        width: Int,
        height: Int,
        outRgba: ByteArray,
    ): Int

    private external fun swapAsset(
        projectXml: String,
        layerId: String,
        newAssetPath: String,
        assetsDir: String,
    ): String

    private external fun exportPackage(
        projectXml: String,
        assetsDir: String,
        outputPath: String,
    ): String

    private external fun applyEffectPreset(
        projectXml: String,
        layerId: String,
        effectXml: String,
    ): String

    fun renderFrame(
        project: SceneProject,
        timeMs: Long,
        width: Int = project.width,
        height: Int = project.height,
    ): BridgeResult<RenderedFrame> {
        if (!isAvailable) return BridgeResult.Unavailable(unavailableReason ?: "native engine unavailable")
        val pixelCount = width * height
        if (pixelCount <= 0) return BridgeResult.Failed("invalid raster size ${width}x$height")
        return try {
            val buffer = ByteArray(pixelCount * BYTES_PER_PIXEL)
            val xml = ProjectCodec.serialize(project)
            synchronized(bufferLock) {
                val written = renderFrame4K(xml, timeMs, width, height, buffer)
                when {
                    written < 0 -> BridgeResult.Failed("renderFrame4K rejected time ${timeMs}ms (code $written)")
                    written > buffer.size -> BridgeResult.Failed("renderFrame4K overflowed buffer ($written > ${buffer.size})")
                    else -> BridgeResult.Ok(RenderedFrame(buffer.copyOf(written), width, height, timeMs))
                }
            }
        } catch (t: Throwable) {
            BridgeResult.Failed(t.message ?: t.javaClass.simpleName)
        }
    }

    fun swapAssetForLayer(
        project: SceneProject,
        layerId: String,
        newAssetPath: String,
        assetsDir: String,
    ): BridgeResult<SceneProject> {
        if (!isAvailable) return BridgeResult.Unavailable(unavailableReason ?: "native engine unavailable")
        if (project.layerById(layerId) == null) return BridgeResult.Failed("layer '$layerId' not in project")
        return try {
            val updatedXml = swapAsset(ProjectCodec.serialize(project), layerId, newAssetPath, assetsDir)
            BridgeResult.Ok(ProjectCodec.deserialize(updatedXml))
        } catch (t: Throwable) {
            BridgeResult.Failed(t.message ?: t.javaClass.simpleName)
        }
    }

    fun exportPackageTo(
        project: SceneProject,
        assetsDir: String,
        outputPath: String,
    ): BridgeResult<String> {
        if (!isAvailable) return BridgeResult.Unavailable(unavailableReason ?: "native engine unavailable")
        return try {
            val status = exportPackage(ProjectCodec.serialize(project), assetsDir, outputPath)
            if (status.isBlank() || status.startsWith("ERR")) BridgeResult.Failed(status.ifBlank { "export failed" })
            else BridgeResult.Ok(outputPath)
        } catch (t: Throwable) {
            BridgeResult.Failed(t.message ?: t.javaClass.simpleName)
        }
    }

    /**
     * Applies a preset snippet to [layerId] and returns the rewritten project.
     *
     * The native engine owns effect semantics; this only marshals. Validation of
     * the snippet happens natively, so a malformed file surfaces as a [BridgeResult.Failed]
     * carrying the parser's own message rather than a Kotlin-side guess.
     */
    fun applyEffectPresetTo(
        project: SceneProject,
        layerId: String,
        effectXml: String,
    ): BridgeResult<SceneProject> {
        if (!isAvailable) return BridgeResult.Unavailable(unavailableReason ?: "native engine unavailable")
        if (project.layerById(layerId) == null) return BridgeResult.Failed("layer '$layerId' not in project")
        if (effectXml.isBlank()) return BridgeResult.Failed("effect snippet is empty")
        return try {
            val updatedXml = applyEffectPreset(ProjectCodec.serialize(project), layerId, effectXml)
            BridgeResult.Ok(ProjectCodec.deserialize(updatedXml))
        } catch (t: Throwable) {
            BridgeResult.Failed(t.message ?: t.javaClass.simpleName)
        }
    }
}