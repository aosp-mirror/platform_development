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

use crates_index::{Crate, SparseIndex};
use std::{cell::RefCell, collections::HashSet, process::Command};
use success_or_error::RunAndExpectSuccess;

use crate::Error;

/// A local index of crates.io data, with a way of updating the index
/// by fetching data from crates.io.
pub struct CratesIoIndex {
    fetcher: Box<dyn CratesIoFetcher>,
}

impl CratesIoIndex {
    /// Constructs a CratesIoIndex that uses the `ureq` crate
    /// to fetch data from crates.io via https.
    #[cfg(feature = "ureq")]
    pub fn new_ureq() -> Result<CratesIoIndex, Error> {
        Ok(CratesIoIndex {
            fetcher: Box::new(UreqFetcher {
                index: crates_index::SparseIndex::new_cargo_default()?,
                agent: ureq::Agent::new_with_defaults(),
                fetched: RefCell::new(HashSet::new()),
            }),
        })
    }
    /// Constructs a CratesIoIndex in offline mode. No new data is
    /// fetched from crates.io, but data already in the local cache
    /// is returned.
    pub fn new_offline() -> Result<CratesIoIndex, Error> {
        Ok(CratesIoIndex {
            fetcher: Box::new(OfflineFetcher {
                index: crates_index::SparseIndex::new_cargo_default()?,
            }),
        })
    }
    /// Constructs a CratesIoIndex that uses cargo commands to fetch data
    /// from crates.io. Less efficient than making direct https requests,
    /// but avoids complicated dependencies on cryptographic crates.
    pub fn new_cargo() -> Result<CratesIoIndex, Error> {
        Ok(CratesIoIndex {
            fetcher: Box::new(CargoFetcher {
                index: crates_index::SparseIndex::new_cargo_default()?,
                fetched: RefCell::new(HashSet::new()),
            }),
        })
    }
    /// Fetches and returns the crates.io data on a crate.
    pub fn get_crate(&self, crate_name: impl AsRef<str>) -> Result<Crate, Error> {
        self.fetcher.fetch(crate_name.as_ref())
    }
}

pub trait CratesIoFetcher {
    fn fetch(&self, crate_name: &str) -> Result<Crate, Error>;
}

#[cfg(feature = "ureq")]
struct UreqFetcher {
    index: SparseIndex,
    agent: ureq::Agent,
    // Keep track of crates we have fetched, to avoid fetching them multiple times.
    fetched: RefCell<HashSet<String>>,
}

#[cfg(feature = "ureq")]
impl CratesIoFetcher for UreqFetcher {
    fn fetch(&self, crate_name: &str) -> Result<Crate, Error> {
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
        let response = crates_index::http::Response::from_parts(parts, body.read_to_vec()?);
        let response = self
            .index
            .parse_cache_response(crate_name, response, true)?
            .ok_or(Error::CrateNotFound(crate_name.to_string()))?;

        fetched.insert(crate_name.to_string());
        Ok(response)
    }
}

struct OfflineFetcher {
    index: SparseIndex,
}

impl CratesIoFetcher for OfflineFetcher {
    fn fetch(&self, crate_name: &str) -> Result<Crate, Error> {
        Ok(self.index.crate_from_cache(crate_name.as_ref())?)
    }
}

// A fetcher that uses cargo commands to update the index. This
// lets us avoid complicated dependencies on crates that implement
// https.
struct CargoFetcher {
    index: SparseIndex,
    // Keep track of crates we have fetched, to avoid fetching them multiple times.
    fetched: RefCell<HashSet<String>>,
}

impl CratesIoFetcher for CargoFetcher {
    fn fetch(&self, crate_name: &str) -> Result<Crate, Error> {
        let mut fetched = self.fetched.borrow_mut();
        if !fetched.contains(crate_name) {
            let tempdir = tempfile::tempdir()?;
            Command::new("cargo")
                .args(["init", "--lib", "--name", "fake-crate"])
                .current_dir(&tempdir)
                .run_quiet_and_expect_success()?;
            Command::new("cargo")
                .args(["add", crate_name])
                .current_dir(&tempdir)
                .run_quiet_and_expect_success()?;
            Command::new("cargo")
                .args(["update", crate_name])
                .current_dir(&tempdir)
                .run_quiet_and_expect_success()?;
            fetched.insert(crate_name.to_string());
        }

        Ok(self.index.crate_from_cache(crate_name.as_ref())?)
    }
}
