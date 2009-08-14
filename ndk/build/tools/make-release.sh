#!/bin/sh
#
# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# This script is used to build complete Android NDK release packages
# from the git repository and a set of prebuilt cross-toolchain tarballs
#

# location of the root ndk directory. we assume this script is under build/tools
NDK_ROOT_DIR=`dirname $0`/../..
NDK_ROOT_DIR=`cd $NDK_ROOT_DIR && pwd`

# the release name
RELEASE=1.6_r1

# the package prefix
PREFIX=android-ndk

# the directory containing the prebuilt toolchain tarballs
PREBUILT_DIR=

# the prefix of prebuilt toolchain tarballs in $PREBUILT_DIR
PREBUILT_PREFIX=android-ndk-prebuilt-20090323

# the list of supported host development systems
PREBUILT_SYSTEMS="linux-x86 darwin-x86 windows"

OPTION_HELP=no

for opt do
  optarg=`expr "x$opt" : 'x[^=]*=\(.*\)'`
  case "$opt" in
  --help|-h|-\?) OPTION_HELP=yes
  ;;
  --verbose)
    if [ "$VERBOSE" = "yes" ] ; then
        VERBOSE2=yes
    else
        VERBOSE=yes
    fi
  ;;
  --release=*) RELEASE=$optarg
  ;;
  --prefix=*) PREFIX=$optarg
  ;;
  --prebuilt-prefix=*) PREBUILT_PREFIX=$optarg
  ;;
  --prebuilt-path=*) PREBUILT_DIR=$optarg
  ;;
  --systems=*) PREBUILT_SYSTEMS=$optarg
  ;;
  *)
    echo "unknown option '$opt', use --help"
    exit 1
  esac
done

if [ $OPTION_HELP = yes ] ; then
    echo "Usage: make-release.sh [options]"
    echo ""
    echo "Package a new set of release packages for the Android NDK."
    echo "You will need to specify the path of a directory containing"
    echo "prebuilt toolchain tarballs with the --prebuilt-path option."
    echo ""
    echo "Options: [defaults in brackets after descriptions]"
    echo ""
    echo "  --help                    Print this help message"
    echo "  --prefix=PREFIX           Package prefix name [$PREFIX]"
    echo "  --release=NAME            Specify release name [$RELEASE]"
    echo "  --systems=SYSTEMS         List of host system packages [$PREBUILT_SYSTEMS]"
    echo "  --prebuilt-path=PATH      Location of prebuilt binary tarballs [$PREBUILT_DIR]"
    echo "  --prebuilt-prefix=PREFIX  Prefix of prebuilt binary tarballs [$PREBUILT_PREFIX]"
    echo ""
    exit 1
fi

# Check the prebuilt path
#
if [ -z "$PREBUILT_DIR" ] ; then
    echo "ERROR: You must use --prebuilt-path=PATH to specify the path of prebuilt binary tarballs."
    exit 1
fi

if [ ! -d "$PREBUILT_DIR" ] ; then
    echo "ERROR: the --prebuilt-path argument is not a directory path: $PREBUILT_DIR"
    exit 1
fi

# Check the systems
#
if [ -z "$PREBUILT_SYSTEMS" ] ; then
    echo "ERROR: Your systems list is empty, use --system=LIST to specify a different one."
    exit 1
fi

if [ -z "$PREBUILT_PREFIX" ] ; then
    echo "ERROR: Your prebuilt prefix is empty; use --prebuilt-prefix=PREFIX."
    exit 1
fi

for SYS in $PREBUILT_SYSTEMS; do
    if [ ! -f $PREBUILT_DIR/$PREBUILT_PREFIX-$SYS.tar.bz2 ] ; then
        echo "ERROR: It seems there is no prebuilt binary tarball for the '$SYS' system"
        echo "Please check the content of $PREBUILT_DIR for a file named $PREBUILT_PREFIX-$SYS.tar.bz2."
        exit 1
    fi
done

# The list of git files to copy into the archives
GIT_FILES=`cd $NDK_ROOT_DIR && git ls-files`

# temporary directory used for packaging
TMPDIR=/tmp/ndk-release

RELEASE_PREFIX=$PREFIX-$RELEASE

rm -rf $TMPDIR && mkdir -p $TMPDIR

# first create the reference ndk directory from the git reference
echo "Creating reference from git files"
REFERENCE=$TMPDIR/reference &&
mkdir -p $REFERENCE &&
(cd $NDK_ROOT_DIR && tar cf - $GIT_FILES) | (cd $REFERENCE && tar xf -) &&
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

    ARCHIVE=$BIN_RELEASE.zip
    echo "Creating $ARCHIVE"
    (cd $TMPDIR && zip -9qr $ARCHIVE $RELEASE_PREFIX && rm -rf $DSTDIR) 2>/dev/null 1>&2
    if [ $? != 0 ] ; then
        echo "Could not create zip archive. Aborting."
        exit 1
    fi

    chmod a+r $TMPDIR/$ARCHIVE
done

echo "Cleaning up."
rm -rf $TMPDIR/reference

echo "Done, please see packages in $TMPDIR:"
ls -l $TMPDIR
