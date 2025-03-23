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

use spdx::Licensee;
use std::{
    collections::BTreeSet,
    fs::read_to_string,
    path::{Path, PathBuf},
    sync::OnceLock,
};

use crate::{Error, LICENSE_DATA};

#[derive(Debug)]
pub(crate) struct Classifier {
    file_path: PathBuf,
    contents: String,
    by_content: OnceLock<BTreeSet<Licensee>>,
    by_content_fuzzy: OnceLock<Option<Licensee>>,
}

impl Classifier {
    pub fn new<FP: Into<PathBuf>>(file_path: FP, contents: String) -> Classifier {
        Classifier {
            file_path: file_path.into(),
            contents,
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
    pub fn by_name(&self) -> Option<&Licensee> {
        LICENSE_DATA.classify_file_name(self.file_path())
    }
    pub fn by_content(&self) -> &BTreeSet<Licensee> {
        self.by_content.get_or_init(|| LICENSE_DATA.classify_file_contents(&self.contents))
    }
    pub fn by_content_fuzzy(&self) -> Option<&Licensee> {
        self.by_content_fuzzy
            .get_or_init(|| LICENSE_DATA.classify_file_contents_fuzzy(&self.contents))
            .as_ref()
    }
}
