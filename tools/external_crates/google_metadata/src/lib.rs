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
use protobuf::text_format::ParseError;

#[cfg(soong)]
mod metadata_proto {
    pub use google_metadata_proto::metadata::Identifier;
    pub use google_metadata_proto::metadata::LicenseType;
    pub use google_metadata_proto::metadata::MetaData;
}

#[cfg(not(soong))]
mod metadata_proto {
    include!(concat!(env!("OUT_DIR"), "/protos/mod.rs"));
    pub use crate::metadata::Identifier;
    pub use crate::metadata::LicenseType;
    pub use crate::metadata::MetaData;
}

pub use metadata_proto::*;

#[allow(missing_docs)]
#[derive(thiserror::Error, Debug)]
pub enum Error {
    #[error("File exists: {}", .0.display())]
    FileExists(PathBuf),
    #[error("Crate name not set")]
    CrateNameMissing(),
    #[error("Crate names don't match: {} in METADATA vs {}", .0, .1)]
    CrateNameMismatch(String, String),
    #[error("Glob pattern error")]
    ParseError(#[from] ParseError),
    #[error("Write error")]
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
    pub fn init<P: Into<PathBuf>, Q: Into<String>, R: Into<String>, S: Into<String>>(
        path: P,
        name: Q,
        version: R,
        desc: S,
        license_type: LicenseType,
    ) -> Result<Self, Error> {
        let path = path.into();
        if path.exists() {
            return Err(Error::FileExists(path));
        }
        let mut metadata = GoogleMetadata { path, metadata: MetaData::new() };
        let name = name.into();
        metadata.set_date_to_today()?;
        metadata.set_version_and_urls(&name, version)?;
        let third_party = metadata.metadata.third_party.mut_or_insert_default();
        third_party.set_homepage(crates_io_homepage(&name));
        third_party.set_license_type(license_type);
        metadata.metadata.set_name(name);
        metadata.metadata.set_description(desc.into());
        Ok(metadata)
    }
    /// Writes to the METADATA file.
    ///
    /// The existing file is overwritten.
    pub fn write(&self) -> Result<(), Error> {
        Ok(write(&self.path, protobuf::text_format::print_to_string_pretty(&self.metadata))?)
    }
    /// Sets the date fields to today's date.
    pub fn set_date_to_today(&mut self) -> Result<(), Error> {
        let now = chrono::Utc::now();
        let date = self
            .metadata
            .third_party
            .mut_or_insert_default()
            .last_upgrade_date
            .mut_or_insert_default();
        date.set_day(now.day().try_into().unwrap());
        date.set_month(now.month().try_into().unwrap());
        date.set_year(now.year());
        Ok(())
    }
    /// Sets the version and URL fields.
    ///
    /// Sets third_party.homepage and third_party.version, and
    /// a single "Archive" identifier with crate archive URL and version.
    pub fn set_version_and_urls<Q: Into<String>>(
        &mut self,
        name: impl AsRef<str>,
        version: Q,
    ) -> Result<(), Error> {
        let name_in_metadata = self.metadata.name.as_ref().ok_or(Error::CrateNameMissing())?;
        if name_in_metadata != name.as_ref() {
            return Err(Error::CrateNameMismatch(
                name_in_metadata.clone(),
                name.as_ref().to_string(),
            ));
        }
        let third_party = self.metadata.third_party.mut_or_insert_default();
        third_party.set_homepage(crates_io_homepage(&name));
        let version = version.into();
        third_party.set_version(version.clone());
        let mut identifier = Identifier::new();
        identifier.set_type("Archive".to_string());
        identifier.set_value(crate_archive_url(name, &version));
        identifier.set_version(version);
        self.metadata.third_party.mut_or_insert_default().identifier.clear();
        self.metadata.third_party.mut_or_insert_default().identifier.push(identifier);
        Ok(())
    }
    /// Migrate homepage from an identifier to its own field.
    pub fn migrate_homepage(&mut self) -> bool {
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
        self.metadata.third_party.mut_or_insert_default().identifier.remove(homepage.0);
        self.metadata.third_party.mut_or_insert_default().homepage = homepage.1.value;
        true
    }
    /// Normalize case of 'Archive' identifiers.
    pub fn migrate_archive(&mut self) -> bool {
        let mut updated = false;
        for identifier in self.metadata.third_party.mut_or_insert_default().identifier.iter_mut() {
            if identifier.type_ == Some("ARCHIVE".to_string()) {
                identifier.type_ = Some("Archive".to_string());
                updated = true;
            }
        }
        updated
    }
    /// Remove deprecate URL fields.
    pub fn remove_deprecated_url(&mut self) -> bool {
        let updated = !self.metadata.third_party.url.is_empty();
        self.metadata.third_party.mut_or_insert_default().url.clear();
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
