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
use anyhow::anyhow;
use anyhow::bail;
use anyhow::Context;
use anyhow::Result;
use once_cell::sync::Lazy;
use regex::Regex;
use std::collections::BTreeMap;
use std::env;
use std::fs::{read_to_string, File};
use std::path::Path;
use std::path::PathBuf;

/// Reads the given `cargo.out` and `cargo.metadata` files, and generates a list of crates based on
/// the rustc invocations.
///
/// Ignores crates outside the current directory and build script crates.
pub fn parse_cargo_out(
    cargo_out_path: impl AsRef<Path>,
    cargo_metadata_path: impl AsRef<Path>,
) -> Result<Vec<Crate>> {
    let cargo_out = read_to_string(cargo_out_path).context("failed to read cargo.out")?;
    let metadata = serde_json::from_reader(
        File::open(cargo_metadata_path).context("failed to open cargo.metadata")?,
    )
    .context("failed to parse cargo.metadata")?;
    parse_cargo_out_str(&cargo_out, &metadata, env::current_dir().unwrap().canonicalize().unwrap())
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

    assert!(cargo_out.cc_invocations.is_empty(), "cc not supported yet");
    assert!(cargo_out.ar_invocations.is_empty(), "ar not supported yet");

    let mut crates = Vec::new();
    for rustc in cargo_out.rustc_invocations.iter() {
        let c = Crate::from_rustc_invocation(rustc, metadata)
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
    Ok(crates)
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
        }

        // self.find_warning_owners()

        Ok(result)
    }
}

impl Crate {
    fn from_rustc_invocation(rustc: &str, metadata: &WorkspaceMetadata) -> Result<Crate> {
        let mut out = Crate::default();

        // split into args
        let args: Vec<&str> = rustc.split_whitespace().collect();
        let mut arg_iter = args
            .iter()
            // Remove quotes from simple strings, panic for others.
            .map(|arg| match (arg.chars().next(), arg.chars().skip(1).last()) {
                (Some('"'), Some('"')) => &arg[1..arg.len() - 1],
                (Some('\''), Some('\'')) => &arg[1..arg.len() - 1],
                (Some('"'), _) => panic!("can't handle strings with whitespace"),
                (Some('\''), _) => panic!("can't handle strings with whitespace"),
                _ => arg,
            });
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

                        // Example filename: "libgetrandom-fd8800939535fc59.rmeta"
                        static REGEX: Lazy<Regex> = Lazy::new(|| {
                            Regex::new(r"^lib(.*)-[0-9a-f]*.(rlib|so|rmeta)$").unwrap()
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
                        out.externs.push(Extern {
                            name: name.to_string(),
                            lib_name: lib_name.as_str().to_string(),
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
                _ if arg.starts_with("--error-format=") => {}
                _ if arg.starts_with("--emit=") => {}
                _ if arg.starts_with("--edition=") => {}
                _ if arg.starts_with("--json=") => {}
                _ if arg.starts_with("-Aclippy") => {}
                _ if arg.starts_with("-Wclippy") => {}
                "-W" => {}
                "-D" => {}

                arg => bail!("unsupported rustc argument: {arg:?}"),
            }
        }

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
        out.package_name = package_metadata.name.clone();
        out.version = Some(package_metadata.version.clone());
        out.edition = package_metadata.edition.clone();

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
