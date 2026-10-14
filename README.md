# Mechastron

Mechastron is a clean-room motion graphics engine written in Rust with a Jetpack Compose Android studio interface. It is designed to render 4K video at 60 frames per second on mobile devices without dropping frames or triggering Android garbage collection pauses.

## Why this exists

Most mobile animation tools struggle with 4K timelines. A single uncompressed 3840×2160 RGBA frame is 33,177,600 bytes (roughly 31.6 MB). If an engine allocates new memory for every frame at 60 FPS, the system has to allocate and reclaim almost 1.9 GB of memory every second. On phones, this leads directly to aggressive thermal throttling, stuttering, and memory crashes.

Mechastron takes a different approach:
- **Zero-copy memory recycling**: The 4K buffer is allocated once on engine initialization. Frames composite directly into fixed memory regions or hardware surface locks (`ANativeWindow`).
- **Clean-room architecture**: Written from scratch in Rust with zero dependencies on third-party mobile motion libraries.
- **Strict modularity**: Every engine source file stays under 400 lines for maintainability and compiler predictability.

## Engine architecture

```
Mechastron Core (Rust)
├── render_4k.rs       -> Fixed 33.1MB contiguous buffer & SIMD row blitting
├── temporal_blend.rs  -> Circular frame history with shutter-angle motion blur
├── color_grade.rs     -> Precomputed 256-entry tone curve LUTs & 3D LUT sampler
├── effect_preset.rs   -> Standalone XML effect preset parser & layer injector
├── asset_swap.rs      -> Non-destructive asset replacement engine
├── package.rs         -> .mepack ZIP container archive reader/writer
├── html_export.rs     -> Standalone zero-dependency HTML5 canvas exporter
└── jni/               -> Android NDK C-ABI exports (render, temporal, effects)

Android Studio (Kotlin + Jetpack Compose)
├── StudioCanvasView   -> 4K aspect-ratio viewport with double-tap gesture replacer
├── TimelineScrubber   -> 60 FPS scrubber with keyframe diamonds & timecode
├── EffectPickerSheet  -> Modal bottom sheet for built-in and imported XML effects
└── NativeBridge       -> High-speed JNI bridge to the Rust engine
```

## Core features

### 1. 4K 60 FPS rendering pipeline
The native rasterizer computes cubic bezier keyframe interpolation (`easing.rs`), layer transforms (position, scale, rotation, opacity), and blits RGBA pixels in row chunks. Per-frame time budget is under 16.6 milliseconds.

### 2. Double-tap asset replacement
When an editor imports an XML project or template, double-tapping any image layer on the canvas opens the device media picker. The engine updates the asset reference in memory while leaving every position, scale, and easing keyframe untouched.

### 3. XML effect preset system
Custom visual effects are defined as portable XML snippets. The parser validates property parameters and keyframe tracks, injecting them into the target layer without touching existing layer timings or transform channels:

```xml
<effect id="soft_glow" name="Soft Glow" version="1.0" type="glow">
  <property name="radius" value="18"/>
  <property name="spread" value="0.35"/>
  <keyframes name="intensity">
    <keyframe time="0" value="0.15" ease="0.4,0,0.2,1"/>
    <keyframe time="600" value="1.0" ease="0.4,0,0.2,1"/>
  </keyframes>
</effect>
```

### 4. Portable project packaging (.mepack)
Projects export into a self-contained `.mepack` ZIP archive bundling `project.xml`, image assets, and audio files. Anyone can import this package onto another device and get identical renders with full asset linking.

### 5. Standalone HTML canvas export
Projects can also be saved as single-file HTML documents. These files contain the entire animation timeline rendered directly in JavaScript and HTML5 Canvas, viewable in any desktop or mobile browser with zero plugins.

## Building and running

### Running native tests
You can run the full test suite locally using standard Cargo tooling:

```bash
cargo test
cargo clippy
```

### Building the Android APK
Building the Android application with 64-bit and 32-bit native NDK libraries is handled automatically by GitHub Actions so you do not need to compile NDK binaries on a low-power development machine.

1. Push your changes to GitHub on `main` or a `feat/*` branch.
2. The `.github/workflows/build-apk.yml` workflow triggers automatically.
3. The runner sets up JDK 17, Android NDK 26, and the Rust cross-compilation targets (`aarch64-linux-android`, `armv7-linux-androideabi`, `x86_64-linux-android`).
4. `cargo-ndk` builds `libmechastron.so` and Gradle bundles the release package.
5. Download the completed APK directly from the **Actions** tab artifacts.

## License

MIT License. Designed and built from scratch.
