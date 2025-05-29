// Copyright (C) 2025 The Android Open Source Project
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

// To run:
//
// ./android_cargo.py test -p crate_tool --test integration_test -- --nocapture

#[cfg(not(soong))]
mod test {
    use anyhow::Result;
    use crate_tool::{default_repo_root, maybe_build_cargo_embargo, ManagedRepo};
    use rooted_path::RootedPath;
    use std::{fs::remove_dir_all, path::Path, process::Command};
    use tempfile::TempDir;

    fn dump(path: impl AsRef<Path>) {
        Command::new("tree")
            .arg(path.as_ref().file_name().unwrap())
            .current_dir(path.as_ref().parent().unwrap())
            .spawn()
            .unwrap()
            .wait()
            .unwrap();
    }
    struct TempMonorepo {
        path: RootedPath,
        tempdir: TempDir,
    }

    impl TempMonorepo {
        fn new(path: RootedPath) -> Result<Self> {
            let tempdir = tempfile::tempdir_in(path.abs())?;
            Ok(TempMonorepo { path, tempdir })
        }
        fn path(&self) -> &RootedPath {
            &self.path
        }
        fn monorepo_path(&self) -> RootedPath {
            self.path
                .join(Path::new(self.tempdir.path().file_name().unwrap()))
                .unwrap()
                .join("test_monorepo")
                .unwrap()
        }
        fn vendored_path(&self) -> RootedPath {
            self.path
                .with_same_root("out/rust-vendored-crates")
                .unwrap()
                .join(self.monorepo_path().rel().parent().unwrap())
                .unwrap()
        }
    }

    impl Drop for TempMonorepo {
        fn drop(&mut self) {
            if self.vendored_path().abs().exists() {
                remove_dir_all(self.vendored_path().abs())
                    .expect("Failed to clean up vendored crates");
            }
        }
    }

    #[test]
    fn integration_test() -> Result<()> {
        let repo_root = default_repo_root()?;
        let managed_repo_path =
            TempMonorepo::new(RootedPath::new(repo_root, "development/tools")?)?;

        maybe_build_cargo_embargo(&managed_repo_path.path().root(), false)?;

        let managed_repo = ManagedRepo::new(managed_repo_path.monorepo_path(), false)?;
        managed_repo.init()?;

        assert!(
            managed_repo.analyze_import("nonexistent_crate_blah").is_err(),
            "Analyze import of non-existent crate"
        );
        // base64 has no deps
        managed_repo.analyze_import("base64")?;

        assert!(
            managed_repo.import("base64", "0.21.123", false).is_err(),
            "Import of non-existent version"
        );
        managed_repo.import("base64", "0.21.7", false)?;
        assert!(
            managed_repo.import("base64", "0.22.0", false).is_err(),
            "Import a crate that's already imported"
        );

        managed_repo.analyze_updates("base64")?;

        managed_repo.suggest_updates(true, crate_tool::SemverCompatibilityRule::Ignore, false)?;

        assert!(
            managed_repo.update("base64", "0.21.123").is_err(),
            "Update to non-existent version"
        );
        managed_repo.update("base64", "0.22.1")?;

        dump(managed_repo_path.monorepo_path());

        Ok(())
    }
}
