//! Cubic Bezier Easing Evaluator for Mechastron Motion Engine.
//! Solves for progress along a parametric cubic curve with control handles (x1, y1, x2, y2).

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Serialize, Deserialize)]
pub struct CubicBezier {
    pub x1: f32,
    pub y1: f32,
    pub x2: f32,
    pub y2: f32,
}

impl CubicBezier {
    pub const LINEAR: Self = Self {
        x1: 0.0,
        y1: 0.0,
        x2: 1.0,
        y2: 1.0,
    };
    pub const EASE_IN_OUT: Self = Self {
        x1: 0.42,
        y1: 0.0,
        x2: 0.58,
        y2: 1.0,
    };
    pub const FAST_OUT_SLOW_IN: Self = Self {
        x1: 0.4,
        y1: 0.0,
        x2: 0.2,
        y2: 1.0,
    };
    pub const BOUNCE_OUT: Self = Self {
        x1: 0.175,
        y1: 0.885,
        x2: 0.32,
        y2: 1.275,
    };
    pub const SHARP_SNAP: Self = Self {
        x1: 0.1,
        y1: 0.9,
        x2: 0.2,
        y2: 1.0,
    };

    pub fn new(x1: f32, y1: f32, x2: f32, y2: f32) -> Self {
        Self {
            x1: x1.clamp(0.0, 1.0),
            y1,
            x2: x2.clamp(0.0, 1.0),
            y2,
        }
    }

    /// Solves for y given an input fraction x in [0.0, 1.0].
    pub fn transform(&self, x: f32) -> f32 {
        if x <= 0.0 {
            return 0.0;
        }
        if x >= 1.0 {
            return 1.0;
        }

        // Newton-Raphson iteration to find t such that sample_x(t) == x
        let mut t = x;
        for _ in 0..8 {
            let current_x = self.sample_x(t) - x;
            if current_x.abs() < 1e-5 {
                break;
            }
            let dx = self.sample_derivative_x(t);
            if dx.abs() < 1e-6 {
                break;
            }
            t -= current_x / dx;
        }

        // Clamp t to [0, 1] for safety
        let t_clamped = t.clamp(0.0, 1.0);
        self.sample_y(t_clamped)
    }

    #[inline]
    fn sample_x(&self, t: f32) -> f32 {
        let inv_t = 1.0 - t;
        3.0 * inv_t * inv_t * t * self.x1 + 3.0 * inv_t * t * t * self.x2 + t * t * t
    }

    #[inline]
    fn sample_y(&self, t: f32) -> f32 {
        let inv_t = 1.0 - t;
        3.0 * inv_t * inv_t * t * self.y1 + 3.0 * inv_t * t * t * self.y2 + t * t * t
    }

    #[inline]
    fn sample_derivative_x(&self, t: f32) -> f32 {
        let inv_t = 1.0 - t;
        3.0 * inv_t * inv_t * self.x1
            + 6.0 * inv_t * t * (self.x2 - self.x1)
            + 3.0 * t * t * (1.0 - self.x2)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_linear_easing() {
        let bezier = CubicBezier::LINEAR;
        assert!((bezier.transform(0.0) - 0.0).abs() < 1e-4);
        assert!((bezier.transform(0.5) - 0.5).abs() < 1e-3);
        assert!((bezier.transform(1.0) - 1.0).abs() < 1e-4);
    }

    #[test]
    fn test_ease_in_out() {
        let bezier = CubicBezier::EASE_IN_OUT;
        assert!((bezier.transform(0.0) - 0.0).abs() < 1e-4);
        assert!((bezier.transform(1.0) - 1.0).abs() < 1e-4);
        let mid = bezier.transform(0.5);
        assert!((mid - 0.5).abs() < 0.05);
    }

    #[test]
    fn test_boundary_clamping() {
        let bezier = CubicBezier::FAST_OUT_SLOW_IN;
        assert_eq!(bezier.transform(-0.5), 0.0);
        assert_eq!(bezier.transform(1.5), 1.0);
    }
}
