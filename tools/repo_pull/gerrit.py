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
import xml.dom.minidom

try:
    import ssl
    _HAS_SSL = True
except ImportError:
    _HAS_SSL = False

try:
    # PY3
    from urllib.error import HTTPError
    from urllib.parse import urlencode, urlparse
    from urllib.request import (
        HTTPBasicAuthHandler, HTTPHandler, OpenerDirector, Request,
        build_opener
    )
    if _HAS_SSL:
        from urllib.request import HTTPSHandler
except ImportError:
    # PY2
    from urllib import urlencode
    from urllib2 import (
        HTTPBasicAuthHandler, HTTPError, HTTPHandler, OpenerDirector, Request,
        build_opener
    )
    if _HAS_SSL:
        from urllib2 import HTTPSHandler
    from urlparse import urlparse

try:
    from http.client import HTTPResponse
except ImportError:
    from httplib import HTTPResponse

try:
    from urllib import addinfourl
    _HAS_ADD_INFO_URL = True
except ImportError:
    _HAS_ADD_INFO_URL = False

try:
    from io import BytesIO
except ImportError:
    from StringIO import StringIO as BytesIO

try:
    # PY3.5
    from subprocess import PIPE, run
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


class CurlSocket(object):
    """A mock socket object that loads the response from a curl output file."""

    def __init__(self, file_obj):
        self._file_obj = file_obj

    def makefile(self, *args):
        return self._file_obj

    def close(self):
        self._file_obj = None


def _build_curl_command_for_request(curl_command_name, req):
    """Build the curl command line for an HTTP/HTTPS request."""

    cmd = [curl_command_name]

    # Adds `--no-progress-meter` to hide the progress bar.
    cmd.append('--no-progress-meter')

    # Adds `-i` to print the HTTP response headers to stdout.
    cmd.append('-i')

    # Uses HTTP 1.1.  The `http.client` module can only parse HTTP 1.1 headers.
    cmd.append('--http1.1')

    # Specifies the request method.
    cmd.append('-X')
    cmd.append(req.get_method())

    # Adds the request headers.
    for name, value in req.headers.items():
        cmd.append('-H')
        cmd.append(name + ': ' + value)

    # Adds the request data.
    if req.data:
        cmd.append('-d')
        cmd.append('@-')

    # Adds the request full URL.
    cmd.append(req.get_full_url())
    return cmd


def _handle_open_with_curl(curl_command_name, req):
    """Send the HTTP request with CURL and return a response object that can be
    handled by urllib."""

    # Runs the curl command.
    cmd = _build_curl_command_for_request(curl_command_name, req)
    proc = run(cmd, stdout=PIPE, input=req.data, check=True)

    # Wraps the curl output with a socket-like object.
    outfile = BytesIO(proc.stdout)
    socket = CurlSocket(outfile)

    response = HTTPResponse(socket)
    try:
        # Parses the response header.
        response.begin()
    except:
        response.close()
        raise

    # Overrides `Transfer-Encoding: chunked` because curl combines chunks.
    response.chunked = False
    response.chunk_left = None

    if _HAS_ADD_INFO_URL:
        # PY2 urllib2 expects a different return object.
        result = addinfourl(outfile, response.msg, req.get_full_url())
        result.code = response.status
        result.msg = response.reason
        return result

    return response  # PY3


class CurlHTTPHandler(HTTPHandler):
    """CURL HTTP handler."""

    def __init__(self, curl_command_name):
        self._curl_command_name = curl_command_name

    def http_open(self, req):
        return _handle_open_with_curl(self._curl_command_name, req)


if _HAS_SSL:
    class CurlHTTPSHandler(HTTPSHandler):
        """CURL HTTPS handler."""

        def __init__(self, curl_command_name):
            self._curl_command_name = curl_command_name

        def https_open(self, req):
            return _handle_open_with_curl(self._curl_command_name, req)


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


def _domain_matches(domain_name, domain_pattern):
    """Returns whether `domain_name` matches `domain_pattern` under the
    definition of RFC 6265 (Section 4.1.2.3 and 5.1.3).

    Pattern matching rule defined by Section 5.1.3:

        >>> _domain_matches('example.com', 'example.com')
        True
        >>> _domain_matches('a.example.com', 'example.com')
        True
        >>> _domain_matches('aaaexample.com', 'example.com')
        False

    If the domain pattern starts with '.', '.' is ignored (Section 4.1.2.3):

        >>> _domain_matches('a.example.com', '.example.com')
        True
        >>> _domain_matches('example.com', '.example.com')
        True

    See also:
        https://datatracker.ietf.org/doc/html/rfc6265#section-4.1.2.3
        https://datatracker.ietf.org/doc/html/rfc6265#section-5.1.3
    """
    domain_pattern = domain_pattern.removeprefix('.')
    return (domain_name == domain_pattern or
            (domain_name.endswith(domain_pattern) and
             domain_name[-len(domain_pattern) - 1] == '.'))


