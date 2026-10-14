//! Motion blur entry point: `NativeBridge.accumulateMotionBlur`.
//!
//! The export-path sibling of `nativeRenderFrame4K`. It composites the same
//! frame through [`compose_frame`], retains it in a process-global
//! [`TemporalBlend`], and resolves one blurred frame out of the retained
//! history. Splitting it out of `mod.rs` is what lets the module split hold —
//! and, like [`crate::jni::render`] owns the shared surface, this module owns
//! the accumulator behind its own mutex.
//!
//! **Why a separate entry point.** The live studio preview must stay crisp, so
//! the temporal path is never folded into `nativeRenderFrame4K`. The caller
//! drives it instead: once per frame it feeds the current timestamp, and for a
//! single blurred still it walks sub-frame timestamps through the shutter
//! window.
//!
//! **Lock order.** The surface lock is always taken before the accumulator
//! lock, and only this call takes both, so the pair cannot deadlock.

use crate::codec::MechastronXmlCodec;
use crate::jni::abi::{arg_string, throw, JByteArray, JClass, JFloat, JInt, JLong, JString, JniEnv};
use crate::jni::render::{compose_frame, with_surface};
use crate::jni::{emit_frame, ERR_BAD_ARGUMENT, ERR_RUNTIME};
use crate::render_4k::BYTES_PER_PIXEL;
use crate::temporal_blend::{
    TemporalBlend, DEFAULT_MOTION_BLUR_SAMPLES, MAX_MOTION_BLUR_SAMPLES,
};
use std::sync::Mutex;

/// Process-global history, allocated on first use. Mirrors the shared render
/// surface: the JVM owns a single instance of the bridge, so the accumulator
/// has to survive between frames rather than be rebuilt per call.
static BLENDER: Mutex<Option<TemporalBlend>> = Mutex::new(None);

/// Runs `f` against the shared accumulator while holding its lock.
pub fn with_blender<R>(f: impl FnOnce(&mut TemporalBlend) -> R) -> Option<R> {
    let mut guard = BLENDER.lock().ok()?;
    let blend = guard.get_or_insert_with(|| TemporalBlend::new(DEFAULT_MOTION_BLUR_SAMPLES));
    Some(f(blend))
}

/// Composites `time_ms`, retains the frame, and writes one shutter-blended frame
/// into `out_rgba`, returning the number of bytes written.
///
/// The exposure is set from `shutter_angle` (`0.0` crisp, `1.0` a 180-degree
/// shutter) and `samples` bounds the retained history. Both are validated here
/// even though [`TemporalBlend`] clamps them, so a caller bug surfaces as a
/// thrown argument rather than as a silently unblurred export.
///
/// # Safety
///
/// Called by the JVM with a live `JNIEnv*` and `jclass`. Every argument is
/// null-checked before use; a null reference is reported as an error code
/// (or a null return) rather than dereferenced.
#[no_mangle]
pub unsafe extern "system" fn Java_com_mechastron_studio_NativeBridge_nativeAccumulateMotionBlur(
    env: *mut JniEnv,
    _class: JClass,
    project_xml: JString,
    time_ms: JLong,
    width: JInt,
    height: JInt,
    shutter_angle: JFloat,
    samples: JInt,
    out_rgba: JByteArray,
) -> JInt {
    if width <= 0 || height <= 0 || out_rgba.is_null() {
        throw(env, "accumulateMotionBlur received an invalid raster target");
        return ERR_BAD_ARGUMENT;
    }
    if !shutter_angle.is_finite() || !(0.0..=1.0).contains(&shutter_angle) {
        throw(env, "accumulateMotionBlur shutter angle must be within 0.0..=1.0");
        return ERR_BAD_ARGUMENT;
    }
    if samples < 1 || samples as usize > MAX_MOTION_BLUR_SAMPLES {
        throw(
            env,
            &format!("accumulateMotionBlur sample count must be within 1..={MAX_MOTION_BLUR_SAMPLES}"),
        );
        return ERR_BAD_ARGUMENT;
    }

    let Some(xml) = arg_string(env, project_xml) else {
        throw(env, "accumulateMotionBlur could not read project XML");
        return ERR_BAD_ARGUMENT;
    };
    let Ok(project) = MechastronXmlCodec::deserialize(&xml) else {
        throw(env, "accumulateMotionBlur received malformed project XML");
        return ERR_BAD_ARGUMENT;
    };

    let required = width as usize * height as usize * BYTES_PER_PIXEL;
    let Some(written) = with_surface(|fb| {
        compose_frame(fb, &project, time_ms, width, height);
        with_blender(|blend| {
            blend.set_shutter_angle(shutter_angle);
            blend.set_samples(samples as usize);
            // The freshly composed frame is copied into the ring before the
            // blend, so the resolve reads only the history and never the
            // surface it is writing into.
            blend.push_frame(fb.as_bytes());
            blend.blend_into(&mut *fb);
            // Emitted straight from the surface: the blended frame is never
            // staged in a second 33 MB copy.
            emit_frame(env, out_rgba, fb.as_bytes(), required)
        })
        .unwrap_or(ERR_RUNTIME)
    }) else {
        return ERR_RUNTIME;
    };
    written
}

