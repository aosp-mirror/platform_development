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

"""Provides class FolderMounter.

The FolderMounter implements the abstract class BaseMounter. It can
get files from a given folder. The  folder is usually the system/vendor folder
of $OUT folder in an Android build environment.
"""

import logging
import os

from gsi_util.mounters import base_mounter


class _FolderFileAccessor(base_mounter.BaseFileAccessor):

  def __init__(self, folder_dir, path_prefix):
    super(_FolderFileAccessor, self).__init__(path_prefix)
    self._folder_dir = folder_dir

  # override
  def _handle_prepare_file(self, filename_in_storage):
    filename = os.path.join(self._folder_dir, filename_in_storage)
    logging.info('_FolderFileAccessor: Prepare file %s -> %s',
                 filename_in_storage, filename)
    if not os.path.isfile(filename):
      logging.info('  File is not exist: %s', filename_in_storage)
      return None
    return base_mounter.MounterFile(filename)


class FolderMounter(base_mounter.BaseMounter):
  """Provides a file accessor which can access files in the given folder."""

  def __init__(self, folder_dir, path_prefix):
    super(FolderMounter, self).__init__()
    self._folder_dir = folder_dir
    self._path_prefix = path_prefix

  # override
  def _handle_mount(self):
    return _FolderFileAccessor(self._folder_dir, self._path_prefix)