def _find_auth_credentials(credentials, domain):
    """Find the first set of login credentials (username, password)
    that `domain` matches.
    """
    for domain_pattern, login in credentials.items():
        if _domain_matches(domain, domain_pattern):
            return login
    raise KeyError('Domain {} not found'.format(domain))


def create_url_opener(cookie_file_path, domain):
    """Load username and password from .gitcookies and return a URL opener with
    an authentication handler."""

    # Load authentication credentials
    credentials = load_auth_credentials(cookie_file_path)
    username, password = _find_auth_credentials(credentials, domain)

    # Create URL opener with authentication handler
    auth_handler = HTTPBasicAuthHandler()
    auth_handler.add_password(domain, domain, username, password)
    return build_opener(auth_handler)


def create_url_opener_from_args(args):
    """Create URL opener from command line arguments."""

    if args.use_curl:
        handlers = []
        handlers.append(CurlHTTPHandler(args.use_curl))
        if _HAS_SSL:
            handlers.append(CurlHTTPSHandler(args.use_curl))

        opener = build_opener(*handlers)
        return opener

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
    """Trim XSSI protector and decode JSON objects.

    Returns:
        An object returned by json.loads().

    Raises:
        ValueError: If data doesn't start with a XSSI token.
        json.JSONDecodeError: If data failed to decode.
    """

    # Decode UTF-8
    data = data.decode('utf-8')

    # Trim cross site script inclusion (XSSI) protector
    if data[0:4] != ')]}\'':
        raise ValueError('unexpected responsed content: ' + data)
    data = data[4:]

    # Parse JSON objects
    return json.loads(data)


def _query_change_lists(url_opener, gerrit, query_string, start, count):
    """Query change lists from the Gerrit server with a single request.

    This function performs a single query of the Gerrit server based on the
    input parameters for a list of changes.  The server may return less than
    the number of changes requested.  The caller should check the last record
    returned for the _more_changes attribute to determine if more changes are
    available and perform additional queries adjusting the start index.

    Args:
        url_opener:  URL opener for request
        gerrit: Gerrit server URL
        query_string: Gerrit query string to select changes
        start: Number of changes to be skipped from the beginning
        count: Maximum number of changes to return

    Returns:
        List of changes
    """
    data = [
        ('q', query_string),
        ('o', 'CURRENT_REVISION'),
        ('o', 'CURRENT_COMMIT'),
        ('start', str(start)),
        ('n', str(count)),
    ]
    url = gerrit + '/a/changes/?' + urlencode(data)

    response_file = url_opener.open(url)
    try:
        return _decode_xssi_json(response_file.read())
    finally:
        response_file.close()

def query_change_lists(url_opener, gerrit, query_string, start, count):
    """Query change lists from the Gerrit server.

    This function queries the Gerrit server based on the input parameters for a
    list of changes.  This function handles querying the server multiple times
    if necessary and combining the results that are returned to the caller.

    Args:
        url_opener:  URL opener for request
        gerrit: Gerrit server URL
        query_string: Gerrit query string to select changes
        start: Number of changes to be skipped from the beginning
        count: Maximum number of changes to return

    Returns:
        List of changes
    """
    changes = []
    while len(changes) < count:
        chunk = _query_change_lists(url_opener, gerrit, query_string,
                                    start + len(changes), count - len(changes))
        if not chunk:
            break

        changes += chunk

        # The last change object contains a _more_changes attribute if the
        # number of changes exceeds the query parameter or the internal server
        # limit.  Stop iteration if `_more_changes` attribute doesn't exist.
        if '_more_changes' not in chunk[-1]:
            break

    return changes


def _make_json_post_request(url_opener, url, data, method='POST'):
    """Open an URL request and decode its response.

    Returns a 3-tuple of (code, body, json).
        code: A numerical value, the HTTP status code of the response.
        body: A bytes, the response body.
        json: An object, the parsed JSON response.
    """

    data = json.dumps(data).encode('utf-8')
    headers = {
        'Content-Type': 'application/json; charset=UTF-8',
    }

    request = Request(url, data, headers)
    request.get_method = lambda: method

    try:
        response_file = url_opener.open(request)
    except HTTPError as error:
        response_file = error

    with response_file:
        res_code = response_file.getcode()
        res_body = response_file.read()
        try:
            res_json = _decode_xssi_json(res_body)
        except ValueError:
            # The response isn't JSON if it doesn't start with a XSSI token.
            # Possibly a plain text error message or empty body.
            res_json = None
        return (res_code, res_body, res_json)


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


def submit(url_opener, gerrit_url, change_id):
    """Submit a change list."""

    url = '{}/a/changes/{}/submit'.format(gerrit_url, change_id)

    return _make_json_post_request(url_opener, url, {})


