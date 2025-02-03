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
    fs::{copy, read_dir, read_link, read_to_string, remove_dir_all, rename, write},
    os::unix::fs::symlink,
    path::PathBuf,
    process::{Command, Output},
    str::from_utf8,
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
#[derive(Debug)]
pub struct Staged {
    vendored_crate: Crate,
    patch_output: Vec<(String, Output)>,
    cargo_embargo_output: Output,
}
pub trait ManagedCrateState {}
impl ManagedCrateState for New {}
impl ManagedCrateState for Vendored {}
impl ManagedCrateState for Staged {}

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
    fn staging_path(&self) -> RootedPath {
        self.android_crate
            .path()
            .with_same_root("out/rust-crate-temporary-build")
            .unwrap()
            .join(self.name())
            .unwrap()
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
    pub fn fix_metadata(&self) -> Result<()> {
        println!("{} = \"={}\"", self.name(), self.android_version());
        let mut metadata = GoogleMetadata::try_from(self.android_crate_path().join("METADATA")?)?;
        metadata.set_version_and_urls(self.name(), self.android_version().to_string())?;
        metadata.migrate_archive();
        metadata.migrate_homepage();
        metadata.remove_deprecated_url();
        metadata.write()?;
        Ok(())
    }
    pub fn fix_test_mapping(&self) -> Result<()> {
        let mut tm = TestMapping::read(self.android_crate_path().clone())?;
        let mut changed = tm.fix_import_paths();
        changed |= tm.add_new_tests_to_postsubmit()?;
        changed |= tm.remove_unknown_tests()?;
        if changed {
            println!("Updating TEST_MAPPING for {}", self.name());
            tm.write()?;
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
    ) -> Result<ManagedCrate<Staged>> {
        self.into_vendored(pseudo_crate)?.regenerate()
    }
}

impl ManagedCrate<Vendored> {
    fn stage(self) -> Result<ManagedCrate<Staged>> {
        self.copy_to_staging()?;

        self.copy_customizations()?;
        let patch_output = self.apply_patches()?;
        let cargo_embargo_output = run_cargo_embargo(&self.staging_path())?;

        Ok(ManagedCrate {
            android_crate: self.android_crate,
            config: self.config,
            extra: Staged {
                vendored_crate: self.extra.vendored_crate,
                patch_output,
                cargo_embargo_output,
            },
        })
    }
    fn copy_to_staging(&self) -> Result<()> {
        let staging_path = self.staging_path();
        ensure_exists_and_empty(&staging_path)?;
        remove_dir_all(&staging_path).context(format!("Failed to remove {}", staging_path))?;
        copy_dir(self.extra.vendored_crate.path(), &staging_path).context(format!(
            "Failed to copy {} to {}",
            self.extra.vendored_crate.path(),
            self.staging_path()
        ))?;
        Ok(())
    }
    fn copy_customizations(&self) -> Result<()> {
        let dest_dir = self.staging_path();
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
        for deletion in self.config().deletions() {
            let dir = self.staging_path().join(deletion)?;
            ensure!(dir.abs().is_dir(), "{dir} is not a directory");
            remove_dir_all(dir)?;
        }
        Ok(())
    }
    fn apply_patches(&self) -> Result<Vec<(String, Output)>> {
        let mut patch_output = Vec::new();
        for patch in self.patches()? {
            let output = Command::new("patch")
                .args(["-p1", "-l", "--no-backup-if-mismatch", "-i"])
                .arg(&patch)
                .current_dir(self.staging_path())
                .output()?;
            patch_output.push((
                String::from_utf8_lossy(patch.file_name().unwrap().as_encoded_bytes()).to_string(),
                output,
            ));
        }
        Ok(patch_output)
    }
    pub fn regenerate(self) -> Result<ManagedCrate<Staged>> {
        let staged = self.stage()?;
        staged.check_staged()?;
        if !staged.staging_path().abs().exists() {
            return Err(anyhow!("Staged crate not found at {}", staged.staging_path()));
        }

        let mut metadata =
            GoogleMetadata::try_from(staged.staging_path().join("METADATA").unwrap())?;
        let mut writeback = false;
        writeback |= metadata.migrate_homepage();
        writeback |= metadata.migrate_archive();
        writeback |= metadata.remove_deprecated_url();
        let vendored_version = staged.extra.vendored_crate.version();
        if staged.android_crate.version() != vendored_version {
            metadata.set_date_to_today()?;
            metadata.set_version_and_urls(staged.name(), vendored_version.to_string())?;
            writeback |= true;
        }
        if writeback {
            println!("Updating METADATA for {}", staged.name());
            metadata.write()?;
        }

        let android_crate_dir = staged.android_crate.path();
        remove_dir_all(android_crate_dir)?;
        rename(staged.staging_path(), android_crate_dir)?;

        staged.fix_test_mapping()?;
        checksum::generate(android_crate_dir.abs())?;

        Ok(staged)
    }
}

impl ManagedCrate<Staged> {
    pub fn check_staged(&self) -> Result<()> {
        if !self.patch_success() {
            for (patch, output) in self.patch_output() {
                if !output.status.success() {
                    return Err(anyhow!(
                        "Failed to patch {} with {}\nstdout:\n{}\nstderr:\n{}",
                        self.name(),
                        patch,
                        from_utf8(&output.stdout)?,
                        from_utf8(&output.stderr)?
                    ));
                }
            }
        }
        self.cargo_embargo_output()
            .success_or_error()
            .context(format!("cargo_embargo execution failed for {}", self.name()))?;

        Ok(())
    }
    fn patch_success(&self) -> bool {
        self.extra.patch_output.iter().all(|output| output.1.status.success())
    }
    fn patch_output(&self) -> &Vec<(String, Output)> {
        &self.extra.patch_output
    }
    fn cargo_embargo_output(&self) -> &Output {
        &self.extra.cargo_embargo_output
    }
}
