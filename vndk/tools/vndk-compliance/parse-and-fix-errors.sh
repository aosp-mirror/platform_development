#!/bin/bash
# This script uses log file saved from make >log 2>&1. It parses and
# fixes the "file not found" errors by adding dependencies to reported
# modules' Android.mk file. It works for following types of issues:
# error: 'hardware/<file>.h' file not found
# error: 'system/<file>.h' file not found
# error: 'cutils/<file>.h' file not found
# error: 'utils/<file>.h' file not found
# error: 'log/<file>.h' file not found
#
# More can be added by expanding ADD_TO_*_LIBS string
#
# This script will create temp files log.<type> and log.<type>.paths
#
# This script requires manual intervention in 2 places:
# 1. Visually inspecting log.<type>.paths and removing undesirable lines
# 2. Manually checking in uncommitted files reported by repo status


if [ "$PWD" != "$ANDROID_BUILD_TOP" ]; then
  echo "This script needs to be run at top level folder"
  exit 1
fi
if [ ! -f "log" ]; then
  echo "log file not found"
  exit 1
fi

echo "Parsing log"
cat log | grep "FAILED\|error:" > log.error

#libs that should be added to LOCAL_HEADER_LIBRARIES
ADD_TO_HEADER_LIBS=(hardware system cutils utils)

#libs that should be added to LOCAL_SHARED_LIBRARIES
ADD_TO_SHARED_LIBS=(log)

ALL_LIBS=(${ADD_TO_HEADER_LIBS[@]} ${ADD_TO_SHARED_LIBS[@]})

for lib in "${ALL_LIBS[@]}"; do
  echo "Parsing log.error for $lib"
  cat log.error | grep -B1 "error: '$lib\/" | grep FAILED | awk 'BEGIN{FS="_intermediates"}{print $1}' | awk 'BEGIN{FS="S/";}{print $2}' | sort -u > log.$lib

  echo "Parsing log.$lib"
  for module in `cat log.$lib`; do find . -name Android.\* | xargs grep -w -H $module | grep "LOCAL_MODULE\|name:"; done > log.$lib.paths

  echo "Please inspect log.$lib.paths and remove lines for devices other than the one you are compiling for."
  echo "Also remove duplicate makefile paths, even if they have different module names."
  echo "Then press Enter"
  read enter
  if [ -s "log.$lib.paths" ]; then
    not_vendor_list=`cat log.$lib.paths | awk 'BEGIN{FS=":"}{print $1}' | xargs grep -L 'LOCAL_PROPRIETARY_MODULE\|LOCAL_VENDOR_MODULE'`
  else
    not_vendor_list=
  fi
  if [ ! -z "$not_vendor_list" ]; then
    echo "These modules do NOT have proprietary or vendor flag set."
    printf "%s\n" $not_vendor_list
    echo "Please check the makefile and update log."$lib".paths, then press Enter"
    read enter
  fi
done

for lib in "${ADD_TO_HEADER_LIBS[@]}"; do
  echo "Patching makefiles to fix "$lib" errors"
  cat log.$lib.paths | awk 'BEGIN{FS=":"}{print $1}' | xargs sed -i '/include \$(BUILD/i LOCAL_HEADER_LIBRARIES += lib'$lib'_headers'
  echo "Checking for unsaved files"
  repo status
  echo "Please COMMIT them, then press Enter:"
  read enter
done

for lib in "${ADD_TO_SHARED_LIBS[@]}"; do
  echo "Patching makefiles to fix "$lib" errors"
  if [ $lib -eq "log" ]; then
    cat log.$lib.paths | awk 'BEGIN{FS=":"}{print $1}' | xargs sed -i '/include \$(BUILD/i ifdef BOARD_VNDK_VERSION\nLOCAL_SHARED_LIBRARIES += lib'$lib'\nendif'
  else
    cat log.$lib.paths | awk 'BEGIN{FS=":"}{print $1}' | xargs sed -i '/include \$(BUILD/i LOCAL_SHARED_LIBRARIES += lib'$lib
  fi
  echo "Checking for unsaved files"
  repo status
  echo "Please COMMIT them, then press Enter:"
  read enter
done

