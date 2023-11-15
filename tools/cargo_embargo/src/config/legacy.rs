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

use super::{
    add_defaults_to_variant, default_apex_available, default_true, PackageConfig,
    PackageVariantConfig,
};
use crate::renamed_module;
use anyhow::{anyhow, bail, Context, Result};
use serde::Deserialize;
use serde_json::{Map, Value};
use std::collections::BTreeMap;
use std::path::PathBuf;

/// A legacy `cargo2android.json` configuration.
#[derive(Debug, Default, Deserialize, Eq, PartialEq)]
#[serde(deny_unknown_fields, rename_all = "kebab-case")]
pub struct Config {
    #[serde(default)]
    add_toplevel_block: Option<PathBuf>,
    #[allow(unused)] // Deprecated option.
    #[serde(default)]
    dependencies: bool,
    patch: Option<PathBuf>,
    #[serde(default)]
    run: bool,
    variants: Vec<VariantConfig>,
}

impl Config {
    /// Names of all fields in [`Config`] other than `variants` (which is treated specially).
    const FIELD_NAMES: [&str; 4] = ["add-toplevel-block", "dependencies", "patch", "run"];

    /// Parses an instance of this config from a string of JSON.
    pub fn from_json_str(json_str: &str) -> Result<Self> {
        // First parse into untyped map.
        let mut config: Map<String, Value> =
            serde_json::from_str(json_str).context("failed to parse legacy config")?;

        // Flatten variants. First, get the variants from the config file.
        let mut variants = match config.remove("variants") {
            Some(Value::Array(v)) => v,
            Some(_) => bail!("Failed to parse legacy config: variants is not an array"),
            None => {
                // There are no variants, so just put everything into a single variant.
                vec![Value::Object(Map::new())]
            }
        };
        // Set default values in variants from top-level config.
        for variant in &mut variants {
            let variant = variant
                .as_object_mut()
                .context("Failed to parse legacy config: variant is not an object")?;
            add_defaults_to_variant(variant, &config, &Self::FIELD_NAMES);
        }
        // Remove other entries from the top-level config, and put variants back.
        config.retain(|key, _| Self::FIELD_NAMES.contains(&key.as_str()));
        config.insert("variants".to_string(), Value::Array(variants));

        // Parse into `Config` struct.
        serde_json::from_value(Value::Object(config)).context("failed to parse legacy config")
    }

    /// Converts this configuration to the equivalent `cargo_embargo` configuration.
    pub fn to_embargo(&self, package_name: &str, run_cargo: bool) -> Result<super::Config> {
        if !self.run {
            bail!("run was not true");
        }

        let variants = self
            .variants
            .iter()
            .map(|variant| variant.to_embargo(package_name, run_cargo))
            .collect::<Result<_>>()?;

        let package_config = PackageConfig {
            add_toplevel_block: self.add_toplevel_block.clone(),
            patch: self.patch.clone(),
        };
        let mut package = BTreeMap::new();
        // Skip package config if everything matches the defaults.
        if package_config != Default::default() {
            package.insert(package_name.to_owned(), package_config);
        }

        Ok(super::Config { variants, package })
    }
}

/// Legacy `cargo2android.json` configuration for a particular variant.
#[derive(Debug, Deserialize, Eq, PartialEq)]
#[serde(deny_unknown_fields, rename_all = "kebab-case")]
pub struct VariantConfig {
    #[serde(default)]
    add_module_block: Option<PathBuf>,
    #[serde(default)]
    alloc: bool,
    #[serde(default)]
    apex_available: Vec<String>,
    #[serde(default)]
    cfg_blocklist: Vec<String>,
    #[serde(default)]
    dep_suffixes: BTreeMap<String, String>,
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
    name_suffix: Option<String>,
    #[serde(default)]
    no_host: bool,
    #[serde(default)]
    no_std: bool,
    #[serde(default = "default_true")]
    product_available: bool,
    #[serde(default)]
    suffix: Option<String>,
    #[serde(default)]
    test_blocklist: Vec<String>,
    #[serde(default)]
    test_data: Vec<String>,
    #[serde(default)]
    tests: bool,
    #[serde(default = "default_true")]
    vendor_available: bool,
}

impl Default for VariantConfig {
    fn default() -> Self {
        Self {
            add_module_block: None,
            alloc: false,
            apex_available: Default::default(),
            cfg_blocklist: Default::default(),
            dep_suffixes: Default::default(),
            dependency_blocklist: Default::default(),
            device: false,
            features: None,
            force_rlib: false,
            host_first_multilib: false,
            min_sdk_version: None,
            name_suffix: None,
            no_host: false,
            no_std: false,
            product_available: true,
            suffix: None,
            test_blocklist: Default::default(),
            test_data: Default::default(),
            tests: false,
            vendor_available: true,
        }
    }
}

impl VariantConfig {
    /// Converts this variant configuration to the equivalent `cargo_embargo` configuration.
    pub fn to_embargo(&self, package_name: &str, run_cargo: bool) -> Result<super::VariantConfig> {
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
        let mut module_name_overrides = self
            .dep_suffixes
            .iter()
            .map(|(dependency, suffix)| {
                let module_name = package_to_library_name(dependency);
                let with_suffix = format!("{}{}", module_name, suffix);
                (module_name, with_suffix)
            })
            .collect::<BTreeMap<_, _>>();
        let suffix =
            self.suffix.clone().unwrap_or_default() + &self.name_suffix.clone().unwrap_or_default();
        if !suffix.is_empty() {
            let module_name = package_to_library_name(&package_name.replace('-', "_"));
            let with_suffix = format!("{}{}", module_name, suffix);
            module_name_overrides.insert(module_name, with_suffix);
        }
        let package_config = PackageVariantConfig {
            add_module_block: self.add_module_block.clone(),
            alloc: self.alloc,
            device_supported: self.device,
            force_rlib: self.force_rlib,
            host_supported: !self.no_host,
            host_first_multilib: self.host_first_multilib,
            dep_blocklist,
            no_std: self.no_std,
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
        let config = super::VariantConfig {
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

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn variant_config() {
        let config = Config::from_json_str(
            r#"{
            "dependencies": true,
            "device": true,
            "run": true,
            "variants": [
              {
              },
              {
                "features": "",
                "force-rlib": true,
                "no-host": true,
                "no-std": true
              }
            ]
          }"#,
        )
        .unwrap();

        assert_eq!(
            config,
            Config {
                dependencies: true,
                run: true,
                variants: vec![
                    VariantConfig { device: true, ..Default::default() },
                    VariantConfig {
                        device: true,
                        features: Some("".to_string()),
                        force_rlib: true,
                        no_host: true,
                        no_std: true,
                        ..Default::default()
                    },
                ],
                ..Default::default()
            }
        );
    }
}
