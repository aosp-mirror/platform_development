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
"""ADB-related utilities."""

import logging
import subprocess

from gsi_util.utils.cmd_utils import run_command


def root(serial_num=None):
  command = ['adb']
  if serial_num:
    command += ['-s', serial_num]
  command += ['root']

  # 'read_stdout=True' to disable output
  run_command(command, raise_on_error=False, read_stdout=True, log_stderr=True)


def pull(local_filename, remote_filename, serial_num=None):
  command = ['adb']
  if serial_num:
    command += ['-s', serial_num]
  command += ['pull', remote_filename, local_filename]

  # 'read_stdout=True' to disable output
  (returncode, _, _) = run_command(
      command, raise_on_error=False, read_stdout=True)

  return returncode == 0
