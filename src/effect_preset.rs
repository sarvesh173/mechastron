//! Effect Preset Import System for Mechastron Studio.
//!
//! A *preset* is a self-contained, transportable description of a single effect:
//! its identity, its static properties, and its animated properties. Presets
//! travel as standalone `<effect>` snippets, so the same XML can be built into
//! the app, shipped as a file a user picks from storage, or embedded in a
//! project without any of those forms knowing about the others.
//!
//! ```text
//! <effect id="frost_bloom" name="Frost Bloom" version="1.0" type="bloom">
//!   <property name="intensity" value="0.85"/>
//!   <keyframes name="intensity">
//!     <keyframe time="0"   value="0.10" ease="0.4,0,0.2,1"/>
//!     <keyframe time="800" value="1.00" ease="0.4,0,0.2,1"/>
//!   </keyframes>
//! </effect>
//! ```
//!
//! `<track name=… default=…>` is accepted as a synonym for `<keyframes>` so a
//! preset lifted straight out of [`crate::codec`] project XML imports unchanged.
//!
//! Applying a preset is deliberately narrow. [`apply_preset_to_layer`] writes to
//! `layer.effects` and to nothing else, which is what makes the preservation
//! guarantee structural rather than something each caller has to remember:
//! media pointers, timing, parenting, and every transform track survive because
//! no code path exists that can reach them.

pub mod xml;

use crate::keyframe::{FloatKeyframeTrack, Keyframe};
use crate::model::{AppliedEffect, Layer, SceneProject};
use serde::{Deserialize, Serialize};
use std::collections::{BTreeMap, HashMap};

pub use xml::{parse_preset, serialize_preset};

/// Default preset version, used when a snippet omits `version`.
pub const PRESET_VERSION_DEFAULT: &str = "1.0";

/// The category a preset belongs to. Drives UI grouping, not evaluation.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum EffectType {
    Blur,
    Bloom,
    ColorGrade,
    Distort,
    Glow,
    Time,
    Other,
}

impl EffectType {
    /// The `type` attribute spelling used in preset XML.
    pub fn as_wire_name(self) -> &'static str {
        match self {
            EffectType::Blur => "blur",
            EffectType::Bloom => "bloom",
            EffectType::ColorGrade => "color_grade",
            EffectType::Distort => "distort",
            EffectType::Glow => "glow",
            EffectType::Time => "time",
            EffectType::Other => "other",
        }
    }

    /// Parses a `type` attribute, mapping anything unrecognised to
    /// [`EffectType::Other`] so an unfamiliar third-party preset still imports.
    ///
    /// Matching is case-insensitive and trims surrounding whitespace: preset
    /// files are hand-authored and share screens, and a preset that fails only
    /// because it says `Type="Blur"` is not worth rejecting.
    pub fn from_wire_name(value: &str) -> Self {
        match value.trim().to_ascii_lowercase().as_str() {
            "blur" => EffectType::Blur,
            "bloom" => EffectType::Bloom,
            "color_grade" | "color-grade" => EffectType::ColorGrade,
            "distort" => EffectType::Distort,
            "glow" => EffectType::Glow,
            "time" => EffectType::Time,
            _ => EffectType::Other,
        }
    }
}

/// A portable, importable description of one effect.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct EffectPreset {
    /// Stable identifier. Re-applying a preset with the same `id` replaces the
    /// previous instance rather than stacking a duplicate.
    pub id: String,
    /// Human-readable label shown in the picker.
    pub name: String,
    /// Preset schema version, for forward compatibility.
    pub version: String,
    /// Category used for UI grouping.
    pub effect_type: EffectType,
    /// Static parameter values.
    pub properties: BTreeMap<String, f32>,
    /// Animated parameter tracks, keyed by parameter name.
    ///
    /// A `BTreeMap` rather than a `HashMap` so serialization is deterministic;
    /// preset XML is frequently diffed and signed.
    pub keyframes: BTreeMap<String, Vec<Keyframe<f32>>>,
}

