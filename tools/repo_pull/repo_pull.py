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
import os.path
import re
import sys
import xml.dom.minidom

from gerrit import create_url_opener_from_args, query_change_lists

try:
    # pylint: disable=redefined-builtin
    from __builtin__ import raw_input as input  # PY2
except ImportError:
    pass

try:
    from shlex import quote as _sh_quote  # PY3.3
except ImportError:
    # Shell language simple string pattern.  If a string matches this pattern,
    # it doesn't have to be quoted.
    _SHELL_SIMPLE_PATTERN = re.compile('^[a-zA-Z90-9_./-]+$')

    def _sh_quote(txt):
        """Quote a string if it contains special characters."""
        return txt if _SHELL_SIMPLE_PATTERN.match(txt) else json.dumps(txt)

try:
    from subprocess import PIPE, run  # PY3.5
except ImportError:
    from subprocess import CalledProcessError, PIPE, Popen

    class CompletedProcess(object):
        """Process execution result returned by subprocess.run()."""
        # pylint: disable=too-few-public-methods

        def __init__(self, args, returncode, stdout, stderr):
            self.args = args
            self.returncode = returncode
            self.stdout = stdout
            self.stderr = stderr

    def run(*args, **kwargs):
        """Run a command with subprocess.Popen() and redirect input/output."""

        check = kwargs.pop('check', False)

        try:
            stdin = kwargs.pop('input')
            assert 'stdin' not in kwargs
            kwargs['stdin'] = PIPE
        except KeyError:
            stdin = None

        proc = Popen(*args, **kwargs)
        try:
            stdout, stderr = proc.communicate(stdin)
        except:
            proc.kill()
            proc.wait()
            raise
        returncode = proc.wait()

        if check and returncode:
            raise CalledProcessError(returncode, args, stdout)
        return CompletedProcess(args, returncode, stdout, stderr)


if bytes is str:
    def write_bytes(data, file):  # PY2
        """Write bytes to a file."""
        # pylint: disable=redefined-builtin
        file.write(data)
else:
    def write_bytes(data, file):  # PY3
        """Write bytes to a file."""
        # pylint: disable=redefined-builtin
        file.buffer.write(data)


def _confirm(question, default, file=sys.stderr):
    """Prompt a yes/no question and convert the answer to a boolean value."""
    # pylint: disable=redefined-builtin
    answers = {'': default, 'y': True, 'yes': True, 'n': False, 'no': False}
    suffix = '[Y/n] ' if default else ' [y/N] '
    while True:
        file.write(question + suffix)
        file.flush()
        ans = answers.get(input().lower())
        if ans is not None:
            return ans


class ChangeList(object):
    """A ChangeList to be checked out."""
    # pylint: disable=too-few-public-methods,too-many-instance-attributes

    def __init__(self, project, fetch, commit_sha1, commit, change_list):
        """Initialize a ChangeList instance."""
        # pylint: disable=too-many-arguments

        self.project = project
        self.number = change_list['_number']

        self.fetch = fetch

        fetch_git = None
        for protocol in ('http', 'sso', 'rpc'):
            fetch_git = fetch.get(protocol)
            if fetch_git:
                break

        if not fetch_git:
            raise ValueError(
                'unknown fetch protocols: ' + str(list(fetch.keys())))

        self.fetch_url = fetch_git['url']
        self.fetch_ref = fetch_git['ref']

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


def build_project_name_dir_dict(manifest_path):
    """Build the mapping from Gerrit project name to source tree project
    directory path."""
    project_dirs = {}
    parsed_xml = xml.dom.minidom.parse(manifest_path)

    includes = parsed_xml.getElementsByTagName('include')
    for include in includes:
        include_path = include.getAttribute('name')
        if not os.path.isabs(include_path):
            manifest_dir = os.path.dirname(os.path.realpath(manifest_path))
            include_path = os.path.join(manifest_dir, include_path)
        project_dirs.update(build_project_name_dir_dict(include_path))

    projects = parsed_xml.getElementsByTagName('project')
    for project in projects:
        name = project.getAttribute('name')
        path = project.getAttribute('path')
        if path:
            project_dirs[name] = path
        else:
            project_dirs[name] = name

    return project_dirs


def group_and_sort_change_lists(change_lists):
    """Build a dict that maps projects to a list of topologically sorted change
    lists."""

    # Build a dict that map projects to dicts that map commits to changes.
    projects = collections.defaultdict(dict)
    for change_list in change_lists:
        commit_sha1 = None
        for commit_sha1, value in change_list['revisions'].items():
            fetch = value['fetch']
            commit = value['commit']

        if not commit_sha1:
            raise ValueError('bad revision')

        project = change_list['project']

        project_changes = projects[project]
        if commit_sha1 in project_changes:
            raise KeyError('repeated commit sha1 "{}" in project "{}"'.format(
                commit_sha1, project))

        project_changes[commit_sha1] = ChangeList(
            project, fetch, commit_sha1, commit, change_list)

    # Sort all change lists in a project in post ordering.
    def _sort_project_change_lists(changes):
        visited_changes = set()
        sorted_changes = []

        def _post_order_traverse(change):
            visited_changes.add(change)
            for parent in change.parents:
                parent_change = changes.get(parent['commit'])
                if parent_change and parent_change not in visited_changes:
                    _post_order_traverse(parent_change)
            sorted_changes.append(change)

        for change in sorted(changes.values(), key=lambda x: x.number):
            if change not in visited_changes:
                _post_order_traverse(change)

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


