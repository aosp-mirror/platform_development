#!/usr/bin/env python
#
# Copyright 2017 - The Android Open Source Project
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
"""debugfs-related utilities."""

import logging
import os
import re

from gsi_util.utils.cmd_utils import run_command

_DEBUGFS = 'debugfs'


def dump(image_file, file_spec, out_file):
  """Dumps the content of the file file_spec to the output file out_file.

  Args:
    image_file: The image file to be query.
    file_spec: The full file/directory in the image_file to be copied.
    out_file: The output file name in the local directory.
  Returns:
    True if 'debugfs' command success. False otherwise.
  """
  debugfs_command = 'dump {} {}'.format(file_spec, out_file)
  run_command([_DEBUGFS, '-R', debugfs_command, image_file], log_stderr=True)
  if not os.path.isfile(out_file):
    logging.debug('debugfs failed to dump the file %s', file_spec)
    return False

  return True


def get_type(image_file, file_spec):
  """Gets the type of the given file_spec.

  Args:
    image_file: The image file to be query.
    file_spec: The full file/directory in the image_file to be query.
  Returns:
    None if file_spec does not exist.
    'regular' if file_spec is a file.
    'directory' if file_spec is a directory.
  """
  debugfs_command = 'stat {}'.format(file_spec)
  _, output, error = run_command(
      [_DEBUGFS, '-R', debugfs_command, image_file],
      read_stdout=True,
      read_stderr=True,
      log_stderr=True)
  if re.search('File not found', error):
    logging.debug('get_type() returns None')
    return None

  # Search the "type:" field in the output, it should be 'regular' (for a file)
  # or 'directory'
  m = re.search('Type:\\s*([^\\s]+)', output)
  assert m is not None, '{} outputs with an unknown format.'.format(_DEBUGFS)

  ret = m.group(1)
  logging.debug('get_type() returns \'%s\'', ret)

  return ret
