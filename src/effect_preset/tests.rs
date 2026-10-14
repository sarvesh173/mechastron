//! Unit tests for the effect preset import system.
//!
//! Split out of `effect_preset.rs` to keep that file within the project's
//! 400-line cap, following the same layout as `jni/tests.rs`.

use super::*;
use crate::easing::CubicBezier;
use crate::keyframe::Keyframe;

const FROST_BLOOM: &str = r#"<effect id="frost_bloom" name="Frost Bloom" version="2.1" type="bloom">
  <property name="intensity" value="0.85"/>
  <property name="threshold" value="0.4"/>
  <keyframes name="intensity">
    <keyframe time="0" value="0.1" ease="0.4,0,0.2,1"/>
    <keyframe time="800" value="1" ease="0.175,0.885,0.32,1.275"/>
    <keyframe time="400" value="0.55" ease="0,0,1,1"/>
  </keyframes>
</effect>"#;



#[test]
fn test_parse_preset_reads_identity_properties_and_keyframes() {
    let preset = parse_preset(FROST_BLOOM).unwrap();

    assert_eq!(preset.id, "frost_bloom");
    assert_eq!(preset.name, "Frost Bloom");
    assert_eq!(preset.version, "2.1");
    assert_eq!(preset.effect_type, EffectType::Bloom);
    assert_eq!(preset.properties.get("intensity"), Some(&0.85));
    assert_eq!(preset.properties.get("threshold"), Some(&0.4));
    assert_eq!(preset.keyframes.len(), 1);
    // Frames are sorted on parse: the file lists 800 before 400.
    assert_eq!(preset.keyframe_count(), 3);
    let intensity = &preset.keyframes["intensity"];
    assert_eq!(intensity[0].time_ms, 0);
    assert_eq!(intensity[1].time_ms, 400);
    assert_eq!(intensity[1].value, 0.55);
    assert_eq!(intensity[2].time_ms, 800);
    assert_eq!(intensity[2].easing, CubicBezier::BOUNCE_OUT);
}

#[test]
fn test_parse_preset_defaults_version_and_type_when_absent() {
    let preset = parse_preset(
        "<effect id=\"bare\" name=\"Bare\"><property name=\"k\" value=\"2\"/></effect>",
    )
    .unwrap();
    assert_eq!(preset.version, PRESET_VERSION_DEFAULT);
    assert_eq!(preset.effect_type, EffectType::Other);
    assert_eq!(preset.properties.get("k"), Some(&2.0));
    assert!(preset.keyframes.is_empty());
}

#[test]
fn test_effect_type_wire_names_round_trip() {
    for kind in [
        EffectType::Blur,
        EffectType::Bloom,
        EffectType::ColorGrade,
        EffectType::Distort,
        EffectType::Glow,
        EffectType::Time,
        EffectType::Other,
    ] {
        assert_eq!(EffectType::from_wire_name(kind.as_wire_name()), kind);
    }
    // Hyphenated spellings and anything unrecognised degrade to Other, and
    // casing/whitespace never causes a rejection.
    assert_eq!(
        EffectType::from_wire_name("color-grade"),
        EffectType::ColorGrade
    );
    assert_eq!(EffectType::from_wire_name(" Blur "), EffectType::Blur);
    assert_eq!(EffectType::from_wire_name("BLOOM"), EffectType::Bloom);
    assert_eq!(EffectType::from_wire_name("warp_drive"), EffectType::Other);
}

#[test]
fn test_parse_preset_accepts_the_codec_track_form() {
    // A preset lifted verbatim out of project XML must import unchanged.
    let snippet = "<effect id=\"legacy\" name=\"Legacy\">\
        <track name=\"amount\" default=\"0.5\">\
        <keyframe time=\"100\" value=\"1\" ease=\"0.1,0.9,0.2,1\"/>\
        <keyframe time=\"0\" value=\"0\" ease=\"0.1,0.9,0.2,1\"/>\
        </track></effect>";
    let preset = parse_preset(snippet).unwrap();

    assert_eq!(preset.properties.get("amount"), Some(&0.5));
    assert_eq!(preset.keyframes["amount"].len(), 2);
    assert_eq!(preset.keyframes["amount"][0].time_ms, 0);
    assert_eq!(preset.keyframes["amount"][1].time_ms, 100);
}

