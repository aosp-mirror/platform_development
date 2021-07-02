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

set -e

if [[ -z "${DIST_DIR}" ]]; then
  echo "DIST_DIR must be defined." 1>&2
  exit 1
fi

function prepare_kernel_image()
{
  local prebuilt_path=$1
  local kernel_version=$2
  local arch=$3
  local is_debug=$4

  local prebuilts_root="out/prebuilt_cached/${prebuilt_path}"
  local dist_root="${DIST_DIR}/kernel/${kernel_version}"
  local postfix=""
  if [[ "$is_debug" == true ]]; then
     dist_root="${dist_root}-debug"
     postfix="-allsyms"
  fi

  # Pack all compress format for kernel images
  repack \
    "${prebuilts_root}/Image" \
    "${OUT_DIR}/target/kernel/${kernel_version}/${arch}" \
    "${kernel_version}" \
    "${postfix}"

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

prepare_kernel_image \
  "artifacts/common-android12-5_10-kernel_aarch64" \
  "5.10" \
  "arm64" \
  false

prepare_kernel_image \
  "artifacts/common-android12-5_10-kernel_debug_aarch64" \
  "5.10" \
  "arm64" \
  true
