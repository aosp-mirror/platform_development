#!/bin/bash -e
#
# Copyright (C) 2022 The Android Open Source Project
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

function usage {
  cat <<EOF
Usage: $0 <Android_bp_path> <hwasan_snapshots_dir>|restore

 Read <hwasan_snapshots_dir> to update the Android.bp file of <Android_bp_path>
 to use the hwasan prebuilt static libraries instead of the cfi libraries.
 <hwasan_snapshots_dir> must be the relative path to the <Android_bp_path>.
 If 'restore' is set, it will revert the modification.
EOF
  exit 1
}

if [[ "$#" != 2 ]]; then
  usage
fi

bp_path="$1"
bp_org_path="$bp_path".original

if [[ "$2" != "restore" ]]; then
  if [[ -f "$bp_org_path" ]]; then
    echo Already updated. Try again after restoring the bp file with the \"restore\" option.
    exit 1
  fi
  hwasan_dir="${2%/}"
  modules=`ls $(dirname "$bp_path")/"$hwasan_dir"/*.a | xargs -n 1 basename -s .a`

  cp $bp_path $bp_org_path
  for module in $modules; do
    echo Modifying $module...
    bpmodify -w -m $module -move-property -property arch.arm64.cfi -new-location hwasan $bp_path
    bpmodify -w -m $module -property arch.arm64.hwasan.src -str $hwasan_dir/$module.a $bp_path
  done
else
  if [[ ! -f "$bp_org_path" ]]; then
    echo Nothing to restore. Try without the \"restore\" option.
    exit 1
  fi
  mv -f $bp_org_path $bp_path
  echo "$bp_path" restored
fi

exit
