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

use core::fmt::Display;
use std::path::{Path, PathBuf};

#[derive(Debug, PartialEq, Eq)]
pub struct RepoPath {
    root: PathBuf,
    path: PathBuf,
}

impl Display for RepoPath {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.rel().display())
    }
}

impl RepoPath {
    pub fn new<P: Into<PathBuf>, Q: AsRef<Path>>(root: P, path: Q) -> RepoPath {
        let root: PathBuf = root.into();
        let path = path.as_ref();
        assert!(root.is_absolute());
        assert!(path.is_relative());
        let path = root.join(path);
        RepoPath { root, path }
    }
    pub fn root(&self) -> &Path {
        self.root.as_path()
    }
    pub fn rel(&self) -> &Path {
        self.path.strip_prefix(&self.root).unwrap()
    }
    pub fn abs(&self) -> &Path {
        self.path.as_path()
    }
    pub fn join(&self, path: &impl AsRef<Path>) -> RepoPath {
        RepoPath { root: self.root.clone(), path: self.path.join(path) }
    }
    pub fn with_same_root<P: AsRef<Path>>(&self, path: P) -> RepoPath {
        RepoPath::new(self.root.clone(), path)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_basic() {
        let p = RepoPath::new(&"/foo", &"bar");
        assert_eq!(p.root(), Path::new("/foo"));
        assert_eq!(p.rel(), Path::new("bar"));
        assert_eq!(p.abs(), PathBuf::from("/foo/bar"));
        assert_eq!(p.join(&"baz"), RepoPath::new("/foo", "bar/baz"));
        assert_eq!(p.with_same_root(&"baz"), RepoPath::new("/foo", "baz"));
    }
}
