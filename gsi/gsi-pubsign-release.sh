#!/bin/bash

set -e

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
if [[ ! -d "${DIST_DIR}" ]]; then
  mkdir -p "${DIST_DIR}"
fi

# Don't copy logs/ and files whose name starts with $SRC_PRODUCT
rsync --verbose --archive --copy-links --exclude='logs' \
  --exclude="${SRC_PRODUCT}-*" "${SRC_DIR}" "${DIST_DIR}"

# Rename ${SRC_PRODUCT}-xxx.yyy to ${TARGET_PRODUCT}-xxx.yyy
for src_path in $(find "${SRC_DIR}" -type f -name "${SRC_PRODUCT}-*") ; do
  src_file="$(basename ${src_path})"
  target_file="${src_file/${SRC_PRODUCT}/${TARGET_PRODUCT}}"
  cp -v "${src_path}" "${DIST_DIR}/${target_file}"
done
