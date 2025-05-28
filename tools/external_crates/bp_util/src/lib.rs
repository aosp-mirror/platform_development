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

//! Convenience methods for working with Android blueprint files.

use std::{
    collections::{BTreeSet, HashSet},
    sync::LazyLock,
};

use android_bp::{BluePrint, Value};

/// Error types for the 'bp_util' crate.
#[derive(thiserror::Error, Debug)]
pub enum Error {
    /// Blueprint rule has no name
    #[error("Blueprint rule has no name")]
    RuleWithoutName(String),
}

/// Extract rust test rules from a blueprint file.
pub trait RustTests {
    /// Returns the names of all rust_test and rust_test_host rules.
    fn rust_tests(&self) -> Result<BTreeSet<&str>, Error>;
}

impl RustTests for BluePrint {
    fn rust_tests(&self) -> Result<BTreeSet<&str>, Error> {
        let mut tests = BTreeSet::new();
        for module in &self.modules {
            if matches!(module.typ.as_str(), "rust_test" | "rust_test_host") {
                let name = module
                    .get_string("name")
                    .ok_or(Error::RuleWithoutName(module.typ.clone()))?
                    .as_str();
                if !EXCLUDED_TESTS.contains(name) {
                    tests.insert(name);
                }
            }
        }
        Ok(tests)
    }
}

/// Finds all the rustlib dependencies mentioned in a blueprint file.
pub trait RustDeps {
    /// Finds all the rustlibs mentioned in a blueprint file.
    /// This does a limited amount of evaluation, by doing concatenation and resolving
    /// identifiers. So you can have `common_rustlibs = ["foo", "bar"] and do
    /// rust_library { rustlibs = common_rustlibs + ["baz"] }`
    fn rust_deps(&self) -> BTreeSet<&str>;
}

impl RustDeps for BluePrint {
    fn rust_deps(&self) -> BTreeSet<&str> {
        let mut rustlibs = BTreeSet::new();
        for module in &self.modules {
            if let Some(v) = module.get("rustlibs") {
                match v {
                    Value::Array(_) => rustlibs.extend(v.as_strs()),
                    Value::ConcatExpr(_) => rustlibs.extend(v.eval(self)),
                    _ => {
                        println!("Only know how to handle Array and ConcatExpr");
                    }
                }
            }
        }
        rustlibs
    }
}

/// Convenience accessor for arrays of strings.
pub trait AsStringVec {
    /// Interpret a android_bp::Value as an array of strings, and convert it to
    /// an array of &str's. Any element that isn't a string is skipped.
    fn as_strs(&self) -> Vec<&str>;
}

impl AsStringVec for Value {
    fn as_strs(&self) -> Vec<&str> {
        if let Value::Array(vec) = self {
            vec.iter()
                .filter_map(|v| match v {
                    Value::String(s) => Some(s.as_str()),
                    _ => {
                        println!("Array element is not a string");
                        None
                    }
                })
                .collect()
        } else {
            println!("Value is not an array");
            vec![]
        }
    }
}

/// Evaluate concatenations and resolve identifiers.
trait EvalConcat {
    /// Evaluate a concatenation expression, resolving it into a single vector of strings.
    /// The elements being concatenated are assumed to be either identifiers or
    /// arrays of strings. Otherwise, they are skipped.
    fn eval<'a>(&'a self, bp: &'a BluePrint) -> Vec<&'a str>;
}

impl EvalConcat for Value {
    fn eval<'a>(&'a self, bp: &'a BluePrint) -> Vec<&'a str> {
        let mut strings = Vec::new();
        if let Value::ConcatExpr(expr) = self {
            for term in expr {
                match term {
                    Value::Array(_) => strings.extend(term.as_strs()),
                    Value::Ident(ident) => {
                        if let Some(ident_val) = bp.variables.get(ident) {
                            strings.extend(ident_val.as_strs());
                        }
                    }
                    _ => {
                        println!("Concat term is neither ident nor array");
                    }
                }
            }
        } else {
            println!("Value is not a ConcatExpr");
        }
        strings
    }
}

/// Find the names of Rust library targets
pub trait RustLibs {
    /// Returns the name of all Rust library targets in the blueprint.
    fn rust_libs(&self) -> Result<Vec<&str>, Error>;
}

impl RustLibs for BluePrint {
    fn rust_libs(&self) -> Result<Vec<&str>, Error> {
        let mut libs = Vec::new();
        for module in &self.modules {
            if matches!(
                module.typ.as_str(),
                "rust_library" | "rust_library_rlib" | "rust_library_host" | "rust_proc_macro"
            ) {
                libs.push(
                    module
                        .get_string("name")
                        .ok_or(Error::RuleWithoutName(module.typ.clone()))?
                        .as_str(),
                );
            }
        }
        Ok(libs)
    }
}

