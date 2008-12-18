#!/bin/bash

function replace()
{
    echo replacing $1
    rm -rf $UNZIPPED_BASE_DIR/$1
    cp -rf $UNZIPPED_IMAGE_DIR/$1 $UNZIPPED_BASE_DIR/$1
}

BASE=$1
IMAGES=$2
OUTPUT=$3

if [[ -z $BASE || -z $IMAGES || -z $OUTPUT ]] ; then
    echo "usage: combine_sdks.sh BASE IMAGES OUTPUT"
    echo
    echo "  BASE and IMAGES should be sdk zip files.  The system image files,"
    echo "  emulator and other runtime files will be copied from IMAGES and"
    echo "  everything else will be copied from BASE.  All of this will be"
    echo "  bundled into OUTPUT and zipped up again."
    echo
    exit 1
fi

TMP=$(mktemp -d)

TMP_ZIP=tmp.zip

BASE_DIR=$TMP/base
IMAGES_DIR=$TMP/images
OUTPUT_TMP_ZIP=$BASE_DIR/$TMP_ZIP

unzip -q $BASE -d $BASE_DIR
unzip -q $IMAGES -d $IMAGES_DIR

UNZIPPED_BASE_DIR=$(echo $BASE_DIR/*)
UNZIPPED_IMAGE_DIR=$(echo $IMAGES_DIR/*)

#
# The commands to copy over the files that we want
#

# replace tools/emulator # at this time we do not want the exe from SDK1.x
replace tools/lib/images
replace docs
replace android.jar

#
# end
#

pushd $BASE_DIR &> /dev/null
    # rename the directory to the leaf minus the .zip of OUTPUT
    LEAF=$(echo $OUTPUT | sed -e "s:.*\.zip/::" | sed -e "s:.zip$::")
    mv * $LEAF
    # zip it
    zip -qr $TMP_ZIP $LEAF
popd &> /dev/null

cp $OUTPUT_TMP_ZIP $OUTPUT

rm -rf $TMP
