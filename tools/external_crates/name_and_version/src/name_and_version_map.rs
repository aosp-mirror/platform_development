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

//! A mapping from crate names and versions to some associated data.

use std::collections::{BTreeMap, HashSet};

use itertools::Itertools;
use semver::Version;

use crate::{Error, IsUpgradableTo, NameAndVersion, NamedAndVersioned};

/// A mapping from crate names and versions to some associated data.
pub trait NameAndVersionMap {
    /// The data associated with each crate name and version.
    type Value;

    /// Returns a reference to the map field.
    fn map_field(&self) -> &BTreeMap<NameAndVersion, Self::Value>;
    /// Returns a mutable reference to the map field.
    fn map_field_mut(&mut self) -> &mut BTreeMap<NameAndVersion, Self::Value>;
    /// Tries to insert a new key and value, returning an error if the key is already present.
    fn insert_or_error(&mut self, key: NameAndVersion, val: Self::Value) -> Result<(), Error>;
    /// Returns the number of crates. Multiple versions of a crate count multiple times.
    fn num_crates(&self) -> usize;
    /// Returns true if the map contains a crate with the specified name.
    fn contains_name(&self, name: &str) -> bool {
        self.get_versions(name).next().is_some()
    }
    /// Returns an iterator over versions of a specified crate
    fn get_versions<'a, 'b>(
        &'a self,
        name: &'b str,
    ) -> Box<dyn Iterator<Item = (&'a NameAndVersion, &'a Self::Value)> + 'a>;
    /// Returns a mutable iterator over versions of a specified crate
    fn get_versions_mut<'a, 'b>(
        &'a mut self,
        name: &'b str,
    ) -> Box<dyn Iterator<Item = (&'a NameAndVersion, &'a mut Self::Value)> + 'a>;
    /// Get the highest version in the map that is semver-compatible with
    /// a given crate and version.
    fn get_version_upgradable_from<T: NamedAndVersioned + IsUpgradableTo>(
        &self,
        other: &T,
    ) -> Option<&NameAndVersion> {
        let mut best_version = None;
        for (nv, _val) in self.get_versions(other.name()) {
            if other.is_upgradable_to(nv) {
                best_version.replace(nv);
            }
        }
        best_version
    }
    /// Returns an iterator over the map, filtered according to a predicate that acts on
    /// all the available versions for a crate.
    fn filter_versions<
        'a: 'b,
        'b,
        F: Fn(&mut dyn Iterator<Item = (&'b NameAndVersion, &'b Self::Value)>) -> HashSet<Version>
            + 'a,
    >(
        &'a self,
        f: F,
    ) -> Box<dyn Iterator<Item = (&'a NameAndVersion, &'a Self::Value)> + 'a>;
}

impl<ValueType> NameAndVersionMap for BTreeMap<NameAndVersion, ValueType> {
    type Value = ValueType;

    fn map_field(&self) -> &BTreeMap<NameAndVersion, Self::Value> {
        self
    }

    fn map_field_mut(&mut self) -> &mut BTreeMap<NameAndVersion, Self::Value> {
        self
    }

    fn insert_or_error(&mut self, key: NameAndVersion, val: Self::Value) -> Result<(), Error> {
        match self.entry(key) {
            std::collections::btree_map::Entry::Vacant(e) => {
                e.insert(val);
                Ok(())
            }
            std::collections::btree_map::Entry::Occupied(e) => {
                Err(Error::DuplicateVersion(e.key().name().to_string(), e.key().version().clone()))
            }
        }
    }

    fn num_crates(&self) -> usize {
        let mut seen = ::std::collections::HashSet::new();
        for nv in self.keys() {
            seen.insert(nv.name().to_string());
        }
        seen.len()
    }

