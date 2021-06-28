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

# Path for kernel 5.10
PREBUILTS_ROOT="out/prebuilt_cached/artifacts/common-android12-5_10-kernel_aarch64"
DIST_ROOT="${DIST_DIR}/kernel/5.10"

# Pack all compress format for kernel images
repack \
  "${PREBUILTS_ROOT}/Image" \
  "${OUT_DIR}/target/kernel/5.10/arm64" 5.10

# Prepare the dist folder
mkdir -p "${DIST_ROOT}"

# Prepare prebuilt-info.txt
BID=$(cat "${PREBUILTS_ROOT}/BUILD_INFO" | sed -n 's/^.*"bid":\s*"\(.*\)".*$/\1/p')
cat > "${DIST_ROOT}/prebuilt-info.txt" <<EOF
{
    "kernel-build-id": ${BID}
}
EOF

# Copy all other helper files
cp "${PREBUILTS_ROOT}/System.map" "${DIST_ROOT}"
cp "${PREBUILTS_ROOT}/vmlinux.symvers" "${DIST_ROOT}"
cp "${PREBUILTS_ROOT}/modules.builtin" "${DIST_ROOT}"
cp "${PREBUILTS_ROOT}/modules.builtin.modinfo" "${DIST_ROOT}"
