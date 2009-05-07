#!/bin/sh
#
# This script is used to build complete Android NDK release packages
# from the git repository and a set of prebuilt cross-toolchain tarballs
#

# location of the root ndk directory. we assume this script is under build/tools
NDK_ROOT_DIR=`dirname $0`/../..
NDK_ROOT_DIR=`cd $NDK_ROOT_DIR && pwd`

# the release name
RELEASE=1.5_r1

# the directory containing the prebuilt toolchain tarballs
PREBUILT_DIR=/home/digit/android/ndk

# the prefix of prebuilt toolchain tarballs in $PREBUILT_DIR
PREBUILT_PREFIX=android-ndk-prebuilt-20090323

# the list of supported host development systems
PREBUILT_SYSTEMS="linux-x86 linux-x86_64 darwin-x86 windows"

# the list of git files to copy into the archives
GIT_FILES=`cd $NDK_ROOT_DIR && git ls-files`

# temporary directory used for packaging
TMPDIR=/tmp/ndk-release

RELEASE_PREFIX=android-ndk-$RELEASE

rm -rf $TMPDIR && mkdir -p $TMPDIR

# first create the reference ndk directory from the git reference
echo "Creating reference from git files"
REFERENCE=$TMPDIR/reference &&
mkdir -p $REFERENCE &&
(for ff in $GIT_FILES; do
  mkdir -p $REFERENCE/`dirname $ff` && cp -pf $NDK_ROOT_DIR/$ff $REFERENCE/$ff;
done) &&
rm -f $REFERENCE/Android.mk
if [ $? != 0 ] ; then
    echo "Could not create git reference. Aborting."
    exit 2
fi

# now, for each system, create a preview package
#
for SYSTEM in $PREBUILT_SYSTEMS; do
    echo "Preparing package for system $SYSTEM."
    BIN_RELEASE=$RELEASE_PREFIX-$SYSTEM
    PREBUILT=$PREBUILT_DIR/$PREBUILT_PREFIX-$SYSTEM
    DSTDIR=$TMPDIR/$RELEASE_PREFIX
    rm -rf $DSTDIR && mkdir -p $DSTDIR &&
    cp -rp $REFERENCE/* $DSTDIR
    if [ $? != 0 ] ; then
        echo "Could not copy reference. Aborting."
        exit 2
    fi

    echo "Unpacking $PREBUILT.tar.bz2"
    (cd $DSTDIR && tar xjf $PREBUILT.tar.bz2) 2>/dev/null 1>&2
    if [ $? != 0 ] ; then
        echo "Could not unpack prebuilt for system $SYSTEM. Aborting."
        exit 1
    fi

    case $SYSTEM in
        # prefer zip format for windows and darwin
        windows|darwin-*)
            ARCHIVE=$BIN_RELEASE.zip
            echo "Creating $ARCHIVE"
            (cd $TMPDIR && zip -9qr $ARCHIVE $RELEASE_PREFIX && rm -rf $DSTDIR) 2>/dev/null 1>&2
            if [ $? != 0 ] ; then
                echo "Could not create zip archive. Aborting."
                exit 1
            fi
            ;;
        # or tar.bz2 for others
        *)
            ARCHIVE=$BIN_RELEASE.tar.bz2
            echo "Creating $ARCHIVE"
            (cd $TMPDIR && tar cjf $ARCHIVE $RELEASE_PREFIX && rm -rf $DSTDIR) 2>/dev/null 1>&2
            if [ $? != 0 ] ; then
                echo "Could not create archive. Aborting."
                exit 1
            fi
            ;;
    esac
    chmod a+r $TMPDIR/$ARCHIVE
done

echo "Cleaning up."
rm -rf $TMPDIR/reference

echo "Done, please see packages in $TMPDIR:"
ls -l $TMPDIR

