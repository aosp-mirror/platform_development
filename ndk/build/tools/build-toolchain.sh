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
. `dirname $0`/../core/ndk-common.sh

# number of jobs to run in parallel when running make
JOBS=$HOST_NUM_CPUS

PLATFORM=android-3
ABI=arm
GCC_VERSION=4.2.1
GDB_VERSION=6.6
BINUTILS_VERSION=2.17
RELEASE=`date +%Y%m%d`
BUILD_OUT=`mktemp -d /tmp/ndk-toolchain-XXX`

OPTION_HELP=no
OPTION_PLATFORM=
OPTION_FORCE_32=no
OPTION_REBUILD=no
OPTION_GCC_VERSION=
OPTION_GDB_VERSION=
OPTION_PACKAGE=
OPTION_RELEASE=
OPTION_BUILD_OUT=

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
    --gcc-version=*)
        OPTION_GCC_VERSION=$optarg
        ;;
    --gdb-version=*)
        OPTION_GDB_VERSION=$optarg
        ;;
    --package=*)
        OPTION_PACKAGE="$optarg"
        ;;
    --platform=*)
        PLATFORM=$optarg
        ;;
    --build-out=*)
        OPTION_BUILD_OUT="$optarg"
        ;;
    --abi=*)
        ABI=$optarg
        ;;
    --release=*)
        OPTION_RELEASE=$optarg
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
    echo "This script will automatically download the sources from the"
    echo "Internet, unless you use the --package=<file> option to specify"
    echo "the exact source package to use."
    echo ""
    echo "See build/tools/download-toolchain-sources.sh for a tool that"
    echo "can create a compatible source package from the current"
    echo "git repositories."
    echo ""
    echo "options (defaults are within brackets):"
    echo ""
    echo "  --help                   print this message"
    echo "  --gcc-version=<version>  select GCC version [$GCC_VERSION]"
    echo "  --gdb-version=<version>  select GDB version [$GDB_VERSION]"
    echo "  --package=<file>         specify download source package"
    echo "  --platform=<name>        generate toolchain from platform <name> [$PLATFORM]"
    echo "  --abi=<name>             generate toolchain from abi <name> [$ABI]"
    echo "  --release=<name>         specify prebuilt release name [$RELEASE]"
    echo "  --build-out=<path>       set temporary build out directory [/tmp/<random>]"
    echo "  --force-download         force a download and unpacking of the toolchain sources"
    echo "  --force-build            force a rebuild of the sources"
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
        echo "$@"
        $@ 2>&1
    }
    log ()
    {
        echo "LOG: $@"
    }
else
    echo "To follow build, please use in another terminal: tail -F $TMPLOG"
    run ()
    {
        echo "##### NEW COMMAND" >> $TMPLOG
        echo "$@" >> $TMPLOG
        $@ >>$TMPLOG 2>&1
    }
    log ()
    {
        echo "$@" > /dev/null
    }
fi

if [ -n "$OPTION_GCC_VERSION" ] ; then
    GCC_VERSION="$OPTION_GCC_VERSION"
    log "Using gcc version $GCC_VERSION"
else
    log "Using default gcc version $GCC_VERSION"
fi

if [ -n "$OPTION_GDB_VERSION" ] ; then
    GDB_VERSION="$OPTION_GDB_VERSION"
    log "Using gdb version $GDB_VERSION"
else
    log "Using default gdb version $GDB_VERSION"
fi

if [ -n "$OPTION_RELEASE" ] ; then
    RELEASE="$RELEASE"
    log "Using toolchain release name '$RELEASE'"
else
    log "Using default toolchain name '$RELEASE'"
fi

if [ -n "$OPTION_PACKAGE" ] ; then
    if [ ! -f "$OPTION_PACKAGE" ] ; then
        echo "Package is not a file: $OPTION_PACKAGE"
        exit 1
    fi
fi

if [ -n "$OPTION_BUILD_OUT" ] ; then
    BUILD_OUT=$OPTION_BUILD_OUT
    log "Using specific build out directory: $BUILD_OUT"
