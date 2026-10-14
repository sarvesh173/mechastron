package com.mechastron.studio.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.IOException

/** MIME types offered to the system file picker when importing an effect. */
val EFFECT_XML_MIME_TYPES = arrayOf("text/xml", "application/xml", "text/plain", "*/*")

/**
 * A preset the user can apply, paired with the exact XML that defines it.
 *
 * The XML string is the source of truth; [id] and [name] exist only so the list
 * can be labelled without re-parsing every row on each recomposition.
 */
data class EffectPresetEntry(
    val id: String,
    val name: String,
    val effectType: String,
    val xml: String,
)

/**
 * Built-in presets shipped with the app.
 *
 * Each snippet uses the same grammar the native importer accepts, so a
 * built-in and a user-imported file travel exactly the same path — the native
 * parser validates every one of them, including these.
 */
fun builtInEffectPresets(): List<EffectPresetEntry> = listOf(
    EffectPresetEntry(
        id = "soft_glow",
        name = "Soft Glow",
        effectType = "glow",
        xml = """
            <effect id="soft_glow" name="Soft Glow" version="1.0" type="glow">
              <property name="radius" value="18"/>
              <property name="spread" value="0.35"/>
              <keyframes name="intensity">
                <keyframe time="0" value="0.15" ease="0.4,0,0.2,1"/>
                <keyframe time="600" value="1" ease="0.4,0,0.2,1"/>
                <keyframe time="2400" value="0.8" ease="0.175,0.885,0.32,1.275"/>
              </keyframes>
            </effect>
        """.trimIndent(),
    ),
    EffectPresetEntry(
        id = "depth_blur",
        name = "Depth Blur",
        effectType = "blur",
        xml = """
            <effect id="depth_blur" name="Depth Blur" version="1.0" type="blur">
              <property name="radius" value="24"/>
              <keyframes name="radius">
                <keyframe time="0" value="4" ease="0.42,0,0.58,1"/>
                <keyframe time="900" value="24" ease="0.42,0,0.58,1"/>
              </keyframes>
            </effect>
        """.trimIndent(),
    ),
    EffectPresetEntry(
        id = "warm_grade",
        name = "Warm Grade",
        effectType = "color_grade",
        xml = """
            <effect id="warm_grade" name="Warm Grade" version="1.0" type="color_grade">
              <property name="temperature" value="0.28"/>
              <property name="tint" value="0.06"/>
              <property name="saturation" value="1.12"/>
            </effect>
        """.trimIndent(),
    ),
    EffectPresetEntry(
        id = "ripple_shift",
        name = "Ripple Shift",
        effectType = "distort",
        xml = """
            <effect id="ripple_shift" name="Ripple Shift" version="1.0" type="distort">
              <property name="frequency" value="3.5"/>
              <keyframes name="amplitude">
                <keyframe time="0" value="0" ease="0.4,0,0.2,1"/>
                <keyframe time="1200" value="14" ease="0.4,0,0.2,1"/>
                <keyframe time="3000" value="0" ease="0.4,0,0.2,1"/>
              </keyframes>
            </effect>
        """.trimIndent(),
    ),
    EffectPresetEntry(
        id = "grain_field",
        name = "Grain Field",
        effectType = "other",
        xml = """
            <effect id="grain_field" name="Grain Field" version="1.0" type="other">
              <property name="amount" value="0.14"/>
              <property name="size" value="1.5"/>
            </effect>
        """.trimIndent(),
    ),
    EffectPresetEntry(
        id = "stutter_step",
        name = "Stutter Step",
        effectType = "time",
        xml = """
            <effect id="stutter_step" name="Stutter Step" version="1.0" type="time">
              <property name="hold_frames" value="3"/>
              <property name="stutter" value="0.5"/>
              <keyframes name="stutter">
                <keyframe time="0" value="1" ease="0.1,0.9,0.2,1"/>
                <keyframe time="1500" value="0" ease="0.1,0.9,0.2,1"/>
              </keyframes>
            </effect>
        """.trimIndent(),
    ),
)

/**
 * Bottom sheet listing every effect that can be applied to the selected layer.
 *
 * The sheet holds no state of its own beyond visibility: it reports the choice
 * and lets [StudioScreen] own the native call, so the picker stays reusable and
 * the launcher's lifetime is tied to the screen rather than to this sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectPickerSheet(
    selectedLayerId: String?,
    onApplyPreset: (EffectPresetEntry) -> Unit,
    onImportXml: () -> Unit,
    onDismiss: () -> Unit,
) {
    val presets = builtInEffectPresets()
    val enabled = selectedLayerId != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioPalette.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Effects",
                    style = MaterialTheme.typography.titleSmall,
                    color = StudioPalette.TextPrimary,
                )
                Text(
                    text = selectedLayerId ?: "No layer selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) StudioPalette.Selection else StudioPalette.Warning,
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioPalette.Backdrop),
            ) {
                items(presets, key = { it.id }) { preset ->
                    PresetRow(
                        preset = preset,
                        enabled = enabled,
                        onClick = { onApplyPreset(preset) },
                    )
                }
            }

            Button(
                onClick = onImportXml,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioPalette.SurfaceRaised,
                    contentColor = StudioPalette.TextPrimary,
                ),
            ) {
                Text("Import XML Effect", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PresetRow(
    preset: EffectPresetEntry,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioPalette.Backdrop)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = preset.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) StudioPalette.TextPrimary else StudioPalette.TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 10.dp),
        )
        Text(
            text = preset.effectType.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = StudioPalette.Accent,
        )
    }
}

/**
 * Best-effort display name for an imported snippet.
 *
 * Used only to label the status line after a successful apply. The native
 * parser is the authority on validity, so a wrong guess here cannot make a bad
 * file import — it just shows up as a less friendly label.
 */
fun effectNameFromXml(xml: String): String? =
    Regex("""\bname\s*=\s*"([^"]*)"""")
        .find(xml)
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf { it.isNotBlank() }

/**
 * Reads a picked document as UTF-8 text, or null if it cannot be opened.
 *
 * Failure is reported as null rather than an exception so the caller can put a
 * message on the status line; the content resolver is the authority on whether
 * the grant actually resolves.
 */
fun readEffectXml(context: Context, uri: Uri): String? = try {
    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
} catch (e: IOException) {
    null
} catch (e: SecurityException) {
    null
}