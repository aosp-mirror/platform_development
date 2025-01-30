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

//! Data structures for representing a crate name and version, which can be
//! used as a map key.

use std::{
    borrow::Borrow,
    cmp::Ordering,
    hash::{Hash, Hasher},
};

use semver::{BuildMetadata, Prerelease, Version};

static MIN_VERSION: Version =
    Version { major: 0, minor: 0, patch: 0, pre: Prerelease::EMPTY, build: BuildMetadata::EMPTY };

/// A name and version pair trait.
pub trait NamedAndVersioned {
    /// Returns the name.
    fn name(&self) -> &str;
    /// Returns the version.
    fn version(&self) -> &Version;
    /// Returns a reference that can be used as a map key.
    fn key(&self) -> NameAndVersionRef;
}

/// An owned namd and version.
#[derive(Debug, PartialOrd, Ord, PartialEq, Eq, Hash, Clone)]
pub struct NameAndVersion {
    name: String,
    version: Version,
}

/// A reference to a name and version.
#[derive(Copy, Clone, Debug, PartialOrd, Ord, PartialEq, Eq, Hash)]
pub struct NameAndVersionRef<'a> {
    name: &'a str,
    version: &'a Version,
}

impl NameAndVersion {
    /// Constructor that takes ownership of args.
    pub fn new(name: String, version: Version) -> Self {
        NameAndVersion { name, version }
    }
    /// Constructor that clones a reference.
    pub fn from(nv: &impl NamedAndVersioned) -> Self {
        NameAndVersion { name: nv.name().to_string(), version: nv.version().clone() }
    }
    /// The lowest possible version, used to find the first key in a map with this name.
    pub fn min_version(name: String) -> Self {
        NameAndVersion { name, version: MIN_VERSION.clone() }
    }
    /// Intended for testing.
    pub fn try_from_str(name: &str, version: &str) -> Result<Self, semver::Error> {
        Ok(NameAndVersion::new(name.to_string(), Version::parse(version)?))
    }
}

impl NamedAndVersioned for NameAndVersion {
    fn name(&self) -> &str {
        self.name.as_str()
    }

    fn version(&self) -> &Version {
        &self.version
    }
    fn key(&self) -> NameAndVersionRef {
        NameAndVersionRef::new(self.name(), self.version())
    }
}

impl<'a> NameAndVersionRef<'a> {
    /// Construct a reference to a name and version.
    pub fn new(name: &'a str, version: &'a Version) -> Self {
        NameAndVersionRef { name, version }
    }
}

impl NamedAndVersioned for NameAndVersionRef<'_> {
    fn name(&self) -> &str {
        self.name
    }
    fn version(&self) -> &Version {
        self.version
    }
    fn key(&self) -> NameAndVersionRef {
        *self
    }
}

impl<'a> Borrow<dyn NamedAndVersioned + 'a> for NameAndVersion {
    fn borrow(&self) -> &(dyn NamedAndVersioned + 'a) {
        self
    }
}

impl PartialEq for (dyn NamedAndVersioned + '_) {
    fn eq(&self, other: &Self) -> bool {
        self.key().eq(&other.key())
    }
}

impl Eq for (dyn NamedAndVersioned + '_) {}

impl PartialOrd for (dyn NamedAndVersioned + '_) {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

impl Ord for (dyn NamedAndVersioned + '_) {
    fn cmp(&self, other: &Self) -> Ordering {
        self.key().cmp(&other.key())
    }
}

impl Hash for (dyn NamedAndVersioned + '_) {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.key().hash(state)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_name_version_ref() -> Result<(), semver::Error> {
        let version = Version::parse("2.3.4")?;
        let nvp = NameAndVersionRef::new("foo", &version);
        assert_eq!(nvp.name(), "foo");
        assert_eq!(nvp.version().to_string(), "2.3.4");
        Ok(())
    }

    #[test]
    fn test_name_and_version() -> Result<(), semver::Error> {
        let version = Version::parse("2.3.4")?;
        let nvp = NameAndVersion::new("foo".to_string(), version);
        assert_eq!(nvp.name(), "foo");
        assert_eq!(nvp.version().to_string(), "2.3.4");
        Ok(())
    }
}
