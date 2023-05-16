// Copyright (C) 2022 The Android Open Source Project
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

use anyhow::Result;
use std::collections::BTreeMap;

/// Build module.
#[derive(Clone, PartialEq, Eq, PartialOrd, Ord)]
pub struct BpModule {
    module_type: String,
    pub props: BpProperties,
}

/// Properties of a build module, or of a nested object value.
#[derive(Clone, PartialEq, Eq, PartialOrd, Ord)]
pub struct BpProperties {
    map: BTreeMap<String, BpValue>,
    /// A raw block of text to append after the last key-value pair, but before the closing brace.
    /// For example, if you have the properties
    ///
    ///     {
    ///         name: "foo",
    ///         srcs: ["main.rs"],
    ///     }
    ///
    /// and add `raw_block = "some random text"`, you'll get
    ///
    ///     {
    ///         name: "foo",
    ///         srcs: ["main.rs"],
    ///         some random text
    ///     }
    pub raw_block: Option<String>,
}

#[derive(Clone, PartialEq, Eq, PartialOrd, Ord)]
pub enum BpValue {
    Object(BpProperties),
    Bool(bool),
    String(String),
    List(Vec<BpValue>),
}

impl BpModule {
    pub fn new(module_type: String) -> BpModule {
        BpModule { module_type, props: BpProperties::new() }
    }

    /// Serialize to Android.bp format.
    pub fn write(&self, w: &mut impl std::fmt::Write) -> Result<()> {
        w.write_str(&self.module_type)?;
        w.write_str(" ")?;
        self.props.write(w)?;
        w.write_str("\n")?;
        Ok(())
    }
}

impl BpProperties {
    pub fn new() -> Self {
        BpProperties { map: BTreeMap::new(), raw_block: None }
    }

    pub fn get_string(&self, k: &str) -> &str {
        match self.map.get(k).unwrap() {
            BpValue::String(s) => s,
            _ => unreachable!(),
        }
    }

    pub fn set<T: Into<BpValue>>(&mut self, k: &str, v: T) {
        self.map.insert(k.to_string(), v.into());
    }

    pub fn object(&mut self, k: &str) -> &mut BpProperties {
        let v =
            self.map.entry(k.to_string()).or_insert_with(|| BpValue::Object(BpProperties::new()));
        match v {
            BpValue::Object(v) => v,
            _ => panic!("key {k:?} already has non-object value"),
        }
    }

    /// Serialize to Android.bp format.
    pub fn write(&self, w: &mut impl std::fmt::Write) -> Result<()> {
        w.write_str("{\n")?;
        // Sort stuff to match what cargo2android.py's output order.
        let canonical_order = &[
            "name",
            "defaults",
            "stem",
            "host_supported",
            "prefer_rlib",
            "crate_name",
            "cargo_env_compat",
            "cargo_pkg_version",
            "srcs",
            "test_suites",
            "auto_gen_config",
            "test_options",
            "edition",
            "features",
            "rustlibs",
            "proc_macros",
            "static_libs",
            "shared_libs",
            "arch",
            "target",
            "ld_flags",
            "apex_available",
            "visibility",
        ];
        let mut props: Vec<(&String, &BpValue)> = self.map.iter().collect();
        props.sort_by_key(|(k, _)| {
            let i = canonical_order.iter().position(|x| k == x).unwrap_or(canonical_order.len());
            (i, (*k).clone())
        });
        for (k, v) in props {
            w.write_str(k)?;
            w.write_str(": ")?;
            v.write(w)?;
            w.write_str(",\n")?;
        }
        if let Some(raw_block) = &self.raw_block {
            w.write_str(raw_block)?;
            w.write_str(",\n")?;
        }
        w.write_str("}")?;
        Ok(())
    }
}

impl BpValue {
    /// Serialize to Android.bp format.
    pub fn write(&self, w: &mut impl std::fmt::Write) -> Result<()> {
        match self {
            BpValue::Object(p) => p.write(w)?,
            BpValue::Bool(b) => write!(w, "{b}")?,
            BpValue::String(s) => write!(w, "\"{s}\"")?,
            BpValue::List(vs) => {
                w.write_str("[")?;
                for (i, v) in vs.iter().enumerate() {
                    v.write(w)?;
                    if i != vs.len() - 1 {
                        w.write_str(", ")?;
                    }
                }
                w.write_str("]")?;
            }
        }
        Ok(())
    }
}

impl From<bool> for BpValue {
    fn from(x: bool) -> Self {
        BpValue::Bool(x)
    }
}

impl From<&str> for BpValue {
    fn from(x: &str) -> Self {
        BpValue::String(x.to_string())
    }
}

impl From<String> for BpValue {
    fn from(x: String) -> Self {
        BpValue::String(x)
    }
}

impl<T: Into<BpValue>> From<Vec<T>> for BpValue {
    fn from(x: Vec<T>) -> Self {
        BpValue::List(x.into_iter().map(|x| x.into()).collect())
    }
}
