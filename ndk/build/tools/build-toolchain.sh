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
#  This shell script is used to rebuild the Android NDK's prebuilt binaries.
#
#  The source tarballs must be located in $ANDROID_NDK_ROOT/build/archive
#  They will be located in $ANDROID_NDK_ROOT/build/toolchain after compilation
#

# include common function and variable definitions
source `dirname $0`/../core/ndk-common.sh

# number of jobs to run in parallel when running make
JOBS=$HOST_NUM_CPUS

TOOLCHAIN_NAME=arm-eabi-4.2.1
PLATFORM=android-3
ABI=arm

OPTION_HELP=no
OPTION_PLATFORM=
OPTION_FORCE_32=no
OPTION_REBUILD=no

VERBOSE=no
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
  --toolchain=*)
    TOOLCHAIN_NAME=$optarg
    ;;
  --platform=*)
    PLATFORM=$optarg
    ;;
  --abi=*)
    ABI=$optarg
    ;;
  --force-download)
    OPTION_FORCE_DOWNLOAD=yes
    OPTION_FORCE_BUILD=yes
    ;;
  --force-build)
    OPTION_FORCE_BUILD=yes
    ;;
  --verbose)
    VERBOSE=yes
    ;;
  *)
    echo "unknown option '$opt', use --help"
    exit 1
  esac
done

if [ $OPTION_HELP = "yes" ] ; then
    echo "Rebuild the prebuilt binaries for the Android NDK toolchain."
    echo ""
    echo "options:"
    echo ""
    echo "  --help             print this message"
    echo "  --toolchain=<name> toolchain name (default is $TOOLCHAIN_NAME)"
    echo "  --platform=<name>  generate toolchain from platform <name> (default is $PLATFORM)"
    echo "  --abi=<name>       generate toolchain from abi <name> (default is $ABI)"
    echo "  --build-out=<path> set Android build out directory"
    echo "  --force-download   force a download and unpacking of the toolchain sources"
    echo "  --force-build      force a rebuild of the sources"
    echo ""
    exit 0
fi

# Force generation of 32-bit binaries on 64-bit systems
case $HOST_TAG in
    *-x86_64)
        HOST_CFLAGS="$HOST_CFLAGS -m32"
        HOST_LDFLAGS="$HOST_LDFLAGS -m32"
        force_32bit_binaries  # to modify HOST_TAG and others
        ;;
esac

TMPLOG=/tmp/android-toolchain-build-$$.log
rm -rf $TMPLOG

if [ $VERBOSE = yes ] ; then
    run ()
    {
        echo "##### NEW COMMAND"
        echo $@
        $@ 2>&1 | tee $TMPLOG
    }
else
    echo "To follow build long, please use in another terminal: tail -F $TMPLOG"
    run ()
    {
        echo "##### NEW COMMAND" >> $TMPLOG
        echo "$@" >> $TMPLOG
        $@ 1>$TMPLOG 2>&1
    }
fi

ANDROID_NDK_ROOT=`cd $ANDROID_NDK_ROOT && pwd`
ANDROID_NDK_ARCHIVE=$ANDROID_NDK_ROOT/build/toolchains/archive
ANDROID_PLATFORMS_ROOT=$ANDROID_NDK_ROOT/build/platforms

# where all generated files will be placed
OUT=$ANDROID_NDK_ROOT/out/$TOOLCHAIN_NAME
PACKAGE_OUT=$OUT/packages
TIMESTAMP_OUT=$OUT/timestamps

# where the sysroot is located
ANDROID_SYSROOT=$ANDROID_NDK_ROOT/build/platforms/$PLATFORM/arch-$ABI

# where the toolchain binaries will be placed
ANDROID_TOOLCHAIN_OUT=$OUT/toolchain
ANDROID_TOOLCHAIN_SRC=$ANDROID_TOOLCHAIN_OUT/src
ANDROID_TOOLCHAIN_BUILD=$ANDROID_TOOLCHAIN_OUT/build

# where the gdbserver binaries will be placed
ANDROID_GDBSERVER_OUT=$OUT/gdbserver
ANDROID_GDBSERVER_BUILD=$ANDROID_GDBSERVER_OUT/build
ANDROID_GDBSERVER_DEST=$ANDROID_SYSROOT/usr/bin

