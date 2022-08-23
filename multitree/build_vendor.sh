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
Usage: $0 -d device-name -p product-name -r dist-dir -i build-id
Builds a vendor image for given product and analyze ninja inputs.

  -d  device name to build (e.g. vsoc_x86_64)
  -p  product name to build (e.g. cf_x86_64_phone)
  -r  directory for dist (e.g. out/dist)
  -i  build ID
  -h  display this help and exit

EOF
  exit 1
}

while getopts d:p:r:i:h flag
do
    case "${flag}" in
        d) device=${OPTARG};;
        p) product=${OPTARG};;
        r) dist_dir=${OPTARG};;
        i) build_id=${OPTARG};;
        h) usage;;
        *) usage;;
    esac
done

if [[ -z "$device" || -z "$product" || -z "$dist_dir" || -z "$build_id" ]]; then
  echo "missing arguments"
  usage
fi

export ALLOW_MISSING_DEPENDENCIES=true

build/soong/soong_ui.bash --make-mode vendorimage collect_ninja_inputs \
  TARGET_PRODUCT=$product TARGET_BUILD_VARIANT=userdebug

mkdir -p $dist_dir/soong

for f in out/*.ninja out/soong/build.ninja; do
  gzip -c $f > $dist_dir/${f#out/}.gz
done

cp out/target/product/$device/vendor.img $dist_dir

out/host/linux-x86/bin/collect_ninja_inputs -n prebuilts/build-tools/linux-x86/bin/ninja \
  -f out/combined-$product.ninja -t vendorimage -m $dist_dir/manifest_$build_id.xml \
  > $dist_dir/ninja_inputs.json
