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
    collections::BTreeMap,
    fs::{remove_file, write},
    path::Path,
    sync::LazyLock,
};

use anyhow::{anyhow, Result};
use glob::glob;
use google_metadata::metadata::LicenseType;
use license_checker::LicenseState;
use spdx::{LicenseReq, Licensee};

/// Update MODULE_LICENSE_* files in a directory based on the applicable licenses.
/// These files are typically empty, and their name indicates the type of license that
/// applies to the code, for example MODULE_LICENSE_APACHE2.
pub fn update_module_license_files(path: &impl AsRef<Path>, licenses: &LicenseState) -> Result<()> {
    let path = path.as_ref();
    for old_module_license_file in glob(
        path.join("MODULE_LICENSE*").to_str().ok_or(anyhow!("Failed to convert path to string"))?,
    )? {
        remove_file(old_module_license_file?)?;
    }
    for license in licenses.satisfied.keys().chain(&licenses.unsatisfied) {
        if let Some(mod_lic) = MODULE_LICENSE_FILES.get(license) {
            write(path.join(mod_lic), "")?; // Write an empty file. Essentially "touch".
        }
    }
    Ok(())
}

fn discriminant(lt: LicenseType) -> u8 {
    // Smaller --> more restricted
    // Larger --> less restricted
    match lt {
        LicenseType::UNKNOWN => 0,
        LicenseType::BY_EXCEPTION_ONLY => 1,
        LicenseType::RESTRICTED => 2,
        LicenseType::RESTRICTED_IF_STATICALLY_LINKED => 3,
        LicenseType::RECIPROCAL => 4,
        LicenseType::NOTICE => 5,
        LicenseType::PERMISSIVE => 6,
        LicenseType::UNENCUMBERED => 7,
    }
}

pub fn most_restrictive_type(licenses: &LicenseState) -> LicenseType {
    licenses
        .satisfied
        .keys()
        .chain(&licenses.unsatisfied)
        .map(|req| LICENSE_TYPES.get(req).cloned().unwrap_or(LicenseType::UNKNOWN))
        .min_by(|a, b| discriminant(*a).cmp(&discriminant(*b)))
        .unwrap_or(LicenseType::UNKNOWN)
}

static MODULE_LICENSE_FILES: LazyLock<BTreeMap<LicenseReq, &'static str>> = LazyLock::new(|| {
    vec![
        ("Apache-2.0", "MODULE_LICENSE_APACHE2"),
        ("MIT", "MODULE_LICENSE_MIT"),
        ("BSD-3-Clause", "MODULE_LICENSE_BSD"),
        ("BSD-2-Clause", "MODULE_LICENSE_BSD"),
        ("ISC", "MODULE_LICENSE_ISC"),
        ("MPL-2.0", "MODULE_LICENSE_MPL"),
        ("0BSD", "MODULE_LICENSE_PERMISSIVE"),
        ("Unlicense", "MODULE_LICENSE_PERMISSIVE"),
        ("Zlib", "MODULE_LICENSE_ZLIB"),
        ("Unicode-DFS-2016", "MODULE_LICENSE_UNICODE"),
        ("NCSA", "MODULE_LICENSE_NCSA"),
        ("OpenSSL", "MODULE_LICENSE_OPENSSL"),
    ]
    .into_iter()
    .map(|l| (Licensee::parse(l.0).unwrap().into_req(), l.1))
    .collect()
});
static LICENSE_TYPES: LazyLock<BTreeMap<LicenseReq, LicenseType>> = LazyLock::new(|| {
    vec![
        ("Apache-2.0", LicenseType::NOTICE),
        ("MIT", LicenseType::NOTICE),
        ("BSD-3-Clause", LicenseType::NOTICE),
        ("BSD-2-Clause", LicenseType::NOTICE),
        ("ISC", LicenseType::NOTICE),
        ("MPL-2.0", LicenseType::RECIPROCAL),
        ("0BSD", LicenseType::PERMISSIVE),
        ("Unlicense", LicenseType::PERMISSIVE),
        ("Zlib", LicenseType::NOTICE),
        ("Unicode-DFS-2016", LicenseType::NOTICE),
        ("NCSA", LicenseType::NOTICE),
        ("OpenSSL", LicenseType::NOTICE),
    ]
    .into_iter()
    .map(|l| (Licensee::parse(l.0).unwrap().into_req(), l.1))
    .collect()
});
