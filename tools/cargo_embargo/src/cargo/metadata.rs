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

//! Types for parsing cargo.metadata JSON files.

use super::{Crate, CrateType, Extern, ExternType};
use crate::config::Config;
use anyhow::{anyhow, bail, Context, Result};
use serde::Deserialize;
use std::collections::BTreeMap;
use std::fs::File;
use std::ops::Deref;
use std::path::{Path, PathBuf};

/// `cargo metadata` output.
#[derive(Clone, Debug, Deserialize, Eq, PartialEq)]
pub struct WorkspaceMetadata {
    pub packages: Vec<PackageMetadata>,
    pub workspace_members: Vec<String>,
}

#[derive(Clone, Debug, Deserialize, Eq, PartialEq)]
pub struct PackageMetadata {
    pub name: String,
    pub version: String,
    pub edition: String,
    pub manifest_path: String,
    pub dependencies: Vec<DependencyMetadata>,
    pub features: BTreeMap<String, Vec<String>>,
    pub id: String,
    pub targets: Vec<TargetMetadata>,
}

#[derive(Clone, Debug, Deserialize, Eq, PartialEq)]
pub struct DependencyMetadata {
    pub name: String,
    pub kind: Option<String>,
    pub optional: bool,
    pub target: Option<String>,
}

impl DependencyMetadata {
    /// Returns whether the dependency should be included when the given features are enabled.
    fn enabled(&self, features: &[String]) -> bool {
        // TODO: Parse target properly.
        self.target.is_none()
            && (!self.optional || features.contains(&format!("dep:{}", self.name)))
    }
}

#[derive(Clone, Debug, Deserialize, Eq, PartialEq)]
#[allow(dead_code)]
pub struct TargetMetadata {
    pub crate_types: Vec<CrateType>,
    pub doc: bool,
    pub doctest: bool,
    pub edition: String,
    pub kind: Vec<TargetKind>,
    pub name: String,
    pub src_path: PathBuf,
    pub test: bool,
}

#[derive(Copy, Clone, Debug, Deserialize, Eq, PartialEq)]
#[serde[rename_all = "kebab-case"]]
pub enum TargetKind {
    Bin,
    CustomBuild,
    Bench,
    Example,
    Lib,
    Staticlib,
    Cdylib,
    ProcMacro,
    Test,
}

pub fn parse_cargo_metadata_file(
    cargo_metadata_path: impl AsRef<Path>,
    cfg: &Config,
) -> Result<Vec<Crate>> {
    let metadata: WorkspaceMetadata = serde_json::from_reader(
        File::open(cargo_metadata_path).context("failed to open cargo.metadata")?,
    )
    .context("failed to parse cargo.metadata")?;
    parse_cargo_metadata(&metadata, &cfg.features, cfg.tests)
}

fn parse_cargo_metadata(
    metadata: &WorkspaceMetadata,
    features: &Option<Vec<String>>,
    include_tests: bool,
) -> Result<Vec<Crate>> {
    let mut crates = Vec::new();
    for package in &metadata.packages {
        if !metadata.workspace_members.contains(&package.id) {
            continue;
        }

        let features = resolve_features(features, &package.features);
        let externs: Vec<Extern> = package
            .dependencies
            .iter()
            .filter_map(|dependency| {
                // Kind is None for normal dependencies, as opposed to dev dependencies.
                if dependency.enabled(&features) && dependency.kind.is_none() {
                    let dependency_name = dependency.name.replace('-', "_");
                    Some(Extern {
                        name: dependency_name.to_owned(),
                        lib_name: dependency_name.to_owned(),
                        extern_type: extern_type(&metadata.packages, &dependency.name),
                    })
                } else {
                    None
                }
            })
            .collect();
        let features_without_deps: Vec<String> =
            features.clone().into_iter().filter(|feature| !feature.starts_with("dep:")).collect();
        let package_dir = package_dir_from_id(&package.id)?;

        for target in &package.targets {
            let [target_kind] = target.kind.deref() else {
                bail!("Target kind had unexpected length: {:?}", target.kind);
            };
            // TODO: Consider whether to support Staticlib and Cdylib.
            if ![TargetKind::Bin, TargetKind::Lib, TargetKind::ProcMacro, TargetKind::Test]
                .contains(target_kind)
            {
                // Only binaries, libraries and integration tests are supported.
                continue;
            }
            let main_src = split_src_path(&target.src_path, &package_dir);
            // Hypens are not allowed in crate names. See
            // https://github.com/rust-lang/rfcs/blob/master/text/0940-hyphens-considered-harmful.md
            // for background.
            let target_name = target.name.replace('-', "_");
            let target_triple = if *target_kind == TargetKind::ProcMacro {
                None
            } else {
                Some("x86_64-unknown-linux-gnu".to_string())
            };
            // Don't generate an entry for integration tests, they will be covered by the test case
            // below.
            if *target_kind != TargetKind::Test {
                crates.push(Crate {
                    name: target_name.clone(),
                    package_name: package.name.to_owned(),
                    version: Some(package.version.to_owned()),
                    types: target.crate_types.clone(),
                    features: features_without_deps.clone(),
                    edition: package.edition.to_owned(),
                    package_dir: package_dir.clone(),
                    main_src: main_src.to_owned(),
                    target: target_triple.clone(),
                    externs: externs.clone(),
                    ..Default::default()
                });
            }
            // This includes both unit tests and integration tests.
            if target.test && include_tests {
                let mut externs: Vec<Extern> = package
                    .dependencies
                    .iter()
                    .filter_map(|dependency| {
                        if dependency.enabled(&features)
                            && dependency.kind.as_deref() != Some("build")
                        {
                            let dependency_name = dependency.name.replace('-', "_");
                            Some(Extern {
                                name: dependency_name.to_owned(),
                                lib_name: dependency_name.to_owned(),
                                extern_type: extern_type(&metadata.packages, &dependency.name),
                            })
                        } else {
                            None
                        }
                    })
                    .collect();
                // Add the library itself as a dependency for integration tests.
                if *target_kind == TargetKind::Test {
                    let package_name = package.name.replace('-', "_");
                    externs.push(Extern {
                        name: package_name.to_owned(),
                        lib_name: package_name.to_owned(),
                        extern_type: ExternType::Rust,
                    });
                }
                crates.push(Crate {
                    name: target_name,
                    package_name: package.name.to_owned(),
                    version: Some(package.version.to_owned()),
                    types: vec![CrateType::Test],
                    features: features_without_deps.clone(),
                    edition: package.edition.to_owned(),
                    package_dir: package_dir.clone(),
                    main_src: main_src.to_owned(),
                    target: target_triple.clone(),
                    externs,
                    ..Default::default()
                });
            }
        }
    }
    Ok(crates)
}

