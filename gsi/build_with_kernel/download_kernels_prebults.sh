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

if [ -z "${ANDROID_BUILD_TOP}" ]; then
    WD="$(pwd)"
else
    WD="${ANDROID_BUILD_TOP}"
fi
if [ -z "${OUT_DIR}" ]; then
    OUT_DIR="${WD}/out"
fi

function download_project() {
  local branch=$1
  local target=$2
  shift 2
  local file_list=("$@")

  local bid_file="${WD}/prebuilts/build-artifacts3/aosp_kernel-common-${branch}/${target}/bid"
  local bid=$(<$bid_file)
  local download_url="https://ci.android.com/builds/submitted/${bid}/${target}/latest/raw"
  local dist_base="${OUT_DIR}/prebuilt_cached/artifacts/common-${branch//./_}-${target}"

  mkdir -p "$dist_base"
  for f in "${file_list[@]}"; do
    wget "${download_url}/${f}" -O "${dist_base}/${f}"
  done
}

function download-android12-5.10-kernel_aarch64() {
  local file_list=(
    "BUILD_INFO"
    "Image"
    "Image.lz4"
    "System.map"
    "vmlinux"
    "vmlinux.symvers"
    "modules.builtin"
    "modules.builtin.modinfo"
  )

  download_project "android12-5.10" "kernel_aarch64" "${file_list[@]}"
}

function download-android12-5.10-kernel_debug_aarch64() {
  local file_list=(
    "BUILD_INFO"
    "Image"
    "Image.lz4"
    "System.map"
    "vmlinux"
    "vmlinux.symvers"
    "modules.builtin"
    "modules.builtin.modinfo"
  )

  download_project "android12-5.10" "kernel_debug_aarch64" "${file_list[@]}"
}

function download-android12-5.10-kernel_x86_64() {
  local file_list=(
    "BUILD_INFO"
    "bzImage"
    "System.map"
    "vmlinux"
    "vmlinux.symvers"
    "modules.builtin"
    "modules.builtin.modinfo"
  )

  download_project "android12-5.10" "kernel_x86_64" "${file_list[@]}"
}

function download-android12-5.10-kernel_debug_x86_64() {
  local file_list=(
    "BUILD_INFO"
    "bzImage"
    "System.map"
    "vmlinux"
    "vmlinux.symvers"
    "modules.builtin"
    "modules.builtin.modinfo"
  )

  download_project "android12-5.10" "kernel_debug_x86_64" "${file_list[@]}"
}

function download-android12-5.10-kernel_virt_aarch64() {
  local file_list=(
    "BUILD_INFO"
    "initramfs.img"
  )

  download_project "android12-5.10" "kernel_virt_aarch64" "${file_list[@]}"
}

function download-android12-5.10-kernel_virt_x86_64() {
  local file_list=(
    "BUILD_INFO"
    "initramfs.img"
  )

  download_project "android12-5.10" "kernel_virt_x86_64" "${file_list[@]}"
}

download-android12-5.10-kernel_aarch64
download-android12-5.10-kernel_debug_aarch64
download-android12-5.10-kernel_x86_64
download-android12-5.10-kernel_debug_x86_64
download-android12-5.10-kernel_virt_aarch64
download-android12-5.10-kernel_virt_x86_64
