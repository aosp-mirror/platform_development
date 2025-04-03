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

//! Traits to reduce boilerplate when running commands.
//! Typically, an unsuccessful exit status indicates a problem,
//! and these traits make it easy to turn that into an error.

use std::{
    fmt::Display,
    process::{Command, ExitStatus, Output},
};

/// Error types for the 'success_or_error' crate.
#[derive(thiserror::Error, Debug)]
pub enum Error {
    /// Process failed
    #[error("Command failed with exit code {maybe_status}", maybe_status = MaybeExitCode::from(.0))]
    CommandFailed(Option<i32>),
    /// Process failed, with output.
    #[error("Command failed with exit code {}\nstdout:\n{stdout}\nstderr:\n{stderr}", MaybeExitCode::from(.code), )]
    CommandFailedwithOutput {
        /// The exit code.
        code: Option<i32>,
        /// The contents of STDOUT, lossily converted to UTF-8.
        stdout: String,
        /// The contents of STDERR, lossily converted to UTF-8.
        stderr: String,
    },
    /// Failed to run command
    #[error("Failed to run command: {0}")]
    FailedToRunCommand(#[from] std::io::Error),
}

/// Trait for converting unsuccessful process exit codes into Rust errors.
pub trait SuccessOrError {
    /// Returns an error if the process did not exit successfully.
    /// Otherwise, returns `self`.
    fn success_or_error(self) -> Result<Self, Error>
    where
        Self: std::marker::Sized;
}
impl SuccessOrError for ExitStatus {
    fn success_or_error(self) -> Result<Self, Error> {
        if !self.success() {
            Err(Error::CommandFailed(self.code()))
        } else {
            Ok(self)
        }
    }
}
impl SuccessOrError for Output {
    fn success_or_error(self) -> Result<Self, Error> {
        (&self).success_or_error()?;
        Ok(self)
    }
}
impl SuccessOrError for &Output {
    fn success_or_error(self) -> Result<Self, Error> {
        if !self.status.success() {
            Err(Error::from(self))
        } else {
            Ok(self)
        }
    }
}

/// Trait to run a command, capturing its output and checking that it exited successfully.
pub trait RunAndExpectSuccess {
    /// Run a command and return its output, or an error if the process did not
    /// exit successfully.
    fn run_quiet_and_expect_success(&mut self) -> Result<Output, Error>;
}
impl RunAndExpectSuccess for Command {
    fn run_quiet_and_expect_success(&mut self) -> Result<Output, Error> {
        self.output()?.success_or_error()
    }
}

// For pretty-printing optional exit codes.
struct MaybeExitCode<'a>(&'a Option<i32>);
impl Display for MaybeExitCode<'_> {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        self.0.map(|code| write!(f, "{}", code)).unwrap_or(f.write_str("(unknown)"))
    }
}
impl<'a> From<&'a Option<i32>> for MaybeExitCode<'a> {
    fn from(value: &'a Option<i32>) -> Self {
        MaybeExitCode(value)
    }
}
impl From<&Output> for Error {
    fn from(value: &Output) -> Self {
        Error::CommandFailedwithOutput {
            code: value.status.code(),
            stdout: String::from_utf8_lossy(&value.stdout).to_string(),
            stderr: String::from_utf8_lossy(&value.stderr).to_string(),
        }
    }
}
