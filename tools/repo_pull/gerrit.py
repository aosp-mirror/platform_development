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

"""Gerrit Restful API client library."""

from __future__ import print_function

import argparse
import base64
import json
import os
import sys

try:
    from urllib.request import (
        HTTPBasicAuthHandler, Request, build_opener)  # PY3
except ImportError:
    from urllib2 import (
        HTTPBasicAuthHandler, Request, build_opener)  # PY2

try:
    # pylint: disable=ungrouped-imports
    from urllib.parse import urlencode, urlparse  # PY3
except ImportError:
    # pylint: disable=ungrouped-imports
    from urllib import urlencode  # PY2
    from urlparse import urlparse  # PY2


def load_auth_credentials_from_file(cookie_file):
    """Load credentials from an opened .gitcookies file."""
    credentials = {}
    for line in cookie_file:
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

        credentials[domain] = (username, password)
    return credentials


def load_auth_credentials(cookie_file_path):
    """Load credentials from a .gitcookies file path."""
    with open(cookie_file_path, 'r') as cookie_file:
        return load_auth_credentials_from_file(cookie_file)


def create_url_opener(cookie_file_path, domain):
    """Load username and password from .gitcookies and return a URL opener with
    an authentication handler."""

    # Load authentication credentials
    credentials = load_auth_credentials(cookie_file_path)
    username, password = credentials[domain]

    # Create URL opener with authentication handler
    auth_handler = HTTPBasicAuthHandler()
    auth_handler.add_password(domain, domain, username, password)
    return build_opener(auth_handler)


def create_url_opener_from_args(args):
    """Create URL opener from command line arguments."""

    domain = urlparse(args.gerrit).netloc

    try:
        return create_url_opener(args.gitcookies, domain)
    except KeyError:
        print('error: Cannot find the domain "{}" in "{}". '
              .format(domain, args.gitcookies), file=sys.stderr)
        print('error: Please check the Gerrit Code Review URL or follow the '
              'instructions in '
              'https://android.googlesource.com/platform/development/'
              '+/master/tools/repo_pull#installation', file=sys.stderr)
        sys.exit(1)


def _decode_xssi_json(data):
    """Trim XSSI protector and decode JSON objects."""

    # Decode UTF-8
    data = data.decode('utf-8')

    # Trim cross site script inclusion (XSSI) protector
    if data[0:4] != ')]}\'':
        raise ValueError('unexpected responsed content: ' + data)
    data = data[4:]

    # Parse JSON objects
    return json.loads(data)


def query_change_lists(url_opener, gerrit, query_string, limits):
    """Query change lists."""
    data = [
        ('q', query_string),
        ('o', 'CURRENT_REVISION'),
        ('o', 'CURRENT_COMMIT'),
        ('n', str(limits)),
    ]
    url = gerrit + '/a/changes/?' + urlencode(data)

    response_file = url_opener.open(url)
    try:
        return _decode_xssi_json(response_file.read())
    finally:
        response_file.close()


def _make_json_post_request(url_opener, url, data, method='POST'):
    data = json.dumps(data).encode('utf-8')
    headers = {
        'Content-Type': 'application/json; charset=UTF-8',
    }

    request = Request(url, data, headers)
    request.get_method = lambda: method
    response_file = url_opener.open(request)
    try:
        res_code = response_file.getcode()
        res_json = _decode_xssi_json(response_file.read())
        return (res_code, res_json)
    finally:
        response_file.close()


def set_review(url_opener, gerrit_url, change_id, labels, message):
    """Set review votes to a change list."""

    url = '{}/a/changes/{}/revisions/current/review'.format(
        gerrit_url, change_id)

    data = {}
    if labels:
        data['labels'] = labels
    if message:
        data['message'] = message

    return _make_json_post_request(url_opener, url, data)


def abandon(url_opener, gerrit_url, change_id, message):
    """Abandon a change list."""

    url = '{}/a/changes/{}/abandon'.format(gerrit_url, change_id)

    data = {}
    if message:
        data['message'] = message

    return _make_json_post_request(url_opener, url, data)


def set_topic(url_opener, gerrit_url, change_id, name):
    """Set the topic name."""

    url = '{}/a/changes/{}/topic'.format(gerrit_url, change_id)
    data = {'topic': name}
    return _make_json_post_request(url_opener, url, data, method='PUT')


def delete_topic(url_opener, gerrit_url, change_id):
    """Delete the topic name."""

    url = '{}/a/changes/{}/topic'.format(gerrit_url, change_id)
    request = Request(url)
    request.get_method = lambda: 'DELETE'
    response_file = url_opener.open(request)
    try:
        return (response_file.getcode(), response_file.read())
    finally:
        response_file.close()


def set_hashtags(url_opener, gerrit_url, change_id, add_tags=None,
                 remove_tags=None):
    """Add or remove hash tags."""

    url = '{}/a/changes/{}/hashtags'.format(gerrit_url, change_id)

    data = {}
    if add_tags:
        data['add'] = add_tags
    if remove_tags:
        data['remove'] = remove_tags

    return _make_json_post_request(url_opener, url, data)


def get_patch(url_opener, gerrit_url, change_id, revision_id='current'):
    """Download the patch file."""

    url = '{}/a/changes/{}/revisions/{}/patch'.format(
        gerrit_url, change_id, revision_id)

    response_file = url_opener.open(url)
    try:
        return base64.b64decode(response_file.read())
    finally:
        response_file.close()


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

    return parser.parse_args()


def main():
    """Main function"""
    args = _parse_args()

    # Query change lists
    url_opener = create_url_opener_from_args(args)
    change_lists = query_change_lists(
        url_opener, args.gerrit, args.query, args.limits)

    # Print the result
    json.dump(change_lists, sys.stdout, indent=4, separators=(', ', ': '))
    print()  # Print the end-of-line

if __name__ == '__main__':
    main()
