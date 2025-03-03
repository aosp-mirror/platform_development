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

//! Crate for handling Google METADATA files.

use std::{
    fs::{read_to_string, write},
    path::PathBuf,
};

use chrono::Datelike;

#[cfg(soong)]
mod metadata_proto {
    pub use google_metadata_proto::metadata::Identifier;
    pub use google_metadata_proto::metadata::LicenseType;
    pub use google_metadata_proto::metadata::MetaData;
    pub use google_metadata_proto::metadata::ThirdPartyMetaData;
}

#[cfg(not(soong))]
mod metadata_proto {
    include!(concat!(env!("OUT_DIR"), "/protos/mod.rs"));
    pub use crate::metadata::Identifier;
    pub use crate::metadata::LicenseType;
    pub use crate::metadata::MetaData;
    pub use crate::metadata::ThirdPartyMetaData;
}

pub use metadata_proto::*;

/// Error types for the 'google_metadata' crate.
#[derive(thiserror::Error, Debug)]
pub enum Error {
    /// Error trying to initialize a METADATA file that already exists
    #[error("METADATA file exists: {0}")]
    FileExists(PathBuf),
    /// Crate name not set in METADATA file
    #[error("Crate name not set in METADATA file")]
    CrateNameMissing,
    /// Crate name mismatch
    #[error("Crate names don't match: {} in METADATA vs {}", .0, .1)]
    CrateNameMismatch(String, String),
    /// Protobuf parse error
    #[error(transparent)]
    ParseError(#[from] protobuf::text_format::ParseError),
    /// Error writing METADATA file
    #[error(transparent)]
    WriteError(#[from] std::io::Error),
}

/// Wrapper around a Google METADATA file.
pub struct GoogleMetadata {
    path: PathBuf,
    metadata: MetaData,
}

impl GoogleMetadata {
    /// Reads an existing METADATA file.
    pub fn try_from<P: Into<PathBuf>>(path: P) -> Result<Self, Error> {
        let path = path.into();
        let metadata = read_to_string(&path)?;
        let metadata: MetaData = protobuf::text_format::parse_from_str(&metadata)?;
        Ok(GoogleMetadata { path, metadata })
    }
    /// Initializes a new METADATA file.
    pub fn init<P: Into<PathBuf>, V: Into<String>>(
        path: P,
        name: impl AsRef<str>,
        version: V,
        description: impl AsRef<str>,
        license_type: LicenseType,
    ) -> Result<Self, Error> {
        let path = path.into();
        if path.exists() {
            return Err(Error::FileExists(path));
        }
        let mut metadata = GoogleMetadata { path, metadata: MetaData::new() };
        metadata.update(name, version, description, license_type);
        Ok(metadata)
    }
    /// Writes to the METADATA file.
    ///
    /// The existing file is overwritten.
    pub fn write(&self) -> Result<(), Error> {
        Ok(write(&self.path, protobuf::text_format::print_to_string_pretty(&self.metadata))?)
    }
    fn third_party(&mut self) -> &mut ThirdPartyMetaData {
        self.metadata.third_party.mut_or_insert_default()
    }
    /// Sets the date fields to today's date.
    fn set_date_to_today(&mut self) {
        let now = chrono::Utc::now();
        let date = self.third_party().last_upgrade_date.mut_or_insert_default();
        date.set_day(now.day().try_into().unwrap());
        date.set_month(now.month().try_into().unwrap());
        date.set_year(now.year());
    }
    /// Updates the METADATA based on the crate name, version, description, and license.
    /// Also migrates and fixes up deprecated fields.
    pub fn update<V: Into<String>>(
        &mut self,
        name: impl AsRef<str>,
        version: V,
        description: impl AsRef<str>,
        license_type: LicenseType,
    ) {
        self.migrate_homepage();
        self.migrate_archive();
        self.remove_deprecated_url();

        let name = name.as_ref();
        self.third_party().set_homepage(crates_io_homepage(name));
        self.metadata.set_name(name.to_string());
        let description = description.as_ref();
        self.metadata.set_description(
            description.trim().replace("\n", " ").replace("“", "\"").replace("”", "\""),
        );
        self.third_party().set_homepage(crates_io_homepage(name));
        self.third_party().set_license_type(license_type);

        let version = version.into();
        if self.third_party().version() != version {
            self.set_date_to_today();
        }
        self.third_party().set_version(version.clone());

        let mut identifier = Identifier::new();
        identifier.set_type("Archive".to_string());
        identifier.set_value(crate_archive_url(name, &version));
        identifier.set_version(version);
        self.third_party().identifier.clear();
        self.third_party().identifier.push(identifier);
    }
    /// Migrate homepage from an identifier to its own field.
    fn migrate_homepage(&mut self) -> bool {
        let mut homepage = None;
        for (idx, identifier) in self.metadata.third_party.identifier.iter().enumerate() {
            if identifier.type_.as_ref().unwrap_or(&String::new()).to_lowercase() == "homepage" {
                match homepage {
                    Some(info) => panic!("Homepage set twice? {info:?} {identifier:?}"),
                    None => homepage = Some((idx, identifier.clone())),
                }
            }
        }
        let Some(homepage) = homepage else { return false };
        self.third_party().identifier.remove(homepage.0);
        self.third_party().homepage = homepage.1.value;
        true
    }
    /// Normalize case of 'Archive' identifiers.
    fn migrate_archive(&mut self) -> bool {
        let mut updated = false;
        for identifier in self.third_party().identifier.iter_mut() {
            if identifier.type_ == Some("ARCHIVE".to_string()) {
                identifier.type_ = Some("Archive".to_string());
                updated = true;
            }
        }
        updated
    }
    /// Remove deprecate URL fields.
    fn remove_deprecated_url(&mut self) -> bool {
        let updated = !self.metadata.third_party.url.is_empty();
        self.third_party().url.clear();
        updated
    }
}

fn crate_archive_url(name: impl AsRef<str>, version: impl AsRef<str>) -> String {
    format!(
        "https://static.crates.io/crates/{}/{}-{}.crate",
        name.as_ref(),
        name.as_ref(),
        version.as_ref()
    )
}
fn crates_io_homepage(name: impl AsRef<str>) -> String {
    format!("https://crates.io/crates/{}", name.as_ref())
}
