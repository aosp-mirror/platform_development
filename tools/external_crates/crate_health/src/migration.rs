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
    fs::{copy, read_link},
    os::unix::fs::symlink,
    process::Output,
};

use anyhow::{anyhow, Context, Result};
use glob::glob;

use crate::{copy_dir, crate_type::diff_android_bp, CompatibleVersionPair, Crate};

static CUSTOMIZATIONS: &'static [&'static str] =
    &["*.bp", "cargo_embargo.json", "patches", "METADATA", "TEST_MAPPING", "MODULE_LICENSE_*"];

static SYMLINKS: &'static [&'static str] = &["LICENSE", "NOTICE"];

impl<'a> CompatibleVersionPair<'a, Crate> {
    pub fn copy_customizations(&self) -> Result<()> {
        let dest_dir = self.dest.staging_path();
        for pattern in CUSTOMIZATIONS {
            let full_pattern = self.source.path().join(pattern)?;
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
            let src_path = self.source.path().join(link)?;
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
    pub fn diff_android_bps(&self) -> Result<Output> {
        diff_android_bp(
            &self.source.android_bp().rel(),
            &self.dest.staging_path().join(&"Android.bp")?.rel(),
            &self.source.path().root(),
        )
        .context("Failed to diff Android.bp".to_string())
    }
}
