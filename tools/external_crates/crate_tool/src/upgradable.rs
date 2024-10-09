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

use semver::{Version, VersionReq};

/// A trait for determining semver compatibility.
pub trait IsUpgradableTo {
    /// Returns true if the object version is semver-compatible with 'other'.
    fn is_upgradable_to(&self, other: &Version) -> bool;
    /// Returns true if the object version is semver-compatible with 'other', or if
    /// both have a major version of 0 and the other version is greater.
    fn is_upgradable_to_relaxed(&self, other: &Version) -> bool;
}

impl IsUpgradableTo for semver::Version {
    fn is_upgradable_to(&self, other: &Version) -> bool {
        VersionReq::parse(&self.to_string()).is_ok_and(|req| req.matches(other))
    }
    fn is_upgradable_to_relaxed(&self, other: &Version) -> bool {
        VersionReq::parse(&self.to_string()).is_ok_and(|req| req.matches_relaxed(other))
    }
}

/// A trait for relaxed semver compatibility. Major versions of 0 are treated as if they were non-zero.
pub trait MatchesRelaxed {
    /// Returns true if the version matches the req, but treats
    /// major version of zero as if it were non-zero.
    fn matches_relaxed(&self, version: &Version) -> bool;
}
impl MatchesRelaxed for VersionReq {
    fn matches_relaxed(&self, version: &Version) -> bool {
        if self.matches(version) {
            return true;
        }
        if self.comparators.len() == 1 && self.comparators[0].major == 0 && version.major == 0 {
            let mut fake_v = version.clone();
            fake_v.major = 1;
            let mut fake_req = self.clone();
            fake_req.comparators[0].major = 1;
            return fake_req.matches(&fake_v);
        }
        false
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    use anyhow::Result;

    #[test]
    fn test_is_upgradable() -> Result<()> {
        let version = Version::parse("2.3.4")?;
        let patch = Version::parse("2.3.5")?;
        let minor = Version::parse("2.4.0")?;
        let major = Version::parse("3.0.0")?;
        let older = Version::parse("2.3.3")?;

        // All have same behavior for is_upgradable_to_relaxed
        assert!(version.is_upgradable_to(&patch), "Patch update");
        assert!(version.is_upgradable_to_relaxed(&patch), "Patch update");

        assert!(version.is_upgradable_to(&minor), "Minor version update");
        assert!(version.is_upgradable_to_relaxed(&minor), "Minor version update");

        assert!(!version.is_upgradable_to(&major), "Incompatible (major version) update");
        assert!(!version.is_upgradable_to_relaxed(&major), "Incompatible (major version) update");

        assert!(!version.is_upgradable_to(&older), "Downgrade");
        assert!(!version.is_upgradable_to_relaxed(&older), "Downgrade");

        Ok(())
    }

    #[test]
    fn test_is_upgradable_major_zero() -> Result<()> {
        let version = Version::parse("0.3.4")?;
        let patch = Version::parse("0.3.5")?;
        let minor = Version::parse("0.4.0")?;
        let major = Version::parse("1.0.0")?;
        let older = Version::parse("0.3.3")?;

        assert!(version.is_upgradable_to(&patch), "Patch update");
        assert!(version.is_upgradable_to_relaxed(&patch), "Patch update");

        // Different behavior for minor version changes.
        assert!(!version.is_upgradable_to(&minor), "Minor version update");
        assert!(version.is_upgradable_to_relaxed(&minor), "Minor version update");

        assert!(!version.is_upgradable_to(&major), "Incompatible (major version) update");
        assert!(!version.is_upgradable_to_relaxed(&major), "Incompatible (major version) update");

        assert!(!version.is_upgradable_to(&older), "Downgrade");
        assert!(!version.is_upgradable_to_relaxed(&older), "Downgrade");

        Ok(())
    }
}
