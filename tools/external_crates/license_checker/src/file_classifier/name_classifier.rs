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

use std::{collections::BTreeMap, ffi::OsString, path::Path, sync::LazyLock};

use spdx::{LicenseReq, Licensee};

use super::normalize_filename;

pub(super) fn classify_license_file_name(file: impl AsRef<Path>) -> Option<LicenseReq> {
    if let Some(req) = LICENSE_FILE_NAME_CLASSIFICATION.get(&normalize_filename(file)) {
        return Some(req.clone());
    }
    None
}

static LICENSE_FILE_NAME_CLASSIFICATION: LazyLock<BTreeMap<OsString, LicenseReq>> = LazyLock::new(
    ||
        // Filenames are case-insensitive.
        vec![
            ("LICENSE-MIT", "MIT"),
            ("LICENSES/MIT", "MIT"),
            ("docs/LICENSE-MIT", "MIT"),

            ("LICENSE-APACHE", "Apache-2.0"),
            ("LICENSE-APACHE-2.0", "Apache-2.0"),
            ("LICENSES/Apache-2.0", "Apache-2.0"),
            ("LICENSE-Apache-2.0_WITH_LLVM-exception", "Apache-2.0 WITH LLVM-exception"),
            ("docs/LICENSE-APACHE", "Apache-2.0"),

            ("LICENSE-BSD-2-Clause", "BSD-2-Clause"),
            ("LICENSE.BSD-2-Clause", "BSD-2-Clause"),
            ("LICENSE-BSD-3-Clause", "BSD-3-Clause"),

            ("LICENSE-0BSD", "0BSD"),

            ("LICENSE-ZLIB", "Zlib"),
            ("UNLICENSE", "Unlicense"),
            ("LICENSE-BOOST", "BSL-1.0"),
            ("LICENSES/CC0-1.0", "CC0-1.0"),
        ]
        .into_iter()
        .map(|(file, req)| (OsString::from(file.to_uppercase()), Licensee::parse(req).unwrap().into_req()))
        .collect(),
);

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
