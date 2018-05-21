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

"""Provides class AdbMounter.

The AdbMounter implements the abstract class BaseMounter. It can get files from
a device which is connected by adb.
"""

import errno
import logging
import os
import shutil
import tempfile

from gsi_util.mounters import base_mounter
from gsi_util.utils import adb_utils


class _AdbFileAccessor(base_mounter.BaseFileAccessor):
  """Provides file access over an adb connection."""

  def __init__(self, temp_dir, serial_num):
    super(_AdbFileAccessor, self).__init__()
    self._temp_dir = temp_dir
    self._serial_num = serial_num

  @staticmethod
  def _make_parent_dirs(filename):
    """Make parent directories as needed, no error if it exists."""
    dir_path = os.path.dirname(filename)
    try:
      os.makedirs(dir_path)
    except OSError as exc:
      if exc.errno != errno.EEXIST:
        raise

  # override
  def _handle_prepare_file(self, filename_in_storage):
    filename = os.path.join(self._temp_dir, filename_in_storage)
    logging.info('_AdbFileAccessor: Prepare file %s -> %s',
                 filename_in_storage, filename)

    self._make_parent_dirs(filename)
    if not adb_utils.pull(filename, filename_in_storage, self._serial_num):
      logging.info('  Fail to prepare file: %s', filename_in_storage)
      return None

    return base_mounter.MounterFile(filename)


class AdbMounter(base_mounter.BaseMounter):
  """Provides a file accessor which can access files by adb."""

  def __init__(self, serial_num=None):
    super(AdbMounter, self).__init__()
    self._serial_num = serial_num
    self._temp_dir = None

  # override
  def _handle_mount(self):
    adb_utils.root(self._serial_num)

    self._temp_dir = tempfile.mkdtemp()
    logging.debug('Created temp dir: %s', self._temp_dir)

    return _AdbFileAccessor(self._temp_dir, self._serial_num)

  # override
  def _handle_unmount(self):
    if self._temp_dir:
      logging.debug('Removing temp dir: %s', self._temp_dir)
      shutil.rmtree(self._temp_dir)
      self._temp_dir = None