# Let's check that we have a working md5sum here
A_MD5=`echo "A" | md5sum | cut -d' ' -f1`
if [ "$A_MD5" != "bf072e9119077b4e76437a93986787ef" ] ; then
    echo "Please install md5sum on this machine"
    exit 2
fi

# And wget too
WGET=`which wget`
CURL=`which curl`
SCP=`which scp`

# download a file with either 'curl', 'wget' or 'scp'
# $1: source
# $2: target
download_file ()
{
    # is this HTTP, HTTPS or FTP ?
    echo $1 | grep -q -e "^\(http\|https\):.*"
    if [ $? = 0 ] ; then
        if [ -n "$WGET" ] ; then
            $WGET -O $2 $1 
        elif [ -n "$CURL" ] ; then
            $CURL -o $2 $1
        else
            echo "Please install wget or curl on this machine"
            exit 1
        fi
        return
    fi

    # is this SSH ?
    echo $1 | grep -q -e "^ssh:.*"
    if [ $? = 0 ] ; then
        if [ -n "$SCP" ] ; then
            scp_src=`echo $1 | sed -e s%ssh://%%g`
            $SCP $scp_src $2
        else
            echo "Please install scp on this machine"
            exit 1
        fi
        return
    fi

    echo $1 | grep -q -e "^/.*"
    if [ $? = 0 ] ; then
        cp -f $1 $2
    fi
}

TOOLCHAIN_SRC=$ANDROID_TOOLCHAIN_SRC
TOOLCHAIN_BUILD=$ANDROID_TOOLCHAIN_BUILD
TOOLCHAIN_PREFIX=$ANDROID_NDK_ROOT/build/prebuilt/$HOST_TAG/$TOOLCHAIN_NAME

GDBSERVER_BUILD=$ANDROID_GDBSERVER_BUILD

timestamp_check ()
{
    [ -f $TIMESTAMP_OUT/$1/timestamp-$2 ]
}

timestamp_set ()
{
    mkdir -p $TIMESTAMP_OUT/$1
    touch $TIMESTAMP_OUT/$1/timestamp-$2
}

timestamp_clear ()
{
    rm -f $TIMESTAMP_OUT/$1/timestamp-*
}

timestamp_force ()
{
    rm -f $TIMESTAMP_OUT/$1/timestamp-$2
}

# this function will be used to download and verify a toolchain
# package 
# $1: directory name under build/archive  (e.g. 'android-toolchain')
#
download_package ()
{
    WORKSPACE=$ANDROID_NDK_ARCHIVE/$1
    if [ ! -d $WORKSPACE ] ; then
        echo "No directory named $1 under $ANDROID_NDK_ARCHIVE"
        exit 2
    fi
    SOURCES=$WORKSPACE/sources.txt
    if [ ! -f $SOURCES ] ; then
        echo "Missing sources.txt in $WORKSPACE"
        exit 2
    fi
    # First line must be file name
    PKGNAME=`cat $SOURCES | sed 1q`
    # Second line must be md5sum
    PKGSUM=`cat $SOURCES | sed 1d | sed 1q`
    if [ -z "$PKGNAME" -o -z "$PKGSUM" ] ; then
        echo "Corrupted file: $SOURCES"
        exit 2
    fi

    # Try to download the package if it is not there
    # the Third line of sources.txt, and all others behind
    # must be wget urls or something.
    PACKAGE_TARBALL=$PACKAGE_OUT/$PKGNAME
    if [ ! -f $PACKAGE_TARBALL ] ; then
        cat $SOURCES | sed 1,2d | while read src; do
            echo $src | grep -q -e "^/.*"
            if [ $? = 0 ] ; then
                if [ -f $src ] ; then
                    echo "Copy    : $PKGNAME"
                    echo "          from `dirname $src`"
                    echo "          into $PACKAGE_TARBALL"
                    run cp -f $src $PACKAGE_TARBALL
                    if [ $? = 0 ] ; then
                        break
                    fi
                    echo "Copy    : Problem copying from $src"
                else
                    echo "Copy    : Can't find $src (skipping)"
                fi
                continue
            fi
            echo $src | grep -q -e "^\(http\|https\|ftp\|ssh\):.*"
            if [ $? = 0 ] ; then
                echo "Download: $PKGNAME"
                echo "          from $src"
                echo "          into $PACKAGE_TARBALL"
                download_file $src $PACKAGE_TARBALL
                if [ $? = 0 ] ; then
                    break
                fi
                continue
            else
                "Copy    : Unknown method in $src"
            fi
        done
        if [ ! -f $PACKAGE_TARBALL ] ; then
            echo "ERROR: Could not copy or download $PKGNAME !"
            echo "Your probably need to edit $WORKSPACE/sources.txt"
            exit 1
        fi
    fi

    if ! timestamp_check $1 verify ; then
        SUM=`md5sum $PACKAGE_TARBALL | cut -d " " -f 1`
        if [ "$SUM" != "$PKGSUM" ] ; then
            echo "ERROR: Invalid MD5 Sum for $PACKAGE_TARBALL"
            echo "    Expected $PKGSUM"
            echo "    Computed $SUM"
            echo "You might want to use the --force-download option."
            exit 2
        fi

        echo "Verified: $PACKAGE_TARBALL"
        timestamp_set   $1 verify
        timestamp_force $1 unpack
    fi
    eval PKG_$1=$PACKAGE_TARBALL
}

