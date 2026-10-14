//! Frame composition helpers shared by the JNI entry points.
//!
//! The scratch surface is process-global and mutex-guarded so concurrent
//! callers never reallocate the 33 MB block and never receive two mutable
//! views of it at once. Callers that need to observe a result *and* the pixels
//! it produced must do both inside a single [`with_surface`] closure.

use crate::jni::abi::JInt;
use crate::model::Layer;
use crate::render_4k::{FrameBuffer4K, Rgba, BYTES_PER_PIXEL, ROW_BYTES};
use std::sync::Mutex;

/// Immutable scratch surface, allocated on first use.
static SURFACE: Mutex<Option<FrameBuffer4K>> = Mutex::new(None);

/// Runs `f` against the shared surface while holding the lock.
pub fn with_surface<R>(f: impl FnOnce(&mut FrameBuffer4K) -> R) -> Option<R> {
    let mut guard = SURFACE.lock().ok()?;
    let fb = guard.get_or_insert_with(FrameBuffer4K::new);
    Some(f(fb))
}

/// Whether a layer's time range covers `time_ms`.
pub fn layer_is_live(layer: &Layer, time_ms: i64) -> bool {
    time_ms >= layer.start_time_ms && time_ms <= layer.end_time_ms
}

/// Top-left corner of a layer's quad in raster space, clamped to the target.
///
/// Position is evaluated at t=0 and combined with the track's default, which
/// is the static anchor; animated offsets are applied per frame by the caller.
pub fn layer_raster_origin(layer: &Layer, width: JInt, height: JInt) -> (usize, usize) {
    let transform = &layer.transform;
    let cx = width as f32 / 2.0;
    let cy = height as f32 / 2.0;
    let x = transform.pos_x.get_value_at(0) + transform.pos_x.default_value + cx;
    let y = transform.pos_y.get_value_at(0) + transform.pos_y.default_value + cy;
    (
        x.clamp(0.0, (width - 1).max(0) as f32) as usize,
        y.clamp(0.0, (height - 1).max(0) as f32) as usize,
    )
}

/// Blits one layer's evaluated transform into `fb`.
///
/// The layer occupies `[origin_x, origin_x + width)` on each destination row
/// it covers. Because [`FrameBuffer4K`] moves whole rows, the row is staged
/// once per layer — transparent left of the origin, then the layer colour —
/// then tiled. Per-frame allocation therefore stays at one row regardless of
/// how many layers are composited. Returns bytes written.
pub fn blit_layer(
    fb: &mut FrameBuffer4K,
    layer: &Layer,
    time_ms: i64,
    origin_x: usize,
    origin_y: usize,
    width: JInt,
    height: JInt,
) -> usize {
    let opacity = layer_opacity_at(layer, time_ms);
    if opacity <= 0.0 || width <= 0 || height <= 0 {
        return 0;
    }

    let row_stride = fb.row_bytes();
    let pixels_per_row = row_stride / BYTES_PER_PIXEL;
    if origin_x >= pixels_per_row {
        return 0;
    }
    let span = (width as usize).min(pixels_per_row - origin_x);
    let rows = fb.height().min(height as usize).saturating_sub(origin_y);
    if span == 0 || rows == 0 {
        return 0;
    }

    let color = Rgba::new(255, 255, 255, (opacity * 255.0).round() as u8);
    let mut staged = vec![0u8; row_stride];
    for px in staged[origin_x * BYTES_PER_PIXEL..]
        .chunks_exact_mut(BYTES_PER_PIXEL)
        .take(span)
    {
        px[0] = color.r;
        px[1] = color.g;
        px[2] = color.b;
        px[3] = color.a;
    }

    fb.fill_rows(&staged, origin_y, rows) * ROW_BYTES
}

