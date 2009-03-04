#!/bin/bash
# Entry point to build the Eclipse plugins for local deployment.
#
# Input parameters:
# $1: Optional build number. If present, will be appended to the date qualifier.
#     The build number cannot contain spaces *nor* periods (dashes are ok.)
# -i: Optional, if present, the Google internal update site will be built. Otherwise, 
#     the external site will be built
#
# Workflow:
# - calls buildserver with /home/$USER/www/no_crawl and -z
#   to build and create the update size but do not zip it in the destination directory.

set -e  # Fail this script as soon as a command fails -- fail early, fail fast

D=`dirname $0`
BUILD_NUMBER=""
INTERNAL_BUILD=""
# parse input parameters
while [ $# -gt 0 ]; do
  if [ "$1" == "-i" ]; then
    INTERNAL_BUILD="-i"
  elif [ "$1" != "" ]; then
    BUILD_NUMBER="$1"
  fi
  shift
done

DEST_DIR="$HOME"
[ -z "$DEST_DIR" ] && [ -n "$USER" ] && DEST_DIR="/home/$USER"
[ -z "$DEST_DIR" ] && DEST_DIR="~"
DEST_DIR="$DEST_DIR/www/no_crawl"

"$D/build_server.sh" "$DEST_DIR" "$BUILD_NUMBER" -z "$INTERNAL_BUILD"
