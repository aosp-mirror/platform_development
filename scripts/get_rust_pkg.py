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

This script will abort if the target directory exists.
"""

import argparse
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


def parse_args():
  """Parse main arguments."""
  parser = argparse.ArgumentParser("get_rust_pkg")
  parser.add_argument(
      "-v", action="store_true", default=False,
      help="echo executed commands")
  parser.add_argument(
      "-o", metavar="out_dir", default=".",
      help="output directory")
  parser.add_argument(
      dest="pkg", metavar="pkg_name",
      help="name of Rust package to be fetched from crates.io")
  return parser.parse_args()


def echo(args, msg):
  if args.v:
    print("INFO: {}".format(msg))


def pkg_base_name(args, name):
  """Remove version string of name."""
  base = name
  version = ""
  match = PKG_VERSION_MATCHER.match(name)
  if match is not None:
    base = match.group(1)
    version = match.group(2)
  echo(args, "package base name: {}  version: {}".format(base, version))
  return base, version


def find_dl_path(args, name):
  """Ask crates.io for the latest version download path."""
  base_name, version = pkg_base_name(args, name)
  url = "https://crates.io/api/v1/crates/{}/versions".format(base_name)
  echo(args, "get versions at {}".format(url))
  with urllib.request.urlopen(url) as request:
    data = json.loads(request.read().decode())
    versions = data["versions"]
    # version with the largest id number is assumed to be the latest
    last_id = 0
    dl_path = ""
    for v in versions:
      if version == v["num"]:
        dl_path = v["dl_path"]
        break
      if not version and int(v["id"]) > last_id:
        last_id = int(v["id"])
        version = v["num"]
        dl_path = v["dl_path"]
    if not dl_path:
      print("ERROR: cannot find version {} of package {}"
            .format(version, base_name))
      sys.exit(1)
    echo(args, "found download path for version {}".format(version))
    return dl_path


def fetch_pkg(args, dl_path):
  """Fetch package from crates.io and untar it into a subdirectory."""
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
      sys.exit(1)  # leave tar_file and tmp_dir
    else:
      echo(args, "move {} to {}".format(pkg_tmp_dir, dest_dir))
      shutil.move(pkg_tmp_dir, dest_dir)
  echo(args, "delete downloaded tar file {}".format(tar_file))
  os.remove(tar_file)
  echo(args, "delete temp directory {}".format(tmp_dir))
  shutil.rmtree(tmp_dir)
  print("SUCCESS: downloaded package in {}".format(dest_dir))


def main():
  args = parse_args()
  fetch_pkg(args, find_dl_path(args, args.pkg))


if __name__ == "__main__":
  main()
