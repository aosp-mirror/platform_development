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

//! TOML file to store per-crate configuration.

use std::{fs::read_to_string, io, path::Path};

use serde::Deserialize;

/// A parsed android_config.toml file
#[derive(Deserialize, Debug, Default, Clone)]
pub struct CrateConfig {
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    deletions: Vec<String>,

    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    update_with: Vec<String>,
}

#[allow(missing_docs)]
#[derive(thiserror::Error, Debug)]
pub enum Error {
    #[error("TOML parse error")]
    TomlParseError(#[from] toml::de::Error),
    #[error("IO error")]
    IoError(#[from] io::Error),
}

/// The crate config file name.
pub static CONFIG_FILE_NAME: &str = "android_config.toml";

impl CrateConfig {
    /// Read the android_config.toml file in the specified directory. If not present,
    /// a default version is returned.
    pub fn read(crate_dir: impl AsRef<Path>) -> Result<CrateConfig, Error> {
        let config_file = crate_dir.as_ref().join(CONFIG_FILE_NAME);
        if !config_file.exists() {
            return Ok(CrateConfig::default());
        }
        let toml: CrateConfig = toml::from_str(read_to_string(config_file)?.as_str())?;
        Ok(toml)
    }
    /// Get an iterator over directories and files to delete.
    pub fn deletions(&self) -> impl Iterator<Item = &str> {
        self.deletions.iter().map(|d| d.as_str())
    }
    /// Get an iterator of crates that also need to be updated at the same time as this crate.
    pub fn update_with(&self) -> impl Iterator<Item = &str> {
        self.update_with.iter().map(|d| d.as_str())
    }
}

#[cfg(test)]
mod tests {
    use std::fs::write;

    use super::*;

    #[test]
    fn basic() {
        let dir = tempfile::tempdir().expect("Failed to create tempdir");
        write(dir.path().join(CONFIG_FILE_NAME), r#"deletions = ["foo"]"#)
            .expect("Failed to write to tempdir");
        let config = CrateConfig::read(dir.path()).expect("Failed to read config file");
        assert_eq!(config.deletions, ["foo"]);
    }

    #[test]
    fn default() {
        let dir = tempfile::tempdir().expect("Failed to create tempdir");
        let config = CrateConfig::read(dir.path()).expect("Failed to get default config");
        assert!(config.deletions.is_empty());
    }

    #[test]
    fn parse_error() {
        let dir = tempfile::tempdir().expect("Failed to create tempdir");
        write(dir.path().join(CONFIG_FILE_NAME), r#"blah"#).expect("Failed to write to tempdir");
        assert!(matches!(CrateConfig::read(dir.path()), Err(Error::TomlParseError(_))));
    }
}
