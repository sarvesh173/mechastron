//! XML transport for effect presets.
//!
//! Presets are exchanged as standalone `<effect>` snippets, so everything that
//! turns text into an [`EffectPreset`] — and back — lives here, keeping the
//! data model in the parent module free of parsing detail.
//!
//! The scanner is a tolerant tag walker rather than a validating parser, and
//! that is deliberate: this is an *import* path for files that may have been
//! hand-edited, so it degrades on malformed input (skipping an element) where
//! it can and reports an error only when the snippet is unusable as a whole.

use super::{EffectPreset, EffectType, PRESET_VERSION_DEFAULT};
use crate::codec::{escape_xml, extract_attr};
use crate::easing::CubicBezier;
use crate::keyframe::Keyframe;

/// Serialises `preset` back to a standalone snippet.
///
/// `parse_preset(&serialize_preset(p))` reproduces `p`. Property and keyframe
/// names come from `BTreeMap`s, so the output is byte-stable across runs.
pub fn serialize_preset(preset: &EffectPreset) -> String {
    let mut out = String::with_capacity(256);
    out.push_str(&format!(
        "<effect id=\"{}\" name=\"{}\" version=\"{}\" type=\"{}\">\n",
        escape_xml(&preset.id),
        escape_xml(&preset.name),
        escape_xml(&preset.version),
        preset.effect_type.as_wire_name()
    ));
    for (name, value) in &preset.properties {
        out.push_str(&format!(
            "  <property name=\"{}\" value=\"{}\"/>\n",
            escape_xml(name),
            value
        ));
    }
    for (name, frames) in &preset.keyframes {
        out.push_str(&format!("  <keyframes name=\"{}\">\n", escape_xml(name)));
        for frame in frames {
            let e = &frame.easing;
            out.push_str(&format!(
                "    <keyframe time=\"{}\" value=\"{}\" ease=\"{},{},{},{}\"/>\n",
                frame.time_ms, frame.value, e.x1, e.y1, e.x2, e.y2
            ));
        }
        out.push_str("  </keyframes>\n");
    }
    out.push_str("</effect>\n");
    out
}

/// The three child elements a preset body may contain.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum PresetElement {
    Property,
    Keyframes,
    Track,
}

impl PresetElement {
    fn tag(self) -> &'static str {
        match self {
            PresetElement::Property => "property",
            PresetElement::Keyframes => "keyframes",
            PresetElement::Track => "track",
        }
    }
}

/// Locates the next recognised child element at or after `from`.
fn next_preset_element(body: &str, from: usize) -> Option<(usize, PresetElement)> {
    let tail = &body[from..];
    let candidates = [
        tail.find("<property ")
            .map(|i| (from + i, PresetElement::Property)),
        tail.find("<keyframes ")
            .map(|i| (from + i, PresetElement::Keyframes)),
        tail.find("<track ")
            .map(|i| (from + i, PresetElement::Track)),
    ];
    candidates.into_iter().flatten().min_by_key(|(at, _)| *at)
}

/// Parses a standalone effect XML snippet into an [`EffectPreset`].
///
/// `id` and `name` are required; `version` and `type` fall back to
/// [`PRESET_VERSION_DEFAULT`] and [`EffectType::Other`].
pub fn parse_preset(xml: &str) -> Result<EffectPreset, String> {
    let root_at = xml
        .find("<effect ")
        .ok_or_else(|| "effect snippet contains no <effect> element".to_string())?;
    let header_at = root_at + "<effect ".len();
    let header_end = header_at
        + xml[header_at..]
            .find('>')
            .ok_or_else(|| "effect element header is never closed".to_string())?;
    let header = &xml[header_at..header_end];
    let body_start = (header_end + 1).min(xml.len());
    let body_end = xml[body_start..]
        .find("</effect>")
        .map_or(xml.len(), |i| body_start + i);
    let body = &xml[body_start..body_end];

    let id = extract_attr(header, "id")
        .ok_or_else(|| "effect element is missing the required 'id' attribute".to_string())?;
    let name = extract_attr(header, "name")
        .ok_or_else(|| "effect element is missing the required 'name' attribute".to_string())?;

    let type_attr = extract_attr(header, "type").or_else(|| extract_attr(header, "effect_type"));
    let mut preset = EffectPreset::new(
        id,
        name,
        extract_attr(header, "version").unwrap_or_else(|| PRESET_VERSION_DEFAULT.to_string()),
        type_attr
            .as_deref()
            .map_or(EffectType::Other, EffectType::from_wire_name),
    );

    let mut cursor = 0usize;
    while let Some((start, kind)) = next_preset_element(body, cursor) {
        let open_at = start + 1;
        let Some(offset) = body[open_at..].find('>') else {
            break;
        };
        let open_end = open_at + offset;
        let element_header = &body[open_at..open_end];
        let close_tag = format!("</{}>", kind.tag());

        let (inner, next) = if element_header.ends_with('/') {
            ("", open_end + 1)
        } else {
            match body[open_end..].find(&close_tag) {
                Some(i) => {
                    let end = open_end + i;
                    (&body[open_end..end], end + close_tag.len())
                }
                None => (&body[open_end..], body.len()),
            }
        };

        match kind {
            PresetElement::Property => {
                if let Some(key) = extract_attr(element_header, "name") {
                    let value = extract_attr(element_header, "value")
                        .and_then(|v| v.parse::<f32>().ok())
                        .unwrap_or(0.0);
                    preset.properties.insert(key, value);
                }
            }
            PresetElement::Keyframes => {
                if let Some(key) = extract_attr(element_header, "name") {
                    preset.keyframes.insert(key, parse_keyframes(inner));
                }
            }
            PresetElement::Track => {
                if let Some(key) = extract_attr(element_header, "name") {
                    let default = extract_attr(element_header, "default")
                        .and_then(|v| v.parse::<f32>().ok())
                        .unwrap_or(0.0);
                    preset.properties.insert(key.clone(), default);
                    let frames = parse_keyframes(inner);
                    if !frames.is_empty() {
                        preset.keyframes.insert(key, frames);
                    }
                }
            }
        }

        cursor = next.max(cursor + 1);
    }

    Ok(preset)
}

/// Extracts every `<keyframe/>` in `chunk`, sorted by time.
fn parse_keyframes(chunk: &str) -> Vec<Keyframe<f32>> {
    let mut frames: Vec<Keyframe<f32>> = chunk
        .split("<keyframe ")
        .skip(1)
        .map(|raw| {
            let end = raw.find('>').unwrap_or(raw.len());
            let header = &raw[..end];
            Keyframe::new(
                extract_attr(header, "time")
                    .and_then(|v| v.parse().ok())
                    .unwrap_or(0),
                extract_attr(header, "value")
                    .and_then(|v| v.parse().ok())
                    .unwrap_or(0.0),
                parse_easing(extract_attr(header, "ease").as_deref()),
            )
        })
        .collect();
    frames.sort_by_key(|k| k.time_ms);
    frames
}

/// Parses a `x1,y1,x2,y2` easing attribute, defaulting when malformed.
fn parse_easing(raw: Option<&str>) -> CubicBezier {
    let parts: Vec<f32> = raw
        .unwrap_or_default()
        .split(',')
        .filter_map(|s| s.trim().parse().ok())
        .collect();
    if parts.len() == 4 {
        CubicBezier::new(parts[0], parts[1], parts[2], parts[3])
    } else {
        CubicBezier::FAST_OUT_SLOW_IN
    }
}
