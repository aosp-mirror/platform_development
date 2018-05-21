# Copyright 2018 - The Android Open Source Project
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
"""Provide common implementation of image sources."""

import logging

from gsi_util.mounters import composite_mounter

_DESCRIPTION = """The image sources to be mounted targets.

An image source could be:

 adb[:SERIAL_NUM]: form the device which be connected with adb
  image file name: from the given image file, e.g. the file name of a GSI.
                   If a image file is assigned to be the source of system
                   image, gsi_util will detect system-as-root automatically.
      folder name: from the given folder, e.g. the system/vendor folder in an
                   Android build out folder.
"""


def create_composite_mounter_by_args(args):
  """Creates a CompositeMounter by the images in given args."""

  logging.info('Mount images...')
  mounter = composite_mounter.CompositeMounter()
  for partition in composite_mounter.SUPPORTED_PARTITIONS:
    image_source = vars(args)[partition]
    if image_source:
      logging.info('  %s=%s', partition, image_source)
      mounter.add_by_mount_target(partition, image_source)

  if mounter.is_empty():
    raise RuntimeError('Must give at least one image source.')

  return mounter


def add_argument_group(parser, required_images=None):
  """Add a argument group into the given parser for image sources.

  Args:
    parser: The parser to be added the argument group.
    required_images: A list contains the required images. e.g.
      ['system', 'vendor']. Default is no required images.
  """
  # To avoid pylint W0102
  required_images = required_images or []

  group = parser.add_argument_group('image sources', _DESCRIPTION)
  for partition in composite_mounter.SUPPORTED_PARTITIONS:
    group.add_argument(
        '--' + partition,
        type=str,
        required=partition in required_images,
        help='{} image file name, folder name or "adb"'.format(partition))
