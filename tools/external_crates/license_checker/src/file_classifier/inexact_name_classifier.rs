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

use std::{collections::BTreeMap, ffi::OsString, path::Path, sync::LazyLock};

use super::normalize_filename;

#[derive(Debug, Clone, PartialEq)]
pub(crate) enum InexactLicenseType {
    BSD,
    UNICODE,
    LGPL,
}

pub(super) fn classify_inexact_license_file_name(
    file: impl AsRef<Path>,
) -> Option<InexactLicenseType> {
    if let Some(inexact_type) = INEXACT_LICENSE_FILE_NAMES.get(&normalize_filename(file)) {
        return Some(inexact_type.clone());
    }
    None
}

static INEXACT_LICENSE_FILE_NAMES: LazyLock<BTreeMap<OsString, InexactLicenseType>> =
    LazyLock::new(|| {
        vec![
            ("LICENSE-BSD", InexactLicenseType::BSD),
            ("LICENSE-UNICODE", InexactLicenseType::UNICODE),
            ("LICENSE-LGPL", InexactLicenseType::LGPL),
            ("LICENSE.LGPL-2.1", InexactLicenseType::LGPL),
        ]
        .into_iter()
        .map(|(file, inexact_type)| (OsString::from(file.to_uppercase()), inexact_type))
        .collect()
    });

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_classify() {
        assert!(
            classify_inexact_license_file_name(Path::new("LICENSE-BSD-2-Clause")).is_none(),
            "Specific license file name"
        );
        assert_eq!(
            classify_inexact_license_file_name(Path::new("LICENSE-BSD")),
            Some(InexactLicenseType::BSD),
            "Some kind of BSD license file"
        );
    }
}
