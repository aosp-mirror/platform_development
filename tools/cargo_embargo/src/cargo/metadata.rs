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
use crate::config::VariantConfig;
use anyhow::{bail, Context, Result};
use serde::Deserialize;
use std::collections::{BTreeMap, BTreeSet};
use std::path::{Path, PathBuf};

/// `cfg` strings for dependencies which should be considered enabled. It would be better to parse
/// them properly, but this is good enough in practice so far.
const ENABLED_CFGS: [&str; 6] = [
    r#"unix"#,
    r#"not(windows)"#,
    r#"any(unix, target_os = "wasi")"#,
    r#"not(all(target_family = "wasm", target_os = "unknown"))"#,
    r#"not(target_family = "wasm")"#,
    r#"any(target_os = "linux", target_os = "android")"#,
];

/// `cargo metadata` output.
#[derive(Clone, Debug, Deserialize, Eq, PartialEq)]
pub struct WorkspaceMetadata {
    pub packages: Vec<PackageMetadata>,
    pub workspace_members: Vec<String>,
}

#[derive(Clone, Debug, Default, Deserialize, Eq, PartialEq)]
pub struct PackageMetadata {
    pub name: String,
    pub version: String,
    pub edition: String,
    pub manifest_path: String,
    pub dependencies: Vec<DependencyMetadata>,
    pub features: BTreeMap<String, Vec<String>>,
    pub id: String,
    pub targets: Vec<TargetMetadata>,
    pub license: Option<String>,
    pub license_file: Option<String>,
}

#[derive(Clone, Debug, Deserialize, Eq, PartialEq)]
pub struct DependencyMetadata {
    pub name: String,
    pub kind: Option<String>,
    pub optional: bool,
    pub target: Option<String>,
    pub rename: Option<String>,
}

impl DependencyMetadata {
    /// Returns whether the dependency should be included when the given features are enabled.
    fn enabled(&self, features: &[String], cfgs: &[String]) -> bool {
        if let Some(target) = &self.target {
            if target.starts_with("cfg(") && target.ends_with(')') {
                let target_cfg = &target[4..target.len() - 1];
                if !ENABLED_CFGS.contains(&target_cfg) && !cfgs.contains(&target_cfg.to_string()) {
                    return false;
                }
            }
        }
        let name = self.rename.as_ref().unwrap_or(&self.name);
        !self.optional || features.contains(&format!("dep:{}", name))
    }
}

#[derive(Clone, Debug, Default, Deserialize, Eq, PartialEq)]
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
    Rlib,
    Staticlib,
    Cdylib,
    ProcMacro,
    Test,
}

pub fn parse_cargo_metadata_str(cargo_metadata: &str, cfg: &VariantConfig) -> Result<Vec<Crate>> {
    let metadata =
        serde_json::from_str(cargo_metadata).context("failed to parse cargo metadata")?;
    parse_cargo_metadata(
        &metadata,
        &cfg.features,
        &cfg.extra_cfg,
        cfg.tests,
        &cfg.workspace_excludes,
    )
}

