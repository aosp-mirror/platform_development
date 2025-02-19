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

//! A crate for finding license files in crates that satisfy their SPDX license expressions.

use std::{
    collections::{BTreeMap, BTreeSet},
    path::{Path, PathBuf},
};

use file_classifier::Classifier;
use spdx::LicenseReq;

mod expression_parser;
mod file_classifier;
mod license_file_finder;

/// Error types for the 'license_checker' crate.
#[derive(thiserror::Error, Debug)]
pub enum Error {
    /// Couldn't convert filesystem path to a string for globbing.
    #[error("Couldn't convert filesystem path {0} to a string for globbing.")]
    PathToString(PathBuf),
    /// Glob error
    #[error(transparent)]
    GlobError(#[from] glob::GlobError),
    /// Glob pattern error
    #[error(transparent)]
    PatternError(#[from] glob::PatternError),
    /// Error stripping prefix from path
    #[error(transparent)]
    StripPrefixError(#[from] std::path::StripPrefixError),
    /// License expression special case doesn't match what's in Cargo.toml
    #[error("Found a license expression special case for crate {crate_name} but the Cargo.toml license field doesn't match. Expected '{expected_license}', found '{cargo_toml_license}'")]
    LicenseExpressionSpecialCase {
        /// The name of the crate
        crate_name: String,
        /// The expected license expression in special case
        expected_license: String,
        /// The actual license expression in Cargo.toml
        cargo_toml_license: String,
    },
    /// The crate doesn't have a license field in Cargo.toml, and no special case was found for this crate
    #[error("Crate {0} doesn't have a license field in Cargo.toml, and no special case was found for this crate")]
    MissingLicenseField(String),
    /// Error parsing SPDX license expression
    #[error(transparent)]
    ParseError(#[from] spdx::ParseError),
    /// Error minimizing SPDX expression
    #[error(transparent)]
    MinimizeError(#[from] spdx::expression::MinimizeError),
}

/// The result of license file verification, containing a set of acceptable licenses, and the
/// corresponding license files, if present.
#[derive(Debug)]
pub struct LicenseState {
    /// Unsatisfied licenses. These are licenses that are required by evaluation of SPDX license in
    /// Cargo.toml, but for which no matching license file was found.
    pub unsatisfied: BTreeSet<LicenseReq>,
    /// Licenses for which a license file file was found, and the path to that file.
    pub satisfied: BTreeMap<LicenseReq, PathBuf>,
}

/// Evaluates the license expression for a crate at a given path and returns a minimal set of
/// acceptable licenses, and whether we could find a matching license file for each one.
///
/// Returns an error if the licensing for the crate requires us to adopt unacceptable licenses.
pub fn find_licenses(
    crate_path: impl AsRef<Path>,
    crate_name: &str,
    cargo_toml_license: Option<&str>,
) -> Result<LicenseState, Error> {
    let crate_path = crate_path.as_ref();
    let mut state = LicenseState { unsatisfied: BTreeSet::new(), satisfied: BTreeMap::new() };

    state.unsatisfied =
        expression_parser::evaluate_license_expr(crate_name, cargo_toml_license)?.required;
    let possible_license_files = license_file_finder::find_license_files(crate_path)?;

    for file in &possible_license_files {
        let classifier = Classifier::new(crate_path, file.clone());
        if let Some(req) = classifier.by_name() {
            if state.unsatisfied.remove(req) {
                state.satisfied.insert(req.clone(), file.clone());
            }
        } else {
            for req in classifier.by_content() {
                if state.unsatisfied.remove(req) {
                    state.satisfied.insert(req.clone(), file.clone());
                }
            }
        }
    }

    Ok(state)
}
