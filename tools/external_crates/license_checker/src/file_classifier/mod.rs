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
mod name_classifier;

use content_classifier::classify_license_file_contents;
use name_classifier::classify_license_file_name;
use spdx::LicenseReq;
use std::{cell::OnceCell, fs::read_to_string, path::PathBuf};

#[derive(Debug)]
pub(crate) struct Classifier {
    crate_path: PathBuf,
    file_path: PathBuf,
    by_name: Option<LicenseReq>,
    by_content: OnceCell<Vec<LicenseReq>>,
}

impl Classifier {
    pub fn new<CP: Into<PathBuf>, FP: Into<PathBuf>>(crate_path: CP, file_path: FP) -> Classifier {
        let file_path = file_path.into();
        let by_name = classify_license_file_name(&file_path);
        Classifier {
            crate_path: crate_path.into(),
            file_path,
            by_name,
            by_content: OnceCell::new(),
        }
    }
    pub fn by_name(&self) -> Option<&LicenseReq> {
        self.by_name.as_ref()
    }
    pub fn by_content(&self) -> &Vec<LicenseReq> {
        self.by_content.get_or_init(|| {
            let contents = read_to_string(self.crate_path.join(&self.file_path)).unwrap();
            classify_license_file_contents(&contents)
        })
    }
}
