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

use cfg_expr::{
    targets::{Arch, Family, Os},
    Predicate, TargetPredicate,
};
use crates_index::Dependency;

/// Parse cfg expressions in dependencies and determine if they refer to a target relevant to Android.
/// Dependencies are relevant if they are for Unix, Android, or Linux, and for an architecture we care about (Arm, RISC-V, or X86)
pub trait AndroidTarget {
    /// Returns true if this dependency is likely to be relevant to Android.
    fn is_android_target(&self) -> bool;
}

impl AndroidTarget for Dependency {
    fn is_android_target(&self) -> bool {
        self.target().map_or(true, is_android)
    }
}

fn is_android(target: &str) -> bool {
    let expr = cfg_expr::Expression::parse(target);
    if expr.is_err() {
        return false;
    }
    let expr = expr.unwrap();
    expr.eval(|pred| match pred {
        Predicate::Target(target_predicate) => match target_predicate {
            TargetPredicate::Family(family) => *family == Family::unix,
            TargetPredicate::Os(os) => *os == Os::android || *os == Os::linux,
            TargetPredicate::Arch(arch) => {
                [Arch::arm, Arch::aarch64, Arch::riscv32, Arch::riscv64, Arch::x86, Arch::x86_64]
                    .contains(arch)
            }
            _ => true,
        },
        _ => true,
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn test_android_cfgs() {
        assert!(!is_android("asmjs-unknown-emscripten"), "Parse error");
        assert!(!is_android("cfg(windows)"));
        assert!(is_android("cfg(unix)"));
        assert!(!is_android(r#"cfg(target_os = "redox")"#));
        assert!(!is_android(r#"cfg(target_arch = "wasm32")"#));
        assert!(is_android(r#"cfg(any(target_os = "linux", target_os = "android"))"#));
        assert!(is_android(
            r#"cfg(any(all(target_arch = "arm", target_pointer_width = "32"), target_arch = "mips", target_arch = "powerpc"))"#
        ));
        assert!(!is_android(
            r#"cfg(all(target_arch = "wasm32", target_vendor = "unknown", target_os = "unknown"))"#
        ));
        assert!(is_android("cfg(tracing_unstable)"));
        assert!(is_android(r#"cfg(any(unix, target_os = "wasi"))"#));
        assert!(is_android(r#"cfg(not(all(target_arch = "arm", target_os = "none")))"#))
    }
}
