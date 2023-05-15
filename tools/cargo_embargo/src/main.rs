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

//! Converts a cargo project to Soong.
//!
//! Forked from development/scripts/cargo2android.py. Missing many of its features. Adds various
//! features to make it easier to work with projects containing many crates.
//!
//! At a high level, this is done by
//!
//!     1. Running `cargo build -v` and saving the output to a "cargo.out" file.
//!     2. Parsing the "cargo.out" file to find invocations of compilers, e.g. `rustc` and `cc`.
//!     3. For each compiler invocation, generating a equivalent Soong module, e.g. a "rust_library".
//!
//! The last step often involves messy, project specific business logic, so many options are
//! available to tweak it via a config file.

mod bp;
mod cargo_out;

use anyhow::bail;
use anyhow::Context;
use anyhow::Result;
use bp::*;
use cargo_out::*;
use clap::Parser;
use once_cell::sync::Lazy;
use regex::Regex;
use std::collections::BTreeMap;
use std::collections::VecDeque;
use std::fs::File;
use std::io::Write;
use std::path::Path;
use std::path::PathBuf;
use std::process::Command;

// Major TODOs
//  * handle errors, esp. in cargo.out parsing. they should fail the program with an error code
//  * handle warnings. put them in comments in the android.bp, some kind of report section

#[derive(Parser, Debug)]
#[clap()]
struct Args {
    /// Use the cargo binary in the `cargo_bin` directory. Defaults to cargo in $PATH.
    ///
    /// TODO: Should default to android prebuilts.
    #[clap(long)]
    cargo_bin: Option<PathBuf>,
    /// Config file.
    #[clap(long)]
    cfg: PathBuf,
    /// Skip the `cargo build` commands and reuse the "cargo.out" file from a previous run if
    /// available.
    #[clap(long)]
    reuse_cargo_out: bool,
}

fn default_apex_available() -> Vec<String> {
    vec!["//apex_available:platform".to_string(), "//apex_available:anyapex".to_string()]
}

/// Options that apply to everything.
#[derive(serde::Deserialize)]
#[serde(deny_unknown_fields)]
struct Config {
    /// Whether to output "rust_test" modules.
    tests: bool,
    /// Set of features to enable. If non-empty, disables the default crate features.
    #[serde(default)]
    features: Vec<String>,
    /// Whether to build with --workspace.
    #[serde(default)]
    workspace: bool,
    /// When workspace is enabled, list of --exclude crates.
    #[serde(default)]
    workspace_excludes: Vec<String>,
    /// Value to use for every generated module's "defaults" field.
    global_defaults: Option<String>,
    /// Value to use for every generated library module's "apex_available" field.
    #[serde(default = "default_apex_available")]
    apex_available: Vec<String>,
    /// Map of renames for modules. For example, if a "libfoo" would be generated and there is an
    /// entry ("libfoo", "libbar"), the generated module will be called "libbar" instead.
    ///
    /// Also, affects references to dependencies (e.g. in a "static_libs" list), even those outside
    /// the project being processed.
    #[serde(default)]
    module_name_overrides: BTreeMap<String, String>,
    /// Package specific config options.
    #[serde(default)]
    package: BTreeMap<String, PackageConfig>,
    /// Modules in this list will not be generated.
    #[serde(default)]
    module_blocklist: Vec<String>,
    /// Modules name => Soong "visibility" property.
    #[serde(default)]
    module_visibility: BTreeMap<String, Vec<String>>,
}

/// Options that apply to everything in a package (i.e. everything associated with a particular
/// Cargo.toml file).
#[derive(serde::Deserialize, Default)]
#[serde(deny_unknown_fields)]
struct PackageConfig {
    /// Whether to compile for device. Defaults to true.
    #[serde(default)]
    device_supported: Option<bool>,
    /// Whether to compile for host. Defaults to true.
    #[serde(default)]
    host_supported: Option<bool>,
    /// Generate "rust_library_rlib" instead of "rust_library".
    #[serde(default)]
    force_rlib: bool,
    /// Whether to disable "unit_test" for "rust_test" modules.
    // TODO: Should probably be a list of modules or crates. A package might have a mix of unit and
    // integration tests.
    #[serde(default)]
    no_presubmit: bool,
    /// File with content to append to the end of the generated Android.bp.
    add_toplevel_block: Option<PathBuf>,
    /// File with content to append to the end of each generated module.
    add_module_block: Option<PathBuf>,
    /// Modules in this list will not be added as dependencies of generated modules.
    #[serde(default)]
    dep_blocklist: Vec<String>,
    /// Patch file to apply after Android.bp is generated.
    patch: Option<PathBuf>,
    /// Copy build.rs output to ./out/* and add a genrule to copy ./out/* to genrule output.
    /// For crates with code pattern:
    ///     include!(concat!(env!("OUT_DIR"), "/<some_file>.rs"))
    #[serde(default)]
    copy_out: bool,
}

