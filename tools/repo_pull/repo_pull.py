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

"""A command line utility to pull multiple change lists from Gerrit."""

from __future__ import print_function

import argparse
import collections
import itertools
import json
import multiprocessing
import os
import re
import sys
import xml.dom.minidom

try:
    from urllib.parse import urlencode  # PY3
except ImportError:
    from urllib import urlencode  # PY2

try:
    from urllib.request import HTTPBasicAuthHandler, build_opener  # PY3
except ImportError:
    from urllib2 import HTTPBasicAuthHandler, build_opener  # PY2

try:
    from shlex import quote as _sh_quote  # PY3.3
except ImportError:
    # Shell language simple string pattern.  If a string matches this pattern,
    # it doesn't have to be quoted.
    _SHELL_SIMPLE_PATTERN = re.compile('^[a-zA-Z90-9_./-]+$')

    def _sh_quote(s):
        """Quote a string if it contains special characters."""
        return s if _SHELL_SIMPLE_PATTERN.match(s) else json.dumps(s)

try:
    from subprocess import PIPE, run  # PY3.5
except ImportError:
    from subprocess import CalledProcessError, PIPE, Popen

    class CompletedProcess(object):
        def __init__(self, args, returncode, stdout, stderr):
            self.args = args
            self.returncode = returncode
            self.stdout = stdout
            self.stderr = stderr

    def run(*args, **kwargs):
        check = kwargs.pop('check', False)

        try:
            input = kwargs.pop('input')
            assert 'stdin' not in kwargs
            kwargs['stdin'] = PIPE
        except KeyError:
            input = None

        proc = Popen(*args, **kwargs)
        try:
            stdout, stderr = proc.communicate(input)
        except:
            process.kill()
            process.wait()
            raise
        returncode = proc.wait()

        if check and returncode:
            raise CalledProcessError(returncode, args, stdout)
        return CompletedProcess(args, returncode, stdout, stderr)

if bytes is str:
    def write_bytes(data, file):  # PY2
        """Write bytes to a file."""
        file.write(data)
else:
    def write_bytes(data, file):  # PY3
        """Write bytes to a file."""
        file.buffer.write(data)


class ChangeList(object):
    """A ChangeList to be checked out."""

    def __init__(self, project, project_dir, fetch, commit_sha1, commit,
                 change_list):
        """Initialize a ChangeList instance."""
        self.project = project
        self.project_dir = project_dir
        self.number = change_list['_number']

        self.fetch = fetch
        self.fetch_url = fetch['http']['url']
        self.fetch_ref = fetch['http']['ref']

        self.commit_sha1 = commit_sha1
        self.commit = commit
        self.parents = commit['parents']

        self.change_list = change_list


    def is_merge(self):
        """Check whether this change list a merge commit."""
        return len(self.parents) > 1


def find_manifest_xml(dir_path):
    """Find the path to manifest.xml for this Android source tree."""
    dir_path_prev = None
    while dir_path != dir_path_prev:
        path = os.path.join(dir_path, '.repo', 'manifest.xml')
        if os.path.exists(path):
            return path
        dir_path_prev = dir_path
        dir_path = os.path.dirname(dir_path)
    raise ValueError('.repo dir not found')


def build_project_name_to_directory_dict(manifest_path):
    """Build the mapping from Gerrit project name to source tree project
    directory path."""
    project_dirs = {}
    parsed_xml = xml.dom.minidom.parse(manifest_path)
    projects = parsed_xml.getElementsByTagName('project')
    for project in projects:
        name = project.getAttribute('name')
        path = project.getAttribute('path')
        if path:
            project_dirs[name] = path
        else:
            project_dirs[name] = name
    return project_dirs


def load_auth(cookie_file_path):
    """Load username and password from .gitcookies and return an
    HTTPBasicAuthHandler."""
    auth_handler = HTTPBasicAuthHandler()
    with open(cookie_file_path, 'r') as cookie_file:
        for lineno, line in enumerate(cookie_file, start=1):
            if line.startswith('#HttpOnly_'):
                line = line[len('#HttpOnly_'):]
            if not line or line[0] == '#':
                continue
            row = line.split('\t')
            if len(row) != 7:
                continue
            domain = row[0]
            cookie = row[6]
            sep = cookie.find('=')
            if sep == -1:
                continue
            username = cookie[0:sep]
            password = cookie[sep + 1:]
            auth_handler.add_password(domain, domain, username, password)
    return auth_handler


