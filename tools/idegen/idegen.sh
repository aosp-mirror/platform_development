#!/bin/bash

if [ ! -d development ]; then
    echo "Error: Run from the root of the tree."
    exit 1
fi

if [[ -z ${OUT_DIR_COMMON_BASE} ]]; then
idegenjar=`find out -name idegen.jar -follow | grep -v intermediates`
else
idegenjar=`find $OUT_DIR_COMMON_BASE/$(basename "$PWD") -name idegen.jar -follow | grep -v intermediates`
fi

if [ "" = "$idegenjar" ]; then
    echo "Couldn't find idegen.jar. Please run make first."
else
    java -cp $idegenjar Main
fi
