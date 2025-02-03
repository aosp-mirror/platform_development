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

use std::{collections::BTreeSet, path::PathBuf};

use anyhow::{bail, Result};
use clap::{Args, Parser, Subcommand};
use crate_tool::{
    default_repo_root, maybe_build_cargo_embargo, ManagedRepo, SemverCompatibilityRule,
};
use rooted_path::RootedPath;

#[derive(Parser)]
struct Cli {
    #[command(subcommand)]
    command: Cmd,

    // The path to the Android source repo.
    #[arg(long, default_value_os_t=default_repo_root().unwrap_or(PathBuf::from(".")))]
    android_root: PathBuf,

    // The path to the crate monorepo, relative to the Android source repo.
    #[arg(long, default_value_os_t=PathBuf::from("external/rust/android-crates-io"))]
    managed_repo_path: PathBuf,

    /// Rebuild cargo_embargo and bpfmt, even if they are already present in the out directory.
    #[arg(long, default_value_t = false)]
    rebuild_cargo_embargo: bool,

    /// Print command output in case of error, and full diffs.
    #[arg(long, default_value_t = false)]
    verbose: bool,

    /// Don't make network requests to crates.io. Only check the local cache.
    #[arg(long, default_value_t = false)]
    offline: bool,
}

#[derive(Subcommand)]
enum Cmd {
    /// Analyze a crate to see if it can be imported.
    #[command(hide = true)]
    AnalyzeImport {
        /// The crate name.
        crate_name: String,
    },
    /// Import a crate and its dependencies into the monorepo.
    #[command(hide = true)]
    Import {
        /// The crate name.
        crate_name: String,

        /// The crate version.
        version: String,

        /// Run "cargo_embargo autoconfig"
        #[arg(long, default_value_t = false)]
        autoconfig: bool,
    },
    /// Regenerate crates from vendored code by applying patches, running cargo_embargo, etc.
    Regenerate {
        #[command(flatten)]
        crates: CrateList,
    },
    /// Run pre-upload checks.
    PreuploadCheck {
        /// List of changed files
        files: Vec<String>,
    },
    /// Fix problems with license files.
    FixLicenses {
        #[command(flatten)]
        crates: CrateList,
    },
    /// Fix up METADATA files.
    FixMetadata {
        #[command(flatten)]
        crates: CrateList,
    },
    /// Recontextualize patch files.
    RecontextualizePatches {
        #[command(flatten)]
        crates: CrateList,
    },
    /// Find crates with a newer version on crates.io
    UpdatableCrates {},
    /// Analyze possible updates for a crate and try to identify potential problems.
    AnalyzeUpdates { crate_name: String },
    /// Suggest crate updates.
    SuggestUpdates {
        /// Don't exclude crates that have patches.
        #[arg(long, default_value_t = false)]
        patches: bool,

        /// How strict to be about enforcing semver compatibility.
        #[arg(long, value_enum, default_value_t = SemverCompatibilityRule::Loose)]
        semver_compatibility: SemverCompatibilityRule,

        #[arg(long, default_value_t = false)]
        json: bool,
    },
    /// Update a crate to the specified version.
    Update {
        /// The crate name.
        crate_name: String,

        /// The crate version.
        version: String,
    },
    /// Initialize a new managed repo.
    Init {},
    /// Update TEST_MAPPING files.
    #[command(hide = true)]
    TestMapping {
        #[command(flatten)]
        crates: CrateList,
    },
    /// Verify checksums for a crate.
    VerifyChecksum {
        #[command(flatten)]
        crates: CrateList,
    },
}

#[derive(Args)]
struct CrateList {
    /// The crate names.
    crates: Vec<String>,

    /// All crates.
    #[arg(long, default_value_t = false)]
    all: bool,

    /// Comma-separated list of crates to exclude from --all.
    #[arg(long, value_parser = CrateList::parse_crate_list, required=false, default_value="")]
    exclude: BTreeSet<String>,
}

impl CrateList {
    fn to_list(&self, managed_repo: &ManagedRepo) -> Result<Vec<String>> {
        Ok(if self.all {
            managed_repo.all_crate_names()?.difference(&self.exclude).cloned().collect::<Vec<_>>()
        } else {
            if self.crates.is_empty() {
                bail!("No crates specified");
            }
            self.crates.clone()
        })
    }
    pub fn parse_crate_list(arg: &str) -> Result<BTreeSet<String>> {
        Ok(arg.split(',').map(|k| k.to_string()).collect())
    }
}

fn main() -> Result<()> {
    let args = Cli::parse();

    maybe_build_cargo_embargo(&args.android_root, args.rebuild_cargo_embargo)?;

    let managed_repo = ManagedRepo::new(
        RootedPath::new(args.android_root, args.managed_repo_path)?,
        args.offline,
    )?;

    match args.command {
        Cmd::Regenerate { crates } => {
            managed_repo.regenerate(crates.to_list(&managed_repo)?.into_iter())
        }
        Cmd::PreuploadCheck { files } => managed_repo.preupload_check(&files),
        Cmd::AnalyzeImport { crate_name } => managed_repo.analyze_import(&crate_name),
        Cmd::Import { crate_name, version, autoconfig } => {
            managed_repo.import(&crate_name, &version, autoconfig)
        }
        Cmd::FixLicenses { crates } => {
            managed_repo.fix_licenses(crates.to_list(&managed_repo)?.into_iter())
        }
        Cmd::FixMetadata { crates } => {
            managed_repo.fix_metadata(crates.to_list(&managed_repo)?.into_iter())
        }
        Cmd::RecontextualizePatches { crates } => {
            managed_repo.recontextualize_patches(crates.to_list(&managed_repo)?.into_iter())
        }
        Cmd::UpdatableCrates {} => managed_repo.updatable_crates(),
        Cmd::AnalyzeUpdates { crate_name } => managed_repo.analyze_updates(crate_name),
        Cmd::SuggestUpdates { patches, semver_compatibility, json } => {
            managed_repo.suggest_updates(patches, semver_compatibility, json)
        }
        Cmd::Update { crate_name, version } => managed_repo.update(crate_name, version),
        Cmd::Init {} => managed_repo.init(),
        Cmd::TestMapping { crates } => {
            managed_repo.fix_test_mapping(crates.to_list(&managed_repo)?.into_iter())
        }
        Cmd::VerifyChecksum { crates } => {
            managed_repo.verify_checksums(crates.to_list(&managed_repo)?.into_iter())
        }
    }
}
