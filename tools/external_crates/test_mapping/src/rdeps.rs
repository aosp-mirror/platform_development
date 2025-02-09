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

//! Find reverse dependencies of Rust crates by searching blueprint files
//! for rustlibs.

use std::{
    collections::{BTreeSet, HashMap},
    path::{Path, PathBuf},
    process::Command,
    str::from_utf8,
    sync::{LazyLock, Mutex},
};

use android_bp::BluePrint;

use crate::{blueprint::RustDeps, Error};

#[derive(Clone)]
pub(crate) struct ReverseDeps {
    // Mapping from Rust build rule target name => list of paths that depend on it.
    rdeps: HashMap<String, BTreeSet<String>>,
}

impl ReverseDeps {
    /// Returns a reverse dependency lookup for the Android source repo
    /// at the specified absolute path. Each lookup is created once
    /// and cached.
    pub fn for_repo(repo_root: &Path) -> ReverseDeps {
        static RDEPS: LazyLock<Mutex<HashMap<PathBuf, ReverseDeps>>> =
            LazyLock::new(|| Mutex::new(HashMap::new()));

        RDEPS
            .lock()
            .unwrap()
            .entry(repo_root.to_path_buf())
            .or_insert_with(|| ReverseDeps::grep_and_parse(repo_root).unwrap())
            .clone()
    }
    /// Get the paths that depend on a rust library.
    pub fn get(&self, name: &str) -> Option<&BTreeSet<String>> {
        self.rdeps.get(name)
    }
    fn grep_and_parse<P: Into<PathBuf>>(repo_root: P) -> Result<ReverseDeps, Error> {
        let repo_root = repo_root.into();
        // Empirically, TEST_MAPPING files for 3rd party crates only
        // have imports from external, packages, system, and tools.
        let output = Command::new("grep")
            .args([
                "-r",
                "-l",
                "--include=*.bp",
                "rustlibs",
                "external",
                "packages",
                "system",
                "tools",
            ])
            .current_dir(&repo_root)
            .output()?;
        let stdout = from_utf8(&output.stdout)?;
        let mut rdeps = HashMap::new();
        for line in stdout.lines() {
            if EXCLUDED_PATHS.iter().any(|excluded| line.starts_with(excluded)) {
                continue;
            }
            let (dir, _) = line.rsplit_once('/').ok_or(Error::GrepParseError(line.to_string()))?;
            if let Ok(bp) = BluePrint::from_file(repo_root.join(line)) {
                for rustlib in bp.rust_deps() {
                    rdeps.entry(rustlib).or_insert(BTreeSet::new()).insert(dir.to_string());
                }
            }
        }
        Ok(ReverseDeps { rdeps })
    }
}

// Originally taken from update_crate_tests.py, but of the values in there, only external/crosvm
// seems to exist.
static EXCLUDED_PATHS: LazyLock<Vec<&'static str>> =
    LazyLock::new(|| vec!["external/crosvm/", "development/tools/cargo_embargo/testdata/"]);
