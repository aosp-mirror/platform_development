#!/bin/bash

set -e

# CD to the top android directory
D=`dirname "$0"`
cd "$D/../../../../"

# computes relative ".." paths from $1 to here (in /android)
function back() {
  echo $1 | sed 's@[^/]*@..@g'
}

HOST=`uname`
if [ "${HOST:0:6}" == "CYGWIN" ]; then
    # We can't use symlinks under Cygwin
    function cpdir() { # $1=dest $2=source
        echo "rsync $2 => $1"
        rsync -avW --delete-after $2 $1
    }

else
    # For all other systems which support symlinks
    function cpdir() { # $1=dest $2=source
        ln -svf `back $1`/$2 $1
    }
fi

BASE="development/tools/eclipse/plugins/com.android.ide.eclipse.tests"
DEST=$BASE
BACK=`back $DEST`

HOST=`uname`
if [ "$HOST" == "Linux" ]; then
    ln -svf $BACK/out/host/linux-x86/framework/kxml2-2.3.0.jar "$DEST/"
    ln -svf $BACK/out/host/linux-x86/framework/layoutlib.jar   "$DEST/"

elif [ "$HOST" == "Darwin" ]; then
    ln -svf $BACK/out/host/darwin-x86/framework/kxml2-2.3.0.jar "$DEST/"
    ln -svf $BACK/out/host/darwin-x86/framework/layoutlib.jar   "$DEST/"

elif [ "${HOST:0:6}" == "CYGWIN" ]; then
    if [ ! -f "$DEST/kxml2-2.3.0.jar" ]; then
        cp -v "prebuilt/common/kxml2/kxml2-2.3.0.jar" "$DEST/"
    fi

    LIBS="layoutlib.jar sdkuilib.jar"
    NEED_MAKE="yes"
    for LIB in $LIBS ; do
        SRCJAR="out/host/windows-x86/framework/$LIB"
        DSTJAR="$DEST/$LIB"
        if [[ $NEED_MAKE ]] && ! diff -q "$SRCJAR" "$DSTJAR" >/dev/null ; then
            MAKE_LIBS="${LIBS//.jar/}"
            echo "Make java libs: $MAKE_LIBS"
            make -j3 showcommands $MAKE_LIBS || die "adt-tests: Failed to build one of $LIBS."
            NEED_MAKE=""
        fi
        
        cp -v "$SRCJAR" "$DSTJAR"
    done

    chmod -v a+rx "$DEST"/*.jar
else
    echo "Unsupported platform ($HOST). Nothing done."
fi

# create link to ddmlib tests
DEST=$BASE/unittests/com/android
cpdir $DEST development/tools/ddms/libs/ddmlib/tests/src/com/android/ddmlib
cpdir $DEST development/tools/sdkmanager/libs/sdklib/tests/com/android/sdklib
cpdir $DEST development/tools/sdkmanager/libs/sdkuilib/tests/com/android/sdkuilib

DEST=$BASE/unittests/com/android/layoutlib
mkdir -p $DEST
cpdir $DEST frameworks/base/tools/layoutlib/bridge/tests/com/android/layoutlib/bridge
cpdir $DEST frameworks/base/tools/layoutlib/bridge/tests/com/android/layoutlib/testdata