fn parse_cargo_metadata(
    metadata: &WorkspaceMetadata,
    features: &Option<Vec<String>>,
    cfgs: &[String],
    include_tests: bool,
    workspace_excludes: &[String],
) -> Result<Vec<Crate>> {
    let mut crates = Vec::new();
    for package in &metadata.packages {
        if !metadata.workspace_members.contains(&package.id)
            || workspace_excludes.contains(&package.name)
        {
            continue;
        }

        let features = resolve_features(features, &package.features, &package.dependencies);
        let features_without_deps: Vec<String> =
            features.clone().into_iter().filter(|feature| !feature.starts_with("dep:")).collect();
        let package_dir = package_dir_from_id(&package.id)?;

        for target in &package.targets {
            let target_kinds = target
                .kind
                .clone()
                .into_iter()
                .filter(|kind| {
                    [
                        TargetKind::Bin,
                        TargetKind::Cdylib,
                        TargetKind::Lib,
                        TargetKind::ProcMacro,
                        TargetKind::Rlib,
                        TargetKind::Staticlib,
                        TargetKind::Test,
                    ]
                    .contains(kind)
                })
                .collect::<Vec<_>>();
            if target_kinds.is_empty() {
                // Only binaries, libraries and integration tests are supported.
                continue;
            }
            let main_src = split_src_path(&target.src_path, &package_dir);
            // Hypens are not allowed in crate names. See
            // https://github.com/rust-lang/rfcs/blob/master/text/0940-hyphens-considered-harmful.md
            // for background.
            let target_name = target.name.replace('-', "_");
            let target_triple = if target_kinds == [TargetKind::ProcMacro] {
                None
            } else {
                Some("x86_64-unknown-linux-gnu".to_string())
            };
            // Don't generate an entry for integration tests, they will be covered by the test case
            // below.
            if target_kinds != [TargetKind::Test] {
                crates.push(Crate {
                    name: target_name.clone(),
                    package_name: package.name.to_owned(),
                    version: Some(package.version.to_owned()),
                    types: target.crate_types.clone(),
                    features: features_without_deps.clone(),
                    edition: package.edition.to_owned(),
                    license: package.license.clone(),
                    license_file: package.license_file.clone(),
                    package_dir: package_dir.clone(),
                    main_src: main_src.to_owned(),
                    target: target_triple.clone(),
                    externs: get_externs(
                        package,
                        &metadata.packages,
                        &features,
                        cfgs,
                        &target_kinds,
                        false,
                    )?,
                    cfgs: cfgs.to_owned(),
                    ..Default::default()
                });
            }
            // This includes both unit tests and integration tests.
            if target.test && include_tests {
                crates.push(Crate {
                    name: target_name,
                    package_name: package.name.to_owned(),
                    version: Some(package.version.to_owned()),
                    types: vec![CrateType::Test],
                    features: features_without_deps.clone(),
                    edition: package.edition.to_owned(),
                    license: package.license.clone(),
                    license_file: package.license_file.clone(),
                    package_dir: package_dir.clone(),
                    main_src: main_src.to_owned(),
                    target: target_triple.clone(),
                    externs: get_externs(
                        package,
                        &metadata.packages,
                        &features,
                        cfgs,
                        &target_kinds,
                        true,
                    )?,
                    cfgs: cfgs.to_owned(),
                    ..Default::default()
                });
            }
        }
    }
    Ok(crates)
}

fn get_externs(
    package: &PackageMetadata,
    packages: &[PackageMetadata],
    features: &[String],
    cfgs: &[String],
    target_kinds: &[TargetKind],
    test: bool,
) -> Result<Vec<Extern>> {
    let mut externs = package
        .dependencies
        .iter()
        .filter_map(|dependency| {
            // Kind is None for normal dependencies, as opposed to dev dependencies.
            if dependency.enabled(features, cfgs)
                && dependency.kind.as_deref() != Some("build")
                && (dependency.kind.is_none() || test)
            {
                Some(make_extern(packages, dependency))
            } else {
                None
            }
        })
        .collect::<Result<Vec<Extern>>>()?;

    // If there is a library target and this is a binary or integration test, add the library as an
    // extern.
    if matches!(target_kinds, [TargetKind::Bin] | [TargetKind::Test]) {
        for target in &package.targets {
            if target.kind.contains(&TargetKind::Lib) {
                let lib_name = target.name.replace('-', "_");
                externs.push(Extern {
                    name: lib_name.clone(),
                    lib_name,
                    raw_name: target.name.clone(),
                    extern_type: ExternType::Rust,
                });
            }
        }
    }

    externs.sort();
    externs.dedup();
    Ok(externs)
}

fn make_extern(packages: &[PackageMetadata], dependency: &DependencyMetadata) -> Result<Extern> {
    let Some(package) = packages.iter().find(|package| package.name == dependency.name) else {
        bail!("package {} not found in metadata", dependency.name);
    };
    let Some(target) = package.targets.iter().find(|target| {
        target.kind.contains(&TargetKind::Lib) || target.kind.contains(&TargetKind::ProcMacro)
    }) else {
        bail!("Package {} didn't have any library or proc-macro targets", dependency.name);
    };
    let lib_name = target.name.replace('-', "_");
    // This is ugly but looking at the source path is the easiest way to tell if the raw
    // crate name uses a hyphen instead of an underscore. It won't work if it uses both.
    let raw_name = target.name.replace('_', "-");
    let src_path = target.src_path.to_str().expect("failed to convert src_path to string");
    let raw_name = if src_path.contains(&raw_name) { raw_name } else { lib_name.clone() };
    let name =
        if let Some(rename) = &dependency.rename { rename.clone() } else { lib_name.clone() };

    // Check whether the package is a proc macro.
    let extern_type =
        if package.targets.iter().any(|target| target.kind.contains(&TargetKind::ProcMacro)) {
            ExternType::ProcMacro
        } else {
            ExternType::Rust
        };
    Ok(Extern { name, lib_name, raw_name, extern_type })
}

