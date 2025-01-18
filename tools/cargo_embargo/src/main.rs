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
mod cargo;
mod config;

use crate::config::Config;
use crate::config::PackageConfig;
use crate::config::PackageVariantConfig;
use crate::config::VariantConfig;
use anyhow::anyhow;
use anyhow::bail;
use anyhow::Context;
use anyhow::Result;
use bp::*;
use cargo::{
    cargo_out::parse_cargo_out, metadata::parse_cargo_metadata_str, Crate, CrateType, ExternType,
};
use clap::Parser;
use clap::Subcommand;
use log::debug;
use nix::fcntl::OFlag;
use nix::unistd::pipe2;
use std::collections::BTreeMap;
use std::collections::VecDeque;
use std::env;
use std::fs::{read_to_string, write, File};
use std::io::{Read, Write};
use std::path::Path;
use std::path::PathBuf;
use std::process::{Command, Stdio};
use std::sync::LazyLock;
use tempfile::tempdir;

// Major TODOs
//  * handle errors, esp. in cargo.out parsing. they should fail the program with an error code
//  * handle warnings. put them in comments in the android.bp, some kind of report section

/// Rust modules which shouldn't use the default generated names, to avoid conflicts or confusion.
pub static RENAME_MAP: LazyLock<BTreeMap<&str, &str>> = LazyLock::new(|| {
    [
        ("libash", "libash_rust"),
        ("libatomic", "libatomic_rust"),
        ("libbacktrace", "libbacktrace_rust"),
        ("libbase", "libbase_rust"),
        ("libbase64", "libbase64_rust"),
        ("libfuse", "libfuse_rust"),
        ("libgcc", "libgcc_rust"),
        ("liblog", "liblog_rust"),
        ("libminijail", "libminijail_rust"),
        ("libsync", "libsync_rust"),
        ("libx86_64", "libx86_64_rust"),
        ("libxml", "libxml_rust"),
        ("protoc_gen_rust", "protoc-gen-rust"),
    ]
    .into_iter()
    .collect()
});

/// This map tracks Rust crates that have special rules.mk modules that were not
/// generated automatically by this script. Examples include compiler builtins
/// and other foundational libraries. It also tracks the location of rules.mk
/// build files for crates that are not under external/rust/crates.
pub static RULESMK_RENAME_MAP: LazyLock<BTreeMap<&str, &str>> = LazyLock::new(|| {
    [
        ("liballoc", "trusty/user/base/lib/liballoc-rust"),
        ("libcompiler_builtins", "trusty/user/base/lib/libcompiler_builtins-rust"),
        ("libcore", "trusty/user/base/lib/libcore-rust"),
        ("libhashbrown", "trusty/user/base/lib/libhashbrown-rust"),
        ("libpanic_abort", "trusty/user/base/lib/libpanic_abort-rust"),
        ("libstd", "trusty/user/base/lib/libstd-rust"),
        ("libstd_detect", "trusty/user/base/lib/libstd_detect-rust"),
        ("libunwind", "trusty/user/base/lib/libunwind-rust"),
    ]
    .into_iter()
    .collect()
});

/// Given a proposed module name, returns `None` if it is blocked by the given config, or
/// else apply any name overrides and returns the name to use.
fn override_module_name(
    module_name: &str,
    blocklist: &[String],
    module_name_overrides: &BTreeMap<String, String>,
    rename_map: &BTreeMap<&str, &str>,
) -> Option<String> {
    if blocklist.iter().any(|blocked_name| blocked_name == module_name) {
        None
    } else if let Some(overridden_name) = module_name_overrides.get(module_name) {
        Some(overridden_name.to_string())
    } else if let Some(renamed) = rename_map.get(module_name) {
        Some(renamed.to_string())
    } else {
        Some(module_name.to_string())
    }
}

/// Command-line parameters for `cargo_embargo`.
#[derive(Parser, Debug)]
struct Args {
    /// Use the cargo binary in the `cargo_bin` directory. Defaults to using the Android prebuilt.
    #[clap(long)]
    cargo_bin: Option<PathBuf>,
    /// Store `cargo build` output in this directory. If not set, a temporary directory is created and used.
    #[clap(long)]
    cargo_out_dir: Option<PathBuf>,
    /// Skip the `cargo build` commands and reuse the "cargo.out" file from a previous run if
    /// available. Requires setting --cargo_out_dir.
    #[clap(long)]
    reuse_cargo_out: bool,
    #[command(subcommand)]
    mode: Mode,
}

#[derive(Clone, Debug, Subcommand)]
enum Mode {
    /// Generates `Android.bp` files for the crates under the current directory using the given
    /// config file.
    Generate {
        /// `cargo_embargo.json` config file to use.
        config: PathBuf,
    },
    /// Dumps information about the crates to the given JSON file.
    DumpCrates {
        /// `cargo_embargo.json` config file to use.
        config: PathBuf,
        /// Path to `crates.json` to output.
        crates: PathBuf,
    },
    /// Tries to automatically generate a suitable `cargo_embargo.json` config file for the package
    /// in the current directory.
    Autoconfig {
        /// `cargo_embargo.json` config file to create.
        config: PathBuf,
    },
}

fn main() -> Result<()> {
    env_logger::init();
    let args = Args::parse();

    if args.reuse_cargo_out && args.cargo_out_dir.is_none() {
        return Err(anyhow!("Must specify --cargo_out_dir with --reuse_cargo_out"));
    }
    let tempdir = tempdir()?;
    let intermediates_dir = args.cargo_out_dir.as_deref().unwrap_or(tempdir.path());

    match &args.mode {
        Mode::DumpCrates { config, crates } => {
            dump_crates(&args, config, crates, intermediates_dir)?;
        }
        Mode::Generate { config } => {
            run_embargo(&args, config, intermediates_dir)?;
        }
        Mode::Autoconfig { config } => {
            autoconfig(&args, config, intermediates_dir)?;
        }
    }

    Ok(())
}

/// Runs cargo_embargo with the given JSON configuration string, but dumps the crate data to the
/// given `crates.json` file rather than generating an `Android.bp`.
fn dump_crates(
    args: &Args,
    config_filename: &Path,
    crates_filename: &Path,
    intermediates_dir: &Path,
) -> Result<()> {
    let cfg = Config::from_file(config_filename)?;
    let crates = make_all_crates(args, &cfg, intermediates_dir)?;
    serde_json::to_writer(
        File::create(crates_filename)
            .with_context(|| format!("Failed to create {:?}", crates_filename))?,
        &crates,
    )?;
    Ok(())
}

