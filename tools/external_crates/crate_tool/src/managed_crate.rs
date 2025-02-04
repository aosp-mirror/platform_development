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

use std::{
    cell::OnceCell,
    fs::{copy, read_dir, read_link, read_to_string, remove_dir_all, remove_file, rename, write},
    os::unix::fs::symlink,
    path::PathBuf,
    process::Command,
};

use anyhow::{anyhow, ensure, Context, Result};
use crate_config::CrateConfig;
use glob::glob;
use google_metadata::GoogleMetadata;
use license_checker::find_licenses;
use name_and_version::NamedAndVersioned;
use rooted_path::RootedPath;
use semver::Version;
use test_mapping::TestMapping;

use crate::{
    android_bp::run_cargo_embargo,
    copy_dir,
    crate_type::Crate,
    ensure_exists_and_empty,
    license::update_module_license_files,
    patch::Patch,
    pseudo_crate::{CargoVendorClean, PseudoCrate},
    SuccessOrError,
};

#[derive(Debug)]
pub struct ManagedCrate<State: ManagedCrateState> {
    /// The crate with Android customizations, a subdirectory of crates/.
    android_crate: Crate,
    config: OnceCell<CrateConfig>,
    extra: State,
}

#[derive(Debug)]
pub struct New {}
#[derive(Debug)]
pub struct Vendored {
    /// The vendored copy of the crate, from running `cargo vendor`
    vendored_crate: Crate,
}
pub trait ManagedCrateState {}
impl ManagedCrateState for New {}
impl ManagedCrateState for Vendored {}

static CUSTOMIZATIONS: &[&str] = &[
    "*.bp",
    "*.bp.fragment",
    "*.mk",
    "android_config.toml",
    "android",
    "cargo_embargo.json",
    "patches",
    "METADATA",
    "TEST_MAPPING",
    "MODULE_LICENSE_*",
];

static SYMLINKS: &[&str] = &["LICENSE", "NOTICE"];

impl<State: ManagedCrateState> ManagedCrate<State> {
    pub fn name(&self) -> &str {
        self.android_crate.name()
    }
    pub fn android_version(&self) -> &Version {
        self.android_crate.version()
    }
    fn android_crate_path(&self) -> &RootedPath {
        self.android_crate.path()
    }
    pub fn config(&self) -> &CrateConfig {
        self.config.get_or_init(|| {
            CrateConfig::read(self.android_crate_path().abs()).unwrap_or_else(|e| {
                panic!(
                    "Failed to read crate config {}/{}: {}",
                    self.android_crate_path(),
                    crate_config::CONFIG_FILE_NAME,
                    e
                )
            })
        })
    }
    fn patch_dir(&self) -> RootedPath {
        self.android_crate_path().join("patches").unwrap()
    }
    pub fn patches(&self) -> Result<Vec<PathBuf>> {
        let mut patches = Vec::new();
        let patch_dir = self.patch_dir();
        if patch_dir.abs().exists() {
            for entry in
                read_dir(&patch_dir).context(format!("Failed to read_dir {}", patch_dir))?
            {
                let entry = entry?;
                if entry.file_name() == "Android.bp.patch"
                    || entry.file_name() == "Android.bp.diff"
                    || entry.file_name() == "rules.mk.diff"
                {
                    continue;
                }
                patches.push(entry.path());
            }
        }
        patches.sort();

        Ok(patches)
    }
    pub fn recontextualize_patches(&self) -> Result<()> {
        let output = Command::new("git")
            .args(["status", "--porcelain", "."])
            .current_dir(self.android_crate_path())
            .output()?
            .success_or_error()?;
        if !output.stdout.is_empty() {
            return Err(anyhow!(
                "Crate directory {} has uncommitted changes",
                self.android_crate_path()
            ));
        }
        let mut new_patch_contents = Vec::new();
        for patch in self.patches()? {
            println!("Recontextualizing {}", patch.display());
            // Patch files can be in many different formats, and patch is very
            // forgiving. We might be able to use "git apply -R --directory=crates/foo"
            // once we have everything in the same format.
            Command::new("patch")
                .args(["-R", "-p1", "-l", "--reject-file=-", "--no-backup-if-mismatch", "-i"])
                .arg(&patch)
                .current_dir(self.android_crate_path())
                .spawn()?
                .wait()?
                .success_or_error()?;
            Command::new("git")
                .args(["add", "."])
                .current_dir(self.android_crate_path())
                .spawn()?
                .wait()?
                .success_or_error()?;
            let output = Command::new("git")
                .args([
                    "diff",
                    format!("--relative=crates/{}", self.name()).as_str(),
                    "-p",
                    "--stat",
                    "-R",
                    "--staged",
                    ".",
                ])
                .current_dir(self.android_crate_path())
                .output()?
                .success_or_error()?;
            Command::new("git")
                .args(["restore", "--staged", "."])
                .current_dir(self.android_crate_path())
                .spawn()?
                .wait()?
                .success_or_error()?;
            Command::new("git")
                .args(["restore", "."])
                .current_dir(self.android_crate_path())
                .spawn()?
                .wait()?
                .success_or_error()?;
            Command::new("git")
                .args(["clean", "-f", "."])
                .current_dir(self.android_crate_path())
                .spawn()?
                .wait()?
                .success_or_error()?;
            let patch_contents = read_to_string(&patch)?;
            let parsed = Patch::parse(&patch_contents);
            new_patch_contents.push((patch, parsed.reassemble(&output.stdout)));
        }
        for (path, contents) in new_patch_contents {
            write(path, contents)?;
        }
        Ok(())
    }
    pub fn fix_licenses(&self) -> Result<()> {
        println!("{} = \"={}\"", self.name(), self.android_version());
        let state =
            find_licenses(self.android_crate_path(), self.name(), self.android_crate.license())?;
        if !state.unsatisfied.is_empty() {
            println!("{:?}", state);
        } else {
            // For now, just update MODULE_LICENSE_*
            update_module_license_files(self.android_crate_path(), &state)?;
        }
        Ok(())
    }
}