def abandon(url_opener, gerrit_url, change_id, message):
    """Abandon a change list."""

    url = '{}/a/changes/{}/abandon'.format(gerrit_url, change_id)

    data = {}
    if message:
        data['message'] = message

    return _make_json_post_request(url_opener, url, data)


def restore(url_opener, gerrit_url, change_id):
    """Restore a change list."""

    url = '{}/a/changes/{}/restore'.format(gerrit_url, change_id)

    return _make_json_post_request(url_opener, url, {})


def delete(url_opener, gerrit_url, change_id):
    """Delete a change list."""

    url = '{}/a/changes/{}'.format(gerrit_url, change_id)

    return _make_json_post_request(url_opener, url, {}, method='DELETE')


def set_topic(url_opener, gerrit_url, change_id, name):
    """Set the topic name."""

    url = '{}/a/changes/{}/topic'.format(gerrit_url, change_id)
    data = {'topic': name}
    return _make_json_post_request(url_opener, url, data, method='PUT')


def delete_topic(url_opener, gerrit_url, change_id):
    """Delete the topic name."""

    url = '{}/a/changes/{}/topic'.format(gerrit_url, change_id)

    return _make_json_post_request(url_opener, url, {}, method='DELETE')


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


def add_reviewers(url_opener, gerrit_url, change_id, reviewers):
    """Add reviewers."""

    url = '{}/a/changes/{}/revisions/current/review'.format(
        gerrit_url, change_id)

    data = {}
    if reviewers:
        data['reviewers'] = reviewers

    return _make_json_post_request(url_opener, url, data)


def delete_reviewer(url_opener, gerrit_url, change_id, name):
    """Delete reviewer."""

    url = '{}/a/changes/{}/reviewers/{}/delete'.format(
        gerrit_url, change_id, name)

    return _make_json_post_request(url_opener, url, {})


def get_patch(url_opener, gerrit_url, change_id, revision_id='current'):
    """Download the patch file."""

    url = '{}/a/changes/{}/revisions/{}/patch'.format(
        gerrit_url, change_id, revision_id)

    response_file = url_opener.open(url)
    try:
        return base64.b64decode(response_file.read())
    finally:
        response_file.close()

def find_gerrit_name():
    """Find the gerrit instance specified in the default remote."""
    manifest_cmd = ['repo', 'manifest']
    raw_manifest_xml = run(manifest_cmd, stdout=PIPE, check=True).stdout

    manifest_xml = xml.dom.minidom.parseString(raw_manifest_xml)
    default_remote = manifest_xml.getElementsByTagName('default')[0]
    default_remote_name = default_remote.getAttribute('remote')
    for remote in manifest_xml.getElementsByTagName('remote'):
        name = remote.getAttribute('name')
        review = remote.getAttribute('review')
        if review and name == default_remote_name:
            return review.rstrip('/')

    raise ValueError('cannot find gerrit URL from manifest')

def normalize_gerrit_name(gerrit):
    """Strip the trailing slashes because Gerrit will return 404 when there are
    redundant trailing slashes."""
    return gerrit.rstrip('/')

def add_common_parse_args(parser):
    parser.add_argument('query', help='Change list query string')
    parser.add_argument('-g', '--gerrit', help='Gerrit review URL')
    parser.add_argument('--gitcookies',
                        default=os.path.expanduser('~/.gitcookies'),
                        help='Gerrit cookie file')
    parser.add_argument('--limits', default=1000, type=int,
                        help='Max number of change lists')
    parser.add_argument('--start', default=0, type=int,
                        help='Skip first N changes in query')
    parser.add_argument(
        '--use-curl',
        help='Send requests with the specified curl command (e.g. `curl`)')

def _parse_args():
    """Parse command line options."""
    parser = argparse.ArgumentParser()
    add_common_parse_args(parser)
    parser.add_argument('--format', default='json',
                        choices=['json', 'oneline'],
                        help='Print format')
    return parser.parse_args()

def main():
    """Main function"""
    args = _parse_args()

    if args.gerrit:
        args.gerrit = normalize_gerrit_name(args.gerrit)
    else:
        try:
            args.gerrit = find_gerrit_name()
        # pylint: disable=bare-except
        except:
            print('gerrit instance not found, use [-g GERRIT]')
            sys.exit(1)

    # Query change lists
    url_opener = create_url_opener_from_args(args)
    change_lists = query_change_lists(
        url_opener, args.gerrit, args.query, args.start, args.limits)

    # Print the result
    if args.format == 'json':
        json.dump(change_lists, sys.stdout, indent=4, separators=(', ', ': '))
        print()  # Print the end-of-line
    elif args.format == 'oneline':
        for i, change in enumerate(change_lists):
            print('{i:<8} {number:<16} {status:<20} ' \
                  '{change_id:<60} {project:<120} ' \
                  '{subject}'.format(i=i,
                                     project=change['project'],
                                     change_id=change['change_id'],
                                     status=change['status'],
                                     number=change['_number'],
                                     subject=change['subject']))


if __name__ == '__main__':
    main()
