#!/bin/sh
#
# This is used to cleanup the project directories before making a commit or
# a clean release. This will get rid of auto-generated files in the
# apps/<name>/project directories.
#
for projectPath in `find apps/*/project` ; do
    rm -rf $projectPath/bin
    rm -rf $projectPath/gen
    rm -f  $projectPath/build.xml
    rm -f  $projectPath/local.properties
done
