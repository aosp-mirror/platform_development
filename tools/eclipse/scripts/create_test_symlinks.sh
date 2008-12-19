#!/bin/bash

set -e

# CD to the top android directory
D=`dirname "$0"`
cd "$D/../../../../"

# computes relative ".." paths from $1 to here (in /android)
function back() {
  echo $1 | sed 's@[^/]*@..@g'
}

BASE="development/tools/eclipse/plugins/com.android.ide.eclipse.tests"
DEST=$BASE
BACK=`back $DEST`


HOST=`uname`
if [ "$HOST" == "Linux" ]; then
    DIR="ln -svf"
    ln -svf $BACK/out/host/linux-x86/framework/kxml2-2.3.0.jar "$DEST/"

elif [ "$HOST" == "Darwin" ]; then
    DIR="ln -svf"
    ln -svf $BACK/out/host/darwin-x86/framework/kxml2-2.3.0.jar "$DEST/"

elif [ "${HOST:0:6}" == "CYGWIN" ]; then
    DIR="rsync -avW --delete-after"
    JAR="kxml2-2.3.0.jar"
    if [ ! -f "$DEST/$JAR" ]; then
        # Get the jar from ADT if we can, otherwise download it.
        if [ -f "$DEST/../com.android.ide.eclipse.adt/$JAR" ]; then
            cp "$DEST/../com.android.ide.eclipse.adt/$JAR" "$DEST/$JAR"
        else
            wget -O "$DEST/$JAR" "http://internap.dl.sourceforge.net/sourceforge/kxml/$JAR"
        fi
        chmod a+rx "$DEST/$JAR"
    fi
else
    echo "Unsupported platform ($HOST). Nothing done."
fi

# create link to ddmlib tests
DEST=$BASE/unittests/com/android
BACK=`back $DEST`
$DIR $BACK/development/tools/ddms/libs/ddmlib/tests/src/com/android/ddmlib $DEST/

