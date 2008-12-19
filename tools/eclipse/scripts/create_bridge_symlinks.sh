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
    if [ "x$1" == "x" ] || [ `basename "$1"` != "layoutlib.jar" ]; then
        echo "Usage: $0 sdk/platforms/xxx/data/layoutlib.jar"
        echo "Argument 1 should be the path to the layoutlib.jar that should be updated."
        exit 1
    fi

    LIBS="layoutlib ninepatch"
    echo "Make java libs: $LIBS"
    make -j3 showcommands $LIBS || die "Bridge: Failed to build one of $LIBS."

    echo "Updating your SDK in $1"
    cp -vf  "out/host/windows-x86/framework/layoutlib.jar" "$1"
    chmod -v a+rx "$1"

else
    echo "Unsupported platform ($HOST). Nothing done."
fi