/// Trait for determining the features enabled for a Rust crate.
pub trait CrateFeatures {
    /// Return the features enabled for all crate variants in the Blueprint.
    fn crate_features<'a>(
        &'a self,
        crate_name: impl AsRef<str> + 'a,
    ) -> impl Iterator<Item = Option<Vec<&'a str>>>;
    /// Returns true if a feature is enabled for any build variant of the crate.
    fn is_enabled(&self, crate_name: impl AsRef<str>, feature: impl AsRef<str>) -> bool {
        self.crate_features(crate_name).any(|variant_features| {
            variant_features.is_some_and(|features| features.contains(&feature.as_ref()))
        })
    }
}

impl CrateFeatures for BluePrint {
    fn crate_features<'a>(
        &'a self,
        crate_name: impl AsRef<str> + 'a,
    ) -> impl Iterator<Item = Option<Vec<&'a str>>> {
        let crate_name = crate_name.as_ref().to_string();
        self.modules.iter().filter_map(move |m| {
            if m.get_string("crate_name").is_some_and(|c| *c == crate_name) {
                Some(m.get("features").map(|v| v.as_strs()))
            } else {
                None
            }
        })
    }
}

// Taken from update_crate_tests.py
static EXCLUDED_TESTS: LazyLock<HashSet<&'static str>> = LazyLock::new(|| {
    HashSet::from([
        "ash_test_src_lib",
        "ash_test_tests_constant_size_arrays",
        "ash_test_tests_display",
        "shared_library_test_src_lib",
        "vulkano_test_src_lib",
        // These are helper binaries for aidl_integration_test
        // and aren't actually meant to run as individual tests.
        "aidl_test_rust_client",
        "aidl_test_rust_service",
        "aidl_test_rust_service_async",
        // This is a helper binary for AuthFsHostTest and shouldn't
        // be run directly.
        "open_then_run",
        // TODO: Remove when b/198197213 is closed.
        "diced_client_test",
        "CoverageRustSmokeTest",
        "libtrusty-rs-tests",
        "terminal-size_test_src_lib",
    ])
});

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rust_tests() -> Result<(), Error> {
        let bp = BluePrint::parse(
            r###"
rust_test { name: "foo" }
rust_test_host { name: "bar" }
"###,
        )
        .expect("Blueprint parse error");
        assert_eq!(bp.rust_tests()?, BTreeSet::from(["foo", "bar"]));
        Ok(())
    }

    #[test]
    fn rust_deps() {
        let bp = BluePrint::parse(
            r###"
rust_library { rustlibs: ["foo", "bar"] }
rust_library { rustlibs: ["bar", "baz"] }
"###,
        )
        .expect("Blueprint parse error");
        assert_eq!(bp.rust_deps(), BTreeSet::from(["foo", "bar", "baz"]));
    }

    #[test]
    fn rust_deps_eval() {
        let bp = BluePrint::parse(
            r###"
foo = ["foo"]
rust_library { rustlibs: foo + ["bar"] }
"###,
        )
        .expect("Blueprint parse error");
        assert_eq!(bp.rust_deps(), BTreeSet::from(["foo", "bar"]));
    }

    #[test]
    fn rust_libs() -> Result<(), Error> {
        let bp = BluePrint::parse(
            r###"
rust_library { name: "foo" }
"###,
        )
        .expect("Blueprint parse error");
        assert_eq!(bp.rust_libs()?, vec!["foo"]);
        Ok(())
    }

    #[test]
    fn features() {
        let bp = BluePrint::parse(
            r###"
rust_library { crate_name: "foo" }
"###,
        )
        .expect("Blueprint parse error");
        assert_eq!(
            bp.crate_features("foo").collect::<Vec<_>>(),
            vec![None],
            "Missing features array returns None"
        );

        let bp = BluePrint::parse(
            r###"
rust_library { crate_name: "foo", features: [] }
"###,
        )
        .expect("Blueprint parse error");
        assert_eq!(
            bp.crate_features("foo").collect::<Vec<_>>(),
            vec![Some(Vec::new())],
            "Empty features array is preserved"
        );

        let bp = BluePrint::parse(
            r###"
rust_library { crate_name: "foo", features: ["foo"] }
rust_library { crate_name: "bar", features: ["bar"] }
"###,
        )
        .expect("Blueprint parse error");
        assert_eq!(
            bp.crate_features("foo").collect::<Vec<_>>(),
            vec![Some(vec!["foo"])],
            "Crate name must match"
        );

        let bp = BluePrint::parse(
            r###"
rust_library { crate_name: "foo", features: ["foo1"] }
rust_library { crate_name: "foo", features: ["foo2"] }
"###,
        )
        .expect("Blueprint parse error");
        assert_eq!(
            bp.crate_features("foo").collect::<Vec<_>>(),
            vec![Some(vec!["foo1"]), Some(vec!["foo2"])],
            "Multiple variants of the same crate each return a list of features"
        );
    }

    #[test]
    fn feature_is_enabled() {
        let bp = BluePrint::parse(
            r###"
rust_library { crate_name: "foo", features: ["bar"] }
rust_library { crate_name: "foo", features: ["baz"] }
"###,
        )
        .expect("Blueprint parse error");
        assert!(bp.is_enabled("foo", "bar"));
        assert!(bp.is_enabled("foo", "baz"));
        assert!(!bp.is_enabled("foo", "qux"));
    }
}
