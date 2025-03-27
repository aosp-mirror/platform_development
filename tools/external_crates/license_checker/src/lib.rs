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
    fs::{remove_file, write},
    path::{Path, PathBuf},
};

use file_classifier::Classifier;
use glob::glob;
use google_metadata::LicenseType;
use license_data::{CrateLicenseSpecialCase, License};
use license_terms::LicenseTerms;
use licenses::LICENSE_DATA;
use parsed_license_data::{CrateLicenseSpecialCases, ParsedLicense};
use spdx::LicenseReq;

mod file_classifier;
mod license_data;
mod license_data_tests;
mod license_file_finder;
mod license_terms;
mod licenses;
mod parsed_license_data;
mod util;

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
    LicenseExpressionSpecialCaseMismatch {
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
    LicenseParseError(#[from] spdx::ParseError),
    /// Error minimizing SPDX expression
    #[error(transparent)]
    MinimizeError(#[from] spdx::expression::MinimizeError),
    /// Failed to read file
    #[error("Failed to read {0}: {1}")]
    FileReadError(PathBuf, std::io::Error),
    /// The set of known licenses is empty.
    #[error("The set of known licenses is empty")]
    NoLicenses,
    /// License with neither text nor file names.
    #[error("License {0} has neither text nor file names")]
    LicenseWithoutTextOrFileNames(String),
    /// Duplicate license
    #[error("Duplicate license {0}")]
    DuplicateLicense(String),
    /// Duplicate crate license special case
    #[error("Duplicate license special case for crate {0}")]
    DuplicateCrateLicenseSpecialCase(String),
    /// The license text is empty.
    #[error("The license text for {0} is empty")]
    EmptyLicenseText(String),
    /// License text is ambiguous because it is a substring of another license text.
    #[error("The license text for {0} is a substring of the license text for {0}")]
    AmbiguousLicenseText(String, String),
    /// Duplicate license file name
    #[error("The file name {file_name} matches multiple licenses: {license} and {other_license}")]
    DuplicateLicenseFileName {
        /// The name of the license file.
        file_name: String,
        /// The license associated with the filename.
        license: String,
        /// The other license that is also associated with the same filename.
        other_license: String,
    },
    /// License file name not findable by any known license file glob patterns.
    #[error(
        "The license file name {0} for {1} is not findable by any known license file glob patterns"
    )]
    LicenseFileNotFindable(String, String),
    /// Inexact license file name not findable by any known license file glob patterns.
    #[error(
        "The inexact license file name {0} is not findable by any known license file glob patterns"
    )]
    InexactLicenseFileNotFindable(String),
    /// The list of license preferences contains an unknown license
    #[error("The license preference list contains unknown license {0}")]
    LicensePreferenceForUnknownLicense(String),
    /// Invalid MODULE_LICENSE_* file name.
    #[error("The MODULE_LICENSE_* file name '{0}' does not begin with 'MODULE_LICENSE_'")]
    InvalidModuleLicenseFileName(String),
    /// License type is too restrictive.
    #[error("The license type {0:?} is too restrictive")]
    LicenseTypeTooRestrictive(LicenseType),
    /// I/O error.
    #[error(transparent)]
    IoError(#[from] std::io::Error),
}

/// The result of license file verification, containing a set of acceptable licenses, and the
/// corresponding license files, if present.
#[derive(Debug)]
pub struct LicenseState {
    /// Unsatisfied licenses. These are licenses that are required by evaluation of the SPDX
    /// license in Cargo.toml, but for which no matching license file was found.
    pub unsatisfied: BTreeSet<LicenseReq>,
    /// Licenses for which a license file file was found, and the path to that file.
    pub satisfied: BTreeMap<LicenseReq, PathBuf>,
    /// License files which are unneeded. That is, they are for license terms we are not
    /// required to follow, such as LICENSE-MIT in the case of "Apache-2.0 OR MIT".
    pub unneeded: BTreeMap<LicenseReq, PathBuf>,
    /// Unexpected license files. They don't correspond to any terms in Cargo.toml, and
    /// indicate that the stated license terms may be incorrect.
    pub unexpected: BTreeMap<LicenseReq, PathBuf>,
}

