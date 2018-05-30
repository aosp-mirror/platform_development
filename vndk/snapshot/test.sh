#!/bin/bash
#
# Copyright (C) 2017 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Test script for build/make/core/tasks/vndk.mk.
# Makes sure VNDK snapshots include all required prebuilts and config files.
#
# Local usage:
#   First, generate VNDK snapshots with development/vndk/snapshot/build.sh or
#   fetch VNDK snapshot build artifacts to $DIST_DIR, then run this script.

set -eo pipefail

if [[ "$#" -ne 1 ]]; then
    echo "Usage: \"$0 all\" to test all VNDK snapshot variants at once."
    echo "       \"$0 \$TARGET_PRODUCT\" to test a specific VNDK snapshot."
    exit 1
fi

if [[ "$1" == 'all' ]]; then
    readonly TARGET_PRODUCTS=('aosp_arm' 'aosp_arm_ab' 'aosp_arm64' 'aosp_x86' 'aosp_x86_ab' 'aosp_x86_64')
else
    readonly TARGET_PRODUCTS=($1)
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly ANDROID_BUILD_TOP=$(dirname $(dirname $(dirname "${script_dir}")))
echo "ANDROID_BUILD_TOP: "${ANDROID_BUILD_TOP}""

OUT_DIR=${OUT_DIR:-}
DIST_DIR=${DIST_DIR:-}
if [[ -z "${DIST_DIR}" ]]; then
    if [[ -z "${OUT_DIR}" ]]; then
        DIST_DIR="${ANDROID_BUILD_TOP}"/out/dist
    else
        DIST_DIR="${OUT_DIR}"/dist
    fi
fi

# Get PLATFORM_VNDK_VERSION
source ""${ANDROID_BUILD_TOP}"/build/envsetup.sh" >/dev/null
readonly PLATFORM_VNDK_VERSION="$(get_build_var PLATFORM_VNDK_VERSION)"

readonly TEMP_DIR="$(mktemp -d /tmp/"$(basename $0)"_XXXXXXXX)"
readonly SNAPSHOT_TOP="${TEMP_DIR}"/android-vndk-snapshot
readonly SNAPSHOT_TEMPFILE="${TEMP_DIR}"/snapshot_libs.txt
readonly SYSTEM_TEMPFILE="${TEMP_DIR}"/system_libs.txt
readonly BINDER_32_DIRNAME='binder32'

readonly RED='\033[0;31m'
readonly NC='\033[0m'
readonly PASS="::: PASS :::"
readonly FAIL=""${RED}"::: FAIL :::"${NC}""


function set_vars() {
    TARGET_PRODUCT="$1"
    ARCH=''
    PRODUCT_OUT=''
    BITNESS_SUFFIX=''
    BINDER_BITNESS_PATH=''
    TARGET_2ND_ARCH=''
    case "$1" in
        aosp_arm64)
            ARCH='arm64'
            PRODUCT_OUT='generic_arm64'
            BITNESS_SUFFIX='64'
            TARGET_2ND_ARCH='arm'
            ;;
        aosp_arm)
            ARCH='arm'
            PRODUCT_OUT='generic'
            ;;
        aosp_arm_ab)
            ARCH='arm'
            PRODUCT_OUT='generic_arm_ab'
            BINDER_BITNESS_PATH="${BINDER_32_DIRNAME}"
            ;;
        aosp_x86_64)
            ARCH='x86_64'
            PRODUCT_OUT='generic_x86_64'
            BITNESS_SUFFIX='64'
            TARGET_2ND_ARCH='x86'
            ;;
        aosp_x86)
            ARCH='x86'
            PRODUCT_OUT='generic_x86'
            ;;
        aosp_x86_ab)
            ARCH='x86'
            PRODUCT_OUT='generic_x86'
            BINDER_BITNESS_PATH="${BINDER_32_DIRNAME}"
            ;;
        *)
            echo "Unrecognized \$TARGET_PRODUCT: "$1""
            exit 1
            ;;
    esac
}


function cleanup {
    echo "[Cleanup]"
    echo "Removing TEMP_DIR: "${TEMP_DIR}""
    rm -rf ""${TEMP_DIR}""
}
trap cleanup EXIT


