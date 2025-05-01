// Copyright (C) 2025 The Android Open Source Project
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

/// Trait for parsing and comparing crate versions. crates_index stores them as
/// strings, but it's nicer if they are parsed into semver::Version.
pub trait ParsedVersion {
    /// Get the parsed representation of a crate version.
    fn parsed_version(&self) -> Result<semver::Version, semver::Error>;
    /// Return true if the version is equal to `other`.
    /// Unparseable versions are never equal to anything.
    fn version_eq(&self, other: &semver::Version) -> bool {
        self.parsed_version().is_ok_and(|parsed| parsed.eq(other))
    }
    /// Return true if the version is greater than to `other`.
    /// Returns false if the version is unparseable.
    fn version_gt(&self, other: &semver::Version) -> bool {
        self.parsed_version().is_ok_and(|parsed| parsed.gt(other))
    }
}

impl ParsedVersion for crates_index::Version {
    fn parsed_version(&self) -> Result<semver::Version, semver::Error> {
        semver::Version::parse(self.version())
    }
}

/// Trait for checking if a crate version is "safe", meaning not yanked or pre-release.
pub trait IsSafe {
    /// Return true if the version is "safe", meaning not yanked or pre-release.
    fn is_safe(&self) -> bool;
}

impl IsSafe for crates_index::Version {
    fn is_safe(&self) -> bool {
        !self.is_yanked() && self.parsed_version().is_ok_and(|parsed| parsed.pre.is_empty())
    }
}

/// Filter crate versions by those that are "safe", meaning not yanked or pre-release.
pub trait SafeVersions {
    /// Versions of the crate that aren't yanked or pre-release.
    fn safe_versions(&self) -> impl DoubleEndedIterator<Item = &crates_index::Version>;
    /// Safe versions of the crate grater than the specified version.
    /// That is, potential upgrades, more or less.
    fn safe_versions_gt(
        &self,
        version: &semver::Version,
    ) -> impl DoubleEndedIterator<Item = &crates_index::Version> {
        self.safe_versions().filter(|v| v.version_gt(version))
    }
}

impl SafeVersions for crates_index::Crate {
    fn safe_versions(&self) -> impl DoubleEndedIterator<Item = &crates_index::Version> {
        self.versions().iter().filter(|v| v.is_safe())
    }
}

/// Get a specific version of a crate.
pub trait GetVersion {
    /// Get a specific version of a crate. O(N)
    fn get_version(&self, version: &semver::Version) -> Option<&crates_index::Version>;
    /// Get a particular version from the list of safe versions, or None if no
    /// safe version exists.
    fn get_safe_version(&self, version: &semver::Version) -> Option<&crates_index::Version>;
}

impl GetVersion for crates_index::Crate {
    fn get_version(&self, version: &semver::Version) -> Option<&crates_index::Version> {
        self.versions().iter().find(|v| v.version_eq(version))
    }
    fn get_safe_version(&self, version: &semver::Version) -> Option<&crates_index::Version> {
        self.safe_versions().find(|v| v.version_eq(version))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    use itertools::assert_equal;

    #[test]
    fn test_parsed_version() {
        let hashbrown_0_12_3: crates_index::Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.12.3"))
                .expect("Failed to parse JSON testdata");
        assert_eq!(
            hashbrown_0_12_3.parsed_version().expect("Failed to parse version"),
            semver::Version::new(0, 12, 3)
        );
        assert!(hashbrown_0_12_3.version_eq(&semver::Version::new(0, 12, 3)));
        assert!(!hashbrown_0_12_3.version_eq(&semver::Version::new(0, 12, 4)));
        assert!(hashbrown_0_12_3.version_gt(&semver::Version::new(0, 12, 2)));
        assert!(!hashbrown_0_12_3.version_gt(&semver::Version::new(0, 12, 3)));
        assert!(!hashbrown_0_12_3.version_gt(&semver::Version::new(0, 12, 4)));
    }

    #[test]
    fn test_is_safe() {
        let hashbrown_0_12_3: crates_index::Version =
            serde_json::from_str(include_str!("testdata/hashbrown-0.12.3"))
                .expect("Failed to parse JSON testdata");
        assert!(hashbrown_0_12_3.is_safe());
        let ahash_0_8_3: crates_index::Version =
            serde_json::from_str(include_str!("testdata/ahash-0.8.3"))
                .expect("Failed to parse JSON testdata");
        assert!(!ahash_0_8_3.is_safe(), "ahash 0.8.3 is yanked");
        let ring_0_17_11_alpha1: crates_index::Version =
            serde_json::from_str(include_str!("testdata/ring-0.17.11-alpha1"))
                .expect("Failed to parse JSON testdata");
        assert!(!ring_0_17_11_alpha1.is_safe(), "ring 0.17.11-alpha1 is pre-release");
    }

    #[test]
    fn test_safe_versions() {
        let ring: crates_index::Crate = serde_json::from_str(include_str!("testdata/ring"))
            .expect("Failed to parse JSON testdata");
        assert_equal(ring.safe_versions().map(|v| v.version()), ["0.17.0", "0.17.2"]);
        assert_equal(
            ring.safe_versions_gt(&semver::Version::new(0, 17, 0)).map(|v| v.version()),
            ["0.17.2"],
        );
    }

    #[test]
    fn test_get_version() {
        let ring: crates_index::Crate = serde_json::from_str(include_str!("testdata/ring"))
            .expect("Failed to parse JSON testdata");
        assert!(ring.get_version(&semver::Version::new(0, 16, 0)).is_none());
        assert!(ring
            .get_version(&semver::Version::new(0, 17, 0))
            .is_some_and(|v| v.version() == "0.17.0"));
        assert!(ring
            .get_version(&semver::Version::new(0, 17, 1))
            .is_some_and(|v| v.version() == "0.17.1"));
        assert!(ring
            .get_version(&semver::Version::parse("0.17.1-alpha.1").unwrap())
            .is_some_and(|v| v.version() == "0.17.1-alpha.1"));

        assert!(ring.get_safe_version(&semver::Version::new(0, 16, 0)).is_none());
        assert!(ring
            .get_safe_version(&semver::Version::new(0, 17, 0))
            .is_some_and(|v| v.version() == "0.17.0"));
        assert!(ring.get_safe_version(&semver::Version::new(0, 17, 1)).is_none(), "yanked version");
        assert!(
            ring.get_safe_version(&semver::Version::parse("0.17.1-alpha.1").unwrap()).is_none(),
            "pre-release version"
        );
    }
}
