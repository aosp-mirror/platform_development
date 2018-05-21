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

"""A command line utility to post review votes to multiple CLs on Gerrit."""

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
    from urllib.request import (
        HTTPBasicAuthHandler, Request, build_opener)  # PY3
except ImportError:
    from urllib2 import HTTPBasicAuthHandler, Request, build_opener  # PY2


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


def _decode_xssi_json(data):
    """Trim XSSI protector and decode JSON objects."""
    # Trim cross site script inclusion (XSSI) protector
    data = data.decode('utf-8')[4:]
    # Parse JSON objects
    return json.loads(data)


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
        return _decode_xssi_json(response_file.read())
    finally:
        response_file.close()


def set_review(gerrit, gitcookies, change_id, labels, message):
    """Set review votes to a change list."""

    url = '{}/a/changes/{}/revisions/current/review'.format(gerrit, change_id)

    auth_handler = load_auth(gitcookies)
    opener = build_opener(auth_handler)

    data = {}
    if labels:
        data['labels'] = labels
    if message:
        data['message'] = message
    data = json.dumps(data).encode('utf-8')

    headers = {
        'Content-Type': 'application/json; charset=UTF-8',
    }

    request = Request(url, data, headers)
    response_file = opener.open(request)
    try:
        res_code = response_file.getcode()
        res_json = _decode_xssi_json(response_file.read())
        return (res_code, res_json)
    finally:
        response_file.close()


def _get_change_lists_from_args(args):
    """Query the change lists by args."""
    return query_change_lists(args.gerrit, args.query, args.gitcookies,
                              args.limits)


def _get_labels_from_args(args):
    """Collect and check labels from args."""
    if not args.label:
        print('error: --label must be specified', file=sys.stderr)
        sys.exit(1)
    labels = {}
    for (name, value) in args.label:
        try:
            labels[name] = int(value)
        except ValueError:
            print('error: Label {} takes integer, but {} is specified'
                  .format(name, value), file=sys.stderr)
    return labels


def _print_change_lists(change_lists, file=sys.stdout):
    """Print matching change lists for each projects."""
    change_lists = sorted(
        change_lists, key=lambda change: (change['project'], change['_number']))

    prev_project = None
    print('Change Lists:', file=file)
    for change in change_lists:
        project = change['project']
        if project != prev_project:
            print(' ', project, file=file)
            prev_project = project

        change_id = change['change_id']
        revision_sha1 = change['current_revision']
        revision = change['revisions'][revision_sha1]
        subject = revision['commit']['subject']
        print('   ', change_id, '--', subject, file=file)


def _confirm(question):
    """Confirm before proceeding."""
    try:
        if input(question + ' [yn] ').lower() not in {'y', 'yes'}:
            print('Cancelled', file=sys.stderr)
            sys.exit(1)
    except KeyboardInterrupt:
        print('Cancelled', file=sys.stderr)
        sys.exit(1)


def _parse_args():
    """Parse command line options."""
    parser = argparse.ArgumentParser()

    parser.add_argument('query', help='Change list query string')
    parser.add_argument('-g', '--gerrit', required=True,
                        help='Gerrit review URL')

    parser.add_argument('--gitcookies',
                        default=os.path.expanduser('~/.gitcookies'),
                        help='Gerrit cookie file')
    parser.add_argument('--limits', default=1000,
                        help='Max number of change lists')

    parser.add_argument('-l', '--label', nargs=2, action='append',
                        help='Labels to be added')
    parser.add_argument('-m', '--message', help='Review message')

    return parser.parse_args()


_SEP_SPLIT = '=' * 79
_SEP = '-' * 79


def main():
    """Set review labels to selected change lists"""

    args = _parse_args()

    # Convert label arguments
    labels = _get_labels_from_args(args)

    # Retrieve change lists
    change_lists = _get_change_lists_from_args(args)
    if not change_lists:
        print('error: No matching change lists.', file=sys.stderr)
        sys.exit(1)

    # Print matching lists
    _print_change_lists(change_lists, file=sys.stdout)

    # Confirm
    _confirm('Do you want to continue?')

    # Post review votes
    has_error = False
    for change in change_lists:
        try:
            res_code, res_json = set_review(
                args.gerrit, args.gitcookies, change['id'], labels,
                args.message)
        except HTTPError as e:
            res_code = e.code
            res_json = None

        if res_code != 200:
            has_error = True

            change_id = change['change_id']
            project = change['project']
            revision_sha1 = change['current_revision']
            revision = change['revisions'][revision_sha1]
            subject = revision['commit']['subject']

            print(_SEP_SPLIT, file=sys.stderr)
            print('Project:', project, file=sys.stderr)
            print('Change-Id:', change_id, file=sys.stderr)
            print('Subject:', subject, file=sys.stderr)
            print('HTTP status code:', res_code, file=sys.stderr)
            if res_json:
                print(_SEP, file=sys.stderr)
                json.dump(res_json, sys.stderr, indent=4,
                          separators=(', ', ': '))
                print(file=sys.stderr)
            print(_SEP_SPLIT, file=sys.stderr)

    if has_error:
        sys.exit(1)


if __name__ == '__main__':
    main()