impl LicenseState {
    fn from(terms: LicenseTerms, file_classifiers: &Vec<Classifier>) -> LicenseState {
        let mut state = LicenseState {
            unsatisfied: terms.required,
            satisfied: BTreeMap::new(),
            unneeded: BTreeMap::new(),
            unexpected: BTreeMap::new(),
        };
        let not_required = terms.not_required;

        for classifier in file_classifiers {
            if let Some(req) = classifier.by_name() {
                if state.unsatisfied.remove(req) {
                    state.satisfied.insert(req.clone(), classifier.file_path().to_owned());
                } else if !state.satisfied.contains_key(req) {
                    if not_required.contains(req) {
                        state.unneeded.insert(req.clone(), classifier.file_path().to_owned());
                    } else {
                        state.unexpected.insert(req.clone(), classifier.file_path().to_owned());
                    }
                }
            }
        }

        if !state.unsatisfied.is_empty() {
            for classifier in file_classifiers {
                for req in classifier.by_content() {
                    if state.unsatisfied.remove(req) {
                        state.satisfied.insert(req.clone(), classifier.file_path().to_owned());
                    } else if !state.satisfied.contains_key(req) && !not_required.contains(req) {
                        state.unexpected.insert(req.clone(), classifier.file_path().to_owned());
                    }
                }
                if classifier.by_content().len() == 1 {
                    let req = classifier.by_content().first().unwrap();
                    if !state.satisfied.contains_key(req) && not_required.contains(req) {
                        state.unneeded.insert(req.clone(), classifier.file_path().to_owned());
                    }
                }
            }
        }

        if !state.unsatisfied.is_empty() {
            for classifier in file_classifiers {
                if classifier.by_name().is_some() || !classifier.by_content().is_empty() {
                    continue;
                }
                if let Some(req) = classifier.by_content_fuzzy() {
                    if state.unsatisfied.remove(req) {
                        state.satisfied.insert(req.clone(), classifier.file_path().to_owned());
                        if state.unsatisfied.is_empty() {
                            break;
                        }
                    }
                }
            }
        }

        state
    }

    /// Update MODULE_LICENSE_* files in a directory based on the applicable licenses.
    /// These files are typically empty, and their name indicates the type of license that
    /// applies to the code, for example MODULE_LICENSE_APACHE2.
    pub fn update_module_license_files(&self, path: &impl AsRef<Path>) -> Result<(), Error> {
        let path = path.as_ref();
        for old_module_license_file in glob(&path.join("MODULE_LICENSE*").to_string_lossy())? {
            remove_file(old_module_license_file?)?;
        }
        for file in self.required_terms().map(|req| LICENSE_DATA.module_license_file(req).unwrap())
        {
            write(path.join(file), "")?; // Write an empty file. Essentially "touch".
        }
        Ok(())
    }

    /// Find the most restrictive type of license, for reporting in the METADATA file.
    pub fn most_restrictive_type(&self) -> LicenseType {
        LICENSE_DATA.most_restrictive_type(self.required_terms())
    }

    fn required_terms(&self) -> impl Iterator<Item = &LicenseReq> {
        self.satisfied.keys().chain(&self.unsatisfied)
    }
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

    let terms = LICENSE_DATA.evaluate_crate_license(crate_name, cargo_toml_license)?;
    Ok(LicenseState::from(
        terms,
        &Classifier::new_vec(
            crate_path.to_owned(),
            license_file_finder::find_license_files(crate_path)?,
        )?,
    ))
}

#[cfg(test)]
mod tests {
    use super::*;

    mod license_req {
        use spdx::{LicenseReq, Licensee};

        pub(super) fn apache() -> LicenseReq {
            Licensee::parse("Apache-2.0").unwrap().into_req()
        }
        pub(super) fn mit() -> LicenseReq {
            Licensee::parse("MIT").unwrap().into_req()
        }
    }

    mod license_terms {
        use crate::{LicenseTerms, LICENSE_DATA};

        pub(super) fn apache() -> LicenseTerms {
            LICENSE_DATA.evaluate_crate_license("foo", Some("Apache-2.0")).unwrap()
        }
        pub(super) fn apache_or_mit() -> LicenseTerms {
            LICENSE_DATA.evaluate_crate_license("foo", Some("Apache-2.0 OR MIT")).unwrap()
        }
        pub(super) fn apache_or_bsd() -> LicenseTerms {
            LICENSE_DATA.evaluate_crate_license("foo", Some("Apache-2.0 OR BSD-3-Clause")).unwrap()
        }
    }

    mod classifiers {
        use itertools::Itertools;

        use crate::Classifier;

        pub(super) fn apache_by_name() -> Classifier {
            Classifier::new("LICENSE-APACHE", "".to_string())
        }
        pub(super) fn apache_by_content() -> Classifier {
            Classifier::new("LICENSE", include_str!("licenses/Apache-2.0.txt").to_string())
        }
        pub(super) fn unknown() -> Classifier {
            Classifier::new("LICENSE", "".to_string())
        }
        pub(super) fn mit_by_name() -> Classifier {
            Classifier::new("LICENSE-MIT", "".to_string())
        }
        pub(super) fn apache_and_mit_concatenated() -> Classifier {
            Classifier::new(
                "LICENSE",
                [include_str!("licenses/Apache-2.0.txt"), include_str!("licenses/MIT.txt")]
                    .iter()
                    .join("\n\n\n-----\n\n\n"),
            )
        }
        pub(super) fn bsd_fuzzy() -> Classifier {
            Classifier::new(
                "LICENSE",
                include_str!("testdata/BSD-3-Clause-bindgen.txt").to_string(),
            )
        }
        pub(super) fn bsd_inexact() -> Classifier {
            Classifier::new("LICENSE-BSD", "".to_string())
        }
    }

