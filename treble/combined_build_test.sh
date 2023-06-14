#!/bin/bash
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

usage() {
    echo "usage: ${0} -t TARGET -v VARIANT [-d DIST_OUT] [-a ALTER_TARGET] [-c] [-o] [-r] [GOALS ...]"
    echo "  -t TARGET      : Primay target to build"
    echo "  -v VARIANT     : Build variant (ex. user, userdebug)"
    echo "  -d DIST_OUT    : Path for dist out"
    echo "  -a ALTER_TARGET: The secondary target that shares the build artifacts with the primary target"
    echo "  -c             : Run the target build again after installclean for reference"
    echo '  -o             : Write build time results to "build_time_results.txt" file in "${OUT_DIR}" or "${DIST_OUT}/logs" if -d defined'
    echo "  -r             : Dryrun to see the commands without actually building the targets"
}

while getopts ha:cd:ort:v: opt; do
    case "${opt}" in
        h)
            usage
            ;;
        a)
            alter_target="${OPTARG}"
            ;;
        c)
            installclean="true"
            ;;
        d)
            dist_dir="${OPTARG}"
            ;;
        o)
            result_out="build_time_results.txt"
            ;;
        r)
            dry_run="true"
            ;;
        t)
            target="${OPTARG}"
            ;;
        v)
            variant="${OPTARG}"
            ;;
        *)
            usage
            ;;
    esac
done

if [[ -z "${target}" ]]; then
    echo "-t must set for the primary target"
    usage
    exit 1
fi
if [[ -z "${variant}" ]]; then
    echo "-v must set for build variant"
    usage
    exit 1
fi

goals="${@:OPTIND}"

readonly ANDROID_TOP="$(cd $(dirname $0)/../..; pwd)"
cd "${ANDROID_TOP}"

out_dir="${OUT_DIR:-out}"
if [[ -n "${dist_dir}" ]]; then
    out_dir="${dist_dir}/logs"
fi

base_command="build/soong/soong_ui.bash --make-mode"
if [[ -n "${dist_dir}" ]]; then
    base_command="${base_command} DIST_DIR=${dist_dir} dist"
fi

run_command() {
    if [[ -z "${dry_run}" ]]; then
        $1
    else
        echo "$1"
    fi
}

write_output() {
    if [[ -z "${result_out}" || -n "${dry_run}" ]]; then
        echo "Output: $1"
    else
        echo "$1" >> "${out_dir}/${result_out}"
    fi
}

get_build_trace() {
    run_command "cp -f ${out_dir}/build.trace.gz ${out_dir}/${1}"
    if [[ -n "${result_out}" ]]; then
        write_output "$(python3 development/treble/read_build_trace_gz.py ${out_dir}/${1})"
    fi
}

if [[ -n "${result_out}" ]]; then
    run_command "rm -f ${out_dir}/${result_out}"
    write_output "target, soong, kati, ninja, total"
fi

# Build the target first.
echo "Initial build..."
run_command "${base_command} TARGET_PRODUCT=${target} TARGET_BUILD_VARIANT=${variant} ${goals}"

if [[ -n "${installclean}" ]]; then
    # Run the same build after installclean
    echo "Installclean..."
    run_command "${base_command} TARGET_PRODUCT=${target} TARGET_BUILD_VARIANT=${variant} installclean"
    echo "Build the same initial build..."
    run_command "${base_command} TARGET_PRODUCT=${target} TARGET_BUILD_VARIANT=${variant} ${goals}"
    get_build_trace "build_${target}_installclean.trace.gz"
fi

if [[ -n "${alter_target}" ]]; then
    # Building two targets with a single artifacts
    echo "Installclean for the alternative target..."
    run_command "${base_command} TARGET_PRODUCT=${alter_target} TARGET_BUILD_VARIANT=${variant} installclean"
    echo "Build the alternative target..."
    run_command "${base_command} TARGET_PRODUCT=${alter_target} TARGET_BUILD_VARIANT=${variant} ${goals}"
    get_build_trace "build_${alter_target}_ab.trace.gz"

    echo "Installclean for the primary target..."
    run_command "${base_command} TARGET_PRODUCT=${target} TARGET_BUILD_VARIANT=${variant} installclean"
    echo "Build the primary target again..."
    run_command "${base_command} TARGET_PRODUCT=${target} TARGET_BUILD_VARIANT=${variant} ${goals}"
    get_build_trace "build_${target}_aba.trace.gz"
fi
