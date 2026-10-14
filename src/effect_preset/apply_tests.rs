
use super::*;
use crate::easing::CubicBezier;
use crate::model::{Layer, SceneProject};

const FROST_BLOOM: &str = r#"<effect id="frost_bloom" name="Frost Bloom" version="2.1" type="bloom">
  <property name="intensity" value="0.85"/>
  <property name="threshold" value="0.4"/>
  <keyframes name="intensity">
    <keyframe time="0" value="0.1" ease="0.4,0,0.2,1"/>
    <keyframe time="800" value="1" ease="0.175,0.885,0.32,1.275"/>
    <keyframe time="400" value="0.55" ease="0,0,1,1"/>
  </keyframes>
</effect>"#;

fn fully_loaded_layer() -> Layer {
    let mut layer = Layer::new_image("hero_1", "Hero", "assets/hero.png", 4000);
    layer.start_time_ms = 250;
    layer.parent_layer_id = Some("ctrl_1".to_string());
    layer.transform.pos_x.add_keyframe(0, 0.0, CubicBezier::BOUNCE_OUT);
    layer.transform.pos_x.add_keyframe(1200, 720.0, CubicBezier::BOUNCE_OUT);
    layer.transform.pos_y.add_keyframe(0, 0.0, CubicBezier::LINEAR);
    layer.transform.pos_y.add_keyframe(1200, 380.0, CubicBezier::LINEAR);
    layer.transform.scale_x.add_keyframe(0, 0.6, CubicBezier::FAST_OUT_SLOW_IN);
    layer.transform.rotation_deg.add_keyframe(600, 12.0, CubicBezier::SHARP_SNAP);
    layer.transform.opacity.add_keyframe(0, 0.0, CubicBezier::EASE_IN_OUT);
    layer.transform.opacity.add_keyframe(400, 1.0, CubicBezier::EASE_IN_OUT);
    layer
}

fn original_transform(layer: &Layer) -> crate::model::Transform {
    layer.transform.clone()
}

#[test]
fn test_apply_preset_injects_effect_and_preserves_assets_and_position_keyframes() {
    let mut layer = fully_loaded_layer();
    let before = original_transform(&layer);

    let applied = apply_preset_xml_to_layer(&mut layer, FROST_BLOOM).unwrap();

    assert_eq!(applied.effect_id, "frost_bloom");
    assert_eq!(layer.effects.len(), 1);

    // Media assets survive.
    assert_eq!(layer.asset_ref.as_deref(), Some("assets/hero.png"));
    // Timing, identity and parenting survive.
    assert_eq!(layer.start_time_ms, 250);
    assert_eq!(layer.end_time_ms, 4000);
    assert_eq!(layer.parent_layer_id.as_deref(), Some("ctrl_1"));
    assert_eq!(layer.layer_type, crate::model::LayerType::Image);
    // Every transform track survives untouched.
    assert_eq!(layer.transform, before);
    assert_eq!(layer.transform.pos_x.keyframes.len(), 2);
    assert_eq!(layer.transform.pos_x.keyframes[1].value, 720.0);
    assert_eq!(
        layer.transform.pos_x.keyframes[1].easing,
        CubicBezier::BOUNCE_OUT
    );
    assert_eq!(layer.transform.opacity.keyframes.len(), 2);
}

#[test]
fn test_apply_preset_leaves_pre_existing_effects_intact_and_appends() {
    let mut layer = fully_loaded_layer();
    let existing = AppliedEffect::new("shadow_pass", "Shadow Pass");
    layer.effects.push(existing.clone());

    let applied = parse_preset(FROST_BLOOM).unwrap();
    apply_preset_to_layer(&mut layer, &applied);

    assert_eq!(layer.effects.len(), 2);
    assert_eq!(layer.effects[0], existing, "stack order is preserved");
    assert_eq!(layer.effects[1].effect_id, "frost_bloom");
}

#[test]
fn test_apply_preset_replaces_matching_effect_instead_of_duplicating() {
    let mut layer = fully_loaded_layer();
    layer.effects.push(AppliedEffect::new("other", "Other"));
    layer
        .effects
        .push(AppliedEffect::new("frost_bloom", "Stale Frost Bloom"));

    let mut replacement =
        EffectPreset::new("frost_bloom", "Frost Bloom v2", "2.1", EffectType::Bloom)
            .with_property("threshold", 0.9);
    apply_preset_to_layer(&mut layer, &replacement);

    assert_eq!(
        layer.effects.len(),
        2,
        "re-applying must not stack a duplicate"
    );
    assert_eq!(layer.effects[1].effect_name, "Frost Bloom v2");
    assert_eq!(layer.effects[1].parameters["threshold"].default_value, 0.9);

    // Applied twice in a row is idempotent.
    replacement.properties.insert("threshold".to_string(), 0.1);
    apply_preset_to_layer(&mut layer, &replacement);
    assert_eq!(layer.effects.len(), 2);
    assert_eq!(layer.effects[1].parameters["threshold"].default_value, 0.1);
}

#[test]
fn test_apply_preset_xml_leaves_layer_untouched_when_parsing_fails() {
    let mut layer = fully_loaded_layer();
    let before = original_transform(&layer);
    let error = apply_preset_xml_to_layer(&mut layer, "<effect name=\"NoId\"/>").unwrap_err();

    assert!(error.contains("'id'"), "got: {error}");
    assert!(
        layer.effects.is_empty(),
        "nothing may be applied on failure"
    );
    assert_eq!(layer.transform, before);
}

