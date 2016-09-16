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
"""Annotates an existing version script with data for the NDK."""
import argparse
import collections
import json
import logging
import os
import sys


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


def verify_version_script(lines, json_db):
    """Checks that every symbol in the NDK is in the version script."""
    symbols = dict(json_db)
    for line in lines:
        if ';' in line:
            name, _ = line.split(';')
            name = name.strip()

            if name in symbols:
                del symbols[name]
    if len(symbols) > 0:
        for symbol in symbols.keys():
            logger().error(
                'NDK symbol not present in version script: {}'.format(symbol))
        sys.exit(1)


def was_always_present(db_entry, arches):
    """Returns whether the symbol has always been present or not."""
    for arch in arches:
        is_64 = arch.endswith('64')
        introduced_tag = 'introduced-' + arch
        if introduced_tag not in db_entry:
            return False
        if is_64 and db_entry[introduced_tag] != 21:
            return False
        elif not is_64 and db_entry[introduced_tag] != 9:
            return False
        # Else we have the symbol in this arch and was introduced in the first
        # version of it.
    return True


def get_common_introduced(db_entry, arches):
    """Returns the common introduction API level or None.

    If the symbol was introduced in the same API level for all architectures,
    return that API level. If the symbol is not present in all architectures or
    was introduced to them at different times, return None.
    """
    introduced = None
    for arch in arches:
        introduced_tag = 'introduced-' + arch
        if introduced_tag not in db_entry:
            return None
        if introduced is None:
            introduced = db_entry[introduced_tag]
        elif db_entry[introduced_tag] != introduced:
            return None
        # Else we have the symbol in this arch and it's the same introduction
        # level. Keep going.
    return introduced


def annotate_symbol(line, json_db):
    """Returns the line with NDK data appended."""
    name_part, rest = line.split(';')
    name = name_part.strip()
    if name not in json_db:
        return line

    rest = rest.rstrip()
    tags = []
    db_entry = json_db[name]
    if db_entry['is_var'] == 'true':
        tags.append('var')

    arches = ALL_ARCHITECTURES
    if '#' in rest:
        had_tags = True
        # Current tags aren't necessarily arch tags. Check them before using
        # them.
        _, old_tags = rest.split('#')
        arch_tags = []
        for tag in old_tags.strip().split(' '):
            if tag in ALL_ARCHITECTURES:
                arch_tags.append(tag)
        if len(arch_tags) > 0:
            arches = arch_tags
    else:
        had_tags = False

    always_present = was_always_present(db_entry, arches)
    common_introduced = get_common_introduced(db_entry, arches)
    if always_present:
        # No need to tag things that have always been there.
        pass
    elif common_introduced is not None:
        tags.append('introduced={}'.format(common_introduced))
    else:
        for arch in ALL_ARCHITECTURES:
            introduced_tag = 'introduced-' + arch
            if introduced_tag not in db_entry:
                continue
            tags.append(
                '{}={}'.format(introduced_tag, db_entry[introduced_tag]))

    if tags:
        if not had_tags:
            rest += ' #'
        rest += ' ' + ' '.join(tags)
    return name_part + ';' + rest + '\n'


def annotate_version_script(version_script, json_db, lines):
    """Rewrites a version script with NDK annotations."""
    for line in lines:
        # Lines contain a semicolon iff they contain a symbol name.
        if ';' in line:
            version_script.write(annotate_symbol(line, json_db))
        else:
            version_script.write(line)


def create_version_script(version_script, json_db):
    """Creates a new version script based on an NDK library definition."""
    json_db = collections.OrderedDict(sorted(json_db.items()))

    version_script.write('LIB {\n')
    version_script.write('  global:\n')
    for symbol in json_db.keys():
        line = annotate_symbol('    {};\n'.format(symbol), json_db)
        version_script.write(line)
    version_script.write('  local:\n')
    version_script.write('    *;\n')
    version_script.write('};')


def parse_args():
    """Returns parsed command line arguments."""
    parser = argparse.ArgumentParser()

    parser.add_argument(
        '--create', action='store_true',
        help='Create a new version script instead of annotating.')

    parser.add_argument(
        'data_file', metavar='DATA_FILE', type=os.path.realpath,
        help='Path to JSON DB generated by build_symbol_db.py.')

    parser.add_argument(
        'version_script', metavar='VERSION_SCRIPT', type=os.path.realpath,
        help='Version script to be annotated.')

    parser.add_argument('-v', '--verbose', action='count', default=0)

    return parser.parse_args()


def main():
    """Program entry point."""
    args = parse_args()

    verbose_map = (logging.WARNING, logging.INFO, logging.DEBUG)
    verbosity = args.verbose
    if verbosity > 2:
        verbosity = 2

    logging.basicConfig(level=verbose_map[verbosity])
    with open(args.data_file) as json_db_file:
        json_db = json.load(json_db_file)

    if args.create:
        with open(args.version_script, 'w') as version_script:
            create_version_script(version_script, json_db)
    else:
        with open(args.version_script, 'r') as version_script:
            file_data = version_script.readlines()
        verify_version_script(file_data, json_db)
        with open(args.version_script, 'w') as version_script:
            annotate_version_script(version_script, json_db, file_data)


if __name__ == '__main__':
    main()