impl EffectPreset {
    /// A preset with no properties, ready for builder calls.
    pub fn new(
        id: impl Into<String>,
        name: impl Into<String>,
        version: impl Into<String>,
        effect_type: EffectType,
    ) -> Self {
        Self {
            id: id.into(),
            name: name.into(),
            version: version.into(),
            effect_type,
            properties: BTreeMap::new(),
            keyframes: BTreeMap::new(),
        }
    }

    /// Adds or overwrites a static property.
    pub fn with_property(mut self, name: impl Into<String>, value: f32) -> Self {
        self.properties.insert(name.into(), value);
        self
    }

    /// Adds or overwrites an animated property. Frames are sorted by time.
    pub fn with_keyframes(
        mut self,
        name: impl Into<String>,
        keyframes: impl Into<Vec<Keyframe<f32>>>,
    ) -> Self {
        let mut frames = keyframes.into();
        frames.sort_by_key(|k| k.time_ms);
        self.keyframes.insert(name.into(), frames);
        self
    }

    /// Total number of animated keyframes across every parameter.
    pub fn keyframe_count(&self) -> usize {
        self.keyframes.values().map(Vec::len).sum()
    }

    /// Materialises the runtime [`AppliedEffect`] this preset describes.
    ///
    /// Static properties become empty tracks and animated properties become
    /// populated tracks, so the result is uniformly addressable by name at
    /// evaluation time regardless of how the preset authored the value.
    pub fn to_applied_effect(&self) -> AppliedEffect {
        let mut parameters: HashMap<String, FloatKeyframeTrack> = HashMap::new();
        for (name, value) in &self.properties {
            parameters.insert(name.clone(), FloatKeyframeTrack::new(*value));
        }
        for (name, frames) in &self.keyframes {
            if frames.is_empty() {
                continue;
            }
            // Built through `add_keyframe` so repeated times collapse, matching
            // how project-authored tracks behave.
            let mut track = FloatKeyframeTrack::new(frames[0].value);
            for frame in frames {
                track.add_keyframe(frame.time_ms, frame.value, frame.easing);
            }
            parameters.insert(name.clone(), track);
        }

        AppliedEffect {
            effect_id: self.id.clone(),
            effect_name: self.name.clone(),
            parameters,
        }
    }

    /// Serialises back to standalone preset XML. `parse_preset(to_xml(p))`
    /// reproduces `p`.
    pub fn to_xml(&self) -> String {
        serialize_preset(self)
    }
}

/// Injects `preset` into `layer` and returns the resulting [`AppliedEffect`].
///
/// Replaces an existing effect carrying the same `effect_id` in place, keeping
/// stack position; otherwise appends. Every other field of `layer` — including
/// `asset_ref` and all six transform tracks — is left unchanged.
pub fn apply_preset_to_layer(layer: &mut Layer, preset: &EffectPreset) -> AppliedEffect {
    let applied = preset.to_applied_effect();
    match layer
        .effects
        .iter()
        .position(|e| e.effect_id == applied.effect_id)
    {
        Some(index) => layer.effects[index] = applied.clone(),
        None => layer.effects.push(applied.clone()),
    }
    applied
}

/// Parses `xml` and applies it to `layer`.
///
/// On a parse failure the layer is left untouched and the parser's message is
/// returned, so a bad file cannot half-apply.
pub fn apply_preset_xml_to_layer(layer: &mut Layer, xml: &str) -> Result<AppliedEffect, String> {
    let preset = parse_preset(xml)?;
    Ok(apply_preset_to_layer(layer, &preset))
}

/// Applies `preset` to the layer named `layer_id`, reporting an unknown layer
/// rather than silently doing nothing.
pub fn apply_preset_to_project(
    project: &mut SceneProject,
    layer_id: &str,
    preset: &EffectPreset,
) -> Result<AppliedEffect, String> {
    let layer = project
        .find_layer_mut(layer_id)
        .ok_or_else(|| format!("layer '{layer_id}' not found in project"))?;
    Ok(apply_preset_to_layer(layer, preset))
}

#[cfg(test)]
mod tests;
#[cfg(test)]
mod apply_tests;
