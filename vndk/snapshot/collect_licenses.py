#!/usr/bin/env python3
#
# Copyright (C) 2021 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import argparse
import glob
import logging
import os

import utils

LICENSE_KINDS_PREFIX = 'SPDX-license-identifier-'
LICENSE_KEYWORDS = {
    'Apache-2.0': ('Apache License', 'Version 2.0',),
    'BSD': ('BSD ',),
    'CC0-1.0': ('CC0 Public Domain Dedication license',),
    'FTL': ('FreeType Project LICENSE',),
    'ISC': ('Internet Systems Consortium',),
    'ISC': ('ISC license',),
    'MIT': (' MIT ',),
    'MPL-2.0': ('Mozilla Public License Version 2.0',),
    'MPL': ('Mozilla Public License',),
    'NCSA': ('University of Illinois', 'NCSA',),
    'OpenSSL': ('The OpenSSL Project',),
    'Zlib': ('zlib License',),
    'LGPL-3.0': ('LESSER GENERAL PUBLIC LICENSE', 'Version 3,',),
    'LGPL-2.1': ('LESSER GENERAL PUBLIC LICENSE', 'Version 2.1',),
    'LGPL-2.0': ('GNU LIBRARY GENERAL PUBLIC LICENSE', 'Version 2,',),
    'LGPL': ('LESSER GENERAL PUBLIC LICENSE',),
    'GPL-2.0': ('GNU GENERAL PUBLIC LICENSE', 'Version 2,',),
    'GPL': ('GNU GENERAL PUBLIC LICENSE',),
}

LICENSE_INCLUDE = ['legacy_permissive', 'legacy_unencumbered']

class LicenseCollector(object):
    """ Collect licenses from a VNDK snapshot directory

    This is to collect the license_kinds to be used in license modules.

    Initialize the LicenseCollector with a vndk snapshot directory.
    After run() is called, 'license_kinds' will include the licenses found from
    the snapshot directory.
    """
    def __init__(self, install_dir):
        self._install_dir = install_dir
        self._paths_to_check = [os.path.join(install_dir,
                                              utils.NOTICE_FILES_DIR_PATH),]
        self._paths_to_check = self._paths_to_check + glob.glob(os.path.join(self._install_dir, '*/include'))

        self.license_kinds = set()

    def read_and_check_licenses(self, license_text, license_keywords):
        """ Read the license keywords and check if all keywords are in the file.

        The found licenses will be added to license_kinds set. This function will
        return True if any licenses are found, False otherwise.
        """
        found = False
        for lic, patterns in license_keywords.items():
            for pattern in patterns:
                if pattern not in license_text:
                    break
            else:
                self.license_kinds.add(LICENSE_KINDS_PREFIX + lic)
                found = True
        return found

    def check_licenses(self, filepath):
        """ Read a license text file and find the license_kinds.
        """
        with open(filepath, 'r') as file_to_check:
            file_string = file_to_check.read()
            self.read_and_check_licenses(file_string, LICENSE_KEYWORDS)

    def run(self, module=''):
        """ search licenses in vndk snapshots

        Args:
          module: module name to find the license kind.
                  If empty, check all license files.
        """
        if module == '':
            for path in self._paths_to_check:
                logging.info('Reading {}'.format(path))
                for (root, _, files) in os.walk(path):
                    for f in files:
                        self.check_licenses(os.path.join(root, f))
            self.license_kinds.update(LICENSE_INCLUDE)
        else:
            license_text_path = '{notice_dir}/{module}.txt'.format(
                notice_dir=utils.NOTICE_FILES_DIR_NAME,
                module=module)
            logging.info('Reading {}'.format(license_text_path))
            self.check_licenses(os.path.join(self._install_dir, utils.COMMON_DIR_PATH, license_text_path))
            if not self.license_kinds:
                # Add 'legacy_permissive' if no licenses are found for this file.
                self.license_kinds.add('legacy_permissive')

def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        'vndk_version',
        type=utils.vndk_version_int,
        help='VNDK snapshot version to check, e.g. "{}".'.format(
            utils.MINIMUM_VNDK_VERSION))
    parser.add_argument(
        '-v',
        '--verbose',
        action='count',
        default=0,
        help='Increase output verbosity, e.g. "-v", "-vv".')
    return parser.parse_args()

def main():
    """ For the local testing purpose.
    """
    ANDROID_BUILD_TOP = utils.get_android_build_top()
    PREBUILTS_VNDK_DIR = utils.join_realpath(ANDROID_BUILD_TOP,
                                             'prebuilts/vndk')
    args = get_args()
    vndk_version = args.vndk_version
    install_dir = os.path.join(PREBUILTS_VNDK_DIR, 'v{}'.format(vndk_version))
    utils.set_logging_config(args.verbose)

    license_collector = LicenseCollector(install_dir)
    license_collector.run()
    print(sorted(license_collector.license_kinds))

if __name__ == '__main__':
    main()
