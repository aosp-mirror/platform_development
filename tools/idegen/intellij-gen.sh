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
# To use, run the following command from either your repo root or
# development/tools/idegen:
#   intellij-gen.sh <module name>
#
# where module name is the LOCAL_PACKAGE_NAME in Android.mk for the project.
#
# For example, to generate a project for Contacts, use:
#   intellij-gen.sh Contacts
#
# The project directory (.idea) will be put in the root directory of
# the module.  Sharable iml files will be put into each respective
# module directory.
#
# Only tested on linux.  Should work for macs but have not tried.
#
set -e

progname=`basename $0`
if [ $# -lt 2 ]
then
    echo "Usage: $progname project_dir module_dir <module_dir>..."
    exit 1
fi
project_dir=${PWD}/$1
shift
module_dirs=$@
echo $module_dirs
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
root_dir=$PWD
if [ ! -e $root_dir/.repo ]; then
  root_dir=$PWD/../../..
  if [ ! -e $root_dir/.repo ]; then
    echo "Repo root not found. Run this script from your repo root or the idegen directory."
    exit 1
  fi
fi
index_file=$root_dir/module-index.txt
idegenjar=$script_dir/idegen.jar
if [ ! -e $idegenjar ]; then
  # See if the jar is in the build directory.
  platform="linux"
  if [ "Darwin" = "$(uname)" ]; then
    platform="darwin"
  fi
  idegenjar="$root_dir/out/host/$platform-x86/framework/idegen.jar"
fi

if [ ! -e "$index_file" ]; then
  echo "Module index file missing; generating this is only done the first time."
  echo "If any dependencies change, you should generate a new index file by running index-gen.sh."
  $script_dir/index-gen.sh
fi

echo "Checking for $idegenjar"
if [ -e "$idegenjar" ]; then
  echo "Generating project files for $module_dirs"
  cmd="java -cp $idegenjar com.android.idegen.IntellijProject $index_file $project_dir $module_dirs"
  echo $cmd
  $cmd
else
  echo "Couldn't find idegen.jar. Please run make first."
fi
