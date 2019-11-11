#
# Copyright (C) 2019 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os
import subprocess
import sys
from collections import defaultdict
from pathlib import Path
import hashlib
import argparse
import zipfile

def silent_call(cmd):
  return subprocess.call(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL) == 0

def sha1sum(f):
  with open(f, 'rb') as fin:
    return hashlib.sha1(fin.read()).hexdigest()

def sha1sum_without_signing_key(filepath):
  apk = zipfile.ZipFile(filepath)
  l = []
  for f in sorted(apk.namelist()):
    if f.startswith('META-INF/'):
      continue
    l.append(hashlib.sha1(apk.read(f)).hexdigest())
    l.append(f)
  return hashlib.sha1(",".join(l).encode()).hexdigest()

def strip_and_sha1sum(filepath):
  tmp_filepath = filepath + '.tmp.no-build-id'
  strip_all_and_remove_build_id = lambda: silent_call(
      ["llvm-strip", "--strip-all", "--keep-section=.ARM.attributes",
       "--remove-section=.note.gnu.build-id", filepath, "-o", tmp_filepath])
  try:
    if strip_all_and_remove_build_id():
      return sha1sum(tmp_filepath)
    else:
      return sha1sum(filepath)
  finally:
    if os.path.exists(tmp_filepath):
      os.remove(tmp_filepath)

  return sha1sum(filepath)


def main(all_targets, search_paths, ignore_signing_key=False):
  def get_target_name(path):
    return os.path.basename(os.path.normpath(path))

  def run(path):
    is_native_component = silent_call(["llvm-objdump", "-a", path])
    is_apk = path.endswith('.apk')
    if is_native_component:
      return strip_and_sha1sum(path)
    elif is_apk and ignore_signing_key:
      return sha1sum_without_signing_key(path)
    else:
      return sha1sum(path)

  artifact_target_map = defaultdict(list)
  for target in all_targets:
    paths = []
    for search_path in search_paths:
      for path in Path(target, search_path).glob('**/*'):
        if path.exists() and not path.is_dir():
          paths.append((str(path), str(path.relative_to(target))))

    results = [(run(path), filename) for path, filename in paths]

    for sha1, filename in results:
      artifact_target_map[(sha1, filename)].append(get_target_name(target))

  def pretty_print(sha1, filename, targets):
    return filename + ", " + sha1[:10] + ", " + ";".join(targets) + "\n"

  header = "filename, sha1sum, targets\n"

  def is_common(targets):
    return len(targets) == len(all_targets)
  common = sorted([pretty_print(sha1, filename, targets)
                   for (sha1, filename), targets in artifact_target_map.items() if is_common(targets)])
  diff = sorted([pretty_print(sha1, filename, targets)
                 for (sha1, filename), targets in artifact_target_map.items() if not is_common(targets)])

  with open("common.csv", 'w') as fout:
    fout.write(header)
    fout.writelines(common)
  with open("diff.csv", 'w') as fout:
    fout.write(header)
    fout.writelines(diff)


if __name__ == "__main__":
  parser = argparse.ArgumentParser(prog="compare_images", usage="compare_images -t model1 model2 [model...] -s dir1 [dir...] [-i] [-u]")
  parser.add_argument("-t", "--target", nargs='+', required=True)
  parser.add_argument("-s", "--search_path", nargs='+', required=True)
  parser.add_argument("-i", "--ignore_signing_key", action='store_true')
  parser.add_argument("-u", "--unzip", action='store_true')
  args = parser.parse_args()
  if len(args.target) < 2:
    parser.error("The number of targets has to be at least two.")
  if args.unzip:
    for t in args.target:
      unzip_cmd = ["unzip", "-qd", t, os.path.join(t, "*.zip")]
      unzip_cmd.extend([os.path.join(s, "*") for s in args.search_path])
      subprocess.call(unzip_cmd)
  main(args.target, args.search_path, args.ignore_signing_key)
