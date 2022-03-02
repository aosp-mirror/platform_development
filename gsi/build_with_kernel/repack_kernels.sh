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

source development/gsi/build_with_kernel/repack_kernels_common.sh

set -e

prepare_lz4

prepare_kernel_image \
  "artifacts/common-android13-5_10-kernel_aarch64" \
  "5.10" \
  "arm64"

prepare_kernel_image \
  "artifacts/common-android13-5_10-kernel_debug_aarch64" \
  "5.10" \
  "arm64" \
  "debug"

prepare_kernel_modules \
  "artifacts/common-android13-5_10-kernel_virt_aarch64" \
  "5.10" \
  "arm64"

prepare_kernel_image \
  "artifacts/common-android13-5_15-kernel_aarch64" \
  "5.15" \
  "arm64"

prepare_kernel_image \
  "artifacts/common-android13-5_15-kernel_debug_aarch64" \
  "5.15" \
  "arm64" \
  "debug"

prepare_kernel_modules \
  "artifacts/common-android13-5_15-kernel_virt_aarch64" \
  "5.15" \
  "arm64"
