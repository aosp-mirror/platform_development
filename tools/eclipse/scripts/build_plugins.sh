#!/bin/bash

# build script for eclipse adt build on linux platform
#
# Usage: development/tools/eclipse/scripts/build_plugins <build_version> 
#
# It expects environment variable ECLIPSE_HOME to be defined to point to _your_
# version of Eclipse RCP (must have the WTP & GEF plugins available too.)
#
# If ECLIPSE_HOME is not provided, this script will _download_ a reference version
# of Eclipse RCP and install it in a specific location.
# 
# Other properties, ant scripts that drive the build are defined in ./buildConfig
# Currently, this script will create an update site at ${user.home}/www/no_crawl/android-build
# or at the directory specified using "-d"

# Known Issues:
# - Build does not properly clean up after itself (build server always executes from
#   a clean state.)
# - Script will fail if current absolute path has spaces in it.
# - Only linux is supported for now


set -e # abort this script early if any command fails

#
# -- Utility methods --
# 

function printUsage() {
  echo "Usage: $0 <build_qualifier> [-i] [-d <destination-directory>] [-a <archivePrefix>] "
  echo "<build_qualifier>: build qualifier string"
  echo "-i = build internal site. Otherwise, external site will be built"
  echo "-d = destination directory. Default is $USER/www/no_crawl/. Cannot contain spaces."
  echo "-a = archive prefix. Cannot contain spaces."
}

function die() {
  echo $@
  exit 1
}

function dieWithUsage() {
  echo $@
  echo
  printUsage
  exit 1
}


#
# -- Setup our custom version of Eclipse --
#

# The dependency on the linux platform comes from a series of environment
# variables that the eclipse ant runner expects. These are defined in the
# build.properties file. We can easily support other platforms but would need
# to override those values in this script.
HOST=`uname`
[ "$HOST" == "Linux" ] || die "ERROR: This script is currently only supported on Linux platform"

# Make sure this runs from the tools/eclipse plugin.
D=`dirname "$0"`
cd "$D/.."
[ `basename "$PWD"` == "eclipse" ] || dieWithUsage "Please run this script from the device/tools/eclipse directory"

# check for number of parameters
[ $# -lt 1 ] && dieWithUsage "ERROR: Not enough parameters"

# check if ECLIPSE_HOME set (ECLIPSE_HOME is were the "eclipse" binary and the
# "plugins" sub-directory are located)
if [ -z "$ECLIPSE_HOME" ]; then
  BASE_DIR=/buildbot/eclipse-android

  echo "ECLIPSE_HOME not set, using $BASE_DIR as default"

  if [ ! -d "$BASE_DIR" ]; then
    mkdir -p "$BASE_DIR" || die "Please create a directory $BASE_DIR where Eclipse will be installed, i.e. execute 'mkdir -p $BASE_DIR && chown $USER $BASE_DIR'."
  fi

  # download the version if not available
  VERSION="3.4.0"
  BASE_DIR="$BASE_DIR/$VERSION"
  scripts/setup_eclipse.sh -p "$BASE_DIR"

  ECLIPSE_HOME="$BASE_DIR/eclipse"      # path to installed directory
  PID_FILE="$BASE_DIR/eclipse.pid"
  [ -f "$PID_FILE" ] && ECLIPSE_PID=`cat "$PID_FILE"`
fi

echo "PWD=`pwd`"
echo "ECLIPSE_HOME=$ECLIPSE_HOME"

#
# -- Site parameters and Build version --
#

BUILD_VERSION="$1" ; shift

# parse for build internal site flag. If set, pass in internalSite property to ant scripts
if [ "-i" == "$1" ]; then
  shift
  echo "Setting for internal site build"
  SITE_PARAM="-DinternalSite=1 -DupdateSiteSource=$PWD/sites/internal"
else
  SITE_PARAM="-DupdateSiteSource=$PWD/sites/external"
fi

if [ "-d" == $1 ]; then
  shift
  echo "Setting destination directory to $1"
  SITE_PARAM="$SITE_PARAM -DupdateSiteRoot=$1"
  shift
fi

if [ "-a" == "$1" ]; then
  shift
  echo "Setting archivePrefix to $1"
  SITE_PARAM="$SITE_PARAM -DarchivePrefix=$1"
  shift
fi


#
# -- Configuration directory --
#

# The "configuration directory" will hold the workspace for this build.
# If it contains old data the build may fail so we need to clean it first
# and create it if it doesn't exist.
CONFIG_DIR="../../../out/eclipse-configuration-$BUILD_VERSION"
[ -d "$CONFIG_DIR" ] && rm -rfv "$CONFIG_DIR"
mkdir -p "$CONFIG_DIR"

# The "buildConfig" directory contains our customized ant rules
BUILDCONFIG="$PWD/buildConfig"


#
# -- Find Eclipse Launcher --
#

# Get the Eclipse launcher and build script to use
function findFirst() {
  for i in "$@"; do
    if [ -f "$i" ]; then
      echo "$i"
      return
    fi
  done
}

LAUNCHER=`findFirst "$ECLIPSE_HOME"/plugins/org.eclipse.equinox.launcher_*.jar`
BUILDFILE=`findFirst "$ECLIPSE_HOME"/plugins/org.eclipse.pde.build_*/scripts/build.xml`

# make sure we found valid files
if [ ! -f "$LAUNCHER" ]; then
  echo "Installation Error: Eclipse plugin org.eclipse.equinox.launcher...jar not detected. " \
       "Found '$LAUNCHER'. Aborting."
  exit 1
fi
if [ ! -f "$BUILDFILE" ]; then
  echo "Installation Error: Eclipse build file org.eclipse.pde.build_.../scripts/build.xml " \
       "not detected. Found '$BUILDFILE'. Aborting."
  exit 1
fi


#
# -- Print configuration used and actually execute the build --
#

echo "Eclipse configuration found:"
echo "  Eclipse Home: $ECLIPSE_HOME"
echo "  Launcher:     $LAUNCHER"
echo "  Build File:   $BUILDFILE"
echo "  Build Config: $BUILDCONFIG"
echo "  Config Dir:   $CONFIG_DIR"

# clean input directories to make sure there's nothing left from previous run

rm -fv *.properties *.xml
find . -name "@*" | xargs rm -rfv

# Now execute the ant runner

set +e  # don't stop on errors anymore, we want to catch there here

java \
  -jar $LAUNCHER \
  -data "$CONFIG_DIR" \
  -configuration "$CONFIG_DIR" \
  -application org.eclipse.ant.core.antRunner \
  -buildfile $BUILDFILE \
  -Dbuilder=$BUILDCONFIG \
  -DbuildDirectory=$PWD \
  -DforceContextQualifier=$BUILD_VERSION \
  -DECLIPSE_HOME=$ECLIPSE_HOME \
  $SITE_PARAM
RESULT=$?

if [ "0" != "$RESULT" ]; then
    echo "JAVA died with error code $RESULT"
    echo "Dump of build config logs:"
    for i in "$CONFIG_DIR"/*.log; do
        if [ -f "$i" ]; then
            echo "----------------------"
            echo "--- $i"
            echo "----------------------"
            cat "$i"
            echo
        fi
    done
fi

#
# -- Cleanup
#

if [ -n "$ECLIPSE_PID" ] && [ -f "$PID_FILE" ]; then
  rm -fv "$PID_FILE"
  kill -9 "$ECLIPSE_PID"
fi

# we're done!
