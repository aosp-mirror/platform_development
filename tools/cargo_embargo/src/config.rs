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

//! Code for reading configuration json files, usually called `cargo_embargo.json`.
//!
//! A single configuration file may cover several Rust packages under its directory tree, and may
//! have multiple "variants". A variant is a particular configuration for building a set of
//! packages, such as what feature flags to enable. Multiple variants are most often used to build
//! both a std variant of a library and a no_std variant. Each variant generates one or more modules
//! for each package, usually distinguished by a suffix.
//!
//! The [`Config`] struct has a map of `PackageConfig`s keyed by package name (for options that
//! apply across all variants of a package), and a vector of `VariantConfig`s. There must be at
//! least one variant for the configuration to generate any output. Each `VariantConfig` has the
//! options that apply to that variant across all packages, and then a map of
//! `PackageVariantConfig`s for options specific to a particular package of the variant.

use anyhow::{bail, Context, Result};
use serde::{Deserialize, Serialize};
use serde_json::{Map, Value};
use std::collections::BTreeMap;
use std::path::{Path, PathBuf};

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
#[derive(Clone, Debug, Deserialize, Eq, PartialEq, Serialize)]
#[serde(deny_unknown_fields)]
pub struct Config {
    pub variants: Vec<VariantConfig>,
    /// Package specific config options across all variants.
    #[serde(default, skip_serializing_if = "BTreeMap::is_empty")]
    pub package: BTreeMap<String, PackageConfig>,
}

/// Inserts entries from `defaults` into `variant` if neither it nor `ignored_fields` contain
/// matching keys.
fn add_defaults_to_variant(
    variant: &mut Map<String, Value>,
    defaults: &Map<String, Value>,
    ignored_fields: &[&str],
) {
    for (key, value) in defaults {
        if !ignored_fields.contains(&key.as_str()) && !variant.contains_key(key) {
            variant.insert(key.to_owned(), value.to_owned());
        }
    }
}

