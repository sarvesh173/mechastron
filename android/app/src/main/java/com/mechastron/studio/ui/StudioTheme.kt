package com.mechastron.studio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object StudioPalette {
    val Backdrop = Color(0xFF0B0B12)
    val Surface = Color(0xFF14141F)
    val SurfaceRaised = Color(0xFF1D1D2B)
    val ViewportBackdrop = Color(0xFF05050A)
    val TrackBackdrop = Color(0xFF161622)

    val TextPrimary = Color(0xFFE8E8F0)
    val TextDim = Color(0xFF8B8BA3)

    val Accent = Color(0xFF5B8CFF)
    val PlayedRegion = Color(0x225B8CFF)
    val Playhead = Color(0xFFFF5C7A)
    val KeyframeMarker = Color(0xFFFFC857)
    val MarkerGuide = Color(0x33FFC857)
    val ActiveBand = Color(0x1A5B8CFF)
    val TickLine = Color(0x2B8B8BA3)

    val PlaceholderFill = Color(0xFF4F46E5)
    val PlaceholderStroke = Color(0xFF7C74F0)
    val SolidFill = Color(0xFF232337)
    val Selection = Color(0xFFFFC857)
    val Warning = Color(0xFFFFB020)
}

private val StudioColorScheme = darkColorScheme(
    primary = StudioPalette.Accent,
    onPrimary = Color.White,
    secondary = StudioPalette.Playhead,
    background = StudioPalette.Backdrop,
    onBackground = StudioPalette.TextPrimary,
    surface = StudioPalette.Surface,
    onSurface = StudioPalette.TextPrimary,
    surfaceVariant = StudioPalette.SurfaceRaised,
    onSurfaceVariant = StudioPalette.TextDim,
)

@Composable
fun MechastronStudioTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = StudioColorScheme,
        content = content,
    )
}