#!/usr/bin/python2.4
#
# Copyright 2010, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Test cases for com.android.monkeyrunner.MonkeyRunner."""

import time
import unittest

from com.android.monkeyrunner import MonkeyRunner


class TestMonkeyRunnerArgParsing(unittest.TestCase):
  """Test ArgParsing for the MonkeyRunner methods."""
  def testWaitForConnectionNoArgs(self):
    MonkeyRunner.waitForConnection()

  def testWaitForConnectionSingleArg(self):
    MonkeyRunner.waitForConnection(2)

  def testWaitForConnectionDoubleArg(self):
    MonkeyRunner.waitForConnection(2, '*')

  def testWaitForConnectionKeywordArg(self):
    MonkeyRunner.waitForConnection(timeout=2, deviceId='foo')

  def testWaitForConnectionKeywordArgTooMany(self):
    try:
      MonkeyRunner.waitForConnection(timeout=2, deviceId='foo', extra='fail')
    except TypeError:
      return
    self.fail('Should have raised TypeError')

  def testSleep(self):
    start = time.time()
    MonkeyRunner.sleep(1.5)
    end = time.time()

    self.assertTrue(end - start >= 1.5)

if __name__ == '__main__':
  unittest.main()
