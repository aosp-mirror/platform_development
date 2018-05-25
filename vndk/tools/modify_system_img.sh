#!/bin/bash -ex
# Copyright (C) 2018 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

usage () {
    echo "Modifies the system image in a target_files.zip package so that it is"
    echo "    compatible with vendor images of older Android versions."
    echo "    This script is intended to be run on the Android build servers"
    echo "    for inter branch mixed build targets."
    echo
    echo "Usage: $0 [-v <vendor_version>]"
    echo "    <target_files_path> [<new_security_patch_level>]"
    echo
    echo "vendor_version is the version of the vendor image"
    echo "    e.g. 8.1.0 for Android version O-MR1"
    echo "new_security_patch_level is the value to replace the SPL in the"
    echo "    original system.img"
    echo "target_files_path is the path to the *-target_files-*.zip file"
}

# Print error message and exit.
# Usage: exit_badparam message
#
# message is a string to be displayed before exit.
exit_badparam () {
    echo "ERROR: $1" >&2
    usage
    exit 1
}

cleanup_and_exit () {
    readonly result="$?"
    rm -rf "$TEMP_DIR"
    exit "$result"
}

trap cleanup_and_exit EXIT

while getopts :v: opt; do
    case "$opt" in
        v)
            readonly VENDOR_VERSION="$OPTARG"
            ;;
        \?)
            exit_badparam "Invalid options: -"$OPTARG""
            ;;
        :)
            exit_badparam "Option -"$OPTARG" requires an argument."
            ;;
    esac
done
shift "$((OPTIND-1))"

if [[ $# -lt 1 || $# -gt 2 ]]; then
    exit_badparam "Unexpected number of arguments"
fi

readonly SYSTEM_TARGET_FILES="$1"
readonly NEW_SPL="$2"

if [[ ! -f "$SYSTEM_TARGET_FILES" ]]; then
    exit_badparam "Could not find system target files package, "$SYSTEM_TARGET_FILES""
fi

# SPL must have YYYY-MM-DD format
if [[ $# -eq 2 ]] && [[ ! "$NEW_SPL" =~ ^[0-9]{4}-(0[0-9]|1[012])-([012][0-9]|3[01])$ ]]; then
    exit_badparam "<new_security_patch_level> must have YYYY-MM-DD format"
fi

if [[ -z "${ANDROID_BUILD_TOP+x}" ]]; then
    build_top=""
else
    build_top="$ANDROID_BUILD_TOP"/
fi

readonly add_img_to_target_files="$build_top"build/make/tools/releasetools/add_img_to_target_files.py

# Check required script
if [[ ! -f "$add_img_to_target_files" ]]; then
    echo "Error: Cannot find script,", "$add_img_to_target_files"
    echo "Please run lunch or run from root of source tree."
    exit 1
fi

readonly TEMP_DIR="$(mktemp -d /tmp/"$(basename $0)"_XXXXXXXX)"
readonly SPL_PROPERTY_NAME="ro.build.version.security_patch"
readonly RELEASE_VERSION_PROPERTY_NAME="ro.build.version.release"
readonly VNDK_VERSION_PROPERTY="ro.vndk.version"
readonly VNDK_VERSION_PROPERTY_OMR1="$VNDK_VERSION_PROPERTY"=27

readonly BUILD_PROP_PATH="SYSTEM/build.prop"
readonly PROP_DEFAULT_PATH="SYSTEM/etc/prop.default"

# Unzip build.prop and prop.default from target_files.zip
unzip "$SYSTEM_TARGET_FILES" "$BUILD_PROP_PATH" "$PROP_DEFAULT_PATH" -d "$TEMP_DIR"

readonly BUILD_PROP_FILE="$TEMP_DIR"/"$BUILD_PROP_PATH"
readonly PROP_DEFAULT_FILE="$TEMP_DIR"/"$PROP_DEFAULT_PATH"

if [[ -f "$BUILD_PROP_FILE" ]]; then
    readonly CURRENT_SPL=$(sed -n -r "s/^"$SPL_PROPERTY_NAME"=(.*)$/\1/p" "$BUILD_PROP_FILE")
    readonly CURRENT_VERSION=$(sed -n -r "s/^"$RELEASE_VERSION_PROPERTY_NAME"=(.*)$/\1/p" "$BUILD_PROP_FILE")
    echo "Reading build.prop..."
    echo "  Current security patch level: "$CURRENT_SPL""
    echo "  Current release version: "$CURRENT_VERSION""

    # Update SPL to <new_security_patch_level>
    if [[ "$NEW_SPL" != "" ]]; then
        if [[ "$CURRENT_SPL" == "" ]]; then
            echo "ERROR: Cannot find "$SPL_PROPERTY_NAME" in "$BUILD_PROP_PATH""
            exit 1
        else
            # Replace the property inplace
            sed -i "s/^"$SPL_PROPERTY_NAME"=.*$/"$SPL_PROPERTY_NAME"="$NEW_SPL"/" "$BUILD_PROP_FILE"
            echo "Replacing..."
            echo "  New security patch level: "$NEW_SPL""
        fi
    fi

    # Update release version to <vendor_version>
    if [[ "$VENDOR_VERSION" != "" ]]; then
        if [[ "$CURRENT_VERSION" == "" ]]; then
            echo "ERROR: Cannot find "$RELEASE_VERSION_PROPERTY_NAME" in "$BUILD_PROP_PATH""
            exit 1
        else
            # Replace the property inplace
            sed -i "s/^"$RELEASE_VERSION_PROPERTY_NAME"=.*$/"$RELEASE_VERSION_PROPERTY_NAME"="$VENDOR_VERSION"/" "$BUILD_PROP_FILE"
            echo "Replacing..."
            echo "  New release version for vendor.img: "$VENDOR_VERSION""
        fi

        if [[ "$VENDOR_VERSION" == "8.1.0" ]]; then
            # add ro.vndk.version for O-MR1
            if [[ -f "$PROP_DEFAULT_FILE" ]]; then
                readonly CURRENT_VNDK_VERSION=$(sed -n -r "s/^"$VNDK_VERSION_PROPERTY"=(.*)$/\1/p" "$PROP_DEFAULT_FILE")
                if [[ "$CURRENT_VNDK_VERSION" != "" ]]; then
                    echo "WARNING: "$VNDK_VERSION_PROPERTY" is already set to "$CURRENT_VNDK_VERSION" in "$PROP_DEFAULT_PATH""
                    echo "DID NOT overwrite "$VNDK_VERSION_PROPERTY""
                else
                    echo "Adding \""$VNDK_VERSION_PROPERTY_OMR1"\" to "$PROP_DEFAULT_PATH" for O-MR1 vendor image."
                    sed -i -e "\$a\#\n\# FOR O-MR1 DEVICES\n\#\n"$VNDK_VERSION_PROPERTY_OMR1"" "$PROP_DEFAULT_FILE"
                fi
            else
                echo "ERROR: Cannot find "$PROP_DEFAULT_PATH" in "$SYSTEM_TARGET_FILES""
            fi
        fi
    fi
else
    echo "ERROR: Cannot find "$BUILD_PROP_PATH" in "$SYSTEM_TARGET_FILES""
    exit 1
fi

# Re-zip build.prop and prop.default
(
    cd "$TEMP_DIR"
    zip -ur "$SYSTEM_TARGET_FILES" ./*
)

# Rebuild system.img
zip -d "$SYSTEM_TARGET_FILES" IMAGES/system\*
"$add_img_to_target_files" -a "$SYSTEM_TARGET_FILES"

echo "Done."
