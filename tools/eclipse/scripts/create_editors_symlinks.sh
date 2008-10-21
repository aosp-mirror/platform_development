#!/bin/bash
function die() {
  echo "Error: $*"
  exit 1
}

cd ../plugins/com.android.ide.eclipse.editors
HOST=`uname`
if [ "$HOST" == "Linux" ]; then
  ln -svf ../../../../out/host/linux-x86/framework/layoutlib_api.jar .
  ln -svf ../../../../out/host/linux-x86/framework/layoutlib_utils.jar .
  ln -svf ../../../../out/host/linux-x86/framework/kxml2-2.3.0.jar .
  ln -svf ../../../../out/host/linux-x86/framework/ninepatch.jar .
elif [ "$HOST" == "Darwin" ]; then
  ln -svf ../../../../out/host/darwin-x86/framework/layoutlib_api.jar .
  ln -svf ../../../../out/host/darwin-x86/framework/layoutlib_utils.jar .
  ln -svf ../../../../out/host/darwin-x86/framework/kxml2-2.3.0.jar .
  ln -svf ../../../../out/host/darwin-x86/framework/ninepatch.jar .
elif [ "${HOST:0:6}" == "CYGWIN" ]; then
  set -e # fail early
  DEVICE_DIR="../../../../"
  echo "make java libs ..."
  ( cd "$DEVICE_DIR" &&
      make -j3 showcommands layoutlib_api layoutlib_utils ninepatch ) || \
      die "Define javac and 'make layoutlib_api ninepatch' from device."

  echo "Copying java libs to $PWD"
  for JAR in layoutlib_api.jar layoutlib_utils.jar ninepatch.jar ; do
    cp -vf  "$DEVICE_DIR/out/host/windows-x86/framework/$JAR" .
  done
  if [ ! -f "./kxml2-2.3.0.jar" ]; then
      cp -v $DEVICE_DIR/prebuilt/common/kxml2/kxml2-2.3.0.jar .
      chmod -v a+rx *.jar
  fi

else
  echo "Unsupported platform ($HOST). Nothing done."
fi

