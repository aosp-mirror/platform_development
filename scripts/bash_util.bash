# Copyright (C) 2018 The Android Open Source Project
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

script_name="${0##*/}"
script_dir="${0%/*}"

# Exit with 1 with printing a given message on stderr.
function die() {
    echo "$script_name: ERROR: $@" 1>&2
    exit 1
}

# Print a given message on stderr.
function warn() {
    echo "$script_name: WARN: $@" 1>&2
}

# Print a given message on stderr.
function info() {
    echo "$script_name: $@" 1>&2
}

# Wrapper around "adb".
function do_adb() {
    adb $ADB_OPTIONS "$@"
}

# Return the timestamp of the most recent log line, which can be later used with logcat -t or -T.
function get_last_logcat_timestamp() {
    # Output will be like this. Extract the timestamp.
    #--------- beginning of main
    #06-14 00:04:43.909  3993  3993 E QtiImsExtUtils: isCarrierConfigEnabled bundle is null

    do_adb logcat -t 1 | awk '(match($0, "^[0-9]")){print $1 " " $2}'
}

# If $1 is a number, just print it. Otherwise use adb shell pidof to try to resolve it into a pid.
function resolve_pid() {
    local name="$1"

    if [[ -z "$name" ]] ;then
        return 1
    fi

    if [[ "$name" =~ ^[0-9]+$ ]] ; then
        echo "$name"
        return 0
    fi
    local pid="$(do_adb shell pidof "$name")"
    if [[ -z "$pid" ]] ; then
        die "unknown process: $name"
    fi
    echo "$pid"
}

# Find available local port. Optionally take the starting port from $1.
function find_open_port() {
    local port="${1:-10000}" # Take the start port from $1 with 10000 as the default.

    while true; do
        if netstat -an | grep -qw "$port"; then
            port=$(( $port + 1 ))
            continue
        fi
        break # Found
    done

    echo "$port"
}

# Create a temp file name with a timestamp.
function make_temp_file() {
    local suffix="$1"
    local dir="${TMPDIR:-${TEMP:-/tmp}}"

    while true; do
        local file="$dir/temp-$(date '+%Y%m%d-%H%M%S')-$$$suffix"
        if ! [[ -e "$file" ]] ; then
            touch "$file"   # Note it's a bit racy..
            echo "$file"
            return 0
        fi
        sleep 0.5 # Ugh.
    done
}