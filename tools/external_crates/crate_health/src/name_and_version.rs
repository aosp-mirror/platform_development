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

use std::{
    borrow::Borrow,
    cmp::Ordering,
    hash::{Hash, Hasher},
};

#[cfg(test)]
use anyhow::Result;

use semver::{BuildMetadata, Prerelease, Version, VersionReq};

static MIN_VERSION: Version =
    Version { major: 0, minor: 0, patch: 0, pre: Prerelease::EMPTY, build: BuildMetadata::EMPTY };

pub trait NamedAndVersioned {
    fn name(&self) -> &str;
    fn version(&self) -> &Version;
    fn key<'a>(&'a self) -> NameAndVersionRef<'a>;
}

#[derive(Debug, PartialOrd, Ord, PartialEq, Eq, Hash, Clone)]
pub struct NameAndVersion {
    name: String,
    version: Version,
}

#[derive(Copy, Clone, Debug, PartialOrd, Ord, PartialEq, Eq, Hash)]
pub struct NameAndVersionRef<'a> {
    name: &'a str,
    version: &'a Version,
}

impl NameAndVersion {
    pub fn new(name: String, version: Version) -> Self {
        NameAndVersion { name, version }
    }
    pub fn from(nv: &impl NamedAndVersioned) -> Self {
        NameAndVersion { name: nv.name().to_string(), version: nv.version().clone() }
    }
    pub fn min_version(name: String) -> Self {
        NameAndVersion { name, version: MIN_VERSION.clone() }
    }
    #[cfg(test)]
    pub fn try_from_str(name: &str, version: &str) -> Result<Self> {
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
    fn key<'k>(&'k self) -> NameAndVersionRef<'k> {
        NameAndVersionRef::new(self.name(), self.version())
    }
}

impl<'a> NameAndVersionRef<'a> {
    pub fn new(name: &'a str, version: &'a Version) -> Self {
        NameAndVersionRef { name, version }
    }
}

impl<'a> NamedAndVersioned for NameAndVersionRef<'a> {
    fn name(&self) -> &str {
        self.name
    }
    fn version(&self) -> &Version {
        self.version
    }
    fn key<'k>(&'k self) -> NameAndVersionRef<'k> {
        *self
    }
}

impl<'a> Borrow<dyn NamedAndVersioned + 'a> for NameAndVersion {
    fn borrow(&self) -> &(dyn NamedAndVersioned + 'a) {
        self
    }
}

impl<'a> PartialEq for (dyn NamedAndVersioned + 'a) {
    fn eq(&self, other: &Self) -> bool {
        self.key().eq(&other.key())
    }
}

impl<'a> Eq for (dyn NamedAndVersioned + 'a) {}

impl<'a> PartialOrd for (dyn NamedAndVersioned + 'a) {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        self.key().partial_cmp(&other.key())
    }
}

impl<'a> Ord for (dyn NamedAndVersioned + 'a) {
    fn cmp(&self, other: &Self) -> Ordering {
        self.key().cmp(&other.key())
    }
}

impl<'a> Hash for (dyn NamedAndVersioned + 'a) {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.key().hash(state)
    }
}

pub trait IsUpgradableTo: NamedAndVersioned {
    fn is_upgradable_to(&self, other: &impl NamedAndVersioned) -> bool {
        self.name() == other.name()
            && VersionReq::parse(&self.version().to_string())
                .is_ok_and(|req| req.matches(other.version()))
    }
}

impl<'a> IsUpgradableTo for NameAndVersion {}
impl<'a> IsUpgradableTo for NameAndVersionRef<'a> {}

#[cfg(test)]
mod tests {
    use super::*;
    use anyhow::Result;

    #[test]
    fn test_name_version_ref() -> Result<()> {
        let version = Version::parse("2.3.4")?;
        let compat1 = Version::parse("2.3.5")?;
        let compat2 = Version::parse("2.4.0")?;
        let incompat = Version::parse("3.0.0")?;
        let older = Version::parse("2.3.3")?;
        let nvp = NameAndVersionRef::new("foo", &version);
        assert_eq!(nvp.name(), "foo");
        assert_eq!(nvp.version().to_string(), "2.3.4");
        assert!(nvp.is_upgradable_to(&NameAndVersionRef::new("foo", &compat1)), "Patch update");
        assert!(
            nvp.is_upgradable_to(&NameAndVersionRef::new("foo", &compat2)),
            "Minor version update"
        );
        assert!(
            !nvp.is_upgradable_to(&NameAndVersionRef::new("foo", &incompat)),
            "Incompatible (major version) update"
        );
        assert!(!nvp.is_upgradable_to(&NameAndVersionRef::new("foo", &older)), "Downgrade");
        assert!(!nvp.is_upgradable_to(&NameAndVersionRef::new("bar", &compat1)), "Different name");
        Ok(())
    }

    #[test]
    fn test_name_and_version() -> Result<()> {
        let version = Version::parse("2.3.4")?;
        let compat1 = Version::parse("2.3.5")?;
        let compat2 = Version::parse("2.4.0")?;
        let incompat = Version::parse("3.0.0")?;
        let older = Version::parse("2.3.3")?;
        let nvp = NameAndVersion::new("foo".to_string(), version);
        assert_eq!(nvp.name(), "foo");
        assert_eq!(nvp.version().to_string(), "2.3.4");
        assert!(nvp.is_upgradable_to(&NameAndVersionRef::new("foo", &compat1)), "Patch update");
        assert!(
            nvp.is_upgradable_to(&NameAndVersionRef::new("foo", &compat2)),
            "Minor version update"
        );
        assert!(
            !nvp.is_upgradable_to(&NameAndVersionRef::new("foo", &incompat)),
            "Incompatible (major version) update"
        );
        assert!(!nvp.is_upgradable_to(&NameAndVersionRef::new("foo", &older)), "Downgrade");
        assert!(!nvp.is_upgradable_to(&NameAndVersionRef::new("bar", &compat1)), "Different name");
        Ok(())
    }
}
