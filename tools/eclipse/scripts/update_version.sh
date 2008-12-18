#!/bin/bash

OLD="$1"
NEW="$2"

# sanity check in input args
if [ -z "$OLD" ] || [ -z "$NEW" ]; then
    cat <<EOF
Usage: $0 <old> <new>
Changes the ADT plugin revision number.
Example:
  cd tools/eclipse
  scripts/update_version.sh 0.1.2 0.2.3
EOF
    exit 1
fi

# sanity check on current dir
if [ `basename "$PWD"` != "eclipse" ]; then
    echo "Please run this from tools/eclipse."
    exit 1
fi

# quote dots for regexps
OLD="${OLD//./\.}"
NEW="${NEW//./\.}"

# Find all the files with the old pattern, except changes.txt and
# p4 edit them. Skip that if there's no p4 in path.
if which g4 1>/dev/null 2>/dev/null ; then
    grep -rl "$OLD" * | grep -E "\.xml$|\.MF$" | xargs -n 5 g4 edit
fi

# Now find the same files but this time use sed to replace in-place with
# the new pattern. Old files get backuped with the .old extension.
grep -rl "$OLD" * | grep -E "\.xml$|\.MF$" | xargs -n 1 sed -i.old "s/$OLD/$NEW/g"

