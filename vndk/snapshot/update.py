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
import shutil
import subprocess
import sys
import tempfile
import textwrap

from gen_buildfiles import GenBuildFile

ANDROID_BUILD_TOP = os.getenv('ANDROID_BUILD_TOP')
if not ANDROID_BUILD_TOP:
    print('Error: Missing ANDROID_BUILD_TOP env variable. Please run '
          '\'. build/envsetup.sh; lunch <build target>\'. Exiting script.')
    sys.exit(1)

DIST_DIR = os.getenv('DIST_DIR')
if not DIST_DIR:
    OUT_DIR = os.getenv('OUT_DIR')
    if OUT_DIR:
        DIST_DIR = os.path.realpath(os.path.join(OUT_DIR, 'dist'))
    else:
        DIST_DIR = os.path.realpath(os.path.join(ANDROID_BUILD_TOP, 'out/dist'))

PREBUILTS_VNDK_DIR = os.path.realpath(
    os.path.join(ANDROID_BUILD_TOP, 'prebuilts/vndk'))


def logger():
    return logging.getLogger(__name__)


def check_call(cmd):
    logger().debug('Running `{}`'.format(' '.join(cmd)))
    subprocess.check_call(cmd)


def fetch_artifact(branch, build, pattern, destination='.'):
    fetch_artifact_path = '/google/data/ro/projects/android/fetch_artifact'
    cmd = [
        fetch_artifact_path, '--branch', branch, '--target=vndk', '--bid',
        build, pattern, destination
    ]
    check_call(cmd)


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


def install_snapshot(branch, build, install_dir):
    """Installs VNDK snapshot build artifacts to prebuilts/vndk/v{version}.

    1) Fetch build artifacts from Android Build server or from local DIST_DIR
    2) Unzip build artifacts

    Args:
      branch: string or None, branch name of build artifacts
      build: string or None, build number of build artifacts
      install_dir: string, directory to install VNDK snapshot
    """
    artifact_pattern = 'android-vndk-*.zip'

    try:
        if branch and build:
            tempdir = tempfile.mkdtemp()
            artifact_dir = tempdir

            os.chdir(tempdir)
            logger().info('Fetching {pattern} from {branch} (bid: {build})'
                .format(pattern=artifact_pattern, branch=branch, build=build))
            fetch_artifact(branch, build, artifact_pattern)

            manifest_pattern = 'manifest_{}.xml'.format(build)
            manifest_name = 'manifest.xml'
            logger().info('Fetching {file} from {branch} (bid: {build})'.format(
                file=manifest_pattern, branch=branch, build=build))
            fetch_artifact(branch, build, manifest_pattern, manifest_name)
            shutil.move(manifest_name, install_dir)

            os.chdir(install_dir)
        else:
            logger().info('Fetching local VNDK snapshot from {}'.format(
                DIST_DIR))
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
    finally:
        if branch and build:
            logger().info('Deleting tempdir: {}'.format(tempdir))
            shutil.rmtree(tempdir)


def gather_notice_files():
    """Gathers all NOTICE files to a new NOTICE_FILES directory.

    Create a new NOTICE_FILES directory under install_dir and copy to it
    all NOTICE files in arch-*/NOTICE_FILES.
    """
    notices_dir_name = 'NOTICE_FILES'
    logger().info('Creating {} directory...'.format(notices_dir_name))
    os.makedirs(notices_dir_name)
    for arch_dir in glob.glob('arch-*'):
        notices_dir_per_arch = os.path.join(arch_dir, notices_dir_name)
        if os.path.isdir(notices_dir_per_arch):
            for notice_file in glob.glob(
                '{}/*.txt'.format(notices_dir_per_arch)):
                if not os.path.isfile(os.path.join(notices_dir_name,
                    os.path.basename(notice_file))):
                    shutil.copy(notice_file, notices_dir_name)
            shutil.rmtree(notices_dir_per_arch)


def update_buildfiles(buildfile_generator):
    logger().info('Updating Android.mk...')
    buildfile_generator.generate_android_mk()

    logger().info('Updating Android.bp...')
    buildfile_generator.generate_android_bp()


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
        '--use-current-branch',action='store_true',
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
    install_snapshot(args.branch, args.build, install_dir)
    gather_notice_files()

    buildfile_generator = GenBuildFile(install_dir, vndk_version)
    update_buildfiles(buildfile_generator)

    if not args.local:
        commit(args.branch, args.build, vndk_version)


if __name__ == '__main__':
    main()
