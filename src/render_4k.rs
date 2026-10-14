//! 4K 60 FPS zero-copy RGBA rendering surface for Mechastron Studio.
//!
//! The Android bridge hands this buffer straight to a native canvas, so pixels
//! must live in one contiguous allocation that never needs an intermediate copy
//! on the way to the display. Every operation here is therefore in-place and
//! bounded to the fixed 3840x2160 surface.

/// Horizontal resolution of the 4K render surface.
pub const WIDTH_4K: usize = 3840;
/// Vertical resolution of the 4K render surface.
pub const HEIGHT_4K: usize = 2160;
/// Bytes per RGBA pixel.
pub const BYTES_PER_PIXEL: usize = 4;
/// Width of a single row in bytes.
pub const ROW_BYTES: usize = WIDTH_4K * BYTES_PER_PIXEL;
/// Total surface size in bytes: 3840 * 2160 * 4.
pub const BUFFER_BYTES: usize = ROW_BYTES * HEIGHT_4K;

/// A non-premultiplied RGBA color, one byte per channel.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Rgba {
    pub r: u8,
    pub g: u8,
    pub b: u8,
    pub a: u8,
}

impl Rgba {
    pub const TRANSPARENT: Rgba = Rgba {
        r: 0,
        g: 0,
        b: 0,
        a: 0,
    };
    pub const BLACK: Rgba = Rgba {
        r: 0,
        g: 0,
        b: 0,
        a: 255,
    };

    pub const fn new(r: u8, g: u8, b: u8, a: u8) -> Self {
        Self { r, g, b, a }
    }

    /// Reads the pixel stored at byte offset `offset`, if in range.
    pub fn read(bytes: &[u8], offset: usize) -> Option<Rgba> {
        let px = bytes.get(offset..offset + BYTES_PER_PIXEL)?;
        Some(Rgba::new(px[0], px[1], px[2], px[3]))
    }
}

/// A contiguous 3840x2160 RGBA surface with in-place clear and row blitting.
pub struct FrameBuffer4K {
    pixels: Vec<u8>,
}

impl Default for FrameBuffer4K {
    fn default() -> Self {
        Self::new()
    }
}

impl FrameBuffer4K {
    /// Allocates the surface zeroed. Reuses nothing: this is the one place the
    /// 33 MB block is requested from the allocator.
    pub fn new() -> Self {
        Self {
            pixels: vec![0u8; BUFFER_BYTES],
        }
    }

    pub fn width(&self) -> usize {
        WIDTH_4K
    }

    pub fn height(&self) -> usize {
        HEIGHT_4K
    }

    pub fn len(&self) -> usize {
        self.pixels.len()
    }

    pub fn is_empty(&self) -> bool {
        self.pixels.is_empty()
    }

    pub fn as_bytes(&self) -> &[u8] {
        &self.pixels
    }

    /// Mutable view of the surface, for passes that accumulate into the pixels
    /// in place. Borrowed slices of this cover the whole surface, so a caller
    /// must not hold a sub-slice across another mutable borrow.
    pub fn as_mut_bytes(&mut self) -> &mut [u8] {
        &mut self.pixels
    }

    /// Raw pointer handed to the platform for direct submission.
    pub fn as_ptr(&self) -> *const u8 {
        self.pixels.as_ptr()
    }

    /// Raw mutable pointer for callers filling rows in place.
    pub fn as_mut_ptr(&mut self) -> *mut u8 {
        self.pixels.as_mut_ptr()
    }

    pub fn row_bytes(&self) -> usize {
        ROW_BYTES
    }

    /// Fills every pixel with `color`.
    ///
    /// `clear()` + `resize()` keeps the existing 33 MB allocation instead of
    /// freeing and reallocating, which is what makes this cheap enough to run
    /// once per frame at 60 FPS.
    pub fn clear(&mut self, color: Rgba) {
        self.pixels.clear();
        self.pixels.resize(BUFFER_BYTES, 0);
        for px in self.pixels.chunks_exact_mut(BYTES_PER_PIXEL) {
            px[0] = color.r;
            px[1] = color.g;
            px[2] = color.b;
            px[3] = color.a;
        }
    }

