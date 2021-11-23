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
}
RESTRICTED_LICENSE_KEYWORDS = {
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
    It also lists the modules with the restricted licenses.

    Initialize the LicenseCollector with a vndk snapshot directory.
    After run() is called, 'license_kinds' will include the licenses found from
    the snapshot directory.
    'restricted' will have the files that have the restricted licenses.
    """
    def __init__(self, install_dir):
        self._install_dir = install_dir
        self._paths_to_check = [os.path.join(install_dir,
                                              utils.NOTICE_FILES_DIR_PATH),]
        self._paths_to_check = self._paths_to_check + glob.glob(os.path.join(self._install_dir, '*/include'))

        self.license_kinds = set()
        self.restricted = set()

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

    def read_and_check_dir_for_licenses(self, path):
        """ Check licenses for all files under the directory
        """
        for (root, _, files) in os.walk(path):
            for f in files:
                with open(os.path.join(root, f), 'r') as file_to_check:
                    file_string = file_to_check.read()
                    self.read_and_check_licenses(file_string, LICENSE_KEYWORDS)
                    if self.read_and_check_licenses(file_string, RESTRICTED_LICENSE_KEYWORDS):
                        self.restricted.add(f)

    def run(self):
        """ search licenses in vndk snapshots
        """
        for path in self._paths_to_check:
            logging.info('Reading {}'.format(path))
            self.read_and_check_dir_for_licenses(path)
        self.license_kinds.update(LICENSE_INCLUDE)

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
    print(sorted(license_collector.restricted))

if __name__ == '__main__':
    main()