/// A layer's evaluated opacity at `time_ms`, clamped to `[0, 1]`.
pub fn layer_opacity_at(layer: &Layer, time_ms: i64) -> f32 {
    layer
        .transform
        .opacity
        .get_value_at(time_ms)
        .clamp(0.0, 1.0)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::easing::CubicBezier;
    use crate::model::Layer;

    #[test]
    fn test_surface_is_singleton_and_reused_across_calls() {
        // The surface is a process-global, so each observation has to be taken
        // inside one lock acquisition or a parallel test could clear it.
        let (first, second, head) = with_surface(|fb| {
            fb.clear(Rgba::BLACK);
            let first = fb.as_ptr();
            let second = {
                fb.clear(Rgba::new(1, 2, 3, 4));
                fb.as_ptr()
            };
            (first, second, fb.as_bytes()[..4].to_vec())
        })
        .unwrap();
        assert_eq!(first, second, "clear() must not reallocate the surface");
        assert_eq!(head, vec![1, 2, 3, 4]);
        assert_eq!(with_surface(|fb| fb.len()).unwrap(), 33_177_600);
    }

    #[test]
    fn test_render_layer_raster_follows_keyframes() {
        let mut layer = Layer::new_image("l1", "Hero", "assets/hero.png", 2000);
        layer
            .transform
            .opacity
            .add_keyframe(0, 0.0, CubicBezier::LINEAR);
        layer
            .transform
            .opacity
            .add_keyframe(1000, 1.0, CubicBezier::LINEAR);

        assert!((layer_opacity_at(&layer, 0) - 0.0).abs() < 1e-6);
        assert!((layer_opacity_at(&layer, 500) - 0.5).abs() < 1e-2);
        assert!((layer_opacity_at(&layer, 2000) - 1.0).abs() < 1e-6);

        assert!(!layer_is_live(&layer, -1));
        assert!(layer_is_live(&layer, 1500));
        assert!(!layer_is_live(&layer, 2001));
    }

    #[test]
    fn test_raster_origin_and_layer_blit() {
        let mut layer = Layer::new_image("l1", "Hero", "assets/hero.png", 2000);
        assert_eq!(layer_raster_origin(&layer, 3840, 2160), (1920, 1080));

        layer
            .transform
            .pos_x
            .add_keyframe(0, -5000.0, CubicBezier::LINEAR);
        assert_eq!(layer_raster_origin(&layer, 3840, 2160).0, 0);

        // A full-canvas layer at the origin covers every row.
        let written = with_surface(|fb| {
            fb.clear(Rgba::TRANSPARENT);
            blit_layer(fb, &layer, 0, 0, 0, 3840, 2160)
        })
        .unwrap();
        assert_eq!(written, 33_177_600);

        // A half-height layer covers half the surface. Sample points are read
        // inside the same lock acquisition, since the surface is global.
        let (half, head, boundary) = with_surface(|fb| {
            fb.clear(Rgba::TRANSPARENT);
            let bytes = blit_layer(fb, &layer, 0, 0, 0, 3840, 1080);
            let head = Rgba::read(fb.as_bytes(), 0);
            let boundary = Rgba::read(fb.as_bytes(), 1080 * ROW_BYTES);
            (bytes, head, boundary)
        })
        .unwrap();
        assert_eq!(half, 3840 * 1080 * 4);
        assert_eq!(head, Some(Rgba::new(255, 255, 255, 255)));
        assert_eq!(boundary, Some(Rgba::TRANSPARENT));

        // A layer pushed past the right edge contributes nothing.
        assert_eq!(
            with_surface(|fb| blit_layer(fb, &layer, 0, 3840, 0, 3840, 2160)).unwrap(),
            0
        );

        // Fully transparent layers contribute nothing.
        let mut hidden = Layer::new_null("l0", "Rig", 2000);
        hidden
            .transform
            .opacity
            .add_keyframe(0, 0.0, CubicBezier::LINEAR);
        assert_eq!(
            with_surface(|fb| blit_layer(fb, &hidden, 0, 0, 0, 3840, 2160)).unwrap(),
            0
        );
    }
}