/// Given a Cargo package ID, returns the path.
///
/// Extracts `"/path/to/crate"` from
/// `"path+file:///path/to/crate#1.2.3"`. See
/// https://doc.rust-lang.org/cargo/reference/pkgid-spec.html for
/// information on Cargo package ID specifications.
fn package_dir_from_id(id: &str) -> Result<PathBuf> {
    const PREFIX: &str = "path+file://";
    const SEPARATOR: char = '#';
    let Some(stripped) = id.strip_prefix(PREFIX) else {
        bail!("Invalid package ID {id:?}, expected it to start with {PREFIX:?}");
    };
    let Some(idx) = stripped.rfind(SEPARATOR) else {
        bail!("Invalid package ID {id:?}, expected it to contain {SEPARATOR:?}");
    };
    Ok(PathBuf::from(stripped[..idx].to_string()))
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
    dependencies: &[DependencyMetadata],
) -> Vec<String> {
    let mut package_features = package_features.to_owned();
    // Add implicit features for optional dependencies.
    for dependency in dependencies {
        if dependency.optional && !package_features.contains_key(&dependency.name) {
            package_features
                .insert(dependency.name.to_owned(), vec![format!("dep:{}", dependency.name)]);
        }
    }

    let mut features = BTreeSet::new();
    if let Some(chosen_features) = chosen_features {
        for feature in chosen_features {
            add_feature_and_dependencies(&mut features, feature, &package_features);
        }
    } else {
        // If there are no chosen features, then enable the default feature.
        add_feature_and_dependencies(&mut features, "default", &package_features);
    }
    features.into_iter().collect()
}

