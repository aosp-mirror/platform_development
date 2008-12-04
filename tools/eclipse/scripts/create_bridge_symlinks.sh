#!/bin/bash
function die() {
    echo "Error: $*"
    exit 1
}

set -e # fail early

# CD to the top android directory
D=`dirname "$0"`
cd "$D/../../../../"

HOST=`uname`
if [ "$HOST" == "Linux" ]; then
    echo # nothing to do

elif [ "$HOST" == "Darwin" ]; then
    echo # nothing to do

elif [ "${HOST:0:6}" == "CYGWIN" ]; then
    if [ "x$1" == "x" ]; then
        echo "Usage: $0 sdk/tools/lib/"
        echo "Argument 1 should be the path to the jars you want to copy. "
        echo "  e.g. android_sdk_windows_NNN/tools/lib/ "
        echo "This will be used to update layout.lib after it has been built here."
        exit 1
    fi

    echo "make java libs ..."
    LIBS="layoutlib ninepatch"
    make -j3 showcommands $LIBS || die "Define javac and retry."

    for DIR in frameworks/base/tools/layoutlib "$1" ; do
        echo "Copying java libs to $DIR"
        for LIB in $LIBS; do
            cp -vf  "out/host/windows-x86/framework/$LIB.jar" "$DIR"
        done
        chmod -v a+rx "$LIB"/*.jar
    done

else
    echo "Unsupported platform ($HOST). Nothing done."
fi