fn main() -> Result<()> {
    let args = Args::parse();

    let json_str = std::fs::read_to_string(&args.cfg)
        .with_context(|| format!("failed to read file: {:?}", args.cfg))?;
    // Add some basic support for comments to JSON.
    let json_str: String = json_str.lines().filter(|l| !l.trim_start().starts_with("//")).collect();
    let cfg: Config = serde_json::from_str(&json_str).context("failed to parse config")?;

    if !Path::new("Cargo.toml").try_exists().context("when checking Cargo.toml")? {
        bail!("Cargo.toml missing. Run in a directory with a Cargo.toml file.");
    }

    // Add the custom cargo to PATH.
    // NOTE: If the directory with cargo has more binaries, this could have some unpredictable side
    // effects. That is partly intended though, because we want to use that cargo binary's
    // associated rustc.
    if let Some(cargo_bin) = args.cargo_bin {
        let path = std::env::var_os("PATH").unwrap();
        let mut paths = std::env::split_paths(&path).collect::<VecDeque<_>>();
        paths.push_front(cargo_bin);
        let new_path = std::env::join_paths(paths)?;
        std::env::set_var("PATH", new_path);
    }

    let cargo_out_path = "cargo.out";
    let cargo_metadata_path = "cargo.metadata";
    if !args.reuse_cargo_out || !Path::new(cargo_out_path).exists() {
        generate_cargo_out(&cfg, cargo_out_path, cargo_metadata_path)
            .context("generate_cargo_out failed")?;
    }

    let crates =
        parse_cargo_out(cargo_out_path, cargo_metadata_path).context("parse_cargo_out failed")?;

    // Find out files.
    // Example: target.tmp/x86_64-unknown-linux-gnu/debug/build/metrics-d2dd799cebf1888d/out/event_details.rs
    let mut package_out_files: BTreeMap<String, Vec<PathBuf>> = BTreeMap::new();
    if cfg.package.iter().any(|(_, v)| v.copy_out) {
        for entry in glob::glob("target.tmp/**/build/*/out/*")? {
            match entry {
                Ok(path) => {
                    let package_name = || -> Option<_> {
                        let dir_name = path.parent()?.parent()?.file_name()?.to_str()?;
                        Some(dir_name.rsplit_once('-')?.0)
                    }()
                    .unwrap_or_else(|| panic!("failed to parse out file path: {:?}", path));
                    package_out_files
                        .entry(package_name.to_string())
                        .or_default()
                        .push(path.clone());
                }
                Err(e) => eprintln!("failed to check for out files: {}", e),
            }
        }
    }

    // Group by package.
    let mut module_by_package: BTreeMap<PathBuf, Vec<Crate>> = BTreeMap::new();
    for c in crates {
        module_by_package.entry(c.package_dir.clone()).or_default().push(c);
    }
    // Write an Android.bp file per package.
    for (package_dir, crates) in module_by_package {
        write_android_bp(
            &cfg,
            package_dir,
            &crates,
            package_out_files.get(&crates[0].package_name),
        )?;
    }

    Ok(())
}

fn run_cargo(cargo_out: &mut File, cmd: &mut Command) -> Result<()> {
    use std::os::unix::io::OwnedFd;
    use std::process::Stdio;
    let fd: OwnedFd = cargo_out.try_clone()?.into();
    // eprintln!("Running: {:?}\n", cmd);
    let output = cmd.stdout(Stdio::from(fd.try_clone()?)).stderr(Stdio::from(fd)).output()?;
    if !output.status.success() {
        bail!("cargo command failed with exit status: {:?}", output.status);
    }
    Ok(())
}

