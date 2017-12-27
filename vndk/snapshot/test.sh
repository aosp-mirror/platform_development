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

set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "Usage: \"$0 all\" to test all four arch snapshots at once."
    echo "Usage: \"$0 TARGET_ARCH\" to test for a VNDK snapshot of a specific arch."
    exit 1
fi

if [[ $1 == 'all' ]]; then
    ARCHS=('arm' 'arm64' 'x86' 'x86_64')
else
    ARCHS=($1)
fi

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ANDROID_BUILD_TOP=$(dirname $(dirname $(dirname $script_dir)))
echo "ANDROID_BUILD_TOP: $ANDROID_BUILD_TOP"

OUT_DIR=${OUT_DIR:-}
DIST_DIR=${DIST_DIR:-}
if [[ -z $DIST_DIR ]]; then
    if [[ -z $OUT_DIR ]]; then
        DIST_DIR=$ANDROID_BUILD_TOP/out/dist
    else
        DIST_DIR=$OUT_DIR/dist
    fi
fi

SNAPSHOT_TOP=$DIST_DIR/android-vndk-snapshot
SNAPSHOT_TEMPFILE=$DIST_DIR/snapshot_libs.txt
SYSTEM_TEMPFILE=$DIST_DIR/system_libs.txt

RED='\033[0;31m'
NC='\033[0m'
PASS="::: PASS :::"
FAIL="${RED}::: FAIL :::${NC}"



function remove_unzipped_snapshot {
    if [ -d $SNAPSHOT_TOP ]; then
        echo "Removing $SNAPSHOT_TOP"
        rm -rf $SNAPSHOT_TOP
    fi
}


#######################################
# Compares the list of VNDK-core and VNDK-SP
# libs included in the snapshot and installed
# under $PRODUCT_OUT/system/lib*
#
# Arguments:
#   $1: vndk_type: string, one of [vndk-core, vndk-sp]
#   $2: arch: string, one of [arm, arm64, x86, x86_64]
#######################################
function compare_vndk_libs() {
    local vndk_type=$1
    local arch=$2
    local product
    local bitness
    local snapshot_dir
    local system_vndk_dir
    local system_dir

    if [[ $arch == 'arm64' ]]; then
        product='generic_arm64_ab'
    elif [[ $arch == 'arm' ]]; then
        product='generic_arm_ab'
    elif [[ $arch == 'x86_64' ]]; then
        product='generic_x86_64_ab'
    elif [[ $arch == 'x86' ]]; then
        product='generic_x86_ab'
    fi

    if [[ ${arch:-2:length} =~ '64' ]]; then
        bitness='64'
    else
        bitness=''
    fi

    snapshot_dir=$SNAPSHOT_TOP/arch-$arch*/shared/$vndk_type

    if [[ $vndk_type == 'vndk-core' ]]; then
        system_vndk_dir='vndk'
    else
        system_vndk_dir='vndk-sp'
    fi

    system_dir=$ANDROID_BUILD_TOP/out/target/product/$product/system/lib$bitness/$system_vndk_dir

    ls -1 $snapshot_dir > $SNAPSHOT_TEMPFILE
    find $system_dir -type f | xargs -n 1 -I file bash -c "basename file" | sort > $SYSTEM_TEMPFILE

    echo "Comparing libs for VNDK=$vndk_type and ARCH=$arch"
    (diff --old-line-format="Only found in VNDK snapshot: %L" --new-line-format="Only found in system/lib*: %L" \
      --unchanged-line-format="" $SNAPSHOT_TEMPFILE $SYSTEM_TEMPFILE && echo $PASS) \
    || (echo -e $FAIL; exit 1)
}


#######################################
# Executes testcases against VNDK snapshot of specified arch
#
# Arguments:
#   $1: arch: string, one of [arm, arm64, x86, x86_64]
#######################################
function run_test_cases() {
    local arch=$1
    local snapshot_zip=$DIST_DIR/android-vndk-$arch.zip

    echo "[Setup] Unzipping \"android-vndk-$arch.zip\""
    unzip -q $snapshot_zip -d $SNAPSHOT_TOP

    echo "[Test] Comparing VNDK-core and VNDK-SP libs in snapshot vs system/lib*"
    compare_vndk_libs 'vndk-core' $arch
    compare_vndk_libs 'vndk-sp' $arch

    echo "[Test] Checking required config files are present"
    config_files=(
        'ld.config.txt'
        'llndk.libraries.txt'
        'module_paths.txt'
        'vndkcore.libraries.txt'
        'vndkprivate.libraries.txt'
        'vndksp.libraries.txt')
    for config_file in "${config_files[@]}"; do
        config_file_abs_path=$SNAPSHOT_TOP/arch-$arch*/configs/$config_file
        if [ ! -e $config_file_abs_path ]; then
            echo -e "$FAIL The file \"$config_file_abs_path\" was not found in snapshot."
            exit 1
        else
            echo "$PASS Found $config_file"
        fi
    done

    echo "[Test] Checking directory structure of snapshot"
    directories=(
        'configs/'
        'NOTICE_FILES/'
        'shared/vndk-core/'
        'shared/vndk-sp/')
    for sub_dir in "${directories[@]}"; do
        dir_abs_path=$SNAPSHOT_TOP/arch-$arch*/$sub_dir
        if [ ! -d $dir_abs_path ]; then
            echo -e "$FAIL The directory \"$dir_abs_path\" was not found in snapshot."
            exit 1
        else
            echo "$PASS Found $sub_dir"
        fi
    done
}


#######################################
# Cleanup
#######################################
function cleanup {
    echo "[Cleanup]"
    remove_unzipped_snapshot
    echo "[Cleanup] Removing temp files..."
    rm -f $SNAPSHOT_TEMPFILE $SYSTEM_TEMPFILE
}
trap cleanup EXIT


#######################################
# Run testcases
#######################################
remove_unzipped_snapshot
for arch in "${ARCHS[@]}"; do
    echo -e "\n::::::::: Running testcases for ARCH=$arch :::::::::"
    run_test_cases $arch
done

echo "All tests passed!"
