#!/bin/bash
#
# Copyright (C) 2020 The Android Open Source Project
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

# This program fixes prebuilt ELF check errors by updating the "shared_libs"
# fields in Android.bp.
#
# Example:
# $ source build/envsetup.sh
# $ m fix_android_bp_prebuilt bpflatten bpmodify
# $ fix_android_bp_prebuilt --in-place path_to_problematic_android_bp

set -e

function usage() {
  cat <<EOF
Usage:
    $0 [OPTION]... FILE

Options:
    --in-place
        Edit file in place (overwrites source file)
    --diff
        Show diffs
    -h, --help, --usage
        Display this message and exit
EOF
}

function exit_handler() {
  readonly EXIT_CODE="$?"
  # Cleanup any temporary files
  rm -rf "$TEMP_DIR"
  exit "$EXIT_CODE"
}

trap exit_handler EXIT

function trim_space() {
  echo "$1" | sed -E 's/^[[:space:]]+//;s/[[:space:]]+$//'
}

function get_prop() {
  echo "${MODULE_PROP_VALUES_DICT[${1}:${2}]}"
}

function rewrite_prop() {
  local ORIGINAL_VALUE=$(trim_space "$(get_prop "$1" "$2")")
  if [[ -n "$ORIGINAL_VALUE" ]]; then
    bpmodify -m "$1" -property "$2" -r "$ORIGINAL_VALUE" -w "$TEMP_ANDROID_BP"
  fi
  if [[ -n "$3" ]]; then
    bpmodify -m "$1" -property "$2" -a "$3" -w "$TEMP_ANDROID_BP"
  fi
}

function get_dt_needed() {
  local DYNAMIC_TABLE=$($READELF -d "${ANDROID_BP_DIR}/$1")
  if [[ "$?" -ne 0 ]]; then
    return 1
  fi
  echo "$DYNAMIC_TABLE" |
    sed -n -E 's/^[[:space:]]*0x[[:xdigit:]]+[[:space:]]+\(NEEDED\).*\[(.+)\.so\].*$/\1/p' |
    xargs
}

function unique() {
  echo "$1" | xargs -n1 | sort | uniq | xargs
}


while [[ "$1" =~ ^- ]]; do
  case "$1" in
    -h | --help | --usage)
      usage
      exit 0
      ;;
    --in-place)
      EDIT_IN_PLACE=1
      ;;
    --diff)
      SHOW_DIFF=1
      ;;
    -x)
      set -x
      ;;
    --)
      shift
      break
      ;;
    *)
      echo >&2 "Unexpected flag: $1"
      usage >&2
      exit 1
      ;;
  esac
  shift
done

if ! [[ -f "$1" ]]; then
  echo >&2 "No such file: '$1'"
  exit 1
fi

if [[ -e "$(command -v llvm-readelf)" ]]; then
  READELF="llvm-readelf"
elif [[ -e "$(command -v readelf)" ]]; then
  READELF="readelf -W"
else
  echo >&2 'Cannot find readelf in $PATH, please run:'
  echo >&2 '$ source build/envsetup.sh'
  exit 1
fi

if ! [[ -e "$(command -v bpflatten)" && -e "$(command -v bpmodify)" ]]; then
  echo >&2 'Cannot find bpflatten and bpmodify in $PATH, please run:'
  echo >&2 '$ source build/envsetup.sh'
  echo >&2 '$ m blueprint_tools'
  exit 1
fi

readonly EDIT_IN_PLACE
readonly SHOW_DIFF
readonly READELF
readonly ANDROID_BP="$1"
readonly ANDROID_BP_DIR=$(dirname "$ANDROID_BP")
readonly TEMP_DIR=$(mktemp -d)
readonly TEMP_ANDROID_BP="${TEMP_DIR}/Android.bp"

cp -L "$ANDROID_BP" "$TEMP_ANDROID_BP"

# This subshell and `eval` must be on separate lines, so that eval would not
# shadow the subshell's exit code.
# In other words, if `bpflatten` fails, we mustn't eval its output.
FLATTEN_COMMAND=$(bpflatten --bash "$ANDROID_BP")
eval "$FLATTEN_COMMAND"

for MODULE_NAME in "${MODULE_NAMES[@]}" ; do
  MODULE_TYPE="${MODULE_TYPE_DICT[${MODULE_NAME}]}"
  if ! [[ "$MODULE_TYPE" =~ ^(.+_)?prebuilt(_.+)?$ ]]; then
    continue
  fi

  SRCS=$(get_prop "$MODULE_NAME" "srcs")
  SHARED_LIBS=$(get_prop "$MODULE_NAME" "shared_libs")
  if [[ -n "${SRCS}" ]]; then
    DT_NEEDED=$(get_dt_needed "$SRCS")
    if [[ $(unique "$DT_NEEDED") != $(unique "$SHARED_LIBS") ]]; then
      rewrite_prop "$MODULE_NAME" "shared_libs" "$DT_NEEDED"
    fi
  fi

  # Handle different arch / target variants...
  for PROP in ${MODULE_PROP_KEYS_DICT[${MODULE_NAME}]} ; do
    if ! [[ "$PROP" =~ \.srcs$ ]]; then
      continue
    fi
    SRCS=$(get_prop "$MODULE_NAME" "$PROP")
    DT_NEEDED=$(get_dt_needed "$SRCS")
    SHARED_LIBS_PROP="${PROP%.srcs}.shared_libs"
    VARIANT_SHARED_LIBS="${SHARED_LIBS} $(get_prop "$MODULE_NAME" "$SHARED_LIBS_PROP")"
    if [[ $(unique "$DT_NEEDED") != $(unique "$VARIANT_SHARED_LIBS") ]]; then
      rewrite_prop "$MODULE_NAME" "$SHARED_LIBS_PROP" "$DT_NEEDED"
    fi
  done
done

if [[ -n "$SHOW_DIFF" ]]; then
  diff -u "$ANDROID_BP" "$TEMP_ANDROID_BP" || true
fi

if [[ -n "$EDIT_IN_PLACE" ]]; then
  cp "$TEMP_ANDROID_BP" "$ANDROID_BP"
fi

if [[ -z "${SHOW_DIFF}${EDIT_IN_PLACE}" ]]; then
  cat "$TEMP_ANDROID_BP"
fi
