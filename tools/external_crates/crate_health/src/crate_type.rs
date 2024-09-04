// Copyright (C) 2023 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use std::{
    fs::{read_dir, remove_dir_all},
    path::Path,
    process::{Command, Output},
};

use anyhow::{anyhow, Context, Result};
use cargo::{
    core::{Manifest, SourceId},
    util::toml::read_manifest,
    Config,
};
use name_and_version::{IsUpgradableTo, NameAndVersionRef, NamedAndVersioned};
use rooted_path::RootedPath;
use semver::Version;

use crate::{copy_dir, ensure_exists_and_empty, CrateError};

#[derive(Debug)]
pub struct Crate {
    manifest: Manifest,

    path: RootedPath,

    patch_output: Vec<(String, Output)>,
    generate_android_bp_output: Option<Output>,
    android_bp_diff: Option<Output>,
}

impl NamedAndVersioned for Crate {
    fn name(&self) -> &str {
        self.manifest.name().as_str()
    }
    fn version(&self) -> &Version {
        self.manifest.version()
    }
    fn key(&self) -> NameAndVersionRef {
        NameAndVersionRef::new(self.name(), self.version())
    }
}

impl IsUpgradableTo for Crate {}

impl Crate {
    pub fn new(manifest: Manifest, path: RootedPath) -> Crate {
        Crate {
            manifest,
            path,
            patch_output: Vec::new(),
            generate_android_bp_output: None,
            android_bp_diff: None,
        }
    }
    pub fn from(cargo_toml: RootedPath) -> Result<Crate> {
        let manifest_dir =
            cargo_toml.with_same_root(cargo_toml.rel().parent().ok_or(anyhow!(
                "Failed to get parent directory of manifest at {}",
                cargo_toml
            ))?)?;
        let source_id = SourceId::for_path(manifest_dir.abs())?;
        let (manifest, _nested) =
            read_manifest(cargo_toml.as_ref(), source_id, &Config::default()?)?;
        match manifest {
            cargo::core::EitherManifest::Real(r) => Ok(Crate::new(r, manifest_dir)),
            cargo::core::EitherManifest::Virtual(_) => {
                Err(anyhow!(CrateError::VirtualCrate(cargo_toml.as_ref().to_path_buf())))
            }
        }
    }

    pub fn description(&self) -> &str {
        self.manifest.metadata().description.as_deref().unwrap_or("")
    }
    pub fn license(&self) -> Option<&str> {
        self.manifest.metadata().license.as_deref()
    }
    pub fn license_file(&self) -> Option<&str> {
        self.manifest.metadata().license_file.as_deref()
    }
    pub fn repository(&self) -> Option<&str> {
        self.manifest.metadata().repository.as_deref()
    }
    pub fn path(&self) -> &RootedPath {
        &self.path
    }
    pub fn android_bp(&self) -> RootedPath {
        self.path.join("Android.bp").unwrap()
    }
    pub fn cargo_embargo_json(&self) -> RootedPath {
        self.path.join("cargo_embargo.json").unwrap()
    }
    pub fn staging_path(&self) -> RootedPath {
        self.path
            .with_same_root("out/rust-crate-temporary-build")
            .unwrap()
            .join(self.staging_dir_name())
            .unwrap()
    }
    pub fn patch_dir(&self) -> RootedPath {
        self.staging_path().join("patches").unwrap()
    }
    pub fn staging_dir_name(&self) -> String {
        if let Some(dirname) = self.path.rel().file_name().and_then(|x| x.to_str()) {
            if dirname == self.name() {
                return dirname.to_string();
            }
        }
        format!("{}-{}", self.name(), self.version())
    }

    pub fn is_crates_io(&self) -> bool {
        const NOT_CRATES_IO: &[&str] = &[
            "external/rust/beto-rust/",                 // Google crates
            "external/rust/pica/",                      // Google crate
            "external/rust/crates/webpki/third-party/", // Internal/example code
            "external/rust/cxx/third-party/",           // Internal/example code
            "external/rust/cxx/demo/",                  // Internal/example code
        ];
        !NOT_CRATES_IO.iter().any(|prefix| self.path().rel().starts_with(prefix))
    }
    pub fn is_migration_denied(&self) -> bool {
        const MIGRATION_DENYLIST: &[&str] = &[
            "external/rust/crates/openssl/", // It's complicated.
            "external/rust/cxx/",            // It's REALLY complicated.
        ];
        MIGRATION_DENYLIST.iter().any(|prefix| self.path().rel().starts_with(prefix))
    }
    pub fn is_android_bp_healthy(&self) -> bool {
        !self.is_migration_denied()
            && self.android_bp().abs().exists()
            && self.cargo_embargo_json().abs().exists()
            && self.generate_android_bp_success()
            && self.android_bp_unchanged()
    }
    pub fn patch_success(&self) -> bool {
        self.patch_output.iter().all(|output| output.1.status.success())
    }
    pub fn generate_android_bp_success(&self) -> bool {
        self.generate_android_bp_output.as_ref().is_some_and(|output| output.status.success())
    }
    pub fn android_bp_unchanged(&self) -> bool {
        self.android_bp_diff.as_ref().is_some_and(|output| output.status.success())
    }