/// Binding alias for `NativeBridge.accumulateMotionBlur`.
///
/// The JVM resolves externals by exact name and `NativeBridge.kt` declares its
/// members without a `native` prefix, so both spellings must be exported. See
/// the module docs in `jni/mod.rs`.
///
/// # Safety
///
/// Called by the JVM with a live `JNIEnv*` and `jclass`. Every argument is
/// null-checked before use; a null reference is reported as an error code
/// (or a null return) rather than dereferenced.
#[no_mangle]
pub unsafe extern "system" fn Java_com_mechastron_studio_NativeBridge_accumulateMotionBlur(
    env: *mut JniEnv,
    class: JClass,
    project_xml: JString,
    time_ms: JLong,
    width: JInt,
    height: JInt,
    shutter_angle: JFloat,
    samples: JInt,
    out_rgba: JByteArray,
) -> JInt {
    Java_com_mechastron_studio_NativeBridge_nativeAccumulateMotionBlur(
        env,
        class,
        project_xml,
        time_ms,
        width,
        height,
        shutter_angle,
        samples,
        out_rgba,
    )
}

#[cfg(test)]
mod tests {
    use super::*;

    const SHUTTER: JFloat = 0.5;
    const SAMPLES: JInt = 2;

    fn call(env: *mut JniEnv, time_ms: JLong, shutter: JFloat, samples: JInt) -> JInt {
        let xml = std::ptr::null_mut();
        let out = 1usize as JByteArray;
        unsafe {
            Java_com_mechastron_studio_NativeBridge_nativeAccumulateMotionBlur(
                env,
                std::ptr::null_mut(),
                xml,
                time_ms,
                1920,
                1080,
                shutter,
                samples,
                out,
            )
        }
    }

    #[test]
    fn test_null_env_yields_an_error_code_not_a_panic() {
        let env = std::ptr::null_mut();
        // Unreadable string arguments are rejected before parsing is attempted.
        assert_eq!(call(env, 0, SHUTTER, SAMPLES), ERR_BAD_ARGUMENT);
        // The alias must not diverge from the prefixed entry point.
        assert_eq!(
            unsafe {
                Java_com_mechastron_studio_NativeBridge_accumulateMotionBlur(
                    env,
                    std::ptr::null_mut(),
                    std::ptr::null_mut(),
                    0,
                    1920,
                    1080,
                    SHUTTER,
                    SAMPLES,
                    1usize as JByteArray,
                )
            },
            ERR_BAD_ARGUMENT
        );
    }

    #[test]
    fn test_invalid_raster_and_exposure_arguments_are_refused() {
        let env = std::ptr::null_mut();
        let null = std::ptr::null_mut();
        let out = 1usize as JByteArray;
        // A null `env` cannot report a rejection through `throw`, so the
        // argument checks must run first and answer with a code.
        assert_eq!(
            unsafe {
                Java_com_mechastron_studio_NativeBridge_nativeAccumulateMotionBlur(
                    env,
                    null,
                    null,
                    0,
                    0,
                    1080,
                    SHUTTER,
                    SAMPLES,
                    out,
                )
            },
            ERR_BAD_ARGUMENT
        );
        for shutter in [-0.1f32, 1.1, f32::NAN, f32::INFINITY] {
            assert_eq!(
                unsafe {
                    Java_com_mechastron_studio_NativeBridge_nativeAccumulateMotionBlur(
                        env,
                        null,
                        null,
                        0,
                        1920,
                        1080,
                        shutter,
                        SAMPLES,
                        out,
                    )
                },
                ERR_BAD_ARGUMENT,
                "shutter {shutter} should be refused"
            );
        }
        for samples in [0, -1, MAX_MOTION_BLUR_SAMPLES as JInt + 1] {
            assert_eq!(
                unsafe {
                    Java_com_mechastron_studio_NativeBridge_nativeAccumulateMotionBlur(
                        env,
                        null,
                        null,
                        0,
                        1920,
                        1080,
                        SHUTTER,
                        samples,
                        out,
                    )
                },
                ERR_BAD_ARGUMENT,
                "sample count {samples} should be refused"
            );
        }
    }

    #[test]
    fn test_accumulator_is_process_global_and_reused() {
        let (first, second) = with_blender(|blend| {
            blend.reset();
            blend.set_samples(2);
            blend.set_shutter_angle(0.0);
            blend.push_frame(&[7u8; 8]);
            (blend.sample_count(), blend.shutter_angle())
        })
        .unwrap();
        assert_eq!(first, 1);
        assert_eq!(second, 0.0);

        let retained = with_blender(|blend| blend.sample_count()).unwrap();
        assert_eq!(retained, 1, "the history must survive between calls");

        // The shared render surface is untouched by this module's state.
        assert_eq!(with_surface(|fb| fb.len()).unwrap(), 33_177_600);
    }
}