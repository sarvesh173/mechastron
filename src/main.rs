//! Mechastron CLI — Universal Motion Graphics & Animation Studio Tool.

use mechastron::asset_swap::AssetReplacementEngine;
use mechastron::codec::MechastronXmlCodec;
use mechastron::html_export::StandaloneHtmlExporter;
use mechastron::package::PackageArchiveManager;
use std::env;
use std::fs;
use std::path::Path;
use std::process;

fn print_usage() {
    eprintln!(
        r#"
Mechastron Motion Engine v1.0
Clean-room, high-performance 60 FPS motion graphics pipeline.

USAGE:
    mechastron <SUBCOMMAND> [OPTIONS]

SUBCOMMANDS:
    pack <project.xml> <assets_dir> <output.mepack>
        Bundles human-readable XML and media into an offline .mepack archive.

    unpack <project.mepack> <destination_dir>
        Extracts a .mepack bundle to directory for manual editing and playback.

    swap-asset <project.xml> <layer_id> <new_media_file> <assets_dir>
        Simulates double-tap asset replacement, preserving all curves and effects.

    export-html <project.xml> <assets_dir> <output.html>
        Generates zero-dependency standalone 60fps HTML5 canvas player.

    inspect <project.mepack>
        Summarizes layers, keyframe tracks, and asset manifest.
"#
    );
}

fn main() {
    let args: Vec<String> = env::args().collect();
    if args.len() < 2 {
        print_usage();
        process::exit(1);
    }

    match args[1].as_str() {
        "pack" => {
            if args.len() < 5 {
                eprintln!("Error: pack requires <project.xml> <assets_dir> <output.mepack>");
                process::exit(1);
            }
            let xml_path = Path::new(&args[2]);
            let assets_dir = Path::new(&args[3]);
            let output_mepack = Path::new(&args[4]);

            let xml_content = fs::read_to_string(xml_path).unwrap_or_else(|e| {
                eprintln!("Failed to read XML: {e}");
                process::exit(1);
            });

            let project = MechastronXmlCodec::deserialize(&xml_content).unwrap_or_else(|e| {
                eprintln!("Failed to parse project XML: {e}");
                process::exit(1);
            });

            PackageArchiveManager::export_package(&project, assets_dir, output_mepack)
                .unwrap_or_else(|e| {
                    eprintln!("Export failed: {e}");
                    process::exit(1);
                });

            println!(
                "✓ Successfully packaged .mepack: {}",
                output_mepack.display()
            );
        }

        "unpack" => {
            if args.len() < 4 {
                eprintln!("Error: unpack requires <project.mepack> <destination_dir>");
                process::exit(1);
            }
            let zip_path = Path::new(&args[2]);
            let dest_dir = Path::new(&args[3]);

            let project =
                PackageArchiveManager::import_package(zip_path, dest_dir).unwrap_or_else(|e| {
                    eprintln!("Unpack failed: {e}");
                    process::exit(1);
                });

            println!(
                "✓ Unpacked '{}' ({}x{} @ {}fps, {} layers) to: {}",
                project.title,
                project.width,
                project.height,
                project.fps,
                project.layers.len(),
                dest_dir.display()
            );
        }

        "swap-asset" => {
            if args.len() < 6 {
                eprintln!("Error: swap-asset requires <project.xml> <layer_id> <new_media_file> <assets_dir>");
                process::exit(1);
            }
            let xml_path = Path::new(&args[2]);
            let layer_id = &args[3];
            let new_media = Path::new(&args[4]);
            let assets_dir = Path::new(&args[5]);

            let xml_content = fs::read_to_string(xml_path).unwrap_or_else(|e| {
                eprintln!("Failed to read XML: {e}");
                process::exit(1);
            });

            let mut project = MechastronXmlCodec::deserialize(&xml_content).unwrap_or_else(|e| {
                eprintln!("Failed to parse XML: {e}");
                process::exit(1);
            });

            let new_ref = AssetReplacementEngine::replace_layer_asset(
                &mut project,
                layer_id,
                new_media,
                assets_dir,
            )
            .unwrap_or_else(|e| {
                eprintln!("Asset replacement failed: {e}");
                process::exit(1);
            });

            // Write back updated project XML
            let updated_xml = MechastronXmlCodec::serialize(&project);
            fs::write(xml_path, updated_xml).unwrap_or_else(|e| {
                eprintln!("Failed to write updated XML: {e}");
                process::exit(1);
            });

            println!(
                "✓ Double-tap asset swapped for layer '{}' -> {}",
                layer_id, new_ref
            );
        }

        "export-html" => {
            if args.len() < 5 {
                eprintln!("Error: export-html requires <project.xml> <assets_dir> <output.html>");
                process::exit(1);
            }
            let xml_path = Path::new(&args[2]);
            let assets_dir = Path::new(&args[3]);
            let out_html = Path::new(&args[4]);

            let xml_content = fs::read_to_string(xml_path).unwrap_or_else(|e| {
                eprintln!("Failed to read XML: {e}");
                process::exit(1);
            });

            let project = MechastronXmlCodec::deserialize(&xml_content).unwrap_or_else(|e| {
                eprintln!("Failed to parse XML: {e}");
                process::exit(1);
            });

            StandaloneHtmlExporter::export_to_html(&project, assets_dir, out_html).unwrap_or_else(
                |e| {
                    eprintln!("HTML export failed: {e}");
                    process::exit(1);
                },
            );

            println!("✓ Generated standalone HTML player: {}", out_html.display());
        }

        "inspect" => {
            if args.len() < 3 {
                eprintln!("Error: inspect requires <project.mepack>");
                process::exit(1);
            }
            let zip_path = Path::new(&args[2]);
            let temp_dir = tempfile::tempdir().unwrap_or_else(|e| {
                eprintln!("Failed to create tempdir: {e}");
                process::exit(1);
            });

            let project = PackageArchiveManager::import_package(zip_path, temp_dir.path())
                .unwrap_or_else(|e| {
                    eprintln!("Inspect failed: {e}");
                    process::exit(1);
                });

            println!("========================================");
            println!("  MECHASTRON PROJECT INSPECTION");
            println!("========================================");
            println!("Title:      {}", project.title);
            println!(
                "Canvas:     {}x{} @ {} FPS",
                project.width, project.height, project.fps
            );
            println!(
                "Duration:   {} ms ({}s)",
                project.duration_ms,
                project.duration_ms as f32 / 1000.0
            );
            println!("Layers:     {}", project.layers.len());
            println!("----------------------------------------");
            for (idx, layer) in project.layers.iter().enumerate() {
                println!(
                    "  [{}] id='{}' name='{}' type={:?} asset={:?} parent={:?}",
                    idx + 1,
                    layer.id,
                    layer.name,
                    layer.layer_type,
                    layer.asset_ref,
                    layer.parent_layer_id
                );
            }
            println!("========================================");
        }

        cmd => {
            eprintln!("Unknown subcommand: {cmd}");
            print_usage();
            process::exit(1);
        }
    }
}
