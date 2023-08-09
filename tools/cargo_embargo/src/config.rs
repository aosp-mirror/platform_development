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

use serde::Deserialize;
use std::collections::BTreeMap;
use std::path::PathBuf;

fn default_apex_available() -> Vec<String> {
    vec!["//apex_available:platform".to_string(), "//apex_available:anyapex".to_string()]
}

fn default_true() -> bool {
    true
}

/// Options that apply to everything.
#[derive(Default, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct Config {
    /// Whether to output "rust_test" modules.
    #[serde(default)]
    pub tests: bool,
    /// Set of features to enable. If non-empty, disables the default crate features.
    #[serde(default)]
    pub features: Vec<String>,
    /// Whether to build with --workspace.
    #[serde(default)]
    pub workspace: bool,
    /// When workspace is enabled, list of --exclude crates.
    #[serde(default)]
    pub workspace_excludes: Vec<String>,
    /// Value to use for every generated module's "defaults" field.
    pub global_defaults: Option<String>,
    /// Value to use for every generated library module's "apex_available" field.
    #[serde(default = "default_apex_available")]
    pub apex_available: Vec<String>,
    /// Value to use for every generated library module's `product_available` field.
    #[serde(default = "default_true")]
    pub product_available: bool,
    /// Value to use for every generated library module's `vendor_available` field.
    #[serde(default = "default_true")]
    pub vendor_available: bool,
    /// Map of renames for modules. For example, if a "libfoo" would be generated and there is an
    /// entry ("libfoo", "libbar"), the generated module will be called "libbar" instead.
    ///
    /// Also, affects references to dependencies (e.g. in a "static_libs" list), even those outside
    /// the project being processed.
    #[serde(default)]
    pub module_name_overrides: BTreeMap<String, String>,
    /// Package specific config options.
    #[serde(default)]
    pub package: BTreeMap<String, PackageConfig>,
    /// Modules in this list will not be generated.
    #[serde(default)]
    pub module_blocklist: Vec<String>,
    /// Modules name => Soong "visibility" property.
    #[serde(default)]
    pub module_visibility: BTreeMap<String, Vec<String>>,
    /// Whether to run the cargo build and parse its output, rather than just figuring things out
    /// from the `cargo.metadata`.
    #[serde(default = "default_true")]
    pub run_cargo: bool,
}

/// Options that apply to everything in a package (i.e. everything associated with a particular
/// Cargo.toml file).
#[derive(Deserialize)]
#[serde(deny_unknown_fields)]
pub struct PackageConfig {
    /// Whether to compile for device. Defaults to true.
    #[serde(default = "default_true")]
    pub device_supported: bool,
    /// Whether to compile for host. Defaults to true.
    #[serde(default = "default_true")]
    pub host_supported: bool,
    /// Generate "rust_library_rlib" instead of "rust_library".
    #[serde(default)]
    pub force_rlib: bool,
    /// Whether to disable "unit_test" for "rust_test" modules.
    // TODO: Should probably be a list of modules or crates. A package might have a mix of unit and
    // integration tests.
    #[serde(default)]
    pub no_presubmit: bool,
    /// File with content to append to the end of the generated Android.bp.
    pub add_toplevel_block: Option<PathBuf>,
    /// File with content to append to the end of each generated module.
    pub add_module_block: Option<PathBuf>,
    /// Modules in this list will not be added as dependencies of generated modules.
    #[serde(default)]
    pub dep_blocklist: Vec<String>,
    /// Patch file to apply after Android.bp is generated.
    pub patch: Option<PathBuf>,
    /// Copy build.rs output to ./out/* and add a genrule to copy ./out/* to genrule output.
    /// For crates with code pattern:
    ///     include!(concat!(env!("OUT_DIR"), "/<some_file>.rs"))
    #[serde(default)]
    pub copy_out: bool,
}

impl Default for PackageConfig {
    fn default() -> Self {
        Self {
            device_supported: true,
            host_supported: true,
            force_rlib: false,
            no_presubmit: false,
            add_toplevel_block: None,
            add_module_block: None,
            dep_blocklist: Default::default(),
            patch: None,
            copy_out: false,
        }
    }
}
