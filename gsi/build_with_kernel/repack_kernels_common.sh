#!/bin/bash

# Copyright (C) 2021 The Android Open Source Project
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

if [ -z "${ANDROID_BUILD_TOP}" ]; then
    WD="$(pwd)"
else
    WD="${ANDROID_BUILD_TOP}"
fi
if [ -z "${OUT_DIR}" ]; then
    OUT_DIR="${WD}/out"
fi

if [[ -z "${DIST_DIR}" ]]; then
  echo "DIST_DIR must be defined." 1>&2
  exit 1
fi

GZIP="gzip"
LZ4="${OUT_DIR}/host/linux-x86/bin/lz4"

function prepare_lz4()
{
  if ! [ -f ${LZ4} ]; then
    echo "make $LZ4"
    cd ${WD}
    build/soong/soong_ui.bash --make-mode lz4
    cd -
  fi
}

#
#  This function copies kernel prebuilts from build chaining path to a staging
#  output directory.
#
function prepare_kernel_image()
{
  local prebuilt_path=$1
  local kernel_version=$2
  local arch=$3
  local build_variant=$4

  local prebuilts_root="out/prebuilt_cached/${prebuilt_path}"
  local out_root="${OUT_DIR}/target/kernel/${kernel_version}/${arch}"
  local dist_root="${DIST_DIR}/kernel/${kernel_version}"
  local postfix=""
  if [[ "$build_variant" == "debug" ]]; then
     dist_root="${dist_root}-debug"
     postfix="-allsyms"
  fi

  mkdir -p "${out_root}"

  printf "%-38s %20s\n" "copy kernel ${kernel_version} ${arch} ${build_variant} to" "${out_root}"
  if [[ "$arch" == "x86_64" ]]; then
    cp "${prebuilts_root}/bzImage" "${out_root}/kernel-${kernel_version}${postfix}"
  else
    # Pack all compress format for kernel images
    cp "${prebuilts_root}/Image" "${out_root}/kernel-${kernel_version}${postfix}"
    cp "${prebuilts_root}/Image.lz4" "${out_root}/kernel-${kernel_version}-lz4${postfix}"
    "$GZIP" -nc \
       "${prebuilts_root}/Image">"${out_root}/kernel-${kernel_version}-gz${postfix}"
  fi

  # Prepare the dist folder
  mkdir -p "${dist_root}"

  # Prepare prebuilt-info.txt
#  BID=$(cat "${prebuilts_root}/BUILD_INFO" | sed -n 's/^.*"bid":\s*"\(.*\)".*$/\1/p')
#  cat > "${dist_root}/prebuilt-info.txt" <<EOF
#{
#    "kernel-build-id": ${BID}
#}
#EOF
  printf "generate ${dist_root}/prebuilt-info.txt with kernel\n"
  cksum "${out_root}/kernel-${kernel_version}${postfix}"
  local t="${out_root}/prebuilt-info.txt"
  echo "{" > $t
  echo -n "    \"kernel-build-id\": " >> $t
  strings "${out_root}/kernel-${kernel_version}${postfix}" |grep -E "Linux version [0-9]\." | sed -e 's/Linux version.*-ab//'| cut -f1 -d ' ' >> $t
  echo "}" >> $t
  cp $t ${dist_root}/prebuilt-info.txt

}

#
#  This function copies kernel module prebuilts from the build chaining path to a
#  staging output directory.
#
function prepare_kernel_modules()
{
  local prebuilt_path=$1
  local kernel_version=$2
  local arch=$3

  local prebuilts_root="out/prebuilt_cached/${prebuilt_path}"
  local out_root="${OUT_DIR}/target/kernel/${kernel_version}/${arch}"
  local dist_root="${DIST_DIR}/kernel/${kernel_version}"

  local initramfs_root="${out_root}/initramfs"
  rm -rf ${initramfs_root}
  mkdir -p "${initramfs_root}"

  printf "%-38s %20s\n" "copy kernel modules ${kernel_version} ${arch} to" "${initramfs_root}"
  "${LZ4}" -dcfm "${prebuilts_root}/initramfs.img" | (cd "${initramfs_root}"; cpio -imd)

  for x in $(find "${initramfs_root}" -type f -name "*.ko"); do
    cp "$x" "${out_root}"
  done
}

#
#  This function updates kernel prebuilts with the local staging directory.
#
function update_kernel_prebuilts_with_artifact
{
  local kernel_version=$1
  local arch=$2
  local out_root="${OUT_DIR}/target/kernel/${kernel_version}/${arch}"
  local prebuilts_dir="kernel/prebuilts/${kernel_version}/${arch}/"
  local list="\
    kernel-${kernel_version}-allsyms \
    kernel-${kernel_version}-gz-allsyms \
    kernel-${kernel_version}-lz4-allsyms \
    kernel-${kernel_version} \
    kernel-${kernel_version}-gz \
    kernel-${kernel_version}-lz4 \
    prebuilt-info.txt"
  printf "%20s\n --> %20s\n" "${out_root}" "${prebuilts_dir}"
  for f in ${list}; do
    echo \
    cp -f ${out_root}/$f ${prebuilts_dir}
    cp -f ${out_root}/$f ${prebuilts_dir}
  done
}

#
#  This function updates kernel module prebuilts with the local staging directory.
#
function update_kernel_module_prebuilts_with_artifact
{
  local kernel_version=$1
  local arch=$2
  local out_root="${OUT_DIR}/target/kernel/${kernel_version}/${arch}"
  local initramfs_root="${out_root}/initramfs"
  local prebuilts_dir="kernel/prebuilts/common-modules/virtual-device/${kernel_version}/${arch}/"

  printf "%20s\n --> %20s\n" "${initramfs_root}" "${prebuilts_dir}"
  rm -f ${prebuilts_dir}/*.ko
  for x in $(find "${initramfs_root}" -type f -name "*.ko"); do
    cp -f ${x} ${prebuilts_dir}
  done
}

function pack_boot_for_certification
{
  local file=boot.zip
  local dist=$1
  pushd ${dist}
  rm -f ${file}
  zip ${file} boot*.img
  popd
}
