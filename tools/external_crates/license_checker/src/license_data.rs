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

/// A license and the means for recognizing it.
/// We must be able to recognize it either by content, by the `text` field,
/// or by file name, by the `file_names` field, or both.
#[derive(Debug)]
pub(crate) struct License {
    /// The SPDX identifier, e.g. "Apache-2.0"
    pub name: &'static str,
    /// The text of the license.
    pub text: Option<&'static str>,
    /// A set of file names, any of which unambiguously identify the license.
    pub file_names: &'static [&'static str],
}

/// A list of all the licenses we recognize and accept.
/// Only acceptable licenses should go in this list. Please see go/thirdpartylicenses
/// for information on what this means.
pub(crate) static LICENSES: &[License] = &[
    License { name: "0BSD", text: None, file_names: &["LICENSE-0BSD"] },
    License {
        name: "Apache-2.0",
        text: Some(include_str!("licenses/Apache-2.0.txt")),
        file_names: &[
            "LICENSE-APACHE",
            "LICENSE-APACHE-2.0",
            "LICENSES/Apache-2.0",
            "docs/LICENSE-APACHE",
        ],
    },
    License {
        name: "Apache-2.0 WITH LLVM-exception",
        text: None,
        file_names: &["LICENSE-Apache-2.0_WITH_LLVM-exception"],
    },
    License {
        name: "BSD-2-Clause",
        text: Some(include_str!("licenses/BSD-2-Clause.txt")),
        // Note: LICENSE-BSD is not unambiguous. Could be 2-clause or 3-clause
        file_names: &["LICENSE-BSD-2-Clause", "LICENSE.BSD-2-Clause"],
    },
    License {
        name: "BSD-3-Clause",
        text: Some(include_str!("licenses/BSD-3-Clause.txt")),
        // Note: LICENSE-BSD is not unambiguous. Could be 2-clause or 3-clause
        file_names: &["LICENSE-BSD-3-Clause"],
    },
    License { name: "ISC", text: Some(include_str!("licenses/ISC.txt")), file_names: &[] },
    License {
        name: "MIT",
        text: Some(include_str!("licenses/MIT.txt")),
        file_names: &["LICENSE-MIT", "LICENSES/MIT", "docs/LICENSE-MIT"],
    },
    License { name: "MIT-0", text: Some(include_str!("licenses/MIT-0.txt")), file_names: &[] },
    License { name: "MPL-2.0", text: Some(include_str!("licenses/MPL-2.0.txt")), file_names: &[] },
    License { name: "NCSA", text: Some(include_str!("licenses/NCSA.txt")), file_names: &[] },
    License { name: "OpenSSL", text: Some(include_str!("licenses/OpenSSL.txt")), file_names: &[] },
    License {
        name: "Unicode-3.0",
        text: Some(include_str!("licenses/Unicode-3.0.txt")),
        // Note: LICENSE-UNICODE is not unambiguous
        file_names: &[],
    },
    License {
        name: "Unicode-DFS-2016",
        text: Some(include_str!("licenses/Unicode-DFS-2016.txt")),
        // Note: LICENSE-UNICODE is not unambiguous
        file_names: &[],
    },
    License {
        name: "Unlicense",
        text: Some(include_str!("licenses/Unlicense.txt")),
        file_names: &["UNLICENSE"],
    },
    License {
        name: "Zlib",
        text: Some(include_str!("licenses/Zlib.txt")),
        file_names: &["LICENSE-ZLIB"],
    },
    License { name: "BSL-1.0", text: None, file_names: &["LICENSE-BOOST"] },
    License { name: "CC0-1.0", text: None, file_names: &["LICENSES/CC0-1.0"] },
];

/// Specifies the order of preference for choosing a license.
/// The most preferred license goes first.
/// For example, the most common license for Rust crates is "Apache-2.0 OR MIT", in
/// which case we will choose Apache-2.0.
pub(crate) static LICENSE_PREFERENCE: &[&str] =
    &["Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause"];

#[derive(Debug)]
pub(crate) struct CrateLicenseSpecialCase {
    // The name of the crate.
    pub crate_name: &'static str,
    // The incorrect or missing license expression in Cargo.toml
    pub cargo_toml_license: Option<&'static str>,
    // The corrected license expression.
    pub corrected_license_expr: &'static str,
}

/// A list of special cases where the license terms in Cargo.toml are
/// missing or incorrect. In order to apply a licensing special case,
/// both `crate_name` and `cargo_toml_license` must match. This ensures
/// that if the crate maintainers change the license, we will get
/// an error when trying to update the crate, and be forced to
/// update the special case.
pub(crate) static CRATE_LICENSE_SPECIAL_CASES: &[CrateLicenseSpecialCase] = &[
    CrateLicenseSpecialCase {
        crate_name: "futures-channel",
        cargo_toml_license: Some("MIT OR Apache-2.0"),
        corrected_license_expr: "(MIT OR Apache-2.0) AND BSD-2-Clause",
    },
    CrateLicenseSpecialCase {
        crate_name: "libfuzzer-sys",
        // Slash characters are not valid in SPDX expressions, but
        // are nevertheless very common and usually indicate "OR",
        // which is how we interpret them. But that is not correct in this case.
        cargo_toml_license: Some("MIT/Apache-2.0/NCSA"),
        corrected_license_expr: "(MIT OR Apache-2.0) AND NCSA",
    },
    CrateLicenseSpecialCase {
        crate_name: "merge",
        cargo_toml_license: Some("Apache-2.0 OR MIT"),
        corrected_license_expr: "(Apache-2.0 OR MIT) AND CC0-1.0",
    },
    CrateLicenseSpecialCase {
        crate_name: "ring",
        cargo_toml_license: None,
        corrected_license_expr: "MIT AND ISC AND OpenSSL",
    },
    CrateLicenseSpecialCase {
        crate_name: "tonic",
        cargo_toml_license: Some("MIT"),
        corrected_license_expr: "MIT AND Apache-2.0",
    },
    CrateLicenseSpecialCase {
        crate_name: "webpki",
        cargo_toml_license: None,
        corrected_license_expr: "ISC AND BSD-3-Clause",
    },
];
