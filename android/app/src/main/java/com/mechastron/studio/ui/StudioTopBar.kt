package com.mechastron.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * NavBar per blueprint section 'NavBar' (activity_edit_navbar.xml) — 42dp tall
 * Horizontal layout: [exitToProjectList 40×42dp] -> [projectTitle flex] -> [editTimeView]
 * -> [projectSettings 40×42dp] -> [share 40×42dp] -> [groupSettings 40×42dp when visible]
 */
@Composable
fun StudioTopBar(
    projectTitle: String,
    timecode: String = "00:00.00",
    selectedLayerName: String? = null,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onGroupSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (selectedLayerName != null) {
        // Element actionbar mode (fragment_element_actionbar.xml):
        // [navBack] [elementLabel] [layerParent] [delete] [overflow]
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(StudioPalette.A9),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavbarIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back to Project", onBackClick)
            Text(
                text = selectedLayerName,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                color = StudioPalette.W1,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 6.dp),
                maxLines = 1,
            )
            NavbarIconButton(Icons.Default.AccountTree, "Layer Parenting", onGroupSettingsClick)
            NavbarIconButton(Icons.Default.Delete, "Delete Layer", onExportClick)
            NavbarIconButton(Icons.Default.MoreVert, "Layer Overflow", onSettingsClick)
        }
        return
    }

    // Project navbar mode: [exitToProjectList] [projectTitle] [editTimeView]
    // [projectSettings] [share] [groupSettings]
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(StudioPalette.A9),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavbarIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Exit to Projects", onBackClick)

        Text(
            text = projectTitle,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            ),
            color = StudioPalette.W1,
            modifier = Modifier.weight(1f).padding(start = 2.dp),
            maxLines = 1,
        )

        // editTimeView — current playhead timecode in the navbar
        Text(
            text = timecode,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = StudioPalette.D2,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        NavbarIconButton(Icons.Default.Settings, "Project Settings", onSettingsClick)
        NavbarIconButton(Icons.Default.Share, "Export / Share", onExportClick)
        NavbarIconButton(Icons.Default.Group, "Group Settings", onGroupSettingsClick)
    }
}

@Composable
private fun NavbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(width = 40.dp, height = 42.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = StudioPalette.W1,
            modifier = Modifier.size(20.dp),
        )
    }
}