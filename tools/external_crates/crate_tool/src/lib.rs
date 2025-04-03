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

//! A command-line tool for managing repositories of 3rd party crates
//! such as external/rust/android-crates-io.

use std::env::current_dir;
use std::fs::{create_dir_all, remove_dir_all};
use std::path::{Path, PathBuf};
use std::process::Command;

use anyhow::{anyhow, Context, Result};
use success_or_error::RunAndExpectSuccess;
use thiserror::Error;

mod android_bp;
mod crate_collection;
mod crate_type;
mod crates_io;
mod managed_crate;
mod managed_repo;
mod patch;
mod pseudo_crate;
mod upgradable;

pub use self::android_bp::maybe_build_cargo_embargo;
pub use self::managed_repo::ManagedRepo;
pub use self::upgradable::SemverCompatibilityRule;

/// Error types for the 'crate_tool' crate. For the most part,
/// we use `anyhow`, but sometimes type information is useful.
#[derive(Error, Debug)]
pub enum CrateError {
    /// Virtual crate, usually a workspace.
    #[error("Virtual crate: {0}")]
    VirtualCrate(PathBuf),
}

/// Returns the absolute path to the root of the current Android source
/// repo, based on the current working directory.
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

fn ensure_exists_and_empty(dir: impl AsRef<Path>) -> Result<()> {
    let dir = dir.as_ref();
    if dir.exists() {
        remove_dir_all(dir).context(format!("Failed to remove {}", dir.display()))?;
    }
    create_dir_all(dir).context(format!("Failed to create {}", dir.display()))
}

// The copy_dir crate doesn't handle symlinks.
fn copy_dir(src: impl AsRef<Path>, dst: impl AsRef<Path>) -> Result<()> {
    Command::new("cp")
        .arg("--archive")
        .arg(src.as_ref())
        .arg(dst.as_ref())
        .run_quiet_and_expect_success()?;
    Ok(())
}
