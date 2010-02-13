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

. $NDK_ROOT_DIR/build/core/ndk-common.sh
force_32bit_binaries

# the default release name (use today's date)
RELEASE=`date +%Y%m%d`

# the package prefix
PREFIX=android-ndk

# the prefix of prebuilt toolchain tarballs
PREBUILT_PREFIX=

# the list of supported host development systems
PREBUILT_SYSTEMS="linux-x86 darwin-x86 windows"

# a prebuilt NDK archive (.zip file). empty means don't use any
PREBUILT_NDK=

# set to 'yes' if we should use 'git ls-files' to list the files to
# be copied into the archive.
USE_GIT_FILES=yes

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
  --prebuilt-ndk=*) PREBUILT_NDK=$optarg
  ;;
  --prebuilt-prefix=*) PREBUILT_PREFIX=$optarg
  ;;
  --systems=*) PREBUILT_SYSTEMS=$optarg
  ;;
  --no-git) USE_GIT_FILES=no
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
    echo ""
    echo "You will need to have generated one or more prebuilt toolchain tarballs"
    echo "with the build/tools/build-toolchain.sh script. These files should be"
    echo "named like <prefix>-<system>.tar.bz2, where <prefix> is an arbitrary"
    echo "prefix and <system> is one of: $PREBUILT_SYSTEMS"
    echo ""
    echo "Use the --prebuilt-prefix=<path>/<prefix> option to build release"
    echo "packages from these tarballs."
    echo ""
    echo "Alternatively, you can use --prebuilt-ndk=<file> where <file> is the"
    echo "path to a previous official NDK release package. It will be used to"
    echo "extract the toolchain binaries and copy them to your new release."
    echo "Only use this for experimental release packages !"
    echo ""
    echo "The generated release packages will be stored in a temporary directory"
    echo "that will be printed at the end of the generation process."
    echo ""
    echo "Options: [defaults in brackets after descriptions]"
    echo ""
    echo "  --help                    Print this help message"
    echo "  --prefix=PREFIX           Release package prefix name [$PREFIX]"
    echo "  --release=NAME            Specify release name [$RELEASE]"
    echo "  --prebuilt-prefix=PREFIX  Prefix of prebuilt binary tarballs [$PREBUILT_PREFIX]"
    echo "  --prebuilt-ndk=FILE       Specify a previous NDK package [$PREBUILT_NDK]"
    echo "  --systems=SYSTEMS         List of host system packages [$PREBUILT_SYSTEMS]"
    echo "  --no-git                  Don't use git to list input files, take all of them."
    echo ""
    exit 1
fi

# Check the prebuilt path
#
if [ -n "$PREBUILD_NDK" -a -n "$PREBUILT_PREFIX" ] ; then
    echo "ERROR: You cannot use both --prebuilt-ndk and --prebuilt-prefix at the same time."
    exit 1
fi

if [ -z "$PREBUILT_PREFIX" -a -z "$PREBUILT_NDK" ] ; then
    echo "ERROR: You must use one of --prebuilt-prefix or --prebuilt-ndk. See --help for details."
    exit 1
fi

if [ -n "$PREBUILT_PREFIX" ] ; then
    if [ -d "$PREBUILT_PREFIX" ] ; then
        echo "ERROR: the --prebuilt-prefix argument must not be a direct directory path: $PREBUILT_PREFIX."
        exit 1
    fi
    PREBUILT_DIR=`dirname $PREBUILT_PREFIX`
    if [ ! -d "$PREBUILT_DIR" ] ; then
        echo "ERROR: the --prebuilt-prefix argument does not point to a directory: $PREBUILT_DIR"
        exit 1
    fi
    if [ -z "$PREBUILT_SYSTEMS" ] ; then
        echo "ERROR: Your systems list is empty, use --system=LIST to specify a different one."
        exit 1
    fi
    # Check the systems
    #
    for SYS in $PREBUILT_SYSTEMS; do
        if [ ! -f $PREBUILT_PREFIX-$SYS.tar.bz2 ] ; then
            echo "ERROR: It seems there is no prebuilt binary tarball for the '$SYS' system"
            echo "Please check the content of $PREBUILT_DIR for a file named $PREBUILT_PREFIX-$SYS.tar.bz2."
            exit 1
        fi
    done
