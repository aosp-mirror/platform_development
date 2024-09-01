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

use thiserror::Error;

#[derive(Error, Debug)]
pub enum RootedPathError {
    #[error("Root path is not absolute: {}", .0.display())]
    RootNotAbsolute(PathBuf),
    #[error("Path is not relative: {}", .0.display())]
    PathNotRelative(PathBuf),
}

#[derive(Debug, PartialEq, Eq)]
pub struct RootedPath {
    root: PathBuf,
    path: PathBuf,
}

impl RootedPath {
    pub fn new<P: Into<PathBuf>>(
        root: P,
        path: impl AsRef<Path>,
    ) -> Result<RootedPath, RootedPathError> {
        let root: PathBuf = root.into();
        if !root.is_absolute() {
            return Err(RootedPathError::RootNotAbsolute(root));
        }
        let path = path.as_ref();
        if !path.is_relative() {
            return Err(RootedPathError::PathNotRelative(path.to_path_buf()));
        }
        let path = root.join(path);
        Ok(RootedPath { root, path })
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
    pub fn join(&self, path: impl AsRef<Path>) -> Result<RootedPath, RootedPathError> {
        let path = path.as_ref();
        if !path.is_relative() {
            return Err(RootedPathError::PathNotRelative(path.to_path_buf()));
        }
        Ok(RootedPath { root: self.root.clone(), path: self.path.join(path) })
    }
    pub fn with_same_root(&self, path: impl AsRef<Path>) -> Result<RootedPath, RootedPathError> {
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

impl Into<PathBuf> for RootedPath {
    fn into(self) -> PathBuf {
        self.path
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_basic() -> Result<(), RootedPathError> {
        let p = RootedPath::new("/foo", "bar")?;
        assert_eq!(p.root(), Path::new("/foo"));
        assert_eq!(p.rel(), Path::new("bar"));
        assert_eq!(p.abs(), PathBuf::from("/foo/bar"));
        assert_eq!(p.join("baz")?, RootedPath::new("/foo", "bar/baz")?);
        assert_eq!(p.with_same_root("baz")?, RootedPath::new("/foo", "baz")?);
        Ok(())
    }

    #[test]
    fn test_errors() -> Result<(), RootedPathError> {
        assert!(RootedPath::new("foo", "bar").is_err());
        assert!(RootedPath::new("/foo", "/bar").is_err());
        let p = RootedPath::new("/foo", "bar")?;
        assert!(p.join("/baz").is_err());
        assert!(p.with_same_root("/baz").is_err());
        Ok(())
    }

    #[test]
    fn test_conversion() -> Result<(), RootedPathError> {
        let p = RootedPath::new(&"/foo", &"bar")?;

        let path = p.as_ref();
        assert_eq!(path, Path::new("/foo/bar"));

        let pathbuf: PathBuf = p.into();
        assert_eq!(pathbuf, Path::new("/foo/bar"));

        Ok(())
    }
}
