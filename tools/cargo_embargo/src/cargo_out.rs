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

use anyhow::anyhow;
use anyhow::bail;
use anyhow::Context;
use anyhow::Result;
use once_cell::sync::Lazy;
use regex::Regex;
use std::collections::BTreeMap;
use std::path::Path;
use std::path::PathBuf;

/// Combined representation of --crate-type and --test flags.
#[derive(Debug, PartialEq, Eq)]
pub enum CrateType {
    // --crate-type types
    Bin,
    Lib,
    RLib,
    DyLib,
    CDyLib,
    StaticLib,
    ProcMacro,
    // --test
    Test,
    // "--cfg test" without --test. (Assume it is a test with the harness disabled.
    TestNoHarness,
}

/// Info extracted from `CargoOut` for a crate.
///
/// Note that there is a 1-to-many relationship between a Cargo.toml file and these `Crate`
/// objects. For example, a Cargo.toml file might have a bin, a lib, and various tests. Each of
/// those will be a separate `Crate`. All of them will have the same `package_name`.
#[derive(Debug, Default)]
pub struct Crate {
    pub name: String,
    pub package_name: String,
    pub version: Option<String>,
    pub types: Vec<CrateType>,
    pub target: Option<String>,                 // --target
    pub features: Vec<String>,                  // --cfg feature=
    pub cfgs: Vec<String>,                      // non-feature --cfg
    pub externs: Vec<(String, Option<String>)>, // name => rlib file
    pub codegens: Vec<String>,                  // -C
    pub cap_lints: String,
    pub static_libs: Vec<String>,
    pub shared_libs: Vec<String>,
    pub emit_list: String,
    pub edition: String,
    pub package_dir: PathBuf, // canonicalized
    pub main_src: PathBuf,    // relative to package_dir
}

pub fn parse_cargo_out(cargo_out_path: &str, cargo_metadata_path: &str) -> Result<Vec<Crate>> {
    let metadata: WorkspaceMetadata = serde_json::from_str(
        &std::fs::read_to_string(cargo_metadata_path).context("failed to read cargo.metadata")?,
    )
    .context("failed to parse cargo.metadata")?;

    let cargo_out = CargoOut::parse(
        &std::fs::read_to_string(cargo_out_path).context("failed to read cargo.out")?,
    )
    .context("failed to parse cargo.out")?;

    assert!(cargo_out.cc_invocations.is_empty(), "cc not supported yet");
    assert!(cargo_out.ar_invocations.is_empty(), "ar not supported yet");

    let mut crates = Vec::new();
    for rustc in cargo_out.rustc_invocations.iter() {
        let c = Crate::from_rustc_invocation(rustc, &metadata)
            .with_context(|| format!("failed to process rustc invocation: {rustc}"))?;
        // Ignore build.rs crates.
        if c.name.starts_with("build_script_") {
            continue;
        }
        // Ignore crates outside the current directory.
        let cwd = std::env::current_dir().unwrap().canonicalize().unwrap();
        if !c.package_dir.starts_with(cwd) {
            continue;
        }
        crates.push(c);
    }
    Ok(crates)
}

/// `cargo metadata` output.
#[derive(serde::Deserialize)]
struct WorkspaceMetadata {
    packages: Vec<PackageMetadata>,
}

#[derive(serde::Deserialize)]
struct PackageMetadata {
    name: String,
    version: String,
    edition: String,
    manifest_path: String,
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

impl CrateType {
    fn from_str(s: &str) -> CrateType {
        match s {
            "bin" => CrateType::Bin,
            "lib" => CrateType::Lib,
            "rlib" => CrateType::RLib,
            "dylib" => CrateType::DyLib,
            "cdylib" => CrateType::CDyLib,
            "staticlib" => CrateType::StaticLib,
            "proc-macro" => CrateType::ProcMacro,
            _ => panic!("unexpected --crate-type: {}", s),
        }
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
                        out.externs.push((
                            name.to_string(),
                            Some(path.split('/').last().unwrap().to_string()),
                        ));
                    } else {
                        out.externs.push((arg.to_string(), None));
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
                _ if arg.starts_with("--emit=") => {
                    out.emit_list = arg.strip_prefix("--emit=").unwrap().to_string();
                }
                _ if !arg.starts_with('-') => {
                    let src_path = Path::new(arg);
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
                    out.package_dir = src_path.parent().unwrap().to_path_buf();
                    while !out.package_dir.join("Cargo.toml").try_exists()? {
                        if let Some(parent) = out.package_dir.parent() {
                            out.package_dir = parent.to_path_buf();
                        } else {
                            bail!("No Cargo.toml found in parents of {:?}", src_path);
                        }
                    }
                    out.main_src = src_path.strip_prefix(&out.package_dir).unwrap().to_path_buf();
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
