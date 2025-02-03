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

//! Read, update, and write TEST_MAPPING files.

mod blueprint;
mod json;
mod rdeps;

use std::{
    collections::BTreeSet,
    fs::{read_to_string, remove_file, write},
    io,
    path::PathBuf,
    str::Utf8Error,
};

use android_bp::BluePrint;
use blueprint::RustTests;
use json::{TestMappingJson, TestMappingPath};
use rdeps::ReverseDeps;
use rooted_path::RootedPath;

/// Error types for the 'test_mapping' crate.
#[derive(thiserror::Error, Debug)]
pub enum Error {
    /// Blueprint file not found
    #[error("Blueprint file {0} not found")]
    BlueprintNotFound(PathBuf),
    /// Blueprint parse error
    #[error("Blueprint parse error: {0}")]
    BlueprintParseError(String),
    /// Blueprint rule has no name
    #[error("Blueprint rule has no name")]
    RuleWithoutName(String),

    /// Error stripping JSON comments
    #[error("Error stripping JSON comments: {0}")]
    StripJsonCommentsError(io::Error),
    /// JSON parse error
    #[error(transparent)]
    JsonParseError(#[from] serde_json::Error),

    /// I/O Error
    #[error(transparent)]
    IoError(#[from] io::Error),
    /// Command output not UTF-8
    #[error(transparent)]
    Utf8Error(#[from] Utf8Error),
    /// Failed to split grep output on '/'
    #[error("Failed to split grep output line {0} on '/'")]
    GrepParseError(String),
}

/// A parsed TEST_MAPPING file
#[derive(Debug)]
pub struct TestMapping {
    /// The path of the crate directory.
    path: RootedPath,
    /// The contents of TEST_MAPPING
    json: TestMappingJson,
    /// The parsed Android.bp file
    bp: BluePrint,
}

impl TestMapping {
    /// Read the TEST_MAPPING file in the specified directory.
    /// If there is no TEST_MAPPING file, a default value is returned.
    /// Also reads the Android.bp in the directory, because we need that
    /// information to update the TEST_MAPPING.
    pub fn read(path: RootedPath) -> Result<TestMapping, Error> {
        let bpfile = path.join("Android.bp").unwrap();
        if !bpfile.abs().exists() {
            return Err(Error::BlueprintNotFound(bpfile.rel().to_path_buf()));
        }
        let bp = BluePrint::from_file(bpfile).map_err(|e: String| Error::BlueprintParseError(e))?;
        let test_mapping_path = path.join("TEST_MAPPING").unwrap();
        let json = if test_mapping_path.abs().exists() {
            TestMappingJson::parse(read_to_string(test_mapping_path)?)?
        } else {
            TestMappingJson::default()
        };
        Ok(TestMapping { path, json, bp })
    }
    /// Write the TEST_MAPPING file.
    pub fn write(&self) -> Result<(), Error> {
        if self.json.is_empty() && self.test_mapping().abs().exists() {
            remove_file(self.test_mapping())?;
        } else {
            let mut contents = serde_json::to_string_pretty(&self.json)?;
            contents.push('\n');
            write(self.test_mapping(), contents)?;
        }
        Ok(())
    }
    /// Remove tests from TEST_MAPPING that are no longer in the
    /// Android.bp file
    pub fn remove_unknown_tests(&mut self) -> Result<bool, Error> {
        Ok(self.json.remove_unknown_tests(&self.bp.rust_tests()?))
    }
    /// Update the presubmit and presubmit_rust fields to the
    /// set of test targets in the Android.bp file.
    /// Since adding new tests directly to presubmits is discouraged,
    /// It's preferable to use add_new_tests_to_postsubmit and
    /// convert_postsubmit_tests instead.
    pub fn update_presubmits(&mut self) -> Result<(), Error> {
        self.json.set_presubmits(&self.bp.rust_tests()?);
        Ok(())
    }
    /// Add tests that aren't already mentioned in TEST_MAPPING
    /// as post-submit tests.
    pub fn add_new_tests_to_postsubmit(&mut self) -> Result<bool, Error> {
        Ok(self.json.add_new_tests_to_postsubmit(&self.bp.rust_tests()?))
    }
    /// Convert post-submit tests to run at presubmit.
    pub fn convert_postsubmit_tests(&mut self) -> bool {
        self.json.convert_postsubmit_tests()
    }
    /// Fix the import paths of TEST_MAPPING files to refer to the monorepo.
    /// Essentially, replace external/rust/crates with external/rust/android-crates-io/crates.
    pub fn fix_import_paths(&mut self) -> bool {
        let mut changed = false;
        for import in self.json.imports.iter_mut() {
            if import.path.starts_with("external/rust/crates") {
                let new_path = import
                    .path
                    .replace("external/rust/crates", "external/rust/android-crates-io/crates");
                if self.path.with_same_root(new_path.clone()).unwrap().abs().exists() {
                    import.path = new_path;
                    changed = true;
                }
            }
        }
        changed
    }
    /// Update the imports section of TEST_MAPPING to contain all the
    /// paths that depend on this crate.
    pub fn update_imports(&mut self) -> Result<(), Error> {
        let all_rdeps = ReverseDeps::for_repo(self.path.root());
        let mut rdeps = BTreeSet::new();
        for lib in self.libs()? {
            if let Some(paths) = all_rdeps.get(lib.as_str()) {
                rdeps.append(&mut paths.clone());
            }
        }
        let self_path = self.path.rel().to_str().unwrap();
        self.json.imports = rdeps
            .iter()
            .filter(|path| path.as_str() != self_path)
            .map(|t| TestMappingPath { path: t.to_string() })
            .collect();
        Ok(())
    }
    fn test_mapping(&self) -> RootedPath {
        self.path.join("TEST_MAPPING").unwrap()
    }
    fn libs(&self) -> Result<Vec<String>, Error> {
        let mut libs = Vec::new();
        for module in &self.bp.modules {
            if matches!(
                module.typ.as_str(),
                "rust_library" | "rust_library_rlib" | "rust_library_host" | "rust_proc_macro"
            ) {
                libs.push(
                    module
                        .get_string("name")
                        .ok_or(Error::RuleWithoutName(module.typ.clone()))?
                        .clone(),
                );
            }
        }
        Ok(libs)
    }
}