else
    if [ ! -f "$PREBUILT_NDK" ] ; then
        echo "ERROR: the --prebuilt-ndk argument is not a file: $PREBUILT_NDK"
        exit 1
    fi
    # Check that the name ends with the proper host tag
    HOST_NDK_SUFFIX="$HOST_TAG.zip"
    echo "$PREBUILT_NDK" | grep -q "$HOST_NDK_SUFFIX"
    if [ $? != 0 ] ; then
        echo "ERROR: the name of the prebuilt NDK must end in $HOST_NDK_SUFFIX"
        exit 1
    fi
    PREBUILT_SYSTEMS=$HOST_TAG
fi

# The list of git files to copy into the archives
if [ "$USE_GIT_FILES" = "yes" ] ; then
    echo "Collecting sources from git (use --no-git to copy all files instead)."
    GIT_FILES=`cd $NDK_ROOT_DIR && git ls-files`
else
    echo "Collecting all sources files under tree."
    # Cleanup everything that is likely to not be part of the final NDK
    # i.e. generated files...
    rm -rf $NDK_ROOT_DIR/out
    rm -rf $NDK_ROOT_DIR/apps/*/project/libs/armeabi
    rm -rf $NDK_ROOT_DIR/apps/*/project/libs/armeabi-v7a
    # Get all files under the NDK root
    GIT_FILES=`cd $NDK_ROOT_DIR && find .`
    GIT_FILES=`echo $GIT_FILES | sed -e "s!\./!!g"`
fi

# temporary directory used for packaging
TMPDIR=/tmp/ndk-release

RELEASE_PREFIX=$PREFIX-$RELEASE

rm -rf $TMPDIR && mkdir -p $TMPDIR

# first create the reference ndk directory from the git reference
echo "Creating reference from source files"
REFERENCE=$TMPDIR/reference &&
mkdir -p $REFERENCE &&
(cd $NDK_ROOT_DIR && tar cf - $GIT_FILES) | (cd $REFERENCE && tar xf -) &&
rm -f $REFERENCE/Android.mk
if [ $? != 0 ] ; then
    echo "Could not create reference. Aborting."
    exit 2
fi

# now, for each system, create a package
#
for SYSTEM in $PREBUILT_SYSTEMS; do
    echo "Preparing package for system $SYSTEM."
    BIN_RELEASE=$RELEASE_PREFIX-$SYSTEM
    PREBUILT=$PREBUILT_PREFIX-$SYSTEM
    DSTDIR=$TMPDIR/$RELEASE_PREFIX
    rm -rf $DSTDIR && mkdir -p $DSTDIR &&
    cp -rp $REFERENCE/* $DSTDIR
    if [ $? != 0 ] ; then
        echo "Could not copy reference. Aborting."
        exit 2
    fi

    if [ -n "$PREBUILT_NDK" ] ; then
        echo "Unpacking prebuilt toolchain from $PREBUILT_NDK"
        UNZIP_DIR=$TMPDIR/prev-ndk
        rm -rf $UNZIP_DIR && mkdir -p $UNZIP_DIR
        if [ $? != 0 ] ; then
            echo "Could not create temporary directory: $UNZIP_DIR"
            exit 1
        fi
        cd $UNZIP_DIR && unzip -q $PREBUILT_NDK 1>/dev/null 2>&1
        if [ $? != 0 ] ; then
            echo "ERROR: Could not unzip NDK package $PREBUILT_NDK"
            exit 1
        fi
        cd android-ndk-* && cp -rP build/prebuilt $DSTDIR/build
    else
        echo "Unpacking $PREBUILT.tar.bz2"
        (cd $DSTDIR && tar xjf $PREBUILT.tar.bz2) 2>/dev/null 1>&2
        if [ $? != 0 ] ; then
            echo "Could not unpack prebuilt for system $SYSTEM. Aborting."
            exit 1
        fi
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
rm -rf $TMPDIR/prev-ndk

echo "Done, please see packages in $TMPDIR:"
ls -l $TMPDIR
