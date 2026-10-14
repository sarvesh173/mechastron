//! JNI bridge binding Mechastron Studio's Kotlin layer to the native engine.
//!
//! Four entry points back [`com.mechastron.studio.NativeBridge`]:
//! `nativeInit`, `nativeRenderFrame4K`, `nativeSwapAsset` and
//! `nativeExportPackage`.
//!
//! **Symbol-name drift.** `NativeBridge.kt` declares its externals as
//! `renderFrame4K`, `swapAsset` and `exportPackage` — no `native` prefix — and
//! declares no `nativeInit` at all. A JVM resolves externals by exact name, so
//! the `native*`-prefixed symbols alone would not bind and every call would
//! raise `UnsatisfiedLinkError`. The three unprefixed aliases at the bottom of
//! this file forward to the prefixed entry points so both spellings resolve.
//! If the Kotlin side is ever renamed to match, delete the aliases.

pub mod abi;
pub mod render;

#[cfg(test)]
mod tests;

use crate::asset_swap::AssetReplacementEngine;
use crate::codec::MechastronXmlCodec;
use crate::package::PackageArchiveManager;
use crate::render_4k::{Rgba, BUFFER_BYTES};
use abi::{arg_string, array_len, new_java_string, set_array_region, throw};
use abi::{JByteArray, JClass, JInt, JLong, JString};
use render::{blit_layer, layer_is_live, layer_raster_origin, with_surface};
use std::path::Path;

/// Size of the 4K RGBA surface handed to the platform, in bytes.
pub const FRAME_BYTES: JInt = BUFFER_BYTES as JInt;

/// True on platforms where the exported symbols are meaningful (Android).
pub const IS_JNI_AVAILABLE: bool = cfg!(target_os = "android");

/// Returned by the render entry point when a frame is refused. The Kotlin side
/// treats any negative value as "this frame was rejected".
pub const RENDER_REJECTED: JInt = -1;
const ERR_BAD_ARGUMENT: JInt = -2;
const ERR_RUNTIME: JInt = -3;

/// Resolves the destination array and copies the surface into it.
///
/// The copy is clamped to the array length so an undersized buffer cannot be
/// overrun by the 33 MB surface.
///
/// # Safety
///
/// `env` and `out_rgba` must be live JVM references; both are validated before
/// any pixel is written.
unsafe fn emit_frame(
    env: *mut abi::JniEnv,
    out_rgba: JByteArray,
    pixels: &[u8],
    required: usize,
) -> JInt {
    let Some(capacity) = array_len(env, out_rgba).filter(|c| *c >= 0) else {
        return ERR_RUNTIME;
    };
    if (capacity as usize) < required {
        throw(env, "renderFrame4K destination array is too small");
        return ERR_BAD_ARGUMENT;
    }
    let copy_len = pixels.len().min(capacity as usize);
    if !set_array_region(env, out_rgba, 0, &pixels[..copy_len]) {
        return ERR_RUNTIME;
    }
    copy_len as JInt
}

/// Pre-allocates the 4K surface and returns its size in bytes, or `0` if the
/// surface could not be reserved. Call once from the Kotlin object initialiser.
#[no_mangle]
///
/// # Safety
///
/// Called by the JVM with a live `JNIEnv*` and `jclass`. Every argument is
/// null-checked before use; a null reference is reported as an error code
/// (or a null return) rather than dereferenced.
pub unsafe extern "system" fn Java_com_mechastron_studio_NativeBridge_nativeInit(
    env: *mut abi::JniEnv,
    _class: JClass,
    width: JInt,
    height: JInt,
) -> JInt {
    if width <= 0 || height <= 0 {
        throw(env, "render surface dimensions must be positive");
        return 0;
    }
    match with_surface(|fb| {
        fb.clear(Rgba::TRANSPARENT);
        fb.len() as JInt
    }) {
        Some(bytes) if bytes > 0 => bytes,
        _ => {
            throw(env, "failed to reserve 4K render surface");
            0
        }
    }
}

/// Renders one composited frame at `timeMs` into the caller's byte array and
/// returns the number of bytes written.
///
/// Every visible layer is evaluated through its keyframe tracks, then tiled into
/// the destination a row at a time. `required` is checked against the caller's
/// array before any pixel is written.
#[no_mangle]
///
/// # Safety
///
/// Called by the JVM with a live `JNIEnv*` and `jclass`. Every argument is
/// null-checked before use; a null reference is reported as an error code
/// (or a null return) rather than dereferenced.
pub unsafe extern "system" fn Java_com_mechastron_studio_NativeBridge_nativeRenderFrame4K(
    env: *mut abi::JniEnv,
    _class: JClass,
    project_xml: JString,
    time_ms: JLong,
    width: JInt,
    height: JInt,
    out_rgba: JByteArray,
) -> JInt {
    let pixels_per_pixel = crate::render_4k::BYTES_PER_PIXEL;
    if width <= 0 || height <= 0 || out_rgba.is_null() {
        throw(env, "renderFrame4K received an invalid raster target");
        return ERR_BAD_ARGUMENT;
    }

    let Some(xml) = arg_string(env, project_xml) else {
        throw(env, "renderFrame4K could not read project XML");
        return ERR_BAD_ARGUMENT;
    };
    let Ok(project) = MechastronXmlCodec::deserialize(&xml) else {
        throw(env, "renderFrame4K received malformed project XML");
        return ERR_BAD_ARGUMENT;
    };

    let Some(rendered) = with_surface(|fb| {
        fb.clear(Rgba::BLACK);
        for layer in &project.layers {
            if !layer_is_live(layer, time_ms) {
                continue;
            }
            let (x, y) = layer_raster_origin(layer, width, height);
            blit_layer(fb, layer, time_ms, x, y, width, height);
        }
        fb.as_bytes().to_vec()
    }) else {
        return ERR_RUNTIME;
    };

    let required = width as usize * height as usize * pixels_per_pixel;
    emit_frame(env, out_rgba, &rendered, required)
}

