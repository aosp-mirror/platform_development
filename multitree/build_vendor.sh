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

export ALLOW_MISSING_DEPENDENCIES=true

build/soong/soong_ui.bash --make-mode vendorimage collect_ninja_inputs \
  TARGET_PRODUCT=cf_x86_64_phone TARGET_BUILD_VARIANT=userdebug

mkdir -p $DIST_DIR/soong

for f in out/*.ninja out/soong/build.ninja; do
  gzip -c $f > $DIST_DIR/${f#out/}.gz
done

cp out/target/product/vsoc_x86_64/vendor.img $DIST_DIR

out/host/linux-x86/bin/collect_ninja_inputs -n prebuilts/build-tools/linux-x86/bin/ninja \
  -f out/combined-cf_x86_64_phone.ninja -t vendorimage > $DIST_DIR/ninja_inputs.json
