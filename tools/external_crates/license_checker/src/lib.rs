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

use std::{
    collections::{BTreeMap, BTreeSet},
    fs::read_to_string,
    path::{Path, PathBuf},
};

use spdx::LicenseReq;
use thiserror::Error;

mod content_checker;
mod expression_parser;
mod file_name_checker;
mod license_file_finder;

#[derive(Error, Debug)]
pub enum LicenseCheckerError {
    #[error("Couldn't convert filesystem path {} (lossy) to a string for globbing.", .0.to_string_lossy())]
    PathToString(PathBuf),
    #[error("Glob error")]
    GlobError(#[from] glob::GlobError),
    #[error("Glob pattern error")]
    PatternError(#[from] glob::PatternError),
    #[error("Strip prefix error")]
    StripPrefixError(#[from] std::path::StripPrefixError),
    #[error("Found a license expression special case for crate {crate_name} but the Cargo.toml license field doesn't match. Expected '{expected_license}', found '{cargo_toml_license}'")]
    LicenseExpressionSpecialCase {
        crate_name: String,
        expected_license: String,
        cargo_toml_license: String,
    },
    #[error("Crate {0} doesn't have a license field in Cargo.toml, and no special case was found for this crate")]
    MissingLicenseField(String),
    #[error("SPDX expression parse error")]
    ParseError(#[from] spdx::ParseError),
    #[error("SPDX expression minimize error")]
    MinimizeError(#[from] spdx::expression::MinimizeError),
    #[error("Unknown license checker error")]
    Unknown,
}

#[derive(Debug)]
pub struct LicenseState {
    pub unsatisfied: BTreeSet<LicenseReq>,
    pub satisfied: BTreeMap<LicenseReq, PathBuf>,
}

pub fn find_licenses(
    crate_path: impl AsRef<Path>,
    crate_name: &str,
    cargo_toml_license: Option<&str>,
) -> Result<LicenseState, LicenseCheckerError> {
    let crate_path = crate_path.as_ref();
    let mut state = LicenseState { unsatisfied: BTreeSet::new(), satisfied: BTreeMap::new() };

    state.unsatisfied = expression_parser::get_chosen_licenses(crate_name, cargo_toml_license)?;
    let mut possible_license_files = license_file_finder::find_license_files(crate_path)?;

    possible_license_files.retain(|file| {
        if let Some(req) = file_name_checker::classify_license_file_name(file) {
            if state.unsatisfied.remove(&req) {
                state.satisfied.insert(req, file.clone());
                return false;
            }
        }
        true
    });

    if !state.unsatisfied.is_empty() {
        possible_license_files.retain(|file| {
            let contents = read_to_string(crate_path.join(file)).unwrap();
            let matches = content_checker::classify_license_file_contents(&contents);
            for req in &matches {
                if state.unsatisfied.remove(req) {
                    state.satisfied.insert(req.clone(), file.clone());
                }
            }
            !matches.is_empty()
        });
    }

    Ok(state)
}
