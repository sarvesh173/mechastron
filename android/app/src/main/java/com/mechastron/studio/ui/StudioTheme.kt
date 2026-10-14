package com.mechastron.studio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Exact color palette reverse-engineered from reference motion engine:
 * A11: Deep background (#0C0C0C)
 * A10: Workspace surround (#1C1C1C)
 * A9:  Panel background (#151515)
 * S8:  Panel button fill (#282828)
 * S9:  Surface raised/stroke (#1D1D1D)
 * Y1:  Iconic Neon Mint Accent (#00FFA8)
 * W1:  Primary text (#FFFFFF)
 * D2:  Muted icon/label (#979797)
 */
object StudioPalette {
    val Backdrop = Color(0xFF0C0C0C)           // A11
    val CanvasMatte = Color(0xFF1C1C1C)        // A10
    val Surface = Color(0xFF151515)            // A9
    val ButtonBg = Color(0xFF282828)           // S8
    val SurfaceRaised = Color(0xFF1D1D1D)      // S9
    val Stroke = Color(0xFF383838)             // Button border
    val Accent = Color(0xFF00FFA8)             // Y1 (amAccentColor)
    val TextPrimary = Color(0xFFFFFFFF)        // W1
    val TextDim = Color(0xFF979797)            // D2
    val Playhead = Color(0xFFFF3B30)           // Red scrubber
    val KeyframeMarker = Color(0xFF00FFA8)     // Mint diamond
    val Selection = Color(0xFF00FFA8)          // Bounding box
    val ShapeTrack = Color(0xFF00FFA8)         // Green
    val MediaTrack = Color(0xFF00E5FF)         // Cyan
    val TextTrack = Color(0xFFFFD600)          // Yellow
    val AudioTrack = Color(0xFFE040FB)         // Magenta
    val VuMeter = Color(0xFF00FFA8)            // Green VU meter

    // Legacy token aliases mapped onto the reference palette
    val ViewportBackdrop = CanvasMatte          // A10 viewport matte
    val TrackBackdrop = SurfaceRaised           // S9 timeline lanes
    val TickLine = Color(0x33FFFFFF)            // W1_a20 tick marks
    val PlayedRegion = Color(0x3300FFA8)        // Y1 @20% played span
    val ActiveBand = Color(0x1A00FFA8)          // Y1 @10% active band
    val MarkerGuide = Color(0x3300FFA8)         // Keyframe glow guide
    val SolidFill = ButtonBg                    // S8 solid shape fill
    val PlaceholderFill = Color(0x3300FFA8)     // Mint placeholder asset
    val PlaceholderStroke = Color(0x8800FFA8)   // Mint placeholder border
    val Warning = Color(0xFFFFB020)             // Amber warnings
}

private val DarkColorScheme = darkColorScheme(
    primary = StudioPalette.Accent,
    background = StudioPalette.Backdrop,
    surface = StudioPalette.Surface,
    onPrimary = Color.Black,
    onBackground = StudioPalette.TextPrimary,
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
        colorScheme = DarkColorScheme,
        content = content,
    )
}
