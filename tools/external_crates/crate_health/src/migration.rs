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
    fs::{copy, read_link, remove_dir_all},
    os::unix::fs::symlink,
    process::Output,
};

use anyhow::{anyhow, Context, Result};
use glob::glob;

use crate::{
    copy_dir, crate_type::diff_android_bp, most_recent_version, CompatibleVersionPair, Crate,
    CrateCollection, Migratable, NameAndVersionMap, PseudoCrate, RepoPath, VersionMatch,
};

static CUSTOMIZATIONS: &'static [&'static str] =
    &["*.bp", "cargo_embargo.json", "patches", "METADATA", "TEST_MAPPING", "MODULE_LICENSE_*"];

static SYMLINKS: &'static [&'static str] = &["LICENSE", "NOTICE"];

impl<'a> CompatibleVersionPair<'a, Crate> {
    pub fn copy_customizations(&self) -> Result<()> {
        let dest_dir_absolute = self.dest.staging_path().abs();
        for pattern in CUSTOMIZATIONS {
            let full_pattern = self.source.path().join(pattern);
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
                    copy_dir(&entry, &dest_dir_absolute.join(filename)).context(format!(
                        "Failed to copy {} to {}",
                        entry.display(),
                        self.dest.staging_path()
                    ))?;
                } else {
                    let dest_file = dest_dir_absolute.join(&filename);
                    if dest_file.exists() {
                        return Err(anyhow!("Destination file {} exists", dest_file.display()));
                    }
                    copy(&entry, dest_dir_absolute.join(filename)).context(format!(
                        "Failed to copy {} to {}",
                        entry.display(),
                        dest_dir_absolute.display()
                    ))?;
                }
            }
        }
        for link in SYMLINKS {
            let src_path = self.source.path().join(link);
            if src_path.abs().is_symlink() {
                let dest = read_link(src_path.abs())?;
                if dest.exists() {
                    return Err(anyhow!(
                        "Can't symlink {} -> {} because destination exists",
                        link,
                        dest.display(),
                    ));
                }
                symlink(dest, dest_dir_absolute.join(link))?;
            }
        }
        Ok(())
    }
    pub fn diff_android_bps(&self) -> Result<Output> {
        diff_android_bp(
            &self.source.android_bp().rel(),
            &self.dest.staging_path().join(&"Android.bp").rel(),
            &self.source.path().root(),
        )
        .context("Failed to diff Android.bp".to_string())
    }
}

pub fn migrate(
    source_dir: RepoPath,
    pseudo_crate_dir: RepoPath,
) -> Result<VersionMatch<CrateCollection>> {
    let mut source = CrateCollection::new(source_dir.root());
    source.add_from(&source_dir.rel())?;
    source.map_field_mut().retain(|_nv, krate| krate.is_crates_io());

    let pseudo_crate = PseudoCrate::new(pseudo_crate_dir);
    if pseudo_crate.get_path().abs().exists() {
        remove_dir_all(pseudo_crate.get_path().abs())
            .context(format!("Failed to remove {}", pseudo_crate.get_path()))?;
    }
    pseudo_crate.init(
        source
            .filter_versions(&most_recent_version)
            .filter(|(_nv, krate)| krate.is_migration_eligible())
            .map(|(_nv, krate)| krate),
    )?;

    let mut dest = CrateCollection::new(source.repo_root());
    dest.add_from(&pseudo_crate.get_path().join(&"vendor").rel())?;

    let mut version_match = VersionMatch::new(source, dest)?;

    version_match.stage_crates()?;
    version_match.copy_customizations()?;
    version_match.apply_patches()?;
    version_match.generate_android_bps()?;
    version_match.diff_android_bps()?;

    Ok(version_match)
}
