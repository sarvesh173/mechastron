//! Temporal blend and motion blur accumulator for Mechastron Studio.
//!
//! A camera exposes its sensor over an interval rather than sampling it at an
//! instant, and that interval is what smears fast motion. The industry name for
//! the fraction of a frame a shutter stays open is the *shutter angle*:
//! `0.0` is an ideal pinhole (one instant, no smear) and `1.0` is a 180-degree
//! shutter spread across the whole frame interval.
//!
//! [`TemporalBlend`] keeps the last few composed frames in a circular history
//! and resolves one blended frame out of them, which is how the engine reaches
//! motion blur without a separate multi-sample render pass.
//!
//! **Cost.** Every retained frame is a full surface, so the history dominates
//! memory: 4K RGBA is 33 MB per sample, and [`DEFAULT_MOTION_BLUR_SAMPLES`]
//! retains four of them. [`TemporalBlend::memory_bytes`] reports the exact
//! figure and [`MAX_MOTION_BLUR_SAMPLES`] caps what a caller may ask for.
//!
//! **Blending.** Samples are combined with a weighted per-channel mean. Weights
//! are fixed-point and normalised to sum to exactly [`WEIGHT_SCALE`], so the
//! resolve ends in a shift rather than a division and stays exact at 8 bits per
//! channel. Sources are read in place and written straight into the caller's
//! destination; the only scratch is one row of accumulators, allocated once in
//! [`TemporalBlend::with_frame_bytes`] and reused by every call, so resolving a
//! frame performs no allocation.
//!
//! **Precondition.** Frames entering the accumulator must already be flattened —
//! opaque or premultiplied. Averaging non-premultiplied RGBA across differing
//! alpha values fringes transparent edges. The JNI render path clears to opaque
//! black before compositing, which satisfies this.

use crate::render_4k::{FrameBuffer4K, BYTES_PER_PIXEL, ROW_BYTES, WIDTH_4K};

/// Largest history depth a caller may configure.
///
/// The bound is a memory guard, not a quality one: each retained frame costs
/// [`BUFFER_BYTES`](crate::render_4k::BUFFER_BYTES), so the maximum is roughly
/// half a gigabyte at 4K.
pub const MAX_MOTION_BLUR_SAMPLES: usize = 16;

/// Depth used when the caller does not choose one.
pub const DEFAULT_MOTION_BLUR_SAMPLES: usize = 4;

/// Fixed-point denominator for the normalised sample weights.
///
/// A power of two keeps the final resolve a shift, and 10 bits leave each of the
/// [`MAX_MOTION_BLUR_SAMPLES`] samples about 6 bits of precision once the
/// weights are spread across them.
const WEIGHT_SCALE: u32 = 1024;
const WEIGHT_SHIFT: u32 = 10;
const WEIGHT_HALF: u32 = WEIGHT_SCALE >> 1;
const _: () = assert!(WEIGHT_SCALE == 1 << WEIGHT_SHIFT);

/// Outcome of one [`TemporalBlend::blend_into`] call.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct BlendReport {
    /// Retained frames that contributed to the result.
    pub frames: usize,
    /// Pixels written into the destination. Shorter than the destination when
    /// the history holds fewer rows than the surface has.
    pub pixels: usize,
}

impl BlendReport {
    /// Bytes written into the destination.
    pub fn bytes(&self) -> usize {
        self.pixels * BYTES_PER_PIXEL
    }
}

/// Circular history of composed frames with a shutter-angle exposure window.
pub struct TemporalBlend {
    /// Every retained frame back to back: `samples` slots of `frame_bytes`.
    storage: Vec<u8>,
    /// Per-slot contribution, parallel to the slots in `storage`.
    weights: Vec<f32>,
    /// Row count of one retained frame, in 4K-wide rows.
    rows: usize,
    /// Ring depth, which is also the widest the exposure window can open.
    samples: usize,
    /// Slot holding the oldest retained frame.
    head: usize,
    /// How many slots hold a frame, from 0 up to `samples`.
    filled: usize,
    shutter_angle: f32,
    /// One row of per-channel accumulators, reused by every resolve.
    acc: Vec<u32>,
}

impl Default for TemporalBlend {
    fn default() -> Self {
        Self::new(DEFAULT_MOTION_BLUR_SAMPLES)
    }
}

impl TemporalBlend {
    /// Allocates an accumulator whose frames are full 4K surfaces.
    ///
    /// This and [`TemporalBlend::with_frame_bytes`] are the two allocating
    /// constructors; everything after construction is allocation-free.
    pub fn new(samples: usize) -> Self {
        Self::with_frame_bytes(samples, crate::render_4k::BUFFER_BYTES)
    }

