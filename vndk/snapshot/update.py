#!/usr/bin/env python
#
# Copyright (C) 2017 The Android Open Source Project
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
"""Installs VNDK snapshot under prebuilts/vndk/v{version}."""

import argparse
import glob
import logging
import os
import re
import shutil
import subprocess
import sys
import tempfile
import textwrap

import utils

from check_gpl_license import GPLChecker
from gen_buildfiles import GenBuildFile

ANDROID_BUILD_TOP = utils.get_android_build_top()
PREBUILTS_VNDK_DIR = utils.join_realpath(ANDROID_BUILD_TOP, 'prebuilts/vndk')


def start_branch(build):
    branch_name = 'update-' + (build or 'local')
    logging.info('Creating branch {branch} in {dir}'.format(
        branch=branch_name, dir=os.getcwd()))
    utils.check_call(['repo', 'start', branch_name, '.'])


def remove_old_snapshot(install_dir):
    logging.info('Removing any old files in {}'.format(install_dir))
    for file in glob.glob('{}/*'.format(install_dir)):
        try:
            if os.path.isfile(file):
                os.unlink(file)
            elif os.path.isdir(file):
                shutil.rmtree(file)
        except Exception as error:
            logging.error('Error: {}'.format(error))
            sys.exit(1)


def install_snapshot(branch, build, local_dir, install_dir, temp_artifact_dir):
    """Installs VNDK snapshot build artifacts to prebuilts/vndk/v{version}.

    1) Fetch build artifacts from Android Build server or from local_dir
    2) Unzip build artifacts

    Args:
      branch: string or None, branch name of build artifacts
      build: string or None, build number of build artifacts
      local_dir: string or None, local dir to pull artifacts from
      install_dir: string, directory to install VNDK snapshot
      temp_artifact_dir: string, temp directory to hold build artifacts fetched
        from Android Build server. For 'local' option, is set to None.
    """
    artifact_pattern = 'android-vndk-*.zip'

    if branch and build:
        artifact_dir = temp_artifact_dir
        os.chdir(temp_artifact_dir)
        logging.info('Fetching {pattern} from {branch} (bid: {build})'.format(
            pattern=artifact_pattern, branch=branch, build=build))
        utils.fetch_artifact(branch, build, artifact_pattern)

        manifest_pattern = 'manifest_{}.xml'.format(build)
        logging.info('Fetching {file} from {branch} (bid: {build})'.format(
            file=manifest_pattern, branch=branch, build=build))
        utils.fetch_artifact(branch, build, manifest_pattern,
                             utils.MANIFEST_FILE_NAME)

        os.chdir(install_dir)
    elif local_dir:
        logging.info('Fetching local VNDK snapshot from {}'.format(local_dir))
        artifact_dir = local_dir

    artifacts = glob.glob(os.path.join(artifact_dir, artifact_pattern))
    for artifact in artifacts:
        logging.info('Unzipping VNDK snapshot: {}'.format(artifact))
        utils.check_call(['unzip', '-qn', artifact, '-d', install_dir])


def gather_notice_files(install_dir):
    """Gathers all NOTICE files to a common NOTICE_FILES directory."""

    common_notices_dir = utils.NOTICE_FILES_DIR_PATH
    logging.info('Creating {} directory to gather all NOTICE files...'.format(
        common_notices_dir))
    os.makedirs(common_notices_dir)
    for arch in utils.get_snapshot_archs(install_dir):
        notices_dir_per_arch = os.path.join(arch, utils.NOTICE_FILES_DIR_NAME)
        if os.path.isdir(notices_dir_per_arch):
            for notice_file in glob.glob(
                    '{}/*.txt'.format(notices_dir_per_arch)):
                if not os.path.isfile(
                        os.path.join(common_notices_dir,
                                     os.path.basename(notice_file))):
                    shutil.copy(notice_file, common_notices_dir)
            shutil.rmtree(notices_dir_per_arch)


