#!/bin/bash
#
# Copyright (C) 2012 The Android Open Source Project
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
# Generates a module index file by searching through android source
# tree for make files.  The intellij-gen.sh script automatically calls
# this script the first time or if you delete the generated indexed
# file.  The only time you need to run this manually is if modules are
# added or deleted.
#
# To use, run the following command from either your repo root or
# development/tools/idegen:
#   index-gen.sh
#
# Only tested on linux.  Should work for macs but have not tried.
#
set -e

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
#root_dir=`readlink -f -n $script_dir/../../..`
root_dir=$PWD
if [ ! -e $root_dir/.repo ]; then
  root_dir=$PWD/../../..
  if [ ! -e $root_dir/.repo ]; then
    echo "Repo root not found. Run this script from your repo root or the idegen directory."
    exit 1
  fi
fi
tmp_file=tmp.txt
dest_file=module-index.txt

echo "Generating index file $dest_file..."
start=$(($(date +%s%N) / 1000000))
find $root_dir -name '*.mk' \( ! -path "$root_dir/build*" -prune \) \
  \( -exec grep -H '^LOCAL_PACKAGE_NAME ' {} \; \
  -false -o -exec grep -H '^LOCAL_MODULE ' {} \; \) \
  > $tmp_file
sed -e 's/LOCAL_PACKAGE_NAME *:= *//g' -e 's/LOCAL_MODULE *:= *//g' -e 's/\^M*$//g' < $tmp_file > $dest_file

mv $dest_file $tmp_file
# Exclude specific directories from index here.
# TODO: make excludes more generic and configurable
grep -v "^$root_dir/vendor/google" $tmp_file > $dest_file

rm $tmp_file
end=$(($(date +%s%N) / 1000000))
elapse=$(($end - $start))
echo "Took ${elapse}ms"
