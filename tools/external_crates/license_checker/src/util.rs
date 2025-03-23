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
    ffi::{OsStr, OsString},
    path::Path,
};

pub(crate) fn strip_punctuation(text: &str) -> String {
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

pub(crate) fn normalize_filename(file: impl AsRef<Path>) -> OsString {
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
    uppercase_name.to_owned()
}

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
    fn test_normalize_filename() {
        assert_eq!(normalize_filename(Path::new("LICENSE")), OsString::from("LICENSE"));
        assert_eq!(
            normalize_filename(Path::new("LICENSE.txt")),
            OsString::from("LICENSE"),
            ".txt extension removed"
        );
        assert_eq!(
            normalize_filename(Path::new("LICENSE.md")),
            OsString::from("LICENSE"),
            ".md extension removed"
        );
        assert_eq!(
            normalize_filename(Path::new("LICENSE.jpg")),
            OsString::from("LICENSE.JPG"),
            "Other extensions preserved"
        );
        assert_eq!(
            normalize_filename(Path::new("license")),
            OsString::from("LICENSE"),
            "Converted to uppercase"
        );
    }
}
