//! Keyframe animation tracks and temporal property evaluators for Mechastron.

use crate::easing::CubicBezier;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct Keyframe<T> {
    pub time_ms: i64,
    pub value: T,
    pub easing: CubicBezier,
}

impl<T> Keyframe<T> {
    pub fn new(time_ms: i64, value: T, easing: CubicBezier) -> Self {
        Self {
            time_ms,
            value,
            easing,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct FloatKeyframeTrack {
    pub default_value: f32,
    pub keyframes: Vec<Keyframe<f32>>,
}

impl FloatKeyframeTrack {
    pub fn new(default_value: f32) -> Self {
        Self {
            default_value,
            keyframes: Vec::new(),
        }
    }

    pub fn with_keyframes(default_value: f32, mut keyframes: Vec<Keyframe<f32>>) -> Self {
        keyframes.sort_by_key(|k| k.time_ms);
        Self {
            default_value,
            keyframes,
        }
    }

    pub fn add_keyframe(&mut self, time_ms: i64, value: f32, easing: CubicBezier) {
        self.keyframes.retain(|k| k.time_ms != time_ms);
        self.keyframes.push(Keyframe::new(time_ms, value, easing));
        self.keyframes.sort_by_key(|k| k.time_ms);
    }

    pub fn get_value_at(&self, time_ms: i64) -> f32 {
        if self.keyframes.is_empty() {
            return self.default_value;
        }

        if time_ms <= self.keyframes[0].time_ms {
            return self.keyframes[0].value;
        }

        if time_ms >= self.keyframes.last().unwrap().time_ms {
            return self.keyframes.last().unwrap().value;
        }

        // Linear scan for the enclosing interval
        for i in 0..self.keyframes.len() - 1 {
            let k_start = &self.keyframes[i];
            let k_end = &self.keyframes[i + 1];

            if time_ms >= k_start.time_ms && time_ms <= k_end.time_ms {
                let duration = (k_end.time_ms - k_start.time_ms) as f32;
                if duration <= 0.0 {
                    return k_start.value;
                }
                let raw_progress = (time_ms - k_start.time_ms) as f32 / duration;
                let eased_progress = k_start.easing.transform(raw_progress);
                return k_start.value + (k_end.value - k_start.value) * eased_progress;
            }
        }

        self.default_value
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_empty_track_returns_default() {
        let track = FloatKeyframeTrack::new(10.0);
        assert_eq!(track.get_value_at(0), 10.0);
        assert_eq!(track.get_value_at(500), 10.0);
    }

    #[test]
    fn test_single_keyframe() {
        let mut track = FloatKeyframeTrack::new(0.0);
        track.add_keyframe(1000, 50.0, CubicBezier::LINEAR);
        assert_eq!(track.get_value_at(500), 50.0);
        assert_eq!(track.get_value_at(2000), 50.0);
    }

    #[test]
    fn test_linear_interpolation() {
        let mut track = FloatKeyframeTrack::new(0.0);
        track.add_keyframe(0, 0.0, CubicBezier::LINEAR);
        track.add_keyframe(1000, 100.0, CubicBezier::LINEAR);

        assert!((track.get_value_at(500) - 50.0).abs() < 1e-2);
        assert!((track.get_value_at(250) - 25.0).abs() < 1e-2);
        assert!((track.get_value_at(750) - 75.0).abs() < 1e-2);
    }
}
