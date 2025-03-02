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

use itertools::Itertools;
use spdx::{LicenseReq, Licensee};
use std::sync::LazyLock;
use textdistance::str::ratcliff_obershelp;

fn strip_punctuation(text: &str) -> String {
    let lowercase = text.to_lowercase();
    let mut processed = String::with_capacity(lowercase.len());
    for c in lowercase.chars() {
        if c.is_alphanumeric() || c == '.' {
            processed.push(c)
        } else if !processed.ends_with(' ') {
            processed.push(' ')
        }
    }
    processed.trim().to_string()
}

pub(super) fn classify_license_file_contents(contents: &str) -> Vec<LicenseReq> {
    let contents = strip_punctuation(contents);

    // Exact match
    let mut matches = Vec::new();
    for (req, required_text) in LICENSE_CONTENT_CLASSIFICATION.iter() {
        if contents.contains(required_text) {
            matches.push(req.clone());
        }
    }
    matches
}

pub(super) fn classify_license_file_contents_fuzzy(contents: &str) -> Option<LicenseReq> {
    let contents = strip_punctuation(contents);

    // Fuzzy match. This is expensive, so start with licenses that are closest in length to the file,
    // and only return a single match at most.
    for (req, required_text) in LICENSE_CONTENT_CLASSIFICATION.iter().sorted_by(|a, b| {
        let mut ra = a.1.len() as f32 / contents.len() as f32;
        let mut rb = b.1.len() as f32 / contents.len() as f32;
        if ra > 1.0 {
            ra = 1.0 / ra;
        }
        if rb > 1.0 {
            rb = 1.0 / rb;
        }
        rb.partial_cmp(&ra).unwrap()
    }) {
        let similarity = ratcliff_obershelp(contents.as_str(), required_text);
        if similarity > 0.95 {
            return Some(req.clone());
        }
    }

    None
}

static LICENSE_CONTENT_CLASSIFICATION: LazyLock<Vec<(LicenseReq, String)>> = LazyLock::new(|| {
    vec![
        ("MIT", include_str!("licenses/MIT.txt")),
        ("Apache-2.0", include_str!("licenses/Apache-2.0.txt")),
        ("ISC", include_str!("licenses/ISC.txt")),
        ("MPL-2.0", include_str!("licenses/MPL-2.0.txt")),
        ("BSD-2-Clause", include_str!("licenses/BSD-2-Clause.txt")),
        ("BSD-3-Clause", include_str!("licenses/BSD-3-Clause.txt")),
        ("Unicode-3.0", include_str!("licenses/Unicode-3.0.txt")),
        ("Unicode-DFS-2016", include_str!("licenses/Unicode-DFS-2016.txt")),
        ("Unlicense", include_str!("licenses/Unlicense.txt")),
        ("Zlib", include_str!("licenses/Zlib.txt")),
        ("OpenSSL", include_str!("licenses/OpenSSL.txt")),
        ("NCSA", include_str!("licenses/NCSA.txt")),
    ]
    .into_iter()
    .map(|(req, tokens)| {
        let tokens = strip_punctuation(tokens);
        assert!(!tokens.is_empty());
        (Licensee::parse(req).unwrap().into_req(), tokens)
    })
    .collect()
});

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_strip_punctuation() {
        assert_eq!(strip_punctuation("FOO BAR"), "foo bar", "Converted to lowercase");
        assert_eq!(strip_punctuation("foo, bar"), "foo bar", "Punctuation removed");
        assert_eq!(strip_punctuation("foo. bar"), "foo. bar", "Periods preserved");
        assert_eq!(
            strip_punctuation(" foo bar "),
            "foo bar",
            "Leading and trailing whitespace stripped"
        );
        assert_eq!(
            strip_punctuation(" foo\n\n\n\nbar "),
            "foo bar",
            "Multiple whitespace replaced with single space"
        );
    }

    #[test]
    fn test_classify() {
        assert!(classify_license_file_contents("foo").is_empty());
        assert_eq!(
            classify_license_file_contents(include_str!("testdata/LICENSE-MIT-aarch64-paging.txt")),
            vec![Licensee::parse("MIT").unwrap().into_req()]
        );
    }

    #[test]
    fn test_classify_fuzzy() {
        assert!(classify_license_file_contents(include_str!("testdata/BSD-3-Clause-bindgen.txt"))
            .is_empty());
        assert_eq!(
            classify_license_file_contents_fuzzy(include_str!("testdata/BSD-3-Clause-bindgen.txt")),
            Some(Licensee::parse("BSD-3-Clause").unwrap().into_req())
        );
    }

    #[test]
    fn concatenated_licenses() {
        assert_eq!(
            classify_license_file_contents(
                format!(
                    "{}\n\n{}",
                    include_str!("licenses/Apache-2.0.txt"),
                    include_str!("licenses/MIT.txt")
                )
                .as_str()
            ),
            vec![
                Licensee::parse("MIT").unwrap().into_req(),
                Licensee::parse("Apache-2.0").unwrap().into_req()
            ]
        );
    }
}
