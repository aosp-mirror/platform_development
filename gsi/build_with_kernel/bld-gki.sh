#!/bin/bash

# Copyright (C) 2021 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [ -z "${ANDROID_BUILD_TOP}" ]; then
    WD="$(pwd)"
else
    WD="${ANDROID_BUILD_TOP}"
fi
if [ -z "${OUT_DIR}" ]; then
    OUT_DIR="${WD}/out"
fi

GZIP="gzip"
LZ4="${WD}/kernel/prebuilts/kernel-build-tools/linux-x86/bin/lz4"

echo "WD=${WD}"
echo "OUT_DIR=${OUT_DIR}"

function linkto()
{
    local src=$1
    local dst=$2

    echo "ln -s ${src} ${dst}"
    mkdir -p "$(dirname "${dst}")"
    ln -sf "${src}" "${dst}"
}

function setup_dir()
{
    # Symlinks for Kernel building
    linkto "${WD}/prebuilts/clang/host/linux-x86" \
           "${WD}/kernel/prebuilts-master/clang/host/linux-x86"
    linkto "${WD}/prebuilts/gcc/linux-x86/host/x86_64-linux-glibc2.17-4.8" \
           "${WD}/kernel/prebuilts/gcc/linux-x86/host/x86_64-linux-glibc2.17-4.8"
    linkto "${WD}/prebuilts/build-tools" \
           "${WD}/kernel/prebuilts/build-tools"
    linkto "${WD}/out" \
           "${WD}/kernel/out"

    # An empty Android.mk to avoid Platform building the Android.mk under kernel/
    # touch "${WD}/kernel/Android.mk"
}

function bld_k510()
{
    local make_opt=("$@")
    local build_config=${BUILD_CONFIG:=common/build.config.gki.aarch64}
    pushd "${WD}/kernel"
    DIST_DIR="${OUT_DIR}/android12-5.10/dist" BUILD_CONFIG=${build_config} build/build.sh "${make_opt[@]}"
    popd
}

function bld_k510_x86_64()
{
    BUILD_CONFIG=common/build.config.gki.x86_64 \
        bld_k510 $@
}

function bld_k510_ko()
{
    local make_opt=("$@")
    local build_config=${BUILD_CONFIG:=common/build.config.gki.aarch64}
    local common_platform_config=${COMMON_PLATFORM_CONFIG:="common-modules/virtual-device/build.config.virtual_device.aarch64"}

    pushd "${WD}/kernel"
    BUILD_CONFIG=${build_config} build/build.sh "${make_opt[@]}"
    BUILD_CONFIG=${common_platform_config} build/build.sh "${make_opt[@]}"
    popd
}

function bld_k510_ko_x86_64()
{
    BUILD_CONFIG=common/build.config.gki.x86_64 \
    COMMON_PLATFORM_CONFIG=common-modules/virtual-device/build.config.cuttlefish.x86_64 \
        bld_k510_ko $@
}

function chk_k510_ko()
{
    local make_opt=("$@")
    local COMMON_PLATFORM_CONFIG="common-modules/virtual-device/build.config.virtual_device.aarch64"
    local COMMON_PLATFORM_SL="common/android/abi_gki_aarch64_virtual_device"
    local ABI_XML="common/android/abi_gki_aarch64.xml"
    pushd "${WD}/kernel"
    if [ -f "${COMMON_PLATFORM_SL}.ori" ]; then
        mv "${COMMON_PLATFORM_SL}.ori" ${COMMON_PLATFORM_SL}
    fi
    cp ${COMMON_PLATFORM_SL} "${COMMON_PLATFORM_SL}.ori"
    BUILD_CONFIG=common/build.config.gki.aarch64 build/build_abi.sh "${make_opt[@]}"
    BUILD_CONFIG=${COMMON_PLATFORM_CONFIG} build/build_abi.sh "${make_opt[@]}"
    BUILD_CONFIG=${COMMON_PLATFORM_CONFIG} build/build_abi.sh --update-symbol-list "${make_opt[@]}"
    if ! diff ${COMMON_PLATFORM_SL} "${COMMON_PLATFORM_SL}.ori"; then
        echo "${COMMON_PLATFORM_SL} is out-of-sync"
        return 1
    else
        echo "${COMMON_PLATFORM_SL} is up-to-date"
    fi
    popd
}

function repack()
{
    local src_img=$1
    local dst_dir=$2
    local version=$3

    mkdir -p "${dst_dir}"
    cp "${src_img}" "${dst_dir}/kernel-${version}"
    "${GZIP}" -nc \
       "${src_img}">"${dst_dir}/kernel-${version}-gz"
    "${LZ4}" -f -l -12 --favor-decSpeed \
       "${src_img}" "${dst_dir}/kernel-${version}-lz4"
}

function bld_arm64()
{
    setup_dir
    bld_k510 "${MAKE_OPT[@]}"
    repack "${OUT_DIR}/android12-5.10/dist/Image" "${OUT_DIR}/target/kernel/5.10/arm64" 5.10
    bld_k510_ko "${MAKE_OPT[@]}"
    #chk_k510_ko "${MAKE_OPT[@]}"
}

function bld_x86_64()
{
    setup_dir
    bld_k510_x86_64 "${MAKE_OPT[@]}"
    cp "${OUT_DIR}/android12-5.10/dist/bzImage" "${OUT_DIR}/target/kernel/5.10/x86_64/"
    bld_k510_ko_x86_64 "${MAKE_OPT[@]}"
}