def query_change_lists(gerrit, query_string, gitcookies, limits):
    """Query change lists."""
    data = [
        ('q', query_string),
        ('o', 'CURRENT_REVISION'),
        ('o', 'CURRENT_COMMIT'),
        ('n', str(limits)),
    ]
    url = gerrit + '/a/changes/?' + urlencode(data)

    auth_handler = load_auth(gitcookies)
    opener = build_opener(auth_handler)

    response_file = opener.open(url)
    try:
        # Trim cross site script inclusion (XSSI) protector
        data = response_file.read().decode('utf-8')[4:]

        # Parse responsed JSON
        return json.loads(data)
    finally:
        response_file.close()


def group_and_sort_change_lists(change_lists, project_dirs):
    """Build a dict that maps projects to a list of topologically sorted change
    lists."""

    # Build a dict that map projects to dicts that map commits to changes.
    projects = collections.defaultdict(dict)
    for change_list in change_lists:
        for commit_sha1, value in change_list['revisions'].items():
            fetch = value['fetch']
            commit = value['commit']

        project = change_list['project']
        project_dir = project_dirs[project]

        project_changes = projects[project]
        if commit_sha1 in project_changes:
            raise KeyError('repeated commit sha1 "{}" in project "{}"'.format(
                commit_sha1, project))

        project_changes[commit_sha1] = ChangeList(
            project, project_dir, fetch, commit_sha1, commit, change_list)

    # Sort all change lists in a project in post ordering.
    def _sort_project_change_lists(changes):
        visited_changes = set()
        sorted_changes = []

        def post_order_traverse(change):
            visited_changes.add(change)
            for parent in change.parents:
                parent_change = changes.get(parent['commit'])
                if parent_change and parent_change not in visited_changes:
                    post_order_traverse(parent_change)
            sorted_changes.append(change)

        for change in sorted(changes.values(), key=lambda x: x.number):
            if change not in visited_changes:
                post_order_traverse(change)

        return sorted_changes

    # Sort changes in each projects
    sorted_changes = []
    for project in sorted(projects.keys()):
        sorted_changes.append(_sort_project_change_lists(projects[project]))

    return sorted_changes


def _main_json(args):
    """Print the change lists in JSON format."""
    change_lists = _get_change_lists_from_args(args)
    json.dump(change_lists, sys.stdout, indent=4, separators=(', ', ': '))
    print()  # Print the end-of-line


# Git commands for merge commits
_MERGE_COMMANDS = {
    'merge': ['git', 'merge', '--no-edit'],
    'merge-ff-only': ['git', 'merge', '--no-edit', '--ff-only'],
    'merge-no-ff': ['git', 'merge', '--no-edit', '--no-ff'],
    'reset': ['git', 'reset', '--hard'],
    'checkout': ['git', 'checkout'],
}


def build_pull_commands(change, branch_name, merge_opt):
    """Build command lines for each change.  The command lines will be passed
    to subprocess.run()."""

    cmds = []
    cmds.append(['repo', 'start', branch_name])
    cmds.append(['git', 'fetch', change.fetch_url, change.fetch_ref])
    if change.is_merge():
        cmds.append(_MERGE_COMMANDS[merge_opt] + ['FETCH_HEAD'])
    else:
        cmds.append(['git', 'cherry-pick', '--allow-empty', 'FETCH_HEAD'])
    return cmds


def _sh_quote_command(cmd):
    """Convert a command (an argument to subprocess.run()) to a shell command
    string."""
    return ' '.join(_sh_quote(x) for x in cmd)


def _sh_quote_commands(cmds):
    """Convert multiple commands (arguments to subprocess.run()) to shell
    command strings."""
    return ' && '.join(_sh_quote_command(cmd) for cmd in cmds)


def _main_bash(args):
    """Print the bash command to pull the change lists."""

    branch_name = _get_topic_branch_from_args(args)

    manifest_path = _get_manifest_xml_from_args(args)
    project_dirs = build_project_name_to_directory_dict(manifest_path)

    change_lists = _get_change_lists_from_args(args)
    change_list_groups = group_and_sort_change_lists(change_lists, project_dirs)

    for changes in change_list_groups:
        for change in changes:
            cmds = []
            cmds.append(['pushd', change.project_dir])
            cmds.extend(build_pull_commands(change, branch_name, args.merge))
            cmds.append(['popd'])
            print(_sh_quote_commands(cmds))