impl ManagedCrate<New> {
    pub fn new(android_crate: Crate) -> Self {
        ManagedCrate { android_crate, config: OnceCell::new(), extra: New {} }
    }
    fn into_vendored(
        self,
        pseudo_crate: &PseudoCrate<CargoVendorClean>,
    ) -> Result<ManagedCrate<Vendored>> {
        let vendored_crate =
            Crate::from(pseudo_crate.vendored_dir_for(self.android_crate.name())?.clone())?;
        Ok(ManagedCrate {
            android_crate: self.android_crate,
            config: self.config,
            extra: Vendored { vendored_crate },
        })
    }
    pub fn regenerate(
        self,
        pseudo_crate: &PseudoCrate<CargoVendorClean>,
    ) -> Result<ManagedCrate<Vendored>> {
        let vendored = self.into_vendored(pseudo_crate)?;
        vendored.regenerate()?;
        Ok(vendored)
    }
}

impl ManagedCrate<Vendored> {
    fn temporary_build_directory(&self) -> RootedPath {
        self.android_crate
            .path()
            .with_same_root("out/rust-crate-temporary-build")
            .unwrap()
            .join(self.name())
            .unwrap()
    }
    /// Makes a clean copy of the vendored crates in a temporary build directory.
    fn copy_to_temporary_build_directory(&self) -> Result<()> {
        let build_dir = self.temporary_build_directory();
        ensure_exists_and_empty(&build_dir)?;
        remove_dir_all(&build_dir).context(format!("Failed to remove {}", build_dir))?;
        copy_dir(self.extra.vendored_crate.path(), &build_dir).context(format!(
            "Failed to copy {} to {}",
            self.extra.vendored_crate.path(),
            self.temporary_build_directory()
        ))?;
        Ok(())
    }
    /// Copies Android-specific customizations, such as Android.bp, METADATA, etc. from
    /// the managed crate directory to the temporary build directory. These are things that
    /// we have added and know need to be preserved.
    fn copy_customizations(&self) -> Result<()> {
        let dest_dir = self.temporary_build_directory();
        for pattern in CUSTOMIZATIONS {
            let full_pattern = self.android_crate.path().join(pattern)?;
            for entry in glob(
                full_pattern
                    .abs()
                    .to_str()
                    .ok_or(anyhow!("Failed to convert path {} to str", full_pattern))?,
            )? {
                let entry = entry?;
                let filename = entry
                    .file_name()
                    .context(format!("Failed to get file name for {}", entry.display()))?
                    .to_os_string();
                if entry.is_dir() {
                    copy_dir(&entry, &dest_dir.join(filename)?).context(format!(
                        "Failed to copy {} to {}",
                        entry.display(),
                        dest_dir
                    ))?;
                } else {
                    let dest_file = dest_dir.join(&filename)?;
                    if dest_file.abs().exists() {
                        return Err(anyhow!("Destination file {} exists", dest_file));
                    }
                    copy(&entry, dest_dir.join(filename)?).context(format!(
                        "Failed to copy {} to {}",
                        entry.display(),
                        dest_dir
                    ))?;
                }
            }
        }
        for link in SYMLINKS {
            let src_path = self.android_crate.path().join(link)?;
            if src_path.abs().is_symlink() {
                let dest = read_link(src_path)?;
                if dest.exists() {
                    return Err(anyhow!(
                        "Can't symlink {} -> {} because destination exists",
                        link,
                        dest.display(),
                    ));
                }
                symlink(dest, dest_dir.join(link)?)?;
            }
        }
        Ok(())
    }
    /// Applies patches from the patches/ directory in a deterministic order.
    ///
    /// Patches to the Android.bp file are excluded as they are applied
    /// later, by cargo_embargo
    fn apply_patches(&self) -> Result<()> {
        for patch in self.patches()? {
            Command::new("patch")
                .args(["-p1", "-l", "--no-backup-if-mismatch", "-i"])
                .arg(&patch)
                .current_dir(self.temporary_build_directory())
                .output()?
                .success_or_error()?;
        }
        Ok(())
    }
    /// Runs cargo_embargo on the crate in the temporary build directory.
    ///
    /// Because cargo_embargo can modify Cargo.lock files, we save them, if present.
    fn run_cargo_embargo(&self) -> Result<()> {
        let temporary_build_path = self.temporary_build_directory();

        let cargo_lock = temporary_build_path.join("Cargo.lock")?;
        let saved_cargo_lock = temporary_build_path.join("Cargo.lock.saved")?;
        if cargo_lock.abs().exists() {
            rename(&cargo_lock, &saved_cargo_lock)?;
        }

        run_cargo_embargo(&temporary_build_path)?
            .success_or_error()
            .context(format!("cargo_embargo execution failed for {}", self.name()))?;

        if cargo_lock.abs().exists() {
            remove_file(&cargo_lock)?;
        }
        if saved_cargo_lock.abs().exists() {
            rename(saved_cargo_lock, cargo_lock)?;
        }

        Ok(())
    }
    /// Updates the METADATA file in the temporary build directory.
    fn update_metadata(&self) -> Result<()> {
        let mut metadata =
            GoogleMetadata::try_from(self.temporary_build_directory().join("METADATA").unwrap())?;
        let mut writeback = false;
        writeback |= metadata.migrate_homepage();
        writeback |= metadata.migrate_archive();
        writeback |= metadata.remove_deprecated_url();
        let vendored_version = self.extra.vendored_crate.version();
        if self.android_crate.version() != vendored_version {
            metadata.set_date_to_today()?;
            metadata.set_version_and_urls(self.name(), vendored_version.to_string())?;
            writeback |= true;
        }
        if writeback {
            println!("Updating METADATA for {}", self.name());
            metadata.write()?;
        }

        Ok(())
    }
    /// Updates the TEST_MAPPING file in the temporary build directory, by
    /// removing deleted tests and adding new tests as post-submits.
    fn fix_test_mapping(&self) -> Result<()> {
        let mut tm = TestMapping::read(self.temporary_build_directory())?;
        let mut changed = tm.fix_import_paths();
        changed |= tm.add_new_tests_to_postsubmit()?;
        changed |= tm.remove_unknown_tests()?;
        // TODO: Add an option to fix up the reverse dependencies.
        if changed {
            println!("Updating TEST_MAPPING for {}", self.name());
            tm.write()?;
        }
        Ok(())
    }
    pub fn regenerate(&self) -> Result<()> {
        self.copy_to_temporary_build_directory()?;
        self.copy_customizations()?;

        // Delete stuff that we don't want to keep around, as specified in the
        // android_config.toml
        for deletion in self.config().deletions() {
            let dir = self.temporary_build_directory().join(deletion)?;
            ensure!(dir.abs().is_dir(), "{dir} is not a directory");
            remove_dir_all(dir)?;
        }

        self.apply_patches()?;
        self.run_cargo_embargo()?;

        self.update_metadata()?;
        self.fix_test_mapping()?;
        // Fails on dangling symlinks, which happens when we run on the log crate.
        checksum::generate(self.temporary_build_directory())?;

        let android_crate_dir = self.android_crate.path();
        remove_dir_all(android_crate_dir)?;
        rename(self.temporary_build_directory(), android_crate_dir)?;

        Ok(())
    }
}
