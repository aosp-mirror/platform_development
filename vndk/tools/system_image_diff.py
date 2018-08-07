#!/usr/bin/env python3

import argparse
import csv
import glob
import json
import os
import sys

HELP_MSG = '''
This script computes the differences between two system images (system1 -
system2), and lists the files grouped by package. The difference is just based
on the existence of the file, not on its contents.
'''

VENDOR_PATH_MAP = {
    'vendor/google' : 'Google',
    'vendor/unbundled_google': 'Google',
    'vendor/verizon' : 'Verizon',
    'vendor/qcom' : 'Qualcomm',
    'vendor/tmobile' : 'TMobile',
    'vendor/mediatek' : 'Mediatek',
    'vendor/htc' : 'HTC',
    'vendor/realtek' : 'Realtek'
}

def system_files(path):
  """Returns an array of the files under /system, recursively, and ignoring
  symbolic-links"""
  system_files = []
  system_prefix = os.path.join(path, 'system')
  # Skip trailing '/'
  system_prefix_len = len(system_prefix) + 1

  for root, dirs, files in os.walk(system_prefix, topdown=True):
    for file in files:
      # Ignore symbolic links.
      if not os.path.islink(os.path.join(root, file)):
        system_files.append(os.path.join(root[system_prefix_len:], file))

  return system_files

def system_files_to_package_map(path):
  """Returns a dictionary mapping from each file in the /system partition to its
  package, according to modules-info.json."""
  system_files_to_package_map = {}
  system_prefix = os.path.join(path, 'system')
  # Skip trailing '/'
  system_prefix_len = len(system_prefix) + 1

  with open(os.path.join(path, 'module-info.json')) as module_info_json:
    module_info = json.load(module_info_json)
    for module in module_info:
      installs = module_info[module]['installed']
      for install in installs:
        if install.startswith(system_prefix):
          system_file = install[system_prefix_len:]
          # Not clear if collisions can ever happen in modules-info.json (e.g.
          # the same file installed by multiple packages), but it doesn't hurt
          # to check.
          if system_file in system_files_to_package_map:
            system_files_to_package_map[system_file] = "--multiple--"
          else:
            system_files_to_package_map[system_file] = module

  return system_files_to_package_map

def package_to_vendor_map(path):
  """Returns a dictionary mapping from each package in modules-info.json to its
  vendor. If a vendor cannot be found, it maps to "--unknown--". Those cases
  are:

    1. The package maps to multiple modules (e.g., one in APPS and one in
       SHARED_LIBRARIES.
    2. The path to the module is not one of the recognized vendor paths in
       VENDOR_PATH_MAP."""
  package_vendor_map = {}
  system_prefix = os.path.join(path, 'system')
  # Skip trailing '/'
  system_prefix_len = len(system_prefix) + 1
  vendor_prefixes = VENDOR_PATH_MAP.keys()

  with open(os.path.join(path, 'module-info.json')) as module_info_json:
    module_info = json.load(module_info_json)
    for module in module_info:
      paths = module_info[module]['path']
      vendor = ""
      if len(paths) == 1:
        path = paths[0]
        for prefix in vendor_prefixes:
          if path.startswith(prefix):
            vendor = VENDOR_PATH_MAP[prefix]
            break
        if vendor == "":
          vendor = "--unknown--"
      else:
        vendor = "--multiple--"
      package_vendor_map[module] = vendor

  return package_vendor_map

def main():
  parser = argparse.ArgumentParser(description=HELP_MSG)
  parser.add_argument("out1", help="First $OUT directory")
  parser.add_argument("out2", help="Second $OUT directory")
  args = parser.parse_args()

  system_files1 = system_files(args.out1)
  system_files2 = system_files(args.out2)
  system_files_diff = set(system_files1) - set(system_files2)

  system_files_map = system_files_to_package_map(args.out1)
  package_vendor_map = package_to_vendor_map(args.out1)
  packages = {}

  for file in system_files_diff:
    if file in system_files_map:
      package = system_files_map[file]
    else:
      package = "--unknown--"

    if package in packages:
      packages[package].append(file)
    else:
      packages[package] = [file]

  writer = csv.writer(sys.stdout, quoting = csv.QUOTE_NONNUMERIC,
                      delimiter = ',', lineterminator = '\n')
  for package, files in packages.items():
    for file in files:
      if package in package_vendor_map:
        vendor = package_vendor_map[package]
      else:
        vendor = "--unknown--"
      # Get file size
      full_path = os.path.join(args.out1, 'system', file)
      size = os.stat(full_path).st_size
      writer.writerow([vendor, package, file, size])

if __name__ == '__main__':
  main()
