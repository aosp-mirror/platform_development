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

use spdx::LicenseReq;
use std::collections::BTreeSet;

use crate::Error;

/// The result of parsing and evaluating an SPDX license expression.
#[derive(Debug, PartialEq)]
pub(crate) struct LicenseTerms {
    /// The license terms we are required to follow.
    pub required: BTreeSet<LicenseReq>,
    /// Additional terms mentioned in the license expression that we
    /// do not need to follow.
    pub not_required: BTreeSet<LicenseReq>,
}

impl LicenseTerms {
    pub fn try_from(
        expr: spdx::Expression,
        preferences: &Vec<spdx::Licensee>,
    ) -> Result<LicenseTerms, Error> {
        let required = BTreeSet::from_iter(expr.minimized_requirements(preferences)?);
        let not_required = expr
            .requirements()
            .filter_map(
                |req| if !required.contains(&req.req) { Some(req.req.clone()) } else { None },
            )
            .collect::<BTreeSet<_>>();
        Ok(LicenseTerms { required, not_required })
    }
}

#[cfg(test)]
mod tests {
    use spdx::Licensee;

    use super::*;

    #[test]
    fn try_from() {
        assert_eq!(
            LicenseTerms::try_from(
                spdx::Expression::parse("Apache-2.0").unwrap(),
                &vec![spdx::Licensee::parse("Apache-2.0").unwrap()]
            )
            .unwrap(),
            LicenseTerms {
                required: BTreeSet::from([Licensee::parse("Apache-2.0").unwrap().into_req()]),
                not_required: BTreeSet::new()
            },
            "Simple case"
        );
        assert_eq!(
            LicenseTerms::try_from(
                spdx::Expression::parse("Apache-2.0 OR MIT").unwrap(),
                &vec![spdx::Licensee::parse("Apache-2.0").unwrap()]
            )
            .unwrap(),
            LicenseTerms {
                required: BTreeSet::from([Licensee::parse("Apache-2.0").unwrap().into_req()]),
                not_required: BTreeSet::from([Licensee::parse("MIT").unwrap().into_req()])
            },
            "Preference"
        );
        assert!(
            matches!(
                LicenseTerms::try_from(
                    spdx::Expression::parse("Apache-2.0").unwrap(),
                    &vec![spdx::Licensee::parse("MIT").unwrap()]
                ),
                Err(Error::MinimizeError(_))
            ),
            "No acceptable licenses"
        );
        assert_eq!(
            LicenseTerms::try_from(
                spdx::Expression::parse("Apache-2.0 AND MIT").unwrap(),
                &vec![
                    spdx::Licensee::parse("Apache-2.0").unwrap(),
                    spdx::Licensee::parse("MIT").unwrap()
                ]
            )
            .unwrap(),
            LicenseTerms {
                required: BTreeSet::from([
                    Licensee::parse("Apache-2.0").unwrap().into_req(),
                    Licensee::parse("MIT").unwrap().into_req()
                ]),
                not_required: BTreeSet::new(),
            },
            "AND"
        );
        assert_eq!(
            LicenseTerms::try_from(
                spdx::Expression::parse("MIT AND (MIT OR Apache-2.0)").unwrap(),
                &vec![
                    spdx::Licensee::parse("Apache-2.0").unwrap(),
                    spdx::Licensee::parse("MIT").unwrap()
                ]
            )
            .unwrap(),
            LicenseTerms {
                required: BTreeSet::from([Licensee::parse("MIT").unwrap().into_req()]),
                not_required: BTreeSet::from([Licensee::parse("Apache-2.0").unwrap().into_req(),]),
            },
            "Complex expression from libm 0.2.11"
        );
    }
}
