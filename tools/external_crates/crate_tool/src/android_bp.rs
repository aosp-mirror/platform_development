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
    env,
    ffi::OsString,
    fs::{remove_file, rename},
    path::Path,
    process::{Command, Output},
    time::{Duration, SystemTime},
};

use anyhow::{Context, Result};
use rooted_path::RootedPath;

use crate::SuccessOrError;

fn add_bpfmt_to_path(repo_root: impl AsRef<Path>) -> Result<OsString> {
    let host_bin = repo_root.as_ref().join("prebuilts/build-tools/linux-x86/bin");
    let new_path = match env::var_os("PATH") {
        Some(p) => {
            let mut paths = vec![host_bin];
            paths.extend(env::split_paths(&p));
            env::join_paths(paths)?
        }
        None => host_bin.as_os_str().into(),
    };
    Ok(new_path)
}

pub fn run_cargo_embargo(temporary_build_path: &RootedPath) -> Result<Output> {
    maybe_build_cargo_embargo(&temporary_build_path.root(), false)?;
    let new_path = add_bpfmt_to_path(temporary_build_path.root())?;

    let cargo_lock = temporary_build_path.join("Cargo.lock")?;
    let saved_cargo_lock = temporary_build_path.join("Cargo.lock.saved")?;
    if cargo_lock.abs().exists() {
        rename(&cargo_lock, &saved_cargo_lock)?;
    }

    let mut cmd = Command::new(
        temporary_build_path.with_same_root("out/host/linux-x86/bin/cargo_embargo")?.abs(),
    );
    let output = cmd
        .args(["generate", "cargo_embargo.json"])
        .env("PATH", new_path)
        .env("ANDROID_BUILD_TOP", temporary_build_path.root())
        .env_remove("OUT_DIR")
        .current_dir(temporary_build_path)
        .output()
        .context(format!("Failed to execute {:?}", cmd.get_program()))?;

    if cargo_lock.abs().exists() {
        remove_file(&cargo_lock)?;
    }
    if saved_cargo_lock.abs().exists() {
        rename(saved_cargo_lock, cargo_lock)?;
    }

    Ok(output)
}

pub fn cargo_embargo_autoconfig(path: &RootedPath) -> Result<Output> {
    maybe_build_cargo_embargo(&path.root(), false)?;
    let new_path = add_bpfmt_to_path(path.root())?;

    let mut cmd = Command::new(path.with_same_root("out/host/linux-x86/bin/cargo_embargo")?.abs());
    cmd.args(["autoconfig", "cargo_embargo.json"])
        .env("PATH", new_path)
        .env("ANDROID_BUILD_TOP", path.root())
        .env_remove("OUT_DIR")
        .current_dir(path)
        .output()
        .context(format!("Failed to execute {:?}", cmd.get_program()))
}

pub fn maybe_build_cargo_embargo(repo_root: &impl AsRef<Path>, force_rebuild: bool) -> Result<()> {
    let cargo_embargo = repo_root.as_ref().join("out/host/linux-x86/bin/cargo_embargo");
    if force_rebuild
        || !cargo_embargo.exists()
        || SystemTime::now().duration_since(cargo_embargo.metadata()?.modified()?)?
            > Duration::from_secs(14 * 24 * 60 * 60)
    {
        println!("Rebuilding cargo_embargo");
        return build_cargo_embargo(repo_root);
    }
    Ok(())
}

pub fn build_cargo_embargo(repo_root: &impl AsRef<Path>) -> Result<()> {
    Command::new("/usr/bin/bash")
        .args(["-c", "source build/envsetup.sh && lunch aosp_cf_x86_64_phone-trunk_staging-eng && m cargo_embargo"])
        .env_remove("OUT_DIR")
        .current_dir(repo_root)
        .spawn().context("Failed to spawn build of cargo embargo")?
        .wait().context("Failed to wait on child process building cargo embargo")?
        .success_or_error().context("Failed to build_cargo_embargo")?;
    Ok(())
}
