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
use thiserror::Error;

#[derive(Error, Debug)]
pub enum TestMappingError {
    #[error("Blueprint file {0} not found")]
    BlueprintNotFound(PathBuf),
    #[error("Blueprint parse error: {0}")]
    BlueprintParseError(String),
    #[error("Blueprint rule has no name")]
    RuleWithoutName(String),

    #[error("Error stripping JSON comments")]
    StripJsonCommentsError(io::Error),
    #[error("JSON parse error")]
    JsonParseError(#[from] serde_json::Error),

    #[error("IO error")]
    IoError(#[from] io::Error),
    #[error("Command output not UTF-8")]
    Utf8Error(#[from] Utf8Error),
    #[error("Failed to split grep output line {0} on '/'")]
    GrepParseError(String),
}

#[derive(Debug)]
pub struct TestMapping {
    path: RootedPath,
    json: TestMappingJson,
    bp: BluePrint,
}

impl TestMapping {
    /// Read the TEST_MAPPING file in the specified directory.
    /// If there is no TEST_MAPPING file, a default value is returned.
    /// Also reads the Android.bp in the directory, because we need that
    /// information to update the TEST_MAPPING.
    pub fn read(path: RootedPath) -> Result<TestMapping, TestMappingError> {
        let bpfile = path.join("Android.bp").unwrap();
        if !bpfile.abs().exists() {
            return Err(TestMappingError::BlueprintNotFound(bpfile.rel().to_path_buf()));
        }
        let bp = BluePrint::from_file(bpfile)
            .map_err(|e: String| TestMappingError::BlueprintParseError(e))?;
        let test_mapping_path = path.join("TEST_MAPPING").unwrap();
        let json = if path.abs().exists() {
            TestMappingJson::parse(read_to_string(test_mapping_path)?)?
        } else {
            TestMappingJson::default()
        };
        Ok(TestMapping { path, json, bp })
    }
    /// Write the TEST_MAPPING file.
    pub fn write(&self) -> Result<(), TestMappingError> {
        if self.json.is_empty() && self.test_mapping().abs().exists() {
            remove_file(self.test_mapping())?;
        } else {
            let mut contents = serde_json::to_string_pretty(&self.json)?;
            contents.push('\n');
            write(self.test_mapping(), contents)?;
        }
        Ok(())
    }
    /// Update the presubmit and presubmit_rust fields to the
    /// set of test targets in the Android.bp file.
    /// Since adding new tests directly to presubmits is discouraged,
    /// It's preferable to use add_new_tests_to_postsubmit and
    /// convert_postsubmit_tests instead.
    pub fn update_presubmits(&mut self) -> Result<(), TestMappingError> {
        self.json.set_presubmits(&self.bp.rust_tests()?);
        Ok(())
    }
    /// Add tests that aren't already mentioned in TEST_MAPPING
    /// as post-submit tests.
    pub fn add_new_tests_to_postsubmit(&mut self) -> Result<bool, TestMappingError> {
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
    pub fn update_imports(&mut self) -> Result<(), TestMappingError> {
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
    fn libs(&self) -> Result<Vec<String>, TestMappingError> {
        let mut libs = Vec::new();
        for module in &self.bp.modules {
            if matches!(
                module.typ.as_str(),
                "rust_library" | "rust_library_rlib" | "rust_library_host" | "rust_proc_macro"
            ) {
                libs.push(
                    module
                        .get_string("name")
                        .ok_or(TestMappingError::RuleWithoutName(module.typ.clone()))?
                        .clone(),
                );
            }
        }
        Ok(libs)
    }
}
