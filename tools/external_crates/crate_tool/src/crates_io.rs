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

use anyhow::{anyhow, Result};
use cfg_expr::{
    targets::{Arch, Family, Os},
    Predicate, TargetPredicate,
};
use crates_index::{http, Crate, Dependency, DependencyKind, SparseIndex, Version};
use semver::VersionReq;
use std::{
    cell::RefCell,
    collections::{HashMap, HashSet},
};

pub struct CratesIoIndex {
    fetcher: Box<dyn CratesIoFetcher>,
}

impl CratesIoIndex {
    pub fn new() -> Result<CratesIoIndex> {
        Ok(CratesIoIndex {
            fetcher: Box::new(OnlineFetcher {
                index: crates_index::SparseIndex::new_cargo_default()?,
                agent: ureq::Agent::new_with_defaults(),
                fetched: RefCell::new(HashSet::new()),
            }),
        })
    }
    pub fn new_offline() -> Result<CratesIoIndex> {
        Ok(CratesIoIndex {
            fetcher: Box::new(OfflineFetcher {
                index: crates_index::SparseIndex::new_cargo_default()?,
            }),
        })
    }
    pub fn get_crate(&self, crate_name: impl AsRef<str>) -> Result<Crate> {
        self.fetcher.fetch(crate_name.as_ref())
    }
}

pub trait CratesIoFetcher {
    fn fetch(&self, crate_name: &str) -> Result<Crate>;
}

pub struct OnlineFetcher {
    index: SparseIndex,
    agent: ureq::Agent,
    // Keep track of crates we have fetched, to avoid fetching them multiple times.
    fetched: RefCell<HashSet<String>>,
}

pub struct OfflineFetcher {
    index: SparseIndex,
}

impl CratesIoFetcher for OnlineFetcher {
    fn fetch(&self, crate_name: &str) -> Result<Crate> {
        let mut fetched = self.fetched.borrow_mut();
        if fetched.contains(crate_name) {
            return Ok(self.index.crate_from_cache(crate_name.as_ref())?);
        }

        let request = self
            .index
            .make_cache_request(crate_name)?
            .version(ureq::http::Version::HTTP_11)
            .body(())?;

        let response = self.agent.run(request)?;
        let (parts, mut body) = response.into_parts();
        let response = http::Response::from_parts(parts, body.read_to_vec()?);
        let response = self
            .index
            .parse_cache_response(crate_name, response, true)?
            .ok_or(anyhow!("Crate not found"))?;

        fetched.insert(crate_name.to_string());
        Ok(response)
    }
}
impl CratesIoFetcher for OfflineFetcher {
    fn fetch(&self, crate_name: &str) -> Result<Crate> {
        Ok(self.index.crate_from_cache(crate_name.as_ref())?)
    }
}

/// Filter versions by those that are "safe", meaning not yanked or pre-release.
pub trait SafeVersions {
    // Versions of the crate that aren't yanked or pre-release.
    fn safe_versions(&self) -> impl DoubleEndedIterator<Item = &Version>;
    // Versions of the crate greater than 'version'.
    fn versions_gt(&self, version: &semver::Version) -> impl DoubleEndedIterator<Item = &Version> {
        self.safe_versions().filter(|v| {
            semver::Version::parse(v.version()).map_or(false, |parsed| parsed.gt(version))
        })
    }
    // Get a specific version of a crate.
    fn get_version(&self, version: &semver::Version) -> Option<&Version> {
        self.safe_versions().find(|v| {
            semver::Version::parse(v.version()).map_or(false, |parsed| parsed.eq(version))
        })
    }
}
impl SafeVersions for Crate {
    fn safe_versions(&self) -> impl DoubleEndedIterator<Item = &Version> {
        self.versions().iter().filter(|v| {
            !v.is_yanked()
                && semver::Version::parse(v.version()).map_or(false, |parsed| parsed.pre.is_empty())
        })
    }
}