    #[test]
    fn basic() {
        let state = LicenseState::from(
            license_terms::apache_or_mit(),
            &vec![classifiers::apache_by_name(), classifiers::mit_by_name()],
        );
        assert_eq!(
            state.satisfied,
            BTreeMap::from([(
                license_req::apache(),
                classifiers::apache_by_name().file_path().to_owned()
            )])
        );
        assert!(state.unsatisfied.is_empty());
        assert_eq!(
            state.unneeded,
            BTreeMap::from([(
                license_req::mit(),
                classifiers::mit_by_name().file_path().to_owned()
            )])
        );
        assert!(state.unexpected.is_empty());
    }

    #[test]
    fn unsatisfied() {
        let state = LicenseState::from(license_terms::apache_or_mit(), &vec![]);
        assert!(state.satisfied.is_empty());
        assert_eq!(state.unsatisfied, BTreeSet::from([license_req::apache()]));
        assert!(state.unneeded.is_empty());
        assert!(state.unexpected.is_empty());
    }

    #[test]
    fn unexpected() {
        let state = LicenseState::from(
            license_terms::apache(),
            &vec![classifiers::apache_by_name(), classifiers::mit_by_name()],
        );
        assert_eq!(
            state.satisfied,
            BTreeMap::from([(
                license_req::apache(),
                classifiers::apache_by_name().file_path().to_owned()
            )])
        );
        assert!(state.unsatisfied.is_empty());
        assert!(state.unneeded.is_empty());
        assert_eq!(
            state.unexpected,
            BTreeMap::from([(
                license_req::mit(),
                classifiers::mit_by_name().file_path().to_owned()
            )])
        );
    }

    #[test]
    fn name_preferred_to_content() {
        let state = LicenseState::from(
            license_terms::apache(),
            &vec![classifiers::apache_by_content(), classifiers::apache_by_name()],
        );
        assert_eq!(
            state.satisfied,
            BTreeMap::from([(
                license_req::apache(),
                classifiers::apache_by_name().file_path().to_owned()
            )])
        );
        assert!(state.unsatisfied.is_empty());
        assert!(state.unneeded.is_empty());
        assert!(state.unexpected.is_empty());
    }

    #[test]
    fn unknown_files_not_reported() {
        let state = LicenseState::from(
            license_terms::apache(),
            &vec![classifiers::apache_by_name(), classifiers::unknown()],
        );
        assert_eq!(
            state.satisfied,
            BTreeMap::from([(
                license_req::apache(),
                classifiers::apache_by_name().file_path().to_owned()
            )])
        );
        assert!(state.unsatisfied.is_empty());
        assert!(state.unneeded.is_empty());
        assert!(state.unexpected.is_empty());
    }

    #[test]
    fn concatenated_licenses_not_reported_as_unexpected() {
        let state = LicenseState::from(
            license_terms::apache_or_mit(),
            &vec![classifiers::apache_and_mit_concatenated()],
        );
        assert_eq!(
            state.satisfied,
            BTreeMap::from([(
                license_req::apache(),
                classifiers::apache_and_mit_concatenated().file_path().to_owned()
            )])
        );
        assert!(state.unsatisfied.is_empty());
        assert!(state.unneeded.is_empty());
        assert!(state.unexpected.is_empty());
    }

    #[test]
    fn fuzzy_classifications_not_reported_as_unneeded_or_unexpected() {
        let state = LicenseState::from(
            license_terms::apache_or_bsd(),
            &vec![classifiers::apache_by_name(), classifiers::bsd_fuzzy()],
        );
        assert_eq!(
            state.satisfied,
            BTreeMap::from([(
                license_req::apache(),
                classifiers::apache_by_name().file_path().to_owned()
            )])
        );
        assert!(state.unsatisfied.is_empty());
        assert!(state.unneeded.is_empty());
        assert!(state.unexpected.is_empty());

        let state = LicenseState::from(
            license_terms::apache(),
            &vec![classifiers::apache_by_name(), classifiers::bsd_fuzzy()],
        );
        assert_eq!(
            state.satisfied,
            BTreeMap::from([(
                license_req::apache(),
                classifiers::apache_by_name().file_path().to_owned()
            )])
        );
        assert!(state.unsatisfied.is_empty());
        assert!(state.unneeded.is_empty());
        assert!(state.unexpected.is_empty());
    }

    #[test]
    fn inexact_names_reported_as_unneeded_and_unexpected() {
        let state = LicenseState::from(
            license_terms::apache_or_bsd(),
            &vec![classifiers::apache_by_name(), classifiers::bsd_inexact()],
        );
        assert_eq!(
            state.satisfied,
            BTreeMap::from([(
                license_req::apache(),
                classifiers::apache_by_name().file_path().to_owned()
            )])
        );
        assert!(state.unsatisfied.is_empty());
        assert!(state.unneeded.is_empty());
        assert!(state.unexpected.is_empty());

        let state = LicenseState::from(
            license_terms::apache(),
            &vec![classifiers::apache_by_name(), classifiers::bsd_fuzzy()],
        );
        assert_eq!(
            state.satisfied,
            BTreeMap::from([(
                license_req::apache(),
                classifiers::apache_by_name().file_path().to_owned()
            )])
        );
        assert!(state.unsatisfied.is_empty());
        assert!(state.unneeded.is_empty());
        assert!(state.unexpected.is_empty());
    }
}
