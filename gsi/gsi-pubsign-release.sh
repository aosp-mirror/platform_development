#!/bin/bash
#
# Copyright (C) 2020 The Android Open Source Project
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

set -ex

function usage {
  cat <<EOF
Usage: $0 src_product target_product src_dir [dist_dir]

Create artifacts for |target_product| from the artifacts of |src_product|.

src_product
        Product name to copy artifacts from. e.g. aosp_arm64

target_product
        Product name to copy artifacts to. e.g. aosp_arm64_pubsign

src_dir
        Directory containing source artifacts.

dist_dir
        Optional. If given then generate any artifact under dist_dir.
        Otherwise generate under environment variable DIST_DIR.
EOF
}

if [[ $# -lt 3 ]]; then
  usage
  exit 1
fi

SRC_PRODUCT="$1"
TARGET_PRODUCT="$2"
SRC_DIR="$3"

if [[ $# -ge 4 ]]; then
  DIST_DIR="$4"
fi

readonly SRC_PRODUCT
readonly TARGET_PRODUCT
readonly SRC_DIR
readonly DIST_DIR

if [[ -z "${DIST_DIR}" ]]; then
  echo >&2 '$DIST_DIR is not specified'
  exit 1
fi

# Create output directory if not already present
mkdir -p "${DIST_DIR}" || true

if [[ ! -d "${DIST_DIR}" ]]; then
  echo >&2 'Cannot create $DIST_DIR or $DIST_DIR is non-existence'
  exit 1
fi

# Show the artifacts to be copied in the log
echo "Artifacts to copy:"
find "${SRC_DIR}" || true
echo

# Don't copy logs/ and files whose name starts with $SRC_PRODUCT
rsync --verbose --archive --copy-links --exclude='logs' \
  --exclude='*.zip' "${SRC_DIR}/" "${DIST_DIR}"

# Rename ${SRC_PRODUCT}-xxx.zip to ${TARGET_PRODUCT}-xxx.zip
ZIP_PATHNAMES="$(find -L "${SRC_DIR}" -type f -name '*.zip')"

echo "ZIP files to be copied and renamed:"
echo "${ZIP_PATHNAMES}"
echo

for SRC_PATHNAME in ${ZIP_PATHNAMES} ; do
  SRC_FILENAME="$(basename ${SRC_PATHNAME})"
  TARGET_FILENAME="${SRC_FILENAME/${SRC_PRODUCT}/${TARGET_PRODUCT}}"
  cp --verbose --archive --dereference "${SRC_PATHNAME}" "${DIST_DIR}/${TARGET_FILENAME}"
done
