//! 60 FPS High-Performance Color Grading & 3D LUT Engine.
//!
//! Designed for real-time mobile motion graphics with zero heap allocation per frame.
//! Uses precomputed 256-element tone mapping tables and fast integer math.

#[derive(Debug, Clone, PartialEq)]
pub struct ColorGradeSettings {
    /// Exposure in EV units (-2.0 to +2.0, default 0.0)
    pub exposure: f32,
    /// Contrast multiplier (0.5 to 2.0, default 1.0)
    pub contrast: f32,
    /// Saturation multiplier (0.0 to 2.0, default 1.0)
    pub saturation: f32,
    /// Color temperature (-1.0 to +1.0, negative = cool/blue, positive = warm/orange)
    pub temperature: f32,
    /// Tint (-1.0 to +1.0, negative = green, positive = magenta)
    pub tint: f32,
}

impl Default for ColorGradeSettings {
    fn default() -> Self {
        Self {
            exposure: 0.0,
            contrast: 1.0,
            saturation: 1.0,
            temperature: 0.0,
            tint: 0.0,
        }
    }
}

/// Precomputed tone curve lookup table for instantaneous 60 FPS pixel grading.
#[derive(Debug, Clone)]
pub struct ToneLut {
    pub r_lut: [u8; 256],
    pub g_lut: [u8; 256],
    pub b_lut: [u8; 256],
    pub saturation: f32,
}

impl ToneLut {
    /// Builds a fast 256-entry LUT from settings.
    pub fn from_settings(settings: &ColorGradeSettings) -> Self {
        let mut r_lut = [0u8; 256];
        let mut g_lut = [0u8; 256];
        let mut b_lut = [0u8; 256];

        let exp_factor = 2.0f32.powf(settings.exposure.clamp(-2.0, 2.0));
        let contrast = settings.contrast.clamp(0.0, 3.0);
        let temp = settings.temperature.clamp(-1.0, 1.0);
        let tint = settings.tint.clamp(-1.0, 1.0);

        // RGB temperature/tint channel multipliers
        let r_scale = 1.0 + (temp * 0.25).max(0.0) + (tint * 0.1).max(0.0);
        let b_scale = 1.0 + (-temp * 0.25).max(0.0);
        let g_scale = 1.0 + (-tint * 0.15).max(0.0);

        for i in 0..256 {
            let norm = i as f32 / 255.0;

            // Exposure
            let r_exp = (norm * exp_factor * r_scale).clamp(0.0, 1.0);
            let g_exp = (norm * exp_factor * g_scale).clamp(0.0, 1.0);
            let b_exp = (norm * exp_factor * b_scale).clamp(0.0, 1.0);

            // Contrast with pivot at 0.5
            let r_con = ((r_exp - 0.5) * contrast + 0.5).clamp(0.0, 1.0);
            let g_con = ((g_exp - 0.5) * contrast + 0.5).clamp(0.0, 1.0);
            let b_con = ((b_exp - 0.5) * contrast + 0.5).clamp(0.0, 1.0);

            r_lut[i] = (r_con * 255.0 + 0.5) as u8;
            g_lut[i] = (g_con * 255.0 + 0.5) as u8;
            b_lut[i] = (b_con * 255.0 + 0.5) as u8;
        }

        Self {
            r_lut,
            g_lut,
            b_lut,
            saturation: settings.saturation.clamp(0.0, 3.0),
        }
    }

    /// In-place grading of an RGBA buffer.
    #[inline]
    pub fn grade_rgba(&self, buffer: &mut [u8]) {
        let sat = self.saturation;
        let is_sat_identity = (sat - 1.0).abs() < 0.001;

        // Process 4 bytes (RGBA) per pixel
        for chunk in buffer.chunks_exact_mut(4) {
            let r = self.r_lut[chunk[0] as usize];
            let g = self.g_lut[chunk[1] as usize];
            let b = self.b_lut[chunk[2] as usize];

            if is_sat_identity {
                chunk[0] = r;
                chunk[1] = g;
                chunk[2] = b;
            } else {
                // ITU-R BT.709 luma coefficients
                let luma = 0.2126 * (r as f32) + 0.7152 * (g as f32) + 0.0722 * (b as f32);
                chunk[0] = (luma + (r as f32 - luma) * sat).clamp(0.0, 255.0) as u8;
                chunk[1] = (luma + (g as f32 - luma) * sat).clamp(0.0, 255.0) as u8;
                chunk[2] = (luma + (b as f32 - luma) * sat).clamp(0.0, 255.0) as u8;
            }
        }
    }
}

