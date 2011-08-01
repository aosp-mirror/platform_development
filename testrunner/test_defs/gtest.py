#!/usr/bin/python
#
#
# Copyright 2011, The Android Open Source Project
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

"""TestSuite for running C/C++ Android tests using gtest framework."""

# python imports
import os
import re

# local imports
import logger
import run_command
import test_suite


class GTestSuite(test_suite.AbstractTestSuite):
  """A test suite for running gtest on device."""

  def __init__(self):
    test_suite.AbstractTestSuite.__init__(self)
    self._target_exec_path = None

  def GetTargetExecPath(self):
    """Get the target path to gtest executable."""
    return self._target_exec_path

  def SetTargetExecPath(self, path):
    self._target_exec_path = path
    return self

  def Run(self, options, adb):
    """Run the provided gtest test suite.

    Args:
      options: command line options
      adb: adb interface
    """
    shell_cmd = adb.PreviewShellCommand(self.GetTargetExecPath())
    logger.Log(shell_cmd)
    if not options.preview:
      # gtest will log to test results to stdout, so no need to do any
      # extra processing
      run_command.RunCommand(shell_cmd, return_output=False)


class GTestFactory(test_suite.AbstractTestFactory):

  def __init__(self, test_root_path, build_path):
    test_suite.AbstractTestFactory.__init__(self, test_root_path,
        build_path)

  def CreateTests(self, sub_tests_path=None):
    """Create tests found in sub_tests_path.

    Looks for test files matching a pattern, and assumes each one is a separate
    binary on target.

    Test files must match one of the following pattern:
      - test_*.[c|cc|cpp]
      - *_test.[c|cc|cpp]
      - *_unittest.[c|cc|cpp]

    """
    if not sub_tests_path:
      sub_tests_path = self.GetTestRootPath()
    test_file_list = []
    if os.path.isfile(sub_tests_path):
      self._EvaluateFile(test_file_list, os.path.basename(sub_tests_path))
    else:
      os.path.walk(sub_tests_path, self._CollectTestSources, test_file_list)
    # TODO: obtain this from makefile instead of hardcoding
    target_root_path = os.path.join('/data', 'nativetest')
    test_suites = []
    for test_file in test_file_list:
      logger.SilentLog('Creating gtest suite for file %s' % test_file)
      suite = GTestSuite()
      suite.SetBuildPath(self.GetBuildPath())
      suite.SetTargetExecPath(os.path.join(target_root_path, test_file))
      test_suites.append(suite)
    return test_suites

  def _CollectTestSources(self, test_list, dirname, files):
    """For each directory, find tests source file and add them to the list.

    Test files must match one of the following pattern:
      - test_*.[cc|cpp]
      - *_test.[cc|cpp]
      - *_unittest.[cc|cpp]

    This method is a callback for os.path.walk.

    Args:
      test_list: Where new tests should be inserted.
      dirname: Current directory.
      files: List of files in the current directory.
    """
    for f in files:
      self._EvaluateFile(test_list, f)

  def _EvaluateFile(self, test_list, file):
    (name, ext) = os.path.splitext(file)
    if ext == ".cc" or ext == ".cpp" or ext == ".c":
      if re.search("_test$|_test_$|_unittest$|_unittest_$|^test_", name):
        logger.SilentLog("Found native test file %s" % file)
        test_list.append(name)
