//! Mechastron Motion Graphics & Animation Studio Engine.
//! A high-performance, clean-room 60 FPS motion graphics pipeline.

pub mod asset_swap;
pub mod codec;
pub mod easing;
pub mod effect_preset;
pub mod html_export;
pub mod jni;
pub mod keyframe;
pub mod model;
pub mod package;
pub mod render_4k;

pub use asset_swap::AssetReplacementEngine;
pub use codec::MechastronXmlCodec;
pub use easing::CubicBezier;
pub use effect_preset::{
    apply_preset_to_layer, apply_preset_to_project, apply_preset_xml_to_layer, parse_preset,
    EffectPreset, EffectType,
};
pub use html_export::StandaloneHtmlExporter;
pub use jni::{FRAME_BYTES, IS_JNI_AVAILABLE};
pub use keyframe::{FloatKeyframeTrack, Keyframe};
pub use model::{AppliedEffect, Layer, LayerType, SceneProject, Transform};
pub use package::PackageArchiveManager;
pub use render_4k::{FrameBuffer4K, Rgba, BUFFER_BYTES, HEIGHT_4K, WIDTH_4K};
