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
    echo "usage: "$0" -t TARGET -v VARIANT [-d DIST_OUT] [-a ALTER_TARGET] [-c] [GOALS ...]"
    echo "  -t TARGET      : Primay target to build"
    echo "  -v VARIANT     : Build variant (ex. user, userdebug)"
    echo "  -d DIST_OUT    : Path for dist out"
    echo "  -a ALTER_TARGET: The secondary target that shares the build artifacts with the primary target"
    echo "  -c             : Installclean between each build"
}

while getopts ha:cd:t:v: opt; do
    case "${opt}" in
        h)
            usage
            ;;
        a)
            alter_target="${OPTARG}"
            ;;
        c)
            installclean=true
            ;;
        d)
            dist_dir="${OPTARG}"
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

if [[ -z ${target} ]]; then
    echo "-t must set for the primary target"
    exit 1
fi
if [[ -z ${variant} ]]; then
    echo "-v must set for build variant"
    exit 1
fi


goals="${@:OPTIND}"

base_command="build/soong/soong_ui.bash --make-mode"
if [[ ! -z ${dist_dir} ]]; then
    base_command="${base_command} DIST_DIR=${dist_dir} dist"
fi

# Build the target first.
echo "Initial build..."
${base_command} TARGET_PRODUCT=${target} TARGET_BUILD_VARIANT=${variant} ${goals}

if [[ ! -z ${alter_target} ]]; then
    # Building two targets with a single artifacts
    if [[ ! -z ${installclean} ]]; then
        echo "Installclean for the alternative target..."
        ${base_command} TARGET_PRODUCT=${alter_target} TARGET_BUILD_VARIANT=${variant} installclean
    fi
    echo "Build the alternative target..."
    ${base_command} TARGET_PRODUCT=${alter_target} TARGET_BUILD_VARIANT=${variant} ${goals}
    if [[ ! -z ${installclean} ]]; then
        echo "Installclean for the primary target..."
        ${base_command} TARGET_PRODUCT=${target} TARGET_BUILD_VARIANT=${variant} installclean
    fi
    echo "Build the primary target again..."
    ${base_command} TARGET_PRODUCT=${target} TARGET_BUILD_VARIANT=${variant} ${goals}
else
    # Build again just after installclean for reference
    if [[ ! -z ${installclean} ]]; then
        echo "Installclean..."
        ${base_command} TARGET_PRODUCT=${target} TARGET_BUILD_VARIANT=${variant} installclean
    fi
    echo "Build the target again..."
    ${base_command} TARGET_PRODUCT=${target} TARGET_BUILD_VARIANT=${variant} ${goals}
fi

