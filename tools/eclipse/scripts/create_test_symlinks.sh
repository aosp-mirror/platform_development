#!/bin/bash

# CD to the top android directory
D=`dirname "$0"`
cd "$D/../../../../"

DEST="development/tools/eclipse/plugins/com.android.ide.eclipse.tests"
# computes "../.." from DEST to here (in /android)
BACK=`echo $DEST | sed 's@[^/]*@..@g'`

HOST=`uname`
if [ "$HOST" == "Linux" ]; then
    ln -svf $BACK/out/host/linux-x86/framework/kxml2-2.3.0.jar "$DEST/"

elif [ "$HOST" == "Darwin" ]; then
    ln -svf $BACK/out/host/darwin-x86/framework/kxml2-2.3.0.jar "$DEST/"

elif [ "${HOST:0:6}" == "CYGWIN" ]; then
    JAR="kxml2-2.3.0.jar"
    if [ ! -f "$DEST/$JAR" ]; then
        # Get the jar from ADT if we can, otherwise download it.
        if [ -f "$DEST/../com.android.ide.eclipse.adt/$JAR" ]; then
            cp "$DEST/../com.android.ide.eclipse.adt/$JAR" "$JAR"
        else
            wget -O "$DEST/$JAR" "http://internap.dl.sourceforge.net/sourceforge/kxml/$JAR"
        fi
        chmod a+rx "$DEST/$JAR"
    fi
else
    echo "Unsupported platform ($HOST). Nothing done."
fi

