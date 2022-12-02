#!/usr/bin/env python3
#
# Copyright (C) 2020 The Android Open Source Project
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
"""Fetch a Rust package from crates.io.

Usage: get_rust_pkg.py -v syn-1.0.7
Get the package syn-1.0.7 from crates.io and untar it into ./syn-1.0.7.

Usage: get_rust_pkg.py -v -o tmp syn
Get the latest version of package syn, say 1.0.17,
and untar it into tmp/syn-1.0.17.

Usage: get_rust_pkg.py -show bindgen cxx
Count dependent packages of bindgen and cxx.

When downloading a package, its target directory should not exist,
or the download will be skipped.
"""

import argparse
import functools
import json
import os
import re
import shutil
import sys
import tarfile
import tempfile
import urllib.request

PKG_VERSION_PATTERN = r"(.*)-([0-9]+\.[0-9]+\.[0-9]+.*)"

PKG_VERSION_MATCHER = re.compile(PKG_VERSION_PATTERN)

VERSION_PATTERN = r"([0-9]+)\.([0-9]+)\.([0-9]+)"

VERSION_MATCHER = re.compile(VERSION_PATTERN)


def parse_args():
  """Parse main arguments."""
  parser = argparse.ArgumentParser("get_rust_pkg")
  parser.add_argument(
      "-v", action="store_true", default=False, help="echo executed commands")
  parser.add_argument(
      "-o", metavar="out_dir", default=".", help="output directory")
  parser.add_argument(
      "-add3prf", "--add3prf",
      action="store_true",
      default=False,
      help="call add3prf.py to add third party review files")
  parser.add_argument(
      "-show", "--show",
      action="store_true",
      default=False,
      help="show all default dependent packages, using crates.io api")
  parser.add_argument(
      dest="pkgs",
      metavar="pkg_name",
      nargs="+",
      help="name of Rust package to be fetched from crates.io")
  return parser.parse_args()


def set2list(a_set):
  return (" " + " ".join(sorted(a_set))) if a_set else ""


def echo(args, msg):
  if args.v:
    print("INFO: {}".format(msg), flush=True)


def echo_all_deps(args, kind, deps):
  if args.v and deps:
    print("INFO: now {} in {}:{}".format(len(deps), kind, set2list(deps)))


def pkg_base_name(pkg):
  match = PKG_VERSION_MATCHER.match(pkg)
  if match is not None:
    return (match.group(1), match.group(2))
  else:
    return (pkg, "")


def get_version_numbers(version):
  match = VERSION_MATCHER.match(version)
  if match is not None:
    return tuple(int(match.group(i)) for i in range(1, 4))
  return (0, 0, 0)


def is_newer_version(args, prev_version, prev_id, check_version, check_id):
  """Return true if check_version+id is newer than prev_version+id."""
  echo(args, "checking version={}  id={}".format(check_version, check_id))
  return ((get_version_numbers(check_version), check_id) >
          (get_version_numbers(prev_version), prev_id))


def get_max_version(pkg):
  """Ask crates.io for a pkg's latest stable version."""
  url = "https://crates.io/api/v1/crates/" + pkg
  with urllib.request.urlopen(url) as request:
    data = json.loads(request.read().decode())
    return data["crate"]["max_stable_version"]


def find_dl_path(args, name):
  """Ask crates.io for the latest stable version download path."""
  base_name, version = pkg_base_name(name)
  if not version:
    version = get_max_version(name)
  url = "https://crates.io/api/v1/crates/{}/{}".format(base_name, version)
  echo(args, "try to get dl_path from {}".format(url))
  with urllib.request.urlopen(url) as request:
    data = json.loads(request.read().decode())
    if "version" not in data or "dl_path" not in data["version"]:
      print("ERROR: cannot find version {} of package {}".format(
          version, base_name))
      return None
    echo(args, "found download path for version {}".format(version))
    return data["version"]["dl_path"]


