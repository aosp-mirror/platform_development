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

"""Unit test for gsi_util.utils.file_utils."""

import os
import tempfile
import unittest

from gsi_util.utils import file_utils


class UnopenedTemporaryFileTest(unittest.TestCase):
  """Unit test for UnopenedTemporaryFile."""

  def test_normal(self):
    with file_utils.UnopenedTemporaryFile(
        prefix='prefix', suffix='suffix') as f:
      self.assertTrue(os.path.exists(f))
      self.assertEqual(0, os.path.getsize(f))
      self.assertRegexpMatches(os.path.basename(f), r'prefix.+suffix')
      self.assertEqual(tempfile.gettempdir(), os.path.dirname(f))
    self.assertFalse(os.path.exists(f))

  def test_remove_before_exit(self):
    with file_utils.UnopenedTemporaryFile() as f:
      self.assertTrue(os.path.exists(f))
      os.unlink(f)
      self.assertFalse(os.path.exists(f))
    self.assertFalse(os.path.exists(f))

  def test_rename_before_exit(self):
    with file_utils.UnopenedTemporaryFile() as f:
      self.assertTrue(os.path.exists(f))
      new_name = f + '.new'
      os.rename(f, new_name)
      self.assertFalse(os.path.exists(f))
    self.assertFalse(os.path.exists(f))
    self.assertTrue(os.path.exists(new_name))
    os.unlink(new_name)


if __name__ == '__main__':
  unittest.main()
