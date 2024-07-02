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
    collections::BTreeMap,
    env,
    path::Path,
    process::{Command, Output},
    str::from_utf8,
    sync::mpsc::channel,
};

use anyhow::{anyhow, Context, Result};
use threadpool::ThreadPool;

use crate::{Crate, NameAndVersion, NameAndVersionMap, NamedAndVersioned, RepoPath};

pub fn generate_android_bps<'a, T: Iterator<Item = &'a Crate>>(
    crates: T,
) -> Result<BTreeMap<NameAndVersion, Output>> {
    let pool = ThreadPool::new(std::cmp::max(num_cpus::get(), 32));
    let (tx, rx) = channel();

    let mut num_crates = 0;
    for krate in crates {
        num_crates += 1;
        let tx = tx.clone();
        let crate_name = krate.name().to_string();
        let crate_version = krate.version().clone();
        let staging_path = krate.staging_path();
        pool.execute(move || {
            tx.send((crate_name, crate_version, generate_android_bp(&staging_path)))
                .expect("Failed to send");
        });
    }
    let mut results = BTreeMap::new();
    for (crate_name, crate_version, result) in rx.iter().take(num_crates) {
        results.insert_or_error(NameAndVersion::new(crate_name, crate_version), result?)?;
    }
    Ok(results)
}

pub(crate) fn generate_android_bp(staging_path: &RepoPath) -> Result<Output> {
    let generate_android_bp_output = run_cargo_embargo(staging_path)?;
    if !generate_android_bp_output.status.success() {
        println!(
            "cargo_embargo failed for {}\nstdout:\n{}\nstderr:\n{}",
            staging_path,
            from_utf8(&generate_android_bp_output.stdout)?,
            from_utf8(&generate_android_bp_output.stderr)?
        );
    }
    Ok(generate_android_bp_output)
}

fn run_cargo_embargo(staging_path: &RepoPath) -> Result<Output> {
    // Make sure we can find bpfmt.
    let host_bin = staging_path.with_same_root(&"out/host/linux-x86/bin").abs();
    let new_path = match env::var_os("PATH") {
        Some(p) => {
            let mut paths = vec![host_bin];
            paths.extend(env::split_paths(&p));
            env::join_paths(paths)?
        }
        None => host_bin.as_os_str().into(),
    };

    let mut cmd =
        Command::new(staging_path.with_same_root(&"out/host/linux-x86/bin/cargo_embargo").abs());
    cmd.args(["generate", "cargo_embargo.json"])
        .env("PATH", new_path)
        .env("ANDROID_BUILD_TOP", staging_path.root())
        .current_dir(staging_path.abs())
        .output()
        .context(format!("Failed to execute {:?}", cmd.get_program()))
}

pub fn maybe_build_cargo_embargo(repo_root: &impl AsRef<Path>, force_rebuild: bool) -> Result<()> {
    if !force_rebuild
        && repo_root.as_ref().join("out/host/linux-x86/bin/cargo_embargo").exists()
        && repo_root.as_ref().join("out/host/linux-x86/bin/bpfmt").exists()
    {
        Ok(())
    } else {
        println!("Rebuilding cargo_embargo");
        build_cargo_embargo(repo_root)
    }
}

pub fn build_cargo_embargo(repo_root: &impl AsRef<Path>) -> Result<()> {
    let status = Command::new("/usr/bin/bash")
        .args(["-c", "source build/envsetup.sh && lunch aosp_cf_x86_64_phone-trunk_staging-eng && m cargo_embargo bpfmt"])
        .current_dir(repo_root).spawn().context("Failed to spawn build of cargo embargo and bpfmt")?.wait().context("Failed to wait on child process building cargo embargo and bpfmt")?;
    match status.success() {
        true => Ok(()),
        false => Err(anyhow!(
            "Building cargo embargo and bpfmt failed with exit code {}",
            status.code().map(|code| { format!("{}", code) }).unwrap_or("(unknown)".to_string())
        )),
    }
}
