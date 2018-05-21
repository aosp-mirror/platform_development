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
DIST_DIR = utils.get_dist_dir(utils.get_out_dir(ANDROID_BUILD_TOP))
PREBUILTS_VNDK_DIR = utils.join_realpath(ANDROID_BUILD_TOP, 'prebuilts/vndk')


def logger():
    return logging.getLogger(__name__)


def check_call(cmd):
    logger().debug('Running `{}`'.format(' '.join(cmd)))
    subprocess.check_call(cmd)


def start_branch(build):
    branch_name = 'update-' + (build or 'local')
    logger().info('Creating branch {branch} in {dir}'.format(
        branch=branch_name, dir=os.getcwd()))
    check_call(['repo', 'start', branch_name, '.'])


def remove_old_snapshot(install_dir):
    logger().info('Removing any old files in {}'.format(install_dir))
    for file in glob.glob('{}/*'.format(install_dir)):
        try:
            if os.path.isfile(file):
                os.unlink(file)
            elif os.path.isdir(file):
                shutil.rmtree(file)
        except Exception as error:
            print error
            sys.exit(1)


def install_snapshot(branch, build, install_dir, temp_artifact_dir):
    """Installs VNDK snapshot build artifacts to prebuilts/vndk/v{version}.

    1) Fetch build artifacts from Android Build server or from local DIST_DIR
    2) Unzip build artifacts

    Args:
      branch: string or None, branch name of build artifacts
      build: string or None, build number of build artifacts
      install_dir: string, directory to install VNDK snapshot
      temp_artifact_dir: string, temp directory to hold build artifacts fetched
        from Android Build server. For 'local' option, is set to None.
    """
    artifact_pattern = 'android-vndk-*.zip'

    if branch and build:
        artifact_dir = temp_artifact_dir
        os.chdir(temp_artifact_dir)
        logger().info('Fetching {pattern} from {branch} (bid: {build})'.format(
            pattern=artifact_pattern, branch=branch, build=build))
        utils.fetch_artifact(branch, build, artifact_pattern)

        manifest_pattern = 'manifest_{}.xml'.format(build)
        logger().info('Fetching {file} from {branch} (bid: {build})'.format(
            file=manifest_pattern, branch=branch, build=build))
        utils.fetch_artifact(branch, build, manifest_pattern,
                             utils.MANIFEST_FILE_NAME)

        os.chdir(install_dir)
    else:
        logger().info('Fetching local VNDK snapshot from {}'.format(DIST_DIR))
        artifact_dir = DIST_DIR

    artifacts = glob.glob(os.path.join(artifact_dir, artifact_pattern))
    artifact_cnt = len(artifacts)
    if artifact_cnt < 4:
        raise RuntimeError(
            'Expected four android-vndk-*.zip files in {path}. Instead '
            'found {cnt}.'.format(path=artifact_dir, cnt=artifact_cnt))
    for artifact in artifacts:
        logger().info('Unzipping VNDK snapshot: {}'.format(artifact))
        check_call(['unzip', '-q', artifact, '-d', install_dir])


def gather_notice_files(install_dir):
    """Gathers all NOTICE files to a common NOTICE_FILES directory."""

    common_notices_dir = utils.NOTICE_FILES_DIR_PATH
    logger().info('Creating {} directory...'.format(common_notices_dir))
    os.makedirs(common_notices_dir)
    for variant in utils.get_snapshot_variants(install_dir):
        notices_dir_per_variant = os.path.join(variant,
                                               utils.NOTICE_FILES_DIR_NAME)
        if os.path.isdir(notices_dir_per_variant):
            for notice_file in glob.glob(
                    '{}/*.txt'.format(notices_dir_per_variant)):
                if not os.path.isfile(
                        os.path.join(common_notices_dir,
                                     os.path.basename(notice_file))):
                    shutil.copy(notice_file, common_notices_dir)
            shutil.rmtree(notices_dir_per_variant)


