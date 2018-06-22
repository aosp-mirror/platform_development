#!/usr/bin/env python3

import argparse
import itertools
import json
import posixpath
import re


def match_any(regex, iterable):
    """Check whether any element in iterable matches regex."""
    return any(regex.match(elem) for elem in iterable)


class ModuleInfo(object):
    def __init__(self, module_info_path):
        with open(module_info_path, 'r') as module_info_file:
            self._json = json.load(module_info_file)


    def list(self, installed_filter=None, module_definition_filter=None):
        for name, info in self._json.items():
            installs = info['installed']
            paths = info['path']

            if installed_filter and not match_any(installed_filter, installs):
                    continue
            if module_definition_filter and \
               not match_any(module_definition_filter, paths):
                    continue

            for install, path in itertools.product(installs, paths):
                yield (install, path)


def _parse_args():
    """Parse command line arguments"""

    parser = argparse.ArgumentParser()

    parser.add_argument('module_info', help='Path to module-info.json')

    parser.add_argument('--out-dir', default='out',
                        help='Android build output directory')

    parser.add_argument('--installed-filter',
                        help='Installation filter (regular expression)')

    parser.add_argument('--module-definition-filter',
                        help='Module definition filter (regular expression)')

    return parser.parse_args()


def main():
    """Main function"""

    args = _parse_args()

    installed_filter = None
    if args.installed_filter:
        installed_filter = re.compile(
            re.escape(posixpath.normpath(args.out_dir)) + '/' +
            '(?:' + args.installed_filter + ')')

    module_definition_filter = None
    if args.module_definition_filter:
        module_definition_filter = re.compile(args.module_definition_filter)

    module_info = ModuleInfo(args.module_info)

    for installed_file, module_path in \
            module_info.list(installed_filter, module_definition_filter):
        print(installed_file, module_path)


if __name__ == '__main__':
    main()
