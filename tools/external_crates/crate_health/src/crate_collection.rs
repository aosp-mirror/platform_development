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
use name_and_version_proc_macros::NameAndVersionMap;
use rooted_path::RootedPath;

use std::{
    collections::HashSet,
    path::{Path, PathBuf},
};

use anyhow::Result;
use semver::Version;
use walkdir::WalkDir;

use crate::{Crate, CrateError};

use std::collections::BTreeMap;

#[derive(NameAndVersionMap)]
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
                    entry.path().strip_prefix(&self.repo_root)?,
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
}
