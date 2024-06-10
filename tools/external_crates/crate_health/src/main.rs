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

use std::{path::PathBuf, process::Command, str::from_utf8};

use anyhow::{anyhow, Result};
use clap::{Parser, Subcommand};
use crate_health::{
    default_repo_root, maybe_build_cargo_embargo, migrate, CrateCollection, Migratable,
    NameAndVersionMap, NamedAndVersioned, RepoPath,
};

#[derive(Parser)]
struct Cli {
    #[command(subcommand)]
    command: Cmd,

    #[arg(long, default_value_os_t=default_repo_root().unwrap_or(PathBuf::from(".")))]
    repo_root: PathBuf,

    /// Rebuild cargo_embargo and bpfmt, even if they are already present in the out directory.
    #[arg(long, default_value_t = false)]
    rebuild_cargo_embargo: bool,
}

#[derive(Subcommand)]
enum Cmd {
    /// Check the health of a crate, and whether it is safe to migrate.
    MigrationHealth {
        /// The crate name. Also the directory name in external/rust/crates
        crate_name: String,
    },
    /// Migrate a crate from external/rust/crates to the monorepo.
    Migrate {
        /// The crate name. Also the directory name in external/rust/crates
        crate_name: String,
    },
    Regenerate {
        /// The crate name.
        crate_name: String,
    },
    /// Run pre-upload checks.
    PreuploadCheck {
        /// List of changed files
        files: Vec<String>,
    },
}

static IGNORED_FILES: &'static [&'static str] = &[
    ".appveyor.yml",
    ".bazelci",
    ".bazelignore",
    ".bazelrc",
    ".bazelversion",
    ".buildkite",
    ".cargo",
    ".cargo-checksum.json",
    ".cargo_vcs_info.json",
    ".circleci",
    ".cirrus.yml",
    ".clang-format",
    ".clang-tidy",
    ".clippy.toml",
    ".clog.toml",
    ".clog.toml",
    ".codecov.yaml",
    ".codecov.yml",
    ".editorconfig",
    ".gcloudignore",
    ".gdbinit",
    ".git",
    ".git-blame-ignore-revs",
    ".git-ignore-revs",
    ".gitallowed",
    ".gitattributes",
    ".github",
    ".gitignore",
    ".idea",
    ".ignore",
    ".istanbul.yml",
    ".mailmap",
    ".md-inc.toml",
    ".mdl-style.rb",
    ".mdlrc",
    ".pylintrc",
    ".pylintrc-examples",
    ".pylintrc-tests",
    ".reuse",
    ".rspec",
    ".rustfmt.toml",
    ".shellcheckrc",
    ".standard-version",
    ".tarpaulin.toml",
    ".tokeignore",
    ".travis.yml",
    ".versionrc",
    ".vim",
    ".vscode",
    ".yapfignore",
    ".yardopts",
    "BUILD",
    "Cargo.lock",
    "Cargo.lock.saved",
    "Cargo.toml.orig",
    "OWNERS",
    // rules.mk related files that we won't migrate.
    "cargo2rulesmk.json",
    "CleanSpec.mk",
    "rules.mk",
    // cargo_embargo intermediates.
    "Android.bp.orig",
    "cargo.metadata",
    "cargo.out",
    "target.tmp",
];

