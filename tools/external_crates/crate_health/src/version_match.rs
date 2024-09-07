// Copyright (C) 2023 The Android Open Source Project
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

use std::{collections::BTreeMap, path::Path};

use anyhow::{anyhow, Result};
use google_metadata::GoogleMetadata;
use name_and_version::{NameAndVersion, NameAndVersionMap, NamedAndVersioned};

use crate::{generate_android_bps, CrateCollection};

#[derive(Debug)]
pub struct VersionPair<'a, T> {
    pub source: &'a T,
    pub dest: Option<&'a T>,
}

#[derive(Debug)]
pub struct CompatibleVersionPair<'a, T> {
    pub source: &'a T,
    pub dest: &'a T,
}

impl<'a, T> VersionPair<'a, T> {
    pub fn to_compatible(self) -> Option<CompatibleVersionPair<'a, T>> {
        self.dest.map(|dest| CompatibleVersionPair { source: self.source, dest })
    }
}

pub struct VersionMatch<CollectionType: NameAndVersionMap> {
    source: CollectionType,
    dest: CollectionType,
    compatibility: BTreeMap<NameAndVersion, Option<NameAndVersion>>,
}

impl<CollectionType: NameAndVersionMap> VersionMatch<CollectionType> {
    pub fn new(source: CollectionType, dest: CollectionType) -> Result<Self> {
        let mut vm = VersionMatch { source, dest, compatibility: BTreeMap::new() };

        for nv in vm.dest.map_field().keys() {
            vm.compatibility.insert_or_error(nv.to_owned(), None)?;
        }

        for nv in vm.source.map_field().keys() {
            let compatibility = if let Some(dest_nv) = vm.dest.get_version_upgradable_from(nv) {
                vm.compatibility.map_field_mut().remove(dest_nv).ok_or(anyhow!(
                    "Destination crate version {} {} expected but not found",
                    dest_nv.name(),
                    dest_nv.version()
                ))?;
                Some(dest_nv.clone())
            } else {
                None
            };
            vm.compatibility.insert_or_error(nv.to_owned(), compatibility)?;
        }

        Ok(vm)
    }
    pub fn is_superfluous(&self, dest: &dyn NamedAndVersioned) -> bool {
        self.dest.map_field().contains_key(dest)
            && self.compatibility.get(dest).is_some_and(|compatibility| compatibility.is_none())
    }
    pub fn get_compatible_version(
        &self,
        source: &dyn NamedAndVersioned,
    ) -> Option<&NameAndVersion> {
        self.compatibility.get(source).and_then(|compatibility| compatibility.as_ref())
    }
    pub fn get_compatible_item(
        &self,
        source: &dyn NamedAndVersioned,
    ) -> Option<&CollectionType::Value> {
        self.get_compatible_version(source).map(|nv| self.dest.map_field().get(nv).unwrap())
    }
    pub fn get_compatible_item_mut(
        &mut self,
        source: &dyn NamedAndVersioned,
    ) -> Option<&mut CollectionType::Value> {
        let nv = self.get_compatible_version(source)?.clone();
        self.dest.map_field_mut().get_mut(&nv)
    }

    pub fn superfluous(&self) -> impl Iterator<Item = (&NameAndVersion, &CollectionType::Value)> {
        self.dest.map_field().iter().filter(|(nv, _val)| {
            self.compatibility.get(*nv).is_some_and(|compatibility| compatibility.is_none())
        })
    }
    pub fn pairs(&self) -> impl Iterator<Item = VersionPair<CollectionType::Value>> {
        self.source
            .map_field()
            .iter()
            .map(|(nv, source)| VersionPair { source, dest: self.get_compatible_item(nv) })
    }
    pub fn compatible_pairs(
        &self,
    ) -> impl Iterator<Item = CompatibleVersionPair<CollectionType::Value>> {
        self.pairs().filter_map(
            |pair: VersionPair<'_, <CollectionType as NameAndVersionMap>::Value>| {
                pair.to_compatible()
            },
        )
    }
}