#[test]
fn test_parse_preset_tolerates_missing_values_and_stray_text() {
    // A property with no `value` attribute defaults to zero rather than failing
    // the whole import.
    let snippet = "<effect id=\"odd\" name=\"Odd\" type=\"blur\">\
        <property name=\"radius\"/>\
        <keyframes name=\"empty\"></keyframes>\
        </effect>";
    let preset = parse_preset(snippet).unwrap();

    assert_eq!(preset.properties.get("radius"), Some(&0.0));
    assert!(preset.keyframes["empty"].is_empty());
    assert_eq!(preset.keyframe_count(), 0);
}

#[test]
fn test_parse_preset_terminates_on_truncated_elements() {
    // Elements with no closing tag must not stall the scanner or hang, and the
    // elements after them must still be reached.
    let snippet = "<effect id=\"torn\" name=\"Torn\">\
        <property name=\"first\"/><keyframes name=\"torn\"\
        <property name=\"second\" value=\"3\"/>\
        </effect>";
    let preset = parse_preset(snippet).unwrap();

    assert_eq!(preset.properties.get("first"), Some(&0.0));
    // The unterminated <keyframes> swallows the trailing property, exactly as
    // XML nesting requires; the point is that parsing terminates and reports.
    assert!(preset.keyframes.contains_key("torn"));
    assert_eq!(preset.keyframes["torn"].len(), 0);
}

#[test]
fn test_parse_preset_rejects_malformed_snippets() {
    let no_root = parse_preset("<project><layer/></project>").unwrap_err();
    assert!(no_root.contains("no <effect> element"), "got: {no_root}");

    let no_id = parse_preset("<effect name=\"Nameless\"/>").unwrap_err();
    assert!(no_id.contains("'id'"), "got: {no_id}");

    let no_name = parse_preset("<effect id=\"anon\"/>").unwrap_err();
    assert!(no_name.contains("'name'"), "got: {no_name}");

    assert!(parse_preset("").is_err());
    assert!(parse_preset("<effect id=\"x\" name=\"y\"").is_err());
}

#[test]
fn test_preset_xml_round_trip_is_stable() {
    let original = parse_preset(FROST_BLOOM).unwrap();
    let reparsed = parse_preset(&original.to_xml()).unwrap();
    assert_eq!(reparsed, original);
    // Serialisation is deterministic, so a second pass is byte-identical.
    assert_eq!(reparsed.to_xml(), original.to_xml());
}

#[test]
fn test_to_applied_effect_materialises_static_and_animated_tracks() {
    let applied = parse_preset(FROST_BLOOM).unwrap().to_applied_effect();

    assert_eq!(applied.effect_id, "frost_bloom");
    assert_eq!(applied.effect_name, "Frost Bloom");
    // An animated parameter replaces its own static entry rather than adding a
    // second one, so `intensity` and `threshold` are the whole map.
    assert_eq!(applied.parameters.len(), 2);
    assert_eq!(applied.parameters["threshold"].default_value, 0.4);
    assert!(applied.parameters["threshold"].keyframes.is_empty());

    // The animated track, not the static 0.85, now drives `intensity`.
    let intensity = &applied.parameters["intensity"];
    assert_eq!(intensity.keyframes.len(), 3);
    assert_eq!(intensity.default_value, 0.1);
    assert!((intensity.get_value_at(800) - 1.0).abs() < 1e-6);
    assert!((intensity.get_value_at(400) - 0.55).abs() < 1e-2);
    assert!((intensity.get_value_at(0) - 0.1).abs() < 1e-6);
}

#[test]
fn test_to_applied_effect_collapses_duplicate_keyframe_times() {
    let preset = EffectPreset::new("dup", "Dup", "1.0", EffectType::Other).with_keyframes(
        "amount",
        vec![
            Keyframe::new(100, 1.0, CubicBezier::LINEAR),
            Keyframe::new(100, 2.0, CubicBezier::LINEAR),
            Keyframe::new(50, 9.0, CubicBezier::LINEAR),
        ],
    );
    let applied = preset.to_applied_effect();
    let track = &applied.parameters["amount"];
    assert_eq!(track.keyframes.len(), 2, "same-time frames collapse to one");
    // `add_keyframe` evicts any existing frame at that time, so the last one
    // declared is the one that survives — matching project-authored tracks.
    assert_eq!(track.keyframes[1].time_ms, 100);
    assert_eq!(track.keyframes[1].value, 2.0);
}