# Unpack a given package in a target location
# $1: package name
# $2: target directory
#
unpack_package ()
{
    WORKSPACE=$ANDROID_NDK_ARCHIVE/$1
    SRCDIR=$2
    SRCPKG=`var_value PKG_$1`
    if ! timestamp_check $1 unpack; then
        echo "Unpack  : $1 sources"
        echo "          from $SRCPKG"
        echo "          into $SRCDIR"
        rm -rf $SRCDIR
        mkdir -p $SRCDIR
        TARFLAGS=xjf
        if [ $VERBOSE = yes ]; then
          TARFLAGS="v$TARFLAGS"
        fi
        run tar $TARFLAGS $SRCPKG -C $SRCDIR
        if [ $? != 0 ] ; then
            echo "ERROR: Could not unpack $1, See $TMPLOG"
            exit 1
        fi
        timestamp_set   $1 unpack
        timestamp_force $1 configure
    fi
}

if [ $OPTION_FORCE_DOWNLOAD ] ; then
    rm -rf $PACKAGE_OUT $ANDROID_TOOLCHAIN_SRC
    timestamp_force toolchain unpack
    timestamp_force toolchain verify
fi

if [ $OPTION_FORCE_BUILD ] ; then
    rm -rf $ANDROID_TOOLCHAIN_BUILD
    timestamp_clear toolchain
    timestamp_clear gdbserver
fi

# checks, we need more checks..
mkdir -p $PACKAGE_OUT
if [ $? != 0 ] ; then
    echo "Can't create download/archive directory for toolchain tarballs"
    exit 2
fi

download_package toolchain
unpack_package   toolchain $ANDROID_TOOLCHAIN_SRC

# remove all info files from the unpacked toolchain sources
# they create countless little problems during the build
# if you don't have exactly the configuration expected by
# the scripts.
#
find $ANDROID_TOOLCHAIN_SRC -type f -a -name "*.info" -print0 | xargs -0 rm -f

# configure the toolchain
if ! timestamp_check toolchain configure; then
    echo "Configure: toolchain build"
    mkdir -p $TOOLCHAIN_BUILD &&
    cd $TOOLCHAIN_BUILD &&
    export CFLAGS="$HOST_CFLAGS" &&
    export LDFLAGS="$HOST_LDFLAGS" && run \
    $TOOLCHAIN_SRC/configure --target=arm-eabi \
                             --disable-nls \
                             --prefix=$TOOLCHAIN_PREFIX \
                             --with-sysroot=$ANDROID_SYSROOT

    if [ $? != 0 ] ; then
        echo "Error while trying to configure toolchain build. See $TMPLOG"
        exit 1
    fi
    timestamp_set   toolchain configure
    timestamp_force toolchain build
fi

# build the toolchain
if ! timestamp_check toolchain build ; then
    echo "Building : toolchain [this can take a long time]."
    cd $TOOLCHAIN_BUILD &&
    export CFLAGS="$HOST_CFLAGS" &&
    export LDFLAGS="$HOST_LDFLAGS" &&
    run make -j$JOBS
    if [ $? != 0 ] ; then
        echo "Error while building toolchain. See $TMPLOG"
        exit 1
    fi
    timestamp_set   toolchain build
    timestamp_force toolchain install
