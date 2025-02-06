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
    cell::OnceCell,
    collections::BTreeSet,
    env,
    fs::{create_dir, create_dir_all, read_dir, write},
    os::unix::fs::symlink,
    path::Path,
    process::Command,
};

use anyhow::{anyhow, bail, Context, Result};
use crates_index::DependencyKind;
use google_metadata::GoogleMetadata;
use itertools::Itertools;
use license_checker::find_licenses;
use name_and_version::{NameAndVersion, NameAndVersionRef, NamedAndVersioned};
use repo_config::RepoConfig;
use rooted_path::RootedPath;
use semver::{Version, VersionReq};
use serde::Serialize;
use spdx::Licensee;

use crate::{
    android_bp::cargo_embargo_autoconfig,
    copy_dir,
    crate_collection::CrateCollection,
    crate_type::Crate,
    crates_io::{AndroidDependencies, CratesIoIndex, DependencyChanges, SafeVersions},
    license::{most_restrictive_type, update_module_license_files},
    managed_crate::ManagedCrate,
    pseudo_crate::{CargoVendorDirty, PseudoCrate},
    upgradable::{IsUpgradableTo, MatchesWithCompatibilityRule, SemverCompatibilityRule},
    SuccessOrError,
};

#[derive(Serialize, Default, Debug)]
struct UpdateSuggestions {
    updates: Vec<UpdateSuggestion>,
}

#[derive(Serialize, Default, Debug)]
struct UpdateSuggestion {
    name: String,
    #[serde(skip)]
    old_version: String,
    version: String,
}

pub struct ManagedRepo {
    path: RootedPath,
    config: OnceCell<RepoConfig>,
    crates_io: CratesIoIndex,
}