/// Checks whether the given package is a proc macro.
fn extern_type(packages: &[PackageMetadata], package_name: &str) -> ExternType {
    let Some(package) = packages.iter().find(|package| package.name == package_name) else {
        return ExternType::Rust;
    };
    if package.targets.iter().any(|target| target.kind.contains(&TargetKind::ProcMacro)) {
        ExternType::ProcMacro
    } else {
        ExternType::Rust
    }
}

/// Given a package ID like
/// `"either 1.8.1 (path+file:///usr/local/google/home/qwandor/aosp/external/rust/crates/either)"`,
/// returns the path to the package, e.g.
/// `"/usr/local/google/home/qwandor/aosp/external/rust/crates/either"`.
fn package_dir_from_id(id: &str) -> Result<PathBuf> {
    const URI_MARKER: &str = "(path+file://";
    let uri_start = id.find(URI_MARKER).ok_or_else(|| anyhow!("Invalid package ID {}", id))?;
    Ok(PathBuf::from(id[uri_start + URI_MARKER.len()..id.len() - 1].to_string()))
}

fn split_src_path<'a>(src_path: &'a Path, package_dir: &Path) -> &'a Path {
    if let Ok(main_src) = src_path.strip_prefix(package_dir) {
        main_src
    } else {
        src_path
    }
}

/// Given a set of chosen features, and the feature dependencies from a package's metadata, returns
/// the full set of features which should be enabled.
fn resolve_features(
    chosen_features: &Option<Vec<String>>,
    package_features: &BTreeMap<String, Vec<String>>,
) -> Vec<String> {
    let mut features = Vec::new();
    if let Some(chosen_features) = chosen_features {
        for feature in chosen_features {
            add_feature_and_dependencies(&mut features, feature, package_features);
        }
    } else if package_features.contains_key("default") {
        // If there is a default feature and no chosen features, then enable it.
        add_feature_and_dependencies(&mut features, "default", package_features);
    }
    features.sort();
    features.dedup();
    features
}

/// Adds the given feature and all features it depends on to the given list of features.
///
/// Ignores features of other packages, i.e. those containing slashes.
fn add_feature_and_dependencies(
    features: &mut Vec<String>,
    feature: &str,
    package_features: &BTreeMap<String, Vec<String>>,
) {
    features.push(feature.to_owned());
    if let Some(dependencies) = package_features.get(feature) {
        for dependency in dependencies {
            if !dependency.contains('/') {
                add_feature_and_dependencies(features, dependency, package_features);
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::tests::testdata_directories;
    use std::fs::File;

    #[test]
    fn resolve_multi_level_feature_dependencies() {
        let chosen = vec!["default".to_string(), "extra".to_string(), "on_by_default".to_string()];
        let package_features = [
            (
                "default".to_string(),
                vec!["std".to_string(), "other".to_string(), "on_by_default".to_string()],
            ),
            ("std".to_string(), vec!["alloc".to_string()]),
            ("not_enabled".to_string(), vec![]),
        ]
        .into_iter()
        .collect();
        assert_eq!(
            resolve_features(&Some(chosen), &package_features),
            vec![
                "alloc".to_string(),
                "default".to_string(),
                "extra".to_string(),
                "on_by_default".to_string(),
                "other".to_string(),
                "std".to_string(),
            ]
        );
    }

    #[test]
    fn parse_metadata() {
        for testdata_directory_path in testdata_directories() {
            let cfg: Config = serde_json::from_reader(
                File::open(testdata_directory_path.join("cargo_embargo.json"))
                    .with_context(|| {
                        format!(
                            "Failed to open {:?}",
                            testdata_directory_path.join("cargo_embargo.json")
                        )
                    })
                    .unwrap(),
            )
            .unwrap();
            let cargo_metadata_path = testdata_directory_path.join("cargo.metadata");
            let expected_crates: Vec<Crate> = serde_json::from_reader(
                File::open(testdata_directory_path.join("crates.json")).unwrap(),
            )
            .unwrap();

            let crates = parse_cargo_metadata_file(cargo_metadata_path, &cfg).unwrap();
            assert_eq!(crates, expected_crates);
        }
    }
}
