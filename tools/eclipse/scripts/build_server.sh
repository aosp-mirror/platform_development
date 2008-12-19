#!/bin/bash
# Entry point to build the Eclipse plugins for the build server.
#
# Input parameters:
# $1: *Mandatory* destination directory. Must already exist. Cannot contain spaces.
# $2: Optional build number. If present, will be appended to the date qualifier.
#     The build number cannot contain spaces *nor* periods (dashes are ok.)
# -z: Optional, prevents the final zip and leaves the udate-site directory intact.
# -i: Optional, if present, the Google internal update site will be built. Otherwise, 
#     the external site will be built
# Workflow:
# - make dx, ddms, ping
# - create symlinks (for eclipse code reorg, for ddms, ping)
# - call the actual builder script from Brett
# - zip resulting stuff and move to $DEST
# Note: currently wrap around existing shell script, reuse most of it,
# eventually both might merge as needed.

set -e  # Fail this script as soon as a command fails -- fail early, fail fast

DEST_DIR=""
BUILD_NUMBER=""
CREATE_ZIP="1"
INTERNAL_BUILD=""

function get_params() {
  # parse input parameters
  while [ $# -gt 0 ]; do
    if [ "$1" == "-z" ]; then
      CREATE_ZIP=""
    elif [ "$1" == "-i" ]; then
      INTERNAL_BUILD="-i"
    elif [ "$1" != "" ] && [ -z "$DEST_DIR" ]; then
      DEST_DIR="$1"
    elif [ "$1" != "" ] && [ -z "$BUILD_NUMBER" ]; then
      BUILD_NUMBER="$1"
    fi
    shift
  done
}

function die() {
  echo "Error:" $*
  echo "Aborting"
  exit 1
}

function check_params() {
  # This needs to run from the top android directory
  # Automatically CD to the top android directory, whatever its name
  D=`dirname "$0"`
  cd "$D/../../../../" && echo "Switched to directory $PWD"

  # The current Eclipse build has some Linux dependency in its config files
  [ `uname` == "Linux" ] || die "This must run from a Linux box."

  # Check dest dir exists
  [ -n "$DEST_DIR" ] || die "Usage: $0 <destination-directory> [build-number]"
  [ -d "$DEST_DIR" ] || die "Destination directory $DEST_DIR must exist."
}

function build_libs() {
  MAKE_OPT="-j8"
  echo "*** Building: make $MAKE_OPT dx ping ddms jarutils androidprefs layoutlib_api ninepatch sdklib sdkuilib"
  make $MAKE_OPT dx ping ddms jarutils androidprefs layoutlib_api layoutlib_utils ninepatch sdklib sdkuilib
}

function build_plugin {
  development/tools/eclipse/scripts/create_all_symlinks.sh

  # Qualifier is "v" followed by date/time in YYYYMMDDHHSS format and the optional
  # build number.
  DATE=`date +v%Y%m%d%H%M`
  QUALIFIER="$DATE"
  [ -n "$BUILD_NUMBER" ] && QUALIFIER="${QUALIFIER}-${BUILD_NUMBER}"

  # Compute the final directory name and remove any leftovers from previous
  # runs if any.
  BUILD_PREFIX="android-eclipse"  
  if [ "$INTERNAL_BUILD" ]; then
    # append 'eng' signifier to end of archive name to denote internal build
    BUILD_PREFIX="${BUILD_PREFIX}-eng"
  fi  

  # exclude date from build-zip name so it can be auto-calculated by continuous
  # test process unless there's no build number, in which case the date is
  # still used (useful for testing)
  ZIP_NAME="${BUILD_PREFIX}-${BUILD_NUMBER:-$DATE}.zip"
  [ -d "$DEST_DIR/$BUILD_PREFIX" ] || rm -rfv "$DEST_DIR/$BUILD_PREFIX"

  # Perform the Eclipse build and move the result in $DEST_DIR/android-build
  development/tools/eclipse/scripts/build_plugins.sh $QUALIFIER $INTERNAL_BUILD -d "$DEST_DIR" -a "$BUILD_PREFIX"

  # Cleanup
  [ -d "$QUALIFIER" ] && rm -rfv "$QUALIFIER"

  if [ "$CREATE_ZIP" ]; then
    # The result is a full update-site under $DEST_DIR/BUILD_PREFIX
    # Zip it and remove the directory.
    echo "**** Package in $DEST_DIR"
    [ -d "$DEST_DIR/$BUILD_PREFIX" ] || \
      die "Build failed to produce $DEST_DIR/$BUILD_PREFIX"
    cd "$DEST_DIR"
    [ -f "$ZIP_NAME" ] && rm -rfv "$ZIP_NAME"
    cd "$BUILD_PREFIX"
    zip -9r "../$ZIP_NAME" *
    cd .. # back to $DEST_DIR
    rm -rfv "$BUILD_PREFIX"  # removes the directory, not the zip
    echo "ZIP of Update site available at $DEST_DIR/${ZIP_NAME}"
  else
    echo "Update site available in $DEST_DIR/$BUILD_PREFIX"
  fi
}

get_params "$@"
check_params
build_libs
build_plugin