    // Make a clean copy of the crate in out/
    pub fn stage_crate(&self) -> Result<()> {
        let staging_path = self.staging_path();
        ensure_exists_and_empty(&staging_path)?;
        remove_dir_all(&staging_path).context(format!("Failed to remove {}", staging_path))?;
        copy_dir(self.path(), &staging_path).context(format!(
            "Failed to copy {} to {}",
            self.path(),
            self.staging_path()
        ))?;
        if staging_path.join(".git")?.abs().is_dir() {
            remove_dir_all(staging_path.join(".git")?)
                .with_context(|| "Failed to remove .git".to_string())?;
        }
        Ok(())
    }

    pub fn diff_android_bp(&mut self) -> Result<()> {
        self.set_diff_output(
            diff_android_bp(
                &self.android_bp().rel(),
                &self.staging_path().join("Android.bp")?.rel(),
                &self.path.root(),
            )
            .context("Failed to diff Android.bp".to_string())?,
        );
        Ok(())
    }

    pub fn apply_patches(&mut self) -> Result<()> {
        let patch_dir = self.patch_dir();
        if patch_dir.abs().exists() {
            for entry in
                read_dir(&patch_dir).context(format!("Failed to read_dir {}", patch_dir))?
            {
                let entry = entry?;
                if entry.file_name() == "Android.bp.patch"
                    || entry.file_name() == "Android.bp.diff"
                    || entry.file_name() == "rules.mk.diff"
                {
                    continue;
                }
                let entry_path = entry.path();
                let output = Command::new("patch")
                    .args(["-p1", "-l", "--no-backup-if-mismatch", "-i"])
                    .arg(&entry_path)
                    .current_dir(self.staging_path())
                    .output()?;
                self.patch_output.push((
                    String::from_utf8_lossy(entry.file_name().as_encoded_bytes()).to_string(),
                    output,
                ));
            }
        }
        Ok(())
    }

    pub fn android_bp_diff(&self) -> Option<&Output> {
        self.android_bp_diff.as_ref()
    }
    pub fn generate_android_bp_output(&self) -> Option<&Output> {
        self.generate_android_bp_output.as_ref()
    }
    pub fn set_generate_android_bp_output(&mut self, c2a_output: Output) {
        self.generate_android_bp_output.replace(c2a_output);
    }
    pub fn set_diff_output(&mut self, diff_output: Output) {
        self.android_bp_diff.replace(diff_output);
    }
    pub fn set_patch_output(&mut self, patch_output: Vec<(String, Output)>) {
        self.patch_output = patch_output;
    }
    pub fn patch_output(&self) -> &Vec<(String, Output)> {
        &self.patch_output
    }
}

pub trait Migratable {
    fn is_migration_eligible(&self) -> bool;
    fn is_migratable(&self) -> bool;
}

impl Migratable for Crate {
    fn is_migration_eligible(&self) -> bool {
        self.is_crates_io()
            && !self.is_migration_denied()
            && self.android_bp().abs().exists()
            && self.cargo_embargo_json().abs().exists()
    }
    fn is_migratable(&self) -> bool {
        self.patch_success() && self.generate_android_bp_success() && self.android_bp_unchanged()
    }
}

#[cfg(test)]
mod tests {
    use std::{
        fs::{create_dir, write},
        path::PathBuf,
    };

    use super::*;
    use anyhow::anyhow;
    use tempfile::tempdir;

    fn write_test_manifest(temp_crate_dir: &Path, name: &str, version: &str) -> Result<RootedPath> {
        let cargo_toml: PathBuf = [temp_crate_dir, &Path::new("Cargo.toml")].iter().collect();
        let cargo_toml = RootedPath::new("/", cargo_toml.strip_prefix("/")?)?;
        write(&cargo_toml, format!("[package]\nname = \"{}\"\nversion = \"{}\"\n", name, version))?;
        let lib_rs: PathBuf = [temp_crate_dir, &Path::new("src/lib.rs")].iter().collect();
        create_dir(lib_rs.parent().ok_or(anyhow!("Failed to get parent"))?)?;
        write(lib_rs.as_path(), "// foo")?;
        Ok(cargo_toml)
    }

    #[test]
    fn test_from_and_properties() -> Result<()> {
        let temp_crate_dir = tempdir()?;
        let cargo_toml = write_test_manifest(temp_crate_dir.path(), "foo", "1.2.0")?;
        let krate = Crate::from(cargo_toml)?;
        assert_eq!(krate.name(), "foo");
        assert_eq!(krate.version().to_string(), "1.2.0");
        assert!(krate.is_crates_io());
        assert_eq!(krate.android_bp().abs(), temp_crate_dir.path().join("Android.bp"));
        assert_eq!(
            krate.cargo_embargo_json().abs(),
            temp_crate_dir.path().join("cargo_embargo.json")
        );
        Ok(())
    }

    #[test]
    fn test_from_error() -> Result<()> {
        let temp_crate_dir = tempdir()?;
        let cargo_toml = write_test_manifest(temp_crate_dir.path(), "foo", "1.2.0")?;
        assert!(Crate::from(cargo_toml.join("blah")?).is_err());
        Ok(())
    }
}

pub fn diff_android_bp(
    a: &impl AsRef<Path>,
    b: &impl AsRef<Path>,
    root: &impl AsRef<Path>,
) -> Result<Output> {
    Ok(Command::new("diff")
        .args([
            "-u",
            "-w",
            "-B",
            "-I",
            r#"default_team: "trendy_team_android_rust""#,
            "-I",
            "// has rustc warnings",
            "-I",
            "This file is generated by",
            "-I",
            "cargo_pkg_version:",
        ])
        .arg(a.as_ref())
        .arg(b.as_ref())
        .current_dir(root)
        .output()?)
}
