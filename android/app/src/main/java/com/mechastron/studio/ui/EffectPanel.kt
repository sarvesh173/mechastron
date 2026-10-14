package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * EffectPanel — Effects panel (fragment_effects.xml).
 *
 * Blueprint elements:
 *   - effectList : list rows (icon + effectName + premium lock + visibility + grip)
 *   - effectSettingsHolder : header (icon, settingsEffectName,
 *                              buttonFxOverflow, buttonDelete)
 *                            + effectSettingsList (parameter rows)
 *
 * Background: A9 (#151515)
 */

/* ─────────────────────────────────────────────────────────────
 * Sample effect data model
 * ───────────────────────────────────────────────────────────── */

data class EffectItem(
    val id: Int,
    val name: String,
    val icon: ImageVector,
    val isPremium: Boolean = false,
    val isEnabled: Boolean = true,
)

private val sampleEffects = listOf(
    EffectItem(1, "Blur", Icons.Default.AutoAwesome),
    EffectItem(2, "Color Correction", Icons.Default.AutoAwesome, isPremium = true),
    EffectItem(3, "Glow", Icons.Default.AutoAwesome),
    EffectItem(4, "Vignette", Icons.Default.AutoAwesome),
    EffectItem(5, "Sharpen", Icons.Default.AutoAwesome),
)

/* ─────────────────────────────────────────────────────────────
 * Public composable
 * ───────────────────────────────────────────────────────────── */

@Composable
fun EffectPanel(
    effects: List<EffectItem> = sampleEffects,
    onEffectToggle: (EffectItem, Boolean) -> Unit = { _, _ -> },
    onEffectDelete: (EffectItem) -> Unit = { },
    onEffectClick: (EffectItem) -> Unit = { },
    onEffectAdd: () -> Unit = { },
    selectedEffectId: Int? = null,
    modifier: Modifier = Modifier,
) {
    val effectsList = remember { mutableStateListOf<EffectItem>().apply { addAll(effects) } }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioPalette.Surface)   // A9
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {

        /* ── Left: effect list ────────────────────────────────── */
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Section header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Effects",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = StudioPalette.TextPrimary,
                    ),
                )
                IconButton(
                    onClick = onEffectAdd,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Effect",
                        tint = StudioPalette.Accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(
                    items = effectsList,
                    key = { _, item -> item.id },
                ) { _, item ->
                    EffectListRow(
                        item = item,
                        isSelected = item.id == selectedEffectId,
                        onToggle = { enabled -> onEffectToggle(item, enabled) },
                        onDelete = {
                            effectsList.remove(item)
                            onEffectDelete(item)
                        },
                        onClick = { onEffectClick(item) },
                    )
                }
            }
        }

        /* ── Right: effect settings holder ────────────────────── */
        if (selectedEffectId != null) {
            val selectedEffect = effectsList.find { it.id == selectedEffectId }
            selectedEffect?.let { effect ->
                EffectSettingsHolder(
                    effect = effect,
                    onDelete = {
                        effectsList.remove(effect)
                        onEffectDelete(effect)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
 * Effect list row — icon + name + premium lock + visibility + grip
 * Matches fragment_effects.xml effect_list_item layout
 * ───────────────────────────────────────────────────────────── */

@Composable
private fun EffectListRow(
    item: EffectItem,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val backgroundColor = when {
        isSelected -> StudioPalette.SurfaceRaised     // selected state
        else -> StudioPalette.S10                      // normal state
    }
    val borderColor = when {
        isSelected -> StudioPalette.Accent.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(6.dp),
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Drag handle grip (far left)
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                tint = StudioPalette.TextDim,
                modifier = Modifier.size(14.dp),
            )

            // Effect icon
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                tint = if (item.isEnabled) StudioPalette.Accent else StudioPalette.TextDim,
                modifier = Modifier.size(18.dp),
            )

            // Effect name
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (item.isEnabled) StudioPalette.TextPrimary else StudioPalette.TextDim,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                ),
                modifier = Modifier.weight(1f),
            )

            // Premium lock indicator
            if (item.isPremium) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Premium",
                    tint = StudioPalette.Warning,
                    modifier = Modifier.size(12.dp),
                )
            }

            // Visibility toggle
            IconButton(
                onClick = { onToggle(!item.isEnabled) },
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = if (item.isEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (item.isEnabled) "Hide" else "Show",
                    tint = if (item.isEnabled) StudioPalette.Accent else StudioPalette.TextDim,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
