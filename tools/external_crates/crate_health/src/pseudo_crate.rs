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
    collections::{BTreeMap, BTreeSet},
    fs::{create_dir, read, write},
    io::BufRead,
    process::Command,
    str::from_utf8,
};

use anyhow::{anyhow, Context, Result};
use itertools::Itertools;
use name_and_version::NamedAndVersioned;
use rooted_path::RootedPath;
use serde::Serialize;
use tinytemplate::TinyTemplate;

use crate::{ensure_exists_and_empty, RunQuiet};

static CARGO_TOML_TEMPLATE: &'static str = include_str!("templates/Cargo.toml.template");

#[derive(Serialize)]
struct Dep {
    name: String,
    version: String,
}

#[derive(Serialize)]
struct CargoToml {
    deps: Vec<Dep>,
}

pub struct PseudoCrate {
    path: RootedPath,
}

impl PseudoCrate {
    pub fn new(path: RootedPath) -> PseudoCrate {
        PseudoCrate { path }
    }
    pub fn init<'a>(
        &self,
        crates: impl Iterator<Item = &'a (impl NamedAndVersioned + 'a)>,
        exact_version: bool,
    ) -> Result<()> {
        if self.path.abs().exists() {
            return Err(anyhow!("Can't init pseudo-crate because {} already exists", self.path));
        }
        ensure_exists_and_empty(&self.path)?;

        let mut deps = Vec::new();
        for krate in crates {
            // Special cases:
            // * libsqlite3-sys is a sub-crate of rusqlite
            // * remove_dir_all has a version not known by crates.io (b/313489216)
            if krate.name() != "libsqlite3-sys" {
                deps.push(Dep {
                    name: krate.name().to_string(),
                    version: format!(
                        "{}{}",
                        if exact_version { "=" } else { "" },
                        if krate.name() == "remove_dir_all"
                            && krate.version().to_string() == "0.7.1"
                        {
                            "0.7.0".to_string()
                        } else {
                            krate.version().to_string()
                        }
                    ),
                });
            }
        }

        let mut tt = TinyTemplate::new();
        tt.add_template("cargo_toml", CARGO_TOML_TEMPLATE)?;
        write(self.path.join("Cargo.toml")?, tt.render("cargo_toml", &CargoToml { deps })?)?;

        create_dir(self.path.join("src")?).context("Failed to create src dir")?;
        write(self.path.join("src/lib.rs")?, "// Nothing")
            .context("Failed to create src/lib.rs")?;

        self.vendor()
    }
    pub fn get_path(&self) -> &RootedPath {
        &self.path
    }
    fn add_internal(&self, crate_and_version_str: &str) -> Result<()> {
        Command::new("cargo")
            .args(["add", crate_and_version_str])
            .current_dir(&self.path)
            .run_quiet_and_expect_success()?;
        Ok(())
    }
    pub fn add(&self, krate: &impl NamedAndVersioned) -> Result<()> {
        self.add_internal(format!("{}@={}", krate.name(), krate.version()).as_str())
    }
    pub fn add_unpinned(&self, krate: &impl NamedAndVersioned) -> Result<()> {
        self.add_internal(format!("{}@{}", krate.name(), krate.version()).as_str())
    }
    pub fn add_unversioned(&self, crate_name: &str) -> Result<()> {
        self.add_internal(crate_name)
    }
    pub fn remove(&self, krate: &impl NamedAndVersioned) -> Result<()> {
        Command::new("cargo")
            .args(["remove", krate.name()])
            .current_dir(&self.path)
            .run_quiet_and_expect_success()?;
        Ok(())
    }
    pub fn vendor(&self) -> Result<()> {
        Command::new("cargo")
            .args(["vendor"])
            .current_dir(&self.path)
            .run_quiet_and_expect_success()?;
        Ok(())
    }
    pub fn deps(&self) -> Result<BTreeMap<String, String>> {
        let output = Command::new("cargo")
            .args(["tree", "--depth=1", "--prefix=none"])
            .current_dir(&self.path)
            .run_quiet_and_expect_success()?;
        let mut deps = BTreeMap::new();
        for line in from_utf8(&output.stdout)?.lines().skip(1) {
            let words = line.split(" ").collect::<Vec<_>>();
            if words.len() < 2 {
                return Err(anyhow!(
                    "Failed to parse crate name and version from cargo tree: {}",
                    line
                ));
            }
            let version = words[1]
                .strip_prefix("v")
                .ok_or(anyhow!("Failed to parse version: {}", words[1]))?;
            deps.insert(words[0].to_string(), version.to_string());
        }
        Ok(deps)
    }
    pub fn deps_of(&self, crate_name: &str) -> Result<BTreeSet<String>> {
        let mut deps = BTreeSet::new();
        let output = Command::new("cargo")
            .args(["tree", "--prefix", "none"])
            .current_dir(self.get_path().abs().join("vendor").join(crate_name))
            .run_quiet_and_expect_success()?;
        for line in from_utf8(&output.stdout)?.lines() {
            deps.insert(line.split_once(' ').unwrap().0.to_string());
        }
        Ok(deps)
    }
    pub fn regenerate_crate_list(&self) -> Result<()> {
        write(self.path.join("crate-list.txt")?, format!("{}\n", self.deps()?.keys().join("\n")))?;
        Ok(())
    }
    pub fn read_crate_list(&self) -> Result<Vec<String>> {
        let mut lines = Vec::new();
        for line in read(self.path.join("crate-list.txt")?)?.lines() {
            lines.push(line?);
        }
        Ok(lines)
    }
}