fi

# install the toolchain to its final location
if ! timestamp_check toolchain install ; then
    echo "Install  : toolchain binaries."
    cd $TOOLCHAIN_BUILD &&
    run make install
    if [ $? != 0 ] ; then
        echo "Error while installing toolchain. See $TMPLOG"
        exit 1
    fi
    # don't forget to copy the GPL and LGPL license files
    cp -f $TOOLCHAIN_SRC/COPYING $TOOLCHAIN_SRC/COPYING.LIB $TOOLCHAIN_PREFIX
    # remove some unneeded files
    rm -f $TOOLCHAIN_PREFIX/bin/*-gccbug
    rm -rf $TOOLCHAIN_PREFIX/man $TOOLCHAIN_PREFIX/info
    # strip binaries to reduce final package size
    strip $TOOLCHAIN_PREFIX/bin/*
    strip $TOOLCHAIN_PREFIX/arm-eabi/bin/*
    strip $TOOLCHAIN_PREFIX/libexec/gcc/*/*/cc1
    strip $TOOLCHAIN_PREFIX/libexec/gcc/*/*/cc1plus
    strip $TOOLCHAIN_PREFIX/libexec/gcc/*/*/collect2
    timestamp_set   toolchain install
    timestamp_force gdbserver configure
fi

# configure the gdbserver build now
if ! timestamp_check gdbserver configure; then
    echo "Configure: gdbserver build."
    mkdir -p $GDBSERVER_BUILD
    cd $GDBSERVER_BUILD &&
    CFLAGS="-g -O2 -static -mandroid -I$ANDROID_SYSROOT/usr/include" \
    LDFLAGS= \
    CC="$TOOLCHAIN_PREFIX/bin/arm-eabi-gcc" \
    run $TOOLCHAIN_SRC/gdb-6.6/gdb/gdbserver/configure \
    --host=arm-eabi-linux \
    --with-sysroot=$ANDROID_SYSROOT
    if [ $? != 0 ] ; then
        echo "Could not configure gdbserver build. See $TMPLOG"
        exit 1
    fi
    timestamp_set   gdbserver configure
    timestamp_force gdbserver build
fi

# build gdbserver
if ! timestamp_check gdbserver build; then
    echo "Building : gdbserver."
    cd $GDBSERVER_BUILD &&
    run make -j$JOBS
    if [ $? != 0 ] ; then
        echo "Could not build gdbserver. See $TMPLOG"
        exit 1
    fi
    timestamp_set   gdbserver build
    timestamp_force gdbserver install
fi

# install gdbserver
#
# note that we install it in the toolchain bin directory
# not in $SYSROOT/usr/bin
#
if ! timestamp_check gdbserver install; then
    echo "Install  : gdbserver."
    DEST=$TOOLCHAIN_PREFIX/bin
    mkdir -p $DEST &&
    $TOOLCHAIN_PREFIX/bin/arm-eabi-strip $GDBSERVER_BUILD/gdbserver &&
    run cp -f $GDBSERVER_BUILD/gdbserver $DEST/gdbserver
    if [ $? != 0 ] ; then
        echo "Could not install gdbserver. See $TMPLOG"
        exit 1
    fi
    timestamp_set   gdbserver install
    timestamp_force package toolchain
fi

# package the toolchain
TOOLCHAIN_TARBALL=/tmp/prebuilt-$TOOLCHAIN_NAME-$HOST_TAG.tar.bz2
if ! timestamp_check package toolchain; then
    echo "Package  : $HOST_ARCH toolchain binaries"
    echo "           into $TOOLCHAIN_TARBALL"
    cd $ANDROID_NDK_ROOT &&
    TARFLAGS="cjf"
    if [ $VERBOSE = yes ] ; then
      TARFLAGS="v$TARFLAGS"
    fi
    run tar $TARFLAGS $TOOLCHAIN_TARBALL build/prebuilt/$HOST_TAG/$TOOLCHAIN_NAME
    if [ $? != 0 ] ; then
        echo "ERROR: Cannot package prebuilt toolchain binaries. See $TMPLOG"
        exit 1
    fi
    timestamp_set package toolchain
else
    echo "prebuilt toolchain is in $TOOLCHAIN_TARBALL"
fi

echo "Done."
rm -f $TMPLOG
