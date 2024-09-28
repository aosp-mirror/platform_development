// Copyright (C) 2024 The Android Open Source Project
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
    fs::{copy, read_dir, read_link, remove_dir_all, rename},
    os::unix::fs::symlink,
    process::{Command, Output},
    str::from_utf8,
};

use anyhow::{anyhow, Context, Result};
use glob::glob;
use google_metadata::GoogleMetadata;
use name_and_version::NamedAndVersioned;
use rooted_path::RootedPath;
use semver::Version;

use crate::{copy_dir, ensure_exists_and_empty, run_cargo_embargo, Crate};

#[derive(Debug)]
pub struct ManagedCrate<State: ManagedCrateState> {
    android_crate: Crate,
    vendored_crate: Crate,
    extra: State,
}

#[derive(Debug)]
pub struct New {}
#[derive(Debug)]
pub struct Staged {
    patch_output: Vec<(String, Output)>,
    cargo_embargo_output: Output,
    android_bp_diff: Output,
}
pub trait ManagedCrateState {}
impl ManagedCrateState for New {}
impl ManagedCrateState for Staged {}

static CUSTOMIZATIONS: &[&str] = &[
    "*.bp",
    "*.mk",
    "cargo_embargo.json",
    "patches",
    "METADATA",
    "TEST_MAPPING",
    "MODULE_LICENSE_*",
];

static SYMLINKS: &[&str] = &["LICENSE", "NOTICE"];

impl<State: ManagedCrateState> ManagedCrate<State> {
    pub fn name(&self) -> &str {
        self.vendored_crate.name()
    }
    pub fn vendored_version(&self) -> &Version {
        self.vendored_crate.version()
    }
    pub fn android_version(&self) -> &Version {
        self.android_crate.version()
    }
    pub fn staging_path(&self) -> RootedPath {
        self.android_crate
            .path()
            .with_same_root("out/rust-crate-temporary-build")
            .unwrap()
            .join(format!("{}-{}", self.name(), self.vendored_version()))
            .unwrap()
    }
    pub fn android_crate_path(&self) -> &RootedPath {
        self.android_crate.path()
    }
    pub fn android_bp(&self) -> RootedPath {
        self.android_crate_path().join("Android.bp").unwrap()
    }
    pub fn cargo_embargo_json(&self) -> RootedPath {
        self.android_crate_path().join("cargo_embargo.json").unwrap()
    }
}

