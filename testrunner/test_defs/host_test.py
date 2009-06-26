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

"""Parser for test definition xml files."""

# python imports
import os

# local imports
from abstract_test import AbstractTestSuite
import errors
import logger
import run_command


class HostTestSuite(AbstractTestSuite):
  """A test suite for running hosttestlib java tests."""

  TAG_NAME = "test-host"

  _CLASS_ATTR = "class"
  # TODO: consider obsoleting in favor of parsing the Android.mk to find the
  # jar name
  _JAR_ATTR = "jar_name"

  _JUNIT_JAR_NAME = "junit.jar"
  _HOSTTESTLIB_NAME = "hosttestlib.jar"
  _DDMLIB_NAME = "ddmlib.jar"
  _lib_names = [_JUNIT_JAR_NAME, _HOSTTESTLIB_NAME, _DDMLIB_NAME]

  _JUNIT_BUILD_PATH = os.path.join("external", "junit")
  _HOSTTESTLIB_BUILD_PATH = os.path.join("development", "tools", "hosttestlib")
  _DDMLIB_BUILD_PATH = os.path.join("development", "tools", "ddms", "libs",
                                    "ddmlib")
  _LIB_BUILD_PATHS = [_JUNIT_BUILD_PATH, _HOSTTESTLIB_BUILD_PATH,
                      _DDMLIB_BUILD_PATH]

  # main class for running host tests
  # TODO: should other runners be supported, and make runner an attribute of
  # the test suite?
  _TEST_RUNNER = "com.android.hosttest.DeviceTestRunner"

  def Parse(self, suite_element):
    super(HostTestSuite, self).Parse(suite_element)
    self._ParseAttribute(suite_element, self._CLASS_ATTR, True)
    self._ParseAttribute(suite_element, self._JAR_ATTR, True)

  def GetBuildDependencies(self, options):
    """Override parent to tag on building host libs."""
    return self._LIB_BUILD_PATHS

  def GetClass(self):
    return self._GetAttribute(self._CLASS_ATTR)

  def GetJarName(self):
    """Returns the name of the host jar that contains the tests."""
    return self._GetAttribute(self._JAR_ATTR)

  def Run(self, options, adb_interface):
    """Runs the host test.

    Results will be displayed on stdout. Assumes 'java' is on system path.

    Args:
      options: command line options for running host tests. Expected member
      fields:
        host_lib_path: path to directory that contains host library files
        test_data_path: path to directory that contains test data files
        preview: if true, do not execute, display commands only
      adb_interface: reference to device under test
    """
    # get the serial number of the device under test, so it can be passed to
    # hosttestlib.
    serial_number = adb_interface.GetSerialNumber()
    self._lib_names.append(self.GetJarName())
    # gather all the host jars that are needed to run tests
    full_lib_paths = []
    for lib in self._lib_names:
      path = os.path.join(options.host_lib_path, lib)
      # make sure jar file exists on host
      if not os.path.exists(path):
        raise errors.AbortError(msg="Could not find jar %s" % path)
      full_lib_paths.append(path)

    # java -cp <libs> <runner class> <test suite class> -s <device serial>
    # -p <test data path>
    cmd = "java -cp %s %s %s -s %s -p %s" % (":".join(full_lib_paths),
                                             self._TEST_RUNNER,
                                             self.GetClass(), serial_number,
                                             options.test_data_path)
    logger.Log(cmd)
    if not options.preview:
      run_command.RunOnce(cmd, return_output=False)
