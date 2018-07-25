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

"""This script scans all Android.bp in an android source tree and check the
correctness of dependencies."""

from __future__ import print_function

import argparse
import itertools
import sys

import vndk


def _check_module_deps(all_libs, llndk_libs, module):
    """Check the dependencies of a module."""

    bad_deps = set()
    shared_deps, static_deps, header_deps = module.get_dependencies()

    # Check vendor module dependencies requirements.
    for dep_name in itertools.chain(shared_deps, static_deps, header_deps):
        if dep_name in llndk_libs:
            continue
        dep_module = all_libs[dep_name]
        if dep_module.is_vendor():
            continue
        if dep_module.is_vendor_available():
            continue
        if dep_module.is_vndk():
            # dep_module is a VNDK-Private module.
            if not module.is_vendor():
                # VNDK-Core may link to VNDK-Private.
                continue
        bad_deps.add(dep_name)

    # Check VNDK dependencies requirements.
    if module.is_vndk() and not module.is_vendor():
        is_vndk_sp = module.is_vndk_sp()
        for dep_name in shared_deps:
            if dep_name in llndk_libs:
                continue
            dep_module = all_libs[dep_name]
            if not dep_module.is_vndk():
                # VNDK must be self-contained.
                bad_deps.add(dep_name)
                break
            if is_vndk_sp and not dep_module.is_vndk_sp():
                # VNDK-SP must be self-contained.
                bad_deps.add(dep_name)
                break

    return bad_deps


def _check_modules_deps(module_dicts):
    """Check the dependencies of modules."""

    all_libs = module_dicts.all_libs
    llndk_libs = module_dicts.llndk_libs

    # Check the dependencies of modules
    all_bad_deps = []
    for name, module in all_libs.items():
        if not module.has_vendor_variant() and not module.is_vendor():
            continue

        bad_deps = _check_module_deps(all_libs, llndk_libs, module)

        if bad_deps:
            all_bad_deps.append((name, sorted(bad_deps)))

    return sorted(all_bad_deps)


def _parse_args():
    """Parse command line options."""
    parser = argparse.ArgumentParser()
    parser.add_argument('root_bp',
                        help='path to Android.bp in ANDROID_BUILD_TOP')
    parser.add_argument('--namespace', action='append', default=[''],
                        help='extra module namespaces')
    return parser.parse_args()


def main():
    """Main function."""

    args = _parse_args()

    module_dicts = vndk.ModuleClassifier.create_from_root_bp(
        args.root_bp, args.namespace)

    all_bad_deps = _check_modules_deps(module_dicts)
    for name, bad_deps in all_bad_deps:
        print('ERROR: {!r} must not depend on {}'.format(name, bad_deps),
              file=sys.stderr)

    if all_bad_deps:
        # Note: Exit with 2 so that it is easier to distinguish bad
        # dependencies from unexpected Python exceptions.
        sys.exit(2)


if __name__ == '__main__':
    main()
