# Copyright 2013-2014 Sebastian Kreft
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Common function used across modules."""

import io
import os
import re


def filter_lines(lines, filter_regex, groups=None):
    """Filters out the lines not matching the pattern.

    Args:
      lines: list[string]: lines to filter.
      pattern: string: regular expression to filter out lines.

    Returns: list[string]: the list of filtered lines.
    """
    pattern = re.compile(filter_regex)
    for line in lines:
        match = pattern.search(line)
        if match:
            if groups is None:
                yield line
            elif len(groups) == 1:
                yield match.group(groups[0])
            else:
                matched_groups = match.groupdict()
                yield tuple(matched_groups.get(group) for group in groups)


# TODO(skreft): add test
def which(program):
    """Returns a list of paths where the program is found."""
    if (os.path.isabs(program) and os.path.isfile(program) and
            os.access(program, os.X_OK)):
        return [program]

    candidates = []
    locations = os.environ.get("PATH").split(os.pathsep)
    for location in locations:
        candidate = os.path.join(location, program)
        if os.path.isfile(candidate) and os.access(candidate, os.X_OK):
            candidates.append(candidate)
    return candidates


def programs_not_in_path(programs):
    """Returns all the programs that are not found in the PATH."""
    return [program for program in programs if not which(program)]


def _open_for_write(filename):
    """Opens filename for writing, creating the directories if needed."""
    dirname = os.path.dirname(filename)
    if not os.path.exists(dirname):
        os.makedirs(dirname)

    return io.open(filename, 'w')


def _get_cache_filename(name, filename):
    """Returns the cache location for filename and linter name."""
    filename = os.path.abspath(filename)[1:]
    home_folder = os.path.expanduser('~')
    base_cache_dir = os.path.join(home_folder, '.git-lint', 'cache')

    return os.path.join(base_cache_dir, name, filename)


def get_output_from_cache(name, filename):
    """Returns the output from the cache if still valid.

    It checks that the cache file is defined and that its modification time is
    after the modification time of the original file.

    Args:
      name: string: name of the linter.
      filename: string: path of the filename for which we are retrieving the
        output.

    Returns: a string with the output, if it is still valid, or None otherwise.
    """
    cache_filename = _get_cache_filename(name, filename)
    if (os.path.exists(cache_filename) and
            os.path.getmtime(filename) < os.path.getmtime(cache_filename)):
        with io.open(cache_filename) as f:
            return f.read()

    return None


def save_output_in_cache(name, filename, output):
    """Saves output in the cache location.

    Args:
      name: string: name of the linter.
      filename: string: path of the filename for which we are saving the output.
      output: string: full output (not yet filetered) of the lint command.
    """
    cache_filename = _get_cache_filename(name, filename)
    with _open_for_write(cache_filename) as f:
        f.write(output)
