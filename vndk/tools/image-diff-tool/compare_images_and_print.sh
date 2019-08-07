#!/bin/bash
build/soong/soong_ui.bash --make-mode compare_images
. build/envsetup.sh
lunch aosp_arm64
out/host/linux-x86/bin/compare_images $1 -u
echo " - Common parts"
cat common.csv
echo
echo " - Different parts"
cat diff.csv
