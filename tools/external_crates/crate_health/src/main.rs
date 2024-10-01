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

use anyhow::Result;
use clap::{Parser, Subcommand};
use crate_health::{default_repo_root, maybe_build_cargo_embargo, ManagedRepo};
use rooted_path::RootedPath;

#[derive(Parser)]
struct Cli {
    #[command(subcommand)]
    command: Cmd,

    // The path to the Android source repo.
    #[arg(long, default_value_os_t=default_repo_root().unwrap_or(PathBuf::from(".")))]
    repo_root: PathBuf,

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
        /// The crate names. Also the directory names in external/rust/crates.
        crates: Vec<String>,

        /// Don't pin the crate version for the specified crates when checking health.
        #[arg(long, value_parser = parse_crate_list, required=false, default_value="")]
        unpinned: BTreeSet<String>,
    },
    /// Migrate crates from external/rust/crates to the monorepo.
    Migrate {
        /// The crate names. Also the directory names in external/rust/crates.
        crates: Vec<String>,

        /// Add the specified crates with unpinned versions.
        #[arg(long, value_parser = parse_crate_list, required=false, default_value="")]
        unpinned: BTreeSet<String>,
    },
    /// Import a crate and its dependencies into the monorepo.
    Import {
        /// The crate name.
        crate_name: String,
    },
    /// Regenerate crates from vendored code by applying patches, running cargo_embargo, etc.
    Regenerate {
        /// The crate names.
        crates: Vec<String>,

        /// Regenerate all crates.
        #[arg(long, default_value_t = false)]
        all: bool,
    },
    /// Run pre-upload checks.
    PreuploadCheck {
        /// List of changed files
        files: Vec<String>,
    },
    /// Try to fix problems with license files.
    FixLicenses {},
    /// Fix up METADATA files.
    FixMetadata {},
    /// Recontextualize patch files.
    RecontextualizePatches {
        /// The crate names. Also the directory names in external/rust/android-crates-io/crates.
        crates: Vec<String>,

        /// Recontextualize patches for all crates.
        #[arg(long, default_value_t = false)]
        all: bool,
    },
    /// Find crates with a newer version on crates.io
    UpdatableCrates {},
    /// Analyze possible updates for a crate and try to identify potential problems.
    AnalyzeUpdates { crate_name: String },
}

fn parse_crate_list(arg: &str) -> Result<BTreeSet<String>> {
    Ok(arg.split(',').map(|k| k.to_string()).collect())
}

fn main() -> Result<()> {
    let args = Cli::parse();

    maybe_build_cargo_embargo(&args.repo_root, args.rebuild_cargo_embargo)?;

    let managed_repo = ManagedRepo::new(
        RootedPath::new(args.repo_root, "external/rust/android-crates-io")?,
        args.offline,
    )?;

    match args.command {
        Cmd::MigrationHealth { crates, unpinned } => {
            for crate_name in &crates {
                if let Err(e) = managed_repo.migration_health(
                    crate_name,
                    args.verbose,
                    unpinned.contains(crate_name),
                ) {
                    println!("Crate {} error: {}", crate_name, e);
                }
            }
            Ok(())
        }
        Cmd::Migrate { crates, unpinned } => managed_repo.migrate(crates, args.verbose, &unpinned),
        Cmd::Regenerate { crates, all } => managed_repo.regenerate(
            if all { managed_repo.all_crate_names()?.into_iter() } else { crates.into_iter() },
            true,
        ),
        Cmd::PreuploadCheck { files } => managed_repo.preupload_check(&files),
        Cmd::Import { crate_name } => managed_repo.import(&crate_name),
        Cmd::FixLicenses {} => managed_repo.fix_licenses(),
        Cmd::FixMetadata {} => managed_repo.fix_metadata(),
        Cmd::RecontextualizePatches { crates, all } => {
            managed_repo.recontextualize_patches(if all {
                managed_repo.all_crate_names()?.into_iter()
            } else {
                crates.into_iter()
            })
        }
        Cmd::UpdatableCrates {} => managed_repo.updatable_crates(),
        Cmd::AnalyzeUpdates { crate_name } => managed_repo.analyze_updates(crate_name),
    }
}
