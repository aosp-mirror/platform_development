#!/bin/bash
function die() {
    echo "Error: $*"
    exit 1
}

set -e # fail early

# CD to the top android directory
D=`dirname "$0"`
cd "$D/../../../../"

DEST="development/tools/eclipse/plugins/com.android.ide.eclipse.adt"
# computes "../.." from DEST to here (in /android)
BACK=`echo $DEST | sed 's@[^/]*@..@g'`

LIBS="sdkstats jarutils androidprefs layoutlib_api layoutlib_utils ninepatch sdklib sdkuilib"

echo "make java libs ..."
make -j3 showcommands $LIBS || die "ADT: Fail to build one of $LIBS."

echo "Copying java libs to $DEST"

HOST=`uname`
if [ "$HOST" == "Linux" ]; then
    for LIB in $LIBS; do
        ln -svf $BACK/out/host/linux-x86/framework/$LIB.jar "$DEST/"
    done
    ln -svf $BACK/out/host/linux-x86/framework/kxml2-2.3.0.jar "$DEST/"
    ln -svf $BACK/out/host/linux-x86/framework/commons-compress-1.0.jar "$DEST/"
  
elif [ "$HOST" == "Darwin" ]; then
    for LIB in $LIBS; do
        ln -svf $BACK/out/host/darwin-x86/framework/$LIB.jar "$DEST/"
    done
    ln -svf $BACK/out/host/darwin-x86/framework/kxml2-2.3.0.jar "$DEST/"
    ln -svf $BACK/out/host/darwin-x86/framework/commons-compress-1.0.jar "$DEST/"

elif [ "${HOST:0:6}" == "CYGWIN" ]; then
    for LIB in $LIBS; do
        cp -vf  out/host/windows-x86/framework/$LIB.jar "$DEST/"
    done

    if [ ! -f "$DEST/kxml2-2.3.0.jar" ]; then
        cp -v "prebuilt/common/kxml2/kxml2-2.3.0.jar" "$DEST/"
    fi

    if [ ! -f "$DEST/commons-compress-1.0.jar" ]; then
        cp -v "prebuilt/common/commons-compress/commons-compress-1.0.jar" "$DEST/"
    fi

    chmod -v a+rx "$DEST"/*.jar
else
    echo "Unsupported platform ($HOST). Nothing done."
fi