impl ManagedRepo {
    pub fn new(path: RootedPath, offline: bool) -> Result<ManagedRepo> {
        Ok(ManagedRepo {
            path,
            config: OnceCell::new(),
            crates_io: if offline { CratesIoIndex::new_offline()? } else { CratesIoIndex::new()? },
        })
    }
    pub fn config(&self) -> &RepoConfig {
        self.config.get_or_init(|| {
            RepoConfig::read(self.path.abs()).unwrap_or_else(|e| {
                panic!(
                    "Failed to read crate config {}/{}: {}",
                    self.path,
                    repo_config::CONFIG_FILE_NAME,
                    e
                )
            })
        })
    }
    fn pseudo_crate(&self) -> PseudoCrate<CargoVendorDirty> {
        PseudoCrate::new(self.path.join("pseudo_crate").unwrap())
    }
    fn contains(&self, crate_name: &str) -> bool {
        self.managed_dir_for(crate_name).abs().exists()
    }
    fn managed_dir(&self) -> RootedPath {
        self.path.join("crates").unwrap()
    }
    fn managed_dir_for(&self, crate_name: &str) -> RootedPath {
        self.managed_dir().join(crate_name).unwrap()
    }
    fn legacy_dir_for(&self, crate_name: &str, version: Option<&Version>) -> Result<RootedPath> {
        match version {
            Some(v) => {
                let cc = self.legacy_crates_for(crate_name)?;
                let nv = NameAndVersionRef::new(crate_name, v);
                Ok(cc
                    .get(&nv as &dyn NamedAndVersioned)
                    .ok_or(anyhow!("Failed to find crate {} v{}", crate_name, v))?
                    .path()
                    .clone())
            }
            None => {
                Ok(self.path.with_same_root("external/rust/crates").unwrap().join(crate_name)?)
            }
        }
    }
    fn legacy_crates_for(&self, crate_name: &str) -> Result<CrateCollection> {
        let mut cc = self.new_cc();
        cc.add_from(format!("external/rust/crates/{}", crate_name))?;
        Ok(cc)
    }
    fn legacy_crates(&self) -> Result<CrateCollection> {
        let mut cc = self.new_cc();
        cc.add_from("external/rust/crates")?;
        Ok(cc)
    }
    fn new_cc(&self) -> CrateCollection {
        CrateCollection::new(self.path.root())
    }
    fn managed_crate_for(
        &self,
        crate_name: &str,
    ) -> Result<ManagedCrate<crate::managed_crate::New>> {
        Ok(ManagedCrate::new(Crate::from(self.managed_dir_for(crate_name))?))
    }
    pub fn all_crate_names(&self) -> Result<BTreeSet<String>> {
        let mut managed_dirs = BTreeSet::new();
        if self.managed_dir().abs().exists() {
            for entry in read_dir(self.managed_dir())? {
                let entry = entry?;
                if entry.path().is_dir() {
                    managed_dirs.insert(entry.file_name().into_string().map_err(|e| {
                        anyhow!("Failed to convert {} to string", e.to_string_lossy())
                    })?);
                }
            }
        }
        Ok(managed_dirs)
    }
    pub fn analyze_import(&self, crate_name: &str) -> Result<()> {
        if self.contains(crate_name) {
            println!("Crate already imported at {}", self.managed_dir_for(crate_name));
            return Ok(());
        }
        let legacy_dir = self.legacy_dir_for(crate_name, None)?;
        if legacy_dir.abs().exists() {
            println!("Legacy crate already imported at {}", legacy_dir);
            return Ok(());
        }

        if !self.config().is_allowed(crate_name) {
            println!("Crate {crate_name} is on the import denylist");
            return Ok(());
        }

        let mut managed_crates = self.new_cc();
        managed_crates.add_from(self.managed_dir().rel())?;
        let legacy_crates = self.legacy_crates()?;

        let cio_crate = self.crates_io.get_crate(crate_name)?;

        for version in cio_crate.versions() {
            println!("Version {}", version.version());
            let mut found_problems = false;
            for (dep, req) in version.android_deps_with_version_reqs() {
                println!("Found dep {}", dep.crate_name());
                let cc = if managed_crates.contains_crate(dep.crate_name()) {
                    &managed_crates
                } else {
                    &legacy_crates
                };
                if !cc.contains_crate(dep.crate_name()) {
                    found_problems = true;
                    println!(
                        "  Dep {} {} has not been imported to Android",
                        dep.crate_name(),
                        dep.requirement()
                    );
                    if !self.config().is_allowed(dep.crate_name()) {
                        println!("    And {} is on the import denylist", dep.crate_name());
                    }
                    // This is a no-op because our dependency code only considers normal deps anyway.
                    // TODO: Fix the deps code.
                    if matches!(dep.kind(), DependencyKind::Dev) {
                        println!("    But this is a dev dependency, probably only needed if you want to run the tests");
                    }
                    if dep.is_optional() {
                        println!("    But this is an optional dependency, used by the following features: {}", dep.features().join(", "));
                    }
                    continue;
                }
                let versions = cc.get_versions(dep.crate_name()).collect::<Vec<_>>();
                let has_matching_version = versions.iter().any(|(nv, _)| {
                    req.matches_with_compatibility_rule(
                        nv.version(),
                        SemverCompatibilityRule::Loose,
                    )
                });
                if !has_matching_version {
                    found_problems = true;
                }
                if !has_matching_version || versions.len() > 1 {
                    if has_matching_version {
                        println!("  Dep {} has multiple versions available. You may need to override the default choice in cargo_embargo.json", dep.crate_name());
                    }
                    for (_, dep_crate) in versions {
                        println!(
                            "  Dep {} {} is {}satisfied by v{} at {}",
                            dep.crate_name(),
                            dep.requirement(),
                            if req.matches_with_compatibility_rule(
                                dep_crate.version(),
                                SemverCompatibilityRule::Loose
                            ) {
                                ""
                            } else {
                                "not "
                            },
                            dep_crate.version(),
                            dep_crate.path()
                        );
                    }
                }
            }
            if !found_problems {
                println!("  No problems found with this version.")
            }
        }
        Ok(())
    }
    pub fn import(&self, crate_name: &str, version: &str, autoconfig: bool) -> Result<()> {
        if self.contains(crate_name) {
            bail!("Crate already imported at {}", self.managed_dir_for(crate_name));
        }
        let legacy_dir = self.legacy_dir_for(crate_name, None)?;
        if legacy_dir.abs().exists() {
            bail!("Legacy crate already imported at {}", legacy_dir);
        }
        if !self.config().is_allowed(crate_name) {
            bail!("Crate {crate_name} is on the import denylist");
        }

        let pseudo_crate = self.pseudo_crate();
        let version = Version::parse(version)?;
        let nv = NameAndVersionRef::new(crate_name, &version);
        pseudo_crate.cargo_add(&nv)?;
        let pseudo_crate = pseudo_crate.vendor()?;

        let vendored_dir = pseudo_crate.vendored_dir_for(crate_name)?;
        let managed_dir = self.managed_dir_for(crate_name);
        println!("Creating {} from vendored crate", managed_dir);
        copy_dir(vendored_dir, &managed_dir)?;

        println!("Sprinkling Android glitter on {}:", crate_name);

        let krate = Crate::from(managed_dir.clone())?;

        println!("  Finding license files");
        let licenses = find_licenses(krate.path().abs(), krate.name(), krate.license())?;

        if !licenses.unsatisfied.is_empty() && licenses.satisfied.is_empty() {
            let mut satisfied = false;
            // Sometimes multiple crates live in a single GitHub repo. A common case
            // is a crate with an associated proc_macro crate. In such cases, the individual
            // crates are in subdirectories with license files at root of the repo, and
            // the license files don't get distributed with the crates.
            // So, if we didn't find a license file, try to guess the URL of the appropriate
            // license file and download it. This is incredibly hacky, and only supports
            // the most common case, which is LICENSE-APACHE.
            if licenses.unsatisfied.len() == 1 {
                let req = licenses.unsatisfied.first().unwrap();
                if let Some(repository) = krate.repository() {
                    if *req == Licensee::parse("Apache-2.0").unwrap().into_req() {
                        let url = format!("{}/master/LICENSE-APACHE", repository);
                        let body = reqwest::blocking::get(
                            url.replace("github.com", "raw.githubusercontent.com"),
                        )?
                        .text()?;
                        write(krate.path().abs().join("LICENSE"), body)?;
                        let patch_dir = krate.path().abs().join("patches");
                        create_dir(&patch_dir)?;
                        let output = Command::new("diff")
                            .args(["-u", "/dev/null", "LICENSE"])
                            .current_dir(krate.path().abs())
                            .output()?;
                        write(patch_dir.join("LICENSE.patch"), output.stdout)?;
                        satisfied = true;
                    }
                }
            }
            if !satisfied {
                return Err(anyhow!(
                    "Could not find license files for all licenses. Missing {}",
                    licenses.unsatisfied.iter().join(", ")
                ));
            }
        }

        // If there's a single applicable license file, symlink it to LICENSE.
        if licenses.satisfied.len() == 1 && licenses.unsatisfied.is_empty() {
            let license_file = krate.path().join("LICENSE")?;
            if !license_file.abs().exists() {
                symlink(licenses.satisfied.iter().next().unwrap().1, license_file)?;
            }
        }

        update_module_license_files(&krate.path().abs(), &licenses)?;

        println!("  Creating METADATA");
        let metadata = GoogleMetadata::init(
            krate.path().join("METADATA")?,
            krate.name(),
            krate.version().to_string(),
            krate.description(),
            most_restrictive_type(&licenses),
        )?;
        metadata.write()?;

        println!("  Creating cargo_embargo.json and Android.bp");
        if autoconfig {
            // TODO: Copy to a temp dir, because otherwise we might run cargo and create/modify Cargo.lock.
            cargo_embargo_autoconfig(&managed_dir)?
                .success_or_error()
                .context("Failed to generate cargo_embargo.json with 'cargo_embargo autoconfig'")?;
        } else {
            write(krate.path().abs().join("cargo_embargo.json"), "{}")?;
        }
        // Workaround. Our logic for crate health assumes the crate isn't healthy if there's
        // no Android.bp. So create an empty one.
        write(krate.path().abs().join("Android.bp"), "")?;

        self.regenerate([&crate_name].iter())?;
        println!("Please edit {} and run 'regenerate' for this crate", managed_dir);

        Ok(())
    }
    pub fn regenerate<T: AsRef<str>>(&self, crates: impl Iterator<Item = T>) -> Result<()> {
        let pseudo_crate = self.pseudo_crate().vendor()?;
        for crate_name in crates {
            println!("Regenerating {}", crate_name.as_ref());
            let mc = self.managed_crate_for(crate_name.as_ref())?;
            // TODO: Don't give up if there's a failure.
            mc.regenerate(&pseudo_crate)?;
        }

        pseudo_crate.regenerate_crate_list()?;

        Ok(())
    }
    pub fn preupload_check(&self, files: &[String]) -> Result<()> {
        let pseudo_crate = self.pseudo_crate().vendor()?;
        let deps = pseudo_crate.deps().keys().cloned().collect::<BTreeSet<_>>();

        let managed_dirs = self.all_crate_names()?;

        if deps != managed_dirs {
            return Err(anyhow!("Deps in pseudo_crate/Cargo.toml don't match directories in {}\nDirectories not in Cargo.toml: {}\nCargo.toml deps with no directory: {}",
                self.managed_dir(), managed_dirs.difference(&deps).join(", "), deps.difference(&managed_dirs).join(", ")));
        }

        let crate_list = pseudo_crate.read_crate_list("crate-list.txt")?;
        if !deps.is_subset(&crate_list) {
            bail!("Deps in pseudo_crate/Cargo.toml don't match deps in crate-list.txt\nCargo.toml: {}\ncrate-list.txt: {}",
                deps.iter().join(", "), crate_list.iter().join(", "));
        }

        let expected_deleted_crates =
            crate_list.difference(&deps).cloned().collect::<BTreeSet<_>>();
        let deleted_crates = pseudo_crate.read_crate_list("deleted-crates.txt")?;
        if deleted_crates != expected_deleted_crates {
            bail!(
                "Deleted crate list is inconsistent. Expected: {}, Found: {}",
                expected_deleted_crates.iter().join(", "),
                deleted_crates.iter().join(", ")
            );
        }

        // Per https://android.googlesource.com/platform/tools/repohooks/,
        // the REPO_PATH environment variable is the path of the git repo relative to the
        // root of the Android source tree.
        let prefix = self.path.rel().strip_prefix(env::var("REPO_PATH")?)?;
        let changed_android_crates = files
            .iter()
            .filter_map(|file| Path::new(file).strip_prefix(prefix).ok())
            .filter_map(|path| {
                let components = path.components().collect::<Vec<_>>();
                if path.starts_with("crates/") && components.len() > 2 {
                    Some(components[1].as_os_str().to_string_lossy().to_string())
                } else {
                    None
                }
            })
            .collect::<BTreeSet<_>>();

        for crate_name in changed_android_crates {
            println!("Verifying checksums for {}", crate_name);
            checksum::verify(self.managed_dir_for(&crate_name).abs())?;
        }
        Ok(())
    }
    pub fn recontextualize_patches<T: AsRef<str>>(
        &self,
        crates: impl Iterator<Item = T>,
    ) -> Result<()> {
        for crate_name in crates {
            let mc = self.managed_crate_for(crate_name.as_ref())?;
            mc.recontextualize_patches()?;
        }
        Ok(())
    }
    pub fn updatable_crates(&self) -> Result<()> {
        let mut cc = self.new_cc();
        cc.add_from(self.managed_dir().rel())?;

        for krate in cc.values() {
            let cio_crate = self.crates_io.get_crate(krate.name())?;
            let upgrades =
                cio_crate.versions_gt(krate.version()).map(|v| v.version()).collect::<Vec<_>>();
            if !upgrades.is_empty() {
                println!(
                    "{} v{}:\n  {}",
                    krate.name(),
                    krate.version(),
                    upgrades
                        .iter()
                        .chunks(10)
                        .into_iter()
                        .map(|mut c| { c.join(", ") })
                        .join(",\n  ")
                );
            }
        }
        Ok(())
    }
    pub fn analyze_updates(&self, crate_name: impl AsRef<str>) -> Result<()> {
        let mut managed_crates = self.new_cc();
        managed_crates.add_from(self.managed_dir().rel())?;
        let legacy_crates = self.legacy_crates()?;

        let krate = self.managed_crate_for(crate_name.as_ref())?;
        println!("Analyzing updates to {} v{}", krate.name(), krate.android_version());
        let patches = krate.patches()?;
        if !patches.is_empty() {
            println!("This crate has patches, so expect a fun time trying to update it:");
            for patch in patches {
                println!(
                    "  {}",
                    Path::new(patch.file_name().ok_or(anyhow!("No file name"))?).display()
                );
            }
        }

        let cio_crate = self.crates_io.get_crate(crate_name)?;

        let base_version = cio_crate.get_version(krate.android_version()).ok_or(anyhow!(
            "{} v{} not found in crates.io",
            krate.name(),
            krate.android_version()
        ))?;
        let base_deps = base_version.android_version_reqs_by_name();

        let mut newer_versions = cio_crate.versions_gt(krate.android_version()).peekable();
        if newer_versions.peek().is_none() {
            println!("There are no newer versions of this crate.");
        }
        for version in newer_versions {
            println!("Version {}", version.version());
            let mut found_problems = false;
            let parsed_version = semver::Version::parse(version.version())?;
            if !krate
                .android_version()
                .is_upgradable_to(&parsed_version, SemverCompatibilityRule::Strict)
            {
                found_problems = true;
                if !krate
                    .android_version()
                    .is_upgradable_to(&parsed_version, SemverCompatibilityRule::Loose)
                {
                    println!("  Not semver-compatible, even by relaxed standards");
                } else {
                    println!("  Semver-compatible, but only by relaxed standards since major version is 0");
                }
            }
            // Check to see if the update has any missing dependencies.
            // We try to be a little clever about this in the following ways:
            // * Only consider deps that are likely to be relevant to Android. For example, ignore Windows-only deps.
            // * If a dep is missing, but the same dep exists for the current version of the crate, it's probably not actually necessary.
            // * Use relaxed version requirements, treating 0.x and 0.y as compatible, even though they aren't according to semver rules.
            for (dep, req) in version.android_deps_with_version_reqs() {
                let cc = if managed_crates.contains_crate(dep.crate_name()) {
                    &managed_crates
                } else {
                    &legacy_crates
                };
                if !cc.contains_crate(dep.crate_name()) {
                    found_problems = true;
                    println!(
                        "  Dep {} {} has not been imported to Android",
                        dep.crate_name(),
                        dep.requirement()
                    );
                    if !dep.is_new_dep(&base_deps) {
                        println!("    But the current version has the same dependency, and it seems to work");
                    } else {
                        continue;
                    }
                }
                for (_, dep_crate) in cc.get_versions(dep.crate_name()) {
                    if !req.matches_with_compatibility_rule(
                        dep_crate.version(),
                        SemverCompatibilityRule::Loose,
                    ) {
                        found_problems = true;
                        println!(
                            "  Dep {} {} is not satisfied by v{} at {}",
                            dep.crate_name(),
                            dep.requirement(),
                            dep_crate.version(),
                            dep_crate.path()
                        );
                        if !dep.is_changed_dep(&base_deps) {
                            println!("    But the current version has the same dependency and it seems to work.")
                        }
                    }
                }
            }
            if !found_problems {
                println!("  No problems found with this version.")
            }
        }

        Ok(())
    }
    pub fn suggest_updates(
        &self,
        consider_patched_crates: bool,
        semver_compatibility: SemverCompatibilityRule,
        json: bool,
    ) -> Result<()> {
        let mut suggestions = UpdateSuggestions::default();
        let mut managed_crates = self.new_cc();
        managed_crates.add_from(self.managed_dir().rel())?;
        let legacy_crates = self.legacy_crates()?;

        for krate in managed_crates.values() {
            let cio_crate = self.crates_io.get_crate(krate.name())?;

            let base_version = cio_crate.get_version(krate.version());
            if base_version.is_none() {
                if !json {
                    println!(
                        "Skipping crate {} v{} because it was not found in crates.io",
                        krate.name(),
                        krate.version()
                    );
                }
                continue;
            }
            let base_version = base_version.unwrap();
            let base_deps = base_version.android_version_reqs_by_name();

            let patch_dir = krate.path().join("patches").unwrap();
            if patch_dir.abs().exists() && !consider_patched_crates {
                if !json {
                    println!(
                        "Skipping crate {} v{} because it has patches",
                        krate.name(),
                        krate.version()
                    );
                }
                continue;
            }

            for version in cio_crate.versions_gt(krate.version()).rev() {
                let parsed_version = semver::Version::parse(version.version())?;
                if !krate.version().is_upgradable_to(&parsed_version, semver_compatibility) {
                    continue;
                }
                if !version.android_deps_with_version_reqs().any(|(dep, req)| {
                    if !dep.is_changed_dep(&base_deps) {
                        return false;
                    }
                    let cc = if managed_crates.contains_crate(dep.crate_name()) {
                        &managed_crates
                    } else {
                        &legacy_crates
                    };
                    for (_, dep_crate) in cc.get_versions(dep.crate_name()) {
                        if req.matches_with_compatibility_rule(
                            dep_crate.version(),
                            SemverCompatibilityRule::Loose,
                        ) {
                            return false;
                        }
                    }
                    true
                }) {
                    suggestions.updates.push(UpdateSuggestion {
                        name: krate.name().to_string(),
                        old_version: krate.version().to_string(),
                        version: version.version().to_string(),
                    });
                    break;
                }
            }
        }

        if json {
            println!("{}", serde_json::to_string_pretty(&suggestions)?)
        } else {
            for suggestion in suggestions.updates {
                println!(
                    "Upgrade crate {} v{} to {}",
                    suggestion.name, suggestion.old_version, suggestion.version,
                );
            }
        }

        Ok(())
    }
    pub fn update(&self, crate_name: impl AsRef<str>, version: impl AsRef<str>) -> Result<()> {
        let crate_name = crate_name.as_ref();
        let version = Version::parse(version.as_ref())?;

        let pseudo_crate = self.pseudo_crate();
        let managed_crate = self.managed_crate_for(crate_name)?;
        let mut crate_updates = vec![NameAndVersion::new(crate_name.to_string(), version.clone())];

        let cio_crate = self.crates_io.get_crate(crate_name)?;
        let cio_crate_version = cio_crate
            .get_version(&version)
            .ok_or(anyhow!("Could not find {crate_name} {version} on crates.io"))?;

        for dependent_crate_name in managed_crate.config().update_with() {
            let dep = cio_crate_version
                .dependencies()
                .iter()
                .find(|dep| dep.crate_name() == dependent_crate_name)
                .ok_or(anyhow!(
                    "Could not find crate {dependent_crate_name} as a dependency of {crate_name}"
                ))?;
            let req = VersionReq::parse(dep.requirement())?;
            let dep_cio_crate = self.crates_io.get_crate(dependent_crate_name)?;
            let version = dep_cio_crate
                .safe_versions()
                .find(|v| {
                    if let Ok(parsed_version) = Version::parse(v.version()) {
                        req.matches(&parsed_version)
                    } else {
                        false
                    }
                })
                .ok_or(anyhow!(
                    "Failed to find a version of {dependent_crate_name} that satisfies {}",
                    dep.requirement()
                ))?;
            println!("Also updating {dependent_crate_name} to {}", version.version());
            crate_updates.push(NameAndVersion::new(
                dependent_crate_name.to_string(),
                Version::parse(version.version())?,
            ));
        }

        for nv in &crate_updates {
            pseudo_crate.remove(nv.name())?;
        }
        for nv in &crate_updates {
            pseudo_crate.cargo_add(nv)?;
        }
        self.regenerate(crate_updates.iter().map(|nv| nv.name()))?;
        Ok(())
    }
    pub fn init(&self) -> Result<()> {
        if self.path.abs().exists() {
            return Err(anyhow!("{} already exists", self.path));
        }
        create_dir_all(&self.path).context(format!("Failed to create {}", self.path))?;
        let crates_dir = self.path.join("crates")?;
        create_dir_all(&crates_dir).context(format!("Failed to create {}", crates_dir))?;
        self.pseudo_crate().init()?;
        Ok(())
    }
    pub fn verify_checksums<T: AsRef<str>>(&self, crates: impl Iterator<Item = T>) -> Result<()> {
        for krate in crates {
            println!("Verifying checksums for {}", krate.as_ref());
            checksum::verify(self.managed_dir_for(krate.as_ref()).abs())?;
        }
        Ok(())
    }
}
