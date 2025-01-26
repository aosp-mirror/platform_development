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

use std::{
    collections::BTreeMap,
    fs::{read_to_string, write},
    path::Path,
};

use anyhow::Result;
use chrono::{DateTime, Days, Local};
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Default)]
pub struct UpdatesTried {
    attempts: BTreeMap<String, CrateUpdate>,
}

#[derive(Serialize, Deserialize)]
struct CrateUpdate {
    name: String,
    version: String,
    time: DateTime<Local>,
    success: bool,
}

impl UpdatesTried {
    /// Read updates-tried.json and prune old entries.
    // TODO: Put this somewhere better than $CWD.
    pub fn read() -> Result<Self> {
        let mut parsed: UpdatesTried = if Path::new("updates-tried.json").exists() {
            serde_json::from_str(read_to_string("updates-tried.json")?.as_str())?
        } else {
            UpdatesTried::default()
        };
        let now = chrono::Local::now();
        parsed.attempts.retain(|_, u| u.time.checked_add_days(Days::new(14)).unwrap_or(now) > now);
        Ok(parsed)
    }
    pub fn write(&self) -> Result<()> {
        let mut contents = serde_json::to_string_pretty(self)?;
        contents.push('\n');
        write("updates-tried.json", contents)?;
        Ok(())
    }
    pub fn contains(&self, name: &str, version: &str) -> bool {
        self.attempts.contains_key(&key(name, version))
    }
    pub fn record(&mut self, name: String, version: String, success: bool) -> Result<()> {
        self.attempts.insert(
            key(&name, &version),
            CrateUpdate { name, version, time: chrono::Local::now(), success },
        );
        self.write()
    }
}

fn key(name: &str, version: &str) -> String {
    format!("{name}-v{version}")
}
