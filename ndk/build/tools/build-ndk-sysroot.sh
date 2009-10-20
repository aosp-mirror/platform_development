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
# build-ndk-sysroot.sh
#
# collect files from an Android tree to assemble a sysroot suitable for
# building a standable toolchain.
#
# after that, you may use build/tools/package-ndk-sysroot.sh to package
# the resulting files for distribution.
#
# NOTE: this is different from the Android toolchain original build-sysroot.sh
#       script because we place target files differently.
#
# WARNING: For now, only a single target ABI/Architecture us supported
#

. `dirname $0`/../core/ndk-common.sh

# PLATFORM is the name of the current Android system platform
PLATFORM=android-3

# ABI is the target ABI name for the NDK
ABI=arm

# ARCH is the target ABI name in the Android sources
ARCH=arm

OPTION_HELP=no
OPTION_BUILD_OUT=
OPTION_PLATFORM=
OPTION_PACKAGE=no
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
  --platform=*)
    OPTION_PLATFORM=$optarg
    ;;
  --build-out=*)
    OPTION_BUILD_OUT=$optarg
    ;;
  --package)
    OPTION_PACKAGE=yes
    ;;
  *)
    echo "unknown option '$opt', use --help"
    exit 1
  esac
done

if [ $OPTION_HELP = "yes" ] ; then
    echo "Collect files from an Android build tree and assembles a sysroot"
    echo "suitable for building a standalone toolchain or be used by the"
    echo "Android NDK."
    echo ""
    echo "options:"
    echo ""
    echo "  --help             print this message"
    echo "  --verbose          enable verbose messages"
    echo "  --platform=<name>  generate sysroot for platform <name> (default is $PLATFORM)"
    echo "  --build-out=<path> set Android build out directory"
    echo "  --package          generate sysroot package tarball"
    echo ""
    exit 0
fi

if [ -n "$OPTION_PLATFORM" ] ; then
    PLATFORM=$OPTION_PLATFORM
fi

# Get the root of the NDK from the current program location
NDK_ROOT=`dirname $0`
NDK_ROOT=`dirname $NDK_ROOT`
NDK_ROOT=`dirname $NDK_ROOT`

# Get the Android out directory
if [ -z "$OPTION_BUILD_OUT" ] ; then
    if [ -z "$ANDROID_PRODUCT_OUT" ] ; then
        echo "ANDROID_PRODUCT_OUT is not defined in your environment. Aborting"
        exit 1
    fi
    if [ ! -d $ANDROID_PRODUCT_OUT ] ; then
        echo "ANDROID_PRODUCT_OUT does not point to a valid directory. Aborting"
        exit 1
    fi
else
    ANDROID_PRODUCT_OUT=$OPTION_BUILD_OUT
    if [ ! -d $ANDROID_PRODUCT_OUT ] ; then
        echo "The build out path is not a valid directory: $OPTION_BUILD_OUT"
        exit 1
    fi
fi

PRODUCT_DIR=$ANDROID_PRODUCT_OUT
SYSROOT=$NDK_ROOT/build/platforms/$PLATFORM/arch-$ABI
COMMON_ROOT=$NDK_ROOT/build/platforms/$PLATFORM/common

# clean up everything in existing sysroot
rm -rf $SYSROOT
mkdir -p $SYSROOT

rm -rf $COMMON_ROOT
mkdir -p $COMMON_ROOT

LIB_ROOT=$SYSROOT/usr/lib
INCLUDE_ROOT=$SYSROOT/usr/include

install_file ()
{
    mkdir -p $2/`dirname $1`
    cp -fp $1 $2/$1
}

install_helper ()
{
  (cd $1 && find . -type f | while read ff; do install_file $ff $2; done)
}

TOP=$PRODUCT_DIR/../../../..

# CRT objects that need to be copied
CRT_OBJS_DIR=$PRODUCT_DIR/obj/lib
CRT_OBJS="$CRT_OBJS_DIR/crtbegin_static.o \
$CRT_OBJS_DIR/crtbegin_dynamic.o \
$CRT_OBJS_DIR/crtend_android.o"

# static libraries that need to be copied.
STATIC_LIBS_DIR=$PRODUCT_DIR/obj/STATIC_LIBRARIES
STATIC_LIBS="$STATIC_LIBS_DIR/libc_intermediates/libc.a \
$STATIC_LIBS_DIR/libm_intermediates/libm.a \
$STATIC_LIBS_DIR/libstdc++_intermediates/libstdc++.a
$STATIC_LIBS_DIR/libthread_db_intermediates/libthread_db.a"

