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

//! A name and version data structure that can be used as map key.

use semver::Version;

pub use self::name_and_version::{NameAndVersion, NameAndVersionRef, NamedAndVersioned};
mod name_and_version;

pub use self::name_and_version_map::{
    crates_with_multiple_versions, crates_with_single_version, most_recent_version,
    NameAndVersionMap,
};
mod name_and_version_map;

/// Error types for the 'name_and_version' crate.
#[derive(thiserror::Error, Debug)]
pub enum Error {
    /// Duplicate crate version
    #[error("Duplicate crate version: {0} {1}")]
    DuplicateVersion(String, Version),
    /// Version parse error
    #[error(transparent)]
    VersionParseError(#[from] semver::Error),
}
