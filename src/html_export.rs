//! Single-file Standalone HTML5 Canvas Player Exporter for Mechastron.
//! Generates a self-contained, offline HTML file with embedded base64 assets and 60fps animation loop.

use crate::model::SceneProject;
use base64::engine::general_purpose::STANDARD as BASE64_STANDARD;
use base64::Engine;
use std::collections::HashMap;
use std::fs;
use std::path::Path;

pub struct StandaloneHtmlExporter;

impl StandaloneHtmlExporter {
    pub fn export_to_html(
        project: &SceneProject,
        assets_dir: &Path,
        output_html_file: &Path,
    ) -> Result<(), String> {
        let mut base64_assets: HashMap<String, String> = HashMap::new();

        for layer in &project.layers {
            if let Some(ref_path) = &layer.asset_ref {
                let clean_name = ref_path.trim_start_matches("assets/");
                let disk_path = assets_dir.join(clean_name);

                if disk_path.exists() {
                    let bytes = fs::read(&disk_path).map_err(|e| e.to_string())?;
                    let ext = disk_path
                        .extension()
                        .and_then(|e| e.to_str())
                        .unwrap_or("png")
                        .to_lowercase();
                    let mime = match ext.as_str() {
                        "jpg" | "jpeg" => "image/jpeg",
                        "webp" => "image/webp",
                        "mp3" => "audio/mpeg",
                        "wav" => "audio/wav",
                        _ => "image/png",
                    };
                    let encoded = BASE64_STANDARD.encode(&bytes);
                    base64_assets.insert(ref_path.clone(), format!("data:{mime};base64,{encoded}"));
                }
            }
        }

        let assets_json = serde_json::to_string(&base64_assets).map_err(|e| e.to_string())?;
        let project_json = serde_json::to_string(project).map_err(|e| e.to_string())?;

        let html = format!(
            r#"<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>{title} — Mechastron Engine</title>
<style>
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{ background: #07070b; color: #fff; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; overflow: hidden; }}
  #stage-container {{ position: relative; max-width: 96vw; max-height: 85vh; aspect-ratio: {width}/{height}; box-shadow: 0 16px 48px rgba(0,0,0,0.85); border-radius: 8px; overflow: hidden; background: #000; }}
  canvas {{ width: 100%; height: 100%; display: block; }}
  #hud {{ margin-top: 16px; display: flex; gap: 16px; align-items: center; background: rgba(255,255,255,0.06); padding: 8px 18px; border-radius: 30px; font-size: 13px; backdrop-filter: blur(10px); }}
  .badge {{ background: #22c55e; color: #000; font-weight: 700; padding: 2px 8px; border-radius: 12px; font-size: 11px; }}
  button {{ background: #3b82f6; border: none; color: #fff; padding: 4px 14px; border-radius: 16px; cursor: pointer; font-weight: 600; font-size: 12px; }}
</style>
</head>
<body>
<div id="stage-container">
  <canvas id="viewport" width="{width}" height="{height}"></canvas>
</div>
<div id="hud">
  <span class="badge">MECHASTRON 60 FPS</span>
  <span>{title} ({width}x{height})</span>
  <button id="toggle-btn" onclick="togglePlay()">Pause</button>
  <span id="time-display">0.0s / {dur_sec}s</span>
</div>
<script>
  const project = {project_json};
  const rawAssets = {assets_json};
  const canvas = document.getElementById('viewport');
  const ctx = canvas.getContext('2d');
  const timeDisplay = document.getElementById('time-display');
  const toggleBtn = document.getElementById('toggle-btn');

  const loadedImages = {{}};
  for (const [k, v] of Object.entries(rawAssets)) {{
    const img = new Image();
    img.src = v;
    loadedImages[k] = img;
  }}

  let isPlaying = true;
  let startTime = performance.now();
  const duration = project.duration_ms;

  function togglePlay() {{
    isPlaying = !isPlaying;
    toggleBtn.textContent = isPlaying ? 'Pause' : 'Play';
  }}

  function cubicBezier(p0, p1, p2, p3, t) {{
    const u = 1 - t;
    return 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t;
  }}

  function render(now) {{
    if (isPlaying) {{
      const elapsed = (now - startTime) % duration;
      timeDisplay.textContent = (elapsed / 1000).toFixed(1) + 's / ' + (duration / 1000).toFixed(1) + 's';

      ctx.clearRect(0, 0, canvas.width, canvas.height);

      for (const layer of project.layers) {{
        if (elapsed < layer.start_time_ms || elapsed > layer.end_time_ms) continue;

        ctx.save();
        // Compute transforms
        let posX = layer.transform.pos_x.default_value;
        let posY = layer.transform.pos_y.default_value;
        let scaleX = layer.transform.scale_x.default_value;
        let scaleY = layer.transform.scale_y.default_value;
        let rot = layer.transform.rotation_deg.default_value;
        let opacity = layer.transform.opacity.default_value;

        ctx.globalAlpha = Math.max(0, Math.min(1, opacity));
        ctx.translate(posX, posY);
        ctx.rotate((rot * Math.PI) / 180);
        ctx.scale(scaleX, scaleY);

        if (layer.asset_ref && loadedImages[layer.asset_ref]) {{
          const img = loadedImages[layer.asset_ref];
          if (img.complete && img.naturalWidth > 0) {{
            ctx.drawImage(img, -img.naturalWidth / 2, -img.naturalHeight / 2);
          }}
        }} else {{
          ctx.fillStyle = '#4f46e5';
          ctx.fillRect(-100, -100, 200, 200);
        }}
        ctx.restore();
      }}
    }}
    requestAnimationFrame(render);
  }}
  requestAnimationFrame(render);
</script>
</body>
</html>"#,
            title = project.title,
            width = project.width,
            height = project.height,
            dur_sec = project.duration_ms as f32 / 1000.0,
            project_json = project_json,
            assets_json = assets_json
        );

        if let Some(parent) = output_html_file.parent() {
            fs::create_dir_all(parent).map_err(|e| e.to_string())?;
        }
        fs::write(output_html_file, html).map_err(|e| e.to_string())?;

        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::model::Layer;
    use tempfile::tempdir;

    #[test]
    fn test_html_export_creates_file() {
        let temp_dir = tempdir().unwrap();
        let assets_dir = temp_dir.path().join("assets");
        fs::create_dir_all(&assets_dir).unwrap();

        let dummy_image = assets_dir.join("avatar.png");
        fs::write(&dummy_image, b"PNG_TEST_DATA").unwrap();

        let mut project = SceneProject::new("HTML Export Test", 1080, 1920, 60, 3000);
        let layer = Layer::new_image("l1", "Avatar", "assets/avatar.png", 3000);
        project.add_layer(layer);

        let out_html = temp_dir.path().join("preview.html");
        StandaloneHtmlExporter::export_to_html(&project, &assets_dir, &out_html).unwrap();

        assert!(out_html.exists());
        let content = fs::read_to_string(&out_html).unwrap();
        assert!(content.contains("HTML Export Test"));
        assert!(content.contains("MECHASTRON 60 FPS"));
        assert!(content.contains("data:image/png;base64,"));
    }
}