# dynamic libraries that need to be copied.
DYNAMIC_LIBS_DIR=$PRODUCT_DIR/symbols/system/lib
DYNAMIC_LIBS="$DYNAMIC_LIBS_DIR/libdl.so \
$DYNAMIC_LIBS_DIR/libc.so \
$DYNAMIC_LIBS_DIR/libm.so \
$DYNAMIC_LIBS_DIR/libstdc++.so \
$DYNAMIC_LIBS_DIR/libthread_db.so"

# Copy all CRT objects and libraries
rm -rf $LIB_ROOT
mkdir -p $LIB_ROOT
cp -f $CRT_OBJS $STATIC_LIBS $DYNAMIC_LIBS $LIB_ROOT

# Check $TOP/bionic to see if this is new source layout.
if [ -d $TOP/bionic ] ;then
  BIONIC_ROOT=$TOP/bionic
  LIBC_ROOT=$BIONIC_ROOT/libc
else
  BIONIC_ROOT=$TOP/system
  LIBC_ROOT=$BIONIC_ROOT/bionic
fi

# Copy headers.  This need to be done in the reverse order of inclusion
# in case there are different headers with the same name.
ARCH_INCLUDE=$SYSROOT/usr/include
rm -rf $ARCH_INCLUDE
mkdir -p $ARCH_INCLUDE

COMMON_INCLUDE=$COMMON_ROOT/include
rm -rf $COMMON_INCLUDE
mkdir -p $COMMON_INCLUDE

# Install a common header and create the appropriate arch-specific
# directory for it.
#
# $1: source directory
# $2: header path, relative to source directory
#
common_header ()
{
    echo "Copy: $COMMON_INCLUDE/$2"
    mkdir -p `dirname $COMMON_INCLUDE/$2`
    install $1/$2 $COMMON_INCLUDE/$2
    # just to be safe
    chmod a-x $COMMON_INCLUDE/$2

    # the link prefix, used to point to common/
    # from arch-$ARCH/usr/
    link_prefix=../../common/include

    # we need to count the number of directory separators in $2
    # for each one of them, we're going to prepend ../ to the
    # link prefix
    for item in `echo $2 | tr '/' ' '`; do
        link_prefix=../$link_prefix
    done

    echo "Link: $ARCH_INCLUDE/$2"
    mkdir -p `dirname $ARCH_INCLUDE/$2`
    ln -s $link_prefix/$2 $ARCH_INCLUDE/$2
}

common_headers ()
{
    srcs=`cd $1 && find . -type f`
    # remove leading ./
    srcs=`echo $srcs | sed -e "s%\./%%g"`

    for src in $srcs; do
        common_header $1 $src
    done
}

arch_header ()
{
    echo "Copy: $ARCH_INCLUDE/$2"
    mkdir -p `dirname $ARCH_INCLUDE/$2`
    install $1/$2 $ARCH_INCLUDE/$2
    # just to be safe
    chmod a-x $ARCH_INCLUDE/$2
}

arch_headers ()
{
    srcs=`cd $1 && find . -type f`
    # remove leading ./
    srcs=`echo $srcs | sed -e "s%\./%%g"`

    for src in $srcs; do
        arch_header $1 $src
    done
}

# ZLib headers
common_header  $TOP/external/zlib zlib.h
common_header  $TOP/external/zlib zconf.h

# Jni header
common_header  $TOP/dalvik/libnativehelper/include/nativehelper jni.h

# libthread_db headers, not sure if this is needed for the NDK
common_headers $BIONIC_ROOT/libthread_db/include

# for libm, just copy math.h and fenv.h
common_header $BIONIC_ROOT/libm/include math.h
arch_header   $BIONIC_ROOT/libm/include $ARCH/fenv.h

# our tiny C++ standard library
common_headers $BIONIC_ROOT/libstdc++/include

# C library kernel headers
common_headers $LIBC_ROOT/kernel/common
arch_headers   $LIBC_ROOT/kernel/arch-arm

# C library headers
common_headers $LIBC_ROOT/include
arch_headers   $LIBC_ROOT/arch-$ARCH/include

# Do we need to package the result
if [ $OPTION_PACKAGE = yes ] ; then
    DATE=`date +%Y%m%d`
    PKGFILE=/tmp/android-ndk-sysroot-$DATE.tar.bz2
    tar cjf $PKGFILE build/platforms/$PLATFORM
    echo "Packaged in $PKGFILE"
fi
