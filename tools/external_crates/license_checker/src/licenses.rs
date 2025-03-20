// Copyright (C) 2025 The Android Open Source Project
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
    ffi::OsString,
    path::Path,
    sync::LazyLock,
};

use itertools::Itertools;
use spdx::Licensee;
use textdistance::str::ratcliff_obershelp;

use crate::{
    license_data::{CRATE_LICENSE_SPECIAL_CASES, LICENSES, LICENSE_PREFERENCE},
    util::{normalize_filename, strip_punctuation},
    CrateLicenseSpecialCase, CrateLicenseSpecialCases, Error, License, LicenseTerms, ParsedLicense,
};

#[derive(Debug)]
pub(crate) struct Licenses {
    licenses: BTreeMap<Licensee, ParsedLicense>,
    license_preference: Vec<Licensee>,
    crate_license_special_cases: CrateLicenseSpecialCases,
    license_file_names: BTreeMap<OsString, Licensee>,
}

impl Licenses {
    fn new(
        raw_licenses: &'static [License],
        license_preference: &[&str],
        crate_license_special_cases: &'static [CrateLicenseSpecialCase],
    ) -> Result<Licenses, Error> {
        if raw_licenses.is_empty() {
            return Err(Error::NoLicenses);
        }

        let mut licenses = BTreeMap::new();
        let mut license_file_names = BTreeMap::new();
        for license in raw_licenses {
            let parsed = ParsedLicense::try_from(license)?;
            let licensee = parsed.licensee().clone();
            for file_name in parsed.file_names() {
                if let Some(other) = license_file_names.insert(file_name.clone(), licensee.clone())
                {
                    return Err(Error::DuplicateLicenseFileName {
                        file_name: file_name.to_string_lossy().into_owned(),
                        license: parsed.licensee().to_string(),
                        other_license: other.to_string(),
                    });
                }
            }
            if licenses.insert(licensee.clone(), parsed).is_some() {
                return Err(Error::DuplicateLicense(licensee.to_string()));
            }
        }

        let mut ranked_licenses = Vec::new();
        for pref in license_preference {
            let licensee = Licensee::parse(pref)?;
            if !licenses.contains_key(&licensee) {
                return Err(Error::LicensePreferenceForUnknownLicense(pref.to_string()));
            }
            ranked_licenses.push(licensee);
        }
        let unranked_licenses = licenses
            .keys()
            .filter_map(|l| if !ranked_licenses.contains(l) { Some(l.clone()) } else { None })
            .collect::<Vec<_>>();
        let license_preference = ranked_licenses.into_iter().chain(unranked_licenses).collect();

        let licenses = Licenses {
            licenses,
            license_preference,
            crate_license_special_cases: crate_license_special_cases.try_into()?,
            license_file_names,
        };
        licenses.validate()?;
        Ok(licenses)
    }

    fn validate(&self) -> Result<(), Error> {
        for (licensee, license) in &self.licenses {
            // The license text can't be a substring of any other license text.
            for (other_licensee, other_license) in &self.licenses {
                if licensee != other_licensee
                    && license
                        .processed_text()
                        .is_some_and(|text| other_license.is_substring_of(text))
                {
                    return Err(Error::AmbiguousLicenseText(
                        other_license.licensee().to_string(),
                        license.licensee().to_string(),
                    ));
                }
            }
        }

        Ok(())
    }

    /// Evaluate the SPDX license expression from Cargo.toml for a given crate.
    /// Slashes such as "MIT/Apache-2.0" are interpreted as OR.
    /// A limited set of exceptions are applied for crates where the license terms are
    /// known to be missing or incorrect.
    pub fn evaluate_crate_license(
        &self,
        crate_name: &str,
        cargo_toml_license: Option<&str>,
    ) -> Result<LicenseTerms, Error> {
        LicenseTerms::try_from(
            self.crate_license_special_cases
                .get_corrected_license(crate_name, cargo_toml_license)?,
            &self.license_preference,
        )
    }

    pub fn classify_file_name(&self, file: impl AsRef<Path>) -> Option<&Licensee> {
        self.license_file_names.get(&normalize_filename(file))
    }

    /// Classify file contents by exact substring match on the license text.
    pub fn classify_file_contents(&self, contents: &str) -> BTreeSet<Licensee> {
        let contents = strip_punctuation(contents);

        let mut matches = BTreeSet::new();
        for license in self.licenses.values() {
            if license.is_substring_of(contents.as_str()) {
                matches.insert(license.licensee().clone());
            }
        }
        matches
    }

