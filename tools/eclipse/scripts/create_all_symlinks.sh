#!/bin/bash

HOST=`uname`
if [ "${HOST:0:6}" == "CYGWIN" ]; then
    if [ "x$1" == "x" ] || [ `basename "$1"` != "layoutlib.jar" ]; then
        echo "Usage: $0 sdk/platforms/xxx/data/layoutlib.jar"
        echo "Argument 1 should be the path to the layoutlib.jar that should be updated by create_bridge_symlinks.sh."
        exit 1
    fi
fi

echo "### $0 executing"

function die() {
    echo "Error: $*"
    exit 1
}

# CD to the top android directory
D=`dirname "$0"`
cd "$D/../../../../"

DEST="development/tools/eclipse/scripts"

set -e # fail early

echo ; echo "### ADT ###" ; echo
$DEST/create_adt_symlinks.sh "$*"
echo ; echo "### DDMS ###" ; echo
$DEST/create_ddms_symlinks.sh "$*"
echo ; echo "### TEST ###" ; echo
$DEST/create_test_symlinks.sh "$*"
echo ; echo "### BRIDGE ###" ; echo
$DEST/create_bridge_symlinks.sh "$*"

echo "### $0 done"
