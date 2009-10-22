#!/usr/bin/python2.4
#
#
# Copyright 2009, The Android Open Source Project
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

import sys
import unittest
sys.path.append('../..')

from testrunner import android_manifest


class AndroidManifestTest(unittest.TestCase):
  """Unit tests for AndroidManifest."""

  def setUp(self):
    """Create android_mainfest for testing from sample file."""
    self._manifest = android_manifest.AndroidManifest(app_path='.')

  def testGetPackageName(self):
    self.assertEquals('com.example.android.tests',
                      self._manifest.GetPackageName())

  def testGetInstrumentationNames(self):
    self.assertEquals(['android.test.InstrumentationTestRunner'],
                      self._manifest.GetInstrumentationNames())


if __name__ == '__main__':
  unittest.main()