def fetch_pkg(args, pkg, dl_path):
  """Fetch package from crates.io and untar it into a subdirectory."""
  if not dl_path:
    print("ERROR: cannot find download path for '{}'".format(pkg))
    return False
  url = "https://crates.io" + dl_path
  tmp_dir = tempfile.mkdtemp()
  echo(args, "fetch tar file from {}".format(url))
  tar_file, _ = urllib.request.urlretrieve(url)
  with tarfile.open(tar_file, mode="r") as tfile:
    echo(args, "extract tar file {} into {}".format(tar_file, tmp_dir))
    tfile.extractall(tmp_dir)
    files = os.listdir(tmp_dir)
    # There should be only one directory in the tar file,
    # but it might not be (name + "-" + version)
    pkg_tmp_dir = os.path.join(tmp_dir, files[0])
    echo(args, "untared package in {}".format(pkg_tmp_dir))
    dest_dir = os.path.join(args.o, files[0])
    if os.path.exists(dest_dir):
      print("ERROR: do not overwrite existing {}".format(dest_dir))
      return False  # leave tar_file and tmp_dir
    else:
      echo(args, "move {} to {}".format(pkg_tmp_dir, dest_dir))
      shutil.move(pkg_tmp_dir, dest_dir)
  echo(args, "delete downloaded tar file {}".format(tar_file))
  os.remove(tar_file)
  echo(args, "delete temp directory {}".format(tmp_dir))
  shutil.rmtree(tmp_dir)
  print("SUCCESS: downloaded package to '{}'".format(dest_dir))
  if args.add3prf:
    echo(args, "Calling add3prf.py in {}".format(dest_dir))
    cwd = os.getcwd()
    os.chdir(dest_dir)
    os.system("add3prf.py")
    os.chdir(cwd)
  return True


def get_crate_dependencies(args, pkg):
  """Ask crates.io for pkg's dependencies."""
  echo(args, "Ask crates.io for {} ...".format(pkg))
  try:
    url = "https://crates.io/api/v1/crates/{}/{}/dependencies".format(
        pkg, get_max_version(pkg))
    with urllib.request.urlopen(url) as request:
      data = json.loads(request.read().decode())
  except urllib.error.HTTPError:
    print("ERROR: failed to find {}".format(pkg))
    return False, None, None
  build_deps = set()
  dev_deps = set()
  for crate in data["dependencies"]:
    if not crate["optional"]:  # some package has a lot of optional features
      # dev_deps is a super set of build_deps
      dev_deps.add(crate["crate_id"])
      if crate["kind"] != "dev":
        build_deps.add(crate["crate_id"])
  return True, build_deps, dev_deps


def compare_pkg_deps(pkg1, pkg2):
  """Compare dependency order of pkg1 and pkg2."""
  base1, build_deps1, dev_deps1 = pkg1
  base2, build_deps2, dev_deps2 = pkg2
  # Some pkg1 can be build-dependent (non-dev-dependent) on pkg2,
  # when pkg2 is only test-dependent (dev-dependent) on pkg1.
  # This is not really a build dependency cycle, because pkg2
  # can be built before pkg1, but tested after pkg1.
  # So the dependency order is based on build_deps first, and then dev_deps.
  if base1 in build_deps2:
    return -1  # pkg2 needs base1
  if base2 in build_deps1:
    return 1  # pkg1 needs base2
  if base1 in dev_deps2:
    return -1  # pkg2 needs base1
  if base2 in dev_deps1:
    return 1  # pkg1 needs base2
  # If there is no dependency between pkg1 and pkg2,
  # order them by the size of build_deps or dev_deps, or the name.
  count1 = (len(build_deps1), len(dev_deps1), base1)
  count2 = (len(build_deps2), len(dev_deps2), base2)
  if count1 != count2:
    return -1 if count1 < count2 else 1
  return 0


def sort_found_pkgs(tuples):
  """A topological sort of tuples based on build_deps."""
  # tuples is a list of (base_name, build_deps, dev_deps)

  # Use build_deps as the dependency relation in a topological sort.
  # The new_tuples list is used in topological sort. It is the input tuples
  # prefixed with a changing build_deps during the sorting process.
  # Collect all package base names.
  # Dependent packages not found in all_base_names will be treated as
  # "external" and ignored in topological sort.
  all_base_names = set(map(lambda t: t[0], tuples))
  new_tuples = []
  all_names = set()
  for (base_name, build_deps, dev_deps) in tuples:
    new_tuples.append((build_deps, (base_name, build_deps, dev_deps)))
    all_names = all_names.union(build_deps)
  external_names = all_names.difference(all_base_names)
  new_tuples = list(
      map(lambda t: (t[0].difference(external_names), t[1]), new_tuples))

  sorted_tuples = []
  # A brute force topological sort;
  # tuples with empty build_deps are put before the others.
  while new_tuples:
    first_group = list(filter(lambda t: not t[0], new_tuples))
    other_group = list(filter(lambda t: t[0], new_tuples))
    new_tuples = []
    if first_group:
      # Remove the extra build_deps in first_group,
      # then sort it, and add its tuples to the sorted_tuples list.
      first_group = list(map(lambda t: t[1], first_group))
      first_group.sort(key=functools.cmp_to_key(compare_pkg_deps))
      sorted_tuples.extend(first_group)
      # Copy other_group to new_tuples but remove names in the first_group.
      base_names = set(map(lambda t: t[0], first_group))
      new_tuples = list(
          map(lambda t: (t[0].difference(base_names), t[1]), other_group))
    else:
      # There is a bug, or a cycle in the build_deps.
      # If we include all optional dependent packages into build_deps,
      # we will see one cycle: futures-util has an optional dependent
      # on futures, which has a normal dependent on futures-util.
      print("ERROR: leftover in topological sort: {}".format(
          list(map(lambda t: t[1][1], other_group))))
      # Anyway, sort the other_group to include them into final report.
      other_group = list(map(lambda t: t[1], other_group))
      other_group.sort(key=functools.cmp_to_key(compare_pkg_deps))
      sorted_tuples.extend(other_group)
  return sorted_tuples