/// Run various cargo commands and save the output to `cargo_out_path`.
fn generate_cargo_out(cfg: &Config, cargo_out_path: &str, cargo_metadata_path: &str) -> Result<()> {
    let mut cargo_out_file = std::fs::File::create(cargo_out_path)?;
    let mut cargo_metadata_file = std::fs::File::create(cargo_metadata_path)?;

    let verbose_args = ["-v"];
    let target_dir_args = ["--target-dir", "target.tmp"];

    // cargo clean
    run_cargo(&mut cargo_out_file, Command::new("cargo").arg("clean").args(target_dir_args))?;

    let default_target = "x86_64-unknown-linux-gnu";
    let feature_args = if cfg.features.is_empty() {
        vec![]
    } else {
        vec!["--no-default-features".to_string(), "--features".to_string(), cfg.features.join(",")]
    };

    let workspace_args = if cfg.workspace {
        let mut v = vec!["--workspace".to_string()];
        if !cfg.workspace_excludes.is_empty() {
            for x in cfg.workspace_excludes.iter() {
                v.push("--exclude".to_string());
                v.push(x.clone());
            }
        }
        v
    } else {
        vec![]
    };

    // cargo metadata
    run_cargo(
        &mut cargo_metadata_file,
        Command::new("cargo")
            .arg("metadata")
            .arg("-q") // don't output warnings to stderr
            .arg("--format-version")
            .arg("1")
            .args(&feature_args),
    )?;

    // cargo build
    run_cargo(
        &mut cargo_out_file,
        Command::new("cargo")
            .args(["build", "--target", default_target])
            .args(verbose_args)
            .args(target_dir_args)
            .args(&workspace_args)
            .args(&feature_args),
    )?;

    if cfg.tests {
        // cargo build --tests
        run_cargo(
            &mut cargo_out_file,
            Command::new("cargo")
                .args(["build", "--target", default_target, "--tests"])
                .args(verbose_args)
                .args(target_dir_args)
                .args(&workspace_args)
                .args(&feature_args),
        )?;
    }

    Ok(())
}

/// Create the Android.bp file for `package_dir`.
fn write_android_bp(
    cfg: &Config,
    package_dir: PathBuf,
    crates: &[Crate],
    out_files: Option<&Vec<PathBuf>>,
) -> Result<()> {
    let bp_path = package_dir.join("Android.bp");

    let package_name = crates[0].package_name.clone();
    let def = PackageConfig::default();
    let package_cfg = cfg.package.get(&package_name).unwrap_or(&def);

    // Keep the old license header.
    let license_section = match std::fs::read_to_string(&bp_path) {
        Ok(s) => s
            .lines()
            .skip_while(|l| l.starts_with("//"))
            .take_while(|l| !l.starts_with("rust_") && !l.starts_with("genrule {"))
            .collect::<Vec<&str>>()
            .join("\n"),
        Err(e) if e.kind() == std::io::ErrorKind::NotFound => "// TODO: Add license.\n".to_string(),
        Err(e) => bail!("error when reading {bp_path:?}: {e}"),
    };

    let mut bp_contents = String::new();
    bp_contents += "// This file is generated by cargo_embargo.\n";
    bp_contents += "// Do not modify this file as changes will be overridden on upgrade.\n\n";
    bp_contents += license_section.trim();
    bp_contents += "\n";

    let mut modules = Vec::new();

    let extra_srcs = match (package_cfg.copy_out, out_files) {
        (true, Some(out_files)) => {
            let out_dir = package_dir.join("out");
            if !out_dir.exists() {
                std::fs::create_dir(&out_dir).expect("failed to create out dir");
            }

            let mut outs: Vec<String> = Vec::new();
            for f in out_files.iter() {
                let dest = out_dir.join(f.file_name().unwrap());
                std::fs::copy(f, dest).expect("failed to copy out file");
                outs.push(f.file_name().unwrap().to_str().unwrap().to_string());
            }

            let mut m = BpModule::new("genrule".to_string());
            let module_name = format!("copy_{}_build_out", package_name);
            m.props.set("name", module_name.clone());
            m.props.set("srcs", vec!["out/*"]);
            m.props.set("cmd", "cp $(in) $(genDir)");
            m.props.set("out", outs);
            modules.push(m);

            vec![":".to_string() + &module_name]
        }
        _ => vec![],
    };

    for c in crates {
        modules.extend(crate_to_bp_modules(c, cfg, package_cfg, &extra_srcs).with_context(
            || {
                format!(
                    "failed to generate bp module for crate \"{}\" with package name \"{}\"",
                    c.name, c.package_name
                )
            },
        )?);
    }
    if modules.is_empty() {
        return Ok(());
    }

    // In some cases there are nearly identical rustc invocations that that get processed into
    // identical BP modules. So far, dedup'ing them is a good enough fix. At some point we might
    // need something more complex, maybe like cargo2android's logic for merging crates.
    modules.sort();
    modules.dedup();

    modules.sort_by_key(|m| m.props.get_string("name").to_string());
    for m in modules {
        m.write(&mut bp_contents)?;
        bp_contents += "\n";
    }
    if let Some(path) = &package_cfg.add_toplevel_block {
        bp_contents +=
            &std::fs::read_to_string(path).with_context(|| format!("failed to read {path:?}"))?;
        bp_contents += "\n";
    }
    File::create(&bp_path)?.write_all(bp_contents.as_bytes())?;

    let bpfmt_output = Command::new("bpfmt").arg("-w").arg(&bp_path).output()?;
    if !bpfmt_output.status.success() {
        eprintln!(
            "WARNING: bpfmt -w {:?} failed before patch: {}",
            bp_path,
            String::from_utf8_lossy(&bpfmt_output.stderr)
        );
    }

    if let Some(patch_path) = &package_cfg.patch {
        let patch_output =
            Command::new("patch").arg("-s").arg(&bp_path).arg(patch_path).output()?;
        if !patch_output.status.success() {
            eprintln!("WARNING: failed to apply patch {:?}", patch_path);
        }
        // Re-run bpfmt after the patch so
        let bpfmt_output = Command::new("bpfmt").arg("-w").arg(&bp_path).output()?;
        if !bpfmt_output.status.success() {
            eprintln!(
                "WARNING: bpfmt -w {:?} failed after patch: {}",
                bp_path,
                String::from_utf8_lossy(&bpfmt_output.stderr)
            );
        }
    }

    Ok(())
}

