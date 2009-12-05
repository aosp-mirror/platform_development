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
#  This shell script is used to download the sources of the Android NDK toolchain
#  from the git server at android.git.kernel.org and package them in a nice tarball
#  that can later be used with the 'built-toolchain.sh' script.
#

# include common function and variable definitions
. `dirname $0`/../core/ndk-common.sh

OPTION_HELP=no
OPTION_RELEASE=
OPTION_GIT=
OPTION_BRANCH=

# the default release name (use today's date)
RELEASE=`date +%Y%m%d`

# the default branch to use
BRANCH=eclair

GITCMD=git

VERBOSE=no
VERBOSE2=no

for opt do
    optarg=`expr "x$opt" : 'x[^=]*=\(.*\)'`
    case "$opt" in
    --help|-h|-\?) OPTION_HELP=yes
    ;;
    --branch=*)
        OPTION_BRANCH="$optarg"
        ;;
    --git=*)
        OPTION_GIT="$optarg"
        ;;
    --verbose)
        if [ "$VERBOSE" = "yes" ] ; then
            VERBOSE2=yes
        else
            VERBOSE=yes
        fi
        ;;
    --release=*)
        OPTION_RELEASE=$optarg
        ;;
    *)
        echo "unknown option '$opt', use --help"
        exit 1
    esac
done

if [ $OPTION_HELP = "yes" ] ; then
    echo "Download the NDK toolchain sources from android.git.kernel.org and package them."
    echo "You will need to run this script before being able to rebuild the NDK toolchain"
    echo "binaries from scratch with build/tools/build-toolchain.sh"
    echo ""
    echo "options (defaults in brackets):"
    echo ""
    echo "  --help               print this message"
    echo "  --branch=<name>      specify release branch [$BRANCH]"
    echo "  --release=<name>     specify release name [$RELEASE]"
    echo "  --git=<executable>   use this version of the git tool [$GITCMD]"
    echo "  --verbose            increase verbosity"
    echo ""
    exit 0
fi

TMPLOG=/tmp/android-ndk-download-toolchain-$$.log
rm -rf $TMPLOG

if [ $VERBOSE = yes ] ; then
    run ()
    {
        echo "##### NEW COMMAND"
        echo $@
        $@ 2>&1 | tee $TMPLOG
    }
    log ()
    {
        echo "LOG: $@"
    }
else
    echo "To follow download, please use in another terminal: tail -F $TMPLOG"
    run ()
    {
        echo "##### NEW COMMAND" >> $TMPLOG
        echo "$@" >> $TMPLOG
        $@ 1>$TMPLOG 2>&1
    }
    log ()
    {
        echo "$@" > /dev/null
    }
fi

if [ -n "$OPTION_RELEASE" ] ; then
    RELEASE="$OPTION_RELEASE"
    log "Using release name $RELEASE"
else
    log "Using default release name $RELEASE"
fi

# Check that 'git' works
if [ -n "$OPTION_GIT" ] ; then
    GITCMD="$OPTION_GIT"
    log "Using git tool command: '$GITCMD'"
else
    log "Using default git tool command."
fi
$GITCMD --version > /dev/null 2>&1
if [ $? != 0 ] ; then
    echo "The git tool doesn't seem to work. Please check $GITCMD"
    exit 1
fi
log "Git seems to work ok."

if [ -n "$OPTION_BRANCH" ] ; then
    BRANCH="$OPTION_BRANCH"
    log "Using branch named $BRANCH"
else
    log "Using default branch name $BRANCH"
fi

# Create temp directory where everything will be copied
#
PKGNAME=android-ndk-toolchain-$RELEASE
TMPDIR=/tmp/$PKGNAME
log "Creating temporary directory $TMPDIR"
rm -rf $TMPDIR && mkdir $TMPDIR
if [ $? != 0 ] ; then
    echo "Could not create temporary directory: $TMPDIR"
fi

# prefix used for all clone operations
GITPREFIX=git://android.git.kernel.org/toolchain

toolchain_clone ()
{
    echo "downloading sources for toolchain/$1"
    log "cloning $GITPREFIX/$1.git"
    run git clone $GITPREFIX/$1.git $1
    if [ $? != 0 ] ; then
        echo "Could not clone $GITPREFIX/$1.git ?"
        exit 1
    fi
    log "checking out $BRANCH branch of $1.git"
    cd $1
    run git checkout -b $BRANCH origin/$BRANCH
    if [ $? != 0 ] ; then
        echo "Could not checkout $1 ?"
        exit 1
    fi
    # get rid of .git directory, we won't need it.
    cd ..
    log "getting rid of .git directory for $1."
    run rm -rf $1/.git
}


cd $TMPDIR
toolchain_clone binutils
toolchain_clone build
toolchain_clone gcc
toolchain_clone gdb
toolchain_clone gmp
#toolchain_clone gold  # not sure about this one !
toolchain_clone mpfr

# We only keep one version of gcc and binutils

# we clearly don't need this
log "getting rid of obsolete sources: gcc-4.3.1 gdb-6.8"
rm -rf $TMPDIR/gcc/gcc-4.3.1
rm -rf $TMPDIR/gcc/gdb-6.8

# create the package
PACKAGE=/tmp/$PKGNAME.tar.bz2
echo "Creating package archive $PACKAGE"
cd `dirname $TMPDIR`
run tar cjvf $PACKAGE -C /tmp/$PKGNAME .
if [ $? != 0 ] ; then
    echo "Could not package toolchain source archive ?. See $TMPLOG"
    exit 1
fi

echo "Toolchain sources downloaded and packaged succesfully at $PACKAGE"
rm -f $TMPLOG
