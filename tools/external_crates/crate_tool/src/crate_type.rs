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

use anyhow::{bail, Result};
use cargo_toml::Manifest;
use name_and_version::{NameAndVersionRef, NamedAndVersioned};
use rooted_path::RootedPath;
use semver::Version;

use crate::CrateError;

#[derive(Debug, Clone)]
pub struct Crate {
    manifest: Manifest,
    path: RootedPath,
    version: Version,
}

impl NamedAndVersioned for Crate {
    fn name(&self) -> &str {
        self.manifest.package().name()
    }
    fn version(&self) -> &Version {
        &self.version
    }
    fn key(&self) -> NameAndVersionRef {
        NameAndVersionRef::new(self.name(), self.version())
    }
}

impl Crate {
    pub fn new(manifest: Manifest, path: RootedPath) -> Crate {
        let version = Version::parse(manifest.package().version()).unwrap();
        Crate { manifest, path, version }
    }
    pub fn from(manifest_dir: RootedPath) -> Result<Crate> {
        let manifest = Manifest::from_path(manifest_dir.abs().join("Cargo.toml"))?;
        if manifest.package.is_none() {
            bail!(CrateError::VirtualCrate(manifest_dir.as_ref().to_path_buf()));
        }
        Ok(Crate::new(manifest, manifest_dir))
    }

    pub fn description(&self) -> &str {
        self.manifest.package().description().unwrap_or("")
    }
    pub fn license(&self) -> Option<&str> {
        self.manifest.package().license()
    }
    pub fn repository(&self) -> Option<&str> {
        self.manifest.package().repository()
    }
    pub fn path(&self) -> &RootedPath {
        &self.path
    }
}

#[cfg(test)]
mod tests {
    use std::{
        fs::{create_dir, write},
        path::Path,
    };

    use super::*;
    use anyhow::anyhow;
    use tempfile::tempdir;

    fn write_test_manifest(
        temp_crate_dir: &Path,
        name: &str,
        version: &str,
        license: &str,
        description: &str,
        repository: &str,
    ) -> Result<RootedPath> {
        let temp_crate_dir = RootedPath::new("/", temp_crate_dir.strip_prefix("/")?)?;
        write(
            temp_crate_dir.join("Cargo.toml")?,
            format!(
                r#"
[package]
name = "{name}"
version = "{version}"
license = "{license}"
description = "{description}"
repository = "{repository}"
"#
            ),
        )?;
        let lib_rs = temp_crate_dir.join("src/lib.rs")?;
        create_dir(lib_rs.abs().parent().ok_or(anyhow!("Failed to get parent"))?)?;
        write(lib_rs, "// foo")?;
        Ok(temp_crate_dir)
    }

    #[test]
    fn test_from_and_properties() -> Result<()> {
        let name = "foo";
        let version = "1.2.0";
        let license = "Apache-2.0";
        let description = "description";
        let repository = "repo";
        let temp_crate_dir = tempdir()?;
        let manifest_dir = write_test_manifest(
            temp_crate_dir.path(),
            name,
            version,
            license,
            description,
            repository,
        )?;
        let krate = Crate::from(manifest_dir)?;
        assert_eq!(krate.name(), name);
        assert_eq!(krate.version().to_string(), version);
        assert_eq!(krate.description(), description);
        assert_eq!(krate.license(), Some(license));
        assert_eq!(krate.repository(), Some(repository));
        assert_eq!(krate.path().abs(), temp_crate_dir.path());
        Ok(())
    }

    #[test]
    fn test_from_error() -> Result<()> {
        let temp_crate_dir = tempdir()?;
        let manifest_dir =
            write_test_manifest(temp_crate_dir.path(), "foo", "1.2.0", "blah", "blah", "blah")?;
        assert!(Crate::from(manifest_dir.join("blah")?).is_err());
        Ok(())
    }
}
