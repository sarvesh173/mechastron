//! XML Serializer and Deserializer for Mechastron Studio Project files.

use crate::easing::CubicBezier;
use crate::keyframe::FloatKeyframeTrack;
use crate::model::{AppliedEffect, Layer, LayerType, SceneProject, Transform};
use std::collections::HashMap;

pub struct MechastronXmlCodec;

impl MechastronXmlCodec {
    pub fn serialize(project: &SceneProject) -> String {
        let mut out = String::with_capacity(4096);
        out.push_str("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        out.push_str(&format!(
            "<mechastron_project title=\"{}\" width=\"{}\" height=\"{}\" fps=\"{}\" duration_ms=\"{}\" engine=\"Mechastron v1.0\">\n",
            escape_xml(&project.title),
            project.width,
            project.height,
            project.fps,
            project.duration_ms
        ));

        for layer in &project.layers {
            let type_str = match layer.layer_type {
                LayerType::Image => "image",
                LayerType::Text => "text",
                LayerType::VectorShape => "vector_shape",
                LayerType::SolidColor => "solid_color",
                LayerType::Audio => "audio",
                LayerType::NullController => "null_controller",
            };

            let parent_attr = match &layer.parent_layer_id {
                Some(p) => format!(" parent=\"{}\"", escape_xml(p)),
                None => String::new(),
            };

            let asset_attr = match &layer.asset_ref {
                Some(a) => format!(" asset_ref=\"{}\"", escape_xml(a)),
                None => String::new(),
            };

            out.push_str(&format!(
                "  <layer id=\"{}\" name=\"{}\" type=\"{}\" start_time=\"{}\" end_time=\"{}\"{}{}>\n",
                escape_xml(&layer.id),
                escape_xml(&layer.name),
                type_str,
                layer.start_time_ms,
                layer.end_time_ms,
                parent_attr,
                asset_attr
            ));

            // Serialize Transform
            out.push_str("    <transform>\n");
            serialize_track(&mut out, "pos_x", &layer.transform.pos_x);
            serialize_track(&mut out, "pos_y", &layer.transform.pos_y);
            serialize_track(&mut out, "scale_x", &layer.transform.scale_x);
            serialize_track(&mut out, "scale_y", &layer.transform.scale_y);
            serialize_track(&mut out, "rotation_deg", &layer.transform.rotation_deg);
            serialize_track(&mut out, "opacity", &layer.transform.opacity);
            out.push_str("    </transform>\n");

            // Serialize Effects
            if !layer.effects.is_empty() {
                out.push_str("    <effects>\n");
                for effect in &layer.effects {
                    out.push_str(&format!(
                        "      <effect id=\"{}\" name=\"{}\">\n",
                        escape_xml(&effect.effect_id),
                        escape_xml(&effect.effect_name)
                    ));
                    for (param_name, track) in &effect.parameters {
                        serialize_track(&mut out, param_name, track);
                    }
                    out.push_str("      </effect>\n");
                }
                out.push_str("    </effects>\n");
            }

            out.push_str("  </layer>\n");
        }

        out.push_str("</mechastron_project>\n");
        out
    }

    pub fn deserialize(xml_str: &str) -> Result<SceneProject, String> {
        // Robust regex/token based reader for project files
        let title =
            extract_attr(xml_str, "title").unwrap_or_else(|| "Untitled Project".to_string());
        let width = extract_attr(xml_str, "width")
            .and_then(|s| s.parse().ok())
            .unwrap_or(1080);
        let height = extract_attr(xml_str, "height")
            .and_then(|s| s.parse().ok())
            .unwrap_or(1920);
        let fps = extract_attr(xml_str, "fps")
            .and_then(|s| s.parse().ok())
            .unwrap_or(60);
        let duration_ms = extract_attr(xml_str, "duration_ms")
            .and_then(|s| s.parse().ok())
            .unwrap_or(5000);

        let mut project = SceneProject::new(title, width, height, fps, duration_ms);

        // Parse layers
        let layer_splits = xml_str.split("<layer ");
        for (i, layer_chunk) in layer_splits.enumerate() {
            if i == 0 {
                continue; // skip preamble
            }
            let layer_end_idx = layer_chunk.find("</layer>").unwrap_or(layer_chunk.len());
            let chunk = &layer_chunk[..layer_end_idx];

            let id = extract_attr(chunk, "id").unwrap_or_else(|| format!("layer_{i}"));
            let name = extract_attr(chunk, "name").unwrap_or_else(|| format!("Layer {i}"));
            let type_str = extract_attr(chunk, "type").unwrap_or_else(|| "image".to_string());
            let start_time_ms = extract_attr(chunk, "start_time")
                .and_then(|s| s.parse().ok())
                .unwrap_or(0);
            let end_time_ms = extract_attr(chunk, "end_time")
                .and_then(|s| s.parse().ok())
                .unwrap_or(duration_ms);
            let parent_layer_id = extract_attr(chunk, "parent");
            let asset_ref = extract_attr(chunk, "asset_ref");

            let layer_type = match type_str.as_str() {
                "text" => LayerType::Text,
                "vector_shape" => LayerType::VectorShape,
                "solid_color" => LayerType::SolidColor,
                "audio" => LayerType::Audio,
                "null_controller" => LayerType::NullController,
                _ => LayerType::Image,
            };

            let transform = parse_transform(chunk);
            let effects = parse_effects(chunk);

            project.add_layer(Layer {
                id,
                name,
                layer_type,
                start_time_ms,
                end_time_ms,
                parent_layer_id,
                asset_ref,
                transform,
                effects,
            });
        }

        Ok(project)
    }
}