else
    log "Using default random build out directory: $BUILD_OUT"
fi

ANDROID_NDK_ROOT=`cd $ANDROID_NDK_ROOT && pwd`
ANDROID_NDK_ARCHIVE=$ANDROID_NDK_ROOT/build/toolchains/archive
ANDROID_PLATFORMS_ROOT=$ANDROID_NDK_ROOT/build/platforms

# where all generated files will be placed
OUT=$BUILD_OUT
PACKAGE_OUT=$OUT/packages
TIMESTAMP_OUT=$OUT/timestamps

# where the sysroot is located
ANDROID_TOOLCHAIN_SRC=$OUT/src
ANDROID_SYSROOT=$ANDROID_NDK_ROOT/build/platforms/$PLATFORM/arch-$ABI

# Let's check that we have a working md5sum here
A_MD5=`echo "A" | md5sum | cut -d' ' -f1`
if [ "$A_MD5" != "bf072e9119077b4e76437a93986787ef" ] ; then
    echo "Please install md5sum on this machine"
    exit 2
fi

# Find if a given shell program is available.
# We need to take care of the fact that the 'which <foo>' command
# may return either an empty string (Linux) or something like
# "no <foo> in ..." (Darwin). Also, we need to redirect stderr
# to /dev/null for Cygwin
#
# $1: variable name
# $2: program name
#
# Result: set $1 to the full path of the corresponding command
#         or to the empty/undefined string if not available
#
find_program ()
{
    local PROG
    PROG=`which $2 2>/dev/null`
    if [ -n "$PROG" ] ; then
        echo "$PROG" | grep -q -e '^no '
        if [ $? = 0 ] ; then
            PROG=
        fi
    fi
    eval $1="$PROG"
}

# And wget too
find_program WGET wget
find_program CURL curl
find_program SCP scp

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
# package.
#
# $1: directory name under build/archive  (e.g. 'toolchain')
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
# $1: package name      (e.g. toolchain)
# $2: target directory  (e.g. /tmp/foo)
#
unpack_package ()
{
    SRCPKG=`var_value PKG_$1`
    SRCDIR=$2
    if ! timestamp_check $1 unpack; then
        echo "Unpack  : $1 sources"
        echo "          from $SRCPKG"
        echo "          into $SRCDIR"
        run rm -rf $SRCDIR
        run mkdir -p $SRCDIR
        TARFLAGS=xjf
        if [ $VERBOSE2 = yes ]; then
          TARFLAGS="v$TARFLAGS"
        fi
        run tar $TARFLAGS $SRCPKG -C $SRCDIR
        if [ $? != 0 ] ; then
            echo "ERROR: Could not unpack $1, See $TMPLOG"
            exit 1
        fi
        timestamp_set   $1 unpack
        timestamp_force $1 patch
    fi
}

