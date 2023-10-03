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
use anyhow::{anyhow, bail, Result};
use serde::Deserialize;
use std::collections::BTreeMap;
use std::path::PathBuf;

/// A legacy `cargo2android.json` configuration.
#[derive(Deserialize)]
#[serde(deny_unknown_fields, rename_all = "kebab-case")]
pub struct Config {
    #[serde(default)]
    apex_available: Vec<String>,
    #[allow(unused)] // Deprecated option.
    #[serde(default)]
    dependencies: bool,
    #[serde(default)]
    dependency_blocklist: Vec<String>,
    #[serde(default)]
    device: bool,
    #[serde(default)]
    features: String,
    #[serde(default)]
    host_first_multilib: bool,
    min_sdk_version: Option<String>,
    #[serde(default)]
    no_host: bool,
    patch: Option<PathBuf>,
    #[serde(default = "default_true")]
    product_available: bool,
    #[serde(default)]
    run: bool,
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

        let features = if self.features.is_empty() {
            Vec::new()
        } else {
            self.features.split(',').map(ToOwned::to_owned).collect()
        };
        let mut test_data: BTreeMap<String, Vec<String>> = BTreeMap::new();
        for entry in &self.test_data {
            let (test_name, data) = entry
                .split_once('=')
                .ok_or_else(|| anyhow!("Invalid test-data entry {}", entry))?;
            test_data.entry(test_name.to_owned()).or_default().push(data.to_owned());
        }
        let package_config = PackageConfig {
            device_supported: self.device,
            host_supported: !self.no_host,
            host_first_multilib: self.host_first_multilib,
            dep_blocklist: self.dependency_blocklist.clone(),
            patch: self.patch.clone(),
            test_data,
            ..Default::default()
        };
        let apex_available = if self.apex_available.is_empty() {
            default_apex_available()
        } else {
            self.apex_available.clone()
        };
        let config = super::Config {
            tests: self.tests,
            features,
            apex_available,
            product_available: self.product_available,
            vendor_available: self.vendor_available,
            min_sdk_version: self.min_sdk_version.clone(),
            package: [(package_name.to_owned(), package_config)].into_iter().collect(),
            run_cargo,
            ..Default::default()
        };
        Ok(config)
    }
}
