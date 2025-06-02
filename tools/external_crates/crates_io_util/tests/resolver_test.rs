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

// This regression test compares the dependency resolver with the result of
// running `cargo tree` on all the crates in external/rust/android-crates-io.
//
// It takes several minutes to run, and can't run as a presubmit test because
// it requires network access. It is therefore marked [ignore].
//
// To run this test manually:
//
// ./android_cargo.py test -p crates_io_util --test resolver_test  -- --nocapture --ignored
//
// For additional information about what the resolver is doing, prepend "RUST_LOG=debug" to
// the command.
//
// To specify which crates to test, prepend "CRATES_TO_TEST=crate1,crate2" to the command.

use std::{collections::BTreeSet, env, path::PathBuf, process::Command};

use android_bp::BluePrint;
use anyhow::{anyhow, Result};
use bp_util::CrateFeatures;
use crate_tool::{default_repo_root, ManagedRepo};
use crates_io_util::{AndroidTarget, CratesIoIndex, FeatureResolver, GetVersion};
use itertools::Itertools;
use rooted_path::RootedPath;
use success_or_error::RunAndExpectSuccess;
use tempfile::TempDir;

#[derive(Debug)]
struct CrateDownloader {
    tempdir: TempDir,
}

impl CrateDownloader {
    fn new() -> Result<Self> {
        let tempdir = tempfile::tempdir()?;
        Command::new("cargo")
            .args(["init", "--lib", "--name", "fake-crate"])
            .current_dir(&tempdir)
            .run_quiet_and_expect_success()?;
        Ok(CrateDownloader { tempdir })
    }
    fn download(&self, crate_name: &str, version: &semver::Version) -> Result<PathBuf> {
        // This will fail for yanked crates.
        Command::new("cargo")
            .arg("add")
            .arg(format!("{crate_name}@={version}"))
            .current_dir(&self.tempdir)
            .run_quiet_and_expect_success()?;
        Command::new("cargo")
            .arg("vendor")
            .current_dir(&self.tempdir)
            .run_quiet_and_expect_success()?;
        Command::new("cargo")
            .args(["remove", crate_name])
            .current_dir(&self.tempdir)
            .run_quiet_and_expect_success()?;
        Ok(self.tempdir.path().join("vendor").join(crate_name))
    }
}

#[derive(Debug, PartialEq, Eq)]
struct Deps {
    required: BTreeSet<String>,
    optional: BTreeSet<String>,
}

struct CargoTreeResolver {
    downloader: CrateDownloader,
}

impl CargoTreeResolver {
    fn new() -> Result<Self> {
        Ok(CargoTreeResolver { downloader: CrateDownloader::new()? })
    }
    fn resolve(
        &self,
        crate_name: &str,
        version: &semver::Version,
        features: &Option<Vec<&str>>,
    ) -> Result<Deps> {
        static COMMON_ARGS: &[&str] = &[
            "tree",
            "--depth=1",
            "--prefix=depth",
            "--edges=normal",
            "--target=aarch64-linux-android",
            "--target=x86_64-unknown-linux-gnu",
            "--target=armv7-linux-androideabi",
            "--config",
            r#"build.rustflags=["--cfg", "mls_build_async"]"#,
        ];
        let features_flag = if let Some(f) = features {
            if f.is_empty() {
                vec!["--no-default-features".to_string()]
            } else {
                vec!["--no-default-features".to_string(), "--features".to_string(), f.join(",")]
            }
        } else {
            Vec::new()
        };
        let crate_path = self.downloader.download(crate_name, version)?;
        let required = CargoTreeResolver::parse_cargo_tree_output(
            &Command::new("cargo")
                .args(COMMON_ARGS)
                .arg("--no-default-features")
                .current_dir(&crate_path)
                .run_quiet_and_expect_success()?
                .stdout,
        )?;
        let optional = CargoTreeResolver::parse_cargo_tree_output(
            &Command::new("cargo")
                .args(COMMON_ARGS)
                .args(features_flag)
                .current_dir(&crate_path)
                .run_quiet_and_expect_success()?
                .stdout,
        )?
        .into_iter()
        .filter(|dep| !required.contains(dep))
        .collect::<BTreeSet<_>>();
        Ok(Deps { required, optional })
    }
    fn parse_cargo_tree_output(stdout: &[u8]) -> Result<BTreeSet<String>> {
        Ok(String::from_utf8_lossy(stdout)
            .lines()
            .filter_map(|line| match line.split_at_checked(1) {
                Some(("1", rest)) => rest.split_whitespace().next().map(String::from),
                _ => None,
            })
            .collect::<BTreeSet<_>>())
    }
}

struct CratesIoResolver {
    index: CratesIoIndex,
}

