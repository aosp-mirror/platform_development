#!/bin/sh
#
# This script is used to build complete Android NDK preview packages
# from the git repository and a set of prebuilt cross-toolchain tarballs
#

TMPDIR=/tmp/ndk-preview
GIT_REFERENCE=/home/digit/android/ndk.git
PREBUILT_PREFIX=/home/digit/android/ndk/android-ndk-prebuilt-20090323
PREVIEW_PREFIX=android-ndk-preview-`date "+%Y%m%d"`
PREBUILT_SYSTEMS="linux-x86 linux-x86_64 darwin-x86 windows"

rm -rf $TMPDIR && mkdir -p $TMPDIR

# first create the reference ndk directory from the git reference
git clone $GIT_REFERENCE $TMPDIR/reference
if [ $? != 0 ] ; then
    echo "Could not clone git reference. Aborting."
    exit 2
fi
# get rid of .git directory
rm -rf $TMPDIR/reference/.git

# now, for each system, create a preview package
#
for SYSTEM in $PREBUILT_SYSTEMS; do
    echo "Preparing package for system $SYSTEM."
    PREVIEW=$PREVIEW_PREFIX-$SYSTEM
    PREBUILT=$PREBUILT_PREFIX-$SYSTEM
    DSTDIR=$TMPDIR/$PREVIEW
    rm -rf $DSTDIR && mkdir -p $DSTDIR &&
    cp -rp $TMPDIR/reference/* $DSTDIR
    if [ $? != 0 ] ; then
        echo "Could not copy reference. Aborting."
        exit 2
    fi
    echo "Unpacking $PREBUILT.tar.bz2"
    (cd $DSTDIR && tar xjf $PREBUILT.tar.bz2)
    if [ $? != 0 ] ; then
        echo "Could not unpack prebuilt for system $SYSTEM. Aborting."
        exit 1
    fi
    echo "Creating $PREVIEW.tar.bz2"
    (cd $TMPDIR && tar cjf $PREVIEW.tar.bz2 $PREVIEW && rm -rf $DSTDIR)
    if [ $? != 0 ] ; then
        echo "Could not create archive. Aborting."
        exit 1
    fi
done

echo "Cleaning up."
rm -rf $TMPDIR/reference

echo "Done, please see packages in $TMPDIR:"
ls -l $TMPDIR

