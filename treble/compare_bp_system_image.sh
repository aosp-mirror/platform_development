#!/bin/bash
echo "Note: Should run 'lunch aosp_cf_x86_64_phone-trunk_staging-userdebug && m' before running this script"
bp_base_path=$ANDROID_BUILD_TOP/out/soong/.intermediates/build/make/target/product/generic/aosp_shared_system_image/android_common
bp_path=$bp_base_path/system
echo $OUT
echo $bp_path
compare_images -t $OUT $bp_path -s system -i
