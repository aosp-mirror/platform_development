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

/// A parsed android_config.json file
#[derive(Deserialize, Debug, Default, Clone)]
pub struct CrateConfig {
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    deletions: Vec<String>,
}

#[allow(missing_docs)]
#[derive(thiserror::Error, Debug)]
pub enum Error {
    #[error("TOML parse error")]
    TomlParseError(#[from] toml::de::Error),
    #[error("IO error")]
    IoError(#[from] io::Error),
}

impl CrateConfig {
    /// Read the android_config.toml file in the specified directory.
    pub fn read(config_file: impl AsRef<Path>) -> Result<CrateConfig, Error> {
        let config_file = config_file.as_ref();
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
}
