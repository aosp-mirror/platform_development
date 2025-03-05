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

mod content_classifier;
mod inexact_name_classifier;
mod name_classifier;

use content_classifier::classify_license_file_contents;
use inexact_name_classifier::{classify_inexact_license_file_name, InexactLicenseType};
use name_classifier::classify_license_file_name;
use spdx::LicenseReq;
use std::{
    collections::BTreeSet,
    ffi::{OsStr, OsString},
    fs::read_to_string,
    path::{Path, PathBuf},
    sync::OnceLock,
};

use crate::Error;

fn normalize_filename(file: impl AsRef<Path>) -> OsString {
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

#[derive(Debug)]
pub(crate) struct Classifier {
    file_path: PathBuf,
    contents: String,
    by_name: Option<LicenseReq>,
    #[allow(dead_code)]
    by_inexact_name: Option<InexactLicenseType>,
    by_content: OnceLock<Vec<LicenseReq>>,
    by_content_fuzzy: OnceLock<Option<LicenseReq>>,
}

impl Classifier {
    pub fn new<FP: Into<PathBuf>>(file_path: FP, contents: String) -> Classifier {
        let file_path = file_path.into();
        let by_name = classify_license_file_name(&file_path);
        let by_inexact_name = classify_inexact_license_file_name(&file_path);
        Classifier {
            file_path,
            contents,
            by_name,
            by_inexact_name,
            by_content: OnceLock::new(),
            by_content_fuzzy: OnceLock::new(),
        }
    }
    pub fn new_vec<CP: Into<PathBuf>>(
        crate_path: CP,
        possible_license_files: BTreeSet<PathBuf>,
    ) -> Result<Vec<Classifier>, Error> {
        let crate_path = crate_path.into();
        let mut classifiers = Vec::new();
        for file_path in possible_license_files {
            let full_path = crate_path.join(&file_path);
            let contents =
                read_to_string(&full_path).map_err(|e| Error::FileReadError(full_path, e))?;
            classifiers.push(Classifier::new(file_path, contents));
        }
        Ok(classifiers)
    }
    pub fn file_path(&self) -> &Path {
        self.file_path.as_path()
    }
    pub fn by_name(&self) -> Option<&LicenseReq> {
        self.by_name.as_ref()
    }
    #[allow(dead_code)]
    pub fn by_inexact_name(&self) -> Option<InexactLicenseType> {
        self.by_inexact_name.clone()
    }
    pub fn by_content(&self) -> &Vec<LicenseReq> {
        self.by_content.get_or_init(|| classify_license_file_contents(&self.contents))
    }
    pub fn by_content_fuzzy(&self) -> Option<&LicenseReq> {
        self.by_content_fuzzy
            .get_or_init(|| {
                content_classifier::classify_license_file_contents_fuzzy(&self.contents)
            })
            .as_ref()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

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
