// Copyright (C) 2023 The Android Open Source Project
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

//! Types and functions for parsing the output of cargo.

pub mod cargo_out;
pub mod metadata;

use serde::{Deserialize, Serialize};
use std::path::PathBuf;

/// Combined representation of --crate-type and --test flags.
#[derive(Copy, Clone, Debug, Deserialize, PartialEq, Eq, Serialize)]
#[serde[rename_all = "lowercase"]]
pub enum CrateType {
    // --crate-type types
    Bin,
    Lib,
    RLib,
    DyLib,
    CDyLib,
    StaticLib,
    #[serde(rename = "proc-macro")]
    ProcMacro,
    // --test
    Test,
    // "--cfg test" without --test. (Assume it is a test with the harness disabled.
    TestNoHarness,
}

impl CrateType {
    fn from_str(s: &str) -> CrateType {
        match s {
            "bin" => CrateType::Bin,
            "lib" => CrateType::Lib,
            "rlib" => CrateType::RLib,
            "dylib" => CrateType::DyLib,
            "cdylib" => CrateType::CDyLib,
            "staticlib" => CrateType::StaticLib,
            "proc-macro" => CrateType::ProcMacro,
            _ => panic!("unexpected --crate-type: {}", s),
        }
    }
}

impl CrateType {
    /// Returns whether the crate type is a kind of library.
    pub fn is_library(self) -> bool {
        matches!(self, Self::Lib | Self::RLib | Self::DyLib | Self::CDyLib | Self::StaticLib)
    }

    /// Returns whether the crate type is a kind of test.
    pub fn is_test(self) -> bool {
        matches!(self, Self::Test | Self::TestNoHarness)
    }

    /// Returns whether the crate type is a kind of C ABI library.
    pub fn is_c_library(self) -> bool {
        matches!(self, Self::CDyLib | Self::StaticLib)
    }
}

/// Info extracted from `CargoOut` for a crate.
///
/// Note that there is a 1-to-many relationship between a Cargo.toml file and these `Crate`
/// objects. For example, a Cargo.toml file might have a bin, a lib, and various tests. Each of
/// those will be a separate `Crate`. All of them will have the same `package_name`.
#[derive(Clone, Debug, Default, Deserialize, Eq, PartialEq, Serialize)]
pub struct Crate {
    pub name: String,
    pub package_name: String,
    pub version: Option<String>,
    pub types: Vec<CrateType>,
    pub target: Option<String>, // --target
    pub features: Vec<String>,  // --cfg feature=
    pub cfgs: Vec<String>,      // non-feature --cfg
    pub externs: Vec<Extern>,
    pub codegens: Vec<String>, // -C
    pub cap_lints: String,
    pub static_libs: Vec<String>,
    pub shared_libs: Vec<String>,
    pub edition: String,
    pub package_dir: PathBuf, // canonicalized
    pub main_src: PathBuf,    // relative to package_dir
    /// Whether it is a test crate which doesn't actually contain any tests or benchmarks.
    pub empty_test: bool,
}

/// A dependency of a Rust crate.
#[derive(Clone, Debug, Deserialize, Eq, Ord, PartialEq, PartialOrd, Serialize)]
pub struct Extern {
    pub name: String,
    pub lib_name: String,
    pub raw_name: String,
    pub extern_type: ExternType,
}

#[derive(Copy, Clone, Debug, Deserialize, Eq, Ord, PartialEq, PartialOrd, Serialize)]
pub enum ExternType {
    Rust,
    ProcMacro,
}