def post_processe_files_if_needed(vndk_version):
    """For O-MR1, replaces unversioned VNDK directories with versioned ones.
    Also, renames ld.config.txt, llndk.libraries.txt and vndksp.libraries.txt
    files to have version suffix.

    Unversioned VNDK directories: /system/${LIB}/vndk[-sp]
    Versioned VNDK directories: /system/${LIB}/vndk[-sp]-27

    Args:
      vndk_version: int, version of VNDK snapshot
    """
    def add_version_suffix(file_name):
        logging.info('Rename {} to have version suffix'.format(vndk_version))
        target_files = glob.glob(
            os.path.join(utils.CONFIG_DIR_PATH_PATTERN, file_name))
        for target_file in target_files:
            name, ext = os.path.splitext(target_file)
            os.rename(target_file, name + '.' + str(vndk_version) + ext)
    if vndk_version == 27:
        logging.info('Revising ld.config.txt for O-MR1...')
        re_pattern = '(system\/\${LIB}\/vndk(?:-sp)?)([:/]|$)'
        VNDK_INSTALL_DIR_RE = re.compile(re_pattern, flags=re.MULTILINE)
        ld_config_txt_paths = glob.glob(
            os.path.join(utils.CONFIG_DIR_PATH_PATTERN, 'ld.config*'))
        for ld_config_file in ld_config_txt_paths:
            with open(ld_config_file, 'r') as file:
                revised = VNDK_INSTALL_DIR_RE.sub(r'\1-27\2', file.read())
            with open(ld_config_file, 'w') as file:
                file.write(revised)

        files_to_add_version_suffix = ('ld.config.txt',
                                       'llndk.libraries.txt',
                                       'vndksp.libraries.txt')
        for file_to_rename in files_to_add_version_suffix:
            add_version_suffix(file_to_rename)

    files_to_enforce_version_suffix = ('vndkcore.libraries.txt',
                                       'vndkprivate.libraries.txt')
    for file_to_rename in files_to_enforce_version_suffix:
        add_version_suffix(file_to_rename)


def update_buildfiles(buildfile_generator):
    logging.info('Generating root Android.bp file...')
    buildfile_generator.generate_root_android_bp()

    logging.info('Generating common/Android.bp file...')
    buildfile_generator.generate_common_android_bp()

    logging.info('Generating Android.bp files...')
    buildfile_generator.generate_android_bp()


def check_gpl_license(license_checker):
    try:
        license_checker.check_gpl_projects()
    except ValueError as error:
        logging.error('***CANNOT INSTALL VNDK SNAPSHOT***: {}'.format(error))
        raise


def commit(branch, build, version):
    logging.info('Making commit...')
    utils.check_call(['git', 'add', '.'])
    message = textwrap.dedent("""\
        Update VNDK snapshot v{version} to build {build}.

        Taken from branch {branch}.""").format(
        version=version, branch=branch, build=build)
    utils.check_call(['git', 'commit', '-m', message])


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        'vndk_version',
        type=int,
        help='VNDK snapshot version to install, e.g. "27".')
    parser.add_argument('-b', '--branch', help='Branch to pull build from.')
    parser.add_argument('--build', help='Build number to pull.')
    parser.add_argument(
        '--local',
        help=('Fetch local VNDK snapshot artifacts from specified local '
              'directory instead of Android Build server. '
              'Example: --local=/path/to/local/dir'))
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
        help='Increase output verbosity, e.g. "-v", "-vv".')
    return parser.parse_args()


def main():
    """Program entry point."""
    args = get_args()

    local = None
    if args.local:
        local = os.path.expanduser(args.local)

    if local:
        if args.build or args.branch:
            raise ValueError(
                'When --local option is set, --branch or --build cannot be '
                'specified.')
        elif not os.path.isdir(local):
            raise RuntimeError(
                'The specified local directory, {}, does not exist.'.format(
                    local))
    else:
        if not (args.build and args.branch):
            raise ValueError(
                'Please provide both --branch and --build or set --local '
                'option.')

    vndk_version = args.vndk_version

    install_dir = os.path.join(PREBUILTS_VNDK_DIR, 'v{}'.format(vndk_version))
    if not os.path.isdir(install_dir):
        raise RuntimeError(
            'The directory for VNDK snapshot version {ver} does not exist.\n'
            'Please request a new git project for prebuilts/vndk/v{ver} '
            'before installing new snapshot.'.format(ver=vndk_version))

    utils.set_logging_config(args.verbose)

    os.chdir(install_dir)

    if not args.use_current_branch:
        start_branch(args.build)

    remove_old_snapshot(install_dir)
    os.makedirs(utils.COMMON_DIR_PATH)

    temp_artifact_dir = None
    if not local:
        temp_artifact_dir = tempfile.mkdtemp()

    try:
        install_snapshot(args.branch, args.build, local, install_dir,
                         temp_artifact_dir)
        gather_notice_files(install_dir)
        post_processe_files_if_needed(vndk_version)

        buildfile_generator = GenBuildFile(install_dir, vndk_version)
        update_buildfiles(buildfile_generator)

        if not local:
            license_checker = GPLChecker(install_dir, ANDROID_BUILD_TOP,
                                         temp_artifact_dir, args.remote)
            check_gpl_license(license_checker)
            logging.info(
                'Successfully updated VNDK snapshot v{}'.format(vndk_version))
    except Exception as error:
        logging.error('FAILED TO INSTALL SNAPSHOT: {}'.format(error))
        raise
    finally:
        if temp_artifact_dir:
            logging.info(
                'Deleting temp_artifact_dir: {}'.format(temp_artifact_dir))
            shutil.rmtree(temp_artifact_dir)

    if not local:
        commit(args.branch, args.build, vndk_version)
        logging.info(
            'Successfully created commit for VNDK snapshot v{}'.format(
                vndk_version))

    logging.info('Done.')


if __name__ == '__main__':
    main()
