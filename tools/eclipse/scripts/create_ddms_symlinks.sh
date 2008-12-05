#!/bin/bash
#----------------------------------------------------------------------------|
# Creates the links to use ddm{ui}lib in the eclipse-ide plugin.
# Run this from device/tools/eclipse/scripts
#----------------------------------------------------------------------------|

CMD="ln -svf"
DIR="ln -svf"
HOST=`uname`
if [ "${HOST:0:6}" == "CYGWIN" ]; then
    CMD="cp -rvf"
    DIR="rsync -avW --delete-after"
fi

# CD to the top android directory
D=`dirname "$0"`
cd "$D/../../../../"

# computes relative ".." paths from $1 to here (in /android)
function back() {
  echo $1 | sed 's@[^/]*@..@g'
}

BASE="development/tools/eclipse/plugins/com.android.ide.eclipse.ddms"

DEST=$BASE/libs
mkdir -p $DEST
BACK=`back $DEST`
for i in prebuilt/common/jfreechart/*.jar; do
  $CMD $BACK/$i $DEST/
done

DEST=$BASE/src/com/android
BACK=`back $DEST`
for i in development/tools/ddms/libs/ddmlib/src/com/android/ddmlib \
         development/tools/ddms/libs/ddmuilib/src/com/android/ddmuilib ; do
  $DIR $BACK/$i $DEST/
done

DEST=$BASE/icons
BACK=`back $DEST`

for i in debug-attach.png debug-wait.png debug-error.png device.png emulator.png \
         heap.png thread.png empty.png warning.png d.png e.png i.png \
         v.png w.png add.png delete.png edit.png save.png push.png pull.png \
         clear.png up.png down.png gc.png halt.png load.png importBug.png \
         play.png pause.png forward.png backward.png ; do
  $CMD $BACK/development/tools/ddms/libs/ddmuilib/src/resources/images/$i $DEST/
done