fn main() -> Result<()> {
    let args = Cli::parse();

    maybe_build_cargo_embargo(&args.repo_root, args.rebuild_cargo_embargo)?;

    match args.command {
        Cmd::MigrationHealth { crate_name } => {
            if args
                .repo_root
                .join("external/rust/android-crates-io/crates")
                .join(&crate_name)
                .exists()
            {
                return Err(anyhow!(
                    "Crate {} already exists in external/rust/android-crates-io/crates",
                    crate_name
                ));
            }

            let mut cc = CrateCollection::new(&args.repo_root);
            cc.add_from(&PathBuf::from("external/rust/crates").join(&crate_name))?;
            cc.map_field_mut().retain(|_nv, krate| krate.is_crates_io());
            if cc.map_field().len() != 1 {
                return Err(anyhow!(
                    "Expected a single crate version for {}, but found {}. Crates with multiple versions are not supported yet.",
                    crate_name,
                    cc.map_field().len()
                ));
            }

            cc.stage_crates()?;
            cc.generate_android_bps()?;
            cc.diff_android_bps()?;

            let krate = cc.map_field().values().next().unwrap();
            println!("Found {} v{} in {}", krate.name(), krate.version(), krate.path());
            let migratable;
            if !krate.is_android_bp_healthy() {
                if krate.is_migration_denied() {
                    println!("This crate is on the migration denylist");
                }
                if !krate.android_bp().abs().exists() {
                    println!("There is no Android.bp file in {}", krate.path());
                }
                if !krate.cargo_embargo_json().abs().exists() {
                    println!("There is no cargo_embargo.json file in {}", krate.path());
                } else if !krate.generate_android_bp_success() {
                    println!("cargo_embargo execution did not succeed for {}", krate.path());
                } else if !krate.android_bp_unchanged() {
                    println!(
                        "Running cargo_embargo on {} produced changes to the Android.bp file:",
                        krate.path()
                    );
                    println!(
                        "{}",
                        from_utf8(
                            &krate
                                .android_bp_diff()
                                .ok_or(anyhow!("No Android.bp diff found"))?
                                .stdout
                        )?
                    );
                }
                migratable = false;
            } else {
                let migration = migrate(
                    RepoPath::new(
                        args.repo_root.clone(),
                        PathBuf::from("external/rust/crates").join(&crate_name),
                    ),
                    RepoPath::new(args.repo_root.clone(), &"out/rust-crate-migration-report"),
                )?;
                let compatible_pairs = migration.compatible_pairs().collect::<Vec<_>>();
                if compatible_pairs.len() != 1 {
                    return Err(anyhow!("Couldn't find a compatible version to migrate to",));
                }
                let pair = compatible_pairs.first().unwrap();
                if pair.source.version() != pair.dest.version() {
                    println!(
                        "Source and destination versions are different: {} -> {}",
                        pair.source.version(),
                        pair.dest.version()
                    );
                }
                if !pair.dest.is_migratable() {
                    if !pair.dest.patch_success() {
                        println!("Patches did not apply successfully to the migrated crate");
                        // TODO: Show errors.
                    }
                    if !pair.dest.generate_android_bp_success() {
                        println!("cargo_embargo execution did not succeed for the migrated crate");
                    } else if pair.dest.android_bp_unchanged() {
                        println!("Running cargo_embargo for the migrated crate produced changes to the Android.bp file:");
                        println!(
                            "{}",
                            from_utf8(
                                &pair
                                    .dest
                                    .android_bp_diff()
                                    .ok_or(anyhow!("No Android.bp diff found"))?
                                    .stdout
                            )?
                        );
                    }
                }

                let diff_status = Command::new("diff")
                    .args(["-u", "-r", "-w", "--no-dereference"])
                    .args(IGNORED_FILES.iter().map(|ignored| format!("--exclude={}", ignored)))
                    .arg(pair.source.path().rel())
                    .arg(pair.dest.staging_path().rel())
                    .current_dir(&args.repo_root)
                    .spawn()?
                    .wait()?;
                if !diff_status.success() {
                    println!(
                        "Found differences between {} and {}",
                        pair.source.path(),
                        pair.dest.staging_path()
                    );
                }
                println!("All diffs:");
                Command::new("diff")
                    .args(["-u", "-r", "-w", "-q", "--no-dereference"])
                    .arg(pair.source.path().rel())
                    .arg(pair.dest.staging_path().rel())
                    .current_dir(&args.repo_root)
                    .spawn()?
                    .wait()?;

                migratable = pair.dest.is_migratable() && diff_status.success()
            }

            println!(
                "The crate is {}",
                if krate.is_android_bp_healthy() && migratable { "healthy" } else { "UNHEALTHY" }
            );
        }
        Cmd::Migrate { crate_name: _ } => todo!(),
        Cmd::Regenerate { crate_name: _ } => todo!(),
        Cmd::PreuploadCheck { files: _ } => todo!(),
    }

    Ok(())
}