    /// Copies one row from `source` into `dst_row`, in place.
    pub fn blit_row(&mut self, source: &[u8], dst_row: usize) -> bool {
        if dst_row >= HEIGHT_4K {
            return false;
        }
        let start = dst_row * ROW_BYTES;
        let Some(chunk) = self.pixels.get_mut(start..start + ROW_BYTES) else {
            return false;
        };
        if source.len() < ROW_BYTES {
            return false;
        }
        chunk.copy_from_slice(&source[..ROW_BYTES]);
        true
    }

    /// Blits `rows` rows from `source` into consecutive destination rows.
    ///
    /// This is the zero-copy path: each row is a single `memcpy` straight into
    /// the destination buffer, with no staging allocation. The source and
    /// destination spans are the same length — a solid fill that repeats one
    /// prepared row uses [`FrameBuffer4K::fill_rows`] instead.
    ///
    /// Rows that would fall outside either surface are skipped rather than
    /// panicking, so a partially visible layer degrades to the rows that fit.
    pub fn blit_rows(
        &mut self,
        source: &[u8],
        src_row: usize,
        dst_row: usize,
        rows: usize,
    ) -> usize {
        let available_src = source.len() / ROW_BYTES;
        let src_rows = src_row.saturating_add(rows).min(available_src);
        let dst_rows = dst_row.saturating_add(rows).min(HEIGHT_4K);

        if src_row >= available_src || dst_row >= HEIGHT_4K {
            return 0;
        }

        let count = (src_rows - src_row).min(dst_rows - dst_row);
        for i in 0..count {
            let src_start = (src_row + i) * ROW_BYTES;
            let dst_start = (dst_row + i) * ROW_BYTES;
            self.pixels[dst_start..dst_start + ROW_BYTES]
                .copy_from_slice(&source[src_start..src_start + ROW_BYTES]);
        }
        count
    }

    /// Tiles a single prepared row across `rows` destination rows.
    ///
    /// A solid-colour or single-row layer only needs one source row in memory,
    /// so it is staged once and copied `rows` times. Stays in place: the
    /// template is only ever read.
    pub fn fill_rows(&mut self, row_template: &[u8], dst_row: usize, rows: usize) -> usize {
        if row_template.len() < ROW_BYTES || dst_row >= HEIGHT_4K {
            return 0;
        }
        let count = rows.min(HEIGHT_4K - dst_row);
        for i in 0..count {
            let dst_start = (dst_row + i) * ROW_BYTES;
            self.pixels[dst_start..dst_start + ROW_BYTES]
                .copy_from_slice(&row_template[..ROW_BYTES]);
        }
        count
    }

