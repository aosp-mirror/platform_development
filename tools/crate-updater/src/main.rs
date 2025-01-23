// Copyright (C) 2025 The Android Open Source Project
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
    collections::BTreeSet,
    path::{Path, PathBuf},
    process::{Command, ExitStatus, Output},
    str::from_utf8,
    sync::LazyLock,
};

use anyhow::{bail, Result};
use chrono::Datelike;
use clap::Parser;
use crate_updater::UpdatesTried;
use rand::seq::SliceRandom;
use rand::thread_rng;

#[derive(Parser)]
struct Cli {
    /// Absolute path to a repo checkout of aosp-main.
    /// It is strongly recommended that you use a source tree dedicated to
    /// running this updater.
    android_root: PathBuf,
}

pub trait SuccessOrError {
    fn success_or_error(self) -> Result<Self>
    where
        Self: std::marker::Sized;
}
impl SuccessOrError for ExitStatus {
    fn success_or_error(self) -> Result<Self> {
        if !self.success() {
            let exit_code =
                self.code().map(|code| format!("{code}")).unwrap_or("(unknown)".to_string());
            bail!("Process failed with exit code {exit_code}");
        }
        Ok(self)
    }
}
impl SuccessOrError for Output {
    fn success_or_error(self) -> Result<Self> {
        (&self).success_or_error()?;
        Ok(self)
    }
}
impl SuccessOrError for &Output {
    fn success_or_error(self) -> Result<Self> {
        if !self.status.success() {
            let exit_code =
                self.status.code().map(|code| format!("{code}")).unwrap_or("(unknown)".to_string());
            bail!(
                "Process failed with exit code {}\nstdout:\n{}\nstderr:\n{}",
                exit_code,
                from_utf8(&self.stdout)?,
                from_utf8(&self.stderr)?
            );
        }
        Ok(self)
    }
}

pub trait RunAndStreamOutput {
    fn run_and_stream_output(&mut self) -> Result<ExitStatus>;
}
impl RunAndStreamOutput for Command {
    fn run_and_stream_output(&mut self) -> Result<ExitStatus> {
        self.spawn()?.wait()?.success_or_error()
    }
}

fn cleanup_and_sync_monorepo(monorepo_path: &Path) -> Result<()> {
    Command::new("git")
        .args(["restore", "--staged", "."])
        .current_dir(monorepo_path)
        .run_and_stream_output()?;
    Command::new("git")
        .args(["restore", "."])
        .current_dir(monorepo_path)
        .run_and_stream_output()?;

    Command::new("git")
        .args(["clean", "-f", "-d"])
        .current_dir(monorepo_path)
        .run_and_stream_output()?;

    Command::new("git")
        .args(["checkout", "aosp/main"])
        .current_dir(monorepo_path)
        .run_and_stream_output()?;

    let output = Command::new("git")
        .args(["status", "--porcelain", "."])
        .current_dir(monorepo_path)
        .output()?
        .success_or_error()?;
    if !output.stdout.is_empty() {
        bail!("Monorepo {} has uncommitted changes", monorepo_path.display());
    }

    Command::new("repo").args(["sync", "."]).current_dir(monorepo_path).run_and_stream_output()?;

    Ok(())
}

fn sync_to_green(monorepo_path: &Path) -> Result<()> {
    Command::new("prodcertstatus").run_and_stream_output()?;
    Command::new("/google/data/ro/projects/android/smartsync_login").run_and_stream_output()?;

    let output = Command::new("/google/data/ro/projects/android/ab")
        .args([
            "lkgb",
            "--branch=aosp-main",
            "--target=aosp_arm64-trunk_staging-userdebug",
            "--raw",
            "--custom_raw_format={o[buildId]}",
        ])
        .output()?
        .success_or_error()?;
    let bid = from_utf8(&output.stdout)?.trim();
    println!("bid = {bid}");

    Command::new("/google/data/ro/projects/android/smartsync_repo")
        .args(["sync", "-j99", "-t", bid])
        .current_dir(monorepo_path)
        .run_and_stream_output()?;

    // Even though we sync the rest of the repository to a green build,
    // we sync the monorepo to tip-of-tree, which reduces merge conflicts
    // and duplicate update CLs.
    Command::new("repo").args(["sync", "."]).current_dir(monorepo_path).run_and_stream_output()?;

    Ok(())
}

fn get_suggestions(monorepo_path: &Path) -> Result<Vec<(String, String)>> {
    // TODO: Improve update suggestion algorithm, and produce output in machine-readable format.
    let mut suggestions = Vec::new();
    for compatibility in ["ignore", "loose", "strict"] {
        let output = Command::new(monorepo_path.join("crate_tool"))
            .args(["suggest-updates", "--patches", "--semver-compatibility", compatibility])
            .current_dir(monorepo_path)
            .output()?
            .success_or_error()?;
        suggestions.extend(from_utf8(&output.stdout)?.trim().lines().map(|suggestion| {
            let words = suggestion.split_whitespace().collect::<Vec<_>>();
            if words.len() != 6 {
                println!("Failed to parse suggestion {suggestion}");
            }
            (words[2].to_string(), words[5].to_string())
        }));
    }

    // Return suggestions in random order. This reduces merge conflicts and ensures
    // all crates eventually get tried, even if something goes wrong and the program
    // terminates prematurely.
    let mut rng = thread_rng();
    suggestions.shuffle(&mut rng);

    Ok(suggestions)
}

