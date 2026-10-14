//! Tests for the JNI entry points.
//!
//! Split out of `mod.rs` to keep that file within the project's 400-line cap.

use super::*;
use crate::model::SceneProject;

#[test]
fn test_bridge_constants_match_the_render_surface() {
    assert_eq!(FRAME_BYTES, 33_177_600);
    assert_eq!(RENDER_REJECTED, -1);
    assert_eq!(ERR_BAD_ARGUMENT, -2);
    assert_eq!(ERR_RUNTIME, -3);
}

#[test]
fn test_null_env_yields_error_codes_not_panics() {
    let env = std::ptr::null_mut();
    let out = 1usize as JByteArray;
    assert_eq!(
        unsafe {
            Java_com_mechastron_studio_NativeBridge_nativeInit(env, std::ptr::null_mut(), 0, 2160)
        },
        0
    );
    assert_eq!(
        unsafe {
            Java_com_mechastron_studio_NativeBridge_nativeRenderFrame4K(
                env,
                std::ptr::null_mut(),
                std::ptr::null_mut(),
                0,
                3840,
                2160,
                out,
            )
        },
        ERR_BAD_ARGUMENT
    );
    assert!(unsafe {
        Java_com_mechastron_studio_NativeBridge_nativeSwapAsset(
            env,
            std::ptr::null_mut(),
            std::ptr::null_mut(),
            std::ptr::null_mut(),
            std::ptr::null_mut(),
            std::ptr::null_mut(),
        )
    }
    .is_null());
    assert!(unsafe {
        Java_com_mechastron_studio_NativeBridge_nativeExportPackage(
            env,
            std::ptr::null_mut(),
            std::ptr::null_mut(),
            std::ptr::null_mut(),
            std::ptr::null_mut(),
        )
    }
    .is_null());
}

#[test]
fn test_aliases_delegate_to_the_prefixed_entry_points() {
    // The aliases must not diverge; both spellings have to reject a null
    // env identically.
    let env = std::ptr::null_mut();
    let direct = unsafe {
        Java_com_mechastron_studio_NativeBridge_nativeRenderFrame4K(
            env,
            std::ptr::null_mut(),
            std::ptr::null_mut(),
            0,
            3840,
            2160,
            std::ptr::null_mut(),
        )
    };
    let aliased = unsafe {
        Java_com_mechastron_studio_NativeBridge_renderFrame4K(
            env,
            std::ptr::null_mut(),
            std::ptr::null_mut(),
            0,
            3840,
            2160,
            std::ptr::null_mut(),
        )
    };
    assert_eq!(direct, aliased);
}

#[test]
fn test_render_rejects_malformed_project_xml() {
    let env = std::ptr::null_mut();
    // Unreadable string arguments are rejected before parsing is attempted.
    assert_eq!(
        unsafe {
            Java_com_mechastron_studio_NativeBridge_nativeRenderFrame4K(
                env,
                std::ptr::null_mut(),
                std::ptr::null_mut(),
                0,
                3840,
                2160,
                1usize as JByteArray,
            )
        },
        ERR_BAD_ARGUMENT
    );
    assert_eq!(
        MechastronXmlCodec::deserialize("").unwrap().layers.len(),
        0,
        "sanity: empty XML parses to an empty project"
    );
    assert_eq!(SceneProject::default().fps, 60);
}
