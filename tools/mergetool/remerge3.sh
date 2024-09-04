#!/bin/bash

usage() {
  cat <<EOF
Usage:
  ${0}                  Remerge all files with conflict markers in the git working tree
  ${0} [FILE...]        Remerge the given files

Options:
  -t, --tool {bcompare,meld,vimdiff}
    Use the specified merge tool.
EOF
}

# shellcheck disable=SC2155
readonly BIN_DIR=$(dirname "${BASH_SOURCE[0]}")
readonly SPLIT3="${BIN_DIR}/split3.awk"
readonly CONFLICT_MARKER_BEGIN='^<{7}( .+)?$'
readonly CONFLICT_MARKER_BASE='^\|{7}( .+)?$'

TEMP_FILES=()
cleanup() {
  rm -rf "${TEMP_FILES[@]}"
}
trap cleanup EXIT

xtrace() {
  (
    set -x
    "${@}"
  )
}

mergetool() {
  local file="${1}"
  local MERGED="$file"
  local BASE="${file}:BASE"
  local LOCAL="${file}:LOCAL"
  local REMOTE="${file}:REMOTE"
  TEMP_FILES+=("$BASE" "$LOCAL" "$REMOTE")

  local has_base=false
  if grep -qE "${CONFLICT_MARKER_BASE}" "$file"; then
    has_base=true
  fi

  $has_base && awk -f "$SPLIT3" -v TARGET=BASE <"$file" >"$BASE"
  awk -f "$SPLIT3" -v TARGET=LOCAL <"$file" >"$LOCAL"
  awk -f "$SPLIT3" -v TARGET=REMOTE <"$file" >"$REMOTE"

  case "$MERGETOOL" in
    bc*)
      if $has_base; then
        xtrace bcompare "$LOCAL" "$REMOTE" "$BASE" -mergeoutput="$MERGED"
      else
        xtrace bcompare "$LOCAL" "$REMOTE" -mergeoutput="$MERGED"
      fi
      ;;
    meld)
      if $has_base; then
        xtrace meld "$LOCAL" "$BASE" "$REMOTE" -o "$MERGED"
      else
        xtrace meld "$LOCAL" "$MERGED" "$REMOTE"
      fi
      ;;
    vim*)
      if $has_base; then
        xtrace vimdiff -c '4wincmd w | wincmd J' "$LOCAL" "$BASE" "$REMOTE" "$MERGED"
      else
        xtrace vimdiff -c 'wincmd l' "$LOCAL" "$MERGED" "$REMOTE"
      fi
      ;;
  esac
}

#
# BEGIN
#

MERGETOOL=vimdiff
if [[ -n "$DISPLAY" ]]; then
  if command -v bcompare; then
    MERGETOOL=bcompare
  elif command -v meld; then
    MERGETOOL=meld
  fi
fi >/dev/null

while [[ "$1" =~ ^- ]]; do
  arg="${1}"
  shift
  case "$arg" in
    --) break ;;
    -t | --tool)
      MERGETOOL="${1}"
      shift
      ;;
    -h | --help | --usage)
      usage
      exit 0
      ;;
    *)
      usage
      exit 1
      ;;
  esac
done

IN_GIT_WORKTREE=false
if git rev-parse >/dev/null 2>/dev/null; then
  IN_GIT_WORKTREE=true
fi

readonly MERGETOOL
readonly IN_GIT_WORKTREE

FILES_UNFILTERED=()
if [[ "${#}" -eq 0 ]]; then
  while IFS= read -r -d '' ARG; do
    FILES_UNFILTERED+=("$ARG")
  done < <(git -c grep.fallbackToNoIndex=true grep -zlE "$CONFLICT_MARKER_BEGIN" 2>/dev/null)
else
  FILES_UNFILTERED+=("${@}")
fi

FILES=()
for file in "${FILES_UNFILTERED[@]}"; do
  if ! [[ -f "$file" ]] || [[ -L "$file" ]]; then
    echo "[SKIPPED] ${file}: not a regular file"
  elif ! grep -qE "$CONFLICT_MARKER_BEGIN" "$file"; then
    echo "[SKIPPED] ${file}: no conflict markers found"
  else
    FILES+=("$file")
  fi
done

echo "Found files with conflict markers:"
printf '    %s\n' "${FILES[@]}"

for file in "${FILES[@]}"; do
  echo
  echo "Merging '${file}'"
  mergetool "$file"
  exit_code="$?"
  if [[ "$exit_code" -eq 0 ]]; then
    if $IN_GIT_WORKTREE && [[ -n "$(git ls-files "$file")" ]]; then
      xtrace git add "$file"
    fi
  else
    echo "Failed to merge '${file}'"
  fi
  read -r -p "Continue merging other files? [Y/n]" -n 1 yn || exit
  echo
  [[ "$yn" == [nNqQ] ]] && exit "$exit_code"
done