# Patch a given package at a target location
# $1: package name      (e.g. toolchain)
# $2: target directory  (e.g. /tmp/foo)
# $3: patch directory   (e.g. build/tools/toolchain-patches)
#
# The rationale here is that anything named like $3/<subpath>/<foo>.patch
# will be applied with "patch -p1" under $2/<subpath>
#
# Patches are listed and applied in alphanumerical order of their names
# as returned by 'find'. Consider using numbered prefixes like the patch
# files generated by "git format-patch" are named.
#
patch_package ()
{
    SRCPKG=`var_value PKG_$1`
    SRCDIR=$2
    if ! timestamp_check $1 patch; then
        PATCH_FILES=`(cd $3 && find . -name "*.patch") 2> /dev/null`
        if [ -z "$PATCH_FILES" ] ; then
            echo "Patch   : none provided"
            return
        fi
        for PATCH in $PATCH_FILES; do
            echo "Patch   : $1 sources"
            echo "          from $PATCH"
            echo "          into $SRCDIR"
            PATCHDIR=`dirname $PATCH`
            PATCHNAME=`basename $PATCH`
            cd $SRCDIR/$PATCHDIR && patch -p1 < $3/$PATCH
            if [ $? != 0 ] ; then
                echo "Patch failure !! Please check toolchain package !"
                exit 1
            fi
        done
        timestamp_set   $1 patch
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

if [ -n "$OPTION_PACKAGE" ] ; then
    PKG_toolchain="$OPTION_PACKAGE"
else
    download_package toolchain
fi

unpack_package toolchain $ANDROID_TOOLCHAIN_SRC
patch_package  toolchain $ANDROID_TOOLCHAIN_SRC $ANDROID_NDK_ROOT/build/tools/toolchain-patches

# remove all info files from the unpacked toolchain sources
# they create countless little problems during the build
# if you don't have exactly the configuration expected by
# the scripts.
#
find $ANDROID_TOOLCHAIN_SRC -type f -a -name "*.info" -print0 | xargs -0 rm -f


# Setup variables to properly build a new toolchain
# $1: toolchain name, e.g. arm-eabi-4.2.1
build_toolchain ()
{
    TOOLCHAIN_NAME=$1

    # where the toolchain is going to be built
    TOOLCHAIN_BUILD=$OUT/$TOOLCHAIN_NAME

    # where the gdbserver binaries will be placed
    GDBSERVER_BUILD=$TOOLCHAIN_BUILD/gdbserver

    TOOLCHAIN_SRC=$ANDROID_TOOLCHAIN_SRC
    TOOLCHAIN_PREFIX=$OUT/build/prebuilt/$HOST_TAG/$TOOLCHAIN_NAME
    TOOLCHAIN_LICENSES=$ANDROID_NDK_ROOT/build/tools/toolchain-licenses

    # configure the toolchain
    if ! timestamp_check $TOOLCHAIN_NAME configure; then
        echo "Configure: $TOOLCHAIN_NAME toolchain build"
        # Old versions of the toolchain source packages placed the
        # configure script at the top-level. Newer ones place it under
        # the build directory though. Probe the file system to check
        # this.
        BUILD_SRCDIR=$TOOLCHAIN_SRC/build
        if [ ! -d $BUILD_SRCDIR ] ; then
            BUILD_SRCDIR=$TOOLCHAIN_SRC
        fi
        OLD_ABI="$ABI"
        OLD_CFLAGS="$CFLAGS"
        OLD_LDFLAGS="$LDFLAGS"
        mkdir -p $TOOLCHAIN_BUILD &&
        cd $TOOLCHAIN_BUILD &&
        export ABI="32" &&  # needed to build a 32-bit gmp
        export CFLAGS="$HOST_CFLAGS" &&
        export LDFLAGS="$HOST_LDFLAGS" && run \
        $BUILD_SRCDIR/configure --target=arm-eabi \
                                --disable-nls \
                                --prefix=$TOOLCHAIN_PREFIX \
                                --with-sysroot=$ANDROID_SYSROOT \
                                --with-binutils-version=$BINUTILS_VERSION \
                                --with-gcc-version=$GCC_VERSION \
                                --with-gdb-version=$GDB_VERSION
        if [ $? != 0 ] ; then
            echo "Error while trying to configure toolchain build. See $TMPLOG"
            exit 1
        fi
        ABI="$OLD_ABI"
        CFLAGS="$OLD_CFLAGS"
        LDFLAGS="$OLD_LDFLAGS"
        timestamp_set   $TOOLCHAIN_NAME configure
        timestamp_force $TOOLCHAIN_NAME build
    fi

    # build the toolchain
    if ! timestamp_check $TOOLCHAIN_NAME build ; then
        echo "Building : $TOOLCHAIN_NAME toolchain [this can take a long time]."
        OLD_CFLAGS="$CFLAGS"
        OLD_LDFLAGS="$LDFLAGS"
        OLD_ABI="$ABI"
        cd $TOOLCHAIN_BUILD &&
        export CFLAGS="$HOST_CFLAGS" &&
        export LDFLAGS="$HOST_LDFLAGS" &&
        export ABI="32" &&
        run make -j$JOBS
        if [ $? != 0 ] ; then
            echo "Error while building toolchain. See $TMPLOG"
            exit 1
        fi
        CFLAGS="$OLD_CFLAGS"
        LDFLAGS="$OLD_LDFLAGS"
        ABI="$OLD_ABI"
        timestamp_set   $TOOLCHAIN_NAME build
        timestamp_force $TOOLCHAIN_NAME install
    fi

    # install the toolchain to its final location
    if ! timestamp_check $TOOLCHAIN_NAME install ; then
        echo "Install  : $TOOLCHAIN_NAME toolchain binaries."
        cd $TOOLCHAIN_BUILD &&
        run make install
        if [ $? != 0 ] ; then
            echo "Error while installing toolchain. See $TMPLOG"
            exit 1
        fi
        # don't forget to copy the GPL and LGPL license files
        cp -f $TOOLCHAIN_LICENSES/COPYING $TOOLCHAIN_LICENSES/COPYING.LIB $TOOLCHAIN_PREFIX
        # remove some unneeded files
        rm -f $TOOLCHAIN_PREFIX/bin/*-gccbug
        rm -rf $TOOLCHAIN_PREFIX/man $TOOLCHAIN_PREFIX/info
        # strip binaries to reduce final package size
        strip $TOOLCHAIN_PREFIX/bin/*
        strip $TOOLCHAIN_PREFIX/arm-eabi/bin/*
        strip $TOOLCHAIN_PREFIX/libexec/gcc/*/*/cc1
        strip $TOOLCHAIN_PREFIX/libexec/gcc/*/*/cc1plus
        strip $TOOLCHAIN_PREFIX/libexec/gcc/*/*/collect2
        timestamp_set   $TOOLCHAIN_NAME install
        timestamp_force $TOOLCHAIN_NAME-gdbserver configure
    fi

    # configure the gdbserver build now
    if ! timestamp_check $TOOLCHAIN_NAME-gdbserver configure; then
        echo "Configure: $TOOLCHAIN_NAME gdbserver build."
        # Old toolchain source packages placed the gdb sources at
        # the top-level, while newer ones place them under the 'gdb'
        # directory. Probe the filesystem to check which one is appropriate.
        GDB_SRCDIR=$TOOLCHAIN_SRC/gdb/gdb-$GDB_VERSION
        if [ ! -d $GDB_SRCDIR ] ; then
            GDB_SRCDIR=$TOOLCHAIN_SRC/gdb-$GDB_VERSION
        fi
        mkdir -p $GDBSERVER_BUILD
        OLD_CC="$CC"
        OLD_CFLAGS="$CFLAGS"
        OLD_LDFLAGS="$LDFLAGS"
        cd $GDBSERVER_BUILD &&
        export CC="$TOOLCHAIN_PREFIX/bin/arm-eabi-gcc" &&
        export CFLAGS="-g -O2 -static -mandroid"  &&
        export LDFLAGS= &&
        run $GDB_SRCDIR/gdb/gdbserver/configure \
        --host=arm-eabi-linux \
        --with-sysroot=$ANDROID_SYSROOT
        if [ $? != 0 ] ; then
            echo "Could not configure gdbserver build. See $TMPLOG"
            exit 1
        fi
        CC="$OLD_CC"
        CFLAGS="$OLD_CFLAGS"
        LDFLAGS="$OLD_LDFLAGS"
        timestamp_set   $TOOLCHAIN_NAME-gdbserver configure
        timestamp_force $TOOLCHAIN_NAME-gdbserver build
    fi

    # build gdbserver
    if ! timestamp_check $TOOLCHAIN_NAME-gdbserver build; then
        echo "Building : $TOOLCHAIN_NAME gdbserver."
        cd $GDBSERVER_BUILD &&
        run make -j$JOBS
        if [ $? != 0 ] ; then
            echo "Could not build $TOOLCHAIN_NAME gdbserver. See $TMPLOG"
            exit 1
        fi
        timestamp_set   $TOOLCHAIN_NAME-gdbserver build
        timestamp_force $TOOLCHAIN_NAME-gdbserver install
    fi

    # install gdbserver
    #
    # note that we install it in the toolchain bin directory
    # not in $SYSROOT/usr/bin
    #
    if ! timestamp_check $TOOLCHAIN_NAME-gdbserver install; then
        echo "Install  : $TOOLCHAIN_NAME gdbserver."
        DEST=$TOOLCHAIN_PREFIX/bin
        mkdir -p $DEST &&
        $TOOLCHAIN_PREFIX/bin/arm-eabi-strip $GDBSERVER_BUILD/gdbserver &&
        run cp -f $GDBSERVER_BUILD/gdbserver $DEST/gdbserver
        if [ $? != 0 ] ; then
            echo "Could not install gdbserver. See $TMPLOG"
            exit 1
        fi
        timestamp_set   $TOOLCHAIN_NAME-gdbserver install
        timestamp_force package toolchain
    fi
}

# Look at the toolchains available from the source package
#
# The old source tarball only contained gcc 4.2.1, the new
# ones contain multiple toolchains
#
if [ -d $ANDROID_TOOLCHAIN_SRC/gcc-4.2.1 ] ; then
    # An old toolchain source package
    ANDROID_TOOLCHAIN_LIST=arm-eabi-4.2.1
else
    ANDROID_TOOLCHAIN_LIST="arm-eabi-4.2.1 arm-eabi-4.4.0"
fi

for _toolchain in $ANDROID_TOOLCHAIN_LIST; do
    if timestamp_check toolchain build; then
        timestamp_force ${_toolchain} configure
        timestamp_force ${_toolchain}-gdbserver configure
    fi
    # Gcc 4.2.1 needs binutils 2.17
    if [ ${_toolchain} = arm-eabi-4.2.1 ] ; then
        GCC_VERSION=4.2.1
        BINUTILS_VERSION=2.17
    else
        GCC_VERSION=4.4.0
        BINUTILS_VERSION=2.19
    fi
    build_toolchain ${_toolchain}
done

# package the toolchain
TOOLCHAIN_TARBALL=/tmp/android-ndk-prebuilt-$RELEASE-$HOST_TAG.tar.bz2
if ! timestamp_check package toolchain; then
    echo "Cleanup  : Removing unuseful stuff"
    rm -rf $OUT/build/prebuilt/$HOST_TAG/*/share
    find $OUT/build/prebuilt/$HOST_TAG -name "libiberty.a" | xargs rm -f
    find $OUT/build/prebuilt/$HOST_TAG -name "libarm-elf-linux-sim.a" | xargs rm -f
    echo "Package  : $HOST_ARCH toolchain binaries"
    echo "           into $TOOLCHAIN_TARBALL"
    cd $ANDROID_NDK_ROOT &&
    TARFLAGS="cjf"
    if [ $VERBOSE = yes ] ; then
      TARFLAGS="v$TARFLAGS"
    fi
    TOOLCHAIN_SRC_DIRS=
    for _toolchain in $ANDROID_TOOLCHAIN_LIST; do
        TOOLCHAIN_SRC_DIRS="$TOOLCHAIN_SRC_DIRS build/prebuilt/$HOST_TAG/${_toolchain}"
    done
    run tar $TARFLAGS $TOOLCHAIN_TARBALL -C $OUT $TOOLCHAIN_SRC_DIRS
    if [ $? != 0 ] ; then
        echo "ERROR: Cannot package prebuilt toolchain binaries. See $TMPLOG"
        exit 1
    fi
    timestamp_set package toolchain
    echo "prebuilt toolchain is in $TOOLCHAIN_TARBALL"
else
    echo "prebuilt toolchain is in $TOOLCHAIN_TARBALL"
fi

if [ -z "$OPTION_BUILD_OUT" ] ; then
    echo "Cleaning temporary directory $OUT"
    rm -rf $OUT
else
    echo "Don't forget to clean build directory $OUT"
fi

echo "Done."
rm -f $TMPLOG