impl ManagedCrate<New> {
    pub fn new(android_crate: Crate, vendored_crate: Crate) -> Self {
        ManagedCrate { android_crate, vendored_crate, extra: New {} }
    }
    pub fn stage(self) -> Result<ManagedCrate<Staged>> {
        self.copy_to_staging()?;

        // Workaround. When checking the health of a legacy crate, there is no separate vendored crate,
        // so we just have self.android_crate and self.vendored_crate point to the same directory.
        // In this case, there is no need to copy Android customizations into the clean vendored copy
        // or apply the patches.
        if self.android_crate.path() != self.vendored_crate.path() {
            self.copy_customizations()?;
        }

        let patch_output = if self.android_crate.path() != self.vendored_crate.path() {
            self.apply_patches()?
        } else {
            Vec::new()
        };

        let cargo_embargo_output = run_cargo_embargo(&self.staging_path())?;
        let android_bp_diff = self.diff_android_bp()?;

        Ok(ManagedCrate {
            android_crate: self.android_crate,
            vendored_crate: self.vendored_crate,
            extra: Staged { patch_output, cargo_embargo_output, android_bp_diff },
        })
    }
    fn copy_to_staging(&self) -> Result<()> {
        let staging_path = self.staging_path();
        ensure_exists_and_empty(&staging_path)?;
        remove_dir_all(&staging_path).context(format!("Failed to remove {}", staging_path))?;
        copy_dir(self.vendored_crate.path(), &staging_path).context(format!(
            "Failed to copy {} to {}",
            self.vendored_crate.path(),
            self.staging_path()
        ))?;
        if staging_path.join(".git")?.abs().is_dir() {
            remove_dir_all(staging_path.join(".git")?)
                .with_context(|| "Failed to remove .git".to_string())?;
        }
        Ok(())
    }
    fn copy_customizations(&self) -> Result<()> {
        let dest_dir = self.staging_path();
        for pattern in CUSTOMIZATIONS {
            let full_pattern = self.android_crate.path().join(pattern)?;
            for entry in glob(
                full_pattern
                    .abs()
                    .to_str()
                    .ok_or(anyhow!("Failed to convert path {} to str", full_pattern))?,
            )? {
                let entry = entry?;
                let filename = entry
                    .file_name()
                    .context(format!("Failed to get file name for {}", entry.display()))?
                    .to_os_string();
                if entry.is_dir() {
                    copy_dir(&entry, &dest_dir.join(filename)?).context(format!(
                        "Failed to copy {} to {}",
                        entry.display(),
                        dest_dir
                    ))?;
                } else {
                    let dest_file = dest_dir.join(&filename)?;
                    if dest_file.abs().exists() {
                        return Err(anyhow!("Destination file {} exists", dest_file));
                    }
                    copy(&entry, dest_dir.join(filename)?).context(format!(
                        "Failed to copy {} to {}",
                        entry.display(),
                        dest_dir
                    ))?;
                }
            }
        }
        for link in SYMLINKS {
            let src_path = self.android_crate.path().join(link)?;
            if src_path.abs().is_symlink() {
                let dest = read_link(src_path)?;
                if dest.exists() {
                    return Err(anyhow!(
                        "Can't symlink {} -> {} because destination exists",
                        link,
                        dest.display(),
                    ));
                }
                symlink(dest, dest_dir.join(link)?)?;
            }
        }
        Ok(())
    }
    fn apply_patches(&self) -> Result<Vec<(String, Output)>> {
        let mut patch_output = Vec::new();
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
                patch_output.push((
                    String::from_utf8_lossy(entry.file_name().as_encoded_bytes()).to_string(),
                    output,
                ));
            }
        }
        Ok(patch_output)
    }
    fn diff_android_bp(&self) -> Result<Output> {
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
            .arg(self.android_bp().rel())
            .arg(self.staging_path().join("Android.bp")?.rel())
            .current_dir(self.android_crate.path().root())
            .output()?)
    }
    fn patch_dir(&self) -> RootedPath {
        self.staging_path().join("patches").unwrap()
    }
    pub fn regenerate(self, update_metadata: bool) -> Result<ManagedCrate<Staged>> {
        // Should be <New>
        let staged = self.stage()?;
        staged.check_staged()?;
        if !staged.staging_path().abs().exists() {
            return Err(anyhow!("Staged crate not found at {}", staged.staging_path()));
        }
        if update_metadata {
            let mut metadata =
                GoogleMetadata::try_from(staged.staging_path().join("METADATA").unwrap())?;
            let mut writeback = false;
            writeback |= metadata.migrate_homepage();
            writeback |= metadata.migrate_archive();
            writeback |= metadata.remove_deprecated_url();
            if staged.android_crate.version() != staged.vendored_crate.version() {
                metadata.set_date_to_today()?;
                metadata.set_version_and_urls(
                    staged.vendored_crate.name(),
                    staged.vendored_crate.version().to_string(),
                )?;
                writeback |= true;
            }
            if writeback {
                metadata.write()?;
            }
        }

        let android_crate_dir = staged.android_crate.path();
        remove_dir_all(android_crate_dir)?;
        rename(staged.staging_path(), android_crate_dir)?;

        Ok(staged)
    }
}

impl ManagedCrate<Staged> {
    pub fn check_staged(&self) -> Result<()> {
        if !self.patch_success() {
            for (patch, output) in self.patch_output() {
                if !output.status.success() {
                    return Err(anyhow!(
                        "Failed to patch {} with {}\nstdout:\n{}\nstderr:\n{}",
                        self.vendored_crate.name(),
                        patch,
                        from_utf8(&output.stdout)?,
                        from_utf8(&output.stderr)?
                    ));
                }
            }
        }
        if !self.cargo_embargo_success() {
            let output = self.cargo_embargo_output();
            return Err(anyhow!(
                "cargo_embargo execution failed for {}:\nstdout:\n{}\nstderr:\n:{}",
                self.vendored_crate.name(),
                from_utf8(&output.stdout)?,
                from_utf8(&output.stderr)?
            ));
        }

        Ok(())
    }
    pub fn diff_staged(&self) -> Result<()> {
        let diff_status = Command::new("diff")
            .args(["-u", "-r", "-w", "--no-dereference"])
            .arg(self.staging_path().rel())
            .arg(self.android_crate.path().rel())
            .current_dir(self.vendored_crate.path().root())
            .spawn()?
            .wait()?;
        if !diff_status.success() {
            return Err(anyhow!(
                "Found differences between {} and {}",
                self.android_crate.path(),
                self.staging_path()
            ));
        }

        Ok(())
    }
    pub fn patch_success(&self) -> bool {
        self.extra.patch_output.iter().all(|output| output.1.status.success())
    }
    pub fn patch_output(&self) -> &Vec<(String, Output)> {
        &self.extra.patch_output
    }
    pub fn android_bp_diff(&self) -> &Output {
        &self.extra.android_bp_diff
    }
    pub fn cargo_embargo_output(&self) -> &Output {
        &self.extra.cargo_embargo_output
    }
    pub fn cargo_embargo_success(&self) -> bool {
        self.extra.cargo_embargo_output.status.success()
    }
    pub fn android_bp_unchanged(&self) -> bool {
        self.extra.android_bp_diff.status.success()
    }
}