/// Convert a `Crate` into `BpModule`s.
///
/// If messy business logic is necessary, prefer putting it here.
fn crate_to_bp_modules(
    crate_: &Crate,
    cfg: &Config,
    package_cfg: &PackageConfig,
    extra_srcs: &[String],
) -> Result<Vec<BpModule>> {
    let mut modules = Vec::new();
    for crate_type in &crate_.types {
        let host = if package_cfg.device_supported.unwrap_or(true) { "" } else { "_host" };
        let rlib = if package_cfg.force_rlib { "_rlib" } else { "" };
        let (module_type, module_name, stem) = match crate_type {
            CrateType::Bin => {
                ("rust_binary".to_string() + host, crate_.name.clone(), crate_.name.clone())
            }
            CrateType::Lib | CrateType::RLib => {
                let stem = "lib".to_string() + &crate_.name;
                ("rust_library".to_string() + rlib + host, stem.clone(), stem)
            }
            CrateType::DyLib => {
                let stem = "lib".to_string() + &crate_.name;
                ("rust_library".to_string() + host + "_dylib", stem.clone() + "_dylib", stem)
            }
            CrateType::CDyLib => {
                let stem = "lib".to_string() + &crate_.name;
                ("rust_ffi".to_string() + host + "_shared", stem.clone() + "_shared", stem)
            }
            CrateType::StaticLib => {
                let stem = "lib".to_string() + &crate_.name;
                ("rust_ffi".to_string() + host + "_static", stem.clone() + "_static", stem)
            }
            CrateType::ProcMacro => {
                let stem = "lib".to_string() + &crate_.name;
                ("rust_proc_macro".to_string(), stem.clone(), stem)
            }
            CrateType::Test | CrateType::TestNoHarness => {
                let suffix = crate_.main_src.to_string_lossy().into_owned();
                let suffix = suffix.replace('/', "_").replace(".rs", "");
                let stem = crate_.package_name.clone() + "_test_" + &suffix;
                if crate_type == &CrateType::TestNoHarness {
                    eprintln!(
                        "WARNING: ignoring test \"{}\" with harness=false. not supported yet",
                        stem
                    );
                    return Ok(Vec::new());
                }
                ("rust_test".to_string() + host, stem.clone(), stem)
            }
        };

        let mut m = BpModule::new(module_type.clone());
        let module_name = cfg.module_name_overrides.get(&module_name).unwrap_or(&module_name);
        if cfg.module_blocklist.contains(module_name) {
            continue;
        }
        m.props.set("name", module_name.clone());
        if &stem != module_name {
            m.props.set("stem", stem);
        }

        if let Some(defaults) = &cfg.global_defaults {
            m.props.set("defaults", vec![defaults.clone()]);
        }

        if package_cfg.host_supported.unwrap_or(true)
            && package_cfg.device_supported.unwrap_or(true)
            && module_type != "rust_proc_macro"
        {
            m.props.set("host_supported", true);
        }

        m.props.set("crate_name", crate_.name.clone());
        m.props.set("cargo_env_compat", true);

        if let Some(version) = &crate_.version {
            m.props.set("cargo_pkg_version", version.clone());
        }

        if crate_.types.contains(&CrateType::Test) {
            m.props.set("test_suites", vec!["general-tests"]);
            m.props.set("auto_gen_config", true);
            if package_cfg.host_supported.unwrap_or(true) {
                m.props.object("test_options").set("unit_test", !package_cfg.no_presubmit);
            }
        }

        let mut srcs = vec![crate_.main_src.to_string_lossy().to_string()];
        srcs.extend(extra_srcs.iter().cloned());
        m.props.set("srcs", srcs);

        m.props.set("edition", crate_.edition.clone());
        if !crate_.features.is_empty() {
            m.props.set("features", crate_.features.clone());
        }
        if !crate_.cfgs.is_empty() {
            m.props.set("cfgs", crate_.cfgs.clone());
        }

        let mut flags = Vec::new();
        if !crate_.cap_lints.is_empty() {
            flags.push(crate_.cap_lints.clone());
        }
        flags.extend(crate_.codegens.clone());
        if !flags.is_empty() {
            m.props.set("flags", flags);
        }

        let mut rust_libs = Vec::new();
        let mut proc_macro_libs = Vec::new();
        for (extern_name, filename) in &crate_.externs {
            if extern_name == "proc_macro" {
                continue;
            }
            let filename =
                filename.as_ref().unwrap_or_else(|| panic!("no filename for {}", extern_name));
            // Example filename: "libgetrandom-fd8800939535fc59.rmeta"
            static REGEX: Lazy<Regex> =
                Lazy::new(|| Regex::new(r"^lib(.*)-[0-9a-f]*.(rlib|so|rmeta)$").unwrap());
            let lib_name = if let Some(x) = REGEX.captures(filename).and_then(|x| x.get(1)) {
                x
            } else {
                bail!("bad filename for extern {}: {}", extern_name, filename);
            };
            if filename.ends_with(".rlib") || filename.ends_with(".rmeta") {
                rust_libs.push(lib_name.as_str().to_string());
            } else if filename.ends_with(".so") {
                // Assume .so files are always proc_macros. May not always be right.
                proc_macro_libs.push(lib_name.as_str().to_string());
            } else {
                unreachable!();
            }
        }

        // Add "lib" prefix and apply name overrides.
        let process_lib_deps = |libs: Vec<String>| -> Vec<String> {
            let mut result = Vec::new();
            for x in libs {
                let module_name = "lib".to_string() + x.as_str();
                let module_name =
                    cfg.module_name_overrides.get(&module_name).unwrap_or(&module_name);
                if package_cfg.dep_blocklist.contains(module_name) {
                    continue;
                }
                result.push(module_name.to_string());
            }
            result.sort();
            result
        };
        if !rust_libs.is_empty() {
            m.props.set("rustlibs", process_lib_deps(rust_libs));
        }
        if !proc_macro_libs.is_empty() {
            m.props.set("proc_macros", process_lib_deps(proc_macro_libs));
        }
        if !crate_.static_libs.is_empty() {
            m.props.set("static_libs", process_lib_deps(crate_.static_libs.clone()));
        }
        if !crate_.shared_libs.is_empty() {
            m.props.set("shared_libs", process_lib_deps(crate_.shared_libs.clone()));
        }

        if !cfg.apex_available.is_empty()
            && [
                CrateType::Lib,
                CrateType::RLib,
                CrateType::DyLib,
                CrateType::CDyLib,
                CrateType::StaticLib,
            ]
            .contains(crate_type)
        {
            m.props.set("apex_available", cfg.apex_available.clone());
        }

        if let Some(visibility) = cfg.module_visibility.get(module_name) {
            m.props.set("visibility", visibility.clone());
        }

        if let Some(path) = &package_cfg.add_module_block {
            let content = std::fs::read_to_string(path)
                .with_context(|| format!("failed to read {path:?}"))?;
            m.props.raw_block = Some(content);
        }

        modules.push(m);
    }
    Ok(modules)
}
