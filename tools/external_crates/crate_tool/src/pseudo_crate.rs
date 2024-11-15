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
    cell::OnceCell,
    collections::{BTreeMap, BTreeSet},
    fs::{read, write},
    io::BufRead,
    process::Command,
    str::from_utf8,
};

use anyhow::{anyhow, Context, Result};
use itertools::Itertools;
use name_and_version::{NameAndVersionMap, NamedAndVersioned};
use rooted_path::RootedPath;
use semver::Version;

use crate::{crate_collection::CrateCollection, ensure_exists_and_empty, RunQuiet};

pub struct PseudoCrate<State: PseudoCrateState> {
    path: RootedPath,
    extra: State,
}

#[derive(Debug)]
pub struct CargoVendorClean {
    crates: OnceCell<CrateCollection>,
    deps: OnceCell<BTreeMap<String, Version>>,
}
#[derive(Debug)]
pub struct CargoVendorDirty {}

pub trait PseudoCrateState {}
impl PseudoCrateState for CargoVendorDirty {}
impl PseudoCrateState for CargoVendorClean {}

impl<State: PseudoCrateState> PseudoCrate<State> {
    pub fn get_path(&self) -> &RootedPath {
        &self.path
    }
    pub fn read_crate_list(&self) -> Result<Vec<String>> {
        let mut lines = Vec::new();
        for line in read(self.path.join("crate-list.txt")?)?.lines() {
            lines.push(line?);
        }
        Ok(lines)
    }
}

impl PseudoCrate<CargoVendorClean> {
    pub fn regenerate_crate_list(&self) -> Result<()> {
        write(self.path.join("crate-list.txt")?, format!("{}\n", self.deps().keys().join("\n")))?;
        Ok(())
    }
    fn version_of(&self, crate_name: &str) -> Result<Version> {
        self.deps()
            .get(crate_name)
            .cloned()
            .ok_or(anyhow!("Crate {} not found in Cargo.toml", crate_name))
    }
    pub fn deps(&self) -> &BTreeMap<String, Version> {
        self.extra.deps.get_or_init(|| {
            let output = Command::new("cargo")
                .args(["tree", "--depth=1", "--prefix=none"])
                .current_dir(&self.path)
                .run_quiet_and_expect_success()
                .unwrap();
            let mut deps = BTreeMap::new();
            for line in from_utf8(&output.stdout).unwrap().lines().skip(1) {
                let words = line.split(' ').collect::<Vec<_>>();
                if words.len() < 2 {
                    panic!("Failed to parse crate name and version from cargo tree: {}", line);
                }
                let version = words[1]
                    .strip_prefix('v')
                    .ok_or(anyhow!("Failed to parse version: {}", words[1]))
                    .unwrap();
                deps.insert(words[0].to_string(), Version::parse(version).unwrap());
            }
            deps
        })
    }
    pub fn vendored_dir_for(&self, crate_name: &str) -> Result<&RootedPath> {
        let version = self.version_of(crate_name)?;
        for (nv, krate) in self.crates().get_versions(crate_name) {
            if *nv.version() == version {
                return Ok(krate.path());
            }
        }
        Err(anyhow!("Couldn't find vendored directory for {} v{}", crate_name, version.to_string()))
    }
    fn crates(&self) -> &CrateCollection {
        self.extra.crates.get_or_init(|| {
            Command::new("cargo")
                .args(["vendor", "--versioned-dirs"])
                .current_dir(&self.path)
                .run_quiet_and_expect_success()
                .unwrap();
            let mut crates = CrateCollection::new(self.path.root());
            crates.add_from(self.get_path().join("vendor").unwrap()).unwrap();
            crates
        })
    }
    pub fn deps_of(&self, crate_name: &str) -> Result<BTreeSet<String>> {
        let mut deps = BTreeSet::new();
        let output = Command::new("cargo")
            .args(["tree", "--prefix", "none"])
            .current_dir(self.vendored_dir_for(crate_name)?)
            .run_quiet_and_expect_success()?;
        for line in from_utf8(&output.stdout)?.lines() {
            deps.insert(line.split_once(' ').unwrap().0.to_string());
        }
        Ok(deps)
    }
    fn mark_dirty(self) -> PseudoCrate<CargoVendorDirty> {
        PseudoCrate { path: self.path, extra: CargoVendorDirty {} }
    }
    #[allow(dead_code)]
    pub fn cargo_add(
        self,
        krate: &impl NamedAndVersioned,
    ) -> Result<PseudoCrate<CargoVendorDirty>> {
        let dirty = self.mark_dirty();
        dirty.cargo_add(krate)?;
        Ok(dirty)
    }
    #[allow(dead_code)]
    pub fn cargo_add_unpinned(
        self,
        krate: &impl NamedAndVersioned,
    ) -> Result<PseudoCrate<CargoVendorDirty>> {
        let dirty: PseudoCrate<CargoVendorDirty> = self.mark_dirty();
        dirty.cargo_add_unpinned(krate)?;
        Ok(dirty)
    }
    #[allow(dead_code)]
    pub fn cargo_add_unversioned(self, crate_name: &str) -> Result<PseudoCrate<CargoVendorDirty>> {
        let dirty: PseudoCrate<CargoVendorDirty> = self.mark_dirty();
        dirty.cargo_add_unversioned(crate_name)?;
        Ok(dirty)
    }
    pub fn remove(self, crate_name: &str) -> Result<PseudoCrate<CargoVendorDirty>> {
        let dirty: PseudoCrate<CargoVendorDirty> = self.mark_dirty();
        dirty.remove(crate_name)?;
        Ok(dirty)
    }
}

