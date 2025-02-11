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

use crate::Error;

/// Extract rust test rules from a blueprint file.
pub(crate) trait RustTests {
    /// Returns the names of all rust_test and rust_test_host rules.
    fn rust_tests(&self) -> Result<BTreeSet<String>, Error>;
}

impl RustTests for BluePrint {
    fn rust_tests(&self) -> Result<BTreeSet<String>, Error> {
        let mut tests = BTreeSet::new();
        for module in &self.modules {
            if matches!(module.typ.as_str(), "rust_test" | "rust_test_host") {
                let name = module
                    .get_string("name")
                    .ok_or(Error::RuleWithoutName(module.typ.clone()))?
                    .clone();
                if !EXCLUDED_TESTS.contains(name.as_str()) {
                    tests.insert(name);
                }
            }
        }
        Ok(tests)
    }
}

/// Finds all the rustlib dependencies mentioned in a blueprint file.
pub(crate) trait RustDeps {
    // Finds all the rustlibs mentioned in a blueprint file.
    // This does a limited amount of evaluation, by doing concatenation and resolving
    // identifiers. So you can have `common_rustlibs = ["foo", "bar"] and do
    // rust_library { rustlibs = common_rustlibs + ["baz"] }`
    fn rust_deps(&self) -> BTreeSet<String>;
}

impl RustDeps for BluePrint {
    fn rust_deps(&self) -> BTreeSet<String> {
        let mut rustlibs = BTreeSet::new();
        for module in &self.modules {
            if let Some(v) = module.get("rustlibs") {
                match v {
                    Value::Array(_) => rustlibs.extend(v.as_string_vec()),
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

// Convenience accessor for arrays of strings.
trait AsStringVec {
    // Interpret a android_bp::Value as an array of strings, and convert it to
    // an array of owned strings. Any element that isn't a string is skipped.
    fn as_string_vec(&self) -> Vec<String>;
}

impl AsStringVec for Value {
    fn as_string_vec(&self) -> Vec<String> {
        if let Value::Array(vec) = self {
            vec.iter()
                .filter_map(|v| match v {
                    Value::String(s) => Some(s.to_string()),
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

// Evaluate concatenations and resolve identifiers.
trait EvalConcat {
    // Evaluate a concatenation expression, resolving it into a single vector of strings.
    // The elements being concatenated are assumed to be either identifiers or
    // arrays of strings. Otherwise, they are skipped.
    fn eval(&self, bp: &BluePrint) -> Vec<String>;
}

impl EvalConcat for Value {
    fn eval(&self, bp: &BluePrint) -> Vec<String> {
        let mut strings = Vec::new();
        if let Value::ConcatExpr(expr) = self {
            for term in expr {
                match term {
                    Value::Array(_) => strings.extend(term.as_string_vec()),
                    Value::Ident(ident) => {
                        if let Some(ident_val) = bp.variables.get(ident) {
                            strings.extend(ident_val.as_string_vec());
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
        assert_eq!(bp.rust_tests()?, BTreeSet::from(["foo".to_string(), "bar".to_string()]));
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
        assert_eq!(
            bp.rust_deps(),
            BTreeSet::from(["foo".to_string(), "bar".to_string(), "baz".to_string()])
        );
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
        assert_eq!(bp.rust_deps(), BTreeSet::from(["foo".to_string(), "bar".to_string()]));
    }
}