impl Config {
    /// Names of all fields in [`Config`] other than `variants` (which is treated specially).
    const FIELD_NAMES: [&'static str; 1] = ["package"];

    /// Parses an instance of this config from the given JSON file.
    pub fn from_file(filename: &Path) -> Result<Self> {
        let json_string = std::fs::read_to_string(filename)
            .with_context(|| format!("failed to read file: {:?}", filename))?;
        Self::from_json_str(&json_string)
    }

    /// Parses an instance of this config from a string of JSON.
    pub fn from_json_str(json_str: &str) -> Result<Self> {
        // Ignore comments.
        let json_str: String =
            json_str.lines().filter(|l| !l.trim_start().starts_with("//")).collect();
        // First parse into untyped map.
        let mut config: Map<String, Value> =
            serde_json::from_str(&json_str).context("failed to parse config")?;

        // Flatten variants. First, get the variants from the config file.
        let mut variants = match config.remove("variants") {
            Some(Value::Array(v)) => v,
            Some(_) => bail!("Failed to parse config: variants is not an array"),
            None => {
                // There are no variants, so just put everything into a single variant.
                vec![Value::Object(Map::new())]
            }
        };
        // Set default values in variants from top-level config.
        for variant in &mut variants {
            let variant = variant
                .as_object_mut()
                .context("Failed to parse config: variant is not an object")?;
            add_defaults_to_variant(variant, &config, &Config::FIELD_NAMES);

            if let Some(packages) = config.get("package") {
                // Copy package entries across.
                let variant_packages = variant
                    .entry("package")
                    .or_insert_with(|| Map::new().into())
                    .as_object_mut()
                    .context("Failed to parse config: variant package is not an object")?;
                for (package_name, package_config) in packages
                    .as_object()
                    .context("Failed to parse config: package is not an object")?
                {
                    let variant_package = variant_packages
                        .entry(package_name)
                        .or_insert_with(|| Map::new().into())
                        .as_object_mut()
                        .context(
                            "Failed to parse config: variant package config is not an object",
                        )?;
                    add_defaults_to_variant(
                        variant_package,
                        package_config
                            .as_object()
                            .context("Failed to parse config: package is not an object")?,
                        &PackageConfig::FIELD_NAMES,
                    );
                }
            }
        }
        // Remove other entries from the top-level config, and put variants back.
        config.retain(|key, _| Self::FIELD_NAMES.contains(&key.as_str()));
        if let Some(package) = config.get_mut("package") {
            for value in package
                .as_object_mut()
                .context("Failed to parse config: package is not an object")?
                .values_mut()
            {
                let package_config = value
                    .as_object_mut()
                    .context("Failed to parse config: package is not an object")?;
                package_config.retain(|key, _| PackageConfig::FIELD_NAMES.contains(&key.as_str()))
            }
        }
        config.insert("variants".to_string(), Value::Array(variants));

        // Parse into `Config` struct.
        serde_json::from_value(Value::Object(config)).context("failed to parse config")
    }

    /// Serializes an instance of this config to a string of pretty-printed JSON.
    pub fn to_json_string(&self) -> Result<String> {
        // First convert to an untyped map.
        let Value::Object(mut config) = serde_json::to_value(self)? else {
            panic!("Config wasn't a map.");
        };

        // Factor out common options which are set for all variants.
        let Value::Array(mut variants) = config.remove("variants").unwrap() else {
            panic!("variants wasn't an array.")
        };
        let mut packages = if let Some(Value::Object(packages)) = config.remove("package") {
            packages
        } else {
            Map::new()
        };
        for (key, value) in variants[0].as_object().unwrap() {
            if key == "package" {
                for (package_name, package_config) in value.as_object().unwrap() {
                    for (package_key, package_value) in package_config.as_object().unwrap() {
                        // Check whether all other variants have the same entry for the same package.
                        if variants[1..variants.len()].iter().all(|variant| {
                            if let Some(Value::Object(variant_packages)) =
                                variant.as_object().unwrap().get("package")
                            {
                                if let Some(Value::Object(variant_package_config)) =
                                    variant_packages.get(package_name)
                                {
                                    return variant_package_config.get(package_key)
                                        == Some(package_value);
                                }
                            }
                            false
                        }) {
                            packages
                                .entry(package_name)
                                .or_insert_with(|| Map::new().into())
                                .as_object_mut()
                                .unwrap()
                                .insert(package_key.to_owned(), package_value.to_owned());
                        }
                    }
                }
            } else {
                // Check whether all the other variants have the same entry.
                if variants[1..variants.len()]
                    .iter()
                    .all(|variant| variant.as_object().unwrap().get(key) == Some(value))
                {
                    // Add it to the top-level config.
                    config.insert(key.to_owned(), value.to_owned());
                }
            }
        }
        // Remove factored out common options from all variants.
        for key in config.keys() {
            for variant in &mut variants {
                variant.as_object_mut().unwrap().remove(key);
            }
        }
        // Likewise, remove package options factored out from variants.
        for (package_name, package_config) in &packages {
            for package_key in package_config.as_object().unwrap().keys() {
                for variant in &mut variants {
                    if let Some(Value::Object(variant_packages)) = variant.get_mut("package") {
                        if let Some(Value::Object(variant_package_config)) =
                            variant_packages.get_mut(package_name)
                        {
                            variant_package_config.remove(package_key);
                        }
                    }
                }
            }
        }
        // Remove any variant packages which are now empty.
        for variant in &mut variants {
            if let Some(Value::Object(variant_packages)) = variant.get_mut("package") {
                variant_packages
                    .retain(|_, package_config| !package_config.as_object().unwrap().is_empty());
                if variant_packages.is_empty() {
                    variant.as_object_mut().unwrap().remove("package");
                }
            }
        }
        // Put packages and variants back into the top-level config.
        if variants.len() > 1 || !variants[0].as_object().unwrap().is_empty() {
            config.insert("variants".to_string(), Value::Array(variants));
        }
        if !packages.is_empty() {
            config.insert("package".to_string(), Value::Object(packages));
        }

        // Serialise the map into a JSON string.
        serde_json::to_string_pretty(&config).context("failed to serialize config")
    }
}

#[derive(Clone, Debug, Deserialize, Eq, PartialEq, Serialize)]
#[serde(deny_unknown_fields)]
pub struct VariantConfig {
    /// Whether to output `rust_test` modules.
    #[serde(default, skip_serializing_if = "is_false")]
    pub tests: bool,
    /// Set of features to enable. If not set, uses the default crate features.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub features: Option<Vec<String>>,
    /// Whether to build with `--workspace`.
    #[serde(default, skip_serializing_if = "is_false")]
    pub workspace: bool,
    /// When workspace is enabled, list of `--exclude` crates.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub workspace_excludes: Vec<String>,
    /// Value to use for every generated module's `defaults` field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub global_defaults: Option<String>,
    /// Value to use for every generated library module's `apex_available` field.
    #[serde(default = "default_apex_available", skip_serializing_if = "is_default_apex_available")]
    pub apex_available: Vec<String>,
    /// Value to use for every generated library module's `native_bridge_supported` field.
    #[serde(default, skip_serializing_if = "is_false")]
    pub native_bridge_supported: bool,
    /// Value to use for every generated library module's `product_available` field.
    #[serde(default = "default_true", skip_serializing_if = "is_true")]
    pub product_available: bool,
    /// Value to use for every generated library module's `ramdisk_available` field.
    #[serde(default, skip_serializing_if = "is_false")]
    pub ramdisk_available: bool,
    /// Value to use for every generated library module's `recovery_available` field.
    #[serde(default, skip_serializing_if = "is_false")]
    pub recovery_available: bool,
    /// Value to use for every generated library module's `vendor_available` field.
    #[serde(default = "default_true", skip_serializing_if = "is_true")]
    pub vendor_available: bool,
    /// Value to use for every generated library module's `vendor_ramdisk_available` field.
    #[serde(default, skip_serializing_if = "is_false")]
    pub vendor_ramdisk_available: bool,
    /// Minimum SDK version.
    #[serde(skip_serializing_if = "Option::is_none")]
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
    pub package: BTreeMap<String, PackageVariantConfig>,
    /// `cfg` flags in this list will not be included.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub cfg_blocklist: Vec<String>,
    /// Extra `cfg` flags to enable in output modules.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub extra_cfg: Vec<String>,
    /// Modules in this list will not be generated.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub module_blocklist: Vec<String>,
    /// Modules name => Soong "visibility" property.
    #[serde(default, skip_serializing_if = "BTreeMap::is_empty")]
    pub module_visibility: BTreeMap<String, Vec<String>>,
    /// Whether to run the cargo build and parse its output, rather than just figuring things out
    /// from the cargo metadata.
    #[serde(default = "default_true", skip_serializing_if = "is_true")]
    pub run_cargo: bool,
    /// Generate Android build rules at Android.bp for this variant if true.
    #[serde(default = "default_true", skip_serializing_if = "is_true")]
    pub generate_androidbp: bool,
    /// Generate Trusty build rules at rules.mk and Android.bp if true.
    #[serde(default, skip_serializing_if = "is_false")]
    pub generate_rulesmk: bool,
}

impl Default for VariantConfig {
    fn default() -> Self {
        Self {
            tests: false,
            features: Default::default(),
            workspace: false,
            workspace_excludes: Default::default(),
            global_defaults: None,
            apex_available: default_apex_available(),
            native_bridge_supported: false,
            product_available: true,
            ramdisk_available: false,
            recovery_available: false,
            vendor_available: true,
            vendor_ramdisk_available: false,
            min_sdk_version: None,
            module_name_overrides: Default::default(),
            package: Default::default(),
            cfg_blocklist: Default::default(),
            extra_cfg: Default::default(),
            module_blocklist: Default::default(),
            module_visibility: Default::default(),
            run_cargo: true,
            generate_androidbp: true,
            generate_rulesmk: false,
        }
    }
}

/// Options that apply to everything in a package (i.e. everything associated with a particular
/// Cargo.toml file), for all variants.
#[derive(Clone, Debug, Default, Deserialize, Eq, PartialEq, Serialize)]
#[serde(deny_unknown_fields)]
pub struct PackageConfig {
    /// File with content to append to the end of the generated Android.bp.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub add_toplevel_block: Option<PathBuf>,
    /// Patch file to apply after Android.bp is generated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub patch: Option<PathBuf>,
    /// Patch file to apply after rules.mk is generated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub rulesmk_patch: Option<PathBuf>,
    /// `license_text` to use for `license` module, overriding the `license_file` given by the
    /// package or the default "LICENSE".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub license_text: Option<String>,
}

impl PackageConfig {
    /// Names of all the fields on `PackageConfig`.
    const FIELD_NAMES: [&'static str; 4] =
        ["add_toplevel_block", "license_text", "patch", "rulesmk_patch"];
}

/// Options that apply to everything in a package (i.e. everything associated with a particular
/// Cargo.toml file), for a particular variant.
#[derive(Clone, Debug, Deserialize, Eq, PartialEq, Serialize)]
#[serde(deny_unknown_fields)]
pub struct PackageVariantConfig {
    /// Link against `alloc`. Only valid if `no_std` is also true.
    #[serde(default, skip_serializing_if = "is_false")]
    pub alloc: bool,
    /// Whether to compile for device. Defaults to true.
    #[serde(default = "default_true", skip_serializing_if = "is_true")]
    pub device_supported: bool,
    /// Whether to compile for host. Defaults to true.
    #[serde(default = "default_true", skip_serializing_if = "is_true")]
    pub host_supported: bool,
    /// Whether to compile for non-build host targets. Defaults to true.
    #[serde(default = "default_true", skip_serializing_if = "is_true")]
    pub host_cross_supported: bool,
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
    /// File with content to append to the end of each generated module.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub add_module_block: Option<PathBuf>,
    /// Modules in this list will not be added as dependencies of generated modules.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub dep_blocklist: Vec<String>,
    /// Don't link against `std`, only `core`.
    #[serde(default, skip_serializing_if = "is_false")]
    pub no_std: bool,
    /// Copy build.rs output to ./out/* and add a genrule to copy ./out/* to genrule output.
    /// For crates with code pattern:
    ///     include!(concat!(env!("OUT_DIR"), "/<some_file>.rs"))
    #[serde(default, skip_serializing_if = "is_false")]
    pub copy_out: bool,
    /// Add the given files to the given tests' `data` property. The key is the test source filename
    /// relative to the crate root.
    #[serde(default, skip_serializing_if = "BTreeMap::is_empty")]
    pub test_data: BTreeMap<String, Vec<String>>,
    /// Static libraries in this list will instead be added as whole_static_libs.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub whole_static_libs: Vec<String>,
    /// Directories with headers to export for C usage.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub exported_c_header_dir: Vec<PathBuf>,
}

impl Default for PackageVariantConfig {
    fn default() -> Self {
        Self {
            alloc: false,
            device_supported: true,
            host_supported: true,
            host_cross_supported: true,
            host_first_multilib: false,
            force_rlib: false,
            no_presubmit: false,
            add_module_block: None,
            dep_blocklist: Default::default(),
            no_std: false,
            copy_out: false,
            test_data: Default::default(),
            whole_static_libs: Default::default(),
            exported_c_header_dir: Default::default(),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn variant_config() {
        let config = Config::from_json_str(
            r#"{
            "tests": true,
            "package": {
                "argh": {
                    "patch": "patches/Android.bp.patch"
                },
                "another": {
                    "add_toplevel_block": "block.bp",
                    "device_supported": false,
                    "force_rlib": true
                },
                "rulesmk": {
                    "rulesmk_patch": "patches/rules.mk.patch"
                }
            },
            "variants": [
                {},
                {
                    "generate_androidbp": false,
                    "generate_rulesmk": true,
                    "tests": false,
                    "features": ["feature"],
                    "vendor_available": false,
                    "package": {
                        "another": {
                            "alloc": false,
                            "force_rlib": false
                        },
                        "variant_package": {
                            "add_module_block": "variant_module_block.bp"
                        }
                    }
                }
            ]
        }"#,
        )
        .unwrap();

        assert_eq!(
            config,
            Config {
                variants: vec![
                    VariantConfig {
                        generate_androidbp: true,
                        generate_rulesmk: false,
                        tests: true,
                        features: None,
                        vendor_available: true,
                        package: [
                            ("argh".to_string(), PackageVariantConfig { ..Default::default() }),
                            (
                                "another".to_string(),
                                PackageVariantConfig {
                                    device_supported: false,
                                    force_rlib: true,
                                    ..Default::default()
                                },
                            ),
                            ("rulesmk".to_string(), PackageVariantConfig { ..Default::default() }),
                        ]
                        .into_iter()
                        .collect(),
                        ..Default::default()
                    },
                    VariantConfig {
                        generate_androidbp: false,
                        generate_rulesmk: true,
                        tests: false,
                        features: Some(vec!["feature".to_string()]),
                        vendor_available: false,
                        package: [
                            ("argh".to_string(), PackageVariantConfig { ..Default::default() }),
                            (
                                "another".to_string(),
                                PackageVariantConfig {
                                    alloc: false,
                                    device_supported: false,
                                    force_rlib: false,
                                    ..Default::default()
                                },
                            ),
                            ("rulesmk".to_string(), PackageVariantConfig { ..Default::default() }),
                            (
                                "variant_package".to_string(),
                                PackageVariantConfig {
                                    add_module_block: Some("variant_module_block.bp".into()),
                                    ..Default::default()
                                },
                            ),
                        ]
                        .into_iter()
                        .collect(),
                        ..Default::default()
                    },
                ],
                package: [
                    (
                        "argh".to_string(),
                        PackageConfig {
                            patch: Some("patches/Android.bp.patch".into()),
                            ..Default::default()
                        },
                    ),
                    (
                        "another".to_string(),
                        PackageConfig {
                            add_toplevel_block: Some("block.bp".into()),
                            ..Default::default()
                        },
                    ),
                    (
                        "rulesmk".to_string(),
                        PackageConfig {
                            rulesmk_patch: Some("patches/rules.mk.patch".into()),
                            ..Default::default()
                        },
                    ),
                ]
                .into_iter()
                .collect(),
            }
        );
    }

    /// Tests that variant configuration options are factored out to the top level where possible.
    #[test]
    fn factor_variants() {
        let config = Config {
            variants: vec![
                VariantConfig {
                    features: Some(vec![]),
                    tests: true,
                    vendor_available: false,
                    package: [(
                        "argh".to_string(),
                        PackageVariantConfig {
                            dep_blocklist: vec!["bad_dep".to_string()],
                            ..Default::default()
                        },
                    )]
                    .into_iter()
                    .collect(),
                    ..Default::default()
                },
                VariantConfig {
                    features: Some(vec![]),
                    tests: true,
                    product_available: false,
                    module_name_overrides: [("argh".to_string(), "argh_nostd".to_string())]
                        .into_iter()
                        .collect(),
                    vendor_available: false,
                    package: [(
                        "argh".to_string(),
                        PackageVariantConfig {
                            dep_blocklist: vec!["bad_dep".to_string()],
                            no_std: true,
                            ..Default::default()
                        },
                    )]
                    .into_iter()
                    .collect(),
                    ..Default::default()
                },
            ],
            package: [(
                "argh".to_string(),
                PackageConfig { add_toplevel_block: Some("block.bp".into()), ..Default::default() },
            )]
            .into_iter()
            .collect(),
        };

        assert_eq!(
            config.to_json_string().unwrap(),
            r#"{
  "features": [],
  "package": {
    "argh": {
      "add_toplevel_block": "block.bp",
      "dep_blocklist": [
        "bad_dep"
      ]
    }
  },
  "tests": true,
  "variants": [
    {},
    {
      "module_name_overrides": {
        "argh": "argh_nostd"
      },
      "package": {
        "argh": {
          "no_std": true
        }
      },
      "product_available": false
    }
  ],
  "vendor_available": false
}"#
        );
    }

    #[test]
    fn factor_trivial_variant() {
        let config = Config {
            variants: vec![VariantConfig {
                tests: true,
                package: [("argh".to_string(), Default::default())].into_iter().collect(),
                ..Default::default()
            }],
            package: Default::default(),
        };

        assert_eq!(
            config.to_json_string().unwrap(),
            r#"{
  "tests": true
}"#
        );
    }
}
