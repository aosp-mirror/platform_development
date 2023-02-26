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

"""A command line utility to download multiple patch files of change lists from
Gerrit."""

from __future__ import print_function

import argparse
import os
import sys

from gerrit import (
    add_common_parse_args, create_url_opener_from_args, find_gerrit_name,
    normalize_gerrit_name, query_change_lists, get_patch
)

def _parse_args():
    """Parse command line options."""
    parser = argparse.ArgumentParser()
    add_common_parse_args(parser)
    return parser.parse_args()


def main():
    """Main function"""
    args = _parse_args()

    if args.gerrit:
        args.gerrit = normalize_gerrit_name(args.gerrit)
    else:
        try:
            args.gerrit = find_gerrit_name()
        # pylint: disable=bare-except
        except:
            print('gerrit instance not found, use [-g GERRIT]')
            sys.exit(1)

    # Query change lists
    url_opener = create_url_opener_from_args(args)
    change_lists = query_change_lists(
        url_opener, args.gerrit, args.query, args.start, args.limits)

    # Download patch files
    num_changes = len(change_lists)
    num_changes_width = len(str(num_changes))
    for i, change in enumerate(change_lists, start=1):
        print('{:>{}}/{} | {} {}'.format(
            i, num_changes_width, num_changes, change['_number'],
            change['subject']))

        patch_file = get_patch(url_opener, args.gerrit, change['id'])
        with open('{}.patch'.format(change['_number']), 'wb') as output_file:
            output_file.write(patch_file)

if __name__ == '__main__':
    main()
