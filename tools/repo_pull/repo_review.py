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
import json
import os
import sys

try:
    from urllib.error import HTTPError  # PY3
except ImportError:
    from urllib2 import HTTPError  # PY2

from gerrit import (
    abandon, create_url_opener_from_args, delete_topic, query_change_lists,
    set_hashtags, set_review, set_topic)


def _get_labels_from_args(args):
    """Collect and check labels from args."""
    if not args.label:
        return None
    labels = {}
    for (name, value) in args.label:
        try:
            labels[name] = int(value)
        except ValueError:
            print('error: Label {} takes integer, but {} is specified'
                  .format(name, value), file=sys.stderr)
    return labels


# pylint: disable=redefined-builtin
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

    parser.add_argument('--abandon', help='Abandon a CL with a message')

    parser.add_argument('--add-hashtag', action='append', help='Add hashtag')
    parser.add_argument('--remove-hashtag', action='append',
                        help='Remove hashtag')
    parser.add_argument('--delete-hashtag', action='append',
                        help='Remove hashtag', dest='remove_hashtag')

    parser.add_argument('--set-topic', help='Set topic name')
    parser.add_argument('--delete-topic', action='store_true',
                        help='Delete topic name')
    parser.add_argument('--remove-topic', action='store_true',
                        help='Delete topic name', dest='delete_topic')

    return parser.parse_args()


def _has_task(args):
    """Determine whether a task has been specified in the arguments."""
    if args.label is not None or args.message is not None:
        return True
    if args.abandon is not None:
        return True
    if args.add_hashtag or args.remove_hashtag:
        return True
    if args.set_topic or args.delete_topic:
        return True
    return False


_SEP_SPLIT = '=' * 79
_SEP = '-' * 79


def _print_error(change, res_code, res_json):
    """Print the error message"""

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


def _do_task(change, func, *args, **kwargs):
    """Process a task and report errors when necessary."""
    try:
        res_code, res_json = func(*args)
    except HTTPError as error:
        res_code = error.code
        res_json = None

    if res_code != kwargs.get('expected_http_code', 200):
        _print_error(change, res_code, res_json)

        errors = kwargs.get('errors')
        if errors is not None:
            errors['num_errors'] += 1


def main():
    """Set review labels to selected change lists"""

    # Parse and check the command line options
    args = _parse_args()
    if not _has_task(args):
        print('error: Either --label, --message, --abandon, --add-hashtag, '
              '--remove-hashtag, --set-topic, or --delete-topic must be ',
              'specified', file=sys.stderr)
        sys.exit(1)

    # Convert label arguments
    labels = _get_labels_from_args(args)

    # Load authentication credentials
    url_opener = create_url_opener_from_args(args)

    # Retrieve change lists
    change_lists = query_change_lists(
        url_opener, args.gerrit, args.query, args.limits)
    if not change_lists:
        print('error: No matching change lists.', file=sys.stderr)
        sys.exit(1)

    # Print matching lists
    _print_change_lists(change_lists, file=sys.stdout)

    # Confirm
    _confirm('Do you want to continue?')

    # Post review votes
    errors = {'num_errors': 0}
    for change in change_lists:
        if args.label or args.message:
            _do_task(change, set_review, url_opener, args.gerrit, change['id'],
                     labels, args.message, errors=errors)
        if args.add_hashtag or args.remove_hashtag:
            _do_task(change, set_hashtags, url_opener, args.gerrit,
                     change['id'], args.add_hashtag, args.remove_hashtag,
                     errors=errors)
        if args.set_topic:
            _do_task(change, set_topic, url_opener, args.gerrit, change['id'],
                     args.set_topic, errors=errors)
        if args.delete_topic:
            _do_task(change, delete_topic, url_opener, args.gerrit,
                     change['id'], expected_http_code=204, errors=errors)
        if args.abandon:
            _do_task(change, abandon, url_opener, args.gerrit, change['id'],
                     args.abandon, errors=errors)

    if errors['num_errors']:
        sys.exit(1)


if __name__ == '__main__':
    main()
