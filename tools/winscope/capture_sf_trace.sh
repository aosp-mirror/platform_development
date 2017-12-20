#!/bin/bash
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

WINSCOPE_URL='http://go/winscope/#sftrace'

set -e

outfile=layerstrace.pb
help=

for arg in "$@"; do
  case $arg in
    -h|--help) help=1;;
    --);;
    -*) echo "Unknown option: $arg"; help=1;;
    *) outfile="$arg";;
  esac
done

if [ "$help" != "" ]; then
  echo "usage: $0 [-h | --help] [OUTFILE]"
  echo
  echo "Traces SurfaceFlinger and writes the output to OUTFILE (default ./layerstrace.pb)."
  echo "To view the traces, use $WINSCOPE_URL."
  echo
  echo "WARNING: This calls adb root and deactivates SELinux."
  exit 1
fi

function log_error() {
  echo "FAILED"
}
trap log_error ERR

outfile_abs="$(cd "$(dirname "$outfile")"; pwd)/$(basename "$outfile")"

function start_tracing() {
  echo -n "Starting SurfaceFlinger trace..."
  adb shell su root service call SurfaceFlinger 1025 i32 1 >/dev/null
  echo "DONE"
  trap stop_tracing EXIT
}
function stop_tracing() {
  echo -n "Stopping SurfaceFlinger trace..."
  adb shell su root service call SurfaceFlinger 1025 i32 0 >/dev/null
  echo "DONE"
  trap - EXIT
}

which adb >/dev/null 2>/dev/null || { echo "ERROR: ADB not found."; exit 1; }
adb get-state 2>/dev/null | grep -q device || { echo "ERROR: No device connected or device is unauthorized."; exit 1; }

echo -n "Deactivating SELinux..."
adb shell su root setenforce 0 2>/dev/null >/dev/null
echo "DONE"

start_tracing
read -p "Press ENTER to stop tracing" -s x
echo
stop_tracing
adb exec-out su root cat /data/misc/trace/layerstrace.pb >"$outfile"

echo
echo "To view the trace, go to $WINSCOPE_URL, click 'OPEN SF TRACE' and open"
echo "${outfile_abs}"
