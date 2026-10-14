package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * BorderShadowPanel — Border & Shadow panel (fragment_border_and_shadow.xml).
 *
 * Blueprint elements:
 *   - color_switch_holder  : ColorView + title + SwitchCompat
 *   - strokeWidthSpinner   : ValueSpinner + strokeWidthValue (bl1@20 pill)
 *   - capSelector          : Butt / Round / Square segmented selector
 *   - joinSelector         : Bevel / Round / Miter segmented selector
 *   - startShapeStyle / endShapeStyle : Start / End cap shape pickers
 *   - slider_border + border_value
 *   - shadowSettingsList + borderList RecyclerViews (placeholder)
 *
 * Background: A9 (#151515)
 */
@Composable
fun BorderShadowPanel(
    borderEnabled: Boolean = true,
    onBorderEnabledChanged: (Boolean) -> Unit = {},
    strokeWidth: Float = 8f,
    onStrokeWidthChanged: (Float) -> Unit = {},
    capStyle: StrokeCap = StrokeCap.Round,
    onCapStyleChanged: (StrokeCap) -> Unit = {},
    joinStyle: StrokeJoin = StrokeJoin.Round,
    onJoinStyleChanged: (StrokeJoin) -> Unit = {},
    startCap: ShapeCap = ShapeCap.Round,
    onStartCapChanged: (ShapeCap) -> Unit = {},
    endCap: ShapeCap = ShapeCap.Round,
    onEndCapChanged: (ShapeCap) -> Unit = {},
    shadowEnabled: Boolean = false,
    onShadowEnabledChanged: (Boolean) -> Unit = {},
    borderOpacity: Float = 1f,
    onBorderOpacityChanged: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioPalette.Surface)            // A9
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {

        /* ── Enable border toggle row ─────────────────────────── */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ColorView swatch
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(StudioPalette.W1)
                        .border(1.dp, StudioPalette.Stroke, RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Border",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = StudioPalette.TextPrimary,
                    ),
                )
            }
            Switch(
                checked = borderEnabled,
                onCheckedChange = onBorderEnabledChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = StudioPalette.Accent,
                    checkedTrackColor = StudioPalette.Accent.copy(alpha = 0.3f),
                    uncheckedThumbColor = StudioPalette.TextDim,
                    uncheckedTrackColor = StudioPalette.SurfaceRaised,
                ),
            )
        }

        if (borderEnabled) {
            /* ── Stroke width row ──────────────────────────────── */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Width",
                    style = MaterialTheme.typography.labelSmall.copy(color = StudioPalette.W1a30),
                    modifier = Modifier.width(40.dp),
                )
                Slider(
                    value = strokeWidth,
                    onValueChange = onStrokeWidthChanged,
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = StudioPalette.Accent,
                        activeTrackColor = StudioPalette.Accent,
                        inactiveTrackColor = StudioPalette.SurfaceRaised,
                    ),
                )
                // Value pill: round_rect4dp_bl1_20
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StudioPalette.Bl1a20)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "%.0f".format(strokeWidth),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = StudioPalette.TextPrimary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        ),
                    )
                }
            }

            /* ── Cap style selector ─────────────────────────────── */
            SelectorRow(
                label = "Cap",
                options = StrokeCap.entries.map { it.displayName },
                selectedIndex = StrokeCap.entries.indexOf(capStyle),
                onSelect = { idx -> onCapStyleChanged(StrokeCap.entries[idx]) },
            )

            /* ── Join style selector ────────────────────────────── */
            SelectorRow(
                label = "Join",
                options = StrokeJoin.entries.map { it.displayName },
                selectedIndex = StrokeJoin.entries.indexOf(joinStyle),
                onSelect = { idx -> onJoinStyleChanged(StrokeJoin.entries[idx]) },
            )

            /* ── Start / End cap shape pickers ──────────────────── */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ShapeCapPicker(
                    label = "Start",
                    selected = startCap,
                    onSelect = onStartCapChanged,
                    modifier = Modifier.weight(1f),
                )
                ShapeCapPicker(
                    label = "End",
                    selected = endCap,
                    onSelect = onEndCapChanged,
                    modifier = Modifier.weight(1f),
                )
            }

            /* ── Border opacity slider ──────────────────────────── */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Opacity",
                    style = MaterialTheme.typography.labelSmall.copy(color = StudioPalette.W1a30),
                    modifier = Modifier.width(40.dp),
                )
                Slider(
                    value = borderOpacity,
                    onValueChange = onBorderOpacityChanged,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = StudioPalette.Accent,
                        activeTrackColor = StudioPalette.Accent,
                        inactiveTrackColor = StudioPalette.SurfaceRaised,
                    ),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StudioPalette.Bl1a20)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "%.0f%%".format(borderOpacity * 100),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = StudioPalette.TextPrimary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        ),
                    )
                }
            }
        }

        /* ── Shadow section ────────────────────────────────────── */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Shadow",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = StudioPalette.TextPrimary,
                ),
            )
            Switch(
                checked = shadowEnabled,
                onCheckedChange = onShadowEnabledChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = StudioPalette.Accent,
                    checkedTrackColor = StudioPalette.Accent.copy(alpha = 0.3f),
                    uncheckedThumbColor = StudioPalette.TextDim,
                    uncheckedTrackColor = StudioPalette.SurfaceRaised,
                ),
            )
        }

        /* ── Shadow settings list placeholder ─────────────────── */
        if (shadowEnabled) {
            ShadowSettingsPlaceholder()
        }
    }
}

/* ─────────────────────────────────────────────────────────────
 * Selector row: label + segmented pill buttons (butt/round/square or bevel/round/miter)
 * ───────────────────────────────────────────────────────────── */

@Composable
private fun SelectorRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = StudioPalette.W1a30),
            modifier = Modifier.width(40.dp),
        )
        // CardView bg: selector_nor_s10_pre_s9
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = StudioPalette.S10),
        ) {
            Row(
                modifier = Modifier.padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                options.forEachIndexed { idx, opt ->
                    val isSelected = idx == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) StudioPalette.SurfaceRaised   // selected → S9
                                else StudioPalette.S10                        // normal stays card bg
                            )
                            .clickable { onSelect(idx) }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) StudioPalette.TextPrimary else StudioPalette.TextDim,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        )
                    }
                }
            }
        }
    }
}