def show_all_dependencies(args, found_pkgs):
  """Topological sort found_pkgs and report number of dependent packages."""
  found_pkgs = sort_found_pkgs(found_pkgs)
  max_length = functools.reduce(lambda a, t: max(a, len(t[0])), found_pkgs, 1)
  name_format = "{:" + str(max_length) + "s}"
  print("\n##### Summary of all dependent package counts #####")
  pattern = "{:>9s}_deps[k] = # of {}dependent packages of {}"
  for (prefix, suffix) in [("    ", "pkg[k]"), ("all_", "pkg[1] to pkg[k]")]:
    for (name, kind) in [("build", "non-dev-"), ("dev", "all ")]:
      print(pattern.format(prefix + name, kind, suffix))
  print(("{:>4s} " + name_format + " {:>10s} {:>10s} {:>14s} {:>14s}").format(
      "k", "pkg", "build_deps", "dev_deps", "all_build_deps", "all_dev_deps"))
  all_pkgs = set()
  all_build_deps = set()
  all_dev_deps = set()
  k = 0
  prev_all_build_deps = set()
  prev_all_dev_deps = set()
  for (pkg, build_deps, dev_deps) in found_pkgs:
    all_pkgs.add(pkg)
    all_build_deps = all_build_deps.union(build_deps).difference(all_pkgs)
    all_dev_deps = all_dev_deps.union(dev_deps).difference(all_pkgs)
    k += 1
    print(("{:4d} " + name_format + " {:10d} {:10d} {:14d} {:14d}").format(
        k, pkg, len(build_deps), len(dev_deps), len(all_build_deps),
        len(all_dev_deps)))
    if prev_all_build_deps != all_build_deps:
      echo_all_deps(args, "all_build_deps", all_build_deps)
      prev_all_build_deps = all_build_deps
    if prev_all_dev_deps != all_dev_deps:
      echo_all_deps(args, "all_dev_deps", all_dev_deps)
      prev_all_dev_deps = all_dev_deps
  print("\nNOTE: from all {} package(s):{}".format(
      len(all_pkgs), set2list(all_pkgs)))
  for (kind, deps) in [("non-dev-", all_build_deps), ("", all_dev_deps)]:
    if deps:
      print("NOTE: found {:3d} other {}dependent package(s):{}".format(
          len(deps), kind, set2list(deps)))


def crates_io_find_pkgs(args, pkgs, found_pkgs):
  """Call crates.io api to find direct dependent packages."""
  success = True
  for pkg in sorted(pkgs):
    ok, build_deps, dev_deps = get_crate_dependencies(args, pkg)
    if not ok:
      success = False
    else:
      found_pkgs.append((pkg, build_deps, dev_deps))
  return success


def add_non_dev_dependencies(args, all_deps, core_pkgs, visited, pkg):
  """Add reachable non-dev dependencies to all_deps[pkg]'s dependencies."""
  if pkg not in all_deps:
    ok, build_deps, dev_deps = get_crate_dependencies(args, pkg)
    if not ok:
      return set()
    all_deps[pkg] = (pkg, build_deps, dev_deps)
  else:
    (_, build_deps, dev_deps) = all_deps[pkg]
  if pkg in visited:
    return build_deps
  visited.add(pkg)

  for p in sorted(build_deps):
    if p not in core_pkgs and pkg in core_pkgs:
      core_pkgs.add(p)
    deps = add_non_dev_dependencies(args, all_deps, core_pkgs, visited, p)
    build_deps = build_deps.union(deps)
  if pkg in core_pkgs:
    for p in sorted(dev_deps):
      deps = add_non_dev_dependencies(args, all_deps, core_pkgs, visited, p)
      dev_deps = dev_deps.union(deps)
  all_deps[pkg] = (pkg, build_deps, dev_deps)
  return build_deps