/// Tries to automatically generate a suitable `cargo_embargo.json` for the package in the current
/// directory.
fn autoconfig(args: &Args, config_filename: &Path, intermediates_dir: &Path) -> Result<()> {
    println!("Trying default config with tests...");
    let mut config_with_build = Config {
        variants: vec![VariantConfig { tests: true, ..Default::default() }],
        package: Default::default(),
    };
    let mut crates_with_build = make_all_crates(args, &config_with_build, intermediates_dir)?;

    let has_tests =
        crates_with_build[0].iter().any(|c| c.types.contains(&CrateType::Test) && !c.empty_test);
    if !has_tests {
        println!("No tests, removing from config.");
        config_with_build =
            Config { variants: vec![Default::default()], package: Default::default() };
        crates_with_build = make_all_crates(args, &config_with_build, intermediates_dir)?;
    }

    println!("Trying without cargo build...");
    let config_no_build = Config {
        variants: vec![VariantConfig { run_cargo: false, tests: has_tests, ..Default::default() }],
        package: Default::default(),
    };
    let crates_without_build = make_all_crates(args, &config_no_build, intermediates_dir)?;

    let config = if crates_with_build == crates_without_build {
        println!("Output without build was the same, using that.");
        config_no_build
    } else {
        println!("Output without build was different. Need to run cargo build.");
        println!("With build: {}", serde_json::to_string_pretty(&crates_with_build)?);
        println!("Without build: {}", serde_json::to_string_pretty(&crates_without_build)?);
        config_with_build
    };
    write(config_filename, format!("{}\n", config.to_json_string()?))?;
    println!(
        "Wrote config to {0}. Run `cargo_embargo generate {0}` to use it.",
        config_filename.to_string_lossy()
    );

    Ok(())
}

/// Finds the path to the directory containing the Android prebuilt Rust toolchain.
fn find_android_rust_toolchain() -> Result<PathBuf> {
    let platform_rustfmt = if cfg!(all(target_arch = "x86_64", target_os = "linux")) {
        "linux-x86/stable/rustfmt"
    } else if cfg!(all(target_arch = "x86_64", target_os = "macos")) {
        "darwin-x86/stable/rustfmt"
    } else if cfg!(all(target_arch = "x86_64", target_os = "windows")) {
        "windows-x86/stable/rustfmt.exe"
    } else {
        bail!("No prebuilt Rust toolchain available for this platform.");
    };

    let android_top = env::var("ANDROID_BUILD_TOP")
        .context("ANDROID_BUILD_TOP was not set. Did you forget to run envsetup.sh?")?;
    let stable_rustfmt = [android_top.as_str(), "prebuilts", "rust", platform_rustfmt]
        .into_iter()
        .collect::<PathBuf>();
    let canonical_rustfmt = stable_rustfmt.canonicalize()?;
    Ok(canonical_rustfmt.parent().unwrap().to_owned())
}

/// Adds the given path to the start of the `PATH` environment variable.
fn add_to_path(extra_path: PathBuf) -> Result<()> {
    let path = env::var_os("PATH").unwrap();
    let mut paths = env::split_paths(&path).collect::<VecDeque<_>>();
    paths.push_front(extra_path);
    let new_path = env::join_paths(paths)?;
    debug!("Set PATH to {:?}", new_path);
    std::env::set_var("PATH", new_path);
    Ok(())
}

/// Calls make_crates for each variant in the given config.
fn make_all_crates(args: &Args, cfg: &Config, intermediates_dir: &Path) -> Result<Vec<Vec<Crate>>> {
    cfg.variants.iter().map(|variant| make_crates(args, variant, intermediates_dir)).collect()
}

fn make_crates(args: &Args, cfg: &VariantConfig, intermediates_dir: &Path) -> Result<Vec<Crate>> {
    if !Path::new("Cargo.toml").try_exists().context("when checking Cargo.toml")? {
        bail!("Cargo.toml missing. Run in a directory with a Cargo.toml file.");
    }

    // Add the custom cargo to PATH.
    // NOTE: If the directory with cargo has more binaries, this could have some unpredictable side
    // effects. That is partly intended though, because we want to use that cargo binary's
    // associated rustc.
    let cargo_bin = if let Some(cargo_bin) = &args.cargo_bin {
        cargo_bin.to_owned()
    } else {
        // Find the Android prebuilt.
        find_android_rust_toolchain()?
    };
    add_to_path(cargo_bin)?;

    let cargo_out_path = intermediates_dir.join("cargo.out");
    let cargo_metadata_path = intermediates_dir.join("cargo.metadata");
    let cargo_output = if args.reuse_cargo_out && cargo_out_path.exists() {
        CargoOutput {
            cargo_out: read_to_string(cargo_out_path)?,
            cargo_metadata: read_to_string(cargo_metadata_path)?,
        }
    } else {
        let cargo_output =
            generate_cargo_out(cfg, intermediates_dir).context("generate_cargo_out failed")?;
        if cfg.run_cargo {
            write(cargo_out_path, &cargo_output.cargo_out)?;
        }
        write(cargo_metadata_path, &cargo_output.cargo_metadata)?;
        cargo_output
    };

    if cfg.run_cargo {
        parse_cargo_out(&cargo_output).context("parse_cargo_out failed")
    } else {
        parse_cargo_metadata_str(&cargo_output.cargo_metadata, cfg)
    }
}

/// Runs cargo_embargo with the given JSON configuration file.
fn run_embargo(args: &Args, config_filename: &Path, intermediates_dir: &Path) -> Result<()> {
    let intermediates_glob = intermediates_dir
        .to_str()
        .ok_or(anyhow!("Failed to convert intermediate dir path to string"))?
        .to_string()
        + "/target.tmp/**/build/*/out/*";

    let cfg = Config::from_file(config_filename)?;
    let crates = make_all_crates(args, &cfg, intermediates_dir)?;

    // TODO: Use different directories for different variants.
    // Find out files.
    // Example: target.tmp/x86_64-unknown-linux-gnu/debug/build/metrics-d2dd799cebf1888d/out/event_details.rs
    let num_variants = cfg.variants.len();
    let mut package_out_files: BTreeMap<String, Vec<Vec<PathBuf>>> = BTreeMap::new();
    for (variant_index, variant_cfg) in cfg.variants.iter().enumerate() {
        if variant_cfg.package.iter().any(|(_, v)| v.copy_out) {
            for entry in glob::glob(&intermediates_glob)? {
                match entry {
                    Ok(path) => {
                        let package_name = || -> Option<_> {
                            let dir_name = path.parent()?.parent()?.file_name()?.to_str()?;
                            Some(dir_name.rsplit_once('-')?.0)
                        }()
                        .unwrap_or_else(|| panic!("failed to parse out file path: {:?}", path));
                        package_out_files
                            .entry(package_name.to_string())
                            .or_insert_with(|| vec![vec![]; num_variants])[variant_index]
                            .push(path.clone());
                    }
                    Err(e) => eprintln!("failed to check for out files: {}", e),
                }
            }
        }
    }

    // If we were configured to run cargo, check whether we could have got away without it.
    if cfg.variants.iter().any(|variant| variant.run_cargo) && package_out_files.is_empty() {
        let mut cfg_no_cargo = cfg.clone();
        for variant in &mut cfg_no_cargo.variants {
            variant.run_cargo = false;
        }
        let crates_no_cargo = make_all_crates(args, &cfg_no_cargo, intermediates_dir)?;
        if crates_no_cargo == crates {
            eprintln!("Running cargo appears to be unnecessary for this crate, consider adding `\"run_cargo\": false` to your cargo_embargo.json.");
        }
    }

    write_all_build_files(&cfg, crates, &package_out_files)
}

