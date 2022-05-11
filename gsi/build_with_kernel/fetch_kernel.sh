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

FETCH=/google/data/ro/projects/android/fetch_artifact
BCHAINING=`pwd`/out/prebuilt_cached/artifacts/

function local_fetch()
{
    local name=$1
    local target=$2
    local version=$(echo $name | sed -e 's/common-//g' -e 's/-kernel.*//g' -e 's/_/\./g')
    mkdir -p ${BCHAINING}/${name}
    cd ${BCHAINING}/${name}
    ${FETCH} --branch aosp_kernel-common-${version} --target ${target} --latest
    cd -
}

local_fetch common-android13-5_10-kernel_aarch64 kernel_aarch64
local_fetch common-android13-5_10-kernel_debug_aarch64 kernel_debug_aarch64
local_fetch common-android13-5_10-kernel_virt_aarch64 kernel_virt_aarch64

local_fetch common-android13-5_15-kernel_aarch64 kernel_aarch64
local_fetch common-android13-5_15-kernel_debug_aarch64 kernel_debug_aarch64
local_fetch common-android13-5_15-kernel_virt_aarch64 kernel_virt_aarch64