def _do_pull_change_lists_for_project(task):
    """Pick a list of changes (usually under a project directory)."""
    changes, task_opts = task

    branch_name = task_opts['branch_name']
    merge_opt = task_opts['merge_opt']

    for i, change in enumerate(changes):
        cwd = change.project_dir
        print(change.commit_sha1[0:10], i + 1, cwd)
        for cmd in build_pull_commands(change, branch_name, merge_opt):
            proc = run(cmd, cwd=cwd, stderr=PIPE)
            if proc.returncode != 0:
                return (change, changes[i + 1:], cmd, proc.stderr)
    return None


def _main_pull(args):
    """Pull the change lists."""

    branch_name = _get_topic_branch_from_args(args)

    manifest_path = _get_manifest_xml_from_args(args)
    project_dirs = build_project_name_to_directory_dict(manifest_path)

    # Collect change lists
    change_lists = _get_change_lists_from_args(args)
    change_list_groups = group_and_sort_change_lists(change_lists, project_dirs)

    # Build the options list for tasks
    task_opts = {
        'branch_name': branch_name,
        'merge_opt': args.merge,
    }

    # Run the commands to pull the change lists
    if args.parallel <= 1:
        results = [_do_pull_change_lists_for_project((changes, task_opts))
                   for changes in change_list_groups]
    else:
        pool = multiprocessing.Pool(processes=args.parallel)
        results = pool.map(_do_pull_change_lists_for_project,
                           zip(change_list_groups, itertools.repeat(task_opts)))

    # Print failures and tracebacks
    failed_results = [result for result in results if result]
    if failed_results:
        _SEPARATOR = '=' * 78
        _SEPARATOR_SUB = '-' * 78

        print(_SEPARATOR, file=sys.stderr)
        for failed_change, skipped_changes, cmd, errors in failed_results:
            print('PROJECT:', failed_change.project, file=sys.stderr)
            print('FAILED COMMIT:', failed_change.commit_sha1, file=sys.stderr)
            for change in skipped_changes:
                print('PENDING COMMIT:', change.commit_sha1, file=sys.stderr)
            print(_SEPARATOR_SUB, file=sys.stderr)
            print('FAILED COMMAND:', _sh_quote_command(cmd), file=sys.stderr)
            write_bytes(errors, file=sys.stderr)
            print(_SEPARATOR, file=sys.stderr)
        sys.exit(1)


def _parse_args():
    """Parse command line options."""
    parser = argparse.ArgumentParser()

    parser.add_argument('command', choices=['pull', 'bash', 'json'],
                        help='Commands')

    parser.add_argument('query', help='Change list query string')
    parser.add_argument('-g', '--gerrit', required=True,
                        help='Gerrit review URL')

    parser.add_argument('--gitcookies',
                        default=os.path.expanduser('~/.gitcookies'),
                        help='Gerrit cookie file')
    parser.add_argument('--manifest', help='Manifest')
    parser.add_argument('--limits', default=1000,
                        help='Max number of change lists')

    parser.add_argument('-m', '--merge',
                        choices=_MERGE_COMMANDS.keys(),
                        default='merge-ff-only',
                        help='Method to pull merge commits')

    parser.add_argument('-b', '--branch',
                        help='Topic branch name to start with')

    parser.add_argument('-j', '--parallel', default=1, type=int,
                        help='Number of parallel running commands')

    return parser.parse_args()


def _get_manifest_xml_from_args(args):
    """Get the path to manifest.xml from args."""
    manifest_path = args.manifest
    if not args.manifest:
        manifest_path = find_manifest_xml(os.getcwd())
    return manifest_path


def _get_change_lists_from_args(args):
    """Query the change lists by args."""
    return query_change_lists(args.gerrit, args.query, args.gitcookies,
                              args.limits)


def _get_topic_branch_from_args(args):
    """Get the topic branch name from args."""
    if not args.branch:
        print('error: --branch must be specified')
        sys.exit(1)
    return args.branch


def main():
    args = _parse_args()
    if args.command == 'json':
        _main_json(args)
    elif args.command == 'bash':
        _main_bash(args)
    elif args.command == 'pull':
        _main_pull(args)
    else:
        raise KeyError('unknown command')

if __name__ == '__main__':
    main()
