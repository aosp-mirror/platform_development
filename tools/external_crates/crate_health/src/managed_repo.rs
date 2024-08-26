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
    collections::BTreeSet,
    fs::{create_dir, remove_dir_all, remove_file, rename, write},
    path::Path,
    process::Command,
    str::from_utf8,
};

use anyhow::{anyhow, Result};
use glob::glob;
use semver::Version;

use crate::{
    copy_dir, CrateCollection, Migratable, NameAndVersion, NameAndVersionMap, NameAndVersionRef,
    NamedAndVersioned, PseudoCrate, RepoPath, VersionMatch,
};

pub struct ManagedRepo {
    path: RepoPath,
    pseudo_crate: PseudoCrate,
}

impl ManagedRepo {
    pub fn new(path: RepoPath) -> ManagedRepo {
        let pseudo_crate = PseudoCrate::new(path.join(&"pseudo_crate"));
        ManagedRepo { path, pseudo_crate }
    }
    pub fn contains(&self, crate_name: &str) -> bool {
        self.managed_dir_for(crate_name).abs().exists()
    }
    pub fn managed_dir(&self) -> RepoPath {
        self.path.join(&Path::new("crates"))
    }
    pub fn managed_dir_for(&self, crate_name: &str) -> RepoPath {
        self.managed_dir().join(&crate_name)
    }
    pub fn vendored_dir_for(&self, crate_name: &str) -> RepoPath {
        self.pseudo_crate.get_path().join(&Path::new("vendor").join(crate_name))
    }
    pub fn legacy_dir_for(&self, crate_name: &str) -> RepoPath {
        self.path.with_same_root("external/rust/crates").join(&crate_name)
    }
    pub fn new_cc(&self) -> CrateCollection {
        CrateCollection::new(self.path.root())
    }
    pub fn vendored_crates(&self) -> Result<CrateCollection> {
        let mut cc = self.new_cc();
        cc.add_from(&self.pseudo_crate.get_path().join(&"vendor").rel())?;
        Ok(cc)
    }
    pub fn migration_health(
        &self,
        crate_name: &str,
        verbose: bool,
        unpinned: bool,
    ) -> Result<Version> {
        if self.contains(crate_name) {
            return Err(anyhow!("Crate {} already exists in {}/crates", crate_name, self.path));
        }

        let mut cc = self.new_cc();
        cc.add_from(&self.legacy_dir_for(crate_name).rel())?;
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
        if !krate.is_android_bp_healthy() {
            let mut show_cargo_embargo_results = true;
            if krate.is_migration_denied() {
                println!("This crate is on the migration denylist");
                show_cargo_embargo_results = false;
            }
            if !krate.android_bp().abs().exists() {
                println!("There is no Android.bp file in {}", krate.path());
                show_cargo_embargo_results = false;
            }
            if !krate.cargo_embargo_json().abs().exists() {
                show_cargo_embargo_results = false;
                println!("There is no cargo_embargo.json file in {}", krate.path());
            }
            if show_cargo_embargo_results {
                if !krate.generate_android_bp_success() {
                    println!(
                        "cargo_embargo execution did not succeed for {}",
                        krate.staging_path(),
                    );
                    if verbose {
                        println!(
                            "stdout:\n{}\nstderr:\n{}",
                            from_utf8(
                                &krate
                                    .generate_android_bp_output()
                                    .ok_or(anyhow!("cargo_embargo output not found"))?
                                    .stdout
                            )?,
                            from_utf8(
                                &krate
                                    .generate_android_bp_output()
                                    .ok_or(anyhow!("cargo_embargo output not found"))?
                                    .stderr
                            )?,
                        );
                    }
                } else if !krate.android_bp_unchanged() {
                    println!(
                        "Running cargo_embargo on {} produced changes to the Android.bp file",
                        krate.path()
                    );
                    if verbose {
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
                }
            }
            println!("The crate is UNHEALTHY");
            return Err(anyhow!("Crate {} is unhealthy", crate_name));
        }

        if unpinned {
            self.pseudo_crate.add_unpinned(krate)?;
        } else {
            self.pseudo_crate.add(krate)?;
        }
        self.pseudo_crate.vendor()?;

        let mut source = self.new_cc();
        source.add_from(&self.legacy_dir_for(crate_name).rel())?;

        let dest = self.vendored_crates()?;

        let mut version_match = VersionMatch::new(source, dest)?;

        version_match.stage_crates()?;
        version_match.copy_customizations()?;
        version_match.apply_patches()?;
        version_match.generate_android_bps()?;
        version_match.diff_android_bps()?;

        self.pseudo_crate.remove(krate)?;
        self.pseudo_crate.vendor()?;

        let compatible_pairs = version_match.compatible_pairs().collect::<Vec<_>>();
        if compatible_pairs.len() != 1 {
            return Err(anyhow!("Couldn't find a compatible version to migrate to",));
        }
        let pair = compatible_pairs.first().unwrap();
        let version = pair.dest.version().clone();
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
                if verbose {
                    for output in pair.dest.patch_output() {
                        if !output.1.status.success() {
                            println!(
                                "Failed to apply {}\nstdout:\n{}\nstderr:\n:{}",
                                output.0,
                                from_utf8(&output.1.stdout)?,
                                from_utf8(&output.1.stderr)?
                            );
                        }
                    }
                }
            }
            if !pair.dest.generate_android_bp_success() {
                println!("cargo_embargo execution did not succeed for the migrated crate");
            } else if !pair.dest.android_bp_unchanged() {
                println!("Running cargo_embargo for the migrated crate produced changes to the Android.bp file");
                if verbose {
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
        }

        let mut diff_cmd = Command::new("diff");
        diff_cmd.args(["-u", "-r", "-w", "--no-dereference"]);
        if !verbose {
            diff_cmd.arg("-q");
        }
        let diff_status = diff_cmd
            .args(IGNORED_FILES.iter().map(|ignored| format!("--exclude={}", ignored)))
            .args(["-I", r#"default_team: "trendy_team_android_rust""#])
            .arg(pair.source.path().rel())
            .arg(pair.dest.staging_path().rel())
            .current_dir(self.path.root())
            .spawn()?
            .wait()?;
        if !diff_status.success() {
            println!(
                "Found differences between {} and {}",
                pair.source.path(),
                pair.dest.staging_path()
            );
        }
        if verbose {
            println!("All diffs:");
            Command::new("diff")
                .args(["-u", "-r", "-w", "-q", "--no-dereference"])
                .arg(pair.source.path().rel())
                .arg(pair.dest.staging_path().rel())
                .current_dir(self.path.root())
                .spawn()?
                .wait()?;
        }

        if !pair.dest.is_migratable() {
            println!("The crate is UNHEALTHY");
            return Err(anyhow!("Crate {} is unhealthy", crate_name));
        }

        if diff_status.success() {
            println!("The crate is healthy");
            return Ok(version);
        }

        if unpinned {
            println!("The crate was added with an unpinned version, and diffs were found which must be inspected manually");
            return Ok(version);
        }

        println!("The crate is UNHEALTHY");
        Err(anyhow!("Crate {} is unhealthy", crate_name))
    }
    pub fn migrate<T: AsRef<str>>(
        &self,
        crates: Vec<T>,
        verbose: bool,
        unpinned: &BTreeSet<String>,
    ) -> Result<()> {
        for crate_name in &crates {
            let crate_name = crate_name.as_ref();
            let version =
                self.migration_health(crate_name, verbose, unpinned.contains(crate_name))?;
            let src_dir = self.legacy_dir_for(crate_name);

            let monorepo_crate_dir = self.managed_dir();
            if !monorepo_crate_dir.abs().exists() {
                create_dir(&monorepo_crate_dir.abs())?;
            }
            copy_dir(&src_dir.abs(), &self.managed_dir_for(crate_name).abs())?;
            if unpinned.contains(crate_name) {
                self.pseudo_crate.add_unpinned(&NameAndVersionRef::new(&crate_name, &version))?;
            } else {
                self.pseudo_crate.add(&NameAndVersionRef::new(&crate_name, &version))?;
            }
        }

        self.regenerate(crates.iter(), false)?;

        for crate_name in &crates {
            let crate_name = crate_name.as_ref();
            let src_dir = self.legacy_dir_for(crate_name);
            for entry in glob(
                src_dir
                    .abs()
                    .join("*.bp")
                    .to_str()
                    .ok_or(anyhow!("Failed to convert path *.bp to str"))?,
            )? {
                remove_file(entry?)?;
            }
            remove_file(src_dir.abs().join("cargo_embargo.json"))?;
            let test_mapping = src_dir.abs().join("TEST_MAPPING");
            if test_mapping.exists() {
                remove_file(test_mapping)?;
            }
            write(
                src_dir.abs().join("Android.bp"),
                format!("// This crate has been migrated to {}.\n", self.path),
            )?;
        }

        Ok(())
    }
    pub fn import(&self, _crate_name: &str) -> Result<()> {
        todo!();
    }
    pub fn regenerate<T: AsRef<str>>(
        &self,
        crates: impl Iterator<Item = T>,
        update_metadata: bool,
    ) -> Result<()> {
        let version_match = self.stage(crates)?;
        if update_metadata {
            version_match.update_metadata()?;
        }

        for pair in version_match.pairs() {
            let source_version = NameAndVersion::from(&pair.source.key());
            let pair = pair.to_compatible().ok_or(anyhow!(
                "No compatible vendored crate found for {} v{}",
                source_version.name(),
                source_version.version()
            ))?;

            if !pair.dest.staging_path().abs().exists() {
                return Err(anyhow!("Staged crate not found at {}", pair.dest.staging_path()));
            }
            let android_crate_dir = self.managed_dir_for(pair.source.name());
            remove_dir_all(&android_crate_dir.abs())?;
            rename(pair.dest.staging_path().abs(), &android_crate_dir.abs())?;
        }

        Ok(())
    }
    pub fn regenerate_all(&self, update_metadata: bool) -> Result<()> {
        self.regenerate(self.pseudo_crate.deps()?.keys().map(|k| k.as_str()), update_metadata)
    }
    pub fn stage<T: AsRef<str>>(
        &self,
        crates: impl Iterator<Item = T>,
    ) -> Result<VersionMatch<CrateCollection>> {
        let mut cc = self.new_cc();
        for crate_name in crates {
            let crate_name = crate_name.as_ref();
            let android_crate_dir = self.managed_dir_for(crate_name);
            if !android_crate_dir.abs().exists() {
                return Err(anyhow!("Crate {} not found in {}", crate_name, self.managed_dir()));
            }

            // Source
            cc.add_from(&android_crate_dir.abs())?;
            cc.map_field_mut().retain(|_nv, krate| krate.is_crates_io());
            let num_versions = cc.get_versions(crate_name).count();
            if num_versions != 1 {
                return Err(anyhow!(
                    "Expected a single crate version for {}, but found {}. Crates with multiple versions are not supported yet.",
                    crate_name,
                    num_versions
                ));
            }
        }

        self.pseudo_crate.vendor()?;

        // Dest
        let dest = self.vendored_crates()?;

        let mut version_match = VersionMatch::new(cc, dest)?;

        version_match.stage_crates()?;
        version_match.copy_customizations()?;
        version_match.apply_patches()?;
        version_match.generate_android_bps()?;
        version_match.diff_android_bps()?;

        Ok(version_match)
    }
    pub fn preupload_check(&self) -> Result<()> {
        let version_match = self.stage(self.pseudo_crate.deps()?.keys().map(|k| k.as_str()))?;

        for pair in version_match.pairs() {
            let source_version = NameAndVersion::from(&pair.source.key());
            let pair = pair.to_compatible().ok_or(anyhow!(
                "No compatible vendored crate found for {} v{}",
                source_version.name(),
                source_version.version()
            ))?;

            let diff_status = Command::new("diff")
                .args(["-u", "-r", "-w", "--no-dereference"])
                .arg(pair.dest.staging_path().rel())
                .arg(self.managed_dir_for(pair.source.name()).rel())
                .current_dir(self.path.root())
                .spawn()?
                .wait()?;
            if !diff_status.success() {
                return Err(anyhow!(
                    "Found differences between {} and {}",
                    pair.source.path(),
                    pair.dest.staging_path()
                ));
            }
        }

        Ok(())
    }
}

// Files that are ignored when migrating a crate to the monorepo.
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
