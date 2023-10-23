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

//! Code for dealing with legacy cargo2android.json config files.

use super::{default_apex_available, default_true, PackageConfig};
use crate::renamed_module;
use anyhow::{anyhow, bail, Result};
use serde::Deserialize;
use std::collections::BTreeMap;
use std::path::PathBuf;

/// A legacy `cargo2android.json` configuration.
#[derive(Deserialize)]
#[serde(deny_unknown_fields, rename_all = "kebab-case")]
pub struct Config {
    #[serde(default)]
    add_module_block: Option<PathBuf>,
    #[serde(default)]
    add_toplevel_block: Option<PathBuf>,
    #[serde(default)]
    alloc: bool,
    #[serde(default)]
    apex_available: Vec<String>,
    #[serde(default)]
    cfg_blocklist: Vec<String>,
    #[serde(default)]
    dep_suffixes: BTreeMap<String, String>,
    #[allow(unused)] // Deprecated option.
    #[serde(default)]
    dependencies: bool,
    #[serde(default)]
    dependency_blocklist: Vec<String>,
    #[serde(default)]
    device: bool,
    #[serde(default)]
    features: Option<String>,
    #[serde(default)]
    force_rlib: bool,
    #[serde(default)]
    host_first_multilib: bool,
    min_sdk_version: Option<String>,
    #[serde(default)]
    no_host: bool,
    #[serde(default)]
    no_std: bool,
    patch: Option<PathBuf>,
    #[serde(default = "default_true")]
    product_available: bool,
    #[serde(default)]
    run: bool,
    #[serde(default)]
    test_blocklist: Vec<String>,
    #[serde(default)]
    test_data: Vec<String>,
    #[serde(default)]
    tests: bool,
    #[serde(default = "default_true")]
    vendor_available: bool,
}

impl Config {
    /// Converts this configuration to the equivalent `cargo_embargo` configuration.
    pub fn to_embargo(&self, package_name: &str, run_cargo: bool) -> Result<super::Config> {
        if !self.run {
            bail!("run was not true");
        }

        let features = self.features.as_ref().map(|features| {
            if features.is_empty() {
                Vec::new()
            } else {
                features.split(',').map(ToOwned::to_owned).collect()
            }
        });
        let mut test_data: BTreeMap<String, Vec<String>> = BTreeMap::new();
        for entry in &self.test_data {
            let (test_name, data) = entry
                .split_once('=')
                .ok_or_else(|| anyhow!("Invalid test-data entry {}", entry))?;
            test_data.entry(test_name.to_owned()).or_default().push(data.to_owned());
        }
        let dep_blocklist = self
            .dependency_blocklist
            .iter()
            .map(|package_name| package_to_library_name(package_name))
            .collect();
        let module_blocklist = self
            .test_blocklist
            .iter()
            .map(|test_filename| test_filename_to_module_name(package_name, test_filename))
            .collect();
        let module_name_overrides = self
            .dep_suffixes
            .iter()
            .map(|(dependency, suffix)| {
                let module_name = package_to_library_name(dependency);
                let with_suffix = format!("{}{}", module_name, suffix);
                (module_name, with_suffix)
            })
            .collect();
        let package_config = PackageConfig {
            add_module_block: self.add_module_block.clone(),
            add_toplevel_block: self.add_toplevel_block.clone(),
            alloc: self.alloc,
            device_supported: self.device,
            force_rlib: self.force_rlib,
            host_supported: !self.no_host,
            host_first_multilib: self.host_first_multilib,
            dep_blocklist,
            no_std: self.no_std,
            patch: self.patch.clone(),
            test_data,
            ..Default::default()
        };
        let mut package = BTreeMap::new();
        // Skip package config if everything matches the defaults.
        if package_config != Default::default() {
            package.insert(package_name.to_owned(), package_config);
        }
        let apex_available = if self.apex_available.is_empty() {
            default_apex_available()
        } else {
            self.apex_available.clone()
        };
        let config = super::Config {
            tests: self.tests,
            features,
            apex_available,
            cfg_blocklist: self.cfg_blocklist.clone(),
            product_available: self.product_available,
            vendor_available: self.vendor_available,
            min_sdk_version: self.min_sdk_version.clone(),
            module_blocklist,
            module_name_overrides,
            package,
            run_cargo,
            ..Default::default()
        };
        Ok(config)
    }
}

fn package_to_library_name(package_name: &str) -> String {
    let module_name = format!("lib{}", package_name);
    renamed_module(&module_name).to_owned()
}

fn test_filename_to_module_name(package_name: &str, test_filename: &str) -> String {
    format!("{}_test_{}", package_name, test_filename.replace('/', "_").replace(".rs", ""))
}
