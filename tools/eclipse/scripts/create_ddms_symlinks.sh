#!/bin/bash
#----------------------------------------------------------------------------|
# Creates the links to use ddm{ui}lib in the eclipse-ide plugin.
# Run this from device/tools/eclipse/scripts
#----------------------------------------------------------------------------|

set -e

HOST=`uname`
if [ "${HOST:0:6}" == "CYGWIN" ]; then
    # We can't use symlinks under Cygwin

    function cpfile { # $1=dest $2=source
        cp -fv $2 $1/
    }

    function cpdir() { # $1=dest $2=source
        rsync -avW --delete-after $2 $1
    }

else
    # For all other systems which support symlinks

    # computes the "reverse" path, e.g. "a/b/c" => "../../.."
    function back() {
        echo $1 | sed 's@[^/]*@..@g'
    }

    function cpfile { # $1=dest $2=source
        ln -svf `back $1`/$2 $1/
    }

    function cpdir() { # $1=dest $2=source
        ln -svf `back $1`/$2 $1
    }
fi

# CD to the top android directory
D=`dirname "$0"`
cd "$D/../../../../"


BASE="development/tools/eclipse/plugins/com.android.ide.eclipse.ddms"

DEST=$BASE/libs
mkdir -p $DEST
for i in prebuilt/common/jfreechart/*.jar; do
  cpfile $DEST $i
done

DEST=$BASE/src/com/android
mkdir -p $DEST
for i in development/tools/ddms/libs/ddmlib/src/com/android/ddmlib \
         development/tools/ddms/libs/ddmuilib/src/com/android/ddmuilib ; do
  cpdir $DEST $i
done

DEST=$BASE/icons
mkdir -p $DEST
for i in \
    add.png \
    backward.png \
    clear.png \
    d.png debug-attach.png debug-error.png debug-wait.png delete.png device.png down.png \
    e.png edit.png empty.png emulator.png \
    forward.png \
    gc.png \
    heap.png halt.png \
    i.png importBug.png \
    load.png \
    pause.png play.png pull.png push.png \
    save.png \
    thread.png \
    up.png \
    v.png \
    w.png warning.png ; do
  cpfile $DEST development/tools/ddms/libs/ddmuilib/src/resources/images/$i
done