/// Filter dependencies for those likely to be relevant to Android.
pub trait AndroidDependencies {
    fn android_deps(&self) -> impl DoubleEndedIterator<Item = &Dependency>;
    fn android_version_reqs_by_name(&self) -> HashMap<&str, &str> {
        self.android_deps().map(|dep| (dep.crate_name(), dep.requirement())).collect()
    }
    fn android_deps_with_version_reqs(
        &self,
    ) -> impl DoubleEndedIterator<Item = (&Dependency, VersionReq)> {
        self.android_deps().filter_map(|dep| {
            VersionReq::parse(dep.requirement()).map_or(None, |req| Some((dep, req)))
        })
    }
}
impl AndroidDependencies for Version {
    fn android_deps(&self) -> impl DoubleEndedIterator<Item = &Dependency> {
        self.dependencies().iter().filter(|dep| {
            dep.kind() == DependencyKind::Normal && !dep.is_optional() && dep.is_android()
        })
    }
}

/// Dependencies that are likely to be relevant to Android.
/// Unconditional dependencies (without a target cfg string) are always relevant.
/// Conditional dependencies are relevant if they are for Unix, Android, or Linux, and for an architecture we care about (Arm, RISC-V, or X86)
pub trait IsAndroid {
    /// Returns true if this dependency is likely to be relevant to Android.
    fn is_android(&self) -> bool;
}
impl IsAndroid for Dependency {
    fn is_android(&self) -> bool {
        self.target().map_or(true, is_android)
    }
}
fn is_android(target: &str) -> bool {
    let expr = cfg_expr::Expression::parse(target);
    if expr.is_err() {
        return false;
    }
    let expr = expr.unwrap();
    expr.eval(|pred| match pred {
        Predicate::Target(target_predicate) => match target_predicate {
            TargetPredicate::Family(family) => *family == Family::unix,
            TargetPredicate::Os(os) => *os == Os::android || *os == Os::linux,
            TargetPredicate::Arch(arch) => {
                [Arch::arm, Arch::aarch64, Arch::riscv32, Arch::riscv64, Arch::x86, Arch::x86_64]
                    .contains(arch)
            }
            _ => true,
        },
        _ => true,
    })
}

pub trait DependencyChanges {
    fn is_new_dep(&self, base_deps: &HashMap<&str, &str>) -> bool;
    fn is_changed_dep(&self, base_deps: &HashMap<&str, &str>) -> bool;
}

impl DependencyChanges for Dependency {
    fn is_new_dep(&self, base_deps: &HashMap<&str, &str>) -> bool {
        !base_deps.contains_key(self.crate_name())
    }

    fn is_changed_dep(&self, base_deps: &HashMap<&str, &str>) -> bool {
        let base_dep = base_deps.get(self.crate_name());
        base_dep.is_none() || base_dep.is_some_and(|base_req| *base_req != self.requirement())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn test_android_cfgs() {
        assert!(!is_android("asmjs-unknown-emscripten"), "Parse error");
        assert!(!is_android("cfg(windows)"));
        assert!(is_android("cfg(unix)"));
        assert!(!is_android(r#"cfg(target_os = "redox")"#));
        assert!(!is_android(r#"cfg(target_arch = "wasm32")"#));
        assert!(is_android(r#"cfg(any(target_os = "linux", target_os = "android"))"#));
        assert!(is_android(
            r#"cfg(any(all(target_arch = "arm", target_pointer_width = "32"), target_arch = "mips", target_arch = "powerpc"))"#
        ));
        assert!(!is_android(
            r#"cfg(all(target_arch = "wasm32", target_vendor = "unknown", target_os = "unknown"))"#
        ));
        assert!(is_android("cfg(tracing_unstable)"));
        assert!(is_android(r#"cfg(any(unix, target_os = "wasi"))"#));
        assert!(is_android(r#"cfg(not(all(target_arch = "arm", target_os = "none")))"#))
    }
}