fn serialize_track(out: &mut String, name: &str, track: &FloatKeyframeTrack) {
    if track.keyframes.is_empty() {
        out.push_str(&format!(
            "      <track name=\"{}\" default=\"{}\"/>\n",
            name, track.default_value
        ));
    } else {
        out.push_str(&format!(
            "      <track name=\"{}\" default=\"{}\">\n",
            name, track.default_value
        ));
        for k in &track.keyframes {
            out.push_str(&format!(
                "        <keyframe time=\"{}\" value=\"{}\" ease=\"{},{},{},{}\"/>\n",
                k.time_ms, k.value, k.easing.x1, k.easing.y1, k.easing.x2, k.easing.y2
            ));
        }
        out.push_str("      </track>\n");
    }
}

fn parse_transform(chunk: &str) -> Transform {
    let mut t = Transform::default();
    if let Some(pos_x) = parse_track(chunk, "pos_x") {
        t.pos_x = pos_x;
    }
    if let Some(pos_y) = parse_track(chunk, "pos_y") {
        t.pos_y = pos_y;
    }
    if let Some(scale_x) = parse_track(chunk, "scale_x") {
        t.scale_x = scale_x;
    }
    if let Some(scale_y) = parse_track(chunk, "scale_y") {
        t.scale_y = scale_y;
    }
    if let Some(rot) = parse_track(chunk, "rotation_deg") {
        t.rotation_deg = rot;
    }
    if let Some(opacity) = parse_track(chunk, "opacity") {
        t.opacity = opacity;
    }
    t
}

fn parse_effects(chunk: &str) -> Vec<AppliedEffect> {
    let mut effects = Vec::new();
    let effect_splits = chunk.split("<effect ");
    for (i, eff_chunk) in effect_splits.enumerate() {
        if i == 0 {
            continue;
        }
        let end_idx = eff_chunk.find("</effect>").unwrap_or(eff_chunk.len());
        let sub = &eff_chunk[..end_idx];
        let id = extract_attr(sub, "id").unwrap_or_default();
        let name = extract_attr(sub, "name").unwrap_or_default();
        let mut params = HashMap::new();

        // Extract tracks in effect
        let track_splits = sub.split("<track ");
        for (j, tr_chunk) in track_splits.enumerate() {
            if j == 0 {
                continue;
            }
            let tr_end = tr_chunk.find('>').unwrap_or(tr_chunk.len());
            let header = &tr_chunk[..tr_end];
            if let Some(tr_name) = extract_attr(header, "name") {
                if let Some(track) = parse_track(sub, &tr_name) {
                    params.insert(tr_name, track);
                }
            }
        }

        effects.push(AppliedEffect {
            effect_id: id,
            effect_name: name,
            parameters: params,
        });
    }
    effects
}

fn parse_track(chunk: &str, track_name: &str) -> Option<FloatKeyframeTrack> {
    let pattern = format!("<track name=\"{track_name}\"");
    let start_idx = chunk.find(&pattern)?;
    let sub = &chunk[start_idx..];
    let end_idx = sub
        .find("</track>")
        .unwrap_or_else(|| sub.find("/>").unwrap_or(sub.len()));
    let track_body = &sub[..end_idx];

    let default_val = extract_attr(track_body, "default")
        .and_then(|s| s.parse().ok())
        .unwrap_or(0.0);

    let mut track = FloatKeyframeTrack::new(default_val);

    let kf_splits = track_body.split("<keyframe ");
    for (i, kf_chunk) in kf_splits.enumerate() {
        if i == 0 {
            continue;
        }
        let time = extract_attr(kf_chunk, "time")
            .and_then(|s| s.parse().ok())
            .unwrap_or(0);
        let value = extract_attr(kf_chunk, "value")
            .and_then(|s| s.parse().ok())
            .unwrap_or(0.0);
        let ease_str = extract_attr(kf_chunk, "ease").unwrap_or_default();
        let ease_parts: Vec<f32> = ease_str
            .split(',')
            .filter_map(|s| s.trim().parse().ok())
            .collect();
        let ease = if ease_parts.len() == 4 {
            CubicBezier::new(ease_parts[0], ease_parts[1], ease_parts[2], ease_parts[3])
        } else {
            CubicBezier::FAST_OUT_SLOW_IN
        };

        track.add_keyframe(time, value, ease);
    }

    Some(track)
}

fn extract_attr(haystack: &str, attr: &str) -> Option<String> {
    let key = format!("{attr}=\"");
    let start = haystack.find(&key)? + key.len();
    let end = haystack[start..].find('"')? + start;
    Some(haystack[start..end].to_string())
}

fn escape_xml(s: &str) -> String {
    s.replace('&', "&amp;")
        .replace('<', "&lt;")
        .replace('>', "&gt;")
        .replace('"', "&quot;")
        .replace('\'', "&apos;")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_roundtrip_xml() {
        let mut project = SceneProject::new("Galactic Overlord", 1080, 1920, 60, 3000);
        let mut layer = Layer::new_image("char_1", "Main Mech", "assets/mech.png", 3000);
        layer
            .transform
            .pos_x
            .add_keyframe(0, 100.0, CubicBezier::LINEAR);
        layer
            .transform
            .pos_x
            .add_keyframe(1000, 500.0, CubicBezier::EASE_IN_OUT);
        project.add_layer(layer);

        let xml_str = MechastronXmlCodec::serialize(&project);
        assert!(xml_str.contains("mechastron_project"));
        assert!(xml_str.contains("Galactic Overlord"));

        let deserialized = MechastronXmlCodec::deserialize(&xml_str).unwrap();
        assert_eq!(deserialized.title, "Galactic Overlord");
        assert_eq!(deserialized.layers.len(), 1);
        assert_eq!(deserialized.layers[0].transform.pos_x.keyframes.len(), 2);
    }
}
