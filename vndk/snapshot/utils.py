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
"""Utility functions for VNDK snapshot."""

import glob
import logging
import os
import re
import subprocess
import sys

# Global Keys
#   All paths are relative to install_dir: prebuilts/vndk/v{version}
ROOT_BP_PATH = 'Android.bp'
COMMON_DIR_NAME = 'common'
COMMON_DIR_PATH = COMMON_DIR_NAME
COMMON_BP_PATH = os.path.join(COMMON_DIR_PATH, 'Android.bp')
CONFIG_DIR_PATH_PATTERN = '*/configs'
MANIFEST_FILE_NAME = 'manifest.xml'
MODULE_PATHS_FILE_NAME = 'module_paths.txt'
NOTICE_FILES_DIR_NAME = 'NOTICE_FILES'
NOTICE_FILES_DIR_PATH = os.path.join(COMMON_DIR_PATH, NOTICE_FILES_DIR_NAME)
BINDER32 = 'binder32'


def set_logging_config(verbose_level):
    verbose_map = (logging.WARNING, logging.INFO, logging.DEBUG)
    verbosity = min(verbose_level, 2)
    logging.basicConfig(
        format='%(levelname)-8s [%(filename)s:%(lineno)d] %(message)s',
        level=verbose_map[verbosity])


def check_call(cmd):
    logging.debug('Running `{}`'.format(' '.join(cmd)))
    subprocess.check_call(cmd)


def check_output(cmd):
    logging.debug('Running `{}`'.format(' '.join(cmd)))
    output = subprocess.check_output(cmd)
    logging.debug('Output: `{}`'.format(output))
    return output


def get_android_build_top():
    ANDROID_BUILD_TOP = os.getenv('ANDROID_BUILD_TOP')
    if not ANDROID_BUILD_TOP:
        print('Error: Missing ANDROID_BUILD_TOP env variable. Please run '
              '\'. build/envsetup.sh; lunch <build target>\'. Exiting script.')
        sys.exit(1)
    return ANDROID_BUILD_TOP


def join_realpath(root, *args):
    return os.path.realpath(os.path.join(root, *args))


def _get_dir_from_env(env_var, default):
    return os.path.realpath(os.getenv(env_var, default))


def get_out_dir(android_build_top):
    return _get_dir_from_env('OUT_DIR', join_realpath(android_build_top,
                                                      'out'))


def get_dist_dir(out_dir):
    return _get_dir_from_env('DIST_DIR', join_realpath(out_dir, 'dist'))


def get_snapshot_archs(install_dir):
    """Returns a list of VNDK snapshot arch flavors under install_dir.

    Args:
      install_dir: string, absolute path of prebuilts/vndk/v{version}
    """
    archs = []
    for file in glob.glob('{}/*'.format(install_dir)):
        basename = os.path.basename(file)
        if os.path.isdir(file) and basename != COMMON_DIR_NAME:
            archs.append(basename)
    return archs


def prebuilt_arch_from_path(path):
    """Extracts arch of prebuilts from path relative to install_dir.

    Args:
      path: string, path relative to prebuilts/vndk/v{version}

    Returns:
      string, arch of prebuilt (e.g., 'arm' or 'arm64' or 'x86' or 'x86_64')
    """
    return path.split('/')[1].split('-')[1]


def snapshot_arch_from_path(path):
    """Extracts VNDK snapshot arch from path relative to install_dir.

    Args:
      path: string, path relative to prebuilts/vndk/v{version}

    Returns:
      string, VNDK snapshot arch (e.g. 'arm64')
    """
    return path.split('/')[0]


def find(path, names):
    """Returns a list of files in a directory that match the given names.

    Args:
      path: string, absolute path of directory from which to find files
      names: list of strings, names of the files to find
    """
    found = []
    for root, _, files in os.walk(path):
        for file_name in sorted(files):
            if file_name in names:
                abspath = os.path.abspath(os.path.join(root, file_name))
                rel_to_root = abspath.replace(os.path.abspath(path), '')
                found.append(rel_to_root[1:])  # strip leading /
    return found


def fetch_artifact(branch, build, pattern, destination='.'):
    """Fetches build artifacts from Android Build server.

    Args:
      branch: string, branch to pull build artifacts from
      build: string, build number to pull build artifacts from
      pattern: string, pattern of build artifact file name
      destination: string, destination to pull build artifact to
    """
    fetch_artifact_path = '/google/data/ro/projects/android/fetch_artifact'
    cmd = [
        fetch_artifact_path, '--branch', branch, '--target=vndk', '--bid',
        build, pattern, destination
    ]
    check_call(cmd)
