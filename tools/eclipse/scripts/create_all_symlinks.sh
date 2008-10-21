#!/bin/bash

echo "### $0 executing"

function die() {
  echo "Error: $*"
  exit 1
}

D="device/tools/eclipse/scripts"
if [ -d "../$D" ]; then
    cd "../$D"
else
  [ "${PWD: -28}" == "$D" ] || die "Please execute this from the $D directory"
fi

set -e # fail early

echo ; echo "### ADT ###" ; echo
./create_adt_symlinks.sh "$*"
echo ; echo "### COMMON ###" ; echo
./create_common_symlinks.sh "$*"
echo ; echo "### EDITORS ###" ; echo
./create_editors_symlinks.sh "$*"
echo ; echo "### DDMS ###" ; echo
./create_ddms_symlinks.sh "$*"
echo ; echo "### TEST ###" ; echo
./create_test_symlinks.sh "$*"
echo ; echo "### BRIDGE ###" ; echo
./create_bridge_symlinks.sh "$*"

echo "### $0 done"
