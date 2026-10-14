//! Mechastron Motion Graphics & Animation Studio Engine.
//! A high-performance, clean-room 60 FPS motion graphics pipeline.

pub mod asset_swap;
pub mod codec;
pub mod easing;
pub mod html_export;
pub mod keyframe;
pub mod model;
pub mod package;

pub use asset_swap::AssetReplacementEngine;
pub use codec::MechastronXmlCodec;
pub use easing::CubicBezier;
pub use html_export::StandaloneHtmlExporter;
pub use keyframe::{FloatKeyframeTrack, Keyframe};
pub use model::{AppliedEffect, Layer, LayerType, SceneProject, Transform};
pub use package::PackageArchiveManager;
