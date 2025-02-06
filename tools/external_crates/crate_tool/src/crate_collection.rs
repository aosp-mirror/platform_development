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

use name_and_version::{NameAndVersion, NameAndVersionMap, NamedAndVersioned};
use rooted_path::RootedPath;

use std::path::{Path, PathBuf};

use anyhow::{anyhow, Result};
use walkdir::WalkDir;

use crate::{crate_type::Crate, CrateError};

use std::collections::BTreeMap;

#[derive(Debug)]
pub struct CrateCollection {
    crates: BTreeMap<NameAndVersion, Crate>,
    repo_root: PathBuf,
}

impl CrateCollection {
    pub fn new<P: Into<PathBuf>>(repo_root: P) -> CrateCollection {
        CrateCollection { crates: BTreeMap::new(), repo_root: repo_root.into() }
    }
    pub fn add_from(&mut self, path: impl AsRef<Path>) -> Result<()> {
        for entry_or_err in WalkDir::new(self.repo_root.join(path)) {
            let entry = entry_or_err?;
            if entry.file_name() == "Cargo.toml" {
                match Crate::from(RootedPath::new(
                    self.repo_root.clone(),
                    entry
                        .path()
                        .parent()
                        .ok_or(anyhow!("Failed to get parent of {}", entry.path().display()))?
                        .strip_prefix(&self.repo_root)?,
                )?) {
                    Ok(krate) => self.crates.insert_or_error(
                        NameAndVersion::new(krate.name().to_string(), krate.version().clone()),
                        krate,
                    )?,
                    Err(e) => match e.downcast_ref() {
                        Some(CrateError::VirtualCrate(_)) => (),
                        _ => return Err(e),
                    },
                };
            }
        }
        Ok(())
    }
    pub fn get(&self, nv: &dyn NamedAndVersioned) -> Option<&Crate> {
        self.crates.get(nv)
    }
    pub fn values(&self) -> impl Iterator<Item = &Crate> {
        self.crates.values()
    }
    pub fn get_versions(
        &self,
        crate_name: impl AsRef<str>,
    ) -> Box<dyn Iterator<Item = (&NameAndVersion, &Crate)> + '_> {
        self.crates.get_versions(crate_name.as_ref())
    }
    pub fn contains_crate(&self, crate_name: impl AsRef<str>) -> bool {
        self.crates.contains_name(crate_name.as_ref())
    }
}
