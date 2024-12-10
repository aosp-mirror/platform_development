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

use std::collections::{BTreeMap, BTreeSet};

use crates_index::Version;

use crate::{
    feature::{FeaturesAndOptionalDeps, TypedFeatures},
    DepSet, Error,
};

// Resolve a list of enabled features to a set of optional dependencies that these features switch on.
pub struct FeatureResolver<'a> {
    version: &'a Version,
    features: TypedFeatures<'a>,
    deps: DepSet<'a>,
}

impl<'a> FeatureResolver<'a> {
    pub fn new(version: &'a Version) -> FeatureResolver<'a> {
        let features = version.features_and_optional_deps();
        let deps = BTreeMap::from_iter(version.dependencies().iter().map(|d| (d.name(), d)));
        FeatureResolver { version, features, deps }
    }
    pub fn resolve(
        &self,
        features: Option<impl Iterator<Item = impl AsRef<str>>>,
    ) -> Result<DepSet<'a>, Error> {
        let mut frontier = BTreeSet::new();
        if let Some(features) = features {
            for feature in features {
                let feature = feature.as_ref().to_string();
                if !self.features.contains(feature.as_str()) {
                    return Err(Error::FeatureNotFound(feature, self.version.name().to_string()));
                }
                frontier.insert(feature);
            }
        } else if self.features.contains("default") {
            frontier.insert("default".to_string());
        }
        let mut resolved = DepSet::new();
        while !frontier.is_empty() {
            let f = frontier.pop_first().unwrap();
            let f = f.as_str();
            if let Some(dep) = self.features.get(f) {
                match dep {
                    crate::FeatureRef::Feature(_) => {
                        for child in self.version.features().get(f).unwrap() {
                            let child = child.split_once('/').unwrap_or((child, "")).0;
                            if child.contains('?') {
                                continue;
                            }
                            if let Some(c) = child.strip_prefix("dep:") {
                                let dep = self.deps.get(c).ok_or(Error::DepNotFound(
                                    c.to_string(),
                                    self.version.name().to_string(),
                                ))?;
                                resolved.insert(dep.name(), dep);
                            } else {
                                frontier.insert(child.to_string());
                            }
                        }
                    }
                    crate::FeatureRef::OptionalDep(d) => {
                        resolved.insert(d.name(), d);
                    }
                }
            } else {
                let dep = self
                    .deps
                    .get(f)
                    .ok_or(Error::DepNotFound(f.to_string(), self.version.name().to_string()))?;
                resolved.insert(dep.name(), dep);
            }
        }
        Ok(resolved)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    use itertools::assert_equal;

    #[test]
    fn resolve_one() -> Result<(), Error> {
        let hashbrown_0_12_3: Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.12.3"))
                .expect("Failed to parse JSON testdata");
        let resolver = FeatureResolver::new(&hashbrown_0_12_3);
        assert_equal(resolver.resolve(Some(["bumpalo"].iter()))?.keys(), ["bumpalo"].iter());
        assert_equal(resolver.resolve(Some(["default"].iter()))?.keys(), ["ahash"].iter());
        // ahash-compile-time-rng depends on ahash/compile-time-rng
        assert_equal(
            resolver.resolve(Some(["ahash-compile-time-rng"].iter()))?.keys(),
            ["ahash"].iter(),
        );
        assert!(
            resolver.resolve(Some(["inline-more"].iter()))?.is_empty(),
            "inline-more has no deps associated with it"
        );

        let hashbrown_0_14_5: Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.14.5"))
                .expect("Failed to parse JSON testdata");
        let resolver = FeatureResolver::new(&hashbrown_0_14_5);
        assert!(
            resolver.resolve(Some(["bumpalo"].iter())).is_err(),
            "bumpalo is no longer an optional dep in hashbrown-0.14.5"
        );
        assert_equal(
            resolver.resolve(Some(["default"].iter()))?.keys(),
            ["ahash", "allocator-api2"].iter(),
        );
        Ok(())
    }

    #[test]
    fn resolve_multiple() -> Result<(), Error> {
        let hashbrown_0_12_3: Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.12.3"))
                .expect("Failed to parse JSON testdata");
        let resolver = FeatureResolver::new(&hashbrown_0_12_3);
        assert_equal(
            resolver.resolve(Some(["ahash", "default", "inline-more", "raw"].iter()))?.keys(),
            ["ahash"].iter(),
        );
        Ok(())
    }

    #[test]
    fn default() -> Result<(), Error> {
        let hashbrown_0_12_3: Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.12.3"))
                .expect("Failed to parse JSON testdata");
        let resolver = FeatureResolver::new(&hashbrown_0_12_3);
        let empty: Option<Box<dyn Iterator<Item = &str>>> = None;
        assert_equal(resolver.resolve(empty), resolver.resolve(Some(["default"].iter())));

        let hashbrown_0_14_5: Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.14.5"))
                .expect("Failed to parse JSON testdata");
        let resolver = FeatureResolver::new(&hashbrown_0_14_5);
        let empty: Option<Box<dyn Iterator<Item = &str>>> = None;
        assert_equal(resolver.resolve(empty), resolver.resolve(Some(["default"].iter())));

        let winnow_0_5_37: Version = serde_json::from_str(include_str!("testdata/winnow-0.5.37"))
            .expect("Failed to parse JSON testdata");
        let resolver = FeatureResolver::new(&winnow_0_5_37);
        let empty: Option<Box<dyn Iterator<Item = &str>>> = None;
        assert_equal(resolver.resolve(empty), resolver.resolve(Some(["default"].iter())));

        let cfg_if_1_0_0: Version = serde_json::from_str(include_str!("testdata/cfg-if-1.0.0"))
            .expect("Failed to parse JSON testdata");
        let resolver = FeatureResolver::new(&cfg_if_1_0_0);
        let empty: Option<Box<dyn Iterator<Item = &str>>> = None;
        assert!(resolver.resolve(empty)?.is_empty(), "cfg-if has no explicit 'default' feature");
        assert!(
            resolver.resolve(Some(["default"].iter())).is_err(),
            "cfg-if has no explicit 'default' feature"
        );

        Ok(())
    }

    #[test]
    fn advanced_syntax() -> Result<(), Error> {
        let winnow_0_5_37: Version = serde_json::from_str(include_str!("testdata/winnow-0.5.37"))
            .expect("Failed to parse JSON testdata");
        let resolver = FeatureResolver::new(&winnow_0_5_37);
        // "simd" depends on "dep:memchr"
        assert_equal(resolver.resolve(Some(["simd"].iter()))?.keys(), ["memchr"].iter());
        assert!(
            resolver.resolve(Some(["std"].iter()))?.is_empty(),
            "'std' depends on 'memchr?/std', which should be omitted since it's optional."
        );

        Ok(())
    }

    #[test]
    fn recursion_bug() -> Result<(), Error> {
        let aarch64_paging_0_7_1: Version =
            serde_json::from_str(include_str!("testdata/aarch64-paging-0.7.1"))
                .expect("Failed to parse JSON testdata");
        let resolver = FeatureResolver::new(&aarch64_paging_0_7_1);
        let empty: Option<Box<dyn Iterator<Item = &str>>> = None;
        assert_equal(resolver.resolve(empty)?.keys(), ["zerocopy"].iter());

        Ok(())
    }
}
