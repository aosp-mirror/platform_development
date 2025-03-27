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

#[cfg(test)]
mod name_classification {
    use crate::LICENSE_DATA;
    use spdx::Licensee;
    use std::path::Path;

    #[test]
    fn file_name_tests() {
        assert!(
            LICENSE_DATA.classify_file_name(Path::new("LICENSE")).is_none(),
            "Generic license name"
        );
        assert_eq!(
            LICENSE_DATA.classify_file_name(Path::new("UNLICENSE")),
            Some(&Licensee::parse("Unlicense").unwrap().into_req()),
            "Unlicense"
        );
        assert_eq!(
            LICENSE_DATA.classify_file_name(Path::new("LICENSE-MIT")),
            Some(&Licensee::parse("MIT").unwrap().into_req()),
            "Standard name"
        );
        assert_eq!(
            LICENSE_DATA.classify_file_name(Path::new("LICENSE-APACHE")),
            Some(&Licensee::parse("Apache-2.0").unwrap().into_req()),
            "Standard name"
        );
        assert_eq!(
            LICENSE_DATA.classify_file_name(Path::new("LICENSE-apache")),
            Some(&Licensee::parse("Apache-2.0").unwrap().into_req()),
            "Case-insensitive"
        );
        assert_eq!(
            LICENSE_DATA.classify_file_name(Path::new("LICENSE-APACHE.md")),
            Some(&Licensee::parse("Apache-2.0").unwrap().into_req()),
            ".md extension ignored"
        );
        assert_eq!(
            LICENSE_DATA.classify_file_name(Path::new("LICENSE-0BSD.txt")),
            Some(&Licensee::parse("0BSD").unwrap().into_req()),
            ".txt extension ignored"
        );
        assert!(
            LICENSE_DATA.classify_file_name(Path::new("LICENSE-0BSD.jpg")).is_none(),
            "Random extension preserved"
        );
        assert_eq!(
            LICENSE_DATA.classify_file_name(Path::new("LICENSES/MIT")),
            Some(&Licensee::parse("MIT").unwrap().into_req()),
            "Subdirectory"
        );
        assert_eq!(
            LICENSE_DATA.classify_file_name(Path::new("LICENSES/Apache-2.0")),
            Some(&Licensee::parse("Apache-2.0").unwrap().into_req()),
            "Subdirectory"
        );
    }
}

#[cfg(test)]
mod content_classification {
    use std::collections::BTreeSet;

    use spdx::Licensee;

    use crate::LICENSE_DATA;

    #[test]
    fn basic() {
        assert!(LICENSE_DATA.classify_file_contents("foo").is_empty());
        assert_eq!(
            LICENSE_DATA
                .classify_file_contents(include_str!("testdata/LICENSE-MIT-aarch64-paging.txt")),
            BTreeSet::from([Licensee::parse("MIT").unwrap().into_req()])
        );
    }

    #[test]
    fn fuzzy() {
        assert!(LICENSE_DATA
            .classify_file_contents(include_str!("testdata/BSD-3-Clause-bindgen.txt"))
            .is_empty());
        assert_eq!(
            LICENSE_DATA
                .classify_file_contents_fuzzy(include_str!("testdata/BSD-3-Clause-bindgen.txt")),
            Some(Licensee::parse("BSD-3-Clause").unwrap().into_req())
        );
    }

    #[test]
    fn concatenated_licenses() {
        assert_eq!(
            LICENSE_DATA.classify_file_contents(
                format!(
                    "{}\n\n{}",
                    include_str!("licenses/Apache-2.0.txt"),
                    include_str!("licenses/MIT.txt")
                )
                .as_str()
            ),
            BTreeSet::from([
                Licensee::parse("MIT").unwrap().into_req(),
                Licensee::parse("Apache-2.0").unwrap().into_req()
            ])
        );
    }
}
