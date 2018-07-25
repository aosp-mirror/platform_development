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
import collections
import os
import re
import sys
import xml.dom.minidom

from blueprint import RecursiveParser, evaluate_defaults, fill_module_namespaces


_GROUPS = ['system_only', 'vendor_only', 'both']


def parse_manifest_xml(manifest_path):
    """Build a dictionary that maps directories into projects."""
    dir_project_dict = {}
    parsed_xml = xml.dom.minidom.parse(manifest_path)
    projects = parsed_xml.getElementsByTagName('project')
    for project in projects:
        name = project.getAttribute('name')
        path = project.getAttribute('path')
        if path:
            dir_project_dict[path] = name
        else:
            dir_project_dict[name] = name
    return dir_project_dict


class DirProjectMatcher(object):
    def __init__(self, dir_project_dict):
        self._projects = sorted(dir_project_dict.items(), reverse=True)
        self._matcher = re.compile(
            '|'.join('(' + re.escape(path) + '(?:/|$))'
                     for path, project in self._projects))

    def find(self, path):
        match = self._matcher.match(path)
        if match:
            return self._projects[match.lastindex - 1][1]
        return None


def parse_blueprint(root_bp_path):
    """Parse Android.bp files."""
    parser = RecursiveParser()
    parser.parse_file(root_bp_path)
    parsed_items = evaluate_defaults(parser.modules)
    return fill_module_namespaces(root_bp_path, parsed_items)


def _get_property(attrs, *names, **kwargs):
    try:
        result = attrs
        for name in names:
            result = result[name]
        return result
    except KeyError:
        return kwargs.get('default', None)


class GitProject(object):
    def __init__(self):
        self.system_only = set()
        self.vendor_only = set()
        self.both = set()

    def add_module(self, path, rule, attrs):
        name = _get_property(attrs, 'name')
        ent = (rule, path, name)

        if rule in {'llndk_library', 'hidl_interface'}:
            self.both.add(ent)
        elif rule.endswith('_binary') or \
             rule.endswith('_library') or \
             rule.endswith('_library_shared') or \
             rule.endswith('_library_static') or \
             rule.endswith('_headers'):
            if _get_property(attrs, 'vendor') or \
               _get_property(attrs, 'proprietary') or \
               _get_property(attrs, 'soc_specific') or \
               _get_property(attrs, 'device_specific'):
                self.vendor_only.add(ent)
            elif _get_property(attrs, 'vendor_available') or \
                 _get_property(attrs, 'vndk', 'enabled'):
                self.both.add(ent)
            else:
                self.system_only.add(ent)

    def __repr__(self):
        return ('GitProject(' +
                'system_only=' + repr(self.system_only) + ', '
                'vendor_only=' + repr(self.vendor_only) + ', '
                'both=' + repr(self.both) + ')')


def _parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-b', '--blueprint', required=True,
                        help='Path to root Android.bp')
    parser.add_argument('-m', '--manifest', required=True,
                        help='Path to repo manifest xml file')
    group = parser.add_mutually_exclusive_group()
    group.add_argument('--skip-no-overlaps', action='store_true',
                       help='Skip projects without overlaps')
    group.add_argument('--has-group', choices=_GROUPS,
                       help='List projects that some modules are in the group')
    group.add_argument('--only-has-group', choices=_GROUPS,
                       help='List projects that all modules are in the group')
    group.add_argument('--without-group', choices=_GROUPS,
                       help='List projects that no modules are in the group')
    return parser.parse_args()


def _dump_module_set(name, modules):
    if not modules:
        return
    print('\t' + name)
    for rule, path, name in sorted(modules):
        print('\t\t' + rule, path, name)


def main():
    args = _parse_args()

    # Load repo manifest xml file
    dir_matcher = DirProjectMatcher(parse_manifest_xml(args.manifest))

    # Classify Android.bp modules
    git_projects = collections.defaultdict(GitProject)

    root_dir = os.path.dirname(os.path.abspath(args.blueprint))
    root_prefix_len = len(root_dir) + 1

    has_error = False

    for rule, attrs in parse_blueprint(args.blueprint):
        path = _get_property(attrs, '_path')[root_prefix_len:]
        project = dir_matcher.find(path)
        if project is None:
            print('error: Path {!r} does not belong to any git projects.'
                  .format(path), file=sys.stderr)
            has_error = True
            continue
        git_projects[project].add_module(path, rule, attrs)

    # Print output
    total_projects = 0
    for project, modules in sorted(git_projects.items()):
        if args.skip_no_overlaps:
            if (int(len(modules.system_only) > 0) +
                int(len(modules.vendor_only) > 0) +
                int(len(modules.both) > 0)) <= 1:
                continue
        elif args.has_group:
            if not getattr(modules, args.has_group):
                continue
        elif args.only_has_group:
            if any(getattr(modules, group)
                   for group in _GROUPS if group != args.only_has_group):
                continue
            if not getattr(modules, args.only_has_group):
                continue
        elif args.without_group:
            if getattr(modules, args.without_group):
                continue

        print(project, len(modules.system_only), len(modules.vendor_only),
              len(modules.both))
        _dump_module_set('system_only', modules.system_only)
        _dump_module_set('vendor_only', modules.vendor_only)
        _dump_module_set('both', modules.both)

        total_projects += 1

    print('Total:', total_projects)

    if has_error:
        sys.exit(2)

if __name__ == '__main__':
    main()