    /// Byte offset of the first pixel of a row, or `None` when out of range.
    pub fn row_offset(row: usize) -> Option<usize> {
        if row < HEIGHT_4K {
            Some(row * ROW_BYTES)
        } else {
            None
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_frame_buffer_has_exact_4k_rgba_size() {
        let fb = FrameBuffer4K::new();
        assert_eq!(WIDTH_4K, 3840);
        assert_eq!(HEIGHT_4K, 2160);
        assert_eq!(fb.width(), 3840);
        assert_eq!(fb.height(), 2160);
        assert_eq!(fb.row_bytes(), 15360);
        assert_eq!(fb.len(), 33177600);
        assert_eq!(BUFFER_BYTES, 33_177_600);
        assert!(!fb.is_empty());
        assert_eq!(
            Rgba::read(fb.as_bytes(), BUFFER_BYTES - 4),
            Some(Rgba::TRANSPARENT)
        );
    }

    #[test]
    fn test_fast_clear_writes_every_pixel() {
        let mut fb = FrameBuffer4K::new();
        fb.clear(Rgba::new(17, 34, 51, 255));

        let bytes = fb.as_bytes();
        assert_eq!(Rgba::read(bytes, 0), Some(Rgba::new(17, 34, 51, 255)));
        assert_eq!(
            Rgba::read(bytes, BUFFER_BYTES - 4),
            Some(Rgba::new(17, 34, 51, 255))
        );

        // Every 4th boundary pixel (last pixel of each row) is written.
        for row in [0usize, 1, 1234, HEIGHT_4K - 1] {
            let offset = FrameBuffer4K::row_offset(row).unwrap() + ROW_BYTES - 4;
            assert_eq!(Rgba::read(bytes, offset), Some(Rgba::new(17, 34, 51, 255)));
        }

        // Clear must not reallocate: same allocation, new contents.
        let before = fb.as_ptr();
        fb.clear(Rgba::BLACK);
        assert_eq!(fb.as_ptr(), before);
        assert_eq!(fb.len(), 33177600);
        assert_eq!(Rgba::read(fb.as_bytes(), 0), Some(Rgba::BLACK));
    }

    #[test]
    fn test_zero_copy_row_blitting() {
        let mut fb = FrameBuffer4K::new();

        let mut src = vec![0u8; ROW_BYTES * 3];
        for (i, byte) in src.iter_mut().enumerate() {
            *byte = (i % 251) as u8;
        }

        assert_eq!(fb.blit_rows(&src, 0, 100, 3), 3);

        {
            let bytes = fb.as_bytes();
            for row in 100..103 {
                let offset = FrameBuffer4K::row_offset(row).unwrap();
                let src_offset = (row - 100) * ROW_BYTES;
                assert_eq!(
                    &bytes[offset..offset + ROW_BYTES],
                    &src[src_offset..src_offset + ROW_BYTES]
                );
            }

            // Destination rows outside the blit stay untouched.
            let untouched = FrameBuffer4K::row_offset(99).unwrap();
            assert_eq!(Rgba::read(bytes, untouched), Some(Rgba::TRANSPARENT));
        }

        // fill_rows tiles one prepared row across a run of destinations.
        let template = vec![9u8; ROW_BYTES];
        assert_eq!(fb.fill_rows(&template, 500, 4), 4);
        assert_eq!(fb.fill_rows(&template, HEIGHT_4K - 2, 8), 2);
        assert_eq!(fb.fill_rows(&template[..10], 0, 1), 0);
        assert_eq!(fb.fill_rows(&template, HEIGHT_4K, 1), 0);

        let bytes = fb.as_bytes();
        for row in 500..504 {
            let offset = FrameBuffer4K::row_offset(row).unwrap();
            assert_eq!(
                &bytes[offset..offset + ROW_BYTES],
                &template[..],
                "row {row} was not tiled"
            );
        }
    }

    #[test]
    fn test_single_row_blit_writes_one_row_only() {
        let mut fb = FrameBuffer4K::new();
        let mut src = vec![0u8; ROW_BYTES];
        for (i, byte) in src.iter_mut().enumerate() {
            *byte = (i % 251) as u8;
        }
        let snapshot = src.clone();

        assert!(fb.blit_row(&src, 7));

        let bytes = fb.as_bytes();
        let offset = FrameBuffer4K::row_offset(7).unwrap();
        assert_eq!(&bytes[offset..offset + ROW_BYTES], &snapshot[..]);

        // Neighbouring rows are untouched.
        let above = FrameBuffer4K::row_offset(6).unwrap();
        let below = FrameBuffer4K::row_offset(8).unwrap();
        assert_eq!(Rgba::read(bytes, above), Some(Rgba::TRANSPARENT));
        assert_eq!(Rgba::read(bytes, below), Some(Rgba::TRANSPARENT));
    }

    #[test]
    fn test_row_blitting_rejects_out_of_range_requests() {
        let mut fb = FrameBuffer4K::new();
        let src = vec![255u8; ROW_BYTES];

        assert!(!fb.blit_row(&src, HEIGHT_4K));
        assert!(!fb.blit_row(&src[..10], 0));
        assert_eq!(fb.blit_rows(&src, 0, HEIGHT_4K, 1), 0);
        assert_eq!(fb.blit_rows(&src, 0, HEIGHT_4K - 1, 4), 1);
        assert_eq!(fb.blit_rows(&src[..ROW_BYTES / 2], 0, 0, 1), 0);
        assert_eq!(FrameBuffer4K::row_offset(HEIGHT_4K), None);
        assert_eq!(fb.len(), 33177600);
    }
}
