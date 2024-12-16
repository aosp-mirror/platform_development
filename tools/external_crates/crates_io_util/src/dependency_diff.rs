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

use std::collections::BTreeMap;

use crates_index::{Dependency, Version};

use crate::DepSet;

// Diff dependencies between two versions of a crate.
pub struct DependencyDiffer<'a> {
    base: &'a Version,
    base_deps: DepSet<'a>,
}

#[derive(Debug)]
pub struct ChangedDep<'base, 'other> {
    pub base: &'base Dependency,
    pub other: &'other Dependency,
}

type ChangedDeps<'base, 'other> = BTreeMap<&'base str, ChangedDep<'base, 'other>>;

#[derive(Debug)]
pub struct DependencyDiff<'base, 'other> {
    pub added: DepSet<'other>,
    pub deleted: DepSet<'base>,
    pub changed: ChangedDeps<'base, 'other>,
}

impl<'a> DependencyDiffer<'a> {
    pub fn new(base: &'a Version) -> DependencyDiffer<'a> {
        let base_deps = BTreeMap::from_iter(base.dependencies().iter().map(|d| (d.name(), d)));
        DependencyDiffer { base, base_deps }
    }
    pub fn diff<'other>(&'a self, other: &'other Version) -> DependencyDiff<'a, 'other> {
        let other_deps = BTreeMap::from_iter(other.dependencies().iter().map(|d| (d.name(), d)));

        let added = other_deps
            .iter()
            .filter(|(name, _)| !self.base_deps.contains_key(**name))
            .map(|(name, dep)| (*name, *dep))
            .collect();
        let deleted = self
            .base_deps
            .iter()
            .filter(|(name, _)| !other_deps.contains_key(**name))
            .map(|(name, dep)| (*name, *dep))
            .collect();
        let mut changed = ChangedDeps::new();
        for (name, base_dep) in &self.base_deps {
            if let Some(other_dep) = other_deps.get(name) {
                if base_dep != other_dep {
                    changed.insert(*name, ChangedDep { base: base_dep, other: other_dep });
                }
            }
        }

        DependencyDiff { added, deleted, changed }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    use itertools::assert_equal;

    #[test]
    fn trivial() {
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
        let hashbrown_0_12_3: Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.12.3"))
                .expect("Failed to parse JSON testdata");
        let hashbrown_0_14_5: Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.14.5"))
                .expect("Failed to parse JSON testdata");
        let differ = DependencyDiffer::new(&hashbrown_0_12_3);
        let diff = differ.diff(&hashbrown_0_14_5);
        assert_equal(diff.added.keys(), ["allocator-api2", "equivalent", "rkyv"].iter());
        assert!(diff.deleted.is_empty());
        assert_equal(diff.changed.keys(), ["ahash", "bumpalo"].iter());
    }

    #[test]
    fn winnow() {
        let winnow_0_5_37: Version = serde_json::from_str(include_str!("testdata/winnow-0.5.37"))
            .expect("Failed to parse JSON testdata");
        let winnow_0_6_20: Version = serde_json::from_str(include_str!("testdata/winnow-0.6.20"))
            .expect("Failed to parse JSON testdata");
        let differ = DependencyDiffer::new(&winnow_0_5_37);
        let diff = differ.diff(&winnow_0_6_20);
        assert_equal(diff.added.keys(), ["annotate-snippets", "anyhow", "automod"].iter());
        assert_equal(diff.deleted.keys(), ["escargot"].iter());
        assert_equal(diff.changed.keys(), ["snapbox", "terminal_size"].iter());
    }
}
