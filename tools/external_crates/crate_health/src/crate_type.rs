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
    fs::{copy, read_dir, remove_dir_all},
    path::{Path, PathBuf},
    process::{Command, Output},
    str::from_utf8,
};

use anyhow::{anyhow, Context, Result};
use cargo::{
    core::{Manifest, SourceId},
    util::toml::read_manifest,
    Config,
};
use copy_dir::copy_dir;
use semver::Version;

use crate::{
    ensure_exists_and_empty, name_and_version::IsUpgradableTo, CrateError, NameAndVersionRef,
    NamedAndVersioned,
};

#[derive(Debug)]
pub struct Crate {
    manifest: Manifest,

    // root is absolute. All other paths are relative to it.
    root: PathBuf,
    relpath: PathBuf,
    pseudo_crate: Option<PathBuf>,

    // compatible_dest_version: Option<Version>,
    patch_output: Vec<Output>,
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
    fn key<'k>(&'k self) -> NameAndVersionRef<'k> {
        NameAndVersionRef::new(self.name(), self.version())
    }
}

impl IsUpgradableTo for Crate {}

impl Crate {
    pub fn new<P: Into<PathBuf>, Q: Into<PathBuf>, R: Into<PathBuf>>(
        manifest: Manifest,
        root: P,
        relpath: Q,
        pseudo_crate: Option<R>,
    ) -> Crate {
        Crate {
            manifest,
            root: root.into(),
            relpath: relpath.into(),
            pseudo_crate: pseudo_crate.map(|p| p.into()),
            // compatible_dest_version: None,
            patch_output: Vec::new(),
            generate_android_bp_output: None,
            android_bp_diff: None,
        }
    }
    pub fn from<P: Into<PathBuf>, Q: Into<PathBuf>>(
        cargo_toml: &impl AsRef<Path>,
        root: P,
        pseudo_crate: Option<Q>,
    ) -> Result<Crate> {
        let root: PathBuf = root.into();
        let manifest_dir = cargo_toml.as_ref().parent().ok_or(anyhow!(
            "Failed to get parent directory of manifest at {}",
            cargo_toml.as_ref().display()
        ))?;
        let relpath = manifest_dir.strip_prefix(&root)?.to_path_buf();
        let source_id = SourceId::for_path(manifest_dir)?;
        let (manifest, _nested) =
            read_manifest(cargo_toml.as_ref(), source_id, &Config::default()?)?;
        match manifest {
            cargo::core::EitherManifest::Real(r) => Ok(Crate::new(r, root, relpath, pseudo_crate)),
            cargo::core::EitherManifest::Virtual(_) => {
                Err(anyhow!(CrateError::VirtualCrate(cargo_toml.as_ref().to_path_buf())))
            }
        }
    }

    pub fn root(&self) -> &Path {
        self.root.as_path()
    }
    pub fn relpath(&self) -> &Path {
        &self.relpath.as_path()
    }
    pub fn path(&self) -> PathBuf {
        self.root.join(&self.relpath)
    }
    pub fn android_bp(&self) -> PathBuf {
        if let Some(d) = self.customization_dir() {
            d.join("Android.bp.disabled")
        } else {
            self.path().join("Android.bp")
        }
    }
    pub fn cargo_embargo_json(&self) -> PathBuf {
        if let Some(d) = self.customization_dir() {
            d.join("cargo_embargo.json")
        } else {
            self.path().join("cargo_embargo.json")
        }
    }
    pub fn staging_path(&self) -> PathBuf {
        Path::new("out/rust-crate-temporary-build").join(self.staging_dir_name())
    }
    pub fn customization_dir(&self) -> Option<PathBuf> {
        self.pseudo_crate.as_ref().map(|pseudo_crate| {
            pseudo_crate.join("android/customizations").join(self.staging_dir_name())
        })
    }
    pub fn patch_dir(&self) -> Option<PathBuf> {
        self.customization_dir().map(|p| p.join("patches"))
    }
    pub fn staging_dir_name(&self) -> String {
        if let Some(dirname) = self.relpath.file_name().and_then(|x| x.to_str()) {
            if dirname == self.name() {
                return dirname.to_string();
            }
        }
        format!("{}-{}", self.name(), self.version().to_string())
    }