def revise_ld_config_txt_if_needed(vndk_version):
    """For O-MR1, replaces unversioned VNDK directories with versioned ones.

    Unversioned VNDK directories: /system/${LIB}/vndk[-sp]
    Versioned VNDK directories: /system/${LIB}/vndk[-sp]-27

    Args:
      vndk_version: string, version of VNDK snapshot
    """
    if vndk_version == '27':
        re_pattern = '(system\/\${LIB}\/vndk(?:-sp)?)([:/]|$)'
        VNDK_INSTALL_DIR_RE = re.compile(re_pattern, flags=re.MULTILINE)
        ld_config_txt_paths = glob.glob(
            os.path.join(utils.CONFIG_DIR_PATH_PATTERN, 'ld.config*'))
        for ld_config_file in ld_config_txt_paths:
            with open(ld_config_file, 'r') as file:
                revised = VNDK_INSTALL_DIR_RE.sub(r'\1-27\2', file.read())
            with open(ld_config_file, 'w') as file:
                file.write(revised)


def update_buildfiles(buildfile_generator):
    logger().info('Generating Android.mk file...')
    buildfile_generator.generate_android_mk()

    logger().info('Generating Android.bp files...')
    buildfile_generator.generate_android_bp()


def check_gpl_license(license_checker):
    try:
        license_checker.check_gpl_projects()
    except ValueError as error:
        print '***CANNOT INSTALL VNDK SNAPSHOT***', error
        raise


def commit(branch, build, version):
    logger().info('Making commit...')
    check_call(['git', 'add', '.'])
    message = textwrap.dedent("""\
        Update VNDK snapshot v{version} to build {build}.

        Taken from branch {branch}.""").format(
        version=version, branch=branch, build=build)
    check_call(['git', 'commit', '-m', message])


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        'vndk_version', type=int,
        help='VNDK snapshot version to install, e.g. "27".')
    parser.add_argument('-b', '--branch', help='Branch to pull build from.')
    parser.add_argument('--build', help='Build number to pull.')
    parser.add_argument(
        '--local', action='store_true',
        help=('Fetch local VNDK snapshot artifacts from DIST_DIR instead of '
              'Android Build server.'))
    parser.add_argument(
        '--use-current-branch', action='store_true',
        help='Perform the update in the current branch. Do not repo start.')
    parser.add_argument(
        '-v', '--verbose', action='count', default=0,
        help='Increase output verbosity, e.g. "-v", "-vv".')
    return parser.parse_args()


def main():
    """Program entry point."""
    args = get_args()

    if args.local:
        if args.build or args.branch:
            raise ValueError(
                'When --local option is set, --branch or --build cannot be '
                'specified.')
        elif not os.path.isdir(DIST_DIR):
            raise RuntimeError(
                'The --local option is set, but DIST_DIR={} does not exist.'.
                format(DIST_DIR))
    else:
        if not (args.build and args.branch):
            raise ValueError(
                'Please provide both --branch and --build or set --local '
                'option.')

    vndk_version = str(args.vndk_version)

    install_dir = os.path.join(PREBUILTS_VNDK_DIR, 'v{}'.format(vndk_version))
    if not os.path.isdir(install_dir):
        raise RuntimeError(
            'The directory for VNDK snapshot version {ver} does not exist.\n'
            'Please request a new git project for prebuilts/vndk/v{ver} '
            'before installing new snapshot.'.format(ver=vndk_version))

    verbose_map = (logging.WARNING, logging.INFO, logging.DEBUG)
    verbosity = min(args.verbose, 2)
    logging.basicConfig(level=verbose_map[verbosity])

    os.chdir(install_dir)

    if not args.use_current_branch:
        start_branch(args.build)

    remove_old_snapshot(install_dir)
    os.makedirs(utils.COMMON_DIR_PATH)

    temp_artifact_dir = None
    if not args.local:
        temp_artifact_dir = tempfile.mkdtemp()

    install_status = True
    try:
        install_snapshot(args.branch, args.build, install_dir,
                         temp_artifact_dir)
        gather_notice_files(install_dir)
        revise_ld_config_txt_if_needed(vndk_version)

        buildfile_generator = GenBuildFile(install_dir, vndk_version)
        update_buildfiles(buildfile_generator)

        if not args.local:
            license_checker = GPLChecker(install_dir, ANDROID_BUILD_TOP,
                                         temp_artifact_dir)
            check_gpl_license(license_checker)
    except:
        logger().info('FAILED TO INSTALL SNAPSHOT')
        install_status = False
    finally:
        if temp_artifact_dir:
            logger().info(
                'Deleting temp_artifact_dir: {}'.format(temp_artifact_dir))
            shutil.rmtree(temp_artifact_dir)

    if not args.local and install_status:
        commit(args.branch, args.build, vndk_version)


if __name__ == '__main__':
    main()
