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

//! Code for reading configuration json files.
//!
//! These are usually called `cargo_embargo.json`.

pub mod legacy;

use serde::{Deserialize, Serialize};
use std::collections::BTreeMap;
use std::path::PathBuf;

fn default_apex_available() -> Vec<String> {
    vec!["//apex_available:platform".to_string(), "//apex_available:anyapex".to_string()]
}

fn is_default_apex_available(apex_available: &[String]) -> bool {
    apex_available == default_apex_available()
}

fn default_true() -> bool {
    true
}

fn is_true(value: &bool) -> bool {
    *value
}

fn is_false(value: &bool) -> bool {
    !*value
}

/// Options that apply to everything.
#[derive(Deserialize, Serialize)]
#[serde(deny_unknown_fields)]
pub struct Config {
    /// Whether to output "rust_test" modules.
    #[serde(default, skip_serializing_if = "is_false")]
    pub tests: bool,
    /// Set of features to enable. If non-empty, disables the default crate features.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub features: Vec<String>,
    /// Whether to build with --workspace.
    #[serde(default, skip_serializing_if = "is_false")]
    pub workspace: bool,
    /// When workspace is enabled, list of --exclude crates.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub workspace_excludes: Vec<String>,
    /// Value to use for every generated module's "defaults" field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub global_defaults: Option<String>,
    /// Value to use for every generated library module's "apex_available" field.
    #[serde(default = "default_apex_available", skip_serializing_if = "is_default_apex_available")]
    pub apex_available: Vec<String>,
    /// Value to use for every generated library module's `product_available` field.
    #[serde(default = "default_true", skip_serializing_if = "is_true")]
    pub product_available: bool,
    /// Value to use for every generated library module's `vendor_available` field.
    #[serde(default = "default_true", skip_serializing_if = "is_true")]
    pub vendor_available: bool,
    /// Minimum SDK version.
    pub min_sdk_version: Option<String>,
    /// Map of renames for modules. For example, if a "libfoo" would be generated and there is an
    /// entry ("libfoo", "libbar"), the generated module will be called "libbar" instead.
    ///
    /// Also, affects references to dependencies (e.g. in a "static_libs" list), even those outside
    /// the project being processed.
    #[serde(default, skip_serializing_if = "BTreeMap::is_empty")]
    pub module_name_overrides: BTreeMap<String, String>,
    /// Package specific config options.
    #[serde(default, skip_serializing_if = "BTreeMap::is_empty")]
    pub package: BTreeMap<String, PackageConfig>,
    /// Modules in this list will not be generated.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub module_blocklist: Vec<String>,
    /// Modules name => Soong "visibility" property.
    #[serde(default, skip_serializing_if = "BTreeMap::is_empty")]
    pub module_visibility: BTreeMap<String, Vec<String>>,
    /// Whether to run the cargo build and parse its output, rather than just figuring things out
    /// from the `cargo.metadata`.
    #[serde(default = "default_true", skip_serializing_if = "is_true")]
    pub run_cargo: bool,
}

impl Default for Config {
    fn default() -> Self {
        Self {
            tests: false,
            features: Default::default(),
            workspace: false,
            workspace_excludes: Default::default(),
            global_defaults: None,
            apex_available: default_apex_available(),
            product_available: true,
            vendor_available: true,
            min_sdk_version: None,
            module_name_overrides: Default::default(),
            package: Default::default(),
            module_blocklist: Default::default(),
            module_visibility: Default::default(),
            run_cargo: true,
        }
    }
}

/// Options that apply to everything in a package (i.e. everything associated with a particular
/// Cargo.toml file).
#[derive(Deserialize, Serialize)]
#[serde(deny_unknown_fields)]
pub struct PackageConfig {
    /// Whether to compile for device. Defaults to true.
    #[serde(default = "default_true", skip_serializing_if = "is_true")]
    pub device_supported: bool,
    /// Whether to compile for host. Defaults to true.
    #[serde(default = "default_true", skip_serializing_if = "is_true")]
    pub host_supported: bool,
    /// Add a `compile_multilib: "first"` property to host modules.
    #[serde(default, skip_serializing_if = "is_false")]
    pub host_first_multilib: bool,
    /// Generate "rust_library_rlib" instead of "rust_library".
    #[serde(default, skip_serializing_if = "is_false")]
    pub force_rlib: bool,
    /// Whether to disable "unit_test" for "rust_test" modules.
    // TODO: Should probably be a list of modules or crates. A package might have a mix of unit and
    // integration tests.
    #[serde(default, skip_serializing_if = "is_false")]
    pub no_presubmit: bool,
    /// File with content to append to the end of the generated Android.bp.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub add_toplevel_block: Option<PathBuf>,
    /// File with content to append to the end of each generated module.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub add_module_block: Option<PathBuf>,
    /// Modules in this list will not be added as dependencies of generated modules.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub dep_blocklist: Vec<String>,
    /// Patch file to apply after Android.bp is generated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub patch: Option<PathBuf>,
    /// Copy build.rs output to ./out/* and add a genrule to copy ./out/* to genrule output.
    /// For crates with code pattern:
    ///     include!(concat!(env!("OUT_DIR"), "/<some_file>.rs"))
    #[serde(default, skip_serializing_if = "is_false")]
    pub copy_out: bool,
    /// Add the given files to the given tests' `data` property. The key is the test source filename
    /// relative to the crate root.
    #[serde(default, skip_serializing_if = "BTreeMap::is_empty")]
    pub test_data: BTreeMap<String, Vec<String>>,
}

impl Default for PackageConfig {
    fn default() -> Self {
        Self {
            device_supported: true,
            host_supported: true,
            host_first_multilib: false,
            force_rlib: false,
            no_presubmit: false,
            add_toplevel_block: None,
            add_module_block: None,
            dep_blocklist: Default::default(),
            patch: None,
            copy_out: false,
            test_data: Default::default(),
        }
    }
}
