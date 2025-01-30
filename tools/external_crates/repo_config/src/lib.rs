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

//! TOML file to store per-repo configuration.

use std::{collections::BTreeSet, fs::read_to_string, io, path::Path};

use serde::Deserialize;

/// A parsed android_repo_config.toml file
#[derive(Deserialize, Debug, Default, Clone)]
pub struct RepoConfig {
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    crate_denylist: BTreeSet<String>,
}

#[allow(missing_docs)]
#[derive(thiserror::Error, Debug)]
pub enum Error {
    #[error("TOML parse error")]
    TomlParseError(#[from] toml::de::Error),
    #[error("IO error")]
    IoError(#[from] io::Error),
}

/// The repo config file name.
pub static CONFIG_FILE_NAME: &str = "android_repo_config.toml";

impl RepoConfig {
    /// Read the android_repo_config.toml file in the specified directory. If not present,
    /// a default version is returned.
    pub fn read(managed_repo_dir: impl AsRef<Path>) -> Result<RepoConfig, Error> {
        let config_file = managed_repo_dir.as_ref().join(CONFIG_FILE_NAME);
        if !config_file.exists() {
            return Ok(RepoConfig::default());
        }
        let toml: RepoConfig = toml::from_str(read_to_string(config_file)?.as_str())?;
        Ok(toml)
    }
    /// Returns true if the crate is on the crate denylist.
    pub fn is_allowed(&self, crate_name: impl AsRef<str>) -> bool {
        !self.crate_denylist.contains(crate_name.as_ref())
    }
}

#[cfg(test)]
mod tests {
    use std::fs::write;

    use super::*;

    #[test]
    fn basic() {
        let dir = tempfile::tempdir().expect("Failed to create tempdir");
        write(dir.path().join(CONFIG_FILE_NAME), r#"crate_denylist = ["foo"]"#)
            .expect("Failed to write to tempdir");
        let config = RepoConfig::read(dir.path()).expect("Failed to read config file");
        assert!(!config.is_allowed("foo"));
        assert!(config.is_allowed("bar"));
    }

    #[test]
    fn default() {
        let dir = tempfile::tempdir().expect("Failed to create tempdir");
        let config = RepoConfig::read(dir.path()).expect("Failed to get default config");
        assert!(config.is_allowed("foo"));
    }

    #[test]
    fn parse_error() {
        let dir = tempfile::tempdir().expect("Failed to create tempdir");
        write(dir.path().join(CONFIG_FILE_NAME), r#"blah"#).expect("Failed to write to tempdir");
        assert!(matches!(RepoConfig::read(dir.path()), Err(Error::TomlParseError(_))));
    }
}
