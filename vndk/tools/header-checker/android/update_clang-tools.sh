#!/bin/bash -eu

# Copyright (C) 2019 The Android Open Source Project
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

if [ -z "${ANDROID_BUILD_TOP:-}" ]; then
  echo 1>&2 "ANDROID_BUILD_TOP must be defined"
  exit 1
fi

BUILD_ID="${1:-}"
if [ -z "${BUILD_ID}" ]; then
  echo 1>&2 "Usage: $0 <buildid>"
  exit 1
fi

FETCH_ARTIFACT="/google/data/ro/projects/android/fetch_artifact"
CLANG_TOOLS_DIR="${ANDROID_BUILD_TOP}/prebuilts/clang-tools"


update_manifest () {
  cd "${CLANG_TOOLS_DIR}"
  "${FETCH_ARTIFACT}" --bid "${BUILD_ID}" --target "linux" \
    "manifest_${BUILD_ID}.xml"
  mv "manifest_${BUILD_ID}.xml" "manifest.xml"
  rm .fetch*
}


update_prebuilts () {
  local name="$1"
  local target="$2"

  cd "${CLANG_TOOLS_DIR}"

  # Remove the old directory and create an empty one
  if [ -d "${name}" ]; then
    git rm -rf "${name}"
  fi
  mkdir -p "${name}"

  cd "${name}"

  # Download and extract prebuilts from the build server
  "${FETCH_ARTIFACT}" --bid "${BUILD_ID}" --target "${target}" \
    "build-prebuilts.zip"
  unzip -o "build-prebuilts.zip"
  rm "build-prebuilts.zip" .fetch*

  find . | xargs touch
}


commit () {
  cd "${CLANG_TOOLS_DIR}"
  echo "Update clang-tools to ab/${BUILD_ID}" > "/tmp/clang-tools-update.msg"
  git add manifest.xml linux-x86 darwin-x86
  git commit -a -t "/tmp/clang-tools-update.msg"
}


cd "${CLANG_TOOLS_DIR}"
repo start "update_${BUILD_ID}" .

update_manifest
update_prebuilts "linux-x86" "linux"
update_prebuilts "darwin-x86" "darwin_mac"
commit
