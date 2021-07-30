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

source development/gsi/build_with_kernel/bld-gki.sh

if [[ -z "${DIST_DIR}" ]]; then
  echo "DIST_DIR must be defined." 1>&2
  exit 1
fi

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

  if [[ "$arch" == "x86_64" ]]; then
    mkdir -p "${out_root}"
    cp "${prebuilts_root}/bzImage" \
      "${out_root}/kernel-${kernel_version}${postfix}"
  else
    # Pack all compress format for kernel images
    repack \
      "${prebuilts_root}/Image" \
      "${out_root}" \
      "${kernel_version}" \
      "${postfix}"
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

  # Copy all other helper files
  cp "${prebuilts_root}/System.map" "${dist_root}"
  cp "${prebuilts_root}/vmlinux" "${dist_root}"
  cp "${prebuilts_root}/vmlinux.symvers" "${dist_root}"
  cp "${prebuilts_root}/modules.builtin" "${dist_root}"
  cp "${prebuilts_root}/modules.builtin.modinfo" "${dist_root}"
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
