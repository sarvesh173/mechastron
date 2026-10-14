# Mechastron Technical Architecture

This document details the internal design of the Mechastron 4K 60 FPS motion graphics engine and its Android NDK integration.

## 1. Frame memory layout

Each 4K frame is stored as contiguous 8-bit RGBA pixels:
- Width: 3840 pixels
- Height: 2160 pixels
- Bytes per pixel: 4 (Red, Green, Blue, Alpha)
- Row stride: 15,360 bytes
- Total frame buffer: 33,177,600 bytes (~31.64 MB)

```
[Row 0: 15,360 bytes] -> [Pixel 0: R, G, B, A] ... [Pixel 3839: R, G, B, A]
[Row 1: 15,360 bytes] -> ...
...
[Row 2159: 15,360 bytes] -> Total: 33,177,600 contiguous bytes
```

### Zero-copy surface lifecycle
The `FrameBuffer4K` struct allocates this memory once on heap initialization behind a thread-safe singleton lock in `src/jni/render.rs`:

```rust
static SURFACE: Mutex<Option<FrameBuffer4K>> = Mutex::new(None);
```

When `renderFrame4K` executes from Android JNI, it acquires the surface lock, composites the active layers into this fixed buffer, and directly transfers or blits rows to the output target. No heap allocation occurs during active playback loops.

## 2. Temporal blend & motion blur accumulator

Motion blur is implemented in `src/temporal_blend.rs` via a circular history buffer (`TemporalBlend`):
- Configurable history ring: 1 to 16 historical frames.
- Shutter angle: normalized from 0.0 (no blur) to 1.0 (full 360-degree exposure across the entire frame interval).
- Weight distribution: frame weights scale linearly with time offset, normalized to integer scale 65536.
- In-place row composition: blends historical samples row-by-row into the destination buffer without creating temporary intermediate frame buffers.

## 3. High-speed color grading

Color grading in `src/color_grade.rs` avoids expensive floating-point trigonometric evaluations per pixel:
- `ToneLut`: On parameter updates (exposure, contrast, temperature, tint), a 256-element lookup table is calculated once for each RGB channel.
- Per-pixel operation: simple array indexing `r_lut[pixel[0]]` with ITU-R BT.709 luma preservation for saturation scaling.
- `Lut3D`: A 16×16×16 trilinear interpolation sampler provides cinematic film grade lookups.

## 4. JNI C-ABI bridge specification

The JNI layer is split across dedicated modules under `src/jni/` to respect the 400-line codebase constraint:
- `abi.rs`: Type aliases for JNI types (`JniEnv`, `JObject`, `JByteArray`, `JString`) and safe pointer conversions.
- `render.rs`: Core rasterization exports (`renderFrame4K`, `nativeRenderFrame4K`).
- `temporal.rs`: Motion blur accumulator export (`accumulateMotionBlur`).
- `mod.rs`: Native initialization and common error handling constants.

The library exports both standard Java JNI naming conventions (`Java_com_mechastron_studio_NativeBridge_...`) and clean aliases so linking never fails due to symbol name mismatch.

## 5. Android Studio UI layer

The Android interface is written in Jetpack Compose:
- `StudioCanvasView.kt`: Custom canvas maintaining 16:9 4K aspect ratio, listening for gestures. A double-tap event queries the active layer ID and fires a media replacement intent.
- `TimelineScrubber.kt`: Custom gesture-driven timeline with microsecond precision, displaying diamond keyframe ticks.
- `EffectPickerSheet.kt`: Compose ModalBottomSheet reading effect XML snippets and applying them non-destructively through the JNI bridge.
