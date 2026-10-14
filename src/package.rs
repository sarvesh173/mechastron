//! Offline ZIP Archive Packaging (.mepack) for Mechastron Studio.

use crate::codec::MechastronXmlCodec;
use crate::model::SceneProject;
use std::fs::{self, File};
use std::io::{Read, Write};
use std::path::Path;
use zip::write::SimpleFileOptions;
use zip::{ZipArchive, ZipWriter};

pub struct PackageArchiveManager;

impl PackageArchiveManager {
    /// Bundles a project and its media assets into an offline, portable .mepack (ZIP).
    pub fn export_package(
        project: &SceneProject,
        asset_dir: &Path,
        output_zip_path: &Path,
    ) -> Result<(), String> {
        if let Some(parent) = output_zip_path.parent() {
            fs::create_dir_all(parent).map_err(|e| e.to_string())?;
        }

        let zip_file = File::create(output_zip_path).map_err(|e| e.to_string())?;
        let mut zip = ZipWriter::new(zip_file);
        let options = SimpleFileOptions::default()
            .compression_method(zip::CompressionMethod::Deflated)
            .unix_permissions(0o644);

        // 1. Write project.xml
        let xml_str = MechastronXmlCodec::serialize(project);
        zip.start_file("project.xml", options)
            .map_err(|e| e.to_string())?;
        zip.write_all(xml_str.as_bytes())
            .map_err(|e| e.to_string())?;

        // 2. Write assets/
        if asset_dir.exists() && asset_dir.is_dir() {
            Self::add_dir_to_zip(&mut zip, asset_dir, Path::new("assets"), options)?;
        }

        zip.finish().map_err(|e| e.to_string())?;
        Ok(())
    }

    /// Unpacks a .mepack ZIP file into the destination directory and parses the project.
    pub fn import_package(zip_path: &Path, extract_to_dir: &Path) -> Result<SceneProject, String> {
        fs::create_dir_all(extract_to_dir).map_err(|e| e.to_string())?;

        let file = File::open(zip_path).map_err(|e| e.to_string())?;
        let mut archive = ZipArchive::new(file).map_err(|e| e.to_string())?;

        let mut project_xml_content = None;

        for i in 0..archive.len() {
            let mut file_entry = archive.by_index(i).map_err(|e| e.to_string())?;
            let outpath = match file_entry.enclosed_name() {
                Some(path) => extract_to_dir.join(path),
                None => continue,
            };

            if file_entry.is_dir() {
                fs::create_dir_all(&outpath).map_err(|e| e.to_string())?;
            } else {
                if let Some(p) = outpath.parent() {
                    if !p.exists() {
                        fs::create_dir_all(p).map_err(|e| e.to_string())?;
                    }
                }
                let mut outfile = File::create(&outpath).map_err(|e| e.to_string())?;
                let mut buffer = Vec::new();
                file_entry
                    .read_to_end(&mut buffer)
                    .map_err(|e| e.to_string())?;
                outfile.write_all(&buffer).map_err(|e| e.to_string())?;

                if file_entry.name() == "project.xml" {
                    project_xml_content = Some(String::from_utf8_lossy(&buffer).to_string());
                }
            }
        }

        let xml = project_xml_content
            .ok_or_else(|| "project.xml not found in .mepack archive".to_string())?;
        MechastronXmlCodec::deserialize(&xml)
    }

    fn add_dir_to_zip<W: Write + std::io::Seek>(
        zip: &mut ZipWriter<W>,
        src_dir: &Path,
        zip_prefix: &Path,
        options: SimpleFileOptions,
    ) -> Result<(), String> {
        let entries = fs::read_dir(src_dir).map_err(|e| e.to_string())?;
        for entry in entries {
            let entry = entry.map_err(|e| e.to_string())?;
            let path = entry.path();
            let file_name = path
                .file_name()
                .ok_or_else(|| "Invalid file name".to_string())?;
            let zip_path = zip_prefix.join(file_name);

            if path.is_dir() {
                Self::add_dir_to_zip(zip, &path, &zip_path, options)?;
            } else {
                let zip_path_str = zip_path
                    .to_str()
                    .ok_or_else(|| "Invalid path string".to_string())?;
                zip.start_file(zip_path_str, options)
                    .map_err(|e| e.to_string())?;
                let mut f = File::open(&path).map_err(|e| e.to_string())?;
                let mut buffer = Vec::new();
                f.read_to_end(&mut buffer).map_err(|e| e.to_string())?;
                zip.write_all(&buffer).map_err(|e| e.to_string())?;
            }
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::model::Layer;
    use tempfile::tempdir;

    #[test]
    fn test_package_roundtrip() {
        let temp_dir = tempdir().unwrap();
        let assets_dir = temp_dir.path().join("assets");
        fs::create_dir_all(&assets_dir).unwrap();

        let dummy_image = assets_dir.join("sample.png");
        fs::write(&dummy_image, b"PNG_RAW_BYTES").unwrap();

        let mut project = SceneProject::new("Bundle Demo", 1080, 1920, 60, 5000);
        let layer = Layer::new_image("l1", "Hero", "assets/sample.png", 5000);
        project.add_layer(layer);

        let mepack_path = temp_dir.path().join("demo.mepack");
        PackageArchiveManager::export_package(&project, &assets_dir, &mepack_path).unwrap();
        assert!(mepack_path.exists());

        let extract_dir = temp_dir.path().join("extracted");
        let imported = PackageArchiveManager::import_package(&mepack_path, &extract_dir).unwrap();

        assert_eq!(imported.title, "Bundle Demo");
        assert_eq!(imported.layers.len(), 1);
        assert_eq!(
            imported.layers[0].asset_ref.as_deref(),
            Some("assets/sample.png")
        );
        assert!(extract_dir.join("assets/sample.png").exists());
    }
}