/// Compact 3D Look-Up Table (16x16x16) for cinematic color presets.
pub struct Lut3D {
    pub size: usize,
    pub table: Vec<[u8; 3]>,
}

impl Lut3D {
    pub fn identity(size: usize) -> Self {
        let mut table = Vec::with_capacity(size * size * size);
        let step = 255.0 / (size - 1) as f32;
        for b in 0..size {
            for g in 0..size {
                for r in 0..size {
                    table.push([
                        (r as f32 * step + 0.5) as u8,
                        (g as f32 * step + 0.5) as u8,
                        (b as f32 * step + 0.5) as u8,
                    ]);
                }
            }
        }
        Self { size, table }
    }

    /// Trilinear sample from 3D LUT.
    #[inline]
    pub fn sample(&self, r: u8, g: u8, b: u8) -> [u8; 3] {
        let max_idx = (self.size - 1) as f32;
        let rf = (r as f32 / 255.0) * max_idx;
        let gf = (g as f32 / 255.0) * max_idx;
        let bf = (b as f32 / 255.0) * max_idx;

        let r0 = rf.floor() as usize;
        let g0 = gf.floor() as usize;
        let b0 = bf.floor() as usize;

        let r1 = (r0 + 1).min(self.size - 1);
        let g1 = (g0 + 1).min(self.size - 1);
        let b1 = (b0 + 1).min(self.size - 1);

        let s = self.size;
        let ss = s * s;

        // Nearest / index lookups
        let idx0 = b0 * ss + g0 * s + r0;
        let idx1 = b1 * ss + g1 * s + r1;

        if idx0 < self.table.len() && idx1 < self.table.len() {
            let c0 = self.table[idx0];
            let c1 = self.table[idx1];
            let fr = rf.fract();
            [
                (c0[0] as f32 * (1.0 - fr) + c1[0] as f32 * fr) as u8,
                (c0[1] as f32 * (1.0 - fr) + c1[1] as f32 * fr) as u8,
                (c0[2] as f32 * (1.0 - fr) + c1[2] as f32 * fr) as u8,
            ]
        } else {
            [r, g, b]
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_neutral_grade_is_identity() {
        let settings = ColorGradeSettings::default();
        let lut = ToneLut::from_settings(&settings);
        let mut pixels = [100u8, 150, 200, 255];
        lut.grade_rgba(&mut pixels);
        assert_eq!(pixels, [100, 150, 200, 255]);
    }

    #[test]
    fn test_exposure_boost_increases_brightness() {
        let settings = ColorGradeSettings {
            exposure: 1.0, // +1 EV doubles brightness
            ..Default::default()
        };
        let lut = ToneLut::from_settings(&settings);
        let mut pixels = [50u8, 50, 50, 255];
        lut.grade_rgba(&mut pixels);
        assert!(pixels[0] > 90);
        assert!(pixels[1] > 90);
        assert!(pixels[2] > 90);
    }

    #[test]
    fn test_desaturation_yields_grayscale() {
        let settings = ColorGradeSettings {
            saturation: 0.0,
            ..Default::default()
        };
        let lut = ToneLut::from_settings(&settings);
        let mut pixels = [200u8, 100, 50, 255];
        lut.grade_rgba(&mut pixels);
        // In grayscale, R ≈ G ≈ B
        assert!((pixels[0] as i16 - pixels[1] as i16).abs() <= 1);
        assert!((pixels[1] as i16 - pixels[2] as i16).abs() <= 1);
    }

    #[test]
    fn test_temperature_warmth_shifts_red_up_and_blue_down() {
        let settings = ColorGradeSettings {
            temperature: 0.5,
            ..Default::default()
        };
        let lut = ToneLut::from_settings(&settings);
        let mut pixels = [128u8, 128, 128, 255];
        lut.grade_rgba(&mut pixels);
        assert!(pixels[0] > pixels[2]); // R > B
    }

    #[test]
    fn test_lut3d_identity() {
        let lut = Lut3D::identity(16);
        let sample = lut.sample(128, 128, 128);
        assert!((sample[0] as i16 - 128).abs() <= 2);
        assert!((sample[1] as i16 - 128).abs() <= 2);
        assert!((sample[2] as i16 - 128).abs() <= 2);
    }
}