fn try_update(
    android_root: &Path,
    monorepo_path: &Path,
    crate_name: &str,
    version: &str,
) -> Result<()> {
    println!("Trying to update {crate_name} to {version}");

    Command::new(monorepo_path.join("crate_tool"))
        .args(["update", crate_name, version])
        .current_dir(monorepo_path)
        .run_and_stream_output()?;

    if Command::new("git")
        .args(["diff", "--exit-code", "pseudo_crate/Cargo.toml"])
        .current_dir(monorepo_path)
        .run_and_stream_output()
        .is_ok()
    {
        bail!("Crate {crate_name} was already updated");
    }

    Command::new("/usr/bin/bash")
        .args([
            "-c",
            format!(
                "source {}/build/envsetup.sh && lunch aosp_husky-trunk_staging-eng && mm && m rust",
                android_root.display()
            )
            .as_str(),
        ])
        .env_remove("OUT_DIR")
        .current_dir(monorepo_path)
        .run_and_stream_output()?;

    let now = chrono::Utc::now();
    Command::new("repo")
        .args([
            "start",
            format!(
                "automatic-crate-update-{}-{}-{}-{}-{}",
                crate_name,
                version,
                now.year(),
                now.month(),
                now.day()
            )
            .as_str(),
        ])
        .current_dir(monorepo_path)
        .run_and_stream_output()?;
    Command::new("git").args(["add", "."]).current_dir(monorepo_path).run_and_stream_output()?;
    Command::new("git")
        .args([
            "commit",
            "-m",
            format!("Update {crate_name} to {version}\n\nTest: m rust").as_str(),
        ])
        .current_dir(monorepo_path)
        .run_and_stream_output()?;
    Command::new("repo")
        .args([
            "upload",
            "-c",
            ".",
            "-y",
            "-o",
            "banned-words~skip",
            "-o",
            "nokeycheck",
            "--label",
            "Presubmit-Ready+1",
            "--label",
            "Autosubmit+1",
            "-o",
            "t=automatic-crate-updates",
        ])
        .current_dir(monorepo_path)
        .run_and_stream_output()?;

    Ok(())
}

#[rustfmt::skip]
static DENYLIST: LazyLock<BTreeSet<&'static str>> = LazyLock::new(|| {
    BTreeSet::from([
        // Paired crates that need to be updated together
        "async-stream",
        "async-stream-impl",
        "clap",
        "clap_builder",
        "linkme",
        "linkme-impl",
        "pin-project",
        "pin-project-internal",
        "protobuf-support",
        "protobuf",
        "ptr_meta",
        "ptr_meta_derive",
        "regex",
        "regex-automata",
        "regex-syntax",
        "serde_derive",
        "serde",
        "thiserror-impl",
        "thiserror",
        "tikv-jemalloc-sys",
        "tikv-jemallocator",
        "zerocopy-derive",
        "zerocopy",
    ])
});

fn main() -> Result<()> {
    let args = Cli::parse();
    if !args.android_root.is_absolute() {
        bail!("Must be an absolute path: {}", args.android_root.display());
    }
    if !args.android_root.is_dir() {
        bail!("Does not exist, or is not a directory: {}", args.android_root.display());
    }
    let monorepo_path = args.android_root.join("external/rust/android-crates-io");

    cleanup_and_sync_monorepo(&monorepo_path)?;

    sync_to_green(&monorepo_path)?;

    Command::new("/usr/bin/bash")
        .args([
            "-c",
            "source build/envsetup.sh && lunch aosp_husky-trunk_staging-eng && m cargo_embargo",
        ])
        .env_remove("OUT_DIR")
        .current_dir(&args.android_root)
        .run_and_stream_output()?;

    let mut updates_tried = UpdatesTried::read()?;
    for (crate_name, version) in get_suggestions(&monorepo_path)?.iter() {
        if DENYLIST.contains(crate_name.as_str()) {
            println!("Skipping {crate_name} (on deny list)");
            continue;
        }
        if updates_tried.contains(crate_name, version) {
            println!("Skipping {crate_name} (already attempted recently)");
            continue;
        }
        cleanup_and_sync_monorepo(&monorepo_path)?;
        let res =
            try_update(&args.android_root, &monorepo_path, crate_name.as_str(), version.as_str())
                .inspect_err(|e| println!("Update failed: {}", e));
        updates_tried.record(crate_name.clone(), version.clone(), res.is_ok())?;
    }
    cleanup_and_sync_monorepo(&monorepo_path)?;

    Ok(())
}