impl VersionMatch<CrateCollection> {
    pub fn copy_customizations(&self) -> Result<()> {
        for pair in self.compatible_pairs() {
            pair.copy_customizations()?;
        }
        Ok(())
    }
    pub fn stage_crates(&mut self) -> Result<()> {
        for pair in self.compatible_pairs() {
            pair.dest.stage_crate()?;
        }
        Ok(())
    }
    pub fn apply_patches(&mut self) -> Result<()> {
        let (s, d, c) = (&self.source, &mut self.dest, &self.compatibility);
        for source_key in s.map_field().keys() {
            if let Some(dest_crate) = c.get(source_key).and_then(|compatibility| {
                compatibility.as_ref().and_then(|dest_key| d.map_field_mut().get_mut(dest_key))
            }) {
                dest_crate.apply_patches()?
            }
        }
        Ok(())
    }
    pub fn generate_android_bps(&mut self) -> Result<()> {
        let results = generate_android_bps(self.compatible_pairs().map(|pair| pair.dest))?;
        for (nv, output) in results.into_iter() {
            self.dest
                .map_field_mut()
                .get_mut(&nv)
                .ok_or(anyhow!("Failed to get crate {} {}", nv.name(), nv.version()))?
                .set_generate_android_bp_output(output);
        }
        Ok(())
    }

    pub fn diff_android_bps(&mut self) -> Result<()> {
        let mut results = BTreeMap::new();
        for pair in self.compatible_pairs() {
            results.insert_or_error(NameAndVersion::from(pair.dest), pair.diff_android_bps()?)?;
        }
        for (nv, output) in results.into_iter() {
            self.dest
                .map_field_mut()
                .get_mut(&nv)
                .ok_or(anyhow!("Failed to get crate {} {}", nv.name(), nv.version()))?
                .set_diff_output(output);
        }
        Ok(())
    }

