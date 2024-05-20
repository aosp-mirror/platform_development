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

use std::{
    fs::{create_dir, write},
    path::Path,
    process::Command,
    str::from_utf8,
};

use anyhow::{anyhow, Context, Result};
use serde::Serialize;
use tinytemplate::TinyTemplate;

use crate::{ensure_exists_and_empty, NamedAndVersioned};

static CARGO_TOML_TEMPLATE: &'static str = include_str!("templates/Cargo.toml.template");

#[derive(Serialize)]
struct Dep {
    name: String,
    version: String,
}

#[derive(Serialize)]
struct CargoToml {
    deps: Vec<Dep>,
}

pub fn write_pseudo_crate<'a>(
    dest_absolute: &impl AsRef<Path>,
    crates: impl Iterator<Item = &'a (impl NamedAndVersioned + 'a)>,
) -> Result<()> {
    let dest_absolute = dest_absolute.as_ref();
    ensure_exists_and_empty(&dest_absolute)?;

    let mut deps = Vec::new();
    for krate in crates {
        // Special cases:
        // * libsqlite3-sys is a sub-crate of rusqlite
        // * remove_dir_all has a version not known by crates.io (b/313489216)
        if krate.name() != "libsqlite3-sys" {
            deps.push(Dep {
                name: krate.name().to_string(),
                version: if krate.name() == "remove_dir_all"
                    && krate.version().to_string() == "0.7.1"
                {
                    "0.7.0".to_string()
                } else {
                    krate.version().to_string()
                },
            });
        }
    }

    let mut tt = TinyTemplate::new();
    tt.add_template("cargo_toml", CARGO_TOML_TEMPLATE)?;
    let cargo_toml = dest_absolute.join("Cargo.toml");
    write(&cargo_toml, tt.render("cargo_toml", &CargoToml { deps })?)?;

    create_dir(dest_absolute.join("src")).context("Failed to create src dir")?;
    write(dest_absolute.join("src/lib.rs"), "// Nothing").context("Failed to create src/lib.rs")?;

    let vendor_output = Command::new("cargo")
        .args(["vendor", "android/vendor"])
        .current_dir(dest_absolute)
        .output()?;
    if !vendor_output.status.success() {
        return Err(anyhow!(
            "cargo vendor failed with exit code {}\nstdout:\n{}\nstderr:\n{}",
            vendor_output.status,
            from_utf8(&vendor_output.stdout)?,
            from_utf8(&vendor_output.stderr)?
        ));
    }

    // TODO: Run "cargo deny"

    Ok(())
}
