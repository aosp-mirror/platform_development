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

set -e

KERNEL_INFO_FILES=(
  "System.map"
  "vmlinux"
  "vmlinux.symvers"
  "modules.builtin"
  "modules.builtin.modinfo"
)

function download_kernel_info_files {
  local bid=$1
  local kernel_target=$2
  local output_folder=$3

  cd "$output_folder"

  for f in "${KERNEL_INFO_FILES[@]}"; do
    /google/data/ro/projects/android/fetch_artifact \
      --bid "$bid" \
      --target "$kernel_target" \
      "$f" || true
  done

  cd -
}

function download_all_kernel_info_files {
  local kernel_target=$1
  local folder_pattern=$2

  for folder in $(find "${DIST_DIR}/kernel" -type d -regex ".*/${folder_pattern}"); do
    local prebuilt_info="${folder}/prebuilt-info.txt"
    local bid=$(cat "$prebuilt_info" | sed -rn 's/.*"kernel-build-id"\s*:\s*([0-9]+).*/\1/p')
    download_kernel_info_files "$bid" "$kernel_target" "$folder"
  done
}


if [[ -z "${DIST_DIR}" ]]; then
  echo "DIST_DIR must be defined." 1>&2
  exit 1
fi

ARCH=$1

download_all_kernel_info_files "kernel_${ARCH}" '[0-9]+.[0-9]+'
download_all_kernel_info_files "kernel_debug_${ARCH}" '[0-9]+.[0-9]+-debug'
