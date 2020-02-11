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
import fnmatch
import tempfile

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
  # TODO: save striped file in tmp directory to support readonly directory.
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


def make_filter_from_whitelists(whitelists, all_targets):
  """Creates a callable filter from a list of whitelist files.

  Whitelist can contain pathname patterns or ignored lines. Pathnames are case
  insensitive.

  For example, this ignores the file "system/build.prop":
    SYSTEM/build.prop

  This ignores txt files:
    *.txt

  This ignores files in directory "system/dontcare/"
    SYSTEM/dontcare/*

  This ignores lines prefixed with pat1 or pat2 in file "system/build.prop":
    SYSTEM/build.prop=pat1 pat2

  Args:
    whitelists: A list of whitelist filenames.
    all_targets: A list of targets to compare.

  Returns:
    A callable object that accepts a file pathname and returns True if the file
    is ignored by the whitelists and False when it is not.
  """
  ignored_patterns = set()
  ignored_lines = defaultdict(list)
  for whitelist in whitelists:
    if not os.path.isfile(whitelist):
      continue
    with open(whitelist, 'rb') as f:
      for line in f:
        pat = line.strip().decode()
        if pat and pat[-1] == '\\':
          pat = pat.rstrip('\\')
        if '=' in pat:
          filename, prefixes = pat.split('=', 1)
          prefixes = prefixes.split()
          if prefixes:
            ignored_lines[filename.lower()].extend(prefixes)
        elif pat:
          ignored_patterns.add(pat.lower())

  def diff_with_ignored_lines(filename, prefixes):
    """Compares sha1 digest of file while ignoring lines.

    Args:
      filename: File to compare among each target.
      prefixes: A list of prefixes. Lines that start with prefix are ignored.

    Returns:
      True if file is identical among each target.
    """
    file_digest_respect_ignore = []
    for target in all_targets:
      pathname = os.path.join(target, filename)
      if not os.path.isfile(pathname):
        return False
      sha1 = hashlib.sha1()
      with open(pathname, 'rb') as f:
        for line in f:
          line_text = line.decode()
          if not any(line_text.startswith(prefix) for prefix in prefixes):
            sha1.update(line)
      file_digest_respect_ignore.append(sha1.hexdigest())
    return (len(file_digest_respect_ignore) == len(all_targets) and
            len(set(file_digest_respect_ignore)) == 1)

  def whitelist_filter(filename):
    norm_filename = filename.lower()
    for pattern in ignored_patterns:
      if fnmatch.fnmatch(norm_filename, pattern):
        return True
    if norm_filename in ignored_lines:
      ignored_prefixes = ignored_lines[norm_filename]
      return diff_with_ignored_lines(filename, ignored_prefixes)
    return False

  return whitelist_filter


def main(all_targets, search_paths, whitelists, ignore_signing_key=False):
  def run(path):
    is_native_component = silent_call(["llvm-objdump", "-a", path])
    is_apk = path.endswith('.apk')
    if is_native_component:
      return strip_and_sha1sum(path)
    elif is_apk and ignore_signing_key:
      return sha1sum_without_signing_key(path)
    else:
      return sha1sum(path)

  # artifact_sha1_target_map[filename][sha1] = list of targets
  artifact_sha1_target_map = defaultdict(lambda: defaultdict(list))
  for target in all_targets:
    paths = []
    for search_path in search_paths:
      for path in Path(target, search_path).glob('**/*'):
        if path.exists() and not path.is_dir():
          paths.append((str(path), str(path.relative_to(target))))

    target_basename = os.path.basename(os.path.normpath(target))
    for path, filename in paths:
      sha1 = run(path)
      artifact_sha1_target_map[filename][sha1].append(target_basename)

  def pretty_print(sha1, filename, targets):
    return '{}, {}, {}\n'.format(filename, sha1[:10], ';'.join(targets))

  def is_common(sha1_target_map):
    for sha1, targets in sha1_target_map.items():
      return len(sha1_target_map) == 1 and len(targets) == len(all_targets)
    return False

  whitelist_filter = make_filter_from_whitelists(whitelists, all_targets)

  common = []
  diff = []
  whitelisted_diff = []
  for filename, sha1_target_map in artifact_sha1_target_map.items():
    if is_common(sha1_target_map):
      for sha1, targets in sha1_target_map.items():
        common.append(pretty_print(sha1, filename, targets))
    else:
      if whitelist_filter(filename):
        for sha1, targets in sha1_target_map.items():
          whitelisted_diff.append(pretty_print(sha1, filename, targets))
      else:
        for sha1, targets in sha1_target_map.items():
          diff.append(pretty_print(sha1, filename, targets))

  common = sorted(common)
  diff = sorted(diff)
  whitelisted_diff = sorted(whitelisted_diff)

  header = "filename, sha1sum, targets\n"

  with open("common.csv", 'w') as fout:
    fout.write(header)
    fout.writelines(common)
  with open("diff.csv", 'w') as fout:
    fout.write(header)
    fout.writelines(diff)
  with open("whitelisted_diff.csv", 'w') as fout:
    fout.write(header)
    fout.writelines(whitelisted_diff)

def main_with_zip(extracted_paths, args):
  for origin_path, tmp_path in zip(args.target, extracted_paths):
    unzip_cmd = ["unzip", "-qd", tmp_path, os.path.join(origin_path, "*.zip")]
    unzip_cmd.extend([os.path.join(s, "*") for s in args.search_path])
    subprocess.call(unzip_cmd)
  main(extracted_paths, args.search_path, args.whitelist, args.ignore_signing_key)

if __name__ == "__main__":
  parser = argparse.ArgumentParser(prog="compare_images", usage="compare_images -t model1 model2 [model...] -s dir1 [dir...] [-i] [-u] [-p] [-w whitelist1] [-w whitelist2]")
  parser.add_argument("-t", "--target", nargs='+', required=True)
  parser.add_argument("-s", "--search_path", nargs='+', required=True)
  parser.add_argument("-i", "--ignore_signing_key", action='store_true')
  parser.add_argument("-u", "--unzip", action='store_true')
  parser.add_argument("-p", "--preserve_extracted_files", action='store_true')
  parser.add_argument("-w", "--whitelist", action="append", default=[])
  args = parser.parse_args()
  if len(args.target) < 2:
    parser.error("The number of targets has to be at least two.")
  if args.unzip:
    if args.preserve_extracted_files:
      main_with_zip(args.target, args)
    else:
      with tempfile.TemporaryDirectory() as tmpdir:
        target_in_tmp = [os.path.join(tmpdir, t) for t in args.target]
        for p in target_in_tmp:
          os.makedirs(p)
        main_with_zip(target_in_tmp, args)
  else:
    main(args.target, args.search_path, args.whitelist, args.ignore_signing_key)
