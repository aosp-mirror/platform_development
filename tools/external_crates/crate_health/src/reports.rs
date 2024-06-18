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

use std::{fmt::Display, fs::write, path::Path, str::from_utf8};

use crate::{
    crates_with_multiple_versions, crates_with_single_version, CompatibleVersionPair, Crate,
    CrateCollection, NameAndVersionMap, NamedAndVersioned, VersionMatch, VersionPair,
};

use anyhow::Result;
use serde::Serialize;
use tinytemplate::TinyTemplate;

static SIZE_REPORT_TEMPLATE: &'static str = include_str!("templates/size_report.html.template");
static TABLE_TEMPLATE: &'static str = include_str!("templates/table.html.template");

static CRATE_HEALTH_REPORT_TEMPLATE: &'static str =
    include_str!("templates/crate_health.html.template");
static MIGRATION_REPORT_TEMPLATE: &'static str =
    include_str!("templates/migration_report.html.template");

pub struct ReportEngine<'template> {
    tt: TinyTemplate<'template>,
}

fn len_formatter(value: &serde_json::Value, out: &mut String) -> tinytemplate::error::Result<()> {
    match value {
        serde_json::Value::Array(a) => {
            out.push_str(&format!("{}", a.len()));
            Ok(())
        }
        _ => Err(tinytemplate::error::Error::GenericError {
            msg: "Can only use length formatter on an array".to_string(),
        }),
    }
}

fn linkify(text: &dyn Display, url: &dyn Display) -> String {
    format!("<a href=\"{}\">{}</a>", url, text)
}

