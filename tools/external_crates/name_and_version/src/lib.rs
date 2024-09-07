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

use semver::Version;
use thiserror::Error;

pub use self::name_and_version::{
    IsUpgradableTo, NameAndVersion, NameAndVersionRef, NamedAndVersioned,
};
mod name_and_version;

pub use self::name_and_version_map::{
    crates_with_multiple_versions, crates_with_single_version, most_recent_version,
    NameAndVersionMap,
};
mod name_and_version_map;

#[derive(Error, Debug)]
pub enum Error {
    #[error("Duplicate crate version: {0} {1}")]
    DuplicateVersion(String, Version),
    #[error("Version parse error")]
    VersionParseError(#[from] semver::Error),
    #[error("Unknown error")]
    Unknown,
}
