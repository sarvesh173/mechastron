package com.mechastron.studio.model

private fun String.attr(name: String): String? {
    val key = "$name=\""
    val start = indexOf(key)
    if (start < 0) return null
    val from = start + key.length
    val end = indexOf('"', from)
    if (end < 0) return null
    return substring(from, end)
}

private fun String.attrFloat(name: String, fallback: Float): Float = attr(name)?.toFloatOrNull() ?: fallback

private fun String.attrLong(name: String, fallback: Long): Long = attr(name)?.toLongOrNull() ?: fallback

private fun String.attrInt(name: String, fallback: Int): Int = attr(name)?.toIntOrNull() ?: fallback

private fun String.escaped(): String = this
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

private fun parseTrack(chunk: String, name: String): FloatKeyframeTrack? {
    val marker = "<track name=\"$name\""
    val start = chunk.indexOf(marker)
    if (start < 0) return null
    val sub = chunk.substring(start)
    val headerEnd = sub.indexOf('>')
    if (headerEnd < 0) return null
    val selfClosed = sub.getOrNull(headerEnd - 1) == '/'
    val body = if (selfClosed) {
        sub.substring(0, headerEnd + 1)
    } else {
        val closeIdx = sub.indexOf("</track>", headerEnd)
        if (closeIdx >= 0) sub.substring(0, closeIdx) else sub
    }

    val defaultValue = body.attrFloat("default", 0f)
    val keyframes = body.split("<keyframe ")
        .drop(1)
        .mapNotNull { raw ->
            val time = raw.attrLong("time", 0L)
            val value = raw.attrFloat("value", 0f)
            val easing = parseEasing(raw.attr("ease"))
            Keyframe(time, value, easing)
        }
        .sortedBy { it.timeMs }
    return FloatKeyframeTrack(defaultValue, keyframes)
}

private fun parseEasing(raw: String?): CubicBezier {
    val parts = raw.orEmpty().split(",").mapNotNull { it.trim().toFloatOrNull() }
    return if (parts.size == 4) CubicBezier(parts[0], parts[1], parts[2], parts[3]) else CubicBezier.FastOutSlowIn
}

private fun parseTransform(chunk: String): Transform {
    val defaults = Transform()
    return Transform(
        posX = parseTrack(chunk, "pos_x") ?: defaults.posX,
        posY = parseTrack(chunk, "pos_y") ?: defaults.posY,
        scaleX = parseTrack(chunk, "scale_x") ?: defaults.scaleX,
        scaleY = parseTrack(chunk, "scale_y") ?: defaults.scaleY,
        rotationDeg = parseTrack(chunk, "rotation_deg") ?: defaults.rotationDeg,
        opacity = parseTrack(chunk, "opacity") ?: defaults.opacity,
    )
}

/**
 * Extracts every `<effect>…</effect>` block from a layer, verbatim.
 *
 * Truncated blocks (no closing tag) are skipped rather than half-carried, so a
 * damaged project cannot smuggle malformed XML into the next native call.
 */
private fun parseEffects(chunk: String): List<String> =
    chunk.split("<effect ")
        .drop(1)
        .mapNotNull { raw ->
            val close = raw.indexOf("</effect>")
            if (close < 0) null else "<effect " + raw.substring(0, close + "</effect>".length)
        }

private fun parseLayers(xml: String, durationMs: Long): List<Layer> =
    xml.split("<layer ")
        .drop(1)
        .mapIndexedNotNull { index, raw ->
            val end = raw.indexOf("</layer>").let { if (it < 0) raw.length else it }
            val chunk = raw.substring(0, end)
            Layer(
                id = chunk.attr("id") ?: "layer_$index",
                name = chunk.attr("name") ?: "Layer $index",
                layerType = LayerType.fromWireName(chunk.attr("type")),
                startTimeMs = chunk.attrLong("start_time", 0L),
                endTimeMs = chunk.attrLong("end_time", durationMs),
                parentLayerId = chunk.attr("parent"),
                assetRef = chunk.attr("asset_ref"),
                transform = parseTransform(chunk),
                effects = parseEffects(chunk),
            )
        }

object ProjectCodec {

    fun deserialize(xml: String): SceneProject {
        val duration = xml.attrLong("duration_ms", 5000L)
        return SceneProject(
            title = xml.attr("title") ?: "Untitled Project",
            width = xml.attrInt("width", 1080),
            height = xml.attrInt("height", 1920),
            fps = xml.attrInt("fps", 60),
            durationMs = duration,
            layers = parseLayers(xml, duration),
        )
    }

    fun serialize(project: SceneProject): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append(
            "<mechastron_project title=\"${project.title.escaped()}\" width=\"${project.width}\" " +
                "height=\"${project.height}\" fps=\"${project.fps}\" duration_ms=\"${project.durationMs}\" " +
                "engine=\"Mechastron v1.0\">\n",
        )
        project.layers.forEach { layer ->
            append("  <layer id=\"${layer.id.escaped()}\" name=\"${layer.name.escaped()}\" ")
            append("type=\"${LayerType.toWireName(layer.layerType)}\" ")
            append("start_time=\"${layer.startTimeMs}\" end_time=\"${layer.endTimeMs}\"")
            layer.parentLayerId?.let { append(" parent=\"${it.escaped()}\"") }
            layer.assetRef?.let { append(" asset_ref=\"${it.escaped()}\"") }
            append(">\n    <transform>\n")
            appendTrack("pos_x", layer.transform.posX)
            appendTrack("pos_y", layer.transform.posY)
            appendTrack("scale_x", layer.transform.scaleX)
            appendTrack("scale_y", layer.transform.scaleY)
            appendTrack("rotation_deg", layer.transform.rotationDeg)
            appendTrack("opacity", layer.transform.opacity)
            append("    </transform>\n")
            if (layer.effects.isNotEmpty()) {
                append("    <effects>\n")
                layer.effects.forEach { append(it.trimEnd()).append('\n') }
                append("    </effects>\n")
            }
            append("  </layer>\n")
        }
        append("</mechastron_project>\n")
    }

    private fun StringBuilder.appendTrack(name: String, track: FloatKeyframeTrack) {
        if (track.keyframes.isEmpty()) {
            append("      <track name=\"$name\" default=\"${track.defaultValue}\"/>\n")
            return
        }
        append("      <track name=\"$name\" default=\"${track.defaultValue}\">\n")
        track.keyframes.forEach { kf ->
            val e = kf.easing
            append(
                "        <keyframe time=\"${kf.timeMs}\" value=\"${kf.value}\" " +
                    "ease=\"${e.x1},${e.y1},${e.x2},${e.y2}\"/>\n",
            )
        }
        append("      </track>\n")
    }
}