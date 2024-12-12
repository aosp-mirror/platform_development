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

// Use to capture more test cases from crates.io.

use std::env;

use crates_io_util::{CratesIoIndex, Error};

fn main() -> Result<(), Error> {
    let args = env::args().collect::<Vec<_>>();
    let krate = args[1].as_str();
    let version = args[2].as_str();

    let fetcher = CratesIoIndex::new()?;
    let cio_crate = fetcher.get_crate(krate)?;
    let version =
        cio_crate.versions().iter().find(|v| v.version() == version).expect("Version not found");

    println!("{}", serde_json::to_string_pretty(version).expect("Failed to serialize JSON"));

    Ok(())
}
