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
"""Image-related utilities."""

import logging

from gsi_util.utils.cmd_utils import run_command


def unsparse(output_filename, input_filename):
  logging.debug('Unsparsing %s...', input_filename)
  run_command(['simg2img', input_filename, output_filename])


def mount(mount_point, image_filename):
  logging.debug('Mounting...')
  run_command(
      ['mount', '-t', 'ext4', '-o', 'loop', image_filename, mount_point],
      sudo=True)


def unmount(mount_point):
  logging.debug('Unmounting...')
  run_command(['umount', '-l', mount_point], sudo=True, raise_on_error=False)


def copy_file(dest, src):
  run_command(['cp', src, dest], sudo=True)
  # This is a hack to give access permission without root
  run_command(['chmod', '+444', dest], sudo=True)
