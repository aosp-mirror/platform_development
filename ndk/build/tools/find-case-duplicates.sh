#!/bin/sh
#
# Copyright (C) 2009 The Android Open Source Project
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

# This script is used to find case-insensitive duplicate file names
# from the git repository. This is used to remove them when generating
# a new sysroot.

# location of the root ndk directory. we assume this script is under build/tools
NDK_ROOT_DIR=`dirname $0`/../..
NDK_ROOT_DIR=`cd $NDK_ROOT_DIR && pwd`

ORG_FILES=`(cd $NDK_ROOT_DIR && git ls-files) | sort -f`
NEW_FILES=

PREVFILE=
PREVUPFILE=XXXXXX
for FILE in $ORG_FILES; do
    # don't use [:lower:] and [:upper:] since they can produce
    # strange results based on the current locale.
    UPFILE=`echo $FILE | tr [a-z] [A-Z]`
    if [ "$UPFILE" != "$PREVUPFILE" ] ; then
      NEW_FILES="$NEW_FILES $FILE"
    else
      echo "$PREVFILE"
      echo "$FILE"
    fi
    PREVFILE=$FILE
    PREVUPFILE=$UPFILE
done
