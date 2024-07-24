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
    echo "    Supported build targets:" \
         "${VALID_SOONG_BINARIES[@]}" "${VALID_SOONG_TESTS[@]}"
}

in_array () {
    value="$1"
    shift
    for i in "$@"; do
        [ "$i" = "${value}" ] && return 0
    done
    return 1
}

VALID_SOONG_BINARIES=(
    "bindgen"
    "cxx_extractor"
    "header-abi-linker"
    "header-abi-dumper"
    "header-abi-diff"
    "ide_query_cc_analyzer"
    "proto_metadata_plugin"
    "protoc_extractor"
    "versioner"
)

VALID_SOONG_TESTS=(
    "header-checker-unittests"
)

BUILD_TARGETS=()

while [ $# -gt 0 ]; do
    case $1 in
        -*) # Display help.
            usage
            exit 0
            ;;
        *) # Add specified build targets into BUILD_TARGETS
            BUILD_TARGETS+=("$1")
            ;;
    esac
    shift
done

set -ex

source "$(dirname "$0")/envsetup.sh"

if [ "$(uname)" != "Linux" ]; then
    echo "error: Unsupported uname: $(uname)"
    exit 1
fi

# Targets to be built
SOONG_BINARIES=()
SOONG_TESTS=()

# Check if all specified targets are valid
for name in "${BUILD_TARGETS[@]}"; do
    if in_array "${name}" "${VALID_SOONG_BINARIES[@]}"; then
        SOONG_BINARIES+=("${name}")
    elif in_array "${name}" "${VALID_SOONG_TESTS[@]}"; then
        SOONG_TESTS+=("${name}")
    else
        echo "build_target ${name} is not one of the supported targets:" \
             "${VALID_SOONG_BINARIES[@]}" "${VALID_SOONG_TESTS[@]}"
        exit 1
    fi
done

if [ "${#BUILD_TARGETS[@]}" -eq 0 ]; then
    # Build everything by default.
    SOONG_BINARIES=("${VALID_SOONG_BINARIES[@]}")
    SOONG_TESTS=("${VALID_SOONG_TESTS[@]}")
fi

if [ -z "${OUT_DIR}" ]; then
    echo "error: Must set OUT_DIR"
    exit 1
fi

TOP=$(pwd)

# Setup Soong configuration
SOONG_OUT="${OUT_DIR}/soong"
SOONG_HOST_OUT="${OUT_DIR}/soong/host/linux-x86"
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

# Build binaries and shared libs
build/soong/soong_ui.bash --make-mode --skip-config --soong-only \
  "${binaries[@]}" "${SOONG_TESTS[@]}"

# Copy binaries and shared libs
SOONG_DIST="${SOONG_OUT}/dist"
mkdir -p "${SOONG_DIST}/bin"
if [ -n "${binaries}" ]; then
    cp "${binaries[@]}" "${SOONG_DIST}/bin"
fi
cp -R "${SOONG_HOST_OUT}/lib64" "${SOONG_DIST}"
# create symlink lib -> lib64 as toolchain libraries have a RUNPATH pointing to
# $ORIGIN/../lib instead of lib64
ln -s "lib64" "${SOONG_DIST}/lib"

# Copy clang header and share files
CLANG_DIR="prebuilts/clang/host/linux-x86/${LLVM_PREBUILTS_VERSION}"
CLANG_LIB_DIR="${CLANG_DIR}/lib/clang/${LLVM_RELEASE_VERSION}"
CLANG_LIB_DIR_OUT="${SOONG_DIST}/lib/clang/${LLVM_RELEASE_VERSION}"
mkdir -p "${CLANG_LIB_DIR_OUT}"
cp -R "${CLANG_LIB_DIR}/share" "${CLANG_LIB_DIR_OUT}/share"
cp -R "${CLANG_LIB_DIR}/include" "${CLANG_LIB_DIR_OUT}/include"
ln -s "lib/clang/${LLVM_RELEASE_VERSION}/include" "${SOONG_DIST}/clang-headers"

# Normalize library file names.  All library file names must match their soname.
function extract_soname () {
    local file="$1"
    readelf -d "${file}" | \
        grep '(SONAME)\s*Library soname: \[.*\]$' -o | \
        sed 's/(SONAME)\s*Library soname: \[\(.*\)\]$/\1/g'
}

for file in "${SOONG_OUT}/dist/lib"*"/"*; do
    soname="$(extract_soname "${file}")"
    if [ -n "${soname}" -a "$(basename "${file}")" != "${soname}" ]; then
        mv "${file}" "$(dirname "${file}")/${soname}"
    fi
done

# Package binaries and shared libs
if [ -z "${DIST_DIR}" ]; then
    echo "DIST_DIR is empty. Skip zipping binaries."
else
    pushd "${SOONG_OUT}/dist"
    zip -qryX build-prebuilts.zip *
    popd
    mkdir -p "${DIST_DIR}" || true
    cp "${SOONG_OUT}/dist/build-prebuilts.zip" "${DIST_DIR}/"
fi
