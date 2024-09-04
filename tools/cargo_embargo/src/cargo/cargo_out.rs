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

use super::metadata::WorkspaceMetadata;
use super::{Crate, CrateType, Extern, ExternType};
use crate::CargoOutput;
use anyhow::anyhow;
use anyhow::bail;
use anyhow::Context;
use anyhow::Result;
use log::debug;
use once_cell::sync::Lazy;
use regex::Regex;
use std::collections::BTreeMap;
use std::env;
use std::path::Path;
use std::path::PathBuf;

/// Reads the given `cargo.out` and `cargo.metadata` files, and generates a list of crates based on
/// the rustc invocations.
///
/// Ignores crates outside the current directory and build script crates.
pub fn parse_cargo_out(cargo_output: &CargoOutput) -> Result<Vec<Crate>> {
    let metadata = serde_json::from_str(&cargo_output.cargo_metadata)
        .context("failed to parse cargo metadata")?;
    parse_cargo_out_str(
        &cargo_output.cargo_out,
        &metadata,
        env::current_dir().unwrap().canonicalize().unwrap(),
    )
}

/// Parses the given `cargo.out` and `cargo.metadata` file contents and generates a list of crates
/// based on the rustc invocations.
///
/// Ignores crates outside `base_directory` and build script crates.
fn parse_cargo_out_str(
    cargo_out: &str,
    metadata: &WorkspaceMetadata,
    base_directory: impl AsRef<Path>,
) -> Result<Vec<Crate>> {
    let cargo_out = CargoOut::parse(cargo_out).context("failed to parse cargo.out")?;
    debug!("Parsed cargo output: {:?}", cargo_out);

    assert!(cargo_out.cc_invocations.is_empty(), "cc not supported yet");
    assert!(cargo_out.ar_invocations.is_empty(), "ar not supported yet");

    let mut raw_names = BTreeMap::new();
    for rustc in cargo_out.rustc_invocations.iter() {
        raw_name_from_rustc_invocation(rustc, &mut raw_names)
    }

    let mut crates = Vec::new();
    for rustc in cargo_out.rustc_invocations.iter() {
        let c = Crate::from_rustc_invocation(rustc, metadata, &cargo_out.tests, &raw_names)
            .with_context(|| format!("failed to process rustc invocation: {rustc}"))?;
        // Ignore build.rs crates.
        if c.name.starts_with("build_script_") {
            continue;
        }
        // Ignore crates outside the base directory.
        if !c.package_dir.starts_with(&base_directory) {
            continue;
        }
        crates.push(c);
    }
    crates.dedup();
    Ok(crates)
}

fn args_from_rustc_invocation(rustc: &str) -> Vec<&str> {
    let mut args = Vec::new();
    let mut chars = rustc.char_indices();
    while let Some((start, c)) = chars.next() {
        match c {
            ' ' => {}
            '\'' => {
                let (end, _) =
                    chars.find(|(_, c)| *c == '\'').expect("Missing closing single quote");
                args.push(&rustc[start + 1..end]);
            }
            '"' => {
                let (end, _) =
                    chars.find(|(_, c)| *c == '"').expect("Missing closing double quote");
                args.push(&rustc[start + 1..end]);
            }
            _ => {
                if let Some((end, _)) = chars.find(|(_, c)| *c == ' ') {
                    args.push(&rustc[start..end]);
                } else {
                    args.push(&rustc[start..]);
                }
            }
        }
    }
    args
}

/// Parse out the path name for a crate from a rustc invocation
fn raw_name_from_rustc_invocation(rustc: &str, raw_names: &mut BTreeMap<String, String>) {
    let mut crate_name = String::new();
    // split into args
    let mut arg_iter = args_from_rustc_invocation(rustc).into_iter();
    // look for the crate name and a string ending in .rs and check whether the
    // path string contains a kebab-case version of the crate name
    while let Some(arg) = arg_iter.next() {
        match arg {
            "--crate-name" => crate_name = arg_iter.next().unwrap().to_string(),
            _ if arg.ends_with(".rs") => {
                assert_ne!(crate_name, "", "--crate-name option should precede input");
                let snake_case_arg = arg.replace('-', "_");
                if let Some(idx) = snake_case_arg.rfind(&crate_name) {
                    let raw_name = arg[idx..idx + crate_name.len()].to_string();
                    if crate_name != raw_name {
                        raw_names.insert(crate_name, raw_name);
                    }
                }
                break;
            }
            _ => {}
        }
    }
}

