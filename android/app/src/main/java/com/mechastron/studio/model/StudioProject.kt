package com.mechastron.studio.model

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class LayerType {
    Image,
    Text,
    VectorShape,
    SolidColor,
    Audio,
    NullController;

    companion object {
        fun fromWireName(value: String?): LayerType = when (value?.lowercase()) {
            "text" -> Text
            "vector_shape" -> VectorShape
            "solid_color" -> SolidColor
            "audio" -> Audio
            "null_controller" -> NullController
            else -> Image
        }

        fun toWireName(type: LayerType): String = when (type) {
            Image -> "image"
            Text -> "text"
            VectorShape -> "vector_shape"
            SolidColor -> "solid_color"
            Audio -> "audio"
            NullController -> "null_controller"
        }
    }
}

data class CubicBezier(val x1: Float, val y1: Float, val x2: Float, val y2: Float) {

    fun transform(x: Float): Float {
        if (x <= 0f) return 0f
        if (x >= 1f) return 1f

        var t = x
        repeat(8) {
            val currentX = sampleX(t) - x
            if (abs(currentX) < 1e-5f) return sampleY(t.coerceIn(0f, 1f))
            val dx = sampleDerivativeX(t)
            if (abs(dx) < 1e-6f) return sampleY(t.coerceIn(0f, 1f))
            t -= currentX / dx
        }
        return sampleY(t.coerceIn(0f, 1f))
    }

    private fun sampleX(t: Float): Float {
        val inv = 1f - t
        return 3f * inv * inv * t * x1 + 3f * inv * t * t * x2 + t * t * t
    }

    private fun sampleY(t: Float): Float {
        val inv = 1f - t
        return 3f * inv * inv * t * y1 + 3f * inv * t * t * y2 + t * t * t
    }

    private fun sampleDerivativeX(t: Float): Float {
        val inv = 1f - t
        return 3f * inv * inv * x1 + 6f * inv * t * (x2 - x1) + 3f * t * t * (1f - x2)
    }

    companion object {
        val Linear = CubicBezier(0f, 0f, 1f, 1f)
        val EaseInOut = CubicBezier(0.42f, 0f, 0.58f, 1f)
        val FastOutSlowIn = CubicBezier(0.4f, 0f, 0.2f, 1f)
        val BounceOut = CubicBezier(0.175f, 0.885f, 0.32f, 1.275f)
        val SharpSnap = CubicBezier(0.1f, 0.9f, 0.2f, 1f)
    }
}

data class Keyframe(val timeMs: Long, val value: Float, val easing: CubicBezier)

data class FloatKeyframeTrack(val defaultValue: Float, val keyframes: List<Keyframe>) {

    fun valueAt(timeMs: Long): Float {
        if (keyframes.isEmpty()) return defaultValue
        val first = keyframes.first()
        if (timeMs <= first.timeMs) return first.value
        val last = keyframes.last()
        if (timeMs >= last.timeMs) return last.value

        for (i in 0 until keyframes.size - 1) {
            val start = keyframes[i]
            val end = keyframes[i + 1]
            if (timeMs in start.timeMs..end.timeMs) {
                val span = (end.timeMs - start.timeMs).toFloat()
                if (span <= 0f) return start.value
                val raw = (timeMs - start.timeMs).toFloat() / span
                return start.value + (end.value - start.value) * start.easing.transform(raw)
            }
        }
        return defaultValue
    }

    fun withKeyframe(timeMs: Long, value: Float, easing: CubicBezier): FloatKeyframeTrack =
        FloatKeyframeTrack(
            defaultValue = defaultValue,
            keyframes = (keyframes.filterNot { it.timeMs == timeMs } + Keyframe(timeMs, value, easing))
                .sortedBy { it.timeMs },
        )
}

data class Transform(
    val posX: FloatKeyframeTrack = FloatKeyframeTrack(0f, emptyList()),
    val posY: FloatKeyframeTrack = FloatKeyframeTrack(0f, emptyList()),
    val scaleX: FloatKeyframeTrack = FloatKeyframeTrack(1f, emptyList()),
    val scaleY: FloatKeyframeTrack = FloatKeyframeTrack(1f, emptyList()),
    val rotationDeg: FloatKeyframeTrack = FloatKeyframeTrack(0f, emptyList()),
    val opacity: FloatKeyframeTrack = FloatKeyframeTrack(1f, emptyList()),
) {
    fun allTracks(): List<FloatKeyframeTrack> = listOf(posX, posY, scaleX, scaleY, rotationDeg, opacity)
}

data class EvaluatedTransform(
    val posX: Float,
    val posY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotationDeg: Float,
    val opacity: Float,
) {
    fun unproject(localX: Float, localY: Float): Pair<Float, Float> {
        val rad = Math.toRadians(rotationDeg.toDouble()).toFloat()
        val cosR = cos(-rad)
        val sinR = sin(-rad)
        val dx = localX - posX
        val dy = localY - posY
        val rotatedX = dx * cosR - dy * sinR
        val rotatedY = dx * sinR + dy * cosR
        return rotatedX / (if (scaleX == 0f) 1e-4f else scaleX) to
            rotatedY / (if (scaleY == 0f) 1e-4f else scaleY)
    }
}

