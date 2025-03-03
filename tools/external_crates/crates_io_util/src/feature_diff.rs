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

use crates_index::Version;

use crate::feature::{FeaturesAndOptionalDeps, TypedFeatures};

// Diff features between two versions of a crate.
pub struct FeatureDiffer<'a> {
    base_features: TypedFeatures<'a>,
}

#[derive(Debug)]
pub struct FeatureDiff<'base, 'other> {
    pub added: TypedFeatures<'other>,
    pub deleted: TypedFeatures<'base>,
}

impl<'a> FeatureDiffer<'a> {
    pub fn new(base: &'a Version) -> FeatureDiffer<'a> {
        let base_features = base.features_and_optional_deps();
        FeatureDiffer { base_features }
    }
    pub fn diff<'other>(&'a self, other: &'other Version) -> FeatureDiff<'a, 'other> {
        let other_features = other.features_and_optional_deps();
        let deleted = self
            .base_features
            .iter()
            .filter(|f| !other_features.contains(*f))
            .cloned()
            .collect::<_>();
        let added =
            other_features.into_iter().filter(|f| !self.base_features.contains(f)).collect::<_>();
        FeatureDiff { added, deleted }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    use itertools::assert_equal;

    #[test]
    fn test_diff() {
        let hashbrown_0_12_3: Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.12.3"))
                .expect("Failed to parse JSON testdata");
        let hashbrown_0_14_5: Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.14.5"))
                .expect("Failed to parse JSON testdata");
        let differ = FeatureDiffer::new(&hashbrown_0_12_3);
        let diff = differ.diff(&hashbrown_0_14_5);
        assert_equal(diff.added.iter().map(|f| f.name()), ["allocator-api2", "equivalent", "rkyv"]);
        assert_equal(diff.deleted.iter().map(|f| f.name()), ["ahash-compile-time-rng", "bumpalo"]);

        let diff = differ.diff(&hashbrown_0_12_3);
        assert!(diff.added.is_empty());
        assert!(diff.deleted.is_empty());
    }
}