impl<'template> ReportEngine<'template> {
    pub fn new() -> Result<ReportEngine<'template>> {
        let mut tt = TinyTemplate::new();
        tt.add_template("size_report", SIZE_REPORT_TEMPLATE)?;
        tt.add_template("table", TABLE_TEMPLATE)?;
        tt.add_template("crate_health", CRATE_HEALTH_REPORT_TEMPLATE)?;
        tt.add_template("migration", MIGRATION_REPORT_TEMPLATE)?;
        tt.add_formatter("len", len_formatter);
        Ok(ReportEngine { tt })
    }
    pub fn size_report(&self, cc: &CrateCollection) -> Result<String> {
        let num_crates = cc.num_crates();
        let crates_with_single_version = cc.filter_versions(&crates_with_single_version).count();
        Ok(self.tt.render(
            "size_report",
            &SizeReport {
                num_crates,
                crates_with_single_version,
                crates_with_multiple_versions: num_crates - crates_with_single_version,
                num_dirs: cc.map_field().len(),
            },
        )?)
    }
    pub fn table<'a>(&self, crates: impl Iterator<Item = &'a Crate>) -> Result<String> {
        let mut table = Table::new(&[&"Crate", &"Version", &"Path"]);
        for krate in crates {
            table.add_row(&[
                &linkify(&krate.name(), &krate.crates_io_url()),
                &krate.version().to_string(),
                &krate
                    .aosp_url()
                    .map_or(format!("{}", krate.path()), |url| linkify(&krate.path(), &url)),
            ]);
        }
        Ok(self.tt.render("table", &table)?)
    }
    pub fn health_table<'a>(&self, crates: impl Iterator<Item = &'a Crate>) -> Result<String> {
        let mut table = Table::new(&[
            &"Crate",
            &"Version",
            &"Path",
            &"Has Android.bp",
            &"Generate Android.bp succeeds",
            &"Android.bp unchanged",
            &"Has cargo_embargo.json",
            &"On migration denylist",
        ]);
        table.set_vertical_headers();
        for krate in crates {
            table.add_row(&[
                &linkify(&krate.name(), &krate.crates_io_url()),
                &krate.version().to_string(),
                &krate
                    .aosp_url()
                    .map_or(format!("{}", krate.path()), |url| linkify(&krate.path(), &url)),
                &prefer_yes(krate.android_bp().abs().exists()),
                &prefer_yes_or_summarize(
                    krate.generate_android_bp_success(),
                    krate
                        .generate_android_bp_output()
                        .map_or("Error".to_string(), |o| {
                            format!(
                                "STDOUT:\n{}\n\nSTDERR:\n{}",
                                from_utf8(&o.stdout).unwrap_or("Error"),
                                from_utf8(&o.stderr).unwrap_or("Error")
                            )
                        })
                        .as_str(),
                ),
                &prefer_yes_or_summarize(
                    krate.android_bp_unchanged(),
                    krate
                        .android_bp_diff()
                        .map_or("Error", |o| from_utf8(&o.stdout).unwrap_or("Error")),
                ),
                &prefer_yes(krate.cargo_embargo_json().abs().exists()),
                &prefer_no(krate.is_migration_denied()),
            ]);
        }
        Ok(self.tt.render("table", &table)?)
    }
    pub fn migratable_table<'a>(
        &self,
        crate_pairs: impl Iterator<Item = CompatibleVersionPair<'a, Crate>>,
    ) -> Result<String> {
        let mut table = Table::new(&[&"Crate", &"Old Version", &"New Version", &"Path"]);
        for crate_pair in crate_pairs {
            let source = crate_pair.source;
            let dest = crate_pair.dest;
            let dest_version = if source.version() == dest.version() {
                "".to_string()
            } else {
                dest.version().to_string()
            };
            table.add_row(&[
                &linkify(&source.name(), &source.crates_io_url()),
                &source.version().to_string(),
                &dest_version,
                &source
                    .aosp_url()
                    .map_or(format!("{}", source.path()), |url| linkify(&source.path(), &url)),
            ]);
        }
        Ok(self.tt.render("table", &table)?)
    }
    pub fn migration_ineligible_table<'a>(
        &self,
        crates: impl Iterator<Item = &'a Crate>,
    ) -> Result<String> {
        let mut table = Table::new(&[
            &"Crate",
            &"Version",
            &"Path",
            &"In crates.io",
            &"Denylisted",
            &"Has Android.bp",
            &"Has cargo_embargo.json",
        ]);
        table.set_vertical_headers();
        for krate in crates {
            table.add_row(&[
                &linkify(&krate.name(), &krate.crates_io_url()),
                &krate.version().to_string(),
                &krate
                    .aosp_url()
                    .map_or(format!("{}", krate.path()), |url| linkify(&krate.path(), &url)),
                &prefer_yes(krate.is_crates_io()),
                &prefer_no(krate.is_migration_denied()),
                &prefer_yes(krate.android_bp().abs().exists()),
                &prefer_yes(krate.cargo_embargo_json().abs().exists()),
            ]);
        }
        Ok(self.tt.render("table", &table)?)
    }
    pub fn migration_eligible_table<'a>(
        &self,
        crate_pairs: impl Iterator<Item = VersionPair<'a, Crate>>,
    ) -> Result<String> {
        let mut table = Table::new(&[
            &"Crate",
            &"Version",
            &"Path",
            &"Compatible version",
            &"Patch succeeds",
            &"Generate Android.bp succeeds",
            &"Android.bp unchanged",
        ]);
        table.set_vertical_headers();
        for crate_pair in crate_pairs {
            let source = crate_pair.source;
            let maybe_dest = crate_pair.dest;
            table.add_row(&[
                &linkify(&source.name(), &source.crates_io_url()),
                &source.version().to_string(),
                &source
                    .aosp_url()
                    .map_or(format!("{}", source.path()), |url| linkify(&source.path(), &url)),
                maybe_dest.map_or(&"None", |dest| {
                    if dest.version() != source.version() {
                        dest.version()
                    } else {
                        &""
                    }
                }),
                &prefer_yes(!maybe_dest.is_some_and(|dest| !dest.patch_success())),
                &prefer_yes_or_summarize(
                    !maybe_dest.is_some_and(|dest| !dest.generate_android_bp_success()),
                    maybe_dest
                        .map_or("Error".to_string(), |dest| {
                            dest.generate_android_bp_output().map_or("Error".to_string(), |o| {
                                format!(
                                    "STDOUT:\n{}\n\nSTDERR:\n{}",
                                    from_utf8(&o.stdout).unwrap_or("Error"),
                                    from_utf8(&o.stderr).unwrap_or("Error")
                                )
                            })
                        })
                        .as_str(),
                ),
                &prefer_yes_or_summarize(
                    !maybe_dest.is_some_and(|dest| !dest.android_bp_unchanged()),
                    maybe_dest.map_or("Error", |dest| {
                        dest.android_bp_diff()
                            .map_or("Error", |o| from_utf8(&o.stdout).unwrap_or("Error"))
                    }),
                ),
            ]);
        }
        Ok(self.tt.render("table", &table)?)
    }
    pub fn health_report(
        &self,
        cc: &CrateCollection,
        output_path: &impl AsRef<Path>,
    ) -> Result<()> {
        let chr = CrateHealthReport {
            crate_count: self.size_report(cc)?,
            crate_multiversion: self.table(
                cc.filter_versions(&crates_with_multiple_versions).map(|(_nv, krate)| krate),
            )?,
            healthy: self.table(
                cc.map_field()
                    .iter()
                    .filter(|(_nv, krate)| krate.is_android_bp_healthy())
                    .map(|(_nv, krate)| krate),
            )?,
            unhealthy: self.health_table(
                cc.map_field()
                    .iter()
                    .filter(|(_nv, krate)| !krate.is_android_bp_healthy())
                    .map(|(_nv, krate)| krate),
            )?,
        };
        Ok(write(output_path, self.tt.render("crate_health", &chr)?)?)
    }
    pub fn migration_report(
        &self,
        m: &VersionMatch<CrateCollection>,
        output_path: &impl AsRef<Path>,
    ) -> Result<()> {
        let mr = MigrationReport {
            migratable: self.migratable_table(m.migratable())?,
            eligible: self.migration_eligible_table(m.eligible_but_not_migratable())?,
            ineligible: self.migration_ineligible_table(m.ineligible())?,
            superfluous: self.table(m.superfluous().map(|(_nv, krate)| krate))?,
        };
        Ok(write(output_path, self.tt.render("migration", &mr)?)?)
    }
}

