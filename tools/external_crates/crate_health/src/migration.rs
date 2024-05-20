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
    fs::copy,
    path::{Path, PathBuf},
};

use anyhow::{anyhow, Context, Result};
use copy_dir::copy_dir;
use glob::glob;

use crate::{
    ensure_exists_and_empty, most_recent_version, write_pseudo_crate, CompatibleVersionPair, Crate,
    CrateCollection, Migratable, NameAndVersionMap, VersionMatch,
};

static CUSTOMIZATIONS: &'static [&'static str] =
    &["*.bp", "cargo_embargo.json", "patches", "METADATA"];

impl<'a> CompatibleVersionPair<'a, Crate> {
    pub fn copy_customizations(&self) -> Result<()> {
        let dest_dir_absolute = self.dest.root().join(
            self.dest
                .customization_dir()
                .ok_or(anyhow!("No customization dir for {}", self.dest.path().display()))?,
        );
        ensure_exists_and_empty(&dest_dir_absolute)?;
        for pattern in CUSTOMIZATIONS {
            let full_pattern = self.source.path().join(pattern);
            for entry in glob(
                full_pattern
                    .to_str()
                    .ok_or(anyhow!("Failed to convert path {} to str", full_pattern.display()))?,
            )? {
                let entry = entry?;
                let mut filename = entry
                    .file_name()
                    .context(format!("Failed to get file name for {}", entry.display()))?
                    .to_os_string();
                if entry.is_dir() {
                    copy_dir(&entry, dest_dir_absolute.join(filename)).context(format!(
                        "Failed to copy {} to {}",
                        entry.display(),
                        dest_dir_absolute.display()
                    ))?;
                } else {
                    if let Some(extension) = entry.extension() {
                        if extension == "bp" {
                            filename.push(".disabled");
                        }
                    }
                    copy(&entry, dest_dir_absolute.join(filename)).context(format!(
                        "Failed to copy {} to {}",
                        entry.display(),
                        dest_dir_absolute.display()
                    ))?;
                }
            }
        }
        Ok(())
    }
}

pub fn migrate<P: Into<PathBuf>>(
    repo_root: P,
    source_dir: &impl AsRef<Path>,
    pseudo_crate_dir: &impl AsRef<Path>,
) -> Result<VersionMatch<CrateCollection>> {
    let mut source = CrateCollection::new(repo_root);
    source.add_from(source_dir, None::<&&str>)?;
    source.map_field_mut().retain(|_nv, krate| krate.is_crates_io());

    let pseudo_crate_dir_absolute = source.repo_root().join(pseudo_crate_dir);
    write_pseudo_crate(
        &pseudo_crate_dir_absolute,
        source
            .filter_versions(&most_recent_version)
            .filter(|(_nv, krate)| krate.is_migration_eligible())
            .map(|(_nv, krate)| krate),
    )?;

    let mut dest = CrateCollection::new(source.repo_root());
    dest.add_from(&pseudo_crate_dir.as_ref().join("android/vendor"), Some(pseudo_crate_dir))?;

    let mut version_match = VersionMatch::new(source, dest)?;

    version_match.copy_customizations()?;
    version_match.stage_crates()?;
    version_match.apply_patches()?;
    version_match.generate_android_bps()?;

    Ok(version_match)
}
