package com.mechastron.studio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Design tokens mapped 1:1 from the reference blueprint
 * (docs/reference-blueprint/UI_BLUEPRINT.md). Dark neutral ramp + mint accent.
 */
object StudioPalette {
    // Dark neutral ramp (backdrop surfaces)
    val A1 = Color(0xFF080808)      // deepest overlay
    val A9 = Color(0xFF151515)      // element inspector panel background
    val A10 = Color(0xFF1C1C1C)     // main edit screen background
    val A11 = Color(0xFF0C0C0C)     // pressed-state of panel buttons / preview surround
    val S8 = Color(0xFF282828)      // panel button normal state
    val S9 = Color(0xFF1D1D1D)      // timeline lane / value field background
    val S10 = Color(0xFF262626)     // move & transform panel background

    // Accent
    val Y1 = Color(0xFF00FFA8)      // primary mint-green accent (select bar, toast, playhead)
    val AccentGradMain = Color(0xFF69FC97)  // gradient start on timeline elements
    val AccentGradTint = Color(0xFF5CE5FF)  // gradient end on timeline elements
    val AccentColorMed = Color(0xFF29E59C)
    val AccentColorDarker = Color(0xFF52CC91)

    // Timeline element gradient colors (rows 1-6)
    val ElementGrad1Start = Color(0xFF69FC97)
    val ElementGrad1End = Color(0xFF5CE5FF)
    val ElementGrad2Start = Color(0xFF5FEBFF)
    val ElementGrad2End = Color(0xFF3C8CE7)
    val ElementGrad3Start = Color(0xFF7466F0)
    val ElementGrad3End = Color(0xFFCE9FFC)
    val ElementGrad4Start = Color(0xFFF16586)
    val ElementGrad4End = Color(0xFFFAD3A5)
    val ElementGrad5Start = Color(0xFFE6D68C)
    val ElementGrad5End = Color(0xFFE6F597)
    val ElementGrad6Start = Color(0xFF52EEE6)
    val ElementGrad6End = Color(0xFF4EA3D5)

    // Text
    val W1 = Color(0xFFFFFFFF)      // primary text
    val W1a30 = Color(0x4DFFFFFF)   // secondary labels (30% white)
    val W1a20 = Color(0x33FFFFFF)   // tick marks (20% white)
    val W1a5 = Color(0x0DFFFFFF)    // speed text pill (5% white)
    val G1 = Color(0xFFEAEAEC)      // tertiary text
    val D2 = Color(0xFF979797)      // muted icon/label

    // Bl1 (black alpha ramp)
    val Bl1a10 = Color(0x19000000)
    val Bl1a20 = Color(0x33000000)
    val Bl1a50 = Color(0x80000000)

    // Add popup visuals
    val AddPopupPanelBg = Color(0x20282828)      // S8 @ 12.5%
    val AddPopupSideTabBg = Color(0x4C262626)    // S8 @ 30%
    val AddPopupSideTabBgSel = Color(0xB2212121) // S8 @ 70%

    // Alias tokens used across existing components
    val Backdrop = A10
    val CanvasMatte = A10
    val Surface = A9
    val ButtonBg = S8
    val SurfaceRaised = S9
    val Stroke = Color(0xFF383838)
    val Accent = Y1
    val TextPrimary = W1
    val TextDim = D2
    val Playhead = Y1
    val KeyframeMarker = Y1
    val Selection = Y1
    val ShapeTrack = ElementGrad1Start
    val MediaTrack = ElementGrad2Start
    val TextTrack = Color(0xFFFFD600)          // Yellow
    val AudioTrack = Color(0xFFE040FB)         // Magenta
    val VuMeter = Y1

    // Legacy token aliases mapped onto the reference palette
    val ViewportBackdrop = CanvasMatte          // A10 viewport matte
    val TrackBackdrop = SurfaceRaised           // S9 timeline lanes
    val TickLine = W1a20                        // tick marks
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