    /// Allocates an accumulator for frames of `frame_bytes` each.
    ///
    /// Frames are addressed in 4K-wide rows, so `frame_bytes` is rounded down
    /// to a whole number of them. A caller compositing into a narrower surface —
    /// a proxy render or a preview at reduced resolution — pays for the reduced
    /// history instead of the full 33 MB per sample; [`TemporalBlend::new`] is
    /// the only constructor that produces the 4K-sized engine accumulator.
    pub fn with_frame_bytes(samples: usize, frame_bytes: usize) -> Self {
        let samples = samples.clamp(1, MAX_MOTION_BLUR_SAMPLES);
        let rows = (frame_bytes / ROW_BYTES).max(1);
        let frame_bytes = rows * ROW_BYTES;
        Self {
            storage: vec![0u8; frame_bytes * samples],
            weights: vec![0.0; samples],
            rows,
            samples,
            head: 0,
            filled: 0,
            shutter_angle: 0.0,
            // Enough accumulators for one row, whatever the frame height.
            acc: vec![0u32; WIDTH_4K * BYTES_PER_PIXEL],
        }
    }

    /// Ring depth, which bounds the exposure window.
    pub fn samples(&self) -> usize {
        self.samples
    }

    /// Frames currently retained, from 0 up to [`TemporalBlend::samples`].
    pub fn sample_count(&self) -> usize {
        self.filled
    }

    /// The current shutter angle, always within `0.0..=1.0`.
    pub fn shutter_angle(&self) -> f32 {
        self.shutter_angle
    }

    /// Bytes held by the history ring, excluding the row accumulator.
    pub fn memory_bytes(&self) -> usize {
        self.storage.len()
    }

    /// Bytes held by the row accumulator reused across resolves.
    pub fn scratch_bytes(&self) -> usize {
        self.acc.len() * std::mem::size_of::<u32>()
    }

    /// Sets the exposure width as a fraction of a frame interval.
    ///
    /// Values outside `0.0..=1.0` are clamped to the nearest end and `NaN`
    /// collapses to `0.0`, so a miscomputed exposure degrades to a crisp frame
    /// instead of poisoning the weights.
    pub fn set_shutter_angle(&mut self, shutter_angle: f32) {
        self.shutter_angle = if shutter_angle.is_nan() {
            0.0
        } else {
            shutter_angle.clamp(0.0, 1.0)
        };
    }

    /// Resizes the ring, clamped to `1..=[`[`MAX_MOTION_BLUR_SAMPLES`]`.
    ///
    /// Reallocation is avoided when the depth is unchanged, so this is cheap to
    /// call every frame. When the depth does change the newest
    /// `min(retained, depth)` frames are carried over in age order, so raising
    /// or lowering the sample count mid-sequence does not throw away the blur
    /// already accumulated.
    pub fn set_samples(&mut self, samples: usize) {
        let wanted = samples.clamp(1, MAX_MOTION_BLUR_SAMPLES);
        if wanted == self.samples {
            return;
        }
        let keep = self.filled.min(wanted);
        let mut storage = vec![0u8; self.rows * ROW_BYTES * wanted];
        let mut weights = vec![0.0; wanted];
        let frame_bytes = self.rows * ROW_BYTES;
        for (age, slot) in weights.iter_mut().enumerate().take(keep) {
            // Carry over the newest `keep` frames, still in age order, so a
            // shrinking ring keeps the end of the exposure rather than its
            // beginning.
            let from_slot = (self.head + self.filled - keep + age) % self.samples;
            let from = from_slot * frame_bytes;
            let to = age * frame_bytes;
            storage[to..to + frame_bytes].copy_from_slice(&self.storage[from..from + frame_bytes]);
            *slot = self.weights[from_slot];
        }
        self.storage = storage;
        self.weights = weights;
        self.samples = wanted;
        self.head = 0;
        self.filled = keep;
    }

    /// Drops every retained frame without freeing the ring.
    pub fn reset(&mut self) {
        self.head = 0;
        self.filled = 0;
    }

    /// Retains `src` with an even share of the exposure window.
    ///
    /// Returns the bytes copied; a short source leaves the remainder of the slot
    /// zeroed.
    pub fn push_frame(&mut self, src: &[u8]) -> usize {
        self.push_frame_weighted(src, 1.0)
    }

    /// Retains `src` with a relative `weight` in the exposure window.
    ///
    /// Weights are relative, not absolute, so a tent filter is expressed as
    /// descending values and is normalised at resolve time. Negative and `NaN`
    /// weights are stored as zero and simply drop out of the exposure.
    pub fn push_frame_weighted(&mut self, src: &[u8], weight: f32) -> usize {
        let slot = if self.filled < self.samples {
            self.filled
        } else {
            self.head
        };
        let frame_bytes = self.rows * ROW_BYTES;
        let start = slot * frame_bytes;
        let copied = src.len().min(frame_bytes);
        self.storage[start..start + copied].copy_from_slice(&src[..copied]);
        self.storage[start + copied..start + frame_bytes].fill(0);
        self.weights[slot] = if weight.is_nan() || weight < 0.0 {
            0.0
        } else {
            weight
        };
        if self.filled < self.samples {
            self.filled += 1;
        } else {
            self.head = (self.head + 1) % self.samples;
        }
        copied
    }

