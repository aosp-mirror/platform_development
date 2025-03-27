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
    ffi::{OsStr, OsString},
};

use google_metadata::LicenseType;

use crate::{
    license_file_finder::is_findable, util::strip_punctuation, CrateLicenseSpecialCase, Error,
    License,
};

#[derive(Debug)]
pub(crate) struct ParsedLicense {
    // The SPDX identifier, e.g. "Apache-2.0"
    licensee: spdx::Licensee,
    // The processed text of the license (lowercased, punctuation stripped, etc.) used for matching.
    processed_text: Option<String>,
    // A set of file names, any of which unambiguously identify the license.
    file_names: BTreeSet<OsString>,
    /// The name of the MODULE_LICENSE_* file for this license.
    module_license_file_name: &'static str,
    /// The license type, per http://go/thirdpartylicenses#types
    license_type: LicenseType,
}

impl ParsedLicense {
    pub fn licensee(&self) -> &spdx::Licensee {
        &self.licensee
    }
    pub fn license_req(&self) -> spdx::LicenseReq {
        self.licensee.clone().into_req()
    }
    pub fn processed_text(&self) -> Option<&str> {
        self.processed_text.as_deref()
    }
    pub fn file_names(&self) -> &BTreeSet<OsString> {
        &self.file_names
    }
    pub fn module_license_file_name(&self) -> &'static str {
        self.module_license_file_name
    }
    pub fn license_type(&self) -> LicenseType {
        self.license_type
    }
    pub fn is_substring_of(&self, other: &str) -> bool {
        self.processed_text.as_ref().is_some_and(|text| other.contains(text.as_str()))
    }
}

impl TryFrom<&License> for ParsedLicense {
    type Error = crate::Error;

    fn try_from(value: &License) -> Result<Self, Self::Error> {
        if value.text.is_none() && value.file_names.is_empty() {
            return Err(Error::LicenseWithoutTextOrFileNames(value.name.to_string()));
        }

        let processed_text = value.text.map(strip_punctuation);
        if processed_text.as_ref().is_some_and(String::is_empty) {
            return Err(Error::EmptyLicenseText(value.name.to_string()));
        }

        let mut file_names = BTreeSet::new();
        for license_file in value.file_names {
            if !is_findable(OsStr::new(license_file)) {
                return Err(Error::LicenseFileNotFindable(
                    license_file.to_string(),
                    value.name.to_string(),
                ));
            }
            file_names.insert(OsString::from(license_file.to_uppercase()));
        }

        if !value.module_license_file_name.starts_with("MODULE_LICENSE_") {
            return Err(Error::InvalidModuleLicenseFileName(
                value.module_license_file_name.to_string(),
            ));
        }

        if matches!(
            value.license_type,
            LicenseType::UNKNOWN
                | LicenseType::BY_EXCEPTION_ONLY
                | LicenseType::RESTRICTED_IF_STATICALLY_LINKED
                | LicenseType::RESTRICTED
        ) {
            return Err(Error::LicenseTypeTooRestrictive(value.license_type));
        }

        Ok(ParsedLicense {
            licensee: spdx::Licensee::parse(value.name)?,
            processed_text: value.text.map(strip_punctuation),
            file_names,
            module_license_file_name: value.module_license_file_name,
            license_type: value.license_type,
        })
    }
}

#[derive(Debug)]
pub(crate) struct ParsedCrateLicenseSpecialCase {
    // The name of the crate.
    crate_name: &'static str,
    // The incorrect or missing license expression in Cargo.toml
    cargo_toml_license: Option<&'static str>,
    // The corrected license expression.
    corrected_license_expr: spdx::Expression,
}

impl TryFrom<&CrateLicenseSpecialCase> for ParsedCrateLicenseSpecialCase {
    type Error = crate::Error;