impl PseudoCrate<CargoVendorDirty> {
    pub fn new(path: RootedPath) -> Self {
        PseudoCrate { path, extra: CargoVendorDirty {} }
    }
    pub fn init(&self) -> Result<()> {
        if self.path.abs().exists() {
            return Err(anyhow!("Can't init pseudo-crate because {} already exists", self.path));
        }
        ensure_exists_and_empty(&self.path)?;

        write(
            self.path.join("Cargo.toml")?,
            r#"[package]
name = "android-pseudo-crate"
version = "0.1.0"
edition = "2021"
publish = false
license = "Apache-2.0"

[dependencies]
"#,
        )?;
        write(self.path.join("crate-list.txt")?, "")?;
        write(self.path.join(".gitignore")?, "target/\nvendor/\n")?;

        ensure_exists_and_empty(&self.path.join("src")?)?;
        write(self.path.join("src/lib.rs")?, "// Nothing")
            .context("Failed to create src/lib.rs")?;

        Ok(())
    }
    fn add_internal(&self, crate_and_version_str: &str, crate_name: &str) -> Result<()> {
        if let Err(e) = Command::new("cargo")
            .args(["add", crate_and_version_str])
            .current_dir(&self.path)
            .run_quiet_and_expect_success()
        {
            self.remove(crate_name).with_context(|| {
                format!("Failed to remove {} after failing to add it: {}", crate_name, e)
            })?;
            return Err(e);
        }
        Ok(())
    }
    pub fn cargo_add(&self, krate: &impl NamedAndVersioned) -> Result<()> {
        self.add_internal(format!("{}@={}", krate.name(), krate.version()).as_str(), krate.name())
    }
    pub fn cargo_add_unpinned(&self, krate: &impl NamedAndVersioned) -> Result<()> {
        self.add_internal(format!("{}@{}", krate.name(), krate.version()).as_str(), krate.name())
    }
    pub fn cargo_add_unversioned(&self, crate_name: &str) -> Result<()> {
        self.add_internal(crate_name, crate_name)
    }
    pub fn remove(&self, crate_name: impl AsRef<str>) -> Result<()> {
        Command::new("cargo")
            .args(["remove", crate_name.as_ref()])
            .current_dir(&self.path)
            .run_quiet_and_expect_success()?;
        Ok(())
    }
    // Mark the crate clean. Ironically, we don't actually need to run "cargo vendor"
    // immediately thanks to LazyCell.
    pub fn vendor(self) -> Result<PseudoCrate<CargoVendorClean>> {
        Ok(PseudoCrate {
            path: self.path,
            extra: CargoVendorClean { crates: OnceCell::new(), deps: OnceCell::new() },
        })
    }
}