    /// Retained frames the current shutter angle exposes.
    ///
    /// `0.0` resolves exactly one frame — the newest — and `1.0` opens the
    /// window across the whole sample depth.
    pub fn exposure_frames(&self) -> usize {
        let span = self.shutter_angle * (self.samples - 1) as f32;
        (span.floor() as usize + 1).clamp(1, self.samples)
    }

    /// Resolves the exposed frames into `dst`.
    ///
    /// The destination is written in place: sources are read straight out of the
    /// ring and the mean lands directly in `dst`, so no frame is copied twice
    /// and nothing is allocated. Rows past the end of the history are left
    /// untouched, which is how a reduced-resolution accumulator blends into a
    /// 4K surface.
    ///
    /// Taking `&mut self` is only to reuse the row accumulator; the retained
    /// history and the exposure window are unchanged by the call.
    pub fn blend_into(&mut self, dst: &mut FrameBuffer4K) -> BlendReport {
        let rows = (dst.len() / ROW_BYTES).min(self.rows);
        if rows == 0 || self.filled == 0 {
            return BlendReport { frames: 0, pixels: 0 };
        }

        let live = self.filled.min(self.exposure_frames());
        let oldest = self.filled - live;
        let (weights, total) = self.resolve_weights(live);
        let (live, oldest, weights) = if total == 0 {
            // Every exposed frame was pushed with a zero weight, so the window
            // has nothing to average. Fall back to the newest frame, which
            // leaves the destination defined without inventing a bias.
            (1, self.filled - 1, {
                let mut w = [0u32; MAX_MOTION_BLUR_SAMPLES];
                w[0] = WEIGHT_SCALE;
                w
            })
        } else {
            (live, oldest, weights)
        };

        // The stride is the retained frame height, which may exceed the rows the
        // destination actually offers.
        let frame_bytes = self.rows * ROW_BYTES;
        let Self {
            storage,
            head,
            acc,
            samples,
            ..
        } = self;
        let dst = dst.as_mut_bytes();
        let mut pixels = 0;

        for row in 0..rows {
            let accumulators = &mut acc[..WIDTH_4K * BYTES_PER_PIXEL];
            accumulators.fill(0);
            for (i, weight) in weights[..live].iter().enumerate() {
                let start = ((*head + oldest + i) % *samples) * frame_bytes + row * ROW_BYTES;
                let src = &storage[start..start + ROW_BYTES];
                for (acc, px) in accumulators.chunks_exact_mut(4).zip(src.chunks_exact(4)) {
                    acc[0] += weight * px[0] as u32;
                    acc[1] += weight * px[1] as u32;
                    acc[2] += weight * px[2] as u32;
                    acc[3] += weight * px[3] as u32;
                }
            }
            let start = row * ROW_BYTES;
            let out = &mut dst[start..start + ROW_BYTES];
            for (px, acc) in out.chunks_exact_mut(4).zip(accumulators.chunks_exact(4)) {
                px[0] = ((acc[0] + WEIGHT_HALF) >> WEIGHT_SHIFT) as u8;
                px[1] = ((acc[1] + WEIGHT_HALF) >> WEIGHT_SHIFT) as u8;
                px[2] = ((acc[2] + WEIGHT_HALF) >> WEIGHT_SHIFT) as u8;
                px[3] = ((acc[3] + WEIGHT_HALF) >> WEIGHT_SHIFT) as u8;
            }
            pixels += WIDTH_4K;
        }
        BlendReport { frames: live, pixels }
    }

    /// Normalises the weights of the `live` oldest frames to sum to
    /// [`WEIGHT_SCALE`], plus the total.
    ///
    /// The remainder from rounding is handed to the heaviest frame so the sum is
    /// exact, which is what lets the resolve end in a shift. A zero total means
    /// the caller pushed nothing but zero weights.
    fn resolve_weights(&self, live: usize) -> ([u32; MAX_MOTION_BLUR_SAMPLES], u32) {
        let mut out = [0u32; MAX_MOTION_BLUR_SAMPLES];
        let mut raw = 0.0f32;
        for age in 0..live {
            raw += self.weight_at(age);
        }
        if raw <= 0.0 || raw.is_nan() {
            return (out, 0);
        }
        let mut assigned = 0u32;
        for (slot, weight) in out.iter_mut().enumerate().take(live) {
            let share = (self.weight_at(slot) / raw) * WEIGHT_SCALE as f32;
            *weight = (share.round() as u32).min(WEIGHT_SCALE - assigned);
            assigned += *weight;
        }
        out[live - 1] += WEIGHT_SCALE - assigned;
        (out, WEIGHT_SCALE)
    }

    /// Weight of the frame `age` positions back from the oldest, or zero when
    /// that position is beyond the retained history.
    fn weight_at(&self, age: usize) -> f32 {
        if age >= self.filled {
            return 0.0;
        }
        let weight = self.weights[(self.head + age) % self.samples];
        if weight.is_nan() || weight < 0.0 {
            0.0
        } else {
            weight
        }
    }
}

