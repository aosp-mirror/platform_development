#!/bin/bash

if [[ -z "$ANDROID_BUILD_TOP" ]]; then
  >&2 echo "ANDROID_BUILD_TOP not set in environment. Run lunch."
  exit 1
fi

set -e
set -x

$ANDROID_BUILD_TOP/build/soong/soong_ui.bash --make-mode update-ndk-abi
update-ndk-abi --src-dir $ANDROID_BUILD_TOP $ANDROID_BUILD_TOP/ndk-abi-out "$@"