    pub fn classify_file_contents_fuzzy(&self, contents: &str) -> Option<Licensee> {
        let contents = strip_punctuation(contents);

        // Fuzzy match. This is expensive, so start with licenses that are closest in length to the file,
        // and only return a single match at most.
        for license in
            self.licenses.values().filter(|l| l.processed_text().is_some()).sorted_by(|a, b| {
                let mut ra = a.processed_text().unwrap().len() as f32 / contents.len() as f32;
                let mut rb = b.processed_text().unwrap().len() as f32 / contents.len() as f32;
                if ra > 1.0 {
                    ra = 1.0 / ra;
                }
                if rb > 1.0 {
                    rb = 1.0 / rb;
                }
                rb.partial_cmp(&ra).unwrap()
            })
        {
            if let Some(processed_text) = license.processed_text() {
                let similarity = ratcliff_obershelp(contents.as_str(), processed_text);
                if similarity > 0.95 {
                    return Some(license.licensee().clone());
                }
            }
        }

        None
    }
}

pub(crate) static LICENSE_DATA: LazyLock<Licenses> = LazyLock::new(|| {
    Licenses::new(LICENSES, LICENSE_PREFERENCE, CRATE_LICENSE_SPECIAL_CASES).unwrap()
});

#[cfg(test)]
mod tests {
    use std::collections::BTreeSet;

    use super::*;

    #[test]
    fn static_data_sanity_test() {
        assert_eq!(LICENSES.len(), LICENSE_DATA.licenses.len());
        assert_eq!(LICENSE_DATA.license_preference.len(), LICENSE_DATA.licenses.len());
    }

    #[test]
    fn basic() {
        assert!(Licenses::new(
            &[
                License { name: "Apache-2.0", text: None, file_names: &["LICENSE-APACHE"] },
                License { name: "MIT", text: None, file_names: &["LICENSE-MIT"] },
                License { name: "BSD-3-Clause", text: None, file_names: &["LICENSE-BSD-3-Clause"] },
            ],
            &["Apache-2.0", "MIT"],
            &[],
        )
        .is_ok());
    }

    #[test]
    fn no_licenses() {
        assert!(matches!(Licenses::new(&[], &[], &[]), Err(Error::NoLicenses)));
    }

    #[test]
    fn duplicate_license() {
        assert!(matches!(
            Licenses::new(
                &[
                    License { name: "MIT", text: None, file_names: &["LICENSE-foo"] },
                    License { name: "MIT", text: None, file_names: &["LICENSE-bar"] }
                ],
                &[],
                &[],
            ),
            Err(Error::DuplicateLicense(_))
        ));
    }

    #[test]
    fn license_text_substrings() {
        assert!(matches!(
            Licenses::new(
                &[
                    License { name: "Apache-2.0", text: Some("foo"), file_names: &[] },
                    License { name: "MIT", text: Some("foobar"), file_names: &[] }
                ],
                &[],
                &[],
            ),
            Err(Error::AmbiguousLicenseText(_, _,))
        ));
    }

    #[test]
    fn duplicate_license_file_names() {
        assert!(matches!(
            Licenses::new(
                &[
                    License { name: "Apache-2.0", text: None, file_names: &["LICENSE"] },
                    License { name: "MIT", text: None, file_names: &["LICENSE"] }
                ],
                &[],
                &[],
            ),
            Err(Error::DuplicateLicenseFileName { file_name: _, license: _, other_license: _ })
        ));
    }

    #[test]
    fn unfindable_license_file() {
        assert!(matches!(
            Licenses::new(&[License { name: "MIT", text: None, file_names: &["foo"] },], &[], &[],),
            Err(Error::LicenseFileNotFindable(_, _))
        ));
    }

    #[test]
    fn preference_for_unknown_license() {
        assert!(matches!(
            Licenses::new(
                &[License { name: "MIT", text: None, file_names: &["LICENSE-MIT"] }],
                &["foo"],
                &[],
            ),
            Err(Error::LicenseParseError(_))
        ));
        assert!(matches!(
            Licenses::new(
                &[License { name: "MIT", text: None, file_names: &["LICENSE-MIT"] }],
                &["Apache-2.0"],
                &[],
            ),
            Err(Error::LicensePreferenceForUnknownLicense(_))
        ));
    }

    #[test]
    fn evaluate_crate_license() {
        let licenses = Licenses::new(
            &[
                License { name: "Apache-2.0", text: None, file_names: &["LICENSE-APACHE"] },
                License { name: "MIT", text: None, file_names: &["LICENSE-MIT"] },
            ],
            &["Apache-2.0", "MIT"],
            &[],
        )
        .unwrap();
        assert_eq!(
            licenses.evaluate_crate_license("foo", Some("Apache-2.0 OR MIT")).unwrap(),
            LicenseTerms {
                required: BTreeSet::from([Licensee::parse("Apache-2.0").unwrap().into_req()]),
                not_required: BTreeSet::from([Licensee::parse("MIT").unwrap().into_req()])
            }
        );
        assert!(
            matches!(
                licenses.evaluate_crate_license("foo", Some("BSD-3-Clause")),
                Err(Error::MinimizeError(_))
            ),
            "Unknown license"
        );
        assert!(
            matches!(
                licenses.evaluate_crate_license("foo", None),
                Err(Error::MissingLicenseField(_))
            ),
            "No license and no special case"
        );
    }
}
