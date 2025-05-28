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

use std::collections::{HashMap, HashSet};

use crates_index::{Dependency, Version};
use itertools::Itertools;
use log::debug;

use crate::dependency::SameDep;

/// Diff dependencies between two versions of a crate.
/// Holds a reference to the current (base) dependencies of a crate,
/// against which multiple newer versions can be compared.
#[derive(Debug)]
pub struct DependencyDiffer<'a> {
    base_version: &'a Version,
}

impl<'a> DependencyDiffer<'a> {
    /// Constructs a DependencyDiffer with the specified base version.
    pub fn new(base_version: &'a Version) -> DependencyDiffer<'a> {
        DependencyDiffer { base_version }
    }
    /// Compares the base version with `other` and returns the differences.
    pub fn diff<'other>(&'a self, other: &'other Version) -> DependencyDiff<'a, 'other> {
        debug!(
            "diff base:\n{}",
            self.base_version
                .dependencies()
                .iter()
                .map(|d| format!("{} {:?}", d.name(), d.kind()))
                .join("\n")
        );
        debug!(
            "diff other:\n{}",
            other
                .dependencies()
                .iter()
                .map(|d| format!("{} {:?}", d.name(), d.kind()))
                .sorted()
                .join("\n")
        );
        let added: HashSet<&Dependency> = other
            .dependencies()
            .iter()
            .filter(|other_dep| {
                !self
                    .base_version
                    .dependencies()
                    .iter()
                    .any(|base_dep| base_dep.same_name_and_kind(other_dep))
            })
            .collect();
        debug!(
            "diff added:\n{}",
            added.iter().map(|d| format!("{} {:?}", d.name(), d.kind())).sorted().join("\n")
        );
        let deleted: HashSet<&Dependency> = self
            .base_version
            .dependencies()
            .iter()
            .filter(|base_dep| {
                !other.dependencies().iter().any(|other_dep| base_dep.same_name_and_kind(other_dep))
            })
            .collect();
        debug!(
            "diff deleted:\n{}",
            deleted.iter().map(|d| format!("{} {:?}", d.name(), d.kind())).join("\n")
        );
        let mut changed = HashMap::new();
        for base_dep in self.base_version.dependencies() {
            if let Some(other_dep) =
                other.dependencies().iter().find(|other_dep| base_dep.same_name_and_kind(other_dep))
            {
                if base_dep != other_dep {
                    changed.insert(other_dep, base_dep);
                }
            }
        }
        debug!(
            "diff changed:\n{}",
            changed.keys().map(|d| format!("{} {:?}", d.name(), d.kind())).sorted().join("\n")
        );

        DependencyDiff { added, deleted, changed }
    }
}

/// The difference between the dependencies of two versions of a crate.
#[derive(Debug)]
pub struct DependencyDiff<'base, 'other> {
    /// Newly added dependencies.
    added: HashSet<&'other Dependency>,
    /// Dependencies that were deleted.
    deleted: HashSet<&'base Dependency>,
    /// Dependencies that differ between the two versions. For example,
    /// they could have different version requirements.
    changed: HashMap<&'other Dependency, &'base Dependency>,
}

impl<'base, 'other> DependencyDiff<'base, 'other> {
    /// Returns true if the dependency is added in the new version.
    pub fn is_added(&self, other: &'other Dependency) -> bool {
        self.added.contains(other)
    }
    /// Returns true if the dependency is deleted in the new version.
    pub fn is_deleted(&self, base: &'base Dependency) -> bool {
        self.deleted.contains(base)
    }
    /// Returns true if the dependency is changed.
    pub fn is_changed(&self, other: &'other Dependency) -> bool {
        self.changed.contains_key(other)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    use crates_index::DependencyKind;
    use itertools::{assert_equal, Itertools};

    fn init_logger() {
        let _ =
            env_logger::builder().filter_level(log::LevelFilter::max()).is_test(true).try_init();
    }

    #[test]
    fn trivial() {
        init_logger();
        let hashbrown_0_12_3: Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.12.3"))
                .expect("Failed to parse JSON testdata");
        let differ = DependencyDiffer::new(&hashbrown_0_12_3);
        let diff = differ.diff(&hashbrown_0_12_3);
        assert!(diff.added.is_empty());
        assert!(diff.deleted.is_empty());
        assert!(diff.changed.is_empty());
    }

    #[test]
    fn hashbrown() {
        init_logger();
        let hashbrown_0_12_3: Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.12.3"))
                .expect("Failed to parse JSON testdata");
        let hashbrown_0_14_5: Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.14.5"))
                .expect("Failed to parse JSON testdata");
        let differ = DependencyDiffer::new(&hashbrown_0_12_3);
        let diff = differ.diff(&hashbrown_0_14_5);
        assert_equal(
            diff.added
                .iter()
                .filter_map(
                    |d| if d.kind() == DependencyKind::Normal { Some(d.name()) } else { None },
                )
                .sorted(),
            ["allocator-api2", "equivalent", "rkyv"],
        );
        assert_equal(
            diff.deleted
                .iter()
                .filter_map(
                    |d| if d.kind() == DependencyKind::Normal { Some(d.name()) } else { None },
                )
                .sorted(),
            ["bumpalo"],
        );
        assert_equal(
            diff.changed
                .keys()
                .filter_map(
                    |d| if d.kind() == DependencyKind::Normal { Some(d.name()) } else { None },
                )
                .sorted(),
            ["ahash"],
        );
    }

    #[test]
    fn winnow() {
        init_logger();
        let winnow_0_5_37: Version = serde_json::from_str(include_str!("testdata/winnow-0.5.37"))
            .expect("Failed to parse JSON testdata");
        let winnow_0_6_20: Version = serde_json::from_str(include_str!("testdata/winnow-0.6.20"))
            .expect("Failed to parse JSON testdata");
        let differ = DependencyDiffer::new(&winnow_0_5_37);
        let diff = differ.diff(&winnow_0_6_20);
        assert_equal(
            diff.added.iter().map(|d| d.name()).sorted(),
            ["annotate-snippets", "anyhow", "automod"],
        );
        assert_equal(diff.deleted.iter().map(|d| d.name()).sorted(), ["escargot"]);
        assert_equal(diff.changed.keys().map(|d| d.name()).sorted(), ["snapbox", "terminal_size"]);
    }
}
