#!/usr/bin/env python3
#
# Copyright (C) 2022 The Android Open Source Project
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

import argparse
import logging

import update
import utils

VNDK_SNAPSHOT_SOURCE_BRANCHES = {
    29: 'qt-gsi-release',
    30: 'android11-gsi',
    31: 'android12-gsi',
    32: 'android12L-gsi',
    33: 'android13-gsi',
}

def fetch_and_update_snapshots(versions, args):
    for version in versions:
        if version not in VNDK_SNAPSHOT_SOURCE_BRANCHES:
            raise ValueError ('Unknown VNDK version: {}'.format(version))
        logging.info('Updating snapshot version {}'.format(version))
        branch = VNDK_SNAPSHOT_SOURCE_BRANCHES[version]
        bid = utils.get_latest_vndk_bid(branch)

        update.run(version, branch, bid, None, args.use_current_branch,
                   args.remote, args.verbose)

def get_args(parser):
    parser.add_argument(
        'versions',
        metavar='vndk_version',
        type=int,
        nargs='*',
        help='list of versions to fetch and update')
    parser.add_argument(
        '--all',
        action='store_true',
        help='fetch all vndk snapshots')
    parser.add_argument(
        '--use-current-branch',
        action='store_true',
        help='Perform the update in the current branch. Do not repo start.')
    parser.add_argument(
        '--remote',
        default='aosp',
        help=('Remote name to fetch and check if the revision of VNDK snapshot '
              'is included in the source to conform GPL license. default=aosp'))
    parser.add_argument(
        '-v',
        '--verbose',
        action='count',
        default=0,
        help='Increase output verbosity, e.g. "-v", "-vv"')
    return parser.parse_args()

def main():
    parser = argparse.ArgumentParser()
    args = get_args(parser)
    utils.set_logging_config(args.verbose)

    if args.all:
        versions = VNDK_SNAPSHOT_SOURCE_BRANCHES.keys()
        fetch_and_update_snapshots(versions, args)
        return

    if not args.versions:
        parser.print_help()
        return

    fetch_and_update_snapshots(args.versions, args)

if __name__ == '__main__':
    main()
