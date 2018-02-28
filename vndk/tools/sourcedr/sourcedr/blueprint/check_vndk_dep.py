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

from blueprint import RecursiveParser, evaluate_defaults


def _is_vndk(module):
    """Get the `vndk.enabled` module property."""
    try:
        return bool(module['vndk']['enabled'])
    except KeyError:
        return False


def _is_vndk_sp(module):
    """Get the `vndk.support_system_process` module property."""
    try:
        return bool(module['vndk']['support_system_process'])
    except KeyError:
        return False


def _is_vendor(module):
    """Get the `vendor` module property."""
    try:
        return module.get('vendor', False) or module.get('proprietary', False)
    except KeyError:
        return False


def _is_vendor_available(module):
    """Get the `vendor_available` module property."""
    try:
        return bool(module['vendor_available'])
    except KeyError:
        return False


def _has_vendor_variant(module):
    """Check whether the module is VNDK or vendor available."""
    return _is_vndk(module) or _is_vendor_available(module)


def _get_dependencies(module):
    """Get module dependencies."""

    shared_libs = set(module.get('shared_libs', []))
    static_libs = set(module.get('static_libs', []))
    header_libs = set(module.get('header_libs', []))

    try:
        target_vendor = module['target']['vendor']
        shared_libs -= set(target_vendor.get('exclude_shared_libs', []))
        static_libs -= set(target_vendor.get('exclude_static_libs', []))
        header_libs -= set(target_vendor.get('exclude_header_libs', []))
    except KeyError:
        pass

    return (sorted(shared_libs), sorted(static_libs), sorted(header_libs))


def _build_module_dict(modules):
    """Build module dictionaries that map module names to modules."""
    all_libs = {}
    llndk_libs = {}

    for rule, module in modules:
        name = module.get('name')
        if name is None:
            continue

        if rule == 'llndk_library':
            llndk_libs[name] = (rule, module)
        if rule in {'llndk_library', 'ndk_library'}:
            continue

        if rule.endswith('_library') or \
           rule.endswith('_library_shared') or \
           rule.endswith('_library_static') or \
           rule.endswith('_headers'):
            all_libs[name] = (rule, module)

        if rule == 'hidl_interface':
            all_libs[name] = (rule, module)
            all_libs[name + '-adapter-helper'] = (rule, module)
            module['vendor_available'] = True

    return (all_libs, llndk_libs)


def _check_module_deps(all_libs, llndk_libs, module):
    """Check the dependencies of a module."""

    bad_deps = set()
    shared_deps, static_deps, header_deps = _get_dependencies(module)

    # Check vendor module dependencies requirements.
    for dep_name in itertools.chain(shared_deps, static_deps, header_deps):
        if dep_name in llndk_libs:
            continue
        dep_module = all_libs[dep_name][1]
        if _is_vendor(dep_module):
            continue
        if _is_vendor_available(dep_module):
            continue
        if _is_vndk(dep_module) and not _is_vendor(module):
            continue
        bad_deps.add(dep_name)

    # Check VNDK dependencies requirements.
    if _is_vndk(module) and not _is_vendor(module):
        is_vndk_sp = _is_vndk_sp(module)
        for dep_name in shared_deps:
            if dep_name in llndk_libs:
                continue
            dep_module = all_libs[dep_name][1]
            if not _is_vndk(dep_module):
                # VNDK must be self-contained.
                bad_deps.add(dep_name)
                break
            if is_vndk_sp and not _is_vndk_sp(dep_module):
                # VNDK-SP must be self-contained.
                bad_deps.add(dep_name)
                break

    return bad_deps


def _check_modules_deps(modules):
    """Check the dependencies of modules."""

    all_libs, llndk_libs = _build_module_dict(modules)

    # Check the dependencies of modules
    all_bad_deps = []
    for name, (_, module) in all_libs.items():
        if not _has_vendor_variant(module) and not _is_vendor(module):
            continue

        bad_deps = _check_module_deps(all_libs, llndk_libs, module)

        if bad_deps:
            all_bad_deps.append((name, sorted(bad_deps)))

    return sorted(all_bad_deps)


def _parse_args():
    """Parse command line options."""
    parser = argparse.ArgumentParser()
    parser.add_argument('root_bp', help='android source tree root')
    return parser.parse_args()


def main():
    """Main function."""

    args = _parse_args()

    parser = RecursiveParser()
    parser.parse_file(args.root_bp)

    all_bad_deps = _check_modules_deps(evaluate_defaults(parser.modules))
    for name, bad_deps in all_bad_deps:
        print('ERROR: {!r} must not depend on {}'.format(name, bad_deps),
              file=sys.stderr)

    if all_bad_deps:
        sys.exit(1)


if __name__ == '__main__':
    main()