#######################################
# Compares the list of VNDK-core and VNDK-SP
# libs included in the snapshot and installed
# under $PRODUCT_OUT/system/lib*
#
# Arguments:
#   $1: vndk_type: one of [vndk-core, vndk-sp]
#######################################
function compare_vndk_libs() {
    local vndk_type="$1"
    local vndk_dir_suffix
    local system_vndk_dir
    local snapshot_dir
    local snapshot_dir_2nd
    local system_lib_dir
    local system_lib_dir_2nd

    if [[ -z "${PLATFORM_VNDK_VERSION}" ]]; then
        vndk_dir_suffix=""
    else
        vndk_dir_suffix="-${PLATFORM_VNDK_VERSION}"
    fi

    if [[ "${vndk_type}" == 'vndk-core' ]]; then
        system_vndk_dir="vndk${vndk_dir_suffix}"
    else
        system_vndk_dir="vndk-sp${vndk_dir_suffix}"
    fi

    function diff_vndk_dirs() {
        local snapshot="$1"
        local system="$2"
        local target_arch="$3"

        ls -1 ${snapshot} > "${SNAPSHOT_TEMPFILE}"
        find "${system}" -type f | xargs -n 1 -I file bash -c "basename file" | sort > "${SYSTEM_TEMPFILE}"

        echo "Comparing libs for TARGET_PRODUCT="${TARGET_PRODUCT}", VNDK="${vndk_type}", ARCH="${target_arch}""
        echo "Snapshot dir:" ${snapshot}
        echo "System dir: "${system}""
        (diff --old-line-format="Only found in VNDK snapshot: %L" \
              --new-line-format="Only found in /system/lib*: %L" \
              --unchanged-line-format="" \
              "${SNAPSHOT_TEMPFILE}" "${SYSTEM_TEMPFILE}" && echo "${PASS}") \
        || (echo -e "${FAIL}"; exit 1)
    }

    if [[ -n "${BINDER_BITNESS_PATH}" ]]; then
        snapshot_dir="${SNAPSHOT_TOP}"/"${ARCH}"/"${BINDER_BITNESS_PATH}"/arch-"${ARCH}"-*/shared/"${vndk_type}"
    else
        snapshot_dir="${SNAPSHOT_TOP}"/"${ARCH}"/arch-"${ARCH}"-*/shared/"${vndk_type}"
    fi

    system_lib_dir="${ANDROID_BUILD_TOP}"/out/target/product/"${PRODUCT_OUT}"/system/lib"${BITNESS_SUFFIX}"/"${system_vndk_dir}"
    diff_vndk_dirs "${snapshot_dir}" $system_lib_dir "${ARCH}"

    if [[ -n "${TARGET_2ND_ARCH}" ]]; then
        snapshot_dir_2nd="${SNAPSHOT_TOP}"/"${ARCH}"/arch-"${TARGET_2ND_ARCH}"-*/shared/"${vndk_type}"
        system_lib_dir_2nd="${ANDROID_BUILD_TOP}"/out/target/product/"${PRODUCT_OUT}"/system/lib/"${system_vndk_dir}"
        diff_vndk_dirs "${snapshot_dir_2nd}" "${system_lib_dir_2nd}" "${TARGET_2ND_ARCH}"
    fi
}


#######################################
# Executes tests against VNDK snapshot of
# specified $TARGET_PRODUCT
#
# Arguments:
#   $1: TARGET_PRODUCT
#######################################
function run_tests() {
    set_vars "$1"
    local snapshot_zip="${DIST_DIR}"/android-vndk-"${TARGET_PRODUCT}".zip
    local snapshot_variant_top="${SNAPSHOT_TOP}"/"${ARCH}"

    echo "[Setup] Unzipping \"android-vndk-"${TARGET_PRODUCT}".zip\""
    unzip -qn "${snapshot_zip}" -d "${SNAPSHOT_TOP}"

    echo "[Test] Comparing VNDK-core and VNDK-SP libs in snapshot vs /system/lib*"
    compare_vndk_libs 'vndk-core'
    compare_vndk_libs 'vndk-sp'

    echo "[Test] Checking required config files are present"
    if [[ -z "${PLATFORM_VNDK_VERSION}" ]]; then
        config_file_suffix=""
    else
        config_file_suffix=".${PLATFORM_VNDK_VERSION}"
    fi

    config_files=(
        "ld.config"${config_file_suffix}".txt"
        "llndk.libraries"${config_file_suffix}".txt"
        "vndksp.libraries"${config_file_suffix}".txt"
        "vndkcore.libraries.txt"
        "vndkprivate.libraries.txt"
        "module_paths.txt")
    for config_file in "${config_files[@]}"; do
        config_file_abs_path="${snapshot_variant_top}"/configs/"${config_file}"
        if [[ ! -e "${config_file_abs_path}" ]]; then
            echo -e ""${FAIL}" The file \""${config_file_abs_path}"\" was not found in snapshot."
            exit 1
        else
            echo ""${PASS}" Found "${config_file}""
        fi
    done

    echo "[Test] Checking directory structure of snapshot"
    directories=(
        "configs/"
        "NOTICE_FILES/")
    for sub_dir in "${directories[@]}"; do
        dir_abs_path="${snapshot_variant_top}"/"${sub_dir}"
        if [[ ! -d "${dir_abs_path}" ]]; then
            echo -e ""${FAIL}" The directory \""${dir_abs_path}"\" was not found in snapshot."
            exit 1
        else
            echo ""${PASS}" Found "${sub_dir}""
        fi
    done
}


# Run tests for each target product
for target_product in "${TARGET_PRODUCTS[@]}"; do
    echo -e "\n::::::::: Running tests for TARGET_PRODUCT="${target_product}" :::::::::"
    run_tests "${target_product}"
done

echo "Done. All tests passed!"
