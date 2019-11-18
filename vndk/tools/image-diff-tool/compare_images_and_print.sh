#!/bin/bash

set -e

PROGNAME="$0"

function usage {
  cat <<EOF
Usage:
    ${PROGNAME} [OPTION] ... "[Args for compare_images]"

Options:
    -h, --help
        Show this message and exit.
    -c, --check
        Exit with a non-zero status if there is any unexpected diffs.
EOF
}

while [[ "$1" =~ ^- ]] && [[ "$1" != "--" ]]; do
  case "$1" in
    -h | --help)
      usage
      exit 0
      ;;
    -c | --check)
      CHECK_DIFF_RESULT=1
      ;;
    *)
      break
      ;;
  esac
  shift
done
if [ "$1" == "--" ]; then
  shift
fi

readonly CHECK_DIFF_RESULT

build/soong/soong_ui.bash --make-mode compare_images
. build/envsetup.sh
lunch aosp_arm64
COMMON_WHITELIST=development/vndk/tools/image-diff-tool/whitelist.txt
out/host/linux-x86/bin/compare_images $1 -u -w "${COMMON_WHITELIST}"

echo
echo " - Common parts"
cat common.csv
echo
echo " - Different parts (that are whitelisted)"
cat whitelisted_diff.csv
echo
echo " - Different parts (that are not whitelisted)"
cat diff.csv

if [ -v DIST_DIR ]; then
  cp common.csv diff.csv whitelisted_diff.csv "${DIST_DIR}"
fi

if [ "$(wc -l diff.csv | cut -d' ' -f1)" -gt "1" ]; then
  cat >&2 <<EOF

[ERROR] Unexpected diffing files.
There are diffing files that are not ignored by whitelist.
The whitelist may need to be updated.
EOF
  if [ -n "${CHECK_DIFF_RESULT}" ]; then
    exit 1
  fi
fi
