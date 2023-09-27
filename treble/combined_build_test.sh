#!/bin/bash -e
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
    echo "usage: ${0} -t TARGET -v VARIANT [-d DIST_OUT] [[-a ALTER_TARGET] ...] [-c] [-o] [-r] [GOALS ...]"
    echo "  -t TARGET      : Primay target to build"
    echo "  -v VARIANT     : Build variant (ex. user, userdebug)"
    echo "  -d DIST_OUT    : Path for dist out"
    echo "  -a ALTER_TARGET: Alternative targets that share the build artifacts with the primary target"
    echo "  -c             : Run the target build again after installclean for reference"
    echo '  -o             : Write build time ("build_time_results.txt") and disk usage results (disk_size_results.txt") to "${OUT_DIR}" or "${DIST_OUT}/logs" if -d defined'
    echo "  -r             : Dryrun to see the commands without actually building the targets"
}

while getopts ha:cd:ort:v: opt; do
    case "${opt}" in
        h)
            usage
            ;;
        a)
            alter_targets+=("${OPTARG}")
            ;;
        c)
            installclean="true"
            ;;
        d)
            dist_dir="${OPTARG}"
            ;;
        o)
            result_out="build_time_results.txt"
            result_out_size="disk_size_results.txt"
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
if [ "${#alter_targets[@]}" -eq 1 ]; then
    # test for a-b-a builds
    alter_targets+=("${target}")
fi

goals="${@:OPTIND}"

readonly ANDROID_TOP="$(cd $(dirname $0)/../..; pwd)"
cd "${ANDROID_TOP}"

out_dir="${OUT_DIR:-out}"
if [[ -n "${dist_dir}" ]]; then
    out_dir="${dist_dir}/logs"
fi

base_command="build/soong/soong_ui.bash --make-mode TARGET_RELEASE=trunk_staging"
if [[ -n "${dist_dir}" ]]; then
    base_command="${base_command} DIST_DIR=${dist_dir} dist"
fi

run_command() {
    echo "**Running: ${1}"
    if [[ -z "${dry_run}" ]]; then
        eval "${1}"
    fi
}

read_df() {
    # read the available disk size
    df . | awk '{print $4}' | sed -n '2p'
}

write_output() {
    if [[ -z "$2" || -n "${dry_run}" ]]; then
        echo "Output: $1"
    else
        echo "$1" >> "${out_dir}/$2"
    fi
}

get_build_trace() {
    run_command "cp -f ${out_dir}/build.trace.gz ${out_dir}/${1}"
    if [[ -n "${result_out}" ]]; then
        write_output "$(python3 development/treble/read_build_trace_gz.py ${out_dir}/${1})" "${result_out}"
    fi
}

if [[ -n "${result_out}" ]]; then
    run_command "rm -f ${out_dir}/${result_out}"
    write_output "target, soong, kati, ninja, total" "${result_out}"
fi

if [[ -n "${result_out_size}" ]]; then
    run_command "rm -f ${out_dir}/${result_out_size}"
    write_output "target, size, size_after_clean" "${result_out_size}"
fi

# Build the target first.
disk_space_source=$(read_df)
echo; echo "Initial build..."
run_command "${base_command} TARGET_PRODUCT=${target} TARGET_BUILD_VARIANT=${variant} ${goals}"
size_primary=$((${disk_space_source}-$(read_df)))

if [[ -n "${installclean}" ]]; then
    # Run the same build after installclean
    echo; echo "Installclean for incremental build..."
    run_command "${base_command} TARGET_PRODUCT=${target} TARGET_BUILD_VARIANT=${variant} installclean"
    size_primary_clean=$((${disk_space_source}-$(read_df)))
    write_output "${target}, ${size_primary}, ${size_primary_clean}" "${result_out_size}"

    echo "Build the same initial build..."
    run_command "${base_command} TARGET_PRODUCT=${target} TARGET_BUILD_VARIANT=${variant} NINJA_ARGS=\"-d explain\" ${goals}"
    get_build_trace "build_${target}_installclean.trace.gz"
    echo "Installclean to prepare for the next build..."
    run_command "${base_command} TARGET_PRODUCT=${target} TARGET_BUILD_VARIANT=${variant} installclean"
fi

count=0
# Building the next targets in sequence
for alter_target in "${alter_targets[@]}"; do
    count=$((${count}+1))
    echo; echo "Build ${alter_target}...(${count})"
    run_command "${base_command} TARGET_PRODUCT=${alter_target} TARGET_BUILD_VARIANT=${variant} NINJA_ARGS=\"-d explain\" ${goals}"
    size_alter=$((${disk_space_source}-$(read_df)))
    get_build_trace "build_${alter_target}_ab${count}.trace.gz"

    echo "Installclean for ${alter_target}..."
    run_command "${base_command} TARGET_PRODUCT=${alter_target} TARGET_BUILD_VARIANT=${variant} installclean"
    size_alter_clean=$((${disk_space_source}-$(read_df)))
    write_output "${alter_target}, ${size_alter}, ${size_alter_clean}" "${result_out_size}"

    if [[ -n "${dist_dir}" ]]; then
        # Remove target-specific dist artifacts
        run_command "rm -f ${dist_dir}/${alter_target}*"
    fi
done

if [[ -n "${dist_dir}" ]]; then
    # Remove some dist artifacts to save disk space
    run_command "rm -f ${dist_dir}/${target}*"
    run_command "rm -f ${dist_dir}/device-tests*"
    run_command "rm -f ${dist_dir}/cvd-host_package.tar.gz"
    run_command "rm -f ${dist_dir}/dexpreopt_tools.zip"
    run_command "rm -f ${dist_dir}/otatools.zip"
fi
