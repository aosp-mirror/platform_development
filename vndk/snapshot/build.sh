#!/bin/bash -e
#
# Copyright (C) 2017 The Android Open Source Project
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
#

function die {
  echo "$0: $*"
  exit 1
}

function usage {
  cat <<EOF
Usage: $0 [OPTION]
Build VNDK snapshots for all arches (arm64, arm, x86_64, x86).

  -a, --build-artifacts   include exported header files and flags
  -h, --help              display this help and exit

EOF
  exit
}

additional_option=
arches="arm64 arm x86_64 x86"

while [[ $# -gt 0 ]]; do
  case "$1" in
    (-a|--build-artifacts) additional_option="VNDK_SNAPSHOT_BUILD_ARTIFACTS=true";;
    (-h|--help) usage;;
    (*) die "Unknown option: '$1'
Try '$0 --help' for more information.";;
  esac
  shift
done

export TARGET_BUILD_VARIANT=user
export BOARD_VNDK_VERSION=current

for arch in $arches; do
  echo "-----Generating VNDK snapshot for $arch"
  build/soong/soong_ui.bash --make-mode vndk dist TARGET_PRODUCT=aosp_$arch $additional_option
done
