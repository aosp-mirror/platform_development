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

cd ../plugins/com.android.ide.eclipse.adt
HOST=`uname`
if [ "$HOST" == "Linux" ]; then
  ln -svf ../../../../out/host/linux-x86/framework/jarutils.jar .
  ln -svf ../../../../out/host/linux-x86/framework/androidprefs.jar .
elif [ "$HOST" == "Darwin" ]; then
  ln -svf ../../../../out/host/darwin-x86/framework/jarutils.jar .
  ln -svf ../../../../out/host/darwin-x86/framework/androidprefs.jar .
elif [ "${HOST:0:6}" == "CYGWIN" ]; then

  DEVICE_DIR="../../../.."
  echo "make java libs ..."
  ( cd "$DEVICE_DIR" &&
      make -j3 showcommands jarutils androidprefs ) || \
      die "Define javac and retry."

  for DIR in "$PWD" ; do
      echo "Copying java libs to $DIR"
      for JAR in jarutils.jar androidprefs.jar ; do
          cp -vf  "$DEVICE_DIR/out/host/windows-x86/framework/$JAR" "$DIR"
      done
  done

  chmod a+rx *.jar
else
  echo "Unsupported platform ($HOST). Nothing done."
fi

