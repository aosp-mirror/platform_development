#!/bin/bash

function replace()
{
        echo replacing $1
        rm $V -rf "$UNZIPPED_BASE_DIR"/$1
        cp $V -rf "$UNZIPPED_IMAGE_DIR"/$1 "$UNZIPPED_BASE_DIR"/$1
}

V=""
Q="-q"
if [ "$1" == "-v" ]; then
    V="-v"
    Q=""
    shift
fi

NOZIP=""
if [ "$1" == "-nozip" ]; then
    NOZIP="1"
    shift
fi

BASE="$1"
IMAGES="$2"
OUTPUT="$3"

if [[ -z "$BASE" || -z "$IMAGES" || -z "$OUTPUT" ]] ; then
    echo "usage: combine_sdks.sh [-v] [-nozip] BASE IMAGES OUTPUT"
    echo
    echo "  BASE and IMAGES should be sdk zip files.  The system image files,"
    echo "  emulator and other runtime files will be copied from IMAGES and"
    echo "  everything else will be copied from BASE.  All of this will be"
    echo "  bundled into OUTPUT and zipped up again (unless -nozip is specified)."
    echo
    exit 1
fi

TMP=$(mktemp -d)

TMP_ZIP=tmp.zip

# determine executable extension
case `uname -s` in
    *_NT-*)  # for Windows
        EXE=.exe
        ;;
    *)
        EXE=
        ;;
esac

BASE_DIR="$TMP"/base
IMAGES_DIR="$TMP"/images
OUTPUT_TMP_ZIP="$BASE_DIR/$TMP_ZIP"

unzip $Q "$BASE"   -d "$BASE_DIR"
unzip $Q "$IMAGES" -d "$IMAGES_DIR"

UNZIPPED_BASE_DIR=$(echo "$BASE_DIR"/*)
UNZIPPED_IMAGE_DIR=$(echo "$IMAGES_DIR"/*)

#
# The commands to copy over the files that we want
#

# replace tools/emulator # at this time we do not want the exe from SDK1.x
replace tools/lib/images
replace tools/lib/res
replace tools/lib/fonts
replace tools/lib/layoutlib.jar
replace docs
replace android.jar

for i in widgets categories broadcast_actions service_actions; do
    replace tools/lib/$i.txt
done

if [ -d "$UNZIPPED_BASE_DIR"/usb_driver ]; then
    replace usb_driver
fi

#
# end
#

if [ -z "$NOZIP" ]; then
    pushd "$BASE_DIR" &> /dev/null
        # rename the directory to the leaf minus the .zip of OUTPUT
        LEAF=$(echo "$OUTPUT" | sed -e "s:.*\.zip/::" | sed -e "s:.zip$::")
        mv * "$LEAF"
        # zip it
        zip $V -qr "$TMP_ZIP" "$LEAF"
    popd &> /dev/null

    cp $V "$OUTPUT_TMP_ZIP" "$OUTPUT"
    echo "Combined SDK available at $OUTPUT"
else
    OUT_DIR="${OUTPUT//.zip/}"
    mv $V "$BASE_DIR"/* "$OUT_DIR"
    echo "Unzipped combined SDK available at $OUT_DIR"
fi

rm $V -rf "$TMP"