    fn try_from(value: &CrateLicenseSpecialCase) -> Result<Self, Self::Error> {
        Ok(ParsedCrateLicenseSpecialCase {
            crate_name: value.crate_name,
            cargo_toml_license: value.cargo_toml_license,
            corrected_license_expr: spdx::Expression::parse(value.corrected_license_expr)?,
        })
    }
}

#[derive(Debug)]
pub(crate) struct CrateLicenseSpecialCases {
    special_cases: BTreeMap<&'static str, ParsedCrateLicenseSpecialCase>,
}

impl CrateLicenseSpecialCases {
    pub fn get_corrected_license(
        &self,
        crate_name: &str,
        cargo_toml_license: Option<&str>,
    ) -> Result<spdx::Expression, Error> {
        // Check special cases.
        if let Some(special_case) = self.special_cases.get(crate_name) {
            if special_case.cargo_toml_license != cargo_toml_license {
                return Err(Error::LicenseExpressionSpecialCaseMismatch {
                    crate_name: crate_name.to_string(),
                    expected_license: special_case
                        .cargo_toml_license
                        .unwrap_or("<None>")
                        .to_string(),
                    cargo_toml_license: cargo_toml_license.unwrap_or("<None>").to_string(),
                });
            }
            return Ok(special_case.corrected_license_expr.clone());
        }
        // Default. Look at the license field in Cargo.toml, and treat '/' as OR.
        if let Some(lic) = cargo_toml_license {
            Ok(spdx::Expression::parse(&lic.replace('/', " OR "))?)
        } else {
            Err(Error::MissingLicenseField(crate_name.to_string()))
        }
    }
}

impl TryFrom<&[CrateLicenseSpecialCase]> for CrateLicenseSpecialCases {
    type Error = crate::Error;

    fn try_from(value: &[CrateLicenseSpecialCase]) -> Result<Self, Self::Error> {
        // BTreeMap::from() doesn't care about duplicate keys, but having multiple special cases
        // for the same crate is an error.
        let mut special_cases = BTreeMap::new();
        for special_case in value {
            let parsed_special_case = ParsedCrateLicenseSpecialCase::try_from(special_case)?;
            if special_cases.insert(parsed_special_case.crate_name, parsed_special_case).is_some() {
                return Err(Error::DuplicateCrateLicenseSpecialCase(
                    special_case.crate_name.to_string(),
                ));
            }
        }

        Ok(CrateLicenseSpecialCases { special_cases })
    }
}

#[cfg(test)]
mod parsed_license_tests {
    use super::*;

    #[test]
    fn invalid_license_name() {
        assert!(matches!(
            ParsedLicense::try_from(&License {
                name: "foo",
                text: None,
                file_names: &["LICENSE"],
                module_license_file_name: "MODULE_LICENSE_FOO",
                license_type: LicenseType::NOTICE,
            }),
            Err(Error::LicenseParseError(_))
        ));
    }

    #[test]
    fn missing_both_text_and_file_name() {
        assert!(matches!(
            ParsedLicense::try_from(&License {
                name: "MIT",
                text: None,
                file_names: &[],
                module_license_file_name: "MODULE_LICENSE_MIT",
                license_type: LicenseType::NOTICE,
            }),
            Err(Error::LicenseWithoutTextOrFileNames(_))
        ));
    }

    #[test]
    fn empty_license_text() {
        assert!(matches!(
            ParsedLicense::try_from(&License {
                name: "MIT",
                text: Some(" "),
                file_names: &["LICENSE-MIT"],
                module_license_file_name: "MODULE_LICENSE_MIT",
                license_type: LicenseType::NOTICE,
            }),
            Err(Error::EmptyLicenseText(_))
        ));
    }

    #[test]
    fn invalid_module_license_file() {
        assert!(matches!(
            ParsedLicense::try_from(&License {
                name: "MIT",
                text: None,
                file_names: &["LICENSE-MIT"],
                module_license_file_name: "blah",
                license_type: LicenseType::NOTICE,
            }),
            Err(Error::InvalidModuleLicenseFileName(_))
        ));
    }

