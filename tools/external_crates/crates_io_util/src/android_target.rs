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

use std::sync::LazyLock;

use cfg_expr::{
    targets::{get_builtin_target_by_triple, TargetInfo},
    Predicate,
};
use crates_index::{Dependency, Version};
use log::debug;

/// Parse cfg expressions in dependencies and determine if they refer to a target relevant to Android.
/// Dependencies are relevant if they are for Unix, Android, or Linux, and for an architecture we care about (Arm, RISC-V, or X86)
pub trait AndroidTarget {
    /// Returns true if this dependency is likely to be relevant to Android.
    fn is_android_target(&self) -> bool;
}

impl AndroidTarget for Dependency {
    fn is_android_target(&self) -> bool {
        self.target().is_none_or(is_android)
    }
}

static ANDROID_TARGETS: LazyLock<Vec<&'static TargetInfo>> = LazyLock::new(|| {
    vec![
        get_builtin_target_by_triple("aarch64-linux-android").unwrap(),
        get_builtin_target_by_triple("armv7-linux-androideabi").unwrap(),
        get_builtin_target_by_triple("i686-linux-android").unwrap(),
        get_builtin_target_by_triple("i686-unknown-linux-gnu").unwrap(),
        get_builtin_target_by_triple("riscv64-linux-android").unwrap(),
        get_builtin_target_by_triple("x86_64-linux-android").unwrap(),
        get_builtin_target_by_triple("x86_64-unknown-linux-gnu").unwrap(),
    ]
});

fn is_android(target: &str) -> bool {
    debug!("is_android({target})");
    let Ok(expr) = cfg_expr::Expression::parse(target) else {
        return false;
    };
    ANDROID_TARGETS.iter().any(|android_target| {
        debug!("Checking target {android_target:?}");
        expr.eval(|pred| match pred {
            Predicate::Target(target_predicate) => {
                let matches = target_predicate.matches(*android_target);
                debug!("Predicate::Target({target_predicate:?}) = {matches}");
                matches
            }
            Predicate::Flag(flag) => {
                debug!("Predicate::Flag({flag})");
                *flag == "mls_build_async" || *flag == "rustix_use_libc"
            }
            Predicate::KeyValue { key, val } => {
                let expr_val = *key != "getrandom_backend";
                debug!("Predicate::KeyValue(key = {key}, val = {val}) = {expr_val}");
                expr_val
            }
            _ => true,
        })
    })
}

/// Get the required Android dependencies of a crate.
pub trait RequiredAndroidDeps {
    /// Returns the required Android dependencies of a crate.
    /// That is, dependencies that are:
    /// * Non-optional
    /// * Normal (not Build or Dev)
    /// * If they have a target cfg expression, it applies to Android.
    fn required_android_deps(&self) -> impl DoubleEndedIterator<Item = &crates_index::Dependency>;
}

impl RequiredAndroidDeps for Version {
    fn required_android_deps(&self) -> impl DoubleEndedIterator<Item = &crates_index::Dependency> {
        self.dependencies().iter().filter(|dep| {
            dep.kind() == crates_index::DependencyKind::Normal
                && !dep.is_optional()
                && dep.is_android_target()
        })
    }
}

#[cfg(test)]
mod tests {
    use itertools::assert_equal;

    use super::*;

    fn init_logger() {
        let _ =
            env_logger::builder().filter_level(log::LevelFilter::max()).is_test(true).try_init();
    }

    #[test]
    fn test_android_cfgs() {
        init_logger();
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
        assert!(!is_android("cfg(tracing_unstable)"));
        assert!(is_android(r#"cfg(any(unix, target_os = "wasi"))"#));
        assert!(is_android(r#"cfg(not(all(target_arch = "arm", target_os = "none")))"#));
        assert!(is_android(r#"cfg(all(target_os = "linux", not(target_env = "musl")))"#));
        assert!(is_android("cfg(mls_build_async)"), "cfg that is enabled for mls-rs crates");
        assert!(
            is_android(
                r#"cfg(any(all(target_arch = "arm", target_pointer_width = "32"), target_arch = "mips", target_arch = "powerpc"))"#
            ),
            "32-bit arm"
        );
        assert!(is_android(
            "cfg(all(not(windows), any(rustix_use_libc, miri, not(all(target_os = \"linux\", target_endian = \"little\", any(target_arch = \"arm\", all(target_arch = \"aarch64\", target_pointer_width = \"64\"), target_arch = \"riscv64\", all(rustix_use_experimental_asm, target_arch = \"powerpc64\"), all(rustix_use_experimental_asm, target_arch = \"mips\"), all(rustix_use_experimental_asm, target_arch = \"mips32r6\"), all(rustix_use_experimental_asm, target_arch = \"mips64\"), all(rustix_use_experimental_asm, target_arch = \"mips64r6\"), target_arch = \"x86\", all(target_arch = \"x86_64\", target_pointer_width = \"64\")))))))"
        ), "rustix 0.38.31");
    }

    #[test]
    fn test_required_android_deps() {
        init_logger();

        let aarch64_paging_0_7_1: Version =
            serde_json::from_str(include_str!("testdata/aarch64-paging-0.7.1"))
                .expect("Failed to parse JSON testdata");
        assert_equal(
            aarch64_paging_0_7_1.required_android_deps(),
            [aarch64_paging_0_7_1
                .dependencies()
                .iter()
                .find(|dep| dep.crate_name() == "bitflags")
                .unwrap()],
        );
    }

    // getrandom 0.3.3 has a complex cfg expression for the libc dependency.
    #[test]
    fn test_getrandom_cfg() {
        init_logger();

        assert!(
            is_android(
                r#"cfg(all(any(target_os = "linux", target_os = "android"), not(any(all(target_os = "linux", target_env = ""), getrandom_backend = "custom", getrandom_backend = "linux_raw", getrandom_backend = "rdrand", getrandom_backend = "rndr"))))"#
            ),
            "getrandom 0.3.3"
        );

        let getrandom_0_3_3: Version =
            serde_json::from_str(include_str!("testdata/getrandom-0.3.3"))
                .expect("Failed to parse JSON testdata");
        assert_equal(
            getrandom_0_3_3.required_android_deps().map(|dep| dep.crate_name()),
            ["cfg-if", "libc"],
        );
    }
}
