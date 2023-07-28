#!/bin/bash -e

# Copyright 2019 Google Inc. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

usage() {
    echo "Usage: $(basename "$0") [build_target]..."
    echo "    Build all targets if build_target is not specified."
    echo "    Supported build targets for macOS: ${MACOS_SOONG_BINARIES[*]}"
    echo "    Supported build targets for Linux: ${LINUX_SOONG_BINARIES[*]}"
}

valid_build_target () {
    for i in "${VALID_SOONG_BINARIES[@]}"; do
        if [ "$i" = "$1" ]; then
            return 0
        fi
    done
    return 1
}

SOONG_BINARIES=()

while [ $# -gt 0 ]; do
    case $1 in
        -*) # Display help.
            usage
            exit 0
            ;;
        *) # Add specified build targets into SOONG_BINARIES
            SOONG_BINARIES+=("$1")
            ;;
    esac
    shift
done

set -ex

source "$(dirname "$0")/envsetup.sh"

UNAME="$(uname)"
case "${UNAME}" in
Linux)
    OS='linux'
    ;;
Darwin)
    OS='darwin'
    ;;
*)
    echo "error: Unknown uname: ${UNAME}"
    exit 1
    ;;
esac

LINUX_SOONG_BINARIES=(
    "bindgen"
    "cxx_extractor"
    "header-abi-linker"
    "header-abi-dumper"
    "header-abi-diff"
    "proto_metadata_plugin"
    "protoc_extractor"
    "versioner"
)

MACOS_SOONG_BINARIES=(
    "versioner"
)

# Targets to be built
if [ "${OS}" = "darwin" ]; then
    VALID_SOONG_BINARIES=("${MACOS_SOONG_BINARIES[@]}")
else
    VALID_SOONG_BINARIES=("${LINUX_SOONG_BINARIES[@]}")
fi

if [ "${#SOONG_BINARIES[@]}" -eq 0 ]; then
    # SOONG_BINARIES is empty, so there must be no commandline argument, thus we
    # build everything.
    SOONG_BINARIES=("${VALID_SOONG_BINARIES[@]}")
fi

# Check if all specified targets are valid
for name in "${SOONG_BINARIES[@]}"; do
  if ! valid_build_target "${name}"; then
    echo "build_target ${name} is not one of the supported targets: ${VALID_SOONG_BINARIES[*]}"
    exit 1
  fi
done

if [ -z "${OUT_DIR}" ]; then
    echo "error: Must set OUT_DIR"
    exit 1
fi

TOP=$(pwd)

# Setup Soong configuration
SOONG_OUT="${OUT_DIR}/soong"
SOONG_HOST_OUT="${OUT_DIR}/soong/host/${OS}-x86"
rm -rf "${SOONG_OUT}"
mkdir -p "${SOONG_OUT}"
cat > "${SOONG_OUT}/soong.variables" << __EOF__
{
    "Allow_missing_dependencies": true,
    "HostArch":"x86_64"
}
__EOF__

# Allow unknown warning options since this may lag behind platform's compiler
# version.
export ALLOW_UNKNOWN_WARNING_OPTION=true

binaries=()
for name in "${SOONG_BINARIES[@]}"; do
    binaries+=("${SOONG_HOST_OUT}/bin/${name}")
done

libs=()
if [ "${OS}" = "darwin" ]; then
    libs+=("${SOONG_HOST_OUT}/lib64/libc++abi_host.dylib")
fi

# Build binaries and shared libs
build/soong/soong_ui.bash --make-mode --skip-config --soong-only "${binaries[@]}" "${libs[@]}"

# Copy binaries and shared libs
SOONG_DIST="${SOONG_OUT}/dist"
mkdir -p "${SOONG_DIST}/bin"
cp "${binaries[@]}" "${SOONG_DIST}/bin"
cp -R "${SOONG_HOST_OUT}/lib64" "${SOONG_DIST}"
# create symlink lib -> lib64 as toolchain libraries have a RUNPATH pointing to
# $ORIGIN/../lib instead of lib64
ln -s "lib64" "${SOONG_DIST}/lib"

# Copy clang header and share files
CLANG_DIR="prebuilts/clang/host/${OS}-x86/${LLVM_PREBUILTS_VERSION}"
CLANG_LIB_DIR="${CLANG_DIR}/lib/clang/${LLVM_RELEASE_VERSION}"
CLANG_LIB_DIR_OUT="${SOONG_DIST}/lib/clang/${LLVM_RELEASE_VERSION}"
mkdir -p "${CLANG_LIB_DIR_OUT}"
cp -R "${CLANG_LIB_DIR}/share" "${CLANG_LIB_DIR_OUT}/share"
cp -R "${CLANG_LIB_DIR}/include" "${CLANG_LIB_DIR_OUT}/include"
ln -s "lib/clang/${LLVM_RELEASE_VERSION}/include" "${SOONG_DIST}/clang-headers"

# Normalize library file names.  All library file names must match their soname.
function extract_soname () {
    local file="$1"

    case "${OS}" in
    linux)
        readelf -d "${file}" | \
            grep '(SONAME)\s*Library soname: \[.*\]$' -o | \
            sed 's/(SONAME)\s*Library soname: \[\(.*\)\]$/\1/g'
        ;;
    darwin)
        local install_path="$(otool -D "${file}" | sed -n 2p)"
        if [ -n "${install_path}" ]; then
            basename "${install_path}"
        fi
        ;;
    esac
}

for file in "${SOONG_OUT}/dist/lib"*"/"*; do
    soname="$(extract_soname "${file}")"
    if [ -n "${soname}" -a "$(basename "${file}")" != "${soname}" ]; then
        mv "${file}" "$(dirname "${file}")/${soname}"
    fi
done

# Package binaries and shared libs
(
    cd "${SOONG_OUT}/dist"
    zip -qryX build-prebuilts.zip *
)

if [ -n "${DIST_DIR}" ]; then
    mkdir -p "${DIST_DIR}" || true
    cp "${SOONG_OUT}/dist/build-prebuilts.zip" "${DIST_DIR}/"
fi
