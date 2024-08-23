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

use std::{
    fs::{read_to_string, write},
    path::PathBuf,
};

use anyhow::{anyhow, Result};
use chrono::{DateTime, Datelike, Local};

use crate::{
    metadata::{self, Identifier, MetaData},
    NamedAndVersioned,
};

pub struct GoogleMetadata {
    path: PathBuf,
    metadata: MetaData,
}

impl GoogleMetadata {
    pub fn try_from<P: Into<PathBuf>>(path: P) -> Result<Self> {
        let path = path.into();
        let metadata = read_to_string(&path)?;
        let metadata: metadata::MetaData = protobuf::text_format::parse_from_str(&metadata)?;
        Ok(GoogleMetadata { path, metadata })
    }
    pub fn init<P: Into<PathBuf>>(path: P, nv: &dyn NamedAndVersioned, desc: &str) -> Result<Self> {
        let path = path.into();
        if path.exists() {
            return Err(anyhow!("{} already exists", path.display()));
        }
        let mut metadata = GoogleMetadata { path, metadata: MetaData::new() };
        metadata.metadata.set_name(nv.name().to_string());
        metadata.metadata.set_description(desc.to_string());
        metadata.set_date_to_today()?;
        metadata.set_identifier(nv)?;
        let third_party = metadata.metadata.third_party.mut_or_insert_default();
        third_party.set_homepage(nv.crates_io_homepage());
        // TODO: handle license.
        Ok(metadata)
    }
    pub fn write(&self) -> Result<()> {
        Ok(write(&self.path, protobuf::text_format::print_to_string_pretty(&self.metadata))?)
    }
    pub fn set_date_to_today(&mut self) -> Result<()> {
        let now: DateTime<Local> = Local::now();
        let date = self
            .metadata
            .third_party
            .mut_or_insert_default()
            .last_upgrade_date
            .mut_or_insert_default();
        date.set_day(now.day().try_into()?);
        date.set_month(now.month().try_into()?);
        date.set_year(now.year());
        Ok(())
    }
    pub fn set_identifier(&mut self, nv: &dyn NamedAndVersioned) -> Result<()> {
        if self.metadata.name.as_ref().ok_or(anyhow!("No name"))? != nv.name() {
            return Err(anyhow!("Names don't match"));
        }
        let mut identifier = Identifier::new();
        identifier.set_type("Archive".to_string());
        identifier.set_value(nv.crate_archive_url());
        identifier.set_version(nv.version().to_string());
        self.metadata.third_party.mut_or_insert_default().identifier.clear();
        self.metadata.third_party.mut_or_insert_default().identifier.push(identifier);
        Ok(())
    }
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
}
