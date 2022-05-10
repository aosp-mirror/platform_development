#!/bin/bash

# Copyright (C) 2021 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Usage:
#   development/gki/kmi_abi_chk/kmi_static_chk.sh \
#     <current_symbol_info> <previous_symbol_info> ...
#
if [[ "$#" -lt 2 ]]; then
  echo "Usage: $0 <current_symbol_info> <previous_symbol_info> ..."
  exit 1
fi

ret=0
for f in "$@"; do
  if [[ ! -e "$f" ]]; then
    echo "Kernel symbol file $f does not exist!" >&2
    ret=1
  elif ! grep -iE "^0x[0-9a-f]{8}+.[_0-9a-z]+.vmlinux.EXPORT_SYMBOL" $f > /dev/null; then
    ret=1
    echo "$f doesn't look like kernel symbol file!" >&2
  fi
done

if [[ ! ret -eq 0 ]]; then
  exit $ret
fi

tmp=$(mktemp /tmp/linux-symvers.XXXXXX)
trap "rm -f $tmp" EXIT

curr=$1
shift

# Filter for vmlinux EXPORTE_SYMBOL* and remove trailing white spaces.
# The reason trailing white spaces is removed only for the current
# symbol file is because the following example/possibility:
#
# In the current symbol file:
# 0x8581ad8e	get_net_ns_by_fd	vmlinux	EXPORT_SYMBOL\t
#
# In the previous symbol file:
# 0x8581ad8e	get_net_ns_by_fd	vmlinux	EXPORT_SYMBOL_GPL\t
#
# The symbol is GPLed previously, but not in the current release, which won't
# break KMI ABI, because the requirement is "relaxed". We want this case to
# pass so a keyword like "...EXPORT_SYMBOL" in the current symbol file can
# still match "...EXPORT_SYMBOL_GPL" in the previous symbol file.
grep "vmlinux.EXPORT_SYMBOL" $curr | sed 's/[ \t]*$//' > $tmp

echo "Current kernel symbol file, $curr, is checking against:"

for f in "$@"; do
  echo "	$f"
# if nothing is found, grep returns 1, which means every symbol in the
# previous release (usually in *.symvers-$BID) can be found in the current
# release, so is considered successful here.
# if grep returns 0, which means some symbols are found in the previous
# symbol file but not in the current symbol file, then something wrong!
  if grep -vf $tmp $f; then
    ret=1
    echo "$f contains symbol(s) not found in, or incompatible with, $curr." >&2
  fi
done

exit $ret
