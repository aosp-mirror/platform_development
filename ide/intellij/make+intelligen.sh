#!/bin/bash

if [ ! -f buildspec.mk.default ]; then
    echo "Error: Please run from the root of the tree."
    exit 1
fi
    
. envsetup.sh
lunch 6

if make -j4; then
    tools/javabuild/intelligen.sh
fi
