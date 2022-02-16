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
"""Provides class CompositeMounter.

CompositeMounter implements the abstract class BaseMounter. It can add multiple
mounters inside as sub-mounters, and operate these sub-mounters with the
BaseMounter interface. Uses CompositeMounter.add_sub_mounter() to add
sub-mounter.

Usually, using CompositeMounter.add_by_mount_target() to add mounters is easier,
the method uses class _MounterFactory to create a mounter and then adds it.

class _MounterFactory provides a method to create a mounter by 'mounter_target'.
'mounter_target' is a name which identify what is the file source to be
mounted. See _MounterFactory.create_by_mount_target() for the detail.
"""

import logging
import os

from gsi_util.mounters import adb_mounter
from gsi_util.mounters import base_mounter
from gsi_util.mounters import folder_mounter
from gsi_util.mounters import image_mounter

SUPPORTED_PARTITIONS = ['system', 'vendor', 'odm']


class _MounterFactory(object):

  @classmethod
  def create_by_mount_target(cls, mount_target, partition):
    """Create a proper Mounter instance by a string of mount target.

    Args:
      partition: the partition to be mounted as
      mount_target: 'adb', a folder name or an image file name to mount.
        see Returns for the detail.

    Returns:
      Returns an AdbMounter if mount_target is 'adb[:SERIAL_NUM]'
      Returns a FolderMounter if mount_target is a folder name
      Returns an ImageMounter if mount_target is an image file name

    Raises:
      ValueError: partiton is not support or mount_target is not exist.
    """
    if partition not in SUPPORTED_PARTITIONS:
      raise ValueError('Wrong partition name "{}"'.format(partition))

    if mount_target == 'adb' or mount_target.startswith('adb:'):
      (_, _, serial_num) = mount_target.partition(':')
      return adb_mounter.AdbMounter(serial_num)

    path_prefix = '/{}/'.format(partition)

    if os.path.isdir(mount_target):
      return folder_mounter.FolderMounter(mount_target, path_prefix)

    if os.path.isfile(mount_target):
      if partition == 'system':
        path_prefix = image_mounter.ImageMounter.DETECT_SYSTEM_AS_ROOT
      return image_mounter.ImageMounter(mount_target, path_prefix)

    raise ValueError('Unknown target "{}"'.format(mount_target))


class _CompositeFileAccessor(base_mounter.BaseFileAccessor):

  def __init__(self, file_accessors):
    super(_CompositeFileAccessor, self).__init__()
    self._file_accessors = file_accessors

  # override
  def _handle_prepare_file(self, filename_in_storage):
    logging.debug('_CompositeFileAccessor._handle_prepare_file(%s)',
                  filename_in_storage)

    pathfile_to_prepare = '/' + filename_in_storage
    for (prefix_path, file_accessor) in self._file_accessors:
      if pathfile_to_prepare.startswith(prefix_path):
        return file_accessor.prepare_file(pathfile_to_prepare)

    logging.debug('  Not found')
    return None


class CompositeMounter(base_mounter.BaseMounter):
  """Implements a BaseMounter which can add multiple sub-mounters."""

  def __init__(self):
    super(CompositeMounter, self).__init__()
    self._mounters = []

  def is_empty(self):
    return not self._mounters

  # override
  def _handle_mount(self):
    file_accessors = [(path_prefix, mounter.mount())
                      for (path_prefix, mounter) in self._mounters]
    return _CompositeFileAccessor(file_accessors)

  # override
  def _handle_unmount(self):
    for (_, mounter) in reversed(self._mounters):
      mounter.unmount()

  def add_sub_mounter(self, mount_point, mounter):
    self._mounters.append((mount_point, mounter))

  def add_by_mount_target(self, partition, mount_target):
    logging.debug('CompositeMounter.add_by_mount_target(%s, %s)',
                  partition, mount_target)
    mount_point = '/{}/'.format(partition)
    mounter = _MounterFactory.create_by_mount_target(mount_target, partition)
    self.add_sub_mounter(mount_point, mounter)