# Git commands for non-merge commits
_PICK_COMMANDS = {
    'pick': ['git', 'cherry-pick', '--allow-empty'],
    'merge': ['git', 'merge', '--no-edit'],
    'merge-ff-only': ['git', 'merge', '--no-edit', '--ff-only'],
    'merge-no-ff': ['git', 'merge', '--no-edit', '--no-ff'],
    'reset': ['git', 'reset', '--hard'],
    'checkout': ['git', 'checkout'],
}


def build_pull_commands(change, branch_name, merge_opt, pick_opt):
    """Build command lines for each change.  The command lines will be passed
    to subprocess.run()."""

    cmds = []
    if branch_name is not None:
        cmds.append(['repo', 'start', branch_name])
    cmds.append(['git', 'fetch', change.fetch_url, change.fetch_ref])
    if change.is_merge():
        cmds.append(_MERGE_COMMANDS[merge_opt] + ['FETCH_HEAD'])
    else:
        cmds.append(_PICK_COMMANDS[pick_opt] + ['FETCH_HEAD'])
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

    branch_name = _get_local_branch_name_from_args(args)

    manifest_path = _get_manifest_xml_from_args(args)
    project_dirs = build_project_name_dir_dict(manifest_path)

    change_lists = _get_change_lists_from_args(args)
    change_list_groups = group_and_sort_change_lists(change_lists)

    for changes in change_list_groups:
        for change in changes:
            project_dir = project_dirs.get(change.project, change.project)
            cmds = []
            cmds.append(['pushd', project_dir])
            cmds.extend(build_pull_commands(
                change, branch_name, args.merge, args.pick))
            cmds.append(['popd'])
            print(_sh_quote_commands(cmds))


def _do_pull_change_lists_for_project(task):
    """Pick a list of changes (usually under a project directory)."""
    changes, task_opts = task

    branch_name = task_opts['branch_name']
    merge_opt = task_opts['merge_opt']
    pick_opt = task_opts['pick_opt']
    project_dirs = task_opts['project_dirs']

    for i, change in enumerate(changes):
        try:
            cwd = project_dirs[change.project]
        except KeyError:
            err_msg = 'error: project "{}" cannot be found in manifest.xml\n'
            err_msg = err_msg.format(change.project).encode('utf-8')
            return (change, changes[i + 1:], [], err_msg)

        print(change.commit_sha1[0:10], i + 1, cwd)
        cmds = build_pull_commands(change, branch_name, merge_opt, pick_opt)
        for cmd in cmds:
            proc = run(cmd, cwd=cwd, stderr=PIPE)
            if proc.returncode != 0:
                return (change, changes[i + 1:], cmd, proc.stderr)
    return None


def _print_pull_failures(failures, file=sys.stderr):
    """Print pull failures and tracebacks."""
    # pylint: disable=redefined-builtin

    separator = '=' * 78
    separator_sub = '-' * 78

    print(separator, file=file)
    for failed_change, skipped_changes, cmd, errors in failures:
        print('PROJECT:', failed_change.project, file=file)
        print('FAILED COMMIT:', failed_change.commit_sha1, file=file)
        for change in skipped_changes:
            print('PENDING COMMIT:', change.commit_sha1, file=file)
        print(separator_sub, file=sys.stderr)
        print('FAILED COMMAND:', _sh_quote_command(cmd), file=file)
        write_bytes(errors, file=sys.stderr)
        print(separator, file=sys.stderr)


def _main_pull(args):
    """Pull the change lists."""

    branch_name = _get_local_branch_name_from_args(args)

    manifest_path = _get_manifest_xml_from_args(args)
    project_dirs = build_project_name_dir_dict(manifest_path)

    # Collect change lists
    change_lists = _get_change_lists_from_args(args)
    change_list_groups = group_and_sort_change_lists(change_lists)

    # Build the options list for tasks
    task_opts = {
        'branch_name': branch_name,
        'merge_opt': args.merge,
        'pick_opt': args.pick,
        'project_dirs': project_dirs,
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
    failures = [result for result in results if result]
    if failures:
        _print_pull_failures(failures)
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
                        choices=sorted(_MERGE_COMMANDS.keys()),
                        default='merge-ff-only',
                        help='Method to pull merge commits')

    parser.add_argument('-p', '--pick',
                        choices=sorted(_PICK_COMMANDS.keys()),
                        default='pick',
                        help='Method to pull merge commits')

    parser.add_argument('-b', '--branch',
                        help='Local branch name for `repo start`')

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
    url_opener = create_url_opener_from_args(args)
    return query_change_lists(url_opener, args.gerrit, args.query, args.limits)


def _get_local_branch_name_from_args(args):
    """Get the local branch name from args."""
    if not args.branch and not _confirm(
            'Do you want to continue without local branch name?', False):
        print('error: `-b` or `--branch` must be specified', file=sys.stderr)
        sys.exit(1)
    return args.branch


def main():
    """Main function"""
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
