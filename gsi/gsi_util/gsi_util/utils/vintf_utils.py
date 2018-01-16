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
"""VINTF-related utilities."""

import logging

from gsi_util.utils.cmd_utils import run_command


def checkvintf(manifest, matrix):
  """call checkvintf.

  Args:
    manifest: manifest file
    matrix: matrix file

  Returns:
    A tuple with (check_result, error_message)
  """
  logging.debug('checkvintf %s %s...', manifest, matrix)

  # 'read_stdout=True' to disable output
  (returncode, _, stderrdata) = run_command(
      ['checkvintf', manifest, matrix],
      raise_on_error=False,
      read_stdout=True,
      read_stderr=True)
  return (returncode == 0, stderrdata)