/// Swaps a layer's media file while preserving every keyframe curve and effect,
/// returning the updated project XML, or a null string on failure.
#[no_mangle]
///
/// # Safety
///
/// Called by the JVM with a live `JNIEnv*` and `jclass`. Every argument is
/// null-checked before use; a null reference is reported as an error code
/// (or a null return) rather than dereferenced.
pub unsafe extern "system" fn Java_com_mechastron_studio_NativeBridge_nativeSwapAsset(
    env: *mut abi::JniEnv,
    _class: JClass,
    project_xml: JString,
    layer_id: JString,
    new_asset_path: JString,
    assets_dir: JString,
) -> JString {
    let (Some(xml), Some(layer), Some(asset), Some(dir)) = (
        arg_string(env, project_xml),
        arg_string(env, layer_id),
        arg_string(env, new_asset_path),
        arg_string(env, assets_dir),
    ) else {
        throw(env, "swapAsset could not read its arguments");
        return std::ptr::null_mut();
    };

    let Ok(mut project) = MechastronXmlCodec::deserialize(&xml) else {
        throw(env, "swapAsset received malformed project XML");
        return std::ptr::null_mut();
    };

    match AssetReplacementEngine::replace_layer_asset(
        &mut project,
        &layer,
        Path::new(&asset),
        Path::new(&dir),
    ) {
        Ok(_) => new_java_string(env, &MechastronXmlCodec::serialize(&project)),
        Err(e) => {
            throw(env, &e);
            std::ptr::null_mut()
        }
    }
}

/// Writes a `.mepack` archive and returns its output path as a Java string, or
/// a null string when packaging fails.
#[no_mangle]
///
/// # Safety
///
/// Called by the JVM with a live `JNIEnv*` and `jclass`. Every argument is
/// null-checked before use; a null reference is reported as an error code
/// (or a null return) rather than dereferenced.
pub unsafe extern "system" fn Java_com_mechastron_studio_NativeBridge_nativeExportPackage(
    env: *mut abi::JniEnv,
    _class: JClass,
    project_xml: JString,
    assets_dir: JString,
    output_path: JString,
) -> JString {
    let (Some(xml), Some(assets), Some(output)) = (
        arg_string(env, project_xml),
        arg_string(env, assets_dir),
        arg_string(env, output_path),
    ) else {
        throw(env, "exportPackage could not read its arguments");
        return std::ptr::null_mut();
    };

    let Ok(project) = MechastronXmlCodec::deserialize(&xml) else {
        throw(env, "exportPackage received malformed project XML");
        return std::ptr::null_mut();
    };

    match PackageArchiveManager::export_package(&project, Path::new(&assets), Path::new(&output)) {
        Ok(()) => new_java_string(env, &output),
        Err(e) => {
            throw(env, &e);
            std::ptr::null_mut()
        }
    }
}

// --- Binding aliases for the unprefixed names in NativeBridge.kt. See the
// module docs: these exist because the JVM matches externals by exact name. ---

/// Binding alias for `NativeBridge.renderFrame4K`.
#[no_mangle]
///
/// # Safety
///
/// Called by the JVM with a live `JNIEnv*` and `jclass`. Every argument is
/// null-checked before use; a null reference is reported as an error code
/// (or a null return) rather than dereferenced.
pub unsafe extern "system" fn Java_com_mechastron_studio_NativeBridge_renderFrame4K(
    env: *mut abi::JniEnv,
    class: JClass,
    project_xml: JString,
    time_ms: JLong,
    width: JInt,
    height: JInt,
    out_rgba: JByteArray,
) -> JInt {
    Java_com_mechastron_studio_NativeBridge_nativeRenderFrame4K(
        env,
        class,
        project_xml,
        time_ms,
        width,
        height,
        out_rgba,
    )
}

/// Binding alias for `NativeBridge.swapAsset`.
#[no_mangle]
///
/// # Safety
///
/// Called by the JVM with a live `JNIEnv*` and `jclass`. Every argument is
/// null-checked before use; a null reference is reported as an error code
/// (or a null return) rather than dereferenced.
pub unsafe extern "system" fn Java_com_mechastron_studio_NativeBridge_swapAsset(
    env: *mut abi::JniEnv,
    class: JClass,
    project_xml: JString,
    layer_id: JString,
    new_asset_path: JString,
    assets_dir: JString,
) -> JString {
    Java_com_mechastron_studio_NativeBridge_nativeSwapAsset(
        env,
        class,
        project_xml,
        layer_id,
        new_asset_path,
        assets_dir,
    )
}

/// Binding alias for `NativeBridge.exportPackage`.
#[no_mangle]
///
/// # Safety
///
/// Called by the JVM with a live `JNIEnv*` and `jclass`. Every argument is
/// null-checked before use; a null reference is reported as an error code
/// (or a null return) rather than dereferenced.
pub unsafe extern "system" fn Java_com_mechastron_studio_NativeBridge_exportPackage(
    env: *mut abi::JniEnv,
    class: JClass,
    project_xml: JString,
    assets_dir: JString,
    output_path: JString,
) -> JString {
    Java_com_mechastron_studio_NativeBridge_nativeExportPackage(
        env,
        class,
        project_xml,
        assets_dir,
        output_path,
    )
}
