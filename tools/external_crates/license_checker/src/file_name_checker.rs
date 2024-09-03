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
    collections::BTreeMap,
    ffi::{OsStr, OsString},
    path::Path,
};

use lazy_static::lazy_static;
use spdx::{LicenseReq, Licensee};

pub(crate) fn classify_license_file_name(file: impl AsRef<Path>) -> Option<LicenseReq> {
    // File should be relative
    let file = file.as_ref();
    let mut basename = if file.extension() == Some(OsStr::new("txt"))
        || file.extension() == Some(OsStr::new("md"))
    {
        file.with_extension("")
    } else {
        file.to_owned()
    };
    let uppercase_name = basename.as_mut_os_string();
    uppercase_name.make_ascii_uppercase();
    if let Some(req) = LICENSE_FILE_NAME_CLASSIFICATION.get(uppercase_name) {
        return Some(req.clone());
    }
    None
}

lazy_static! {
    static ref LICENSE_FILE_NAME_CLASSIFICATION: BTreeMap<OsString, LicenseReq> =
        // Filenames are case-insensitive.
        vec![
            ("LICENSE-MIT", "MIT"),
            ("LICENSES/MIT", "MIT"),

            ("LICENSE-APACHE", "Apache-2.0"),
            ("LICENSE-APACHE-2.0", "Apache-2.0"),
            ("LICENSES/Apache-2.0", "Apache-2.0"),

            ("LICENSE-BSD-3-Clause", "BSD-3-Clause"),

            ("LICENSE-UNICODE", "Unicode-DFS-2016"),

            ("LICENSE-0BSD", "0BSD"),

            ("LICENSE-ZLIB", "Zlib"),

            ("UNLICENSE", "Unlicense"),
        ]
        .into_iter()
        .map(|(file, req)| (OsString::from(file.to_uppercase()), Licensee::parse(req).unwrap().into_req()))
        .collect();
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_classify() {
        assert!(classify_license_file_name(Path::new("LICENSE")).is_none(), "Generic license name");
        assert_eq!(
            classify_license_file_name(Path::new("UNLICENSE")),
            Some(Licensee::parse("Unlicense").unwrap().into_req()),
            "Unlicense"
        );
        assert_eq!(
            classify_license_file_name(Path::new("LICENSE-MIT")),
            Some(Licensee::parse("MIT").unwrap().into_req()),
            "Standard name"
        );
        assert_eq!(
            classify_license_file_name(Path::new("LICENSE-APACHE")),
            Some(Licensee::parse("Apache-2.0").unwrap().into_req()),
            "Standard name"
        );
        assert_eq!(
            classify_license_file_name(Path::new("LICENSE-apache")),
            Some(Licensee::parse("Apache-2.0").unwrap().into_req()),
            "Case-insensitive"
        );
        assert_eq!(
            classify_license_file_name(Path::new("LICENSE-APACHE.md")),
            Some(Licensee::parse("Apache-2.0").unwrap().into_req()),
            ".md extension ignored"
        );
        assert_eq!(
            classify_license_file_name(Path::new("LICENSE-0BSD.txt")),
            Some(Licensee::parse("0BSD").unwrap().into_req()),
            ".txt extension ignored"
        );
        assert!(
            classify_license_file_name(Path::new("LICENSE-0BSD.jpg")).is_none(),
            "Random extension preserved"
        );
        assert_eq!(
            classify_license_file_name(Path::new("LICENSES/MIT")),
            Some(Licensee::parse("MIT").unwrap().into_req()),
            "Subdirectory"
        );
        assert_eq!(
            classify_license_file_name(Path::new("LICENSES/Apache-2.0")),
            Some(Licensee::parse("Apache-2.0").unwrap().into_req()),
            "Subdirectory"
        );
    }
}
