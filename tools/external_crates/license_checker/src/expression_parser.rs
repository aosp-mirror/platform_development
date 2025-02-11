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

use spdx::{Expression, LicenseReq, Licensee};
use std::{
    collections::{BTreeMap, BTreeSet},
    sync::LazyLock,
};

use crate::Error;

pub(crate) fn get_chosen_licenses(
    crate_name: &str,
    cargo_toml_license: Option<&str>,
) -> Result<BTreeSet<LicenseReq>, Error> {
    Ok(BTreeSet::from_iter(
        Expression::parse(&get_spdx_expr(crate_name, cargo_toml_license)?)?
            .minimized_requirements(LICENSE_PREFERENCE.iter())?,
    ))
}

fn get_spdx_expr(crate_name: &str, cargo_toml_license: Option<&str>) -> Result<String, Error> {
    // Check special cases.
    if let Some((raw, expr)) = LICENSE_EXPR_SPECIAL_CASES.get(crate_name) {
        if *raw != cargo_toml_license {
            return Err(Error::LicenseExpressionSpecialCase {
                crate_name: crate_name.to_string(),
                expected_license: raw.unwrap_or_default().to_string(),
                cargo_toml_license: cargo_toml_license.unwrap_or_default().to_string(),
            });
        }
        return Ok(expr.to_string());
    }
    // Default. Look at the license field in Cargo.toml, and treat '/' as OR.
    if let Some(lic) = cargo_toml_license {
        if lic.contains('/') {
            Ok(lic.replace('/', " OR "))
        } else {
            Ok(lic.to_string())
        }
    } else {
        Err(Error::MissingLicenseField(crate_name.to_string()))
    }
}

static LICENSE_PREFERENCE: LazyLock<Vec<Licensee>> = LazyLock::new(|| {
    vec![
        "Apache-2.0",
        "MIT",
        "BSD-3-Clause",
        "BSD-2-Clause",
        "ISC",
        "MPL-2.0",
        "0BSD",
        "Unlicense",
        "Zlib",
        "Unicode-3.0",
        "Unicode-DFS-2016",
        "NCSA",
        "OpenSSL",
    ]
    .into_iter()
    .map(|l| Licensee::parse(l).unwrap())
    .collect()
});
static LICENSE_EXPR_SPECIAL_CASES: LazyLock<
    BTreeMap<&'static str, (Option<&'static str>, &'static str)>,
> = LazyLock::new(|| {
    BTreeMap::from([
        ("futures-channel", (Some("MIT OR Apache-2.0"), "(MIT OR Apache-2.0) AND BSD-2-Clause")),
        ("libfuzzer-sys", (Some("MIT/Apache-2.0/NCSA"), "(MIT OR Apache-2.0) AND NCSA")),
        ("ring", (None, "MIT AND ISC AND OpenSSL")),
        ("webpki", (None, "ISC AND BSD-3-Clause")),
    ])
});

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_get_spdx_expr() -> Result<(), Error> {
        assert_eq!(get_spdx_expr("foo", Some("MIT"))?, "MIT");

        // No license, no exception
        assert!(get_spdx_expr("foo", None).is_err());

        // '/' treated as OR
        assert_eq!(get_spdx_expr("foo", Some("MIT/Apache-2.0"))?, "MIT OR Apache-2.0");

        // Exceptions.
        assert_eq!(
            get_spdx_expr("libfuzzer-sys", Some("MIT/Apache-2.0/NCSA"))?,
            "(MIT OR Apache-2.0) AND NCSA"
        );
        assert_eq!(get_spdx_expr("ring", None)?, "MIT AND ISC AND OpenSSL");

        // Exceptions. Raw license in Cargo.toml must match, if present.
        assert!(get_spdx_expr("libfuzzer-sys", Some("blah")).is_err());
        assert!(get_spdx_expr("libfuzzer-sys", None).is_err());
        assert!(get_spdx_expr("ring", Some("blah")).is_err());

        Ok(())
    }

    #[test]
    fn test_get_chosen_licenses() -> Result<(), Error> {
        assert_eq!(
            get_chosen_licenses("foo", Some("MIT"))?,
            BTreeSet::from([Licensee::parse("MIT").unwrap().into_req()]),
            "Simple case"
        );
        assert_eq!(
            get_chosen_licenses("foo", Some("MIT OR Apache-2.0"))?,
            BTreeSet::from([Licensee::parse("Apache-2.0").unwrap().into_req()]),
            "Apache preferred to MIT"
        );
        assert!(get_chosen_licenses("foo", Some("GPL")).is_err(), "Unacceptable license");
        assert!(get_chosen_licenses("foo", Some("blah")).is_err(), "Unrecognized license");
        assert_eq!(
            get_chosen_licenses("foo", Some("MIT AND Apache-2.0"))?,
            BTreeSet::from([
                Licensee::parse("Apache-2.0").unwrap().into_req(),
                Licensee::parse("MIT").unwrap().into_req()
            ]),
            "Apache preferred to MIT"
        );
        assert_eq!(
            get_chosen_licenses("foo", Some("MIT AND (MIT OR Apache-2.0)"))?,
            BTreeSet::from([Licensee::parse("MIT").unwrap().into_req()]),
            "Complex expression from libm 0.2.11"
        );
        assert_eq!(
            get_chosen_licenses("webpki", None)?,
            BTreeSet::from([
                Licensee::parse("ISC").unwrap().into_req(),
                Licensee::parse("BSD-3-Clause").unwrap().into_req()
            ]),
            "Exception"
        );
        Ok(())
    }
}