/// Input is indexed by variant, then all crates for that variant.
/// Output is a map from package directory to a list of variants, with all crates for that package
/// and variant.
fn group_by_package(crates: Vec<Vec<Crate>>) -> BTreeMap<PathBuf, Vec<Vec<Crate>>> {
    let mut module_by_package: BTreeMap<PathBuf, Vec<Vec<Crate>>> = BTreeMap::new();

    let num_variants = crates.len();
    for (i, variant_crates) in crates.into_iter().enumerate() {
        for c in variant_crates {
            let package_variants = module_by_package
                .entry(c.package_dir.clone())
                .or_insert_with(|| vec![vec![]; num_variants]);
            package_variants[i].push(c);
        }
    }
    module_by_package
}

fn write_all_build_files(
    cfg: &Config,
    crates: Vec<Vec<Crate>>,
    package_out_files: &BTreeMap<String, Vec<Vec<PathBuf>>>,
) -> Result<()> {
    // Group by package.
    let module_by_package = group_by_package(crates);

    let num_variants = cfg.variants.len();
    let empty_package_out_files = vec![vec![]; num_variants];
    let mut has_error = false;
    // Write a build file per package.
    for (package_dir, crates) in module_by_package {
        let package_name = &crates.iter().flatten().next().unwrap().package_name;
        if let Err(e) = write_build_files(
            cfg,
            package_name,
            package_dir,
            &crates,
            package_out_files.get(package_name).unwrap_or(&empty_package_out_files),
        ) {
            // print the error, but continue to accumulate all of the errors
            eprintln!("ERROR: {:#}", e);
            has_error = true;
        }
    }
    if has_error {
        panic!("Encountered fatal errors that must be fixed.");
    }

    Ok(())
}

/// Runs the given command, and returns its standard output and (optionally) standard error as a string.
fn run_cargo(cmd: &mut Command, include_stderr: bool) -> Result<String> {
    let (pipe_read, pipe_write) = pipe2(OFlag::O_CLOEXEC)?;
    if include_stderr {
        cmd.stderr(pipe_write.try_clone()?);
    }
    cmd.stdout(pipe_write).stdin(Stdio::null());
    debug!("Running: {:?}\n", cmd);
    let mut child = cmd.spawn()?;

    // Unset the stdout and stderr for the command so that they are dropped in this process.
    // Otherwise the `read_to_string` below will block forever as there is still an open write file
    // descriptor for the pipe even after the child finishes.
    cmd.stderr(Stdio::null()).stdout(Stdio::null());

    let mut output = String::new();
    File::from(pipe_read).read_to_string(&mut output)?;
    let status = child.wait()?;
    if !status.success() {
        bail!(
            "cargo command `{:?}` failed with exit status: {:?}.\nOutput: \n------\n{}\n------",
            cmd,
            status,
            output
        );
    }

    Ok(output)
}

/// The raw output from running `cargo metadata`, `cargo build` and other commands.
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct CargoOutput {
    cargo_metadata: String,
    cargo_out: String,
}

