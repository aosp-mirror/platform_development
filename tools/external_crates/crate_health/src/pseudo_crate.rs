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
    collections::BTreeMap,
    fs::{create_dir, write},
    process::Command,
    str::from_utf8,
};

use anyhow::{anyhow, Context, Result};
use serde::Serialize;
use tinytemplate::TinyTemplate;

use crate::{ensure_exists_and_empty, NamedAndVersioned, RepoPath, RunQuiet};

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
    path: RepoPath,
}

impl PseudoCrate {
    pub fn new(path: RepoPath) -> PseudoCrate {
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
        ensure_exists_and_empty(&self.path.abs())?;

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
        let cargo_toml = self.path.join(&"Cargo.toml").abs();
        write(&cargo_toml, tt.render("cargo_toml", &CargoToml { deps })?)?;

        create_dir(self.path.join(&"src").abs()).context("Failed to create src dir")?;
        write(self.path.join(&"src/lib.rs").abs(), "// Nothing")
            .context("Failed to create src/lib.rs")?;

        self.vendor()
    }
    pub fn get_path(&self) -> &RepoPath {
        &self.path
    }
    pub fn add(&self, krate: &impl NamedAndVersioned) -> Result<()> {
        Command::new("cargo")
            .args(["add", format!("{}@={}", krate.name(), krate.version()).as_str()])
            .current_dir(self.path.abs())
            .run_quiet_and_expect_success()?;
        Ok(())
    }
    pub fn remove(&self, krate: &impl NamedAndVersioned) -> Result<()> {
        Command::new("cargo")
            .args(["remove", krate.name()])
            .current_dir(self.path.abs())
            .run_quiet_and_expect_success()?;
        Ok(())
    }
    pub fn vendor(&self) -> Result<()> {
        Command::new("cargo")
            .args(["vendor"])
            .current_dir(self.path.abs())
            .run_quiet_and_expect_success()?;
        Ok(())
    }
    pub fn deps(&self) -> Result<BTreeMap<String, String>> {
        let output = Command::new("cargo")
            .args(["tree", "--depth=1", "--prefix=none"])
            .current_dir(self.path.abs())
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
}
