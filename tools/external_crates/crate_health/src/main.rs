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

use std::path::PathBuf;

use anyhow::Result;
use clap::{Parser, Subcommand};
use crate_health::{default_repo_root, maybe_build_cargo_embargo, ManagedRepo, RepoPath};

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
}

#[derive(Subcommand)]
enum Cmd {
    /// Check the health of a crate, and whether it is safe to migrate.
    MigrationHealth {
        /// The crate name. Also the directory name in external/rust/crates.
        crate_name: String,
    },
    /// Migrate a crate from external/rust/crates to the monorepo.
    Migrate {
        /// The crate names. Also the directory names in external/rust/crates.
        crates: Vec<String>,
    },
    /// Regenerate a crate directory.
    Regenerate {
        /// The crate names.
        crates: Vec<String>,
    },
    /// Regenerate all crates
    RegenerateAll {},
    /// Run pre-upload checks.
    PreuploadCheck {
        /// List of changed files
        files: Vec<String>,
    },
}

fn main() -> Result<()> {
    let args = Cli::parse();

    maybe_build_cargo_embargo(&args.repo_root, args.rebuild_cargo_embargo)?;

    let managed_repo =
        ManagedRepo::new(RepoPath::new(args.repo_root, "external/rust/android-crates-io"));

    match args.command {
        Cmd::MigrationHealth { crate_name } => {
            managed_repo.migration_health(&crate_name, args.verbose)?;
            Ok(())
        }
        Cmd::Migrate { crates } => managed_repo.migrate(crates, args.verbose),
        Cmd::Regenerate { crates } => managed_repo.regenerate(crates.iter(), true),
        Cmd::RegenerateAll {} => managed_repo.regenerate_all(true),
        Cmd::PreuploadCheck { files: _ } => managed_repo.preupload_check(),
    }
}
