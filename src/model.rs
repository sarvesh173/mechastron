//! Core Scene Graph and Layer Hierarchy for Mechastron Studio.

use crate::keyframe::FloatKeyframeTrack;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum LayerType {
    Image,
    Text,
    VectorShape,
    SolidColor,
    Audio,
    NullController,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct AppliedEffect {
    pub effect_id: String,
    pub effect_name: String,
    pub parameters: HashMap<String, FloatKeyframeTrack>,
}

impl AppliedEffect {
    pub fn new(effect_id: impl Into<String>, effect_name: impl Into<String>) -> Self {
        Self {
            effect_id: effect_id.into(),
            effect_name: effect_name.into(),
            parameters: HashMap::new(),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct Transform {
    pub pos_x: FloatKeyframeTrack,
    pub pos_y: FloatKeyframeTrack,
    pub scale_x: FloatKeyframeTrack,
    pub scale_y: FloatKeyframeTrack,
    pub rotation_deg: FloatKeyframeTrack,
    pub opacity: FloatKeyframeTrack,
}

impl Default for Transform {
    fn default() -> Self {
        Self {
            pos_x: FloatKeyframeTrack::new(0.0),
            pos_y: FloatKeyframeTrack::new(0.0),
            scale_x: FloatKeyframeTrack::new(1.0),
            scale_y: FloatKeyframeTrack::new(1.0),
            rotation_deg: FloatKeyframeTrack::new(0.0),
            opacity: FloatKeyframeTrack::new(1.0),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct Layer {
    pub id: String,
    pub name: String,
    pub layer_type: LayerType,
    pub start_time_ms: i64,
    pub end_time_ms: i64,
    pub parent_layer_id: Option<String>,
    pub asset_ref: Option<String>,
    pub transform: Transform,
    pub effects: Vec<AppliedEffect>,
}

impl Layer {
    pub fn new_image(
        id: impl Into<String>,
        name: impl Into<String>,
        asset_ref: impl Into<String>,
        duration_ms: i64,
    ) -> Self {
        Self {
            id: id.into(),
            name: name.into(),
            layer_type: LayerType::Image,
            start_time_ms: 0,
            end_time_ms: duration_ms,
            parent_layer_id: None,
            asset_ref: Some(asset_ref.into()),
            transform: Transform::default(),
            effects: Vec::new(),
        }
    }

    pub fn new_null(id: impl Into<String>, name: impl Into<String>, duration_ms: i64) -> Self {
        Self {
            id: id.into(),
            name: name.into(),
            layer_type: LayerType::NullController,
            start_time_ms: 0,
            end_time_ms: duration_ms,
            parent_layer_id: None,
            asset_ref: None,
            transform: Transform::default(),
            effects: Vec::new(),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct SceneProject {
    pub title: String,
    pub width: u32,
    pub height: u32,
    pub fps: u32,
    pub duration_ms: i64,
    pub layers: Vec<Layer>,
}

impl Default for SceneProject {
    fn default() -> Self {
        Self {
            title: "Untitled Mechastron Project".to_string(),
            width: 1080,
            height: 1920,
            fps: 60,
            duration_ms: 5000,
            layers: Vec::new(),
        }
    }
}

impl SceneProject {
    pub fn new(
        title: impl Into<String>,
        width: u32,
        height: u32,
        fps: u32,
        duration_ms: i64,
    ) -> Self {
        Self {
            title: title.into(),
            width,
            height,
            fps,
            duration_ms,
            layers: Vec::new(),
        }
    }

    pub fn add_layer(&mut self, layer: Layer) {
        self.layers.push(layer);
    }

    pub fn find_layer_mut(&mut self, layer_id: &str) -> Option<&mut Layer> {
        self.layers.iter_mut().find(|l| l.id == layer_id)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_project_and_layer_hierarchy() {
        let mut project = SceneProject::new("Cyber Intro", 1080, 1920, 60, 4000);
        let null_parent = Layer::new_null("ctrl_1", "Master Rig", 4000);
        let mut hero = Layer::new_image("hero_1", "Main Character", "assets/hero.png", 4000);
        hero.parent_layer_id = Some("ctrl_1".to_string());

        project.add_layer(null_parent);
        project.add_layer(hero);

        assert_eq!(project.layers.len(), 2);
        assert_eq!(project.layers[1].parent_layer_id.as_deref(), Some("ctrl_1"));
    }
}
