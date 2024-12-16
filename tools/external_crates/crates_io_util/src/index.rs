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

use crates_index::{http, Crate, SparseIndex};
use reqwest::blocking::Client;
use std::{cell::RefCell, collections::HashSet};

use crate::Error;

pub struct CratesIoIndex {
    fetcher: Box<dyn CratesIoFetcher>,
}

impl CratesIoIndex {
    pub fn new() -> Result<CratesIoIndex, Error> {
        Ok(CratesIoIndex {
            fetcher: Box::new(OnlineFetcher {
                index: crates_index::SparseIndex::new_cargo_default()?,
                client: reqwest::blocking::ClientBuilder::new().gzip(true).build()?,
                fetched: RefCell::new(HashSet::new()),
            }),
        })
    }
    pub fn new_offline() -> Result<CratesIoIndex, Error> {
        Ok(CratesIoIndex {
            fetcher: Box::new(OfflineFetcher {
                index: crates_index::SparseIndex::new_cargo_default()?,
            }),
        })
    }
    pub fn get_crate(&self, crate_name: impl AsRef<str>) -> Result<Crate, Error> {
        self.fetcher.fetch(crate_name.as_ref())
    }
}

pub trait CratesIoFetcher {
    fn fetch(&self, crate_name: &str) -> Result<Crate, Error>;
}

struct OnlineFetcher {
    index: SparseIndex,
    client: Client,
    // Keep track of crates we have fetched, to avoid fetching them multiple times.
    fetched: RefCell<HashSet<String>>,
}

struct OfflineFetcher {
    index: SparseIndex,
}

impl CratesIoFetcher for OnlineFetcher {
    fn fetch(&self, crate_name: &str) -> Result<Crate, Error> {
        // Adapted from https://github.com/frewsxcv/rust-crates-index/blob/master/examples/sparse_http_reqwest.rs

        let mut fetched = self.fetched.borrow_mut();
        if fetched.contains(crate_name) {
            return Ok(self.index.crate_from_cache(crate_name.as_ref())?);
        }
        let req = self.index.make_cache_request(crate_name)?.body(())?;
        let req = http::Request::from_parts(req.into_parts().0, vec![]);
        let req: reqwest::blocking::Request = req.try_into()?;
        let res = self.client.execute(req)?;
        let mut builder = http::Response::builder().status(res.status()).version(res.version());
        builder
            .headers_mut()
            .ok_or(Error::HttpHeader)?
            .extend(res.headers().iter().map(|(k, v)| (k.clone(), v.clone())));
        let body = res.bytes()?;
        let res = builder.body(body.to_vec())?;
        let res = self
            .index
            .parse_cache_response(crate_name, res, true)?
            .ok_or(Error::CrateNotFound(crate_name.to_string()))?;
        fetched.insert(crate_name.to_string());
        Ok(res)
    }
}

impl CratesIoFetcher for OfflineFetcher {
    fn fetch(&self, crate_name: &str) -> Result<Crate, Error> {
        Ok(self.index.crate_from_cache(crate_name.as_ref())?)
    }
}
