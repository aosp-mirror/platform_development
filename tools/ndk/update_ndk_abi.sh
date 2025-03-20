#!/bin/bash
set -e
set -x

THIS_DIR=$(dirname "$(realpath $0)")
TOP=$(realpath $THIS_DIR/../../..)

TARGET_RELEASE=trunk_staging $TOP/build/soong/soong_ui.bash --make-mode update-ndk-abi
$TOP/out/host/linux-x86/bin/update-ndk-abi --src-dir $TOP $TOP/ndk-abi-out "$@"