    fn get_versions<'a, 'b>(
        &'a self,
        name: &'b str,
    ) -> Box<dyn Iterator<Item = (&'a NameAndVersion, &'a Self::Value)> + 'a> {
        let owned_name = name.to_string();
        Box::new(
            self.range(std::ops::RangeFrom {
                start: NameAndVersion::min_version(name.to_string()),
            })
            .map_while(move |x| if x.0.name() == owned_name { Some(x) } else { None }),
        )
    }

    fn get_versions_mut<'a, 'b>(
        &'a mut self,
        name: &'b str,
    ) -> Box<dyn Iterator<Item = (&'a NameAndVersion, &'a mut Self::Value)> + 'a> {
        let owned_name = name.to_string();
        Box::new(
            self.range_mut(std::ops::RangeFrom {
                start: NameAndVersion::min_version(name.to_string()),
            })
            .map_while(move |x| if x.0.name() == owned_name { Some(x) } else { None }),
        )
    }

    fn filter_versions<
        'a: 'b,
        'b,
        F: Fn(&mut dyn Iterator<Item = (&'b NameAndVersion, &'b Self::Value)>) -> HashSet<Version>
            + 'a,
    >(
        &'a self,
        f: F,
    ) -> Box<dyn Iterator<Item = (&'a NameAndVersion, &'a Self::Value)> + 'a> {
        let mut kept_keys: HashSet<NameAndVersion> = HashSet::new();
        for (key, mut group) in self.iter().chunk_by(|item| item.0.name()).into_iter() {
            kept_keys.extend(
                f(&mut group).into_iter().map(move |v| NameAndVersion::new(key.to_string(), v)),
            );
        }
        Box::new(self.iter().filter(move |(nv, _krate)| kept_keys.contains(*nv)))
    }
}

/// Select crates with a single version from an iterator.
pub fn crates_with_single_version<'a, ValueType>(
    versions: &mut dyn Iterator<Item = (&'a NameAndVersion, &'a ValueType)>,
) -> HashSet<Version> {
    let mut vset = HashSet::new();
    versions.into_iter().map(|(nv, _crate)| vset.insert(nv.version().clone())).count();
    if vset.len() != 1 {
        vset.clear()
    }
    vset
}

/// Select crates with multiple versions from an iterator.
pub fn crates_with_multiple_versions<'a, ValueType>(
    versions: &mut dyn Iterator<Item = (&'a NameAndVersion, &'a ValueType)>,
) -> HashSet<Version> {
    let mut vset = HashSet::new();
    versions.into_iter().map(|(nv, _crate)| vset.insert(nv.version().clone())).count();
    if vset.len() == 1 {
        vset.clear()
    }
    vset
}

/// Select the most recent version of each crate from an iterator.
pub fn most_recent_version<'a, ValueType>(
    versions: &mut dyn Iterator<Item = (&'a NameAndVersion, &'a ValueType)>,
) -> HashSet<Version> {
    let mut vset = HashSet::new();
    if let Some((nv, _crate)) = versions.into_iter().last() {
        vset.insert(nv.version().clone());
    }
    vset
}

#[cfg(test)]
mod tests {
    use crate::{NameAndVersion, NameAndVersionRef};

    use super::*;
    use itertools::assert_equal;

