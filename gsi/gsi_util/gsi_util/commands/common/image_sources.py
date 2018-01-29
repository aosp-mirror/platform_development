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

from gsi_util.mounters import composite_mounter

_DESCRIPTION = """The image sources to be mount targets.

An image source could be:

 adb[:SERIAL_NUM]: form the device which be connected with adb
  image file name: from the given image file, e.g. the file name of a GSI.
                   If a image file is assigned to be the source of system
                   image, gsi_util will detect system-as-root automatically.
      folder name: from the given folder, e.g. the system/vendor folder in an
                   Android build out folder.
"""


def create_composite_mounter_by_args(args):
  mounter = composite_mounter.CompositeMounter()
  if args.system:
    mounter.add_by_mount_target('system', args.system)
  if args.vendor:
    mounter.add_by_mount_target('vendor', args.vendor)
  return mounter


def add_argument_group(parser, required_system=False, required_vendor=False):
  """Add a argument group into the given parser for image sources."""

  group = parser.add_argument_group('image sources', _DESCRIPTION)
  group.add_argument(
      '--system',
      type=str,
      required=required_system,
      help='system image file name, folder name or "adb"')
  group.add_argument(
      '--vendor',
      type=str,
      required=required_vendor,
      help='vendor image file name, folder name or "adb"')
