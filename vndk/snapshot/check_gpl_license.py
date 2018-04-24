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

import argparse
import glob
import os
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as xml_tree

import utils


class GPLChecker(object):
    """Checks that all GPL projects in a VNDK snapshot have released sources.

    Makes sure that the current source tree have the sources for all GPL
    prebuilt libraries in a specified VNDK snapshot version.
    """
    MANIFEST_XML = utils.MANIFEST_FILE_NAME
    MODULE_PATHS_TXT = utils.MODULE_PATHS_FILE_NAME

    def __init__(self, install_dir, android_build_top, temp_artifact_dir):
        """GPLChecker constructor.

        Args:
          install_dir: string, absolute path to the prebuilts/vndk/v{version}
            directory where the build files will be generated.
          android_build_top: string, absolute path to ANDROID_BUILD_TOP
        """
        self._android_build_top = android_build_top
        self._install_dir = install_dir
        self._manifest_file = os.path.join(temp_artifact_dir,
                                           self.MANIFEST_XML)
        self._notice_files_dir = os.path.join(install_dir,
                                              utils.NOTICE_FILES_DIR_PATH)

        if not os.path.isfile(self._manifest_file):
            raise RuntimeError(
                '{manifest} not found at {manifest_file}'.format(
                    manifest=self.MANIFEST_XML,
                    manifest_file=self._manifest_file))

    def _parse_module_paths(self):
        """Parses the module_path.txt files into a dictionary,

        Returns:
          module_paths: dict, e.g. {libfoo.so: some/path/here}
        """
        module_paths = dict()
        for file in utils.find(self._install_dir, [self.MODULE_PATHS_TXT]):
            file_path = os.path.join(self._install_dir, file)
            with open(file_path, 'r') as f:
                for line in f.read().strip().split('\n'):
                    paths = line.split(' ')
                    if len(paths) > 1:
                        if paths[0] not in module_paths:
                            module_paths[paths[0]] = paths[1]
        return module_paths

    def _parse_manifest(self):
        """Parses manifest.xml file and returns list of 'project' tags."""

        root = xml_tree.parse(self._manifest_file).getroot()
        return root.findall('project')

    def _get_revision(self, module_path, manifest_projects):
        """Returns revision value recorded in manifest.xml for given project.

        Args:
          module_path: string, project path relative to ANDROID_BUILD_TOP
          manifest_projects: list of xml_tree.Element, list of 'project' tags
        """
        revision = None
        for project in manifest_projects:
            path = project.get('path')
            if module_path.startswith(path):
                revision = project.get('revision')
                break
        return revision

    def _check_revision_exists(self, revision, git_project_path):
        """Checks whether a revision is found in a git project of current tree.

        Args:
          revision: string, revision value recorded in manifest.xml
          git_project_path: string, path relative to ANDROID_BUILD_TOP
        """
        path = utils.join_realpath(self._android_build_top, git_project_path)
        try:
            subprocess.check_call(
                ['git', '-C', path, 'rev-list', 'HEAD..{}'.format(revision)])
            return True
        except subprocess.CalledProcessError:
            return False

    def check_gpl_projects(self):
        """Checks that all GPL projects have released sources.

        Raises:
          ValueError: There are GPL projects with unreleased sources.
        """
        print 'Starting license check for GPL projects...'

        notice_files = glob.glob('{}/*'.format(self._notice_files_dir))
        if len(notice_files) == 0:
            raise RuntimeError('No license files found in {}'.format(
                self._notice_files_dir))

        gpl_projects = []
        pattern = 'GENERAL PUBLIC LICENSE'
        for notice_file_path in notice_files:
            with open(notice_file_path, 'r') as notice_file:
                if pattern in notice_file.read():
                    lib_name = os.path.splitext(
                        os.path.basename(notice_file_path))[0]
                    gpl_projects.append(lib_name)

        if not gpl_projects:
            print 'No GPL projects found.'
            return

        print 'GPL projects found:', ', '.join(gpl_projects)

        module_paths = self._parse_module_paths()
        manifest_projects = self._parse_manifest()
        released_projects = []
        unreleased_projects = []

        for lib in gpl_projects:
            if lib in module_paths:
                module_path = module_paths[lib]
                revision = self._get_revision(module_path, manifest_projects)
                if not revision:
                    raise RuntimeError(
                        'No project found for {path} in {manifest}'.format(
                            path=module_path, manifest=self.MANIFEST_XML))
                revision_exists = self._check_revision_exists(
                    revision, module_path)
                if not revision_exists:
                    unreleased_projects.append((lib, module_path))
                else:
                    released_projects.append((lib, module_path))
            else:
                raise RuntimeError(
                    'No module path was found for {lib} in {module_paths}'.
                    format(lib=lib, module_paths=self.MODULE_PATHS_TXT))

        if released_projects:
            print 'Released GPL projects:', released_projects

        if unreleased_projects:
            raise ValueError(
                ('FAIL: The following GPL projects have NOT been released in '
                 'current tree: {}'.format(unreleased_projects)))

        print 'PASS: All GPL projects have source in current tree.'


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        'vndk_version', type=int,
        help='VNDK snapshot version to check, e.g. "27".')
    parser.add_argument('-b', '--branch', help='Branch to pull manifest from.')
    parser.add_argument('--build', help='Build number to pull manifest from.')
    return parser.parse_args()


def main():
    """For local testing purposes.

    Note: VNDK snapshot must be already installed under
      prebuilts/vndk/v{version}.
    """
    ANDROID_BUILD_TOP = utils.get_android_build_top()
    PREBUILTS_VNDK_DIR = utils.join_realpath(ANDROID_BUILD_TOP,
                                             'prebuilts/vndk')

    args = get_args()
    vndk_version = args.vndk_version
    install_dir = os.path.join(PREBUILTS_VNDK_DIR, 'v{}'.format(vndk_version))
    if not os.path.isdir(install_dir):
        raise ValueError(
            'Please provide valid VNDK version. {} does not exist.'
            .format(install_dir))

    temp_artifact_dir = tempfile.mkdtemp()
    os.chdir(temp_artifact_dir)
    manifest_pattern = 'manifest_{}.xml'.format(args.build)
    print 'Fetching {file} from {branch} (bid: {build})'.format(
        file=manifest_pattern, branch=args.branch, build=args.build)
    manifest_dest = os.path.join(temp_artifact_dir, utils.MANIFEST_FILE_NAME)
    utils.fetch_artifact(args.branch, args.build, manifest_pattern,
                         manifest_dest)

    license_checker = GPLChecker(install_dir, ANDROID_BUILD_TOP,
                                 temp_artifact_dir)
    try:
        license_checker.check_gpl_projects()
    except ValueError as error:
        print error
        raise
    finally:
        print 'Deleting temp_artifact_dir: {}'.format(temp_artifact_dir)
        shutil.rmtree(temp_artifact_dir)


if __name__ == '__main__':
    main()
