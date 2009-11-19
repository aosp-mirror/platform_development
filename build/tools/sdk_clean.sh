#!/bin/bash
#
# This script processes a set of files given as arguments as sample code to be  released
# in the SDK.
#
# Note that these files are modified in-place.
#

DIR=$1

#
# Remove BEGIN_INCLUDE and END_INCLUDE lines used by the javadoc.
#
# This does it by replacing these lines with blank lines so line numbers aren't
# changed in the process, making it easier to match 3rd party complaints/questions
# with the source tree.
#
# sed on Mac OS takes -i SUFFIX and sed on Linux takes -iSUFFIX
#
if [ $HOST_OS = darwin ] ; then
find $DIR -name "*.java" -o -name "*.xml" | xargs -n 1 \
    sed \
        -e "s/.*BEGIN_INCLUDE(.*//" \
        -e "s/.*END_INCLUDE(.*//" \
        -i ""
else
find $DIR -name "*.java" -o -name "*.xml" | xargs -n 1 \
    sed \
        -e "s/.*BEGIN_INCLUDE(.*//" \
        -e "s/.*END_INCLUDE(.*//" \
        -i
fi

#
# Fix up the line endings of all text files
#
if [ $HOST_OS = windows ] ; then
    ENDING_TYPE=dos
else
    ENDING_TYPE=unix
fi
find $DIR -name "*.aidl" -o -name "*.css" -o -name "*.html" -o -name "*.java" \
                     -o -name "*.js" -o -name "*.prop" -o -name "*.py" \
                     -o -name "*.template" -o -name "*.txt" -o -name "*.windows" \
                     -o -name "*.xml" \
        | xargs $HOST_OUT_EXECUTABLES/line_endings $ENDING_TYPE


