#!/usr/bin/env python3
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
import logging
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

    def __init__(self, install_dir, android_build_top, gpl_projects,
                 temp_artifact_dir, remote_git):
        """GPLChecker constructor.

        Args:
          install_dir: string, absolute path to the prebuilts/vndk/v{version}
            directory where the build files will be generated.
          android_build_top: string, absolute path to ANDROID_BUILD_TOP
          gpl_projects: list of strings, names of libraries under GPL
          temp_artifact_dir: string, temp directory to hold build artifacts
            fetched from Android Build server.
          remote_git: string, remote name to fetch and check if the revision of
            VNDK snapshot is included in the source if it is not in the current
            git repository.
        """
        self._android_build_top = android_build_top
        self._install_dir = install_dir
        self._remote_git = remote_git
        self._gpl_projects = gpl_projects
        self._manifest_file = os.path.join(temp_artifact_dir,
                                           self.MANIFEST_XML)

        if not os.path.isfile(self._manifest_file):
            raise RuntimeError(
                '{manifest} not found at {manifest_file}'.format(
                    manifest=self.MANIFEST_XML,
                    manifest_file=self._manifest_file))

    def _parse_module_paths(self):
        """Parses the module_paths.txt files into a dictionary,

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

        def _check_rev_list(revision):
            """Checks whether revision is reachable from HEAD of git project."""

            logging.info('Checking if revision {rev} exists in {proj}'.format(
                rev=revision, proj=git_project_path))
            try:
                cmd = [
                    'git', '-C', path, 'rev-list', 'HEAD..{}'.format(revision)
                ]
                output = utils.check_output(cmd).strip()
            except subprocess.CalledProcessError as error:
                logging.error('Error: {}'.format(error))
                return False
            else:
                if output:
                    logging.debug(
                        '{proj} does not have the following revisions: {rev}'.
                        format(proj=git_project_path, rev=output))
                    return False
                else:
                    logging.info(
                        'Found revision {rev} in project {proj}'.format(
                            rev=revision, proj=git_project_path))
            return True

        def _get_2nd_parent_if_merge_commit(revision):
            """Checks if the commit is merge commit.

            Returns:
              revision: string, the 2nd parent which is the merged commit.
              If the commit is not a merge commit, returns None.
            """
            logging.info(
                'Checking if the parent of revision {rev} exists in {proj}'.
                format(rev=revision, proj=git_project_path))
            try:
                cmd = [
                    'git', '-C', path, 'rev-parse', '--verify',
                    '{}^2'.format(revision)]
                parent_revision = utils.check_output(cmd).strip()
            except subprocess.CalledProcessError as error:
                logging.error(
                    'Failed to get parent of revision {rev} from "{remote}": '
                    '{err}'.format(
                        rev=revision, remote=self._remote_git, err=error))
                logging.error('{} is not a merge commit and must be included '
                    'in the current branch'.format(revision))
                return None
            else:
                return parent_revision

        if _check_rev_list(revision):
            return True

        # VNDK snapshots built from a *-release branch will have merge
        # CLs in the manifest because the *-dev branch is merged to the
        # *-release branch periodically. In order to extract the
        # revision relevant to the source of the git_project_path,
        # we find the parent of the merge commit.
        try:
            cmd = ['git', '-C', path, 'fetch', self._remote_git, revision]
            utils.check_call(cmd)
        except subprocess.CalledProcessError as error:
            logging.error(
                'Failed to fetch revision {rev} from "{remote}": '
                '{err}'.format(
                    rev=revision, remote=self._remote_git, err=error))
            logging.error('Try --remote to manually set remote name')
            raise

        parent_revision = _get_2nd_parent_if_merge_commit(revision)
        while True:
            if not parent_revision:
                return False
            if _check_rev_list(parent_revision):
                return True
            parent_revision = _get_2nd_parent_if_merge_commit(parent_revision)

    def check_gpl_projects(self):
        """Checks that all GPL projects have released sources.

        Raises:
          ValueError: There are GPL projects with unreleased sources.
        """
        logging.info('Starting license check for GPL projects...')

        if not self._gpl_projects:
            logging.info('No GPL projects found.')
            return

        logging.info('GPL projects found: {}'.format(', '.join(self._gpl_projects)))

        module_paths = self._parse_module_paths()
        manifest_projects = self._parse_manifest()
        released_projects = []
        unreleased_projects = []

        for name in self._gpl_projects:
            lib = name if name.endswith('.so') else name + '.so'
            if lib not in module_paths:
                raise RuntimeError(
                    'No module path was found for {lib} in {module_paths}'.
                    format(lib=lib, module_paths=self.MODULE_PATHS_TXT))

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

        if released_projects:
            logging.info('Released GPL projects: {}'.format(released_projects))

        if unreleased_projects:
            raise ValueError(
                ('FAIL: The following GPL projects have NOT been released in '
                 'current tree: {}'.format(unreleased_projects)))

        logging.info('PASS: All GPL projects have source in current tree.')


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        'vndk_version',
        type=utils.vndk_version_int,
        help='VNDK snapshot version to check, e.g. "{}".'.format(
            utils.MINIMUM_VNDK_VERSION))
    parser.add_argument('-b', '--branch', help='Branch to pull manifest from.')
    parser.add_argument('--build', help='Build number to pull manifest from.')
    parser.add_argument(
        '--remote',
        default='aosp',
        help=('Remote name to fetch and check if the revision of VNDK snapshot '
              'is included in the source to conform GPL license. default=aosp'))
    parser.add_argument('-m', '--modules', help='list of modules to check',
                        nargs='+')
    parser.add_argument(
        '-v',
        '--verbose',
        action='count',
        default=0,
        help='Increase output verbosity, e.g. "-v", "-vv".')
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
    remote = args.remote
    if not os.path.isdir(install_dir):
        raise ValueError(
            'Please provide valid VNDK version. {} does not exist.'
            .format(install_dir))
    utils.set_logging_config(args.verbose)

    temp_artifact_dir = tempfile.mkdtemp()
    os.chdir(temp_artifact_dir)
    manifest_pattern = 'manifest_{}.xml'.format(args.build)
    manifest_dest = os.path.join(temp_artifact_dir, utils.MANIFEST_FILE_NAME)
    logging.info('Fetching {file} from {branch} (bid: {build})'.format(
        file=manifest_pattern, branch=args.branch, build=args.build))
    utils.fetch_artifact(args.branch, args.build, manifest_pattern,
                         manifest_dest)

    license_checker = GPLChecker(install_dir, ANDROID_BUILD_TOP, args.modules,
                                 temp_artifact_dir, remote)
    try:
        license_checker.check_gpl_projects()
    except ValueError as error:
        logging.error('Error: {}'.format(error))
        raise
    finally:
        logging.info(
            'Deleting temp_artifact_dir: {}'.format(temp_artifact_dir))
        shutil.rmtree(temp_artifact_dir)

    logging.info('Done.')


if __name__ == '__main__':
    main()
