#!/usr/bin/env python3

#
# Copyright (C) 2018 The Android Open Source Project
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
#

from __future__ import print_function

import argparse
import csv
import itertools
import os
import re
import sys

import vndk


def _parse_args():
    """Parse command line options."""
    parser = argparse.ArgumentParser()
    parser.add_argument('root_bp',
                        help='path to Android.bp in ANDROID_BUILD_TOP')
    parser.add_argument('-o', '--output', help='path to output file')
    parser.add_argument('--exclude',
                        help='regular expression for the excluded directories')
    parser.add_argument('--select',
                        help='regular expression for the selected directories')
    parser.add_argument('--namespace', action='append', default=[''],
                        help='extra module namespaces')
    return parser.parse_args()


def print_vndk_module_csv(output_file, module_dicts, root_dir, exclude, select):
    """Print vndk module list to output file."""

    all_libs = module_dicts.all_libs
    vndk_libs = module_dicts.vndk_libs
    vndk_sp_libs = module_dicts.vndk_sp_libs
    vendor_available_libs = module_dicts.vendor_available_libs

    module_names = sorted(set(
        itertools.chain(vndk_libs, vndk_sp_libs, vendor_available_libs)))

    root_dir_prefix_len = len(root_dir) + 1

    writer = csv.writer(output_file, lineterminator='\n')
    writer.writerow(('name', 'vndk', 'vndk_sp', 'vendor_available', 'rule'))
    for name in module_names:
        rule = all_libs[name].rule
        path = all_libs[name].get_property('_path')[root_dir_prefix_len:]
        if select and not select.match(path):
            continue
        if exclude and exclude.match(path):
            continue
        if '_header' not in rule and '_static' not in rule and \
                rule != 'toolchain_library':
            writer.writerow((name,
                             name in vndk_libs,
                             name in vndk_sp_libs,
                             name in vendor_available_libs,
                             rule))


def main():
    """Main function."""

    args = _parse_args()

    # Convert select/exclude regular expressions
    select = re.compile(args.select) if args.select else None
    exclude = re.compile(args.exclude) if args.exclude else None

    # Parse Blueprint files and get VNDK libs
    module_dicts = vndk.ModuleClassifier.create_from_root_bp(
        args.root_bp, args.namespace)

    root_dir = os.path.dirname(args.root_bp)

    if args.output:
        with open(args.output, 'w') as output_file:
            print_vndk_module_csv(
                output_file, module_dicts, root_dir, exclude, select)
    else:
        print_vndk_module_csv(
            sys.stdout, module_dicts, root_dir, exclude, select)


if __name__ == '__main__':
    main()