    #[test]
    fn restrictive_licenses_rejected() {
        for license_type in [
            LicenseType::UNKNOWN,
            LicenseType::BY_EXCEPTION_ONLY,
            LicenseType::RESTRICTED,
            LicenseType::RESTRICTED_IF_STATICALLY_LINKED,
        ] {
            assert!(matches!(
                ParsedLicense::try_from(&License {
                    name: "MIT",
                    text: None,
                    file_names: &["LICENSE-MIT"],
                    module_license_file_name: "MODULE_LICENSE_MIT",
                    license_type,
                }),
                Err(Error::LicenseTypeTooRestrictive(_))
            ));
        }
    }

    #[test]
    fn is_substring_of() {
        let license: ParsedLicense = ParsedLicense::try_from(&License {
            name: "MIT",
            text: Some("foo"),
            file_names: &["LICENSE-MIT"],
            module_license_file_name: "MODULE_LICENSE_MIT",
            license_type: LicenseType::NOTICE,
        })
        .unwrap();
        assert!(license.is_substring_of("foobar"));
        assert!(!license.is_substring_of("asdf"));
        let license: ParsedLicense = ParsedLicense::try_from(&License {
            name: "MIT",
            text: None,
            file_names: &["LICENSE-MIT"],
            module_license_file_name: "MODULE_LICENSE_MIT",
            license_type: LicenseType::NOTICE,
        })
        .unwrap();
        assert!(
            !license.is_substring_of("blah"),
            "Missing license text is not a substring of anything"
        );
    }
}

#[cfg(test)]
mod crate_license_special_case_tests {
    use super::*;

    #[test]
    fn unparseable_special_case() {
        assert!(matches!(
            ParsedCrateLicenseSpecialCase::try_from(&CrateLicenseSpecialCase {
                crate_name: "foo",
                cargo_toml_license: None,
                corrected_license_expr: "foo"
            }),
            Err(Error::LicenseParseError(_))
        ))
    }

    #[test]
    fn duplicate_special_cases() {
        assert!(matches!(
            CrateLicenseSpecialCases::try_from(
                [
                    CrateLicenseSpecialCase {
                        crate_name: "foo",
                        cargo_toml_license: None,
                        corrected_license_expr: "MIT"
                    },
                    CrateLicenseSpecialCase {
                        crate_name: "foo",
                        cargo_toml_license: None,
                        corrected_license_expr: "MIT"
                    }
                ]
                .as_slice()
            ),
            Err(Error::DuplicateCrateLicenseSpecialCase(_))
        ));
    }

    #[test]
    fn get_corrected_license_expr() {
        let special_cases = CrateLicenseSpecialCases::try_from(
            [CrateLicenseSpecialCase {
                crate_name: "foo",
                cargo_toml_license: None,
                corrected_license_expr: "MIT",
            }]
            .as_slice(),
        )
        .unwrap();
        assert_eq!(
            special_cases.get_corrected_license("bar", Some("Apache-2.0")).unwrap(),
            spdx::Expression::parse("Apache-2.0").unwrap(),
            "No special case"
        );
        assert_eq!(
            special_cases.get_corrected_license("foo", None).unwrap(),
            spdx::Expression::parse("MIT").unwrap(),
            "Special case match"
        );
        assert!(
            matches!(
                special_cases.get_corrected_license("foo", Some("MIT")),
                Err(Error::LicenseExpressionSpecialCaseMismatch {
                    crate_name: _,
                    expected_license: _,
                    cargo_toml_license: _
                })
            ),
            "Special case mismatch"
        );
        assert_eq!(
            special_cases.get_corrected_license("bar", Some("Apache-2.0/MIT")).unwrap(),
            spdx::Expression::parse("Apache-2.0 OR MIT").unwrap(),
            "/ treated as OR"
        );
    }
}
