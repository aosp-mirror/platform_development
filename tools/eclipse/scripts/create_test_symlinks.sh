#!/bin/bash
cd ../plugins/com.android.ide.eclipse.tests
HOST=`uname`
if [ "$HOST" == "Linux" ]; then
  ln -svf ../../../../out/host/linux-x86/framework/kxml2-2.3.0.jar .
elif [ "$HOST" == "Darwin" ]; then
  ln -svf ../../../../out/host/darwin-x86/framework/kxml2-2.3.0.jar .
elif [ "${HOST:0:6}" == "CYGWIN" ]; then
  if [ "x$1" == "x" ]; then
    echo "Usage: $0 <path to jars>"
    echo "Argument 1 should be the path to the jars you want to copy. "
    echo "  e.g. android_sdk_windows_NNN/tools/lib/ "
    echo "(since you can't rebuild them under Windows, you need prebuilt ones "
    echo " from an SDK drop or a Linux/Mac)"
    exit 1
  fi
  if [ ! -f "kxml2-2.3.0.jar" ]; then
    wget -O "kxml2-2.3.0.jar" "http://internap.dl.sourceforge.net/sourceforge/kxml/kxml2-2.3.0.jar"
    chmod a+rx *.jar
  fi
else
  echo "Unsupported platform ($HOST). Nothing done."
fi