    pub fn update_metadata(&self) -> Result<()> {
        for pair in self.compatible_pairs() {
            let mut metadata =
                GoogleMetadata::try_from(pair.dest.staging_path().join(Path::new("METADATA"))?)?;
            let mut writeback = false;
            writeback |= metadata.migrate_homepage();
            writeback |= metadata.migrate_archive();
            writeback |= metadata.remove_deprecated_url();
            if pair.source.version() != pair.dest.version() {
                metadata.set_date_to_today()?;
                metadata.set_version_and_urls(pair.dest.name(), pair.dest.version().to_string())?;
                writeback |= true;
            }
            if writeback {
                metadata.write()?;
            }
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use anyhow::Result;
    use itertools::assert_equal;
    use std::collections::BTreeMap;

    fn try_name_version_map_from_iter<'a, ValueType>(
        nvs: impl IntoIterator<Item = (&'a str, &'a str, ValueType)>,
    ) -> Result<BTreeMap<NameAndVersion, ValueType>, name_and_version::Error> {
        let mut test_map = BTreeMap::new();
        for (name, version, val) in nvs {
            test_map.insert_or_error(NameAndVersion::try_from_str(name, version)?, val)?;
        }
        Ok(test_map)
    }

    #[test]
    fn test_version_map() -> Result<()> {
        let source = try_name_version_map_from_iter([
            ("equal", "2.3.4", "equal src".to_string()),
            ("compatible", "1.2.3", "compatible src".to_string()),
            ("incompatible", "1.1.1", "incompatible src".to_string()),
            ("downgrade", "2.2.2", "downgrade src".to_string()),
            ("missing", "1.0.0", "missing src".to_string()),
        ])?;
        let dest = try_name_version_map_from_iter([
            ("equal", "2.3.4", "equal dest".to_string()),
            ("compatible", "1.2.4", "compatible dest".to_string()),
            ("incompatible", "2.0.0", "incompatible dest".to_string()),
            ("downgrade", "2.2.1", "downgrade dest".to_string()),
            ("superfluous", "1.0.0", "superfluous dest".to_string()),
        ])?;

        let equal = NameAndVersion::try_from_str("equal", "2.3.4")?;
        let compatible_old = NameAndVersion::try_from_str("compatible", "1.2.3")?;
        let incompatible_old = NameAndVersion::try_from_str("incompatible", "1.1.1")?;
        let downgrade_old = NameAndVersion::try_from_str("downgrade", "2.2.2")?;
        let missing = NameAndVersion::try_from_str("missing", "1.0.0")?;

        let compatible_new = NameAndVersion::try_from_str("compatible", "1.2.4")?;
        let incompatible_new = NameAndVersion::try_from_str("incompatible", "2.0.0")?;
        let downgrade_new = NameAndVersion::try_from_str("downgrade", "2.2.1")?;
        let superfluous = NameAndVersion::try_from_str("superfluous", "1.0.0")?;

        let mut version_match = VersionMatch::new(source, dest)?;
        assert_eq!(
            version_match.compatibility,
            BTreeMap::from([
                (downgrade_new.clone(), None),
                (downgrade_old.clone(), None),
                (equal.clone(), Some(equal.clone())),
                (compatible_old.clone(), Some(compatible_new.clone())),
                (incompatible_old.clone(), None),
                (incompatible_new.clone(), None),
                (missing.clone(), None),
                (superfluous.clone(), None),
            ])
        );

        // assert!(version_match.has_compatible(&equal));
        assert_eq!(version_match.get_compatible_version(&equal), Some(&equal));
        assert_eq!(version_match.get_compatible_item(&equal), Some(&"equal dest".to_string()));
        assert_eq!(
            version_match.get_compatible_item_mut(&equal),
            Some(&mut "equal dest".to_string())
        );
        assert!(!version_match.is_superfluous(&equal));

        // assert!(version_match.has_compatible(&compatible_old));
        assert_eq!(version_match.get_compatible_version(&compatible_old), Some(&compatible_new));
        assert_eq!(
            version_match.get_compatible_item(&compatible_old),
            Some(&"compatible dest".to_string())
        );
        assert_eq!(
            version_match.get_compatible_item_mut(&compatible_old),
            Some(&mut "compatible dest".to_string())
        );
        assert!(!version_match.is_superfluous(&compatible_old));
        assert!(!version_match.is_superfluous(&compatible_new));

        // assert!(!version_match.has_compatible(&incompatible_old));
        assert!(version_match.get_compatible_version(&incompatible_old).is_none());
        assert!(version_match.get_compatible_item(&incompatible_old).is_none());
        assert!(version_match.get_compatible_item_mut(&incompatible_old).is_none());
        assert!(!version_match.is_superfluous(&incompatible_old));
        assert!(version_match.is_superfluous(&incompatible_new));

        // assert!(!version_match.has_compatible(&downgrade_old));
        assert!(version_match.get_compatible_version(&downgrade_old).is_none());
        assert!(version_match.get_compatible_item(&downgrade_old).is_none());
        assert!(version_match.get_compatible_item_mut(&downgrade_old).is_none());
        assert!(!version_match.is_superfluous(&downgrade_old));
        assert!(version_match.is_superfluous(&downgrade_new));

        // assert!(!version_match.has_compatible(&missing));
        assert!(version_match.get_compatible_version(&missing).is_none());
        assert!(version_match.get_compatible_item(&missing).is_none());
        assert!(version_match.get_compatible_item_mut(&missing).is_none());
        assert!(!version_match.is_superfluous(&missing));

        // assert!(!version_match.has_compatible(&superfluous));
        assert!(version_match.get_compatible_version(&superfluous).is_none());
        assert!(version_match.get_compatible_item(&superfluous).is_none());
        assert!(version_match.get_compatible_item_mut(&superfluous).is_none());
        assert!(version_match.is_superfluous(&superfluous));

        assert_equal(
            version_match.superfluous().map(|(nv, _dest)| nv),
            [&downgrade_new, &incompatible_new, &superfluous],
        );

        assert_equal(
            version_match.pairs().map(|x| x.source),
            ["compatible src", "downgrade src", "equal src", "incompatible src", "missing src"],
        );
        assert_equal(
            version_match.pairs().map(|x| x.dest),
            [
                Some(&"compatible dest".to_string()),
                None,
                Some(&"equal dest".to_string()),
                None,
                None,
            ],
        );

        assert_equal(
            version_match.compatible_pairs().map(|x| x.source),
            ["compatible src", "equal src"],
        );
        assert_equal(
            version_match.compatible_pairs().map(|x| x.dest),
            ["compatible dest", "equal dest"],
        );

        Ok(())
    }
}
