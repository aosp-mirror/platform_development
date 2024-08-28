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

use std::env::current_dir;
use std::fs::{create_dir_all, remove_dir_all};
use std::path::{Path, PathBuf};
use std::process::{Command, Output};
use std::str::from_utf8;

use anyhow::{anyhow, Context, Result};
use semver::Version;
use thiserror::Error;

pub use self::crate_type::{diff_android_bp, Crate, Migratable};
mod crate_type;

pub use self::crate_collection::CrateCollection;
mod crate_collection;

mod migration;

pub use self::pseudo_crate::PseudoCrate;
mod pseudo_crate;

pub use self::version_match::{CompatibleVersionPair, VersionMatch, VersionPair};
mod version_match;

pub use self::android_bp::{
    build_cargo_embargo, cargo_embargo_autoconfig, generate_android_bps, maybe_build_cargo_embargo,
};
mod android_bp;

pub use self::name_and_version::{
    IsUpgradableTo, NameAndVersion, NameAndVersionRef, NamedAndVersioned,
};
mod name_and_version;

pub use self::repo_path::RepoPath;
mod repo_path;

pub use self::google_metadata::GoogleMetadata;
mod google_metadata;

pub use self::license::{most_restrictive_type, update_module_license_files};
mod license;

pub use self::managed_repo::ManagedRepo;
mod managed_repo;

#[cfg(test)]
pub use self::name_and_version_map::try_name_version_map_from_iter;
pub use self::name_and_version_map::{
    crates_with_multiple_versions, crates_with_single_version, most_recent_version,
    NameAndVersionMap,
};
mod name_and_version_map;

#[derive(Error, Debug)]
pub enum CrateError {
    #[error("Virtual crate: {0}")]
    VirtualCrate(PathBuf),

    #[error("Duplicate crate version: {0} {1}")]
    DuplicateCrateVersion(String, Version),
}

pub fn default_repo_root() -> Result<PathBuf> {
    let cwd = current_dir().context("Could not get current working directory")?;
    for cur in cwd.ancestors() {
        for e in cur.read_dir()? {
            if e?.file_name() == ".repo" {
                return Ok(cur.to_path_buf());
            }
        }
    }
    Err(anyhow!(".repo directory not found in any ancestor of {}", cwd.display()))
}

pub fn ensure_exists_and_empty(dir: &impl AsRef<Path>) -> Result<()> {
    let dir = dir.as_ref();
    if dir.exists() {
        remove_dir_all(&dir).context(format!("Failed to remove {}", dir.display()))?;
    }
    create_dir_all(&dir).context(format!("Failed to create {}", dir.display()))
}

pub trait RunQuiet {
    fn run_quiet_and_expect_success(&mut self) -> Result<Output>;
}
impl RunQuiet for Command {
    fn run_quiet_and_expect_success(&mut self) -> Result<Output> {
        let output = self.output().context(format!("Failed to run {:?}", self))?;
        if !output.status.success() {
            return Err(anyhow!(
                "Exit status {} for {:?}\nSTDOUT:\n{}\nSTDERR:\n{}",
                output
                    .status
                    .code()
                    .map(|code| { format!("{}", code) })
                    .unwrap_or("(unknown)".to_string()),
                self,
                from_utf8(&output.stdout)?,
                from_utf8(&output.stderr)?
            ));
        }
        Ok(output)
    }
}

// The copy_dir crate doesn't handle symlinks.
pub fn copy_dir(src: &impl AsRef<Path>, dst: &impl AsRef<Path>) -> Result<()> {
    Command::new("cp")
        .arg("--archive")
        .arg(src.as_ref())
        .arg(dst.as_ref())
        .run_quiet_and_expect_success()?;
    Ok(())
}

include!(concat!(env!("OUT_DIR"), "/protos/mod.rs"));
