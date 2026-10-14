//! Double-Tap Asset Replacement Engine for Mechastron.
//! Replaces layer image/audio assets while guaranteeing 100% retention of transforms, keyframes, and effects.

use crate::model::SceneProject;
use std::fs;
use std::path::Path;
use std::time::{SystemTime, UNIX_EPOCH};

pub struct AssetReplacementEngine;

impl AssetReplacementEngine {
    /// Swaps the asset for a target layer upon double-tap.
    /// Preserves all keyframe tracks, bezier curves, timing, and applied effects.
    pub fn replace_layer_asset(
        project: &mut SceneProject,
        layer_id: &str,
        new_asset_source: &Path,
        project_assets_dir: &Path,
    ) -> Result<String, String> {
        if !new_asset_source.exists() {
            return Err(format!(
                "Source asset does not exist: {}",
                new_asset_source.display()
            ));
        }

        fs::create_dir_all(project_assets_dir).map_err(|e| e.to_string())?;

        let ext = new_asset_source
            .extension()
            .and_then(|e| e.to_str())
            .unwrap_or("png");

        let timestamp = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map(|d| d.as_millis())
            .unwrap_or(0);

        let new_file_name = format!("asset_{timestamp}.{ext}");
        let destination_path = project_assets_dir.join(&new_file_name);

        fs::copy(new_asset_source, &destination_path).map_err(|e| e.to_string())?;

        let relative_asset_ref = format!("assets/{new_file_name}");

        let layer = project
            .find_layer_mut(layer_id)
            .ok_or_else(|| format!("Layer '{layer_id}' not found in project"))?;

        // Update only the media pointer; all curves and effects remain completely intact
        layer.asset_ref = Some(relative_asset_ref.clone());

        Ok(relative_asset_ref)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::easing::CubicBezier;
    use crate::model::Layer;
    use tempfile::tempdir;

    #[test]
    fn test_asset_swap_preserves_keyframes_and_effects() {
        let temp_dir = tempdir().unwrap();
        let assets_dir = temp_dir.path().join("assets");
        fs::create_dir_all(&assets_dir).unwrap();

        let old_image = temp_dir.path().join("old.png");
        let new_image = temp_dir.path().join("new_hero.jpg");
        fs::write(&old_image, b"OLD_IMAGE").unwrap();
        fs::write(&new_image, b"NEW_HERO_IMAGE").unwrap();

        let mut project = SceneProject::new("Swap Test", 1080, 1920, 60, 5000);
        let mut layer = Layer::new_image("char_main", "Hero Character", "assets/old.png", 5000);

        // Add distinct transform curves
        layer
            .transform
            .pos_x
            .add_keyframe(0, 0.0, CubicBezier::FAST_OUT_SLOW_IN);
        layer
            .transform
            .pos_x
            .add_keyframe(1000, 450.0, CubicBezier::BOUNCE_OUT);
        layer
            .transform
            .opacity
            .add_keyframe(500, 0.8, CubicBezier::EASE_IN_OUT);

        project.add_layer(layer);

        // Perform asset swap
        let new_ref = AssetReplacementEngine::replace_layer_asset(
            &mut project,
            "char_main",
            &new_image,
            &assets_dir,
        )
        .unwrap();

        let target_layer = &project.layers[0];
        assert_eq!(target_layer.asset_ref.as_deref(), Some(new_ref.as_str()));
        assert!(new_ref.ends_with(".jpg"));

        // Verify keyframes and curves were NOT altered
        assert_eq!(target_layer.transform.pos_x.keyframes.len(), 2);
        assert_eq!(target_layer.transform.pos_x.keyframes[1].value, 450.0);
        assert_eq!(
            target_layer.transform.pos_x.keyframes[1].easing,
            CubicBezier::BOUNCE_OUT
        );
        assert_eq!(target_layer.transform.opacity.keyframes[0].value, 0.8);
    }
}