#[test]
fn test_apply_preset_to_project_targets_the_named_layer_only() {
    let mut project = SceneProject::new("Preset Test", 1080, 1920, 60, 4000);
    project.add_layer(fully_loaded_layer());
    project.add_layer(Layer::new_null("ctrl_1", "Master Rig", 4000));

    let preset = parse_preset(FROST_BLOOM).unwrap();
    let applied = apply_preset_to_project(&mut project, "hero_1", &preset).unwrap();
    assert_eq!(applied.effect_id, "frost_bloom");

    assert_eq!(project.layers[0].effects.len(), 1);
    assert!(
        project.layers[1].effects.is_empty(),
        "the rig must not gain the effect"
    );

    let missing = apply_preset_to_project(&mut project, "ghost", &preset).unwrap_err();
    assert_eq!(missing, "layer 'ghost' not found in project");
}

#[test]
fn test_apply_preset_survives_a_codec_project_round_trip() {
    // The guarantee has to hold after the XML round trip the app performs on
    // every native call, not just on the in-memory layer.
    let mut project = SceneProject::new("Round Trip", 1080, 1920, 60, 4000);
    project.add_layer(fully_loaded_layer());

    let preset = parse_preset(FROST_BLOOM).unwrap();
    apply_preset_to_project(&mut project, "hero_1", &preset).unwrap();

    let xml = crate::codec::MechastronXmlCodec::serialize(&project);
    let reparsed = crate::codec::MechastronXmlCodec::deserialize(&xml).unwrap();
    let layer = &reparsed.layers[0];

    assert_eq!(layer.effects.len(), 1);
    assert_eq!(layer.effects[0].effect_id, "frost_bloom");
    assert_eq!(layer.effects[0].parameters["intensity"].keyframes.len(), 3);
    assert_eq!(layer.asset_ref.as_deref(), Some("assets/hero.png"));
    assert_eq!(layer.transform.pos_x.keyframes.len(), 2);
    assert_eq!(layer.transform.pos_x.keyframes[1].value, 720.0);
}

/// The XML `ProjectCodec.serialize` (Kotlin) emits once it carries an effect.
///
/// This literal is the cross-language contract: the native codec has to accept
/// the `<effects>` block that the app writes back after every native call. If
/// either side changes its effect markup, this test is the thing that breaks
/// first — which is exactly the failure it exists to catch, because the effect
/// would otherwise be silently dropped between two native round-trips.
const KOTLIN_RESERIALIZED_LAYER: &str = r#"  <layer id="hero_1" name="Hero" type="image" start_time="0" end_time="4000" asset_ref="assets/hero.png">
    <transform>
      <track name="pos_x" default="0.0">
        <keyframe time="0" value="0.0" ease="0.175,0.885,0.32,1.275"/>
        <keyframe time="1200" value="720.0" ease="0.175,0.885,0.32,1.275"/>
      </track>
      <track name="pos_y" default="0.0"/>
      <track name="scale_x" default="1.0"/>
      <track name="scale_y" default="1.0"/>
      <track name="rotation_deg" default="0.0"/>
      <track name="opacity" default="1.0"/>
    </transform>
    <effects>
      <effect id="frost_bloom" name="Frost Bloom">
        <track name="intensity" default="0.1">
          <keyframe time="0" value="0.1" ease="0.4,0.0,0.2,1.0"/>
          <keyframe time="400" value="0.55" ease="0.0,0.0,1.0,1.0"/>
          <keyframe time="800" value="1.0" ease="0.175,0.885,0.32,1.275"/>
        </track>
        <track name="threshold" default="0.4"/>
      </effect>
    </effects>
  </layer>
"#;

#[test]
fn test_native_codec_reads_the_effects_block_the_app_writes_back() {
    let xml = format!(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\
         <mechastron_project title=\"Cross\" width=\"1080\" height=\"1920\" fps=\"60\" \
         duration_ms=\"4000\" engine=\"Mechastron v1.0\">\n{KOTLIN_RESERIALIZED_LAYER}\n\
         </mechastron_project>\n"
    );

    let project = crate::codec::MechastronXmlCodec::deserialize(&xml).unwrap();
    let layer = &project.layers[0];

    assert_eq!(
        layer.effects.len(),
        1,
        "the app's <effects> block must be read"
    );
    assert_eq!(layer.effects[0].effect_id, "frost_bloom");
    assert_eq!(layer.effects[0].effect_name, "Frost Bloom");
    assert_eq!(layer.effects[0].parameters.len(), 2);
    assert_eq!(layer.effects[0].parameters["intensity"].keyframes.len(), 3);
    assert_eq!(layer.effects[0].parameters["threshold"].default_value, 0.4);
    assert_eq!(
        layer.effects[0].parameters["intensity"].keyframes[2].easing,
        CubicBezier::BOUNCE_OUT
    );
    // Media and position keyframes survive the trip through the app unchanged.
    assert_eq!(layer.asset_ref.as_deref(), Some("assets/hero.png"));
    assert_eq!(layer.transform.pos_x.keyframes.len(), 2);
    assert_eq!(layer.transform.pos_x.keyframes[1].value, 720.0);
    assert_eq!(
        layer.transform.pos_x.keyframes[0].easing,
        CubicBezier::BOUNCE_OUT
    );
}
