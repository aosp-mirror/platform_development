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
    fs::{create_dir, write},
    path::{Path, PathBuf},
    process::Command,
};

use anyhow::{anyhow, Context, Result};
use serde::Serialize;
use tinytemplate::TinyTemplate;

use crate::{ensure_exists_and_empty, NamedAndVersioned};

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
    // Absolute path to pseudo-crate.
    path: PathBuf,
}

impl PseudoCrate {
    pub fn new<P: Into<PathBuf>>(path: P) -> PseudoCrate {
        PseudoCrate { path: path.into() }
    }
    pub fn init<'a>(
        &self,
        crates: impl Iterator<Item = &'a (impl NamedAndVersioned + 'a)>,
    ) -> Result<()> {
        if self.path.exists() {
            return Err(anyhow!(
                "Can't init pseudo-crate because {} already exists",
                self.path.display()
            ));
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
                    version: if krate.name() == "remove_dir_all"
                        && krate.version().to_string() == "0.7.1"
                    {
                        "0.7.0".to_string()
                    } else {
                        krate.version().to_string()
                    },
                });
            }
        }

        let mut tt = TinyTemplate::new();
        tt.add_template("cargo_toml", CARGO_TOML_TEMPLATE)?;
        let cargo_toml = self.path.join("Cargo.toml");
        write(&cargo_toml, tt.render("cargo_toml", &CargoToml { deps })?)?;

        create_dir(self.path.join("src")).context("Failed to create src dir")?;
        write(self.path.join("src/lib.rs"), "// Nothing").context("Failed to create src/lib.rs")?;

        self.vendor()

        // TODO: Run "cargo deny"
    }
    pub fn get_path(&self) -> &Path {
        self.path.as_path()
    }
    pub fn add(&self, krate: &impl NamedAndVersioned) -> Result<()> {
        let status = Command::new("cargo")
            .args(["add", format!("{}@={}", krate.name(), krate.version()).as_str()])
            .current_dir(&self.path)
            .spawn()
            .context("Failed to spawn 'cargo add'")?
            .wait()
            .context("Failed to wait on 'cargo add'")?;
        if !status.success() {
            return Err(anyhow!("Failed to run 'cargo add {}@{}'", krate.name(), krate.version()));
        }
        Ok(())
    }
    pub fn vendor(&self) -> Result<()> {
        let status = Command::new("cargo")
            .args(["vendor"])
            .current_dir(&self.path)
            .spawn()
            .context("Failed to spawn 'cargo vendor'")?
            .wait()
            .context("Failed to wait on 'cargo vendor'")?;
        if !status.success() {
            return Err(anyhow!("Failed to run 'cargo vendor'",));
        }
        Ok(())
    }
}
