#!/bin/bash -e
#
# Copyright (C) 2022 The Android Open Source Project
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

function usage() {
  cat <<EOF
Usage: $0 -d device-name -p product-name -r dist-dir -i build-id [targets]
Builds a vendor image for given product and analyze ninja inputs.

  -d  device name to build (e.g. vsoc_x86_64)
  -p  product name to build (e.g. cf_x86_64_phone)
  -r  directory for dist (e.g. out/dist)
  -i  build ID
  -u  whether it is an unbundled build
  -h  display this help and exit

EOF
  exit 1
}

while getopts d:p:r:i:uh flag
do
    case "${flag}" in
        d) device=${OPTARG};;
        p) product=${OPTARG};;
        r) dist_dir=${OPTARG};;
        i) build_id=${OPTARG};;
        u) unbundled_build=true;;
        h) usage;;
        *) usage;;
    esac
done

extra_targets=${@: ${OPTIND}}

if [[ -z "$device" || -z "$product" || -z "$dist_dir" || -z "$build_id" ]]; then
  echo "missing arguments"
  usage
fi

if [[ "$unbundled_build" = true ]]; then
  export TARGET_BUILD_UNBUNDLED_IMAGE=true
fi

export ALLOW_MISSING_DEPENDENCIES=true
export SKIP_VNDK_VARIANTS_CHECK=true
export DIST_DIR=$dist_dir

build/soong/soong_ui.bash --make-mode vendorimage collect_ninja_inputs dist $extra_targets \
  TARGET_PRODUCT=$product TARGET_BUILD_VARIANT=userdebug

cp out/target/product/$device/vendor.img $dist_dir

out/host/linux-x86/bin/collect_ninja_inputs -n prebuilts/build-tools/linux-x86/bin/ninja \
  -f out/combined-$product.ninja -t vendorimage -m $dist_dir/manifest_$build_id.xml \
  --out $dist_dir/logs/ninja_inputs