pub fn prefer_yes(p: bool) -> &'static str {
    if p {
        ""
    } else {
        "No"
    }
}
pub fn prefer_yes_or_summarize(p: bool, details: &str) -> String {
    if p {
        "".to_string()
    } else {
        format!("<details><summary>No</summary><pre>{}</pre></details>", details)
    }
}
pub fn prefer_no(p: bool) -> &'static str {
    if p {
        "Yes"
    } else {
        ""
    }
}

#[derive(Serialize)]
pub struct SizeReport {
    num_crates: usize,
    crates_with_single_version: usize,
    crates_with_multiple_versions: usize,
    num_dirs: usize,
}

#[derive(Serialize)]
pub struct Table {
    header: Vec<String>,
    rows: Vec<Vec<String>>,
    vertical: bool,
}

impl Table {
    pub fn new(header: &[&dyn Display]) -> Table {
        Table {
            header: header.iter().map(|cell| format!("{}", cell)).collect::<Vec<_>>(),
            rows: Vec::new(),
            vertical: false,
        }
    }
    pub fn add_row(&mut self, row: &[&dyn Display]) {
        self.rows.push(row.iter().map(|cell| format!("{}", cell)).collect::<Vec<_>>());
    }
    pub fn set_vertical_headers(&mut self) {
        self.vertical = true;
    }
}

#[derive(Serialize)]
pub struct CrateHealthReport {
    crate_count: String,
    crate_multiversion: String,
    healthy: String,
    unhealthy: String,
}
#[derive(Serialize)]
pub struct MigrationReport {
    migratable: String,
    eligible: String,
    ineligible: String,
    superfluous: String,
}
