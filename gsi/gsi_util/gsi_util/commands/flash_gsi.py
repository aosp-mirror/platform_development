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

"""Implementation of gsi_util flash_gsi command."""

from gsi_util.utils import cmd_utils
from gsi_util.utils import fastboot_utils
from gsi_util.utils import file_utils


def do_flash_gsi(args):
  """Flashes a GSI image (system.img).

  Also erases userdata/metadata partition and disables
  Android Verified Boot (AVB).

  Args:
    args: flash_gsi command arguments.
  """

  fastboot_utils.erase()  # erases userdata/cache partition
  # Not every device has metadata partition, so allow_error is True.
  fastboot_utils.erase('metadata', allow_error=True)

  # Flashes GSI.
  fastboot_utils.flash('system', args.image)

  # Disables AVB.
  with file_utils.UnopenedTemporaryFile() as vbmeta_image:
    # vbmeta flag 2 means disable entire AVB verification.
    cmd_utils.run_command(['avbtool', 'make_vbmeta_image',
                           '--flag', '2',
                           '--padding_size', '4096',
                           '--output', vbmeta_image])
    # Not every device uses AVB, so allow_error is True.
    fastboot_utils.flash('vbmeta', vbmeta_image, allow_error=True)

  # Reboots the device.
  fastboot_utils.reboot()


def setup_command_args(subparsers):
  """Sets up command args for 'flash_gsi'."""
  parser = subparsers.add_parser(
      'flash_gsi', help='flash a GSI image',
      description=('Flash a GSI image - '
                   'including erasing userdata, '
                   'disabling AVB (if the device supports AVB) '
                   'and erasing metadata partition (if the device has).'))
  parser.add_argument('-i', '--image',
                      help='the GSI image to flash', type=str)
  parser.set_defaults(func=do_flash_gsi)
