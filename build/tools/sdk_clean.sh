#!/bin/bash
#
# This script cleans up a set of files given as arguments for release in the SDK
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
find $DIR -name "*.java" -o -name "*.xml" | xargs -n 1 \
    sed \
        -e "s/.*BEGIN_INCLUDE(.*//" \
        -e "s/.*END_INCLUDE(.*//" \
        -i

#
# Fix up the line endings of all text files. This also removes executable permissions.
#
if [ $HOST_OS = windows ] ; then
    ENDING_TYPE=dos
else
    ENDING_TYPE=unix
fi

# Using -n 500 for xargs to limit the max number of arguments per call to line_endings
# to 500. This avoids line_endings failing with "arguments too long".
find $DIR -name "*.aidl" -o -name "*.css" -o -name "*.html" -o -name "*.java" \
                     -o -name "*.js" -o -name "*.prop" -o -name "*.template" \
                     -o -name "*.txt" -o -name "*.windows" -o -name "*.xml" \
        | xargs -n 500 $HOST_OUT_EXECUTABLES/line_endings $ENDING_TYPE


