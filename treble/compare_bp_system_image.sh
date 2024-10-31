#!/bin/bash
echo "Note: Should run 'lunch aosp_cf_x86_64_only_phone-trunk_staging-userdebug && m generic_system_image && m' before running this script"
bp_base_path=$ANDROID_BUILD_TOP/out/soong/.intermediates/build/make/target/product/generic/generic_system_image/android_common
bp_path=$bp_base_path/root
echo $OUT
echo $bp_path
compare_images -t $OUT $bp_path -s system -i
