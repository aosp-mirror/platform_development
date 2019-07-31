#!/bin/bash
build/soong/soong_ui.bash --make-mode compare_images
compare_images $1
echo " - Common parts"
cat common.csv
echo
echo " - Different parts"
cat diff.csv
