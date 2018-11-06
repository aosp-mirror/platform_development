#!/bin/bash
#VERSION=1

SELFNAME=$0

function printUsage() {
    echo "  $SELFNAME             check coding style for HEAD in this git repository"
    echo "  $SELFNAME -h          show this message"
}

function main() {
    test "$1" == "-h" && printUsage && exit
    test "$ANDROID_BUILD_TOP" == "" && echo "please run env setup" && exit
    GITROOTDIR=`git rev-parse --show-toplevel`
    test "$GITROOTDIR" == "" && echo "not inside a git repository" && exit
    MODIFIED=`git status -s --untracked-files=no | wc -l`
    test $MODIFIED -ne 0 && echo "please commit first" && exit

    cd $GITROOTDIR

    #basic check
    local PARAMS=" --config_xml $ANDROID_BUILD_TOP/prebuilts/checkstyle/android-style.xml"
    $ANDROID_BUILD_TOP/prebuilts/checkstyle/checkstyle.py $PARAMS

    #C++ check, no-op if no C, C++ files.
    $ANDROID_BUILD_TOP/prebuilts/clang/host/linux-x86/clang-stable/bin/git-clang-format \
            --commit HEAD^ --style file --extensions c,h,cc,cpp

    #commit message equal or less then 65 char for each line (suggested by lorenzo@20180625)
    local MSG=`git rev-list --format=%B --max-count=1 HEAD`
    local i=1
    while read -r line; do
        test `echo $line | wc -c` -gt 65 && echo "FAILED: Line $i exceed 65 chars limit: $line"
        i=$((i+1))
    done < <(echo "$MSG")

    cd -
}

main $*