/// Run various cargo commands and returns the output.
fn generate_cargo_out(cfg: &VariantConfig, intermediates_dir: &Path) -> Result<CargoOutput> {
    let verbose_args = ["-v"];
    let target_dir = intermediates_dir.join("target.tmp");

    // cargo clean
    run_cargo(Command::new("cargo").arg("clean").arg("--target-dir").arg(&target_dir), true)
        .context("Running cargo clean")?;

    let default_target = "x86_64-unknown-linux-gnu";
    let feature_args = if let Some(features) = &cfg.features {
        if features.is_empty() {
            vec!["--no-default-features".to_string()]
        } else {
            vec!["--no-default-features".to_string(), "--features".to_string(), features.join(",")]
        }
    } else {
        vec![]
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
    let cargo_metadata = run_cargo(
        Command::new("cargo")
            .arg("metadata")
            .arg("-q") // don't output warnings to stderr
            .arg("--format-version")
            .arg("1")
            .args(&feature_args),
        false,
    )
    .context("Running cargo metadata")?;

    let mut cargo_out = String::new();
    if cfg.run_cargo {
        let envs = if cfg.extra_cfg.is_empty() {
            vec![]
        } else {
            vec![(
                "RUSTFLAGS",
                cfg.extra_cfg
                    .iter()
                    .map(|cfg_flag| format!("--cfg {}", cfg_flag))
                    .collect::<Vec<_>>()
                    .join(" "),
            )]
        };

        // cargo build
        cargo_out += &run_cargo(
            Command::new("cargo")
                .envs(envs.clone())
                .args(["build", "--target", default_target])
                .args(verbose_args)
                .arg("--target-dir")
                .arg(&target_dir)
                .args(&workspace_args)
                .args(&feature_args),
            true,
        )?;

        if cfg.tests {
            // cargo build --tests
            cargo_out += &run_cargo(
                Command::new("cargo")
                    .envs(envs.clone())
                    .args(["build", "--target", default_target, "--tests"])
                    .args(verbose_args)
                    .arg("--target-dir")
                    .arg(&target_dir)
                    .args(&workspace_args)
                    .args(&feature_args),
                true,
            )?;
            // cargo test -- --list
            cargo_out += &run_cargo(
                Command::new("cargo")
                    .envs(envs)
                    .args(["test", "--target", default_target])
                    .arg("--target-dir")
                    .arg(&target_dir)
                    .args(&workspace_args)
                    .args(&feature_args)
                    .args(["--", "--list"]),
                true,
            )?;
        }
    }

    Ok(CargoOutput { cargo_metadata, cargo_out })
}

/// Read and return license and other header lines from a build file.
///
/// Skips initial comment lines, then returns all lines before the first line
/// starting with `rust_`, `genrule {`, or `LOCAL_DIR`.
///
/// If `path` could not be read and a license is required, return a
/// placeholder license TODO line.
fn read_license_header(path: &Path, require_license: bool) -> Result<String> {
    // Keep the old license header.
    match std::fs::read_to_string(path) {
        Ok(s) => Ok(s
            .lines()
            .skip_while(|l| l.starts_with("//") || l.starts_with('#'))
            .take_while(|l| {
                !l.starts_with("rust_")
                    && !l.starts_with("genrule {")
                    && !l.starts_with("LOCAL_DIR")
            })
            .collect::<Vec<&str>>()
            .join("\n")),
        Err(e) if e.kind() == std::io::ErrorKind::NotFound => {
            let placeholder = if require_license {
                "// DO NOT SUBMIT: Add license before submitting.\n"
            } else {
                ""
            };
            Ok(placeholder.to_string())
        }
        Err(e) => Err(anyhow!("error when reading {path:?}: {e}")),
    }
}

/// Create the build file for `package_dir`.
///
/// `crates` and `out_files` are both indexed by variant.
fn write_build_files(
    cfg: &Config,
    package_name: &str,
    package_dir: PathBuf,
    crates: &[Vec<Crate>],
    out_files: &[Vec<PathBuf>],
) -> Result<()> {
    assert_eq!(crates.len(), out_files.len());

    let mut bp_contents = String::new();
    let mut mk_contents = String::new();
    for (variant_index, variant_config) in cfg.variants.iter().enumerate() {
        let variant_crates = &crates[variant_index];
        let def = PackageVariantConfig::default();
        let package_variant_cfg = variant_config.package.get(package_name).unwrap_or(&def);

        // If `copy_out` is enabled and there are any generated out files for the package, copy them to
        // the appropriate directory.
        if package_variant_cfg.copy_out && !out_files[variant_index].is_empty() {
            let out_dir = package_dir.join("out");
            if !out_dir.exists() {
                std::fs::create_dir(&out_dir).expect("failed to create out dir");
            }

            for f in out_files[variant_index].iter() {
                let dest = out_dir.join(f.file_name().unwrap());
                std::fs::copy(f, dest).expect("failed to copy out file");
            }
        }

        if variant_config.generate_androidbp {
            bp_contents += &generate_android_bp(
                variant_config,
                package_variant_cfg,
                package_name,
                variant_crates,
                &out_files[variant_index],
            )?;
        }
        if variant_config.generate_rulesmk {
            mk_contents += &generate_rules_mk(
                variant_config,
                package_variant_cfg,
                package_name,
                variant_crates,
                &out_files[variant_index],
            )?;
        }
    }
    let main_module_name_overrides = &cfg.variants.first().unwrap().module_name_overrides;
    if !mk_contents.is_empty() {
        // If rules.mk is generated, then make it accessible via dirgroup.
        bp_contents += &generate_android_bp_for_rules_mk(package_name, main_module_name_overrides)?;
    }

    let def = PackageConfig::default();
    let package_cfg = cfg.package.get(package_name).unwrap_or(&def);
    if let Some(path) = &package_cfg.add_toplevel_block {
        bp_contents +=
            &std::fs::read_to_string(path).with_context(|| format!("failed to read {path:?}"))?;
        bp_contents += "\n";
    }
    if !bp_contents.is_empty() {
        let output_path = package_dir.join("Android.bp");
        let package_header = generate_android_bp_package_header(
            package_name,
            package_cfg,
            read_license_header(&output_path, true)?.trim(),
            crates,
            main_module_name_overrides,
        )?;
        let bp_contents = package_header + &bp_contents;
        write_format_android_bp(&output_path, &bp_contents, package_cfg.patch.as_deref())?;
    }
    if !mk_contents.is_empty() {
        let output_path = package_dir.join("rules.mk");
        let mk_contents = "# This file is generated by cargo_embargo.\n".to_owned()
            + "# Do not modify this file after the LOCAL_DIR line\n"
            + "# because the changes will be overridden on upgrade.\n"
            + "# Content before the first line starting with LOCAL_DIR is preserved.\n"
            + read_license_header(&output_path, false)?.trim()
            + "\n"
            + &mk_contents;
        File::create(&output_path)?.write_all(mk_contents.as_bytes())?;
        if let Some(patch) = package_cfg.rulesmk_patch.as_deref() {
            apply_patch_file(&output_path, patch)?;
        }
    }

    Ok(())
}

fn generate_android_bp_package_header(
    package_name: &str,
    package_cfg: &PackageConfig,
    license_header: &str,
    crates: &[Vec<Crate>],
    module_name_overrides: &BTreeMap<String, String>,
) -> Result<String> {
    let crates = crates.iter().flatten().collect::<Vec<_>>();
    if let Some(first) = crates.first() {
        if let Some(license) = first.license.as_ref() {
            if crates.iter().all(|c| c.license.as_ref() == Some(license)) {
                let mut modules = Vec::new();
                let licenses = choose_licenses(license)?;

                let default_license_name = format!("external_rust_crates_{}_license", package_name);

                let license_name = match override_module_name(
                    &default_license_name,
                    &[],
                    module_name_overrides,
                    &RENAME_MAP,
                ) {
                    Some(x) => x,
                    None => default_license_name,
                };

                let mut package_module = BpModule::new("package".to_string());
                package_module.props.set("default_team", "trendy_team_android_rust");
                package_module.props.set("default_applicable_licenses", vec![license_name.clone()]);
                modules.push(package_module);

                let mut license_module = BpModule::new("license".to_string());
                license_module.props.set("name", license_name);
                license_module.props.set("visibility", vec![":__subpackages__"]);
                license_module.props.set(
                    "license_kinds",
                    licenses
                        .into_iter()
                        .map(|license| format!("SPDX-license-identifier-{}", license))
                        .collect::<Vec<_>>(),
                );
                let license_text = package_cfg.license_text.clone().unwrap_or_else(|| {
                    vec![first.license_file.as_deref().unwrap_or("LICENSE").to_string()]
                });
                license_module.props.set("license_text", license_text);
                modules.push(license_module);

                let mut bp_contents = "// This file is generated by cargo_embargo.\n".to_owned()
                + "// Do not modify this file because the changes will be overridden on upgrade.\n\n";
                for m in modules {
                    m.write(&mut bp_contents)?;
                    bp_contents += "\n";
                }
                return Ok(bp_contents);
            } else {
                eprintln!("Crates have different licenses.");
            }
        }
    }

    Ok("// This file is generated by cargo_embargo.\n".to_owned()
        + "// Do not modify this file after the first \"rust_*\" or \"genrule\" module\n"
        + "// because the changes will be overridden on upgrade.\n"
        + "// Content before the first \"rust_*\" or \"genrule\" module is preserved.\n\n"
        + license_header
        + "\n")
}

/// Given an SPDX license expression that may offer a choice between several licenses, choose one or
/// more to use.
fn choose_licenses(license: &str) -> Result<Vec<&str>> {
    Ok(match license {
        // Variations on "MIT OR Apache-2.0"
        "MIT OR Apache-2.0" => vec!["Apache-2.0"],
        "Apache-2.0 OR MIT" => vec!["Apache-2.0"],
        "MIT/Apache-2.0" => vec!["Apache-2.0"],
        "Apache-2.0/MIT" => vec!["Apache-2.0"],
        "Apache-2.0 / MIT" => vec!["Apache-2.0"],

        // Variations on "BSD-* OR Apache-2.0"
        "Apache-2.0 OR BSD-3-Clause" => vec!["Apache-2.0"],
        "Apache-2.0 or BSD-3-Clause" => vec!["Apache-2.0"],
        "BSD-3-Clause OR Apache-2.0" => vec!["Apache-2.0"],

        // Variations on "BSD-* OR MIT OR Apache-2.0"
        "BSD-3-Clause OR MIT OR Apache-2.0" => vec!["Apache-2.0"],
        "BSD-2-Clause OR Apache-2.0 OR MIT" => vec!["Apache-2.0"],

        // Variations on "Zlib OR MIT OR Apache-2.0"
        "Zlib OR Apache-2.0 OR MIT" => vec!["Apache-2.0"],
        "MIT OR Apache-2.0 OR Zlib" => vec!["Apache-2.0"],

        // Variations on "Apache-2.0 OR *"
        "Apache-2.0 OR BSL-1.0" => vec!["Apache-2.0"],
        "Apache-2.0 WITH LLVM-exception OR Apache-2.0 OR MIT" => vec!["Apache-2.0"],

        // Variations on "Unlicense OR MIT"
        "Unlicense OR MIT" => vec!["MIT"],
        "Unlicense/MIT" => vec!["MIT"],

        // Multiple licenses.
        "(MIT OR Apache-2.0) AND Unicode-DFS-2016" => vec!["Apache-2.0", "Unicode-DFS-2016"],
        "MIT AND BSD-3-Clause" => vec!["BSD-3-Clause", "MIT"],
        // Usually we interpret "/" as "OR", but in the case of libfuzzer-sys, closer
        // inspection of the terms indicates the correct interpretation is "(MIT OR APACHE) AND NCSA".
        "MIT/Apache-2.0/NCSA" => vec!["Apache-2.0", "NCSA"],

        // Other cases.
        "MIT OR LGPL-3.0-or-later" => vec!["MIT"],
        "MIT/BSD-3-Clause" => vec!["MIT"],
        "MIT AND (MIT OR Apache-2.0)" => vec!["MIT"],

        "LGPL-2.1-only OR BSD-2-Clause" => vec!["BSD-2-Clause"],
        _ => {
            // If there is whitespace, it is probably an SPDX expression.
            if license.contains(char::is_whitespace) {
                bail!("Unrecognized license: {license}");
            }
            vec![license]
        }
    })
}

/// Generates and returns a Soong Blueprint for the given set of crates, for a single variant of a
/// package.
fn generate_android_bp(
    cfg: &VariantConfig,
    package_cfg: &PackageVariantConfig,
    package_name: &str,
    crates: &[Crate],
    out_files: &[PathBuf],
) -> Result<String> {
    let mut bp_contents = String::new();

    let mut modules = Vec::new();

    let extra_srcs = if package_cfg.copy_out && !out_files.is_empty() {
        let outs: Vec<String> = out_files
            .iter()
            .map(|f| f.file_name().unwrap().to_str().unwrap().to_string())
            .collect();

        let mut m = BpModule::new("genrule".to_string());
        if let Some(module_name) = override_module_name(
            &format!("copy_{}_build_out", package_name),
            &cfg.module_blocklist,
            &cfg.module_name_overrides,
            &RENAME_MAP,
        ) {
            m.props.set("name", module_name.clone());
            m.props.set("srcs", vec!["out/*"]);
            m.props.set("cmd", "cp $(in) $(genDir)");
            m.props.set("out", outs);
            modules.push(m);

            vec![":".to_string() + &module_name]
        } else {
            vec![]
        }
    } else {
        vec![]
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

    // In some cases there are nearly identical rustc invocations that that get processed into
    // identical BP modules. So far, dedup'ing them is a good enough fix. At some point we might
    // need something more complex, maybe like cargo2android's logic for merging crates.
    modules.sort();
    modules.dedup();

    modules.sort_by_key(|m| m.props.get_string("name").unwrap().to_string());
    for m in modules {
        m.write(&mut bp_contents)?;
        bp_contents += "\n";
    }
    Ok(bp_contents)
}

/// Generates and returns a Trusty rules.mk file for the given set of crates.
fn generate_rules_mk(
    cfg: &VariantConfig,
    package_cfg: &PackageVariantConfig,
    package_name: &str,
    crates: &[Crate],
    out_files: &[PathBuf],
) -> Result<String> {
    let out_files = if package_cfg.copy_out && !out_files.is_empty() {
        out_files.iter().map(|f| f.file_name().unwrap().to_str().unwrap().to_string()).collect()
    } else {
        vec![]
    };

    let crates: Vec<_> = crates
        .iter()
        .filter(|c| {
            if c.types.contains(&CrateType::Bin) {
                eprintln!("WARNING: skipped generation of rules.mk for binary crate: {}", c.name);
                false
            } else if c.types.iter().any(|t| t.is_test()) {
                // Test build file generation is not yet implemented
                eprintln!("WARNING: skipped generation of rules.mk for test crate: {}", c.name);
                false
            } else {
                true
            }
        })
        .collect();
    let [crate_] = &crates[..] else {
        bail!(
            "Expected exactly one library crate for package {package_name} when generating \
               rules.mk, found: {crates:?}"
        );
    };
    crate_to_rulesmk(crate_, cfg, package_cfg, &out_files).with_context(|| {
        format!(
            "failed to generate rules.mk for crate \"{}\" with package name \"{}\"",
            crate_.name, crate_.package_name
        )
    })
}

/// Generates and returns a Soong Blueprint for a Trusty rules.mk
fn generate_android_bp_for_rules_mk(
    package_name: &str,
    module_name_overrides: &BTreeMap<String, String>,
) -> Result<String> {
    let mut bp_contents = String::new();

    let mut m = BpModule::new("dirgroup".to_string());

    let default_dirgroup_name = format!("trusty_dirgroup_external_rust_crates_{}", package_name);
    let dirgroup_name =
        override_module_name(&default_dirgroup_name, &[], module_name_overrides, &RENAME_MAP)
            .unwrap_or(default_dirgroup_name);
    m.props.set("name", dirgroup_name);
    m.props.set("dirs", vec!["."]);
    m.props.set("visibility", vec!["//trusty/vendor/google/aosp/scripts"]);

    m.write(&mut bp_contents)?;
    bp_contents += "\n";

    Ok(bp_contents)
}

/// Apply patch from `patch_path` to file `output_path`.
///
/// Warns but still returns ok if the patch did not cleanly apply,
fn apply_patch_file(output_path: &Path, patch_path: &Path) -> Result<()> {
    let patch_output = Command::new("patch")
        .arg("-s")
        .arg("--no-backup-if-mismatch")
        .arg(output_path)
        .arg(patch_path)
        .output()
        .context("Running patch")?;
    if !patch_output.status.success() {
        let stdout = String::from_utf8(patch_output.stdout)?;
        let stderr = String::from_utf8(patch_output.stderr)?;
        // These errors will cause the cargo_embargo command to fail, but not yet!
        bail!("failed to apply patch {patch_path:?}:\n\nout:\n{stdout}\n\nerr:\n{stderr}");
    }
    Ok(())
}

/// Writes the given contents to the given `Android.bp` file, formats it with `bpfmt`, and applies
/// the patch if there is one.
fn write_format_android_bp(
    bp_path: &Path,
    bp_contents: &str,
    patch_path: Option<&Path>,
) -> Result<()> {
    File::create(bp_path)?.write_all(bp_contents.as_bytes())?;

    let bpfmt_output =
        Command::new("bpfmt").arg("-w").arg(bp_path).output().context("Running bpfmt")?;
    if !bpfmt_output.status.success() {
        eprintln!(
            "WARNING: bpfmt -w {:?} failed before patch: {}",
            bp_path,
            String::from_utf8_lossy(&bpfmt_output.stderr)
        );
    }

    if let Some(patch_path) = patch_path {
        apply_patch_file(bp_path, patch_path)?;
        // Re-run bpfmt after the patch so
        let bpfmt_output = Command::new("bpfmt")
            .arg("-w")
            .arg(bp_path)
            .output()
            .context("Running bpfmt after patch")?;
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
    cfg: &VariantConfig,
    package_cfg: &PackageVariantConfig,
    extra_srcs: &[String],
) -> Result<Vec<BpModule>> {
    let mut modules = Vec::new();
    for crate_type in &crate_.types {
        let host = if package_cfg.device_supported { "" } else { "_host" };
        let rlib = if package_cfg.force_rlib { "_rlib" } else { "" };
        let (module_type, module_name) = match crate_type {
            CrateType::Bin => ("rust_binary".to_string() + host, crate_.name.clone()),
            CrateType::Lib | CrateType::RLib => {
                let stem = "lib".to_string() + &crate_.name;
                ("rust_library".to_string() + host + rlib, stem)
            }
            CrateType::DyLib => {
                let stem = "lib".to_string() + &crate_.name;
                ("rust_library".to_string() + host + "_dylib", stem + "_dylib")
            }
            CrateType::CDyLib => {
                let stem = "lib".to_string() + &crate_.name;
                ("rust_ffi".to_string() + host + "_shared", stem + "_shared")
            }
            CrateType::StaticLib => {
                let stem = "lib".to_string() + &crate_.name;
                ("rust_ffi".to_string() + host + "_static", stem + "_static")
            }
            CrateType::ProcMacro => {
                let stem = "lib".to_string() + &crate_.name;
                ("rust_proc_macro".to_string(), stem)
            }
            CrateType::Test | CrateType::TestNoHarness => {
                let suffix = crate_.main_src.to_string_lossy().into_owned();
                let suffix = suffix.replace('/', "_").replace(".rs", "");
                let stem = crate_.package_name.clone() + "_test_" + &suffix;
                if crate_.empty_test {
                    return Ok(Vec::new());
                }
                if crate_type == &CrateType::TestNoHarness {
                    eprintln!(
                        "WARNING: ignoring test \"{}\" with harness=false. not supported yet",
                        stem
                    );
                    return Ok(Vec::new());
                }
                ("rust_test".to_string() + host, stem)
            }
        };

        let mut m = BpModule::new(module_type.clone());
        let Some(module_name) = override_module_name(
            &module_name,
            &cfg.module_blocklist,
            &cfg.module_name_overrides,
            &RENAME_MAP,
        ) else {
            continue;
        };
        if matches!(
            crate_type,
            CrateType::Lib
                | CrateType::RLib
                | CrateType::DyLib
                | CrateType::CDyLib
                | CrateType::StaticLib
        ) && !module_name.starts_with(&format!("lib{}", crate_.name))
        {
            bail!("Module name must start with lib{} but was {}", crate_.name, module_name);
        }
        m.props.set("name", module_name.clone());

        if let Some(defaults) = &cfg.global_defaults {
            m.props.set("defaults", vec![defaults.clone()]);
        }

        if package_cfg.host_supported
            && package_cfg.device_supported
            && module_type != "rust_proc_macro"
        {
            m.props.set("host_supported", true);
        }

        if module_type != "rust_proc_macro" {
            if package_cfg.host_supported && !package_cfg.host_cross_supported {
                m.props.set("host_cross_supported", false);
            } else if crate_.externs.iter().any(|extern_dep| extern_dep.name == "proc_macro2") {
                // proc_macro2 is host_cross_supported: false.
                // If there's a dependency on it, then we shouldn't build for HostCross.
                m.props.set("host_cross_supported", false);
            } else if crate_.package_name == "proc-macro2" {
                m.props.set("host_cross_supported", false);
            }
        }

        if !crate_type.is_test() && package_cfg.host_supported && package_cfg.host_first_multilib {
            m.props.set("compile_multilib", "first");
        }
        if crate_type.is_c_library() {
            m.props.set_if_nonempty("include_dirs", package_cfg.exported_c_header_dir.clone());
        }

        m.props.set("crate_name", crate_.name.clone());
        m.props.set("cargo_env_compat", true);

        if let Some(version) = &crate_.version {
            m.props.set("cargo_pkg_version", version.clone());
        }

        if crate_.types.contains(&CrateType::Test) {
            m.props.set("test_suites", vec!["general-tests"]);
            m.props.set("auto_gen_config", true);
            if package_cfg.host_supported {
                m.props.object("test_options").set("unit_test", !package_cfg.no_presubmit);
            }
        }

        m.props.set("crate_root", crate_.main_src.clone());
        m.props.set_if_nonempty("srcs", extra_srcs.to_owned());

        m.props.set("edition", crate_.edition.clone());
        m.props.set_if_nonempty("features", crate_.features.clone());
        m.props.set_if_nonempty(
            "cfgs",
            crate_
                .cfgs
                .clone()
                .into_iter()
                .filter(|crate_cfg| !cfg.cfg_blocklist.contains(crate_cfg))
                .collect(),
        );

        let mut flags = Vec::new();
        if !crate_.cap_lints.is_empty() {
            flags.push(crate_.cap_lints.clone());
        }
        flags.extend(crate_.codegens.iter().map(|codegen| format!("-C {}", codegen)));
        m.props.set_if_nonempty("flags", flags);

        let mut rust_libs = Vec::new();
        let mut proc_macro_libs = Vec::new();
        let mut aliases = Vec::new();
        for extern_dep in &crate_.externs {
            match extern_dep.extern_type {
                ExternType::Rust => rust_libs.push(extern_dep.lib_name.clone()),
                ExternType::ProcMacro => proc_macro_libs.push(extern_dep.lib_name.clone()),
            }
            if extern_dep.name != extern_dep.lib_name {
                aliases.push(format!("{}:{}", extern_dep.lib_name, extern_dep.name));
            }
        }

        // Add "lib" prefix and apply name overrides.
        let process_lib_deps = |libs: Vec<String>| -> Vec<String> {
            let mut result = Vec::new();
            for x in libs {
                let module_name = "lib".to_string() + x.as_str();
                if let Some(module_name) = override_module_name(
                    &module_name,
                    &package_cfg.dep_blocklist,
                    &cfg.module_name_overrides,
                    &RENAME_MAP,
                ) {
                    result.push(module_name);
                }
            }
            result.sort();
            result.dedup();
            result
        };
        m.props.set_if_nonempty("rustlibs", process_lib_deps(rust_libs));
        m.props.set_if_nonempty("proc_macros", process_lib_deps(proc_macro_libs));
        let (whole_static_libs, static_libs) = process_lib_deps(crate_.static_libs.clone())
            .into_iter()
            .partition(|static_lib| package_cfg.whole_static_libs.contains(static_lib));
        m.props.set_if_nonempty("static_libs", static_libs);
        m.props.set_if_nonempty("whole_static_libs", whole_static_libs);
        m.props.set_if_nonempty("shared_libs", process_lib_deps(crate_.shared_libs.clone()));
        m.props.set_if_nonempty("aliases", aliases);

        if package_cfg.device_supported {
            if !crate_type.is_test() {
                if cfg.native_bridge_supported {
                    m.props.set("native_bridge_supported", true);
                }
                if cfg.product_available {
                    m.props.set("product_available", true);
                }
                if cfg.ramdisk_available {
                    m.props.set("ramdisk_available", true);
                }
                if cfg.recovery_available {
                    m.props.set("recovery_available", true);
                }
                if cfg.vendor_available {
                    m.props.set("vendor_available", true);
                }
                if cfg.vendor_ramdisk_available {
                    m.props.set("vendor_ramdisk_available", true);
                }
            }
            if crate_type.is_library() {
                m.props.set_if_nonempty("apex_available", cfg.apex_available.clone());
                if let Some(min_sdk_version) = &cfg.min_sdk_version {
                    m.props.set("min_sdk_version", min_sdk_version.clone());
                }
            }
        }
        if crate_type.is_test() {
            if let Some(data) =
                package_cfg.test_data.get(crate_.main_src.to_string_lossy().as_ref())
            {
                m.props.set("data", data.clone());
            }
        } else if package_cfg.no_std {
            m.props.set("prefer_rlib", true);
            m.props.set("no_stdlibs", true);
            let mut stdlibs = vec!["libcompiler_builtins.rust_sysroot", "libcore.rust_sysroot"];
            if package_cfg.alloc {
                stdlibs.push("liballoc.rust_sysroot");
            }
            stdlibs.sort();
            m.props.set("stdlibs", stdlibs);
        }

        if let Some(visibility) = cfg.module_visibility.get(&module_name) {
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

/// Convert a `Crate` into a rules.mk file.
///
/// If messy business logic is necessary, prefer putting it here.
fn crate_to_rulesmk(
    crate_: &Crate,
    cfg: &VariantConfig,
    package_cfg: &PackageVariantConfig,
    out_files: &[String],
) -> Result<String> {
    let mut contents = String::new();

    contents += "LOCAL_DIR := $(GET_LOCAL_DIR)\n";
    contents += "MODULE := $(LOCAL_DIR)\n";
    contents += &format!("MODULE_CRATE_NAME := {}\n", crate_.name);

    if !crate_.types.is_empty() {
        contents += "MODULE_RUST_CRATE_TYPES :=";
        for crate_type in &crate_.types {
            contents += match crate_type {
                CrateType::Lib => " rlib",
                CrateType::StaticLib => " staticlib",
                CrateType::ProcMacro => " proc-macro",
                _ => bail!("Cannot generate rules.mk for crate type {crate_type:?}"),
            };
        }
        contents += "\n";
    }

    contents += &format!("MODULE_SRCS := $(LOCAL_DIR)/{}\n", crate_.main_src.display());

    if !out_files.is_empty() {
        contents += &format!("OUT_FILES := {}\n", out_files.join(" "));
        contents += "BUILD_OUT_FILES := $(addprefix $(call TOBUILDDIR,$(MODULE))/,$(OUT_FILES))\n";
        contents += "$(BUILD_OUT_FILES): $(call TOBUILDDIR,$(MODULE))/% : $(MODULE)/out/%\n";
        contents += "\t@echo copying $^ to $@\n";
        contents += "\t@$(MKDIR)\n";
        contents += "\t@cp $^ $@\n\n";
        contents += "MODULE_RUST_ENV += OUT_DIR=$(call TOBUILDDIR,$(MODULE))\n\n";
        contents += "MODULE_SRCDEPS += $(BUILD_OUT_FILES)\n\n";
        contents += "OUT_FILES :=\n";
        contents += "BUILD_OUT_FILES :=\n";
        contents += "\n";
    }

    // crate dependencies without lib- prefix. Since paths to trusty modules may
    // contain hyphens, we generate the module path using the raw name output by
    // cargo metadata or cargo build.
    let mut library_deps: Vec<_> = crate_.externs.iter().map(|dep| dep.raw_name.clone()).collect();
    if package_cfg.no_std {
        contents += "MODULE_ADD_IMPLICIT_DEPS := false\n";
        library_deps.push("compiler_builtins".to_string());
        library_deps.push("core".to_string());
        if package_cfg.alloc {
            library_deps.push("alloc".to_string());
        }
    }

    contents += &format!("MODULE_RUST_EDITION := {}\n", crate_.edition);

    let mut flags = Vec::new();
    if !crate_.cap_lints.is_empty() {
        flags.push(crate_.cap_lints.clone());
    }
    flags.extend(crate_.codegens.iter().map(|codegen| format!("-C {}", codegen)));
    flags.extend(crate_.features.iter().map(|feat| format!("--cfg 'feature=\"{feat}\"'")));
    flags.extend(
        crate_
            .cfgs
            .iter()
            .filter(|crate_cfg| !cfg.cfg_blocklist.contains(crate_cfg))
            .map(|cfg| format!("--cfg '{cfg}'")),
    );
    if !flags.is_empty() {
        contents += "MODULE_RUSTFLAGS += \\\n\t";
        contents += &flags.join(" \\\n\t");
        contents += "\n\n";
    }

    let mut library_deps: Vec<String> = library_deps
        .into_iter()
        .flat_map(|dep| {
            override_module_name(
                &format!("lib{dep}"),
                &package_cfg.dep_blocklist,
                &BTreeMap::new(),
                &RULESMK_RENAME_MAP,
            )
        })
        .map(|dep| {
            // Rewrite dependency name so it is passed to the FIND_CRATE macro
            // which will expand to the module path when building Trusty.
            if let Some(dep) = dep.strip_prefix("lib") {
                format!("$(call FIND_CRATE,{dep})")
            } else {
                dep
            }
        })
        .collect();
    library_deps.sort();
    library_deps.dedup();
    contents += "MODULE_LIBRARY_DEPS := \\\n\t";
    contents += &library_deps.join(" \\\n\t");
    contents += "\n\n";

    contents += "include make/library.mk\n";
    Ok(contents)
}

#[cfg(test)]
mod tests {
    use super::*;
    use googletest::matchers::eq;
    use googletest::prelude::assert_that;
    use std::env::{current_dir, set_current_dir};
    use std::fs::{self, read_to_string};
    use std::path::PathBuf;

    const TESTDATA_PATH: &str = "testdata";

    #[test]
    fn group_variants_by_package() {
        let main_v1 =
            Crate { name: "main_v1".to_string(), package_dir: "main".into(), ..Default::default() };
        let main_v1_tests = Crate {
            name: "main_v1_tests".to_string(),
            package_dir: "main".into(),
            ..Default::default()
        };
        let other_v1 = Crate {
            name: "other_v1".to_string(),
            package_dir: "other".into(),
            ..Default::default()
        };
        let main_v2 =
            Crate { name: "main_v2".to_string(), package_dir: "main".into(), ..Default::default() };
        let some_v2 =
            Crate { name: "some_v2".to_string(), package_dir: "some".into(), ..Default::default() };
        let crates = vec![
            vec![main_v1.clone(), main_v1_tests.clone(), other_v1.clone()],
            vec![main_v2.clone(), some_v2.clone()],
        ];

        let module_by_package = group_by_package(crates);

        let expected_by_package: BTreeMap<PathBuf, Vec<Vec<Crate>>> = [
            ("main".into(), vec![vec![main_v1, main_v1_tests], vec![main_v2]]),
            ("other".into(), vec![vec![other_v1], vec![]]),
            ("some".into(), vec![vec![], vec![some_v2]]),
        ]
        .into_iter()
        .collect();
        assert_eq!(module_by_package, expected_by_package);
    }

    #[test]
    fn generate_bp() {
        for testdata_directory_path in testdata_directories() {
            let cfg = Config::from_json_str(
                &read_to_string(testdata_directory_path.join("cargo_embargo.json"))
                    .expect("Failed to open cargo_embargo.json"),
            )
            .unwrap();
            let crates: Vec<Vec<Crate>> = serde_json::from_reader(
                File::open(testdata_directory_path.join("crates.json"))
                    .expect("Failed to open crates.json"),
            )
            .unwrap();
            let expected_output =
                read_to_string(testdata_directory_path.join("expected_Android.bp")).unwrap();

            let old_current_dir = current_dir().unwrap();
            set_current_dir(&testdata_directory_path).unwrap();

            let module_by_package = group_by_package(crates);
            assert_eq!(module_by_package.len(), 1);
            let crates = module_by_package.into_values().next().unwrap();

            let package_name = &crates[0][0].package_name;
            let def = PackageConfig::default();
            let package_cfg = cfg.package.get(package_name).unwrap_or(&def);
            let mut output = generate_android_bp_package_header(
                package_name,
                package_cfg,
                "",
                &crates,
                &cfg.variants.first().unwrap().module_name_overrides,
            )
            .unwrap();
            for (variant_index, variant_cfg) in cfg.variants.iter().enumerate() {
                let variant_crates = &crates[variant_index];
                let package_name = &variant_crates[0].package_name;
                let def = PackageVariantConfig::default();
                let package_variant_cfg = variant_cfg.package.get(package_name).unwrap_or(&def);

                output += &generate_android_bp(
                    variant_cfg,
                    package_variant_cfg,
                    package_name,
                    variant_crates,
                    &Vec::new(),
                )
                .unwrap();
            }

            assert_that!(output, eq(&expected_output));

            set_current_dir(old_current_dir).unwrap();
        }
    }

    #[test]
    fn crate_to_bp_empty() {
        let c = Crate {
            name: "name".to_string(),
            package_name: "package_name".to_string(),
            edition: "2021".to_string(),
            types: vec![],
            ..Default::default()
        };
        let cfg = VariantConfig { ..Default::default() };
        let package_cfg = PackageVariantConfig { ..Default::default() };
        let modules = crate_to_bp_modules(&c, &cfg, &package_cfg, &[]).unwrap();

        assert_eq!(modules, vec![]);
    }

    #[test]
    fn crate_to_bp_minimal() {
        let c = Crate {
            name: "name".to_string(),
            package_name: "package_name".to_string(),
            edition: "2021".to_string(),
            types: vec![CrateType::Lib],
            ..Default::default()
        };
        let cfg = VariantConfig { ..Default::default() };
        let package_cfg = PackageVariantConfig { ..Default::default() };
        let modules = crate_to_bp_modules(&c, &cfg, &package_cfg, &[]).unwrap();

        assert_eq!(
            modules,
            vec![BpModule {
                module_type: "rust_library".to_string(),
                props: BpProperties {
                    map: [
                        (
                            "apex_available".to_string(),
                            BpValue::List(vec![
                                BpValue::String("//apex_available:platform".to_string()),
                                BpValue::String("//apex_available:anyapex".to_string()),
                            ])
                        ),
                        ("cargo_env_compat".to_string(), BpValue::Bool(true)),
                        ("crate_name".to_string(), BpValue::String("name".to_string())),
                        ("edition".to_string(), BpValue::String("2021".to_string())),
                        ("host_supported".to_string(), BpValue::Bool(true)),
                        ("name".to_string(), BpValue::String("libname".to_string())),
                        ("product_available".to_string(), BpValue::Bool(true)),
                        ("crate_root".to_string(), BpValue::String("".to_string())),
                        ("vendor_available".to_string(), BpValue::Bool(true)),
                    ]
                    .into_iter()
                    .collect(),
                    raw_block: None
                }
            }]
        );
    }

    #[test]
    fn crate_to_bp_rename() {
        let c = Crate {
            name: "ash".to_string(),
            package_name: "package_name".to_string(),
            edition: "2021".to_string(),
            types: vec![CrateType::Lib],
            ..Default::default()
        };
        let cfg = VariantConfig { ..Default::default() };
        let package_cfg = PackageVariantConfig { ..Default::default() };
        let modules = crate_to_bp_modules(&c, &cfg, &package_cfg, &[]).unwrap();

        assert_eq!(
            modules,
            vec![BpModule {
                module_type: "rust_library".to_string(),
                props: BpProperties {
                    map: [
                        (
                            "apex_available".to_string(),
                            BpValue::List(vec![
                                BpValue::String("//apex_available:platform".to_string()),
                                BpValue::String("//apex_available:anyapex".to_string()),
                            ])
                        ),
                        ("cargo_env_compat".to_string(), BpValue::Bool(true)),
                        ("crate_name".to_string(), BpValue::String("ash".to_string())),
                        ("edition".to_string(), BpValue::String("2021".to_string())),
                        ("host_supported".to_string(), BpValue::Bool(true)),
                        ("name".to_string(), BpValue::String("libash_rust".to_string())),
                        ("product_available".to_string(), BpValue::Bool(true)),
                        ("crate_root".to_string(), BpValue::String("".to_string())),
                        ("vendor_available".to_string(), BpValue::Bool(true)),
                    ]
                    .into_iter()
                    .collect(),
                    raw_block: None
                }
            }]
        );
    }

    /// Returns a list of directories containing test data.
    ///
    /// Each directory under `testdata/` contains a single test case.
    pub fn testdata_directories() -> Vec<PathBuf> {
        fs::read_dir(TESTDATA_PATH)
            .expect("Failed to read testdata directory")
            .filter_map(|entry| {
                let entry = entry.expect("Error reading testdata directory entry");
                if entry
                    .file_type()
                    .expect("Error getting metadata for testdata subdirectory")
                    .is_dir()
                {
                    Some(entry.path())
                } else {
                    None
                }
            })
            .collect()
    }
}
