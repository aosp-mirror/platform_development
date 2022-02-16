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

"""Fastboot-related commands."""

from gsi_util.utils.cmd_utils import run_command


def flash(partition_name, image_name=None, allow_error=False):
  """fastboot flash a partition with a given image."""

  cmd = ['fastboot', 'flash', partition_name]

  # image_name can be None, for `fastboot` to flash
  # ${ANDROID_PRODUCT_OUT}/{partition_name}.img.
  if image_name is not None:
    cmd.append(image_name)

  run_command(cmd, raise_on_error=not allow_error)


def erase(partition_name=None, allow_error=False):
  """fastboot erase a partition."""

  if partition_name is None:
    run_command(['fastboot', '-w'], raise_on_error=not allow_error)
  else:
    run_command(['fastboot', 'erase', partition_name],
                raise_on_error=not allow_error)


def reboot():
  """fastboot reboot a device."""
  run_command(['fastboot', 'reboot'], raise_on_error=False)
