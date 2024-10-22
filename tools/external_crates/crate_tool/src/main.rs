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
    collections::{BTreeMap, BTreeSet},
    path::PathBuf,
};

use anyhow::Result;
use clap::{Args, Parser, Subcommand};
use crate_tool::{default_repo_root, maybe_build_cargo_embargo, ManagedRepo};
use rooted_path::RootedPath;
use semver::Version;

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
    /// Check the health of a crate, and whether it is safe to migrate.
    MigrationHealth {
        #[command(flatten)]
        crates: MigrationCrateList,
    },
    /// Migrate crates from external/rust/crates to the monorepo.
    Migrate {
        #[command(flatten)]
        crates: MigrationCrateList,
    },
    /// Import a crate and its dependencies into the monorepo.
    #[command(hide = true)]
    Import {
        /// The crate name.
        crate_name: String,
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
    },
    /// Update a crate to the specified version.
    Update {
        /// The crate name.
        crate_name: String,

        /// The crate version.
        version: String,
    },
    /// Try suggested crate updates and see which ones succeed.
    ///
    /// Take about 15 minutes per crate, so suggested use is to tee to a file and let it run overnight:
    ///
    /// ./android_cargo.py run --bin crate_tool -- try-updates | tee crate-updates
    TryUpdates {},
    /// Initialize a new managed repo.
    Init {},
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
            managed_repo
                .all_crate_names()?
                .into_iter()
                .filter(|crate_name| !self.exclude.contains(crate_name))
                .collect::<Vec<_>>()
        } else {
            self.crates.clone()
        })
    }
    pub fn parse_crate_list(arg: &str) -> Result<BTreeSet<String>> {
        Ok(arg.split(',').map(|k| k.to_string()).collect())
    }
}

#[derive(Args)]
struct MigrationCrateList {
    /// The crate names. Also the directory names in external/rust/crates.
    crates: Vec<String>,

    /// Don't pin the crate version for the specified crates when checking health or migrating.
    /// Specified as a comma-separated list of crate names.
    #[arg(long, value_parser = CrateList::parse_crate_list, required=false, default_value="")]
    unpinned: BTreeSet<String>,

    /// For crates with multiple versions, use the specified version.
    /// Specified as a comma-separated list of <crate_name>=<version>.
    #[arg(long, value_parser = MigrationCrateList::parse_crate_versions, required=false, default_value="")]
    versions: BTreeMap<String, Version>,
}

impl MigrationCrateList {
    pub fn parse_crate_versions(arg: &str) -> Result<BTreeMap<String, Version>> {
        Ok(arg
            .split(',')
            .filter(|k| !k.is_empty())
            .map(|nv| nv.split_once("=").unwrap())
            .map(|(crate_name, version)| (crate_name.to_string(), Version::parse(version).unwrap()))
            .collect())
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
        Cmd::MigrationHealth { crates } => {
            for crate_name in &crates.crates {
                if let Err(e) = managed_repo.migration_health(
                    crate_name,
                    args.verbose,
                    crates.unpinned.contains(crate_name),
                    crates.versions.get(crate_name),
                ) {
                    println!("Crate {} error: {}", crate_name, e);
                }
            }
            Ok(())
        }
        Cmd::Migrate { crates } => {
            managed_repo.migrate(crates.crates, args.verbose, &crates.unpinned, &crates.versions)
        }
        Cmd::Regenerate { crates } => {
            managed_repo.regenerate(crates.to_list(&managed_repo)?.into_iter(), true)
        }
        Cmd::PreuploadCheck { files } => managed_repo.preupload_check(&files),
        Cmd::Import { crate_name } => managed_repo.import(&crate_name),
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
        Cmd::SuggestUpdates { patches } => managed_repo.suggest_updates(patches).map(|_x| ()),
        Cmd::Update { crate_name, version } => managed_repo.update(crate_name, version),
        Cmd::TryUpdates {} => managed_repo.try_updates(),
        Cmd::Init {} => managed_repo.init(),
    }
}