/// Whether a test target contains any tests or benchmarks.
#[derive(Debug)]
struct TestContents {
    tests: bool,
    benchmarks: bool,
}

/// Raw-ish data extracted from cargo.out file.
#[derive(Debug, Default)]
struct CargoOut {
    rustc_invocations: Vec<String>,

    // package name => cmd args
    cc_invocations: BTreeMap<String, String>,
    ar_invocations: BTreeMap<String, String>,

    // lines starting with "warning: ".
    // line number => line
    warning_lines: BTreeMap<usize, String>,
    warning_files: Vec<String>,

    // output filename => test filename => whether it contains any tests or benchmarks
    tests: BTreeMap<String, BTreeMap<PathBuf, TestContents>>,

    errors: Vec<String>,
    test_errors: Vec<String>,
}

fn match1(regex: &Regex, s: &str) -> Option<String> {
    regex.captures(s).and_then(|x| x.get(1)).map(|x| x.as_str().to_string())
}

fn match3(regex: &Regex, s: &str) -> Option<(String, String, String)> {
    regex.captures(s).and_then(|x| match (x.get(1), x.get(2), x.get(3)) {
        (Some(a), Some(b), Some(c)) => {
            Some((a.as_str().to_string(), b.as_str().to_string(), c.as_str().to_string()))
        }
        _ => None,
    })
}

