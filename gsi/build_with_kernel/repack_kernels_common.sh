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

if [[ -z "${DIST_DIR}" ]]; then
  echo "DIST_DIR must be defined." 1>&2
  exit 1
fi

GZIP="gzip"
LZ4="${WD}/prebuilts/misc/linux-x86/lz4/lz4"


function prepare_kernel_image()
{
  local prebuilt_path=$1
  local kernel_version=$2
  local arch=$3
  local build_variant=$4

  local prebuilts_root="out/prebuilt_cached/${prebuilt_path}"
  local out_root="${OUT_DIR}/target/kernel/${kernel_version}/${arch}"
  local dist_root="${DIST_DIR}/kernel/${kernel_version}"
  local postfix=""
  if [[ "$build_variant" == "debug" ]]; then
     dist_root="${dist_root}-debug"
     postfix="-allsyms"
  fi

  mkdir -p "${out_root}"
  if [[ "$arch" == "x86_64" ]]; then
    cp "${prebuilts_root}/bzImage" "${out_root}/kernel-${kernel_version}${postfix}"
  else
    # Pack all compress format for kernel images
    cp "${prebuilts_root}/Image" "${out_root}/kernel-${kernel_version}${postfix}"
    cp "${prebuilts_root}/Image.lz4" "${out_root}/kernel-${kernel_version}-lz4${postfix}"
    "$GZIP" -nc \
       "${prebuilts_root}/Image">"${out_root}/kernel-${kernel_version}-gz${postfix}"
  fi

  # Prepare the dist folder
  mkdir -p "${dist_root}"

  # Prepare prebuilt-info.txt
  BID=$(cat "${prebuilts_root}/BUILD_INFO" | sed -n 's/^.*"bid":\s*"\(.*\)".*$/\1/p')
  cat > "${dist_root}/prebuilt-info.txt" <<EOF
{
    "kernel-build-id": ${BID}
}
EOF
}

function prepare_kernel_modules()
{
  local prebuilt_path=$1
  local kernel_version=$2
  local arch=$3

  local prebuilts_root="out/prebuilt_cached/${prebuilt_path}"
  local out_root="${OUT_DIR}/target/kernel/${kernel_version}/${arch}"
  local dist_root="${DIST_DIR}/kernel/${kernel_version}"

  local initramfs_root="${out_root}/initramfs"
  mkdir -p "${initramfs_root}"

  "${LZ4}" -dcfm "${prebuilts_root}/initramfs.img" | (cd "${initramfs_root}"; cpio -imd)

  for x in $(find "${initramfs_root}" -type f -name "*.ko"); do
    cp "$x" "${out_root}"
  done
}