def add_indirect_build_deps(args, found_pkgs):
  """Add all indirect dependencies and return a new found_pkgs."""
  all_deps = dict(map(lambda t: (t[0], t), found_pkgs))
  core_pkgs = set(map(lambda t: t[0], found_pkgs))
  dev_pkgs = set()
  dump_pkgs(args, "BEFORE", all_deps, core_pkgs, dev_pkgs)
  visited = set()
  for pkg in sorted(core_pkgs):
    add_non_dev_dependencies(args, all_deps, core_pkgs, visited, pkg)
  # Revisit core packages to add build_deps into core_pkgs and other deps.
  check_again = True
  while check_again:
    pattern = "Revisiting {} core packages:{}"
    echo(args, pattern.format(len(core_pkgs), set2list(core_pkgs)))
    check_again = False
    visited = set()
    num_core_pkgs = len(core_pkgs)
    for pkg in sorted(core_pkgs):
      (_, build_deps, dev_deps) = all_deps[pkg]
      (num_build_deps, num_dev_deps) = (len(build_deps), len(dev_deps))
      add_non_dev_dependencies(args, all_deps, core_pkgs, visited, pkg)
      (_, build_deps, dev_deps) = all_deps[pkg]
      (new_num_build_deps, new_num_dev_deps) = (len(build_deps), len(dev_deps))
      # If build_deps, dev_deps, or core_pkgs was changed, check again.
      if (num_build_deps != new_num_build_deps or
          num_dev_deps != new_num_dev_deps or num_core_pkgs != len(core_pkgs)):
        check_again = True
  dev_pkgs = visited.difference(core_pkgs)
  dump_pkgs(args, "AFTER", all_deps, core_pkgs, dev_pkgs)
  found_pkgs = list(map(lambda p: all_deps[p], core_pkgs))
  found_pkgs.sort(key=lambda t: t[0])
  return found_pkgs


def echo_dump_found_pkgs(args, msg, all_deps, pkgs):
  if not args.v or not pkgs:
    return
  echo(args, msg)
  for pkg in sorted(pkgs):
    (_, build_deps, dev_deps) = all_deps[pkg]
    for (name, deps) in [("normal", build_deps), ("dev", dev_deps)]:
      pattern = "  {} has {} " + name + " deps:{}"
      echo(args, pattern.format(pkg, len(deps), set2list(deps)))


def dump_pkgs(args, msg, all_deps, core_pkgs, dev_pkgs):
  echo_dump_found_pkgs(args, msg + " core_pkgs:", all_deps, core_pkgs)
  echo_dump_found_pkgs(args, msg + " other_dev_pkgs:", all_deps, dev_pkgs)


def show_dependencies(args):
  """Calls crates.io api to find dependent packages; returns True on success."""
  all_pkgs = set(map(lambda p: pkg_base_name(p)[0], args.pkgs))
  if "" in all_pkgs:
    # TODO(chh): detect and report ill formed names in args.pkgs
    print("WARNING: skip some ill formatted pkg arguments.")
    all_pkgs = all_pkgs.remove("")
  if not all_pkgs:
    print("ERROR: no valid pkg names to show dependencies.")
    return False
  pkgs = sorted(all_pkgs)
  found_pkgs = []
  success = crates_io_find_pkgs(args, pkgs, found_pkgs)
  if not found_pkgs:
    return False
  # All normal (non-dev) dependent packages will be added into found_pkgs.
  found_pkgs = add_indirect_build_deps(args, found_pkgs)
  show_all_dependencies(args, found_pkgs)
  return success


def main():
  args = parse_args()
  if args.show:
    # only show dependencies, not to fetch any package
    if not show_dependencies(args):
      sys.exit(2)
    return
  echo(args, "to fetch packages = {}".format(args.pkgs))
  success = True
  for pkg in args.pkgs:
    echo(args, "trying to fetch package '{}'".format(pkg))
    try:
      if not fetch_pkg(args, pkg, find_dl_path(args, pkg)):
        success = False
    except urllib.error.HTTPError:
      print("ERROR: unknown package '{}'".format(pkg))
      success = False
  if not success:
    sys.exit(1)


if __name__ == "__main__":
  main()
