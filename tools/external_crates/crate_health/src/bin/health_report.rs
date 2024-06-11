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

use std::path::PathBuf;

use anyhow::Result;
use clap::Parser;
use crate_health::{
    default_output_dir, default_repo_root, maybe_build_cargo_embargo, CrateCollection,
    NameAndVersionMap, ReportEngine,
};

/// Generate a health report for crates in external/rust/crates
#[derive(Parser, Debug)]
#[command(about, long_about = None)]
struct Args {
    /// Path to the AOSP repo. Defaults to current working directory.
    #[arg(long, default_value_os_t=default_repo_root().unwrap_or(PathBuf::from(".")))]
    repo_root: PathBuf,

    /// Path the health report will be written to.
    #[arg(long, default_value_os_t=default_output_dir("crate-health-report.html"))]
    output_path: PathBuf,
}

fn main() -> Result<()> {
    let args = Args::parse();

    maybe_build_cargo_embargo(&args.repo_root, false)?;

    let mut cc = CrateCollection::new(args.repo_root);
    cc.add_from(&"external/rust/crates")?;
    cc.map_field_mut().retain(|_nv, krate| krate.is_crates_io());

    cc.stage_crates()?;
    cc.generate_android_bps()?;
    cc.diff_android_bps()?;

    let re = ReportEngine::new()?;

    Ok(re.health_report(&cc, &args.output_path)?)
}
