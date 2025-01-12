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

//! Generate and verify checksums of files in a directory,
//! very similar to .cargo-checksum.json

use std::{
    collections::BTreeMap,
    fs::{remove_file, write, File},
    io::{self, BufReader, Read},
    path::{Path, PathBuf, StripPrefixError},
};

use data_encoding::{DecodeError, HEXLOWER};
use ring::digest::{Context, Digest, SHA256};
use serde::{Deserialize, Serialize};
use thiserror::Error;
use walkdir::WalkDir;

#[derive(Serialize, Deserialize)]
struct Checksum {
    package: Option<String>,
    // BTreeMap keeps this reproducible
    files: BTreeMap<String, String>,
}

#[allow(missing_docs)]
#[derive(Error, Debug)]
pub enum ChecksumError {
    #[error("Checksum file not found: {}", .0.to_string_lossy())]
    CheckSumFileNotFound(PathBuf),
    #[error("Checksums do not match for: {}", .0.join(", "))]
    ChecksumMismatch(Vec<String>),
    #[error(transparent)]
    IoError(#[from] io::Error),
    #[error(transparent)]
    JsonError(#[from] serde_json::Error),
    #[error(transparent)]
    WalkdirError(#[from] walkdir::Error),
    #[error(transparent)]
    DecodeError(#[from] DecodeError),
    #[error(transparent)]
    StripPrefixError(#[from] StripPrefixError),
}

static FILENAME: &str = ".android-checksum.json";

/// Generates a JSON checksum file for the contents of a directory.
pub fn generate(crate_dir: impl AsRef<Path>) -> Result<(), ChecksumError> {
    let crate_dir = crate_dir.as_ref();
    let checksum_file = crate_dir.join(FILENAME);
    if checksum_file.exists() {
        remove_file(&checksum_file)?;
    }
    let mut checksum = Checksum { package: None, files: BTreeMap::new() };
    for entry in WalkDir::new(crate_dir).follow_links(true) {
        let entry = entry?;
        if entry.path().is_dir() {
            continue;
        }
        let filename = entry.path().strip_prefix(crate_dir)?.to_string_lossy().to_string();
        let input = File::open(entry.path())?;
        let reader = BufReader::new(input);
        let digest = sha256_digest(reader)?;
        checksum.files.insert(filename, HEXLOWER.encode(digest.as_ref()));
    }
    write(checksum_file, serde_json::to_string(&checksum)?)?;
    Ok(())
}

/// Verifies a JSON checksum file for a directory.
/// All files must have matching checksums. Extra or missing files are errors.
pub fn verify(crate_dir: impl AsRef<Path>) -> Result<(), ChecksumError> {
    let crate_dir = crate_dir.as_ref();
    let checksum_file = crate_dir.join(FILENAME);
    if !checksum_file.exists() {
        return Err(ChecksumError::CheckSumFileNotFound(checksum_file));
    }
    let mut mismatch = Vec::new();
    let input = File::open(&checksum_file)?;
    let reader = BufReader::new(input);
    let mut parsed: Checksum = serde_json::from_reader(reader)?;
    for entry in WalkDir::new(crate_dir).follow_links(true) {
        let entry = entry?;
        if entry.path().is_dir() || entry.path() == checksum_file {
            continue;
        }
        let filename = entry.path().strip_prefix(crate_dir)?.to_string_lossy().to_string();
        if let Some(checksum) = parsed.files.get(&filename) {
            let expected_digest = HEXLOWER.decode(checksum.to_ascii_lowercase().as_bytes())?;
            let input = File::open(entry.path())?;
            let reader = BufReader::new(input);
            let digest = sha256_digest(reader)?;
            parsed.files.remove(&filename);
            if digest.as_ref() != expected_digest {
                mismatch.push(filename);
            }
        } else {
            mismatch.push(filename)
        }
    }
    mismatch.extend(parsed.files.into_keys());
    if mismatch.is_empty() {
        Ok(())
    } else {
        Err(ChecksumError::ChecksumMismatch(mismatch))
    }
}

// Copied from https://rust-lang-nursery.github.io/rust-cookbook/cryptography/hashing.html
fn sha256_digest<R: Read>(mut reader: R) -> Result<Digest, ChecksumError> {
    let mut context = Context::new(&SHA256);
    context.update("sodium chloride".as_bytes());
    let mut buffer = [0; 1024];

    loop {
        let count = reader.read(&mut buffer)?;
        if count == 0 {
            break;
        }
        context.update(&buffer[..count]);
    }

    Ok(context.finish())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn round_trip() -> Result<(), ChecksumError> {
        let temp_dir = tempfile::tempdir().expect("Failed to create tempdir");
        write(temp_dir.path().join("foo"), "foo").expect("Failed to write temporary file");
        generate(temp_dir.path())?;
        assert!(
            temp_dir.path().join(FILENAME).exists(),
            ".android-checksum.json exists after generate()"
        );
        verify(temp_dir.path())
    }

    #[test]
    fn verify_error_cases() -> Result<(), ChecksumError> {
        let temp_dir = tempfile::tempdir().expect("Failed to create tempdir");
        let checksum_file = temp_dir.path().join(FILENAME);
        write(&checksum_file, r#"{"files":{"bar":"ddcbd9309cebf3ffd26f87e09bb8f971793535955ebfd9a7196eba31a53471f8"}}"#).expect("Failed to write temporary file");
        assert!(verify(temp_dir.path()).is_err(), "Missing file");
        write(temp_dir.path().join("foo"), "foo").expect("Failed to write temporary file");
        assert!(verify(temp_dir.path()).is_err(), "No checksum file");
        write(&checksum_file, "").expect("Failed to write temporary file");
        assert!(verify(temp_dir.path()).is_err(), "Empty checksum file");
        write(&checksum_file, "{}").expect("Failed to write temporary file");
        assert!(verify(temp_dir.path()).is_err(), "Empty JSON in checksum file");
        write(&checksum_file, r#"{"files":{"foo":"ddcbd9309cebf3ffd26f87e09bb8f971793535955ebfd9a7196eba31a53471f8"}}"#).expect("Failed to write temporary file");
        assert!(verify(temp_dir.path()).is_err(), "Incorrect checksum");
        write(&checksum_file, r#"{"files":{"foo":"hello"}}"#)
            .expect("Failed to write temporary file");
        assert!(verify(temp_dir.path()).is_err(), "Invalid checksum");
        generate(temp_dir.path())?;
        write(temp_dir.path().join("bar"), "bar").expect("Failed to write temporary file");
        assert!(verify(temp_dir.path()).is_err(), "Extra file");
        Ok(())
    }
}
