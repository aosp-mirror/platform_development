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

use crates_index::Dependency;
use semver::VersionReq;

/// Trait for parsing version requirement strings of dependencies.
pub trait ParsedVersionReq {
    /// Parses a version requirement, returning an error if unsuccessful.
    fn parsed_version_req(&self) -> Result<VersionReq, semver::Error>;
}

impl ParsedVersionReq for Dependency {
    fn parsed_version_req(&self) -> Result<VersionReq, semver::Error> {
        VersionReq::parse(self.requirement())
    }
}

/// Trait for determining if dependencies in different versions of
/// a crate are the same.
pub trait SameDep {
    /// Returns true if the two dependencies from different versions are
    /// (probably) the same. That is, they have the same name and kind.
    fn same_name_and_kind(&self, other: &Dependency) -> bool;
}

impl SameDep for Dependency {
    fn same_name_and_kind(&self, other: &Dependency) -> bool {
        self.name() == other.name() && self.kind() == other.kind()
    }
}