impl CargoOut {
    /// Parse the output of a `cargo build -v` run.
    fn parse(contents: &str) -> Result<CargoOut> {
        let mut result = CargoOut::default();
        let mut in_tests = false;
        let mut cur_test_key = None;
        let mut lines_iter = contents.lines().enumerate();
        while let Some((n, line)) = lines_iter.next() {
            if line.starts_with("warning: ") {
                result.warning_lines.insert(n, line.to_string());
                continue;
            }

            // Cargo -v output of a call to rustc.
            static RUSTC_REGEX: Lazy<Regex> =
                Lazy::new(|| Regex::new(r"^ +Running `rustc (.*)`$").unwrap());
            if let Some(args) = match1(&RUSTC_REGEX, line) {
                result.rustc_invocations.push(args);
                continue;
            }
            // Cargo -vv output of a call to rustc could be split into multiple lines.
            // Assume that the first line will contain some CARGO_* env definition.
            static RUSTC_VV_REGEX: Lazy<Regex> =
                Lazy::new(|| Regex::new(r"^ +Running `.*CARGO_.*=.*$").unwrap());
            if RUSTC_VV_REGEX.is_match(line) {
                // cargo build -vv output can have multiple lines for a rustc command due to
                // '\n' in strings for environment variables.
                let mut line = line.to_string();
                loop {
                    // Use an heuristic to detect the completions of a multi-line command.
                    if line.ends_with('`') && line.chars().filter(|c| *c == '`').count() % 2 == 0 {
                        break;
                    }
                    if let Some((_, next_line)) = lines_iter.next() {
                        line += next_line;
                        continue;
                    }
                    break;
                }
                // The combined -vv output rustc command line pattern.
                static RUSTC_VV_CMD_ARGS: Lazy<Regex> =
                    Lazy::new(|| Regex::new(r"^ *Running `.*CARGO_.*=.* rustc (.*)`$").unwrap());
                if let Some(args) = match1(&RUSTC_VV_CMD_ARGS, &line) {
                    result.rustc_invocations.push(args);
                } else {
                    bail!("failed to parse cargo.out line: {}", line);
                }
                continue;
            }
            // Cargo -vv output of a "cc" or "ar" command; all in one line.
            static CC_AR_VV_REGEX: Lazy<Regex> = Lazy::new(|| {
                Regex::new(r#"^\[([^ ]*)[^\]]*\] running:? "(cc|ar)" (.*)$"#).unwrap()
            });
            if let Some((pkg, cmd, args)) = match3(&CC_AR_VV_REGEX, line) {
                match cmd.as_str() {
                    "ar" => result.ar_invocations.insert(pkg, args),
                    "cc" => result.cc_invocations.insert(pkg, args),
                    _ => unreachable!(),
                };
                continue;
            }
            // Rustc output of file location path pattern for a warning message.
            static WARNING_FILE_REGEX: Lazy<Regex> =
                Lazy::new(|| Regex::new(r"^ *--> ([^:]*):[0-9]+").unwrap());
            if result.warning_lines.contains_key(&n.saturating_sub(1)) {
                if let Some(fpath) = match1(&WARNING_FILE_REGEX, line) {
                    result.warning_files.push(fpath);
                    continue;
                }
            }
            if line.starts_with("error: ") || line.starts_with("error[E") {
                if in_tests {
                    result.test_errors.push(line.to_string());
                } else {
                    result.errors.push(line.to_string());
                }
                continue;
            }
            static CARGO2ANDROID_RUNNING_REGEX: Lazy<Regex> =
                Lazy::new(|| Regex::new(r"^### Running: .*$").unwrap());
            if CARGO2ANDROID_RUNNING_REGEX.is_match(line) {
                in_tests = line.contains("cargo test") && line.contains("--list");
                continue;
            }

            // `cargo test -- --list` output
            // Example: Running unittests src/lib.rs (target.tmp/x86_64-unknown-linux-gnu/debug/deps/aarch64-58b675be7dc09833)
            static CARGO_TEST_LIST_START_PAT: Lazy<Regex> =
                Lazy::new(|| Regex::new(r"^\s*Running (?:unittests )?(.*) \(.*/(.*)\)$").unwrap());
            static CARGO_TEST_LIST_END_PAT: Lazy<Regex> =
                Lazy::new(|| Regex::new(r"^(\d+) tests?, (\d+) benchmarks$").unwrap());
            if let Some(captures) = CARGO_TEST_LIST_START_PAT.captures(line) {
                cur_test_key =
                    Some((captures.get(2).unwrap().as_str(), captures.get(1).unwrap().as_str()));
            } else if let Some((output_filename, main_src)) = cur_test_key {
                if let Some(captures) = CARGO_TEST_LIST_END_PAT.captures(line) {
                    let num_tests = captures.get(1).unwrap().as_str().parse::<u32>().unwrap();
                    let num_benchmarks = captures.get(2).unwrap().as_str().parse::<u32>().unwrap();
                    result.tests.entry(output_filename.to_owned()).or_default().insert(
                        PathBuf::from(main_src),
                        TestContents { tests: num_tests != 0, benchmarks: num_benchmarks != 0 },
                    );
                    cur_test_key = None;
                }
            }
        }

        // self.find_warning_owners()

        Ok(result)
    }
}

impl Crate {
    fn from_rustc_invocation(
        rustc: &str,
        metadata: &WorkspaceMetadata,
        tests: &BTreeMap<String, BTreeMap<PathBuf, TestContents>>,
        raw_names: &BTreeMap<String, String>,
    ) -> Result<Crate> {
        let mut out = Crate::default();
        let mut extra_filename = String::new();

        // split into args
        let mut arg_iter = args_from_rustc_invocation(rustc).into_iter();
        // process each arg
        while let Some(arg) = arg_iter.next() {
            match arg {
                "--crate-name" => out.name = arg_iter.next().unwrap().to_string(),
                "--crate-type" => out
                    .types
                    .push(CrateType::from_str(arg_iter.next().unwrap().to_string().as_str())),
                "--test" => out.types.push(CrateType::Test),
                "--target" => out.target = Some(arg_iter.next().unwrap().to_string()),
                "--cfg" => {
                    // example: feature=\"sink\"
                    let arg = arg_iter.next().unwrap();
                    if let Some(feature) =
                        arg.strip_prefix("feature=\"").and_then(|s| s.strip_suffix('\"'))
                    {
                        out.features.push(feature.to_string());
                    } else {
                        out.cfgs.push(arg.to_string());
                    }
                }
                "--extern" => {
                    // example: proc_macro
                    // example: memoffset=/some/path/libmemoffset-2cfda327d156e680.rmeta
                    let arg = arg_iter.next().unwrap();
                    if let Some((name, path)) = arg.split_once('=') {
                        let filename = path.split('/').last().unwrap();

                        // Example filename: "libgetrandom-fd8800939535fc59.rmeta" or "libmls_rs_uniffi.rlib".
                        static REGEX: Lazy<Regex> = Lazy::new(|| {
                            Regex::new(r"^lib([^-]*)(?:-[0-9a-f]*)?.(rlib|so|rmeta)$").unwrap()
                        });

                        let Some(lib_name) = REGEX.captures(filename).and_then(|x| x.get(1)) else {
                            bail!("bad filename for extern {}: {}", name, filename);
                        };
                        let extern_type =
                            if filename.ends_with(".rlib") || filename.ends_with(".rmeta") {
                                ExternType::Rust
                            } else if filename.ends_with(".so") {
                                // Assume .so files are always proc_macros. May not always be right.
                                ExternType::ProcMacro
                            } else {
                                bail!("Unexpected extension for extern filename {}", filename);
                            };

                        let lib_name = lib_name.as_str().to_string();
                        let raw_name = if let Some(raw_name) = raw_names.get(&lib_name) {
                            raw_name.to_owned()
                        } else {
                            lib_name.clone()
                        };
                        out.externs.push(Extern {
                            name: name.to_string(),
                            lib_name,
                            raw_name,
                            extern_type,
                        });
                    } else if arg != "proc_macro" {
                        panic!("No filename for {}", arg);
                    }
                }
                _ if arg.starts_with("-C") => {
                    // handle both "-Cfoo" and "-C foo"
                    let arg = if arg == "-C" {
                        arg_iter.next().unwrap()
                    } else {
                        arg.strip_prefix("-C").unwrap()
                    };
                    // 'prefer-dynamic' does not work with common flag -C lto
                    // 'embed-bitcode' is ignored; we might control LTO with other .bp flag
                    // 'codegen-units' is set in Android global config or by default
                    //
                    // TODO: this is business logic. move it out of the parsing code
                    if !arg.starts_with("codegen-units=")
                        && !arg.starts_with("debuginfo=")
                        && !arg.starts_with("embed-bitcode=")
                        && !arg.starts_with("extra-filename=")
                        && !arg.starts_with("incremental=")
                        && !arg.starts_with("metadata=")
                        && arg != "prefer-dynamic"
                    {
                        out.codegens.push(arg.to_string());
                    }
                    if let Some(x) = arg.strip_prefix("extra-filename=") {
                        extra_filename = x.to_string();
                    }
                }
                "--cap-lints" => out.cap_lints = arg_iter.next().unwrap().to_string(),
                "-l" => {
                    let arg = arg_iter.next().unwrap();
                    if let Some(lib) = arg.strip_prefix("static=") {
                        out.static_libs.push(lib.to_string());
                    } else if let Some(lib) = arg.strip_prefix("dylib=") {
                        out.shared_libs.push(lib.to_string());
                    } else {
                        out.shared_libs.push(arg.to_string());
                    }
                }
                _ if !arg.starts_with('-') => {
                    (out.package_dir, out.main_src) = split_src_path(Path::new(arg))?;
                }

                // ignored flags
                "-L" => {
                    arg_iter.next().unwrap();
                }
                "--out-dir" => {
                    arg_iter.next().unwrap();
                }
                "--color" => {
                    arg_iter.next().unwrap();
                }
                "--check-cfg" => {
                    arg_iter.next().unwrap();
                }
                _ if arg.starts_with("--error-format=") => {}
                _ if arg.starts_with("--emit=") => {}
                _ if arg.starts_with("--edition=") => {}
                _ if arg.starts_with("--json=") => {}
                _ if arg.starts_with("-Aclippy") => {}
                _ if arg.starts_with("--allow=clippy") => {}
                _ if arg.starts_with("-Wclippy") => {}
                _ if arg.starts_with("--warn=clippy") => {}
                _ if arg.starts_with("-A=rustdoc") => {}
                _ if arg.starts_with("--allow=rustdoc") => {}
                _ if arg.starts_with("-D") => {}
                _ if arg.starts_with("--deny=") => {}
                _ if arg.starts_with("-W") => {}
                _ if arg.starts_with("--warn=") => {}

                arg => bail!("unsupported rustc argument: {arg:?}"),
            }
        }
        out.cfgs.sort();
        out.cfgs.dedup();
        out.codegens.sort();
        out.features.sort();

        if out.name.is_empty() {
            bail!("missing --crate-name");
        }
        if out.main_src.as_os_str().is_empty() {
            bail!("missing main source file");
        }
        // Must have at least one type.
        if out.types.is_empty() {
            if out.cfgs.contains(&"test".to_string()) {
                out.types.push(CrateType::TestNoHarness);
            } else {
                bail!("failed to detect crate type. did not have --crate-type or --test or '--cfg test'");
            }
        }
        if out.types.contains(&CrateType::Test) && out.types.len() != 1 {
            bail!("cannot specify both --test and --crate-type");
        }
        if out.types.contains(&CrateType::Lib) && out.types.contains(&CrateType::RLib) {
            bail!("cannot both have lib and rlib crate types");
        }

        // Find the metadata for the crates containing package by matching the manifest's path.
        let manifest_path = out.package_dir.join("Cargo.toml");
        let package_metadata = metadata
            .packages
            .iter()
            .find(|p| Path::new(&p.manifest_path).canonicalize().unwrap() == manifest_path)
            .ok_or_else(|| {
                anyhow!(
                    "can't find metadata for crate {:?} with manifest path {:?}",
                    out.name,
                    manifest_path,
                )
            })?;
        out.package_name.clone_from(&package_metadata.name);
        out.version = Some(package_metadata.version.clone());
        out.edition.clone_from(&package_metadata.edition);
        out.license.clone_from(&package_metadata.license);
        out.license_file.clone_from(&package_metadata.license_file);

        let output_filename = out.name.clone() + &extra_filename;
        if let Some(test_contents) = tests.get(&output_filename).and_then(|m| m.get(&out.main_src))
        {
            out.empty_test = !test_contents.tests && !test_contents.benchmarks;
        }

        Ok(out)
    }
}

/// Given a path to the main source file of some Rust crate, returns the canonical path to the
/// package directory, and the relative path to the source file within that directory.
fn split_src_path(src_path: &Path) -> Result<(PathBuf, PathBuf)> {
    // Canonicalize the path because:
    //
    // 1. We don't consistently get relative or absolute paths elsewhere. If we
    //    canonicalize everything, it becomes easy to compare paths.
    //
    // 2. We don't want to consider symlinks to code outside the cwd as part of the
    //    project (e.g. AOSP's import of crosvm has symlinks from crosvm's own 3p
    //    directory to the android 3p directories).
    let src_path = src_path
        .canonicalize()
        .unwrap_or_else(|e| panic!("failed to canonicalize {src_path:?}: {}", e));
    let package_dir = find_cargo_toml(&src_path)?;
    let main_src = src_path.strip_prefix(&package_dir).unwrap().to_path_buf();

    Ok((package_dir, main_src))
}

/// Given a path to a Rust source file, finds the closest ancestor directory containing a
/// `Cargo.toml` file.
fn find_cargo_toml(src_path: &Path) -> Result<PathBuf> {
    let mut package_dir = src_path.parent().unwrap();
    while !package_dir.join("Cargo.toml").try_exists()? {
        package_dir = package_dir
            .parent()
            .ok_or_else(|| anyhow!("No Cargo.toml found in parents of {:?}", src_path))?;
    }
    Ok(package_dir.to_path_buf())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parse_args() {
        assert_eq!(args_from_rustc_invocation("foo bar"), vec!["foo", "bar"]);
        assert_eq!(args_from_rustc_invocation("  foo   bar "), vec!["foo", "bar"]);
        assert_eq!(args_from_rustc_invocation("'foo' \"bar\""), vec!["foo", "bar"]);
        assert_eq!(
            args_from_rustc_invocation("'fo o' \" b ar\" ' baz '"),
            vec!["fo o", " b ar", " baz "]
        );
    }
}
