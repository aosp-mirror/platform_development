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

//! A path relative to a root. Useful for paths relative to an Android repo, for example.

use core::fmt::Display;
use std::path::{Path, PathBuf};

/// Error types for the 'rooted_path' crate.
#[derive(thiserror::Error, Debug)]
pub enum Error {
    /// Root path is not absolute
    #[error("Root path is not absolute: {0}")]
    RootNotAbsolute(PathBuf),
    /// Path is not relative
    #[error("Path is not relative: {0}")]
    PathNotRelative(PathBuf),
}

/// A path relative to a root.
#[derive(Debug, PartialEq, Eq, Clone)]
pub struct RootedPath {
    root: PathBuf,
    path: PathBuf,
}

impl RootedPath {
    /// Creates a new RootedPath from an absolute root and a path relative to the root.
    pub fn new<P: Into<PathBuf>>(root: P, path: impl AsRef<Path>) -> Result<RootedPath, Error> {
        let root: PathBuf = root.into();
        if !root.is_absolute() {
            return Err(Error::RootNotAbsolute(root));
        }
        let path = path.as_ref();
        if !path.is_relative() {
            return Err(Error::PathNotRelative(path.to_path_buf()));
        }
        let path = root.join(path);
        Ok(RootedPath { root, path })
    }
    /// Returns the root.
    pub fn root(&self) -> &Path {
        self.root.as_path()
    }
    /// Returns the path relative to the root.
    pub fn rel(&self) -> &Path {
        self.path.strip_prefix(&self.root).unwrap()
    }
    /// Returns the absolute path.
    pub fn abs(&self) -> &Path {
        self.path.as_path()
    }
    /// Creates a new RootedPath with path adjoined to self.
    pub fn join(&self, path: impl AsRef<Path>) -> Result<RootedPath, Error> {
        let path = path.as_ref();
        if !path.is_relative() {
            return Err(Error::PathNotRelative(path.to_path_buf()));
        }
        Ok(RootedPath { root: self.root.clone(), path: self.path.join(path) })
    }
    /// Creates a new RootedPath with the same root but a new relative directory.
    pub fn with_same_root(&self, path: impl AsRef<Path>) -> Result<RootedPath, Error> {
        RootedPath::new(self.root.clone(), path)
    }
}

impl Display for RootedPath {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.rel().display())
    }
}

impl AsRef<Path> for RootedPath {
    fn as_ref(&self) -> &Path {
        self.abs()
    }
}

impl From<RootedPath> for PathBuf {
    fn from(val: RootedPath) -> Self {
        val.path
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_basic() -> Result<(), Error> {
        let p = RootedPath::new("/foo", "bar")?;
        assert_eq!(p.root(), Path::new("/foo"));
        assert_eq!(p.rel(), Path::new("bar"));
        assert_eq!(p.abs(), PathBuf::from("/foo/bar"));
        assert_eq!(p.join("baz")?, RootedPath::new("/foo", "bar/baz")?);
        assert_eq!(p.with_same_root("baz")?, RootedPath::new("/foo", "baz")?);
        Ok(())
    }

    #[test]
    fn test_errors() -> Result<(), Error> {
        assert!(RootedPath::new("foo", "bar").is_err());
        assert!(RootedPath::new("/foo", "/bar").is_err());
        let p = RootedPath::new("/foo", "bar")?;
        assert!(p.join("/baz").is_err());
        assert!(p.with_same_root("/baz").is_err());
        Ok(())
    }

    #[test]
    fn test_conversion() -> Result<(), Error> {
        let p = RootedPath::new("/foo", "bar")?;

        let path = p.as_ref();
        assert_eq!(path, Path::new("/foo/bar"));

        let pathbuf: PathBuf = p.into();
        assert_eq!(pathbuf, Path::new("/foo/bar"));

        Ok(())
    }
}
