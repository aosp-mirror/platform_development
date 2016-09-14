#!/usr/bin/env python
#
# Copyright (C) 2016 The Android Open Source Project
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
"""Builds a database of symbol version introductions."""
import argparse
import json
import logging
import os


THIS_DIR = os.path.realpath(os.path.dirname(__file__))


ALL_ARCHITECTURES = (
    'arm',
    'arm64',
    'mips',
    'mips64',
    'x86',
    'x86_64',
)


def logger():
    """Returns the default logger for this module."""
    return logging.getLogger(__name__)


def get_platform_versions():
    """Returns a list of the platform versions we have data for."""
    versions = []
    platforms_dir = os.path.join(THIS_DIR, 'platforms')
    logger().debug('Getting platform versions from %s', platforms_dir)
    for name in os.listdir(platforms_dir):
        if name.startswith('android-'):
            versions.append(int(name.split('-')[1]))
    return versions


def add_symbols(symbols, symbol_file_path, version, arch, is_var):
    """Adds symbols from a file to the symbol dict."""
    with open(symbol_file_path) as symbol_file:
        names = symbol_file.readlines()

    for name in names:
        name = name.strip()
        if not name:
            continue
        introduced_tag = 'introduced-' + arch
        if name in symbols:
            assert symbols[name]['is_var'] == is_var
            if introduced_tag in symbols[name]:
                continue
            symbols[name][introduced_tag] = version
        else:
            symbols[name] = {}
            symbols[name]['is_var'] = is_var
            symbols[name][introduced_tag] = version


def build_symbol_db(lib_name):
    """Returns a dict of symbols and their version information.

    Args:
        lib_name: Name of the library to return file mapping for.

    Returns: dict of symbol information in the following format:
        {
            "symbol_name": {
                "is_var": "true",
                "introduced-arm": 9,
                "introduced-x86": 14,
                "introduced-mips": 16,
                "introduced-arm64": 21,
                "introduced-mips64": 21,
                "introduced-x86_64": 21,
            },
            ...
        }
    """
    symbols = {}
    versions = sorted(get_platform_versions())
    for version in versions:
        for arch in ALL_ARCHITECTURES:
            symbols_dir = os.path.join(
                THIS_DIR, 'platforms', 'android-' + str(version),
                'arch-' + arch, 'symbols')
            if not os.path.exists(symbols_dir):
                logger().debug('Skipping non-existent %s', symbols_dir)
                continue

            logger().info('Processing android-%d arch-%s', version, arch)

            funcs_file_name = lib_name + '.so.functions.txt'
            funcs_file = os.path.join(symbols_dir, funcs_file_name)
            if os.path.exists(funcs_file):
                add_symbols(symbols, funcs_file, version, arch, is_var='false')

            vars_file_name = lib_name + '.so.variables.txt'
            vars_file = os.path.join(symbols_dir, vars_file_name)
            if os.path.exists(vars_file):
                add_symbols(symbols, vars_file, version, arch, is_var='true')
    return symbols


def parse_args():
    """Returns parsed command line arguments."""
    parser = argparse.ArgumentParser()

    parser.add_argument('-v', '--verbose', action='count', default=0)

    parser.add_argument(
        'library_name', metavar='LIBRARY_NAME',
        help='Name of the library to create a database for.')

    return parser.parse_args()


def main():
    """Program entry point."""
    args = parse_args()
    os.chdir(THIS_DIR)

    verbose_map = (logging.WARNING, logging.INFO, logging.DEBUG)
    verbosity = args.verbose
    if verbosity > 2:
        verbosity = 2
    logging.basicConfig(level=verbose_map[verbosity])

    symbol_db = build_symbol_db(args.library_name)
    with open(args.library_name + '.so.json', 'w') as db_file:
        json.dump(symbol_db, db_file, indent=4, separators=(',', ': '),
                  sort_keys=True)


if __name__ == '__main__':
    main()
