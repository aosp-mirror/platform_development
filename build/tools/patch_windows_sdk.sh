#!/bin/bash
#
# Copyright (C) 2009 The Android Open Source Project
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

# This script takes a Linux SDK, cleans it and injects the necessary Windows
# binaries needed by the SDK. The script has 2 parts:
# - development/tools/build/path_windows_sdk.sh to process the
#   platform-dependent folders and files.
# - sdk/build/patch_windows_sdk.sh to process folder and files which
#   depend on the sdk.git repo. This file is invoked by the makefile
#   at development/build/tools/windows_sdk.mk.
#
# Input arguments:
# -q = Optional arg to make this silent. Must be given first.
# $1 = Temporary SDK directory, that is the Linux SDK being patched into
#      a Windows one.
# $2 = The out/host/windows directory, which contains the new Windows
#      binaries to use.
# $3 = An optional replacement for $TOPDIR (inherited from the Android
#      build system), which is the top directory where Android is located.

set -e # any error stops the build

# Verbose by default. Use -q to make more silent.
V=""
Q=""
if [[ "$1" == "-q" ]]; then
    Q="$1"
    shift
else
  echo "Win SDK: $0 $*"
  set -x # show bash commands; no need for V=-v
fi

TEMP_SDK_DIR=$1
WIN_OUT_DIR=$2
TOPDIR=${TOPDIR:-$3}

# The unix2dos is provided by the APT package "tofrodos". However
# as for ubuntu lucid, the package renamed the command to "todos".
UNIX2DOS=$(which unix2dos || true)
if [[ ! -x $UNIX2DOS ]]; then
  UNIX2DOS=$(which todos || true)
fi

PLATFORMS=( $TEMP_SDK_DIR/platforms/* )
if [[ ${#PLATFORMS[@]} != 1 ]]; then
    echo "Error: Too many platforms found in $TEMP_SDK_DIR"
    echo "Expected one. Instead, found: ${PLATFORMS[@]}"
    exit 1
fi

# Package USB Driver
if [[ -n "$USB_DRIVER_HOOK" ]]; then
    $USB_DRIVER_HOOK $V $TEMP_SDK_DIR $TOPDIR
fi


# Invoke atree to copy the files
# TODO: pass down OUT_HOST_EXECUTABLE to get the right bin/atree directory
${TOPDIR}out/host/linux-x86/bin/atree -f ${TOPDIR}development/build/sdk-windows-x86.atree \
      -I $WIN_OUT_DIR/host/windows-x86 \
      -I ${TOPDIR:-.} \
      -v "PLATFORM_NAME=android-$PLATFORM_VERSION" \
      -o $TEMP_SDK_DIR

# Fix EOL chars to make window users happy - fix all files at the top level
# as well as all batch files including those in platform-tools/
if [[ -x $UNIX2DOS ]]; then
  find $TEMP_SDK_DIR -maxdepth 1 -name "*.[ht]*" -type f -print0 | xargs -0 $UNIX2DOS
  find $TEMP_SDK_DIR -maxdepth 3 -name "*.bat"   -type f -print0 | xargs -0 $UNIX2DOS
fi

# Just to make it easier on the build servers, we want fastboot and adb
# (and its DLLs) next to the new SDK.
for i in fastboot.exe adb.exe AdbWinApi.dll AdbWinUsbApi.dll; do
    cp -f $V $WIN_OUT_DIR/host/windows-x86/bin/$i $TEMP_SDK_DIR/../$i
done

