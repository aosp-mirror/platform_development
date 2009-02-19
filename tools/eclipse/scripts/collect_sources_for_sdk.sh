#!/bin/bash

function usage() {
    cat <<EOF
 Description:
   This script collects all framework Java sources from the current android
   source code and places them in a source folder suitable for the eclipse ADT
   plugin.

 Usage:
   $0 [-n] <android-git-repo root> <sdk/platforms/xyz/sources>
 
 The source and destination directories must already exist.
 Use -n for a dry-run.

EOF
}

DRY=""
if [ "-n" == "$1" ]; then
    DRY="echo"
    shift
fi

SRC="$1"
DST="$2"

if [ -z "$SRC" ] || [ -z "$DST" ] || [ ! -d "$SRC" ] || [ ! -d "$DST" ]; then
    usage
    exit 1
fi

function process() {
    echo "Examine" $1
}

N=0
E=0
for i in `find -L "${SRC}/frameworks" -name "*.java"`; do
    if [ -f "$i" ]; then
        # look for ^package (android.view.blah);$
        PACKAGE=`sed -n '/^package [^ ;]\+; */{s/[^ ]* *\([^ ;]*\).*/\1/p;q}' "$i"`
        if [ -n "$PACKAGE" ]; then
            PACKAGE=${PACKAGE//./\/}    # e.g. android.view => android/view
            JAVA=`basename "$i"`        # e.g. View.java
            [ -z $DRY ] && [ ! -d "$DST/$PACKAGE" ] && mkdir -p -v "$DST/$PACKAGE"
            $DRY cp -v "$i" "$DST/$PACKAGE/$JAVA"
            N=$((N+1))
        else
            echo "Warning: $i does not have a Java package."
            E=$((E+1))
        fi
    fi
done

echo "$N java files copied"
[ $E -gt 0 ] && echo "$E warnings"

