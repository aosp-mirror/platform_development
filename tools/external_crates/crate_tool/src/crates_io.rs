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

use crates_index::{Crate, Dependency, DependencyKind, Version};
use crates_io_util::AndroidTarget;
use semver::VersionReq;
use std::collections::HashMap;

/// Filter versions by those that are "safe", meaning not yanked or pre-release.
pub trait SafeVersions {
    // Versions of the crate that aren't yanked or pre-release.
    fn safe_versions(&self) -> impl DoubleEndedIterator<Item = &Version>;
    // Versions of the crate greater than 'version'.
    fn versions_gt(&self, version: &semver::Version) -> impl DoubleEndedIterator<Item = &Version> {
        self.safe_versions()
            .filter(|v| semver::Version::parse(v.version()).is_ok_and(|parsed| parsed.gt(version)))
    }
    // Get a specific version of a crate.
    fn get_version(&self, version: &semver::Version) -> Option<&Version> {
        self.safe_versions()
            .find(|v| semver::Version::parse(v.version()).is_ok_and(|parsed| parsed.eq(version)))
    }
}
impl SafeVersions for Crate {
    fn safe_versions(&self) -> impl DoubleEndedIterator<Item = &Version> {
        self.versions().iter().filter(|v| {
            !v.is_yanked()
                && semver::Version::parse(v.version()).is_ok_and(|parsed| parsed.pre.is_empty())
        })
    }
}

/// Filter dependencies for those likely to be relevant to Android.
pub trait AndroidDependencies {
    fn android_deps(&self) -> impl DoubleEndedIterator<Item = &Dependency>;
    fn android_version_reqs_by_name(&self) -> HashMap<&str, &str> {
        self.android_deps().map(|dep| (dep.crate_name(), dep.requirement())).collect()
    }
    fn android_deps_with_version_reqs(
        &self,
    ) -> impl DoubleEndedIterator<Item = (&Dependency, VersionReq)> {
        self.android_deps().filter_map(|dep| {
            VersionReq::parse(dep.requirement()).map_or(None, |req| Some((dep, req)))
        })
    }
}
impl AndroidDependencies for Version {
    fn android_deps(&self) -> impl DoubleEndedIterator<Item = &Dependency> {
        self.dependencies().iter().filter(|dep| {
            dep.kind() == DependencyKind::Normal && !dep.is_optional() && dep.is_android_target()
        })
    }
}

pub trait DependencyChanges {
    fn is_new_dep(&self, base_deps: &HashMap<&str, &str>) -> bool;
    fn is_changed_dep(&self, base_deps: &HashMap<&str, &str>) -> bool;
}

impl DependencyChanges for Dependency {
    fn is_new_dep(&self, base_deps: &HashMap<&str, &str>) -> bool {
        !base_deps.contains_key(self.crate_name())
    }

    fn is_changed_dep(&self, base_deps: &HashMap<&str, &str>) -> bool {
        let base_dep = base_deps.get(self.crate_name());
        base_dep.is_none() || base_dep.is_some_and(|base_req| *base_req != self.requirement())
    }
}