impl CratesIoResolver {
    fn new() -> Result<Self> {
        Ok(CratesIoResolver { index: CratesIoIndex::new_cargo()? })
    }
    fn resolve(
        &self,
        crate_name: &str,
        version: &semver::Version,
        features: &Option<Vec<&str>>,
    ) -> Result<Deps> {
        let cio_crate = self.index.get_crate(crate_name)?;
        let version = cio_crate
            .get_version(version)
            .ok_or(anyhow!("{crate_name} version {version} not found"))?;
        let required = version
            .dependencies()
            .iter()
            .filter_map(|dep| {
                if dep.kind() == crates_index::DependencyKind::Normal
                    && !dep.is_optional()
                    && dep.is_android_target()
                {
                    Some(dep.crate_name().to_string())
                } else {
                    None
                }
            })
            .collect::<BTreeSet<String>>();
        let resolver = FeatureResolver::new(version);
        let optional = resolver
            .resolve_optional(features.as_ref().map(|f| f.iter()))?
            .into_iter()
            .map(|d| d.crate_name().to_string())
            .collect::<BTreeSet<String>>();
        Ok(Deps { required, optional })
    }
}

trait CrateFeaturesWithDefault {
    fn crate_features_with_default<'a>(
        &'a self,
        crate_name: &'a str,
    ) -> impl Iterator<Item = Option<Vec<&'a str>>>;
}

impl CrateFeaturesWithDefault for BluePrint {
    fn crate_features_with_default<'a>(
        &'a self,
        crate_name: &'a str,
    ) -> impl Iterator<Item = Option<Vec<&'a str>>> {
        self.crate_features(crate_name)
            .filter_map(move |features_or_none| {
                match features_or_none {
                    // ahash has a "specialize" feature that is added by the build script and passed to
                    // rustc, but not present in Cargo.toml.
                    Some(features) => {
                        if crate_name == "ahash" {
                            Some(Some(
                                features
                                    .into_iter()
                                    .filter(|feature| *feature != "specialize")
                                    .collect(),
                            ))
                        } else {
                            Some(Some(features))
                        }
                    }
                    // We always add the default feature set below with .chain([None]), so
                    // filter it out to avoid testing it twice.
                    None => None,
                }
            })
            .chain([None])
    }
}

#[test]
#[ignore]
fn compare_with_cargo_tree() -> Result<()> {
    env_logger::init();

    let managed_repo = ManagedRepo::new(
        RootedPath::new(default_repo_root()?, "external/rust/android-crates-io")?,
        false,
    )?;
    let cargo_tree_resolver = CargoTreeResolver::new()?;
    let crates_io_resolver = CratesIoResolver::new()?;

    let crate_names = env::var("CRATES_TO_TEST")
        .map(|s| s.split(",").map(String::from).collect::<BTreeSet<_>>())
        .unwrap_or_else(|_| managed_repo.all_crate_names().unwrap());
    let mut all_match = true;
    for crate_name in &crate_names {
        let managed_crate = managed_repo.managed_crate_for(crate_name)?;
        let version = managed_crate.android_version();
        println!("Checking {crate_name} {version}");

        if crates_io_resolver
            .index
            .get_crate(crate_name)?
            .get_version(managed_crate.android_version())
            .ok_or(anyhow!("{crate_name} version {version} not found"))?
            .is_yanked()
        {
            println!("  Yanked. Skipping.");
            continue;
        }

        let bp = BluePrint::from_file(managed_crate.android_crate_path().abs().join("Android.bp"))
            .map_err(|e: String| anyhow!(e))?;
        for features in bp.crate_features_with_default(crate_name) {
            let cargo_tree_deps = match cargo_tree_resolver.resolve(crate_name, version, &features)
            {
                Ok(d) => d,
                Err(e) => {
                    println!("  Failed to resolve with 'cargo tree': {e}");
                    all_match = false;
                    continue;
                }
            };
            let crates_io_deps = match crates_io_resolver.resolve(crate_name, version, &features) {
                Ok(d) => d,
                Err(e) => {
                    println!("  Failed to resolve: {e}");
                    all_match = false;
                    continue;
                }
            };

            if cargo_tree_deps != crates_io_deps {
                all_match = false;
                if cargo_tree_deps.required != crates_io_deps.required {
                    println!("  Required deps different for {:?}", features);
                    println!("    cargo tree: {}", cargo_tree_deps.required.iter().join(", "));
                    println!("    crates.io:  {}", crates_io_deps.required.iter().join(", "));
                }
                if cargo_tree_deps.optional != crates_io_deps.optional {
                    println!("  Optional deps different for {:?}", features);
                    println!("    cargo tree: {}", cargo_tree_deps.optional.iter().join(", "));
                    println!("    crates.io:  {}", crates_io_deps.optional.iter().join(", "));
                }
            }
        }
    }
    assert!(all_match);
    Ok(())
}
