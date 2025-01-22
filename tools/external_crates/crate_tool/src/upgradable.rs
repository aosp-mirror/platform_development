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

use clap::ValueEnum;
use semver::{Version, VersionReq};

/// How strictly to enforce semver compatibility.
#[derive(Copy, Clone, ValueEnum)]
pub enum SemverCompatibilityRule {
    /// Ignore semantic version. Consider any two versions compatible.
    Ignore,
    /// Consider 0.x and 0.y to be compatible, but otherwise follow standard rules.
    Loose,
    /// Follow standard semantic version rules, under which 0.x and 0.y are incompatible.
    Strict,
}
/// A trait for determining semver compatibility.
pub trait IsUpgradableTo {
    /// Returns true if the object version is upgradable to 'other', according to
    /// the specified semantic version compatibility strictness.
    fn is_upgradable_to(
        &self,
        other: &Version,
        semver_compatibility: SemverCompatibilityRule,
    ) -> bool;
}

impl IsUpgradableTo for semver::Version {
    fn is_upgradable_to(
        &self,
        other: &Version,
        semver_compatibility: SemverCompatibilityRule,
    ) -> bool {
        other > self
            && VersionReq::parse(&self.to_string())
                .is_ok_and(|req| req.matches_with_compatibility_rule(other, semver_compatibility))
    }
}

/// A trait for custom semver compatibility logic, allowing it to be ignored or relaxed.
pub trait MatchesWithCompatibilityRule {
    /// Returns true if the version matches the req, according to the
    /// custom compatibility requirements of 'semver_compatibility'.
    fn matches_with_compatibility_rule(
        &self,
        version: &Version,
        semver_compatibility: SemverCompatibilityRule,
    ) -> bool;
}
impl MatchesWithCompatibilityRule for VersionReq {
    fn matches_with_compatibility_rule(
        &self,
        version: &Version,
        semver_compatibility: SemverCompatibilityRule,
    ) -> bool {
        match semver_compatibility {
            SemverCompatibilityRule::Ignore => true,
            SemverCompatibilityRule::Loose => {
                if self.comparators.len() == 1
                    && self.comparators[0].major == 0
                    && version.major == 0
                {
                    let mut fake_v = version.clone();
                    fake_v.major = 1;
                    let mut fake_req = self.clone();
                    fake_req.comparators[0].major = 1;
                    fake_req.matches(&fake_v)
                } else {
                    self.matches(version)
                }
            }
            SemverCompatibilityRule::Strict => self.matches(version),
        }
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

        // All have same behavior for SemverCompatibility::LOOSE.
        assert!(
            version.is_upgradable_to(&patch, SemverCompatibilityRule::Strict),
            "Patch update, strict"
        );
        assert!(
            version.is_upgradable_to(&patch, SemverCompatibilityRule::Loose),
            "Patch update, loose"
        );
        assert!(
            version.is_upgradable_to(&patch, SemverCompatibilityRule::Ignore),
            "Patch update, ignore"
        );

        assert!(
            version.is_upgradable_to(&minor, SemverCompatibilityRule::Strict),
            "Minor version update, strict"
        );
        assert!(
            version.is_upgradable_to(&minor, SemverCompatibilityRule::Loose),
            "Minor version update, loose"
        );
        assert!(
            version.is_upgradable_to(&minor, SemverCompatibilityRule::Ignore),
            "Minor version update, ignore"
        );

        assert!(
            !version.is_upgradable_to(&major, SemverCompatibilityRule::Strict),
            "Incompatible (major version) update, strict"
        );
        assert!(
            !version.is_upgradable_to(&major, SemverCompatibilityRule::Loose),
            "Incompatible (major version) update, loose"
        );
        assert!(
            version.is_upgradable_to(&major, SemverCompatibilityRule::Ignore),
            "Incompatible (major version) update, ignore"
        );

        assert!(
            !version.is_upgradable_to(&older, SemverCompatibilityRule::Strict),
            "Downgrade, strict"
        );
        assert!(
            !version.is_upgradable_to(&older, SemverCompatibilityRule::Loose),
            "Downgrade, loose"
        );
        assert!(
            !version.is_upgradable_to(&older, SemverCompatibilityRule::Ignore),
            "Downgrade, ignore"
        );

        Ok(())
    }

    #[test]
    fn test_is_upgradable_major_zero() -> Result<()> {
        let version = Version::parse("0.3.4")?;
        let patch = Version::parse("0.3.5")?;
        let minor = Version::parse("0.4.0")?;
        let major = Version::parse("1.0.0")?;
        let older = Version::parse("0.3.3")?;

        assert!(
            version.is_upgradable_to(&patch, SemverCompatibilityRule::Strict),
            "Patch update, strict"
        );
        assert!(
            version.is_upgradable_to(&patch, SemverCompatibilityRule::Loose),
            "Patch update, loose"
        );
        assert!(
            version.is_upgradable_to(&patch, SemverCompatibilityRule::Ignore),
            "Patch update, ignore"
        );

        // Different behavior for minor version changes.
        assert!(
            !version.is_upgradable_to(&minor, SemverCompatibilityRule::Strict),
            "Minor version update, strict"
        );
        assert!(
            version.is_upgradable_to(&minor, SemverCompatibilityRule::Loose),
            "Minor version update, loose"
        );
        assert!(
            version.is_upgradable_to(&minor, SemverCompatibilityRule::Ignore),
            "Minor version update, ignore"
        );

        assert!(
            !version.is_upgradable_to(&major, SemverCompatibilityRule::Strict),
            "Incompatible (major version) update, strict"
        );
        assert!(
            !version.is_upgradable_to(&major, SemverCompatibilityRule::Loose),
            "Incompatible (major version) update, loose"
        );
        assert!(
            version.is_upgradable_to(&major, SemverCompatibilityRule::Ignore),
            "Incompatible (major version) update, ignore"
        );

        assert!(
            !version.is_upgradable_to(&older, SemverCompatibilityRule::Strict),
            "Downgrade, strict"
        );
        assert!(
            !version.is_upgradable_to(&older, SemverCompatibilityRule::Loose),
            "Downgrade, loose"
        );
        assert!(
            !version.is_upgradable_to(&older, SemverCompatibilityRule::Ignore),
            "Downgrade, ignore"
        );

        Ok(())
    }
}
