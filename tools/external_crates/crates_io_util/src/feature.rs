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

use std::{borrow::Borrow, collections::BTreeSet};

use crates_index::{Dependency, Version};

// A reference to feature. Either an explicit feature or an optional dependency.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum FeatureRef<'a> {
    Feature(&'a str),
    OptionalDep(&'a Dependency),
}

impl<'a> FeatureRef<'a> {
    pub fn name(&self) -> &str {
        match self {
            FeatureRef::Feature(name) => name,
            FeatureRef::OptionalDep(dep) => dep.name(),
        }
    }
}

// Traits that let us use FeatureRef as an element of a set.
impl<'a> PartialOrd for FeatureRef<'a> {
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        Some(self.cmp(other))
    }
}
impl<'a> Ord for FeatureRef<'a> {
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        self.name().cmp(other.name())
    }
}

// Lets us retrieve a set element by name.
impl<'a> Borrow<str> for FeatureRef<'a> {
    fn borrow(&self) -> &str {
        self.name()
    }
}

pub type TypedFeatures<'a> = BTreeSet<FeatureRef<'a>>;

pub trait FeaturesAndOptionalDeps {
    fn features_and_optional_deps(&self) -> TypedFeatures;
}

impl FeaturesAndOptionalDeps for Version {
    fn features_and_optional_deps(&self) -> TypedFeatures {
        let explicit_deps = self
            .features()
            .values()
            .flat_map(|dep| dep.iter().filter_map(|d| d.strip_prefix("dep:")))
            .collect::<BTreeSet<_>>();
        self.features()
            .keys()
            .map(|f| FeatureRef::Feature(f.as_str()))
            .chain(self.dependencies().iter().filter_map(|d| {
                if d.is_optional() && !explicit_deps.contains(d.name()) {
                    Some(FeatureRef::OptionalDep(d))
                } else {
                    None
                }
            }))
            .collect::<TypedFeatures>()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    use itertools::assert_equal;

    #[test]
    fn test_features() {
        let hashbrown_0_12_3: Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.12.3"))
                .expect("Failed to parse JSON testdata");
        let features = hashbrown_0_12_3.features_and_optional_deps();
        assert_equal(
            features.iter().map(|f| f.name()),
            [
                "ahash",
                "ahash-compile-time-rng",
                "alloc",
                "bumpalo",
                "compiler_builtins",
                "core",
                "default",
                "inline-more",
                "nightly",
                "raw",
                "rayon",
                "rustc-dep-of-std",
                "rustc-internal-api",
                "serde",
            ],
        );
        assert_eq!(
            features.get("ahash-compile-time-rng"),
            Some(&FeatureRef::Feature("ahash-compile-time-rng"))
        );
        assert_eq!(
            features.get("ahash"),
            Some(&FeatureRef::OptionalDep(&hashbrown_0_12_3.dependencies()[0]))
        );
        assert_eq!(
            features.get("alloc"),
            Some(&FeatureRef::OptionalDep(&hashbrown_0_12_3.dependencies()[1]))
        );
        assert_eq!(
            features.get("bumpalo"),
            Some(&FeatureRef::OptionalDep(&hashbrown_0_12_3.dependencies()[2]))
        );

        let hashbrown_0_14_5: Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.14.5"))
                .expect("Failed to parse JSON testdata");
        let features = hashbrown_0_14_5.features_and_optional_deps();
        assert_equal(
            features.iter().map(|f| f.name()),
            [
                "ahash",
                "alloc",
                "allocator-api2",
                "compiler_builtins",
                "core",
                "default",
                "equivalent",
                "inline-more",
                "nightly",
                "raw",
                "rayon",
                "rkyv",
                "rustc-dep-of-std",
                "rustc-internal-api",
                "serde",
            ],
        );

        let winnow_0_5_37: Version = serde_json::from_str(include_str!("testdata/winnow-0.5.37"))
            .expect("Failed to parse JSON testdata");
        let features = winnow_0_5_37.features_and_optional_deps();
        assert_equal(
            features.iter().map(|f| f.name()),
            ["alloc", "debug", "default", "simd", "std", "unstable-doc", "unstable-recover"],
        );

        let cfg_if_1_0_0: Version = serde_json::from_str(include_str!("testdata/cfg-if-1.0.0"))
            .expect("Failed to parse JSON testdata");
        let features = cfg_if_1_0_0.features_and_optional_deps();
        assert_equal(
            features.iter().map(|f| f.name()),
            ["compiler_builtins", "core", "rustc-dep-of-std"],
        );
    }
}
