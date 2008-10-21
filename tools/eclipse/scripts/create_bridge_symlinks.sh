#!/bin/bash
function die() {
  echo "Error: $*"
  exit 1
}

set -e # fail early

# This may run either from the //device directory or from the
# eclipse/script directory. Allow for both.
D="device/tools/eclipse/scripts"
[ -d "$D" ] && cd "$D"
[ -d "../$D" ] && cd "../$D"

cd ../../layoutlib

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

  DEVICE_DIR="../../"
  echo "make java libs ..."
  ( cd "$DEVICE_DIR" &&
      make -j3 showcommands layoutlib ninepatch ) || \
      die "Define javac and retry."

  for DIR in "$PWD" "$1" ; do
      echo "Copying java libs to $DIR"
      for JAR in ninepatch.jar layoutlib.jar ; do
          cp -vf  "$DEVICE_DIR/out/host/windows-x86/framework/$JAR" "$DIR"
      done
  done

else
  echo "Unsupported platform ($HOST). Nothing done."
fi

