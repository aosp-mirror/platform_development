#!/usr/bin/python2.4
#
# Copyright 2010, The Android Open Source Project
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

"""Test runner to run all the tests in this package."""

import os
import re
import sys
import unittest


TESTCASE_RE = re.compile('_test\.py$')


def AllTestFilesInDir(path):
  """Finds all the unit test files in the given path."""
  return filter(TESTCASE_RE.search, os.listdir(path))


def suite(loader=unittest.defaultTestLoader):
  """Creates the all_tests TestSuite."""
  script_parent_path = os.path.abspath(os.path.dirname(sys.argv[0]))
  # Find all the _test.py files in the same directory we are in
  test_files = AllTestFilesInDir(script_parent_path)
  # Convert them into module names
  module_names = [os.path.splitext(f)[0] for f in test_files]
  # And import them
  modules = map(__import__, module_names)
  # And create the test suite for all these modules
  return unittest.TestSuite([loader.loadTestsFromModule(m) for m in modules])

if __name__ == '__main__':
  result = unittest.TextTestRunner().run(suite())
  if not result.wasSuccessful():
    # On failure return an error code
    sys.exit(1)