    fn try_name_version_map_from_iter<'a, ValueType>(
        nvs: impl IntoIterator<Item = (&'a str, &'a str, ValueType)>,
    ) -> Result<BTreeMap<NameAndVersion, ValueType>, Error> {
        let mut test_map = BTreeMap::new();
        for (name, version, val) in nvs {
            test_map.insert_or_error(NameAndVersion::try_from_str(name, version)?, val)?;
        }
        Ok(test_map)
    }

    #[test]
    fn test_name_and_version_map_empty() -> Result<(), Error> {
        let mut test_map: BTreeMap<NameAndVersion, String> = BTreeMap::new();
        let v = Version::parse("1.2.3")?;
        let nvp = NameAndVersionRef::new("foo", &v);
        assert_eq!(test_map.num_crates(), 0);
        assert!(!test_map.contains_key(&nvp as &dyn NamedAndVersioned));
        assert!(!test_map.contains_name("foo"));
        assert!(test_map.get_mut(&nvp as &dyn NamedAndVersioned).is_none());
        Ok(())
    }

    #[test]
    fn test_name_and_version_map_nonempty() -> Result<(), Error> {
        let mut test_map = try_name_version_map_from_iter([
            ("foo", "1.2.3", "foo v1".to_string()),
            ("foo", "2.3.4", "foo v2".to_string()),
            ("bar", "1.0.0", "bar".to_string()),
        ])?;

        let foo1 = NameAndVersion::try_from_str("foo", "1.2.3")?;
        let foo2 = NameAndVersion::try_from_str("foo", "2.3.4")?;
        let bar = NameAndVersion::try_from_str("bar", "1.0.0")?;
        let wrong_name = NameAndVersion::try_from_str("baz", "1.2.3")?;
        let wrong_version = NameAndVersion::try_from_str("foo", "1.0.0")?;

        assert_eq!(test_map.num_crates(), 2);

        assert!(test_map.contains_key(&foo1));
        assert!(test_map.contains_key(&foo2));
        assert!(test_map.contains_key(&bar));
        assert!(!test_map.contains_key(&wrong_name));
        assert!(!test_map.contains_key(&wrong_version));

        assert!(test_map.contains_name("foo"));
        assert!(test_map.contains_name("bar"));
        assert!(!test_map.contains_name("baz"));

        assert_eq!(test_map.get(&foo1), Some(&"foo v1".to_string()));
        assert_eq!(test_map.get(&foo2), Some(&"foo v2".to_string()));
        assert_eq!(test_map.get(&bar), Some(&"bar".to_string()));
        assert!(!test_map.contains_key(&wrong_name));
        assert!(!test_map.contains_key(&wrong_version));

        assert_eq!(test_map.get_mut(&foo1), Some(&mut "foo v1".to_string()));
        assert_eq!(test_map.get_mut(&foo2), Some(&mut "foo v2".to_string()));
        assert_eq!(test_map.get_mut(&bar), Some(&mut "bar".to_string()));
        assert!(test_map.get_mut(&wrong_name).is_none());
        assert!(test_map.get_mut(&wrong_version).is_none());

        assert_eq!(
            test_map.get_version_upgradable_from(&NameAndVersion::try_from_str("foo", "1.2.2")?),
            Some(&foo1)
        );

        // TOOD: Iter
        assert_equal(test_map.keys(), [&bar, &foo1, &foo2]);

        assert_equal(test_map.values(), ["bar", "foo v1", "foo v2"]);
        assert_equal(test_map.values_mut(), ["bar", "foo v1", "foo v2"]);

        assert_equal(
            test_map.iter().filter(|(_nv, x)| x.starts_with("foo")).map(|(_nv, val)| val),
            ["foo v1", "foo v2"],
        );

        test_map.retain(|_nv, x| x.starts_with("foo"));
        assert_equal(test_map.values(), ["foo v1", "foo v2"]);

        Ok(())
    }

    #[test]
    fn test_filter_versions() -> Result<(), Error> {
        let test_map = try_name_version_map_from_iter([
            ("foo", "1.2.3", ()),
            ("foo", "2.3.4", ()),
            ("bar", "1.0.0", ()),
        ])?;
        let foo1 = NameAndVersion::try_from_str("foo", "1.2.3")?;
        let foo2 = NameAndVersion::try_from_str("foo", "2.3.4")?;
        let bar = NameAndVersion::try_from_str("bar", "1.0.0")?;

        assert_equal(
            test_map.filter_versions(crates_with_single_version).map(|(nv, _)| nv),
            [&bar],
        );
        assert_equal(
            test_map.filter_versions(crates_with_multiple_versions).map(|(nv, _)| nv),
            [&foo1, &foo2],
        );
        assert_equal(
            test_map.filter_versions(most_recent_version).map(|(nv, _)| nv),
            [&bar, &foo2],
        );

        Ok(())
    }
}
