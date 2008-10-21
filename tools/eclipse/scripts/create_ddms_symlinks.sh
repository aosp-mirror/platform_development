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

cd ../plugins/com.android.ide.eclipse.ddms
mkdir -p libs
cd libs
$CMD ../../../../../prebuilt/common/jfreechart/jcommon-1.0.12.jar .
$CMD ../../../../../prebuilt/common/jfreechart/jfreechart-1.0.9.jar .
$CMD ../../../../../prebuilt/common/jfreechart/jfreechart-1.0.9-swt.jar .

cd ../src/com/android
$DIR ../../../../../../ddms/libs/ddmlib/src/com/android/ddmlib .
$DIR ../../../../../../ddms/libs/ddmuilib/src/com/android/ddmuilib .

# goes back to the icons directory
cd ../../../icons/
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/debug-attach.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/debug-wait.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/debug-error.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/device.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/emulator.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/heap.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/thread.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/empty.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/warning.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/d.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/e.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/i.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/v.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/w.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/add.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/delete.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/edit.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/save.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/push.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/pull.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/clear.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/up.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/down.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/gc.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/halt.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/load.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/importBug.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/play.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/pause.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/forward.png .
$CMD ../../../../ddms/libs/ddmuilib/src/resources/images/backward.png .