data class Layer(
    val id: String,
    val name: String,
    val layerType: LayerType,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val parentLayerId: String?,
    val assetRef: String?,
    val transform: Transform = Transform(),
    /**
     * Applied effects, each held verbatim as its `<effect>…</effect>` block.
     *
     * Effect semantics live in the native engine, so the Kotlin side carries the
     * XML opaquely rather than modelling parameters it cannot evaluate. Keeping
     * the raw block is what makes [ProjectCodec] round-trip lossless: an effect
     * applied natively survives every later serialise/deserialise cycle instead of
     * being dropped on the next render, swap or export.
     */
    val effects: List<String> = emptyList(),
) {
    val effectCount: Int get() = effects.size

    fun isActiveAt(timeMs: Long): Boolean = timeMs in startTimeMs..endTimeMs

    fun evaluate(timeMs: Long): EvaluatedTransform = EvaluatedTransform(
        posX = transform.posX.valueAt(timeMs),
        posY = transform.posY.valueAt(timeMs),
        scaleX = transform.scaleX.valueAt(timeMs),
        scaleY = transform.scaleY.valueAt(timeMs),
        rotationDeg = transform.rotationDeg.valueAt(timeMs),
        opacity = transform.opacity.valueAt(timeMs).coerceIn(0f, 1f),
    )

    fun contentSize(project: SceneProject): Pair<Float, Float> = when (layerType) {
        LayerType.SolidColor, LayerType.NullController -> project.width.toFloat() to project.height.toFloat()
        LayerType.Text -> project.width * 0.6f to project.height * 0.08f
        else -> project.width * 0.4f to project.height * 0.4f
    }

    fun hitTest(project: SceneProject, timeMs: Long, stageX: Float, stageY: Float): Boolean {
        if (!isActiveAt(timeMs)) return false
        if (layerType == LayerType.NullController) return false
        val evaluated = evaluate(timeMs)
        if (evaluated.opacity <= 0.01f) return false
        val (w, h) = contentSize(project)
        val (localX, localY) = evaluated.unproject(stageX, stageY)
        return abs(localX) <= w / 2f && abs(localY) <= h / 2f
    }
}

data class SceneProject(
    val title: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val durationMs: Long,
    val layers: List<Layer>,
) {
    val frameDurationMs: Long get() = (1000.0 / fps.coerceAtLeast(1)).toLong().coerceAtLeast(1L)
    val totalFrames: Int get() = (durationMs / frameDurationMs).toInt() + 1
    val aspectRatio: Float get() = if (height == 0) 1f else width.toFloat() / height.toFloat()

    fun layerById(id: String): Layer? = layers.firstOrNull { it.id == id }

    fun keyframeTimes(): LongArray {
        val times = sortedSetOf<Long>()
        layers.forEach { layer ->
            layer.transform.allTracks().forEach { track ->
                track.keyframes.forEach { times.add(it.timeMs) }
            }
        }
        return times.toLongArray()
    }

    fun hitTest(timeMs: Long, stageX: Float, stageY: Float): String? =
        layers.lastOrNull { it.hitTest(this, timeMs, stageX, stageY) }?.id

    companion object {
        fun preview(): SceneProject {
            val ease = CubicBezier.BounceOut
            val hero = Layer(
                id = "hero_1",
                name = "Hero Layer",
                layerType = LayerType.Image,
                startTimeMs = 0,
                endTimeMs = 4000,
                parentLayerId = "ctrl_1",
                assetRef = "assets/hero.png",
                transform = Transform(
                    posX = FloatKeyframeTrack(0f, listOf(Keyframe(0, 0f, ease), Keyframe(1200, 720f, ease))),
                    posY = FloatKeyframeTrack(0f, listOf(Keyframe(0, 0f, ease), Keyframe(1200, 380f, ease))),
                    scaleX = FloatKeyframeTrack(1f, listOf(Keyframe(0, 0.6f, CubicBezier.FastOutSlowIn))),
                    scaleY = FloatKeyframeTrack(1f, listOf(Keyframe(0, 0.6f, CubicBezier.FastOutSlowIn))),
                    rotationDeg = FloatKeyframeTrack(0f, listOf(Keyframe(600, 12f, CubicBezier.SharpSnap))),
                    opacity = FloatKeyframeTrack(1f, listOf(Keyframe(0, 0f, CubicBezier.EaseInOut), Keyframe(400, 1f, CubicBezier.EaseInOut))),
                ),
            )
            val ctrl = Layer("ctrl_1", "Master Rig", LayerType.NullController, 0, 4000, null, null)
            return SceneProject("Untitled Mechastron Project", 3840, 2160, 60, 4000, listOf(ctrl, hero))
        }
    }
}

object CanvasSpec {
    const val WIDTH_4K = 3840
    const val HEIGHT_4K = 2160
    const val ASPECT_4K = 16f / 9f
}