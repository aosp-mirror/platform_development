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
    collections::BTreeSet,
    fs::{copy, read_dir, read_to_string, remove_dir_all, remove_file, rename, write},
    os::unix::fs::symlink,
    path::{Path, PathBuf},
    process::Command,
};

use anyhow::{anyhow, bail, Context, Result};
use crate_config::CrateConfig;
use glob::glob;
use google_metadata::GoogleMetadata;
use itertools::Itertools;
use license_checker::{find_licenses, LicenseState};
use name_and_version::NamedAndVersioned;
use rooted_path::RootedPath;
use semver::Version;
use test_mapping::TestMapping;

use crate::{
    android_bp::run_cargo_embargo,
    copy_dir,
    crate_type::Crate,
    ensure_exists_and_empty,
    license::{most_restrictive_type, update_module_license_files},
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

/// Crate state indicating we have run `cargo vendor` on the pseudo-crate.
#[derive(Debug)]
pub struct Vendored {
    /// The vendored copy of the crate, from running `cargo vendor`
    vendored_crate: Crate,
}

/// Crate state indicating we have copied the vendored code to a temporary build
/// directory, copied over Android customizations, and applied patches.
#[derive(Debug)]
pub struct CopiedAndPatched {
    /// The vendored copy of the crate, from running `cargo vendor`
    vendored_crate: Crate,
    /// The license terms and associated license files.
    licenses: LicenseState,
}
pub trait ManagedCrateState {}
impl ManagedCrateState for New {}
impl ManagedCrateState for Vendored {}
impl ManagedCrateState for CopiedAndPatched {}

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
];

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
    fn temporary_build_directory(&self) -> RootedPath {
        self.android_crate
            .path()
            .with_same_root("out/rust-crate-temporary-build")
            .unwrap()
            .join(self.name())
            .unwrap()
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
    ) -> Result<ManagedCrate<CopiedAndPatched>> {
        let regenerated = self.into_vendored(pseudo_crate)?.regenerate()?;
        Ok(regenerated)
    }
}

impl ManagedCrate<Vendored> {
    fn into_copied_and_patched(
        self,
        licenses: LicenseState,
    ) -> Result<ManagedCrate<CopiedAndPatched>> {
        Ok(ManagedCrate {
            android_crate: self.android_crate,
            config: self.config,
            extra: CopiedAndPatched { vendored_crate: self.extra.vendored_crate, licenses },
        })
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
    pub fn regenerate(self) -> Result<ManagedCrate<CopiedAndPatched>> {
        self.copy_to_temporary_build_directory()?;
        self.copy_customizations()?;

        // Delete stuff that we don't want to keep around, as specified in the
        // android_config.toml
        for deletion in self.config().deletions() {
            let dir = self.temporary_build_directory().join(deletion)?;
            if dir.abs().is_dir() {
                remove_dir_all(dir)?;
            } else {
                remove_file(dir)?;
            }
        }

        self.apply_patches()?;

        let licenses = find_licenses(
            self.temporary_build_directory(),
            self.name(),
            self.android_crate.license(),
        )?;
        let regenerated = self.into_copied_and_patched(licenses)?;
        regenerated.regenerate()?;
        Ok(regenerated)
    }
}

impl ManagedCrate<CopiedAndPatched> {
    pub fn regenerate(&self) -> Result<()> {
        // License logic must happen AFTER applying patches, because we use patches
        // to add missing license files. It must also happen BEFORE cargo_embargo,
        // because cargo_embargo needs to put license information in the Android.bp.
        self.update_license_files()?;

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
    fn update_license_files(&self) -> Result<()> {
        // For every chosen license, we must be able to find an associated
        // license file.
        if !self.extra.licenses.unsatisfied.is_empty() {
            bail!(
                "Could not find license files for some licenses: {:?}",
                self.extra.licenses.unsatisfied
            );
        }

        // SOME license must apply to the code. If none apply, that's an error.
        if self.extra.licenses.satisfied.is_empty() {
            bail!("No license terms were found for this crate");
        }

        // TODO: Maybe remove unused license files.

        // Per http://go/thirdpartyreviewers#license:
        // "There must be a file called LICENSE containing an allowed third party license."

        let license_files = self
            .extra
            .licenses
            .satisfied
            .values()
            .map(|path| path.as_path())
            .collect::<BTreeSet<_>>();
        let canonical_license_file_name = Path::new("LICENSE");
        let canonical_license_path = self.temporary_build_directory().abs().join("LICENSE");
        if license_files.len() == 1 {
            // If there's a single applicable license file, it must either
            // be called LICENSE, or else we need to symlink LICENSE to it.
            let license_file = license_files.first().unwrap();
            if *license_file != canonical_license_file_name {
                if canonical_license_path.exists() {
                    // TODO: Maybe just blindly delete LICENSE and replace it with a symlink.
                    // Currently, we have to use a deletions config in android_config.toml
                    // to remove it.
                    bail!("Found a single license file {}, but we can't create a symlink to it because a file named LICENSE already exists", license_file.display());
                } else {
                    symlink(license_file, canonical_license_path)?;
                }
            }
        } else {
            // We found multiple license files. Per go/thirdparty/licenses#multiple they should
            // be concatenated into a single LICENSE file, separated by dashed line dividers.
            let mut license_contents = Vec::new();
            for file in license_files {
                license_contents
                    .push(read_to_string(self.temporary_build_directory().abs().join(file))?);
            }
            // TODO: Maybe warn if LICENSE file exists. But there are cases where we can't just delete it
            // because it contains *some* of the necessary license texts.
            write(
                canonical_license_path,
                license_contents.iter().join("\n\n------------------\n\n"),
            )?;
        }

        update_module_license_files(&self.temporary_build_directory(), &self.extra.licenses)?;
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
        metadata.update(
            self.name(),
            self.extra.vendored_crate.version().to_string(),
            self.extra.vendored_crate.description(),
            most_restrictive_type(&self.extra.licenses),
        );
        metadata.write()?;

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
}