/// Adds the given feature and all features it depends on to the given list of features.
///
/// Ignores features of other packages, and features which don't exist.
fn add_feature_and_dependencies(
    features: &mut BTreeSet<String>,
    feature: &str,
    package_features: &BTreeMap<String, Vec<String>>,
) {
    if features.contains(&feature.to_string()) {
        return;
    }
    if package_features.contains_key(feature) || feature.starts_with("dep:") {
        features.insert(feature.to_owned());
    }

    if let Some(dependencies) = package_features.get(feature) {
        for dependency in dependencies {
            if let Some((dependency_package, _)) = dependency.split_once('/') {
                add_feature_and_dependencies(features, dependency_package, package_features);
            } else {
                add_feature_and_dependencies(features, dependency, package_features);
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::config::Config;
    use crate::tests::testdata_directories;
    use googletest::matchers::eq;
    use googletest::prelude::assert_that;
    use std::fs::{read_to_string, File};

    #[test]
    fn extract_package_dir_from_id() -> Result<()> {
        assert_eq!(
            package_dir_from_id("path+file:///path/to/crate#1.2.3")?,
            PathBuf::from("/path/to/crate")
        );
        Ok(())
    }

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
            ("on_by_default".to_string(), vec![]),
            ("other".to_string(), vec![]),
            ("extra".to_string(), vec![]),
            ("alloc".to_string(), vec![]),
        ]
        .into_iter()
        .collect();
        assert_eq!(
            resolve_features(&Some(chosen), &package_features, &[]),
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
    fn resolve_dep_features() {
        let package_features = [(
            "default".to_string(),
            vec![
                "optionaldep/feature".to_string(),
                "requireddep/feature".to_string(),
                "optionaldep2?/feature".to_string(),
            ],
        )]
        .into_iter()
        .collect();
        let dependencies = vec![
            DependencyMetadata {
                name: "optionaldep".to_string(),
                kind: None,
                optional: true,
                target: None,
                rename: None,
            },
            DependencyMetadata {
                name: "optionaldep2".to_string(),
                kind: None,
                optional: true,
                target: None,
                rename: None,
            },
            DependencyMetadata {
                name: "requireddep".to_string(),
                kind: None,
                optional: false,
                target: None,
                rename: None,
            },
        ];
        assert_eq!(
            resolve_features(&None, &package_features, &dependencies),
            vec!["default".to_string(), "dep:optionaldep".to_string(), "optionaldep".to_string()]
        );
    }

    #[test]
    fn resolve_dep_features_recursion() {
        let chosen = vec!["tokio".to_string()];
        let package_features = [
            ("default".to_string(), vec![]),
            ("tokio".to_string(), vec!["dep:tokio".to_string(), "tokio/net".to_string()]),
        ]
        .into_iter()
        .collect();
        assert_eq!(
            resolve_features(&Some(chosen), &package_features, &[]),
            vec!["dep:tokio".to_string(), "tokio".to_string(),]
        );
    }

    #[test]
    fn get_externs_cfg() {
        let package = PackageMetadata {
            name: "test_package".to_string(),
            dependencies: vec![
                DependencyMetadata {
                    name: "alwayslib".to_string(),
                    kind: None,
                    optional: false,
                    target: None,
                    rename: None,
                },
                DependencyMetadata {
                    name: "unixlib".to_string(),
                    kind: None,
                    optional: false,
                    target: Some("cfg(unix)".to_string()),
                    rename: None,
                },
                DependencyMetadata {
                    name: "windowslib".to_string(),
                    kind: None,
                    optional: false,
                    target: Some("cfg(windows)".to_string()),
                    rename: None,
                },
            ],
            features: [].into_iter().collect(),
            targets: vec![],
            ..Default::default()
        };
        let packages = vec![
            package.clone(),
            PackageMetadata {
                name: "alwayslib".to_string(),
                targets: vec![TargetMetadata {
                    name: "alwayslib".to_string(),
                    kind: vec![TargetKind::Lib],
                    ..Default::default()
                }],
                ..Default::default()
            },
            PackageMetadata {
                name: "unixlib".to_string(),
                targets: vec![TargetMetadata {
                    name: "unixlib".to_string(),
                    kind: vec![TargetKind::Lib],
                    ..Default::default()
                }],
                ..Default::default()
            },
            PackageMetadata {
                name: "windowslib".to_string(),
                targets: vec![TargetMetadata {
                    name: "windowslib".to_string(),
                    kind: vec![TargetKind::Lib],
                    ..Default::default()
                }],
                ..Default::default()
            },
        ];
        assert_eq!(
            get_externs(&package, &packages, &[], &[], &[], false).unwrap(),
            vec![
                Extern {
                    name: "alwayslib".to_string(),
                    lib_name: "alwayslib".to_string(),
                    raw_name: "alwayslib".to_string(),
                    extern_type: ExternType::Rust
                },
                Extern {
                    name: "unixlib".to_string(),
                    lib_name: "unixlib".to_string(),
                    raw_name: "unixlib".to_string(),
                    extern_type: ExternType::Rust
                },
            ]
        );
    }

    #[test]
    fn get_externs_extra_cfg() {
        let package = PackageMetadata {
            name: "test_package".to_string(),
            dependencies: vec![
                DependencyMetadata {
                    name: "foolib".to_string(),
                    kind: None,
                    optional: false,
                    target: Some("cfg(foo)".to_string()),
                    rename: None,
                },
                DependencyMetadata {
                    name: "barlib".to_string(),
                    kind: None,
                    optional: false,
                    target: Some("cfg(bar)".to_string()),
                    rename: None,
                },
            ],
            features: [].into_iter().collect(),
            targets: vec![],
            ..Default::default()
        };
        let packages = vec![
            package.clone(),
            PackageMetadata {
                name: "foolib".to_string(),
                targets: vec![TargetMetadata {
                    name: "foolib".to_string(),
                    kind: vec![TargetKind::Lib],
                    ..Default::default()
                }],
                ..Default::default()
            },
            PackageMetadata {
                name: "barlib".to_string(),
                targets: vec![TargetMetadata {
                    name: "barlib".to_string(),
                    kind: vec![TargetKind::Lib],
                    ..Default::default()
                }],
                ..Default::default()
            },
        ];
        assert_eq!(
            get_externs(&package, &packages, &[], &["foo".to_string()], &[], false).unwrap(),
            vec![Extern {
                name: "foolib".to_string(),
                lib_name: "foolib".to_string(),
                raw_name: "foolib".to_string(),
                extern_type: ExternType::Rust
            },]
        );
    }

    #[test]
    fn get_externs_rename() {
        let package = PackageMetadata {
            name: "test_package".to_string(),
            dependencies: vec![
                DependencyMetadata {
                    name: "foo".to_string(),
                    kind: None,
                    optional: false,
                    target: None,
                    rename: Some("foo2".to_string()),
                },
                DependencyMetadata {
                    name: "bar".to_string(),
                    kind: None,
                    optional: true,
                    target: None,
                    rename: None,
                },
                DependencyMetadata {
                    name: "bar".to_string(),
                    kind: None,
                    optional: true,
                    target: None,
                    rename: Some("baz".to_string()),
                },
            ],
            ..Default::default()
        };
        let packages = vec![
            package.clone(),
            PackageMetadata {
                name: "foo".to_string(),
                targets: vec![TargetMetadata {
                    name: "foo".to_string(),
                    kind: vec![TargetKind::Lib],
                    ..Default::default()
                }],
                ..Default::default()
            },
            PackageMetadata {
                name: "bar".to_string(),
                targets: vec![TargetMetadata {
                    name: "bar".to_string(),
                    kind: vec![TargetKind::Lib],
                    ..Default::default()
                }],
                ..Default::default()
            },
        ];
        assert_eq!(
            get_externs(&package, &packages, &["dep:bar".to_string()], &[], &[], false).unwrap(),
            vec![
                Extern {
                    name: "bar".to_string(),
                    lib_name: "bar".to_string(),
                    raw_name: "bar".to_string(),
                    extern_type: ExternType::Rust
                },
                Extern {
                    name: "foo2".to_string(),
                    lib_name: "foo".to_string(),
                    raw_name: "foo".to_string(),
                    extern_type: ExternType::Rust
                },
            ]
        );
        assert_eq!(
            get_externs(&package, &packages, &["dep:baz".to_string()], &[], &[], false).unwrap(),
            vec![
                Extern {
                    name: "baz".to_string(),
                    lib_name: "bar".to_string(),
                    raw_name: "bar".to_string(),
                    extern_type: ExternType::Rust
                },
                Extern {
                    name: "foo2".to_string(),
                    lib_name: "foo".to_string(),
                    raw_name: "foo".to_string(),
                    extern_type: ExternType::Rust
                },
            ]
        );
    }

    #[test]
    fn parse_metadata() {
        /// Remove anything before "external/rust/crates/" from the
        /// `package_dir` field. This makes the test robust since you
        /// can use `cargo metadata` to regenerate the test files and
        /// you don't have to care about where your AOSP checkout
        /// lives.
        fn normalize_package_dir(mut c: Crate) -> Crate {
            const EXTERNAL_RUST_CRATES: &str = "external/rust/crates/";
            let package_dir = c.package_dir.to_str().unwrap();
            if let Some(idx) = package_dir.find(EXTERNAL_RUST_CRATES) {
                c.package_dir = PathBuf::from(format!(".../{}", &package_dir[idx..]));
            }
            c
        }

        for testdata_directory_path in testdata_directories() {
            let cfg = Config::from_json_str(
                &read_to_string(testdata_directory_path.join("cargo_embargo.json"))
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
            let expected_crates: Vec<Vec<Crate>> = serde_json::from_reader::<_, Vec<Vec<Crate>>>(
                File::open(testdata_directory_path.join("crates.json")).unwrap(),
            )
            .unwrap()
            .into_iter()
            .map(|crates: Vec<Crate>| crates.into_iter().map(normalize_package_dir).collect())
            .collect();

            let crates = cfg
                .variants
                .iter()
                .map(|variant_cfg| {
                    parse_cargo_metadata_str(
                        &read_to_string(&cargo_metadata_path)
                            .with_context(|| format!("Failed to open {:?}", cargo_metadata_path))
                            .unwrap(),
                        variant_cfg,
                    )
                    .unwrap()
                    .into_iter()
                    .map(normalize_package_dir)
                    .collect::<Vec<Crate>>()
                })
                .collect::<Vec<Vec<Crate>>>();
            assert_that!(format!("{crates:#?}"), eq(&format!("{expected_crates:#?}")));
        }
    }
}
