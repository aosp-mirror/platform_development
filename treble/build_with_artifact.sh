#!/bin/sh
#
# Copyright (C) 2023 The Android Open Source Project
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

ARG_SHORT=d:,t:,h
ARG_LONG=dist_dir:,target_product:,help
OPTS=$(getopt -n build_with_artifact --options $ARG_SHORT --longoptions $ARG_LONG -- "$@")

eval set -- "$OPTS"

function print_usage(){
  echo "usage: development/treble/build_with_artifact.sh --dist_dir <dist_dir> --target_product <target_product>"
  exit 2
}

while :
do
  case "$1" in
    -d | --dist_dir )
      DIST_DIR="$2"
      shift 2
      ;;
    -t | --target_product )
      TARGET_PRODUCT="$2"
      shift 2
      ;;
    -h | --help )
      print_usage
      ;;
    -- )
      shift;
      break
      ;;
    * )
      print_usage
      ;;
  esac
done

if [ -z DIST_DIR ] || [ -z TARGET_PRODUCT ] ; then
  print_usage
fi

BUILD_WITH_PARTIAL_ARTIFACT=true build/soong/soong_ui.bash --make-mode TARGET_PRODUCT=$TARGET_PRODUCT TARGET_BUILD_VARIANT=userdebug droid dist DIST_DIR=$DIST_DIR