    pub fn aosp_url(&self) -> Option<String> {
        if self.relpath.starts_with("external/rust/crates") {
            if self.relpath.ends_with(self.name()) {
                Some(format!(
                    "https://android.googlesource.com/platform/{}/+/refs/heads/main",
                    self.relpath().display()
                ))
            } else if self.relpath.parent()?.ends_with(self.name()) {
                Some(format!(
                    "https://android.googlesource.com/platform/{}/+/refs/heads/main/{}",
                    self.relpath().parent()?.display(),
                    self.relpath().file_name()?.to_str()?
                ))
            } else {
                None
            }
        } else {
            None
        }
    }
    pub fn crates_io_url(&self) -> String {
        format!("https://crates.io/crates/{}", self.name())
    }

    pub fn is_vendored(&self) -> bool {
        self.pseudo_crate.is_some()
    }
    pub fn is_crates_io(&self) -> bool {
        const NOT_CRATES_IO: &'static [&'static str] = &[
            "external/rust/beto-rust/",                 // Google crates
            "external/rust/pica/",                      // Google crate
            "external/rust/crates/webpki/third-party/", // Internal/example code
            "external/rust/cxx/third-party/",           // Internal/example code
            "external/rust/cxx/demo/",                  // Internal/example code
        ];
        !NOT_CRATES_IO.iter().any(|prefix| self.relpath.starts_with(prefix))
    }
    pub fn is_migration_denied(&self) -> bool {
        const MIGRATION_DENYLIST: &'static [&'static str] = &[
            "external/rust/crates/openssl/", // It's complicated.
            "external/rust/cxx/",            // It's REALLY complicated.
        ];
        MIGRATION_DENYLIST.iter().any(|prefix| self.relpath.starts_with(prefix))
    }
    pub fn is_android_bp_healthy(&self) -> bool {
        !self.is_migration_denied()
            && self.android_bp().exists()
            && self.cargo_embargo_json().exists()
            && self.generate_android_bp_success()
            && self.android_bp_unchanged()
    }
    pub fn patch_success(&self) -> bool {
        self.patch_output.iter().all(|output| output.status.success())
    }
    pub fn generate_android_bp_success(&self) -> bool {
        self.generate_android_bp_output.as_ref().is_some_and(|output| output.status.success())
    }
    pub fn android_bp_unchanged(&self) -> bool {
        self.android_bp_diff.as_ref().is_some_and(|output| output.status.success())
    }

    pub fn print(&self) -> Result<()> {
        println!("{} {} {}", self.name(), self.version(), self.relpath.display());
        if let Some(output) = &self.generate_android_bp_output {
            println!("generate Android.bp exit status: {}", output.status);
            println!("{}", from_utf8(&output.stdout)?);
            println!("{}", from_utf8(&output.stderr)?);
        }
        if let Some(output) = &self.android_bp_diff {
            println!("diff exit status: {}", output.status);
            println!("{}", from_utf8(&output.stdout)?);
            println!("{}", from_utf8(&output.stderr)?);
        }
        Ok(())
    }

    // Make a clean copy of the crate in out/
    pub fn stage_crate(&self) -> Result<()> {
        let staging_path_absolute = self.root().join(self.staging_path());
        ensure_exists_and_empty(&staging_path_absolute)?;
        remove_dir_all(&staging_path_absolute)
            .context(format!("Failed to remove {}", staging_path_absolute.display()))?;
        copy_dir(self.path(), &staging_path_absolute).context(format!(
            "Failed to copy {} to {}",
            self.path().display(),
            staging_path_absolute.display()
        ))?;
        if staging_path_absolute.join(".git").is_dir() {
            remove_dir_all(staging_path_absolute.join(".git"))
                .with_context(|| "Failed to remove .git".to_string())?;
        }
        self.copy_customizations()
    }
    pub fn copy_customizations(&self) -> Result<()> {
        if let Some(customization_dir) = self.customization_dir() {
            let customization_dir_absolute = self.root().join(customization_dir);
            let staging_path_absolute = self.root().join(self.staging_path());
            for entry in read_dir(&customization_dir_absolute)
                .context(format!("Failed to read_dir {}", customization_dir_absolute.display()))?
            {
                let entry = entry?;
                let entry_path = entry.path();
                let mut filename = entry.file_name().to_os_string();
                if entry_path.is_dir() {
                    copy_dir(&entry_path, staging_path_absolute.join(&filename)).context(
                        format!(
                            "Failed to copy {} to {}",
                            entry_path.display(),
                            staging_path_absolute.display()
                        ),
                    )?;
                } else {
                    if let Some(extension) = entry_path.extension() {
                        if extension == "disabled" {
                            let mut new_filename = entry_path.clone();
                            new_filename.set_extension("");
                            filename = new_filename
                                .file_name()
                                .context(format!(
                                    "Failed to get file name for {}",
                                    new_filename.display()
                                ))?
                                .to_os_string();
                        }
                    }
                    copy(&entry_path, staging_path_absolute.join(filename)).context(format!(
                        "Failed to copy {} to {}",
                        entry_path.display(),
                        staging_path_absolute.display()
                    ))?;
                }
            }
        }
        Ok(())
    }

    pub fn apply_patches(&mut self) -> Result<()> {
        if let Some(patch_dir) = self.patch_dir() {
            let patch_dir_absolute = self.root().join(patch_dir);
            if patch_dir_absolute.exists() {
                println!("Patching {}", self.path().display());
                for entry in read_dir(&patch_dir_absolute)
                    .context(format!("Failed to read_dir {}", patch_dir_absolute.display()))?
                {
                    let entry = entry?;
                    if entry.file_name() == "Android.bp.patch"
                        || entry.file_name() == "Android.bp.diff"
                    {
                        continue;
                    }
                    let entry_path = entry.path();
                    println!("  applying {}", entry_path.display());
                    let output = Command::new("patch")
                        .args(["-p1", "-l"])
                        .arg(&entry_path)
                        .current_dir(self.root().join(self.staging_path()))
                        .output()?;
                    if !output.status.success() {
                        println!(
                            "Failed to apply {}\nstdout:\n{}\nstderr:\n:{}",
                            entry_path.display(),
                            from_utf8(&output.stdout)?,
                            from_utf8(&output.stderr)?
                        );
                    }
                    self.patch_output.push(output);
                }
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
    pub fn set_generate_android_bp_output(&mut self, c2a_output: Output, diff_output: Output) {
        self.generate_android_bp_output.replace(c2a_output);
        self.android_bp_diff.replace(diff_output);
    }
    pub fn set_patch_output(&mut self, patch_output: Vec<Output>) {
        self.patch_output = patch_output;
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
            && self.android_bp().exists()
            && self.cargo_embargo_json().exists()
    }
    fn is_migratable(&self) -> bool {
        self.patch_success() && self.generate_android_bp_success() && self.android_bp_unchanged()
    }
}

#[cfg(test)]
mod tests {
    use std::fs::{create_dir, write};

    use super::*;
    use anyhow::anyhow;
    use tempfile::tempdir;

    fn write_test_manifest(temp_crate_dir: &Path, name: &str, version: &str) -> Result<PathBuf> {
        let cargo_toml: PathBuf = [temp_crate_dir, &Path::new("Cargo.toml")].iter().collect();
        write(
            cargo_toml.as_path(),
            format!("[package]\nname = \"{}\"\nversion = \"{}\"\n", name, version),
        )?;
        let lib_rs: PathBuf = [temp_crate_dir, &Path::new("src/lib.rs")].iter().collect();
        create_dir(lib_rs.parent().ok_or(anyhow!("Failed to get parent"))?)?;
        write(lib_rs.as_path(), "// foo")?;
        Ok(cargo_toml)
    }

    #[test]
    fn test_from_and_properties() -> Result<()> {
        let temp_crate_dir = tempdir()?;
        let cargo_toml = write_test_manifest(temp_crate_dir.path(), "foo", "1.2.0")?;
        let krate = Crate::from(&cargo_toml, &"/", None::<&&str>)?;
        assert_eq!(krate.name(), "foo");
        assert_eq!(krate.version().to_string(), "1.2.0");
        assert!(krate.is_crates_io());
        assert_eq!(krate.android_bp(), temp_crate_dir.path().join("Android.bp"));
        assert_eq!(krate.cargo_embargo_json(), temp_crate_dir.path().join("cargo_embargo.json"));
        Ok(())
    }

    #[test]
    fn test_from_error() -> Result<()> {
        let temp_crate_dir = tempdir()?;
        let cargo_toml = write_test_manifest(temp_crate_dir.path(), "foo", "1.2.0")?;
        assert!(Crate::from(&cargo_toml, &"/blah", None::<&&str>).is_err());
        Ok(())
    }
